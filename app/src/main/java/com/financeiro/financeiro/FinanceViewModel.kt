package com.financeiro.financeiro

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.financeiro.financeiro.data.AccountSettingsEntity
import com.financeiro.financeiro.data.FinanceDatabase
import com.financeiro.financeiro.data.FinancialTransactionEntity
import com.financeiro.financeiro.data.PersonEntity
import com.financeiro.financeiro.data.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class FinancialSummary(
    val income: Double = 0.0,
    val expense: Double = 0.0,
    val balance: Double = 0.0
)

data class AppUpdatePrompt(
    val versionCode: Int,
    val versionName: String,
    val updateUrl: String,
    val title: String,
    val message: String,
    val isRequired: Boolean
)

sealed interface AppUpdateCheckResult {
    data class Available(val update: AppUpdatePrompt) : AppUpdateCheckResult
    data class Unavailable(val message: String) : AppUpdateCheckResult
    data class Error(val message: String) : AppUpdateCheckResult
}

enum class VisualizacaoModo {
    UMA_TELA,
    DUAS_TELAS
}

@OptIn(ExperimentalCoroutinesApi::class)
class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val database = FinanceDatabase.getInstance(application)
    private val transactionDao = database.transactionDao()
    private val personDao = database.personDao()
    private val accountSettingsDao = database.accountSettingsDao()
    private val sessionManager = SupabaseSessionManager(application)
    private val syncService = SupabaseSyncService(application, database, sessionManager)
    private val gateway = SupabaseGateway()
    private val selectedAccountId = MutableStateFlow<Long?>(null)
    private val prefs = application.getSharedPreferences("financeiro_prefs", Context.MODE_PRIVATE)
    private val _visualizacaoModo = MutableStateFlow(
        if (prefs.getString("visualizacao_modo", VisualizacaoModo.UMA_TELA.name) == VisualizacaoModo.DUAS_TELAS.name) {
            VisualizacaoModo.DUAS_TELAS
        } else {
            VisualizacaoModo.UMA_TELA
        }
    )
    val visualizacaoModo: StateFlow<VisualizacaoModo> = _visualizacaoModo

    val accounts: StateFlow<List<PersonEntity>> = personDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val activePerson: StateFlow<PersonEntity?> = personDao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val accountSettings: StateFlow<AccountSettingsEntity?> = selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId == null) flowOf(null) else accountSettingsDao.observeByAccount(accountId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val transactions: StateFlow<List<FinancialTransactionEntity>> = selectedAccountId
        .flatMapLatest { accountId ->
            if (accountId == null) flowOf(emptyList()) else transactionDao.observeByAccount(accountId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val summary: StateFlow<FinancialSummary> = transactions
        .map { list ->
            val income = list
                .filter { it.type == TransactionType.RECEITA }
                .sumOf { it.amount }
            val expense = list
                .filter { it.type == TransactionType.DESPESA }
                .sumOf { it.amount }
            FinancialSummary(
                income = income,
                expense = expense,
                balance = income - expense
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FinancialSummary())

    init {
        viewModelScope.launch {
            ensureDefaultAccount()
            migrateLegacyAccountSettingsIfNeeded()
            repairAccountSettingsFromLegacyListsIfNeeded()
            ensureAccountSettingsForKnownAccounts()
            syncAccountSettingsFromTransactionsForAllAccounts()
        }
        viewModelScope.launch {
            activePerson.collectLatest { person ->
                val accountId = person?.id ?: return@collectLatest
                if (selectedAccountId.value == accountId) return@collectLatest
                ensureAccountSettingsForAccount(accountId)
                selectedAccountId.value = accountId
            }
        }
    }

    fun syncWithCloud(onDone: (String?) -> Unit = {}) {
        viewModelScope.launch {
            val result = syncService.syncAllOnLogin()
            ensureDefaultAccount()
            migrateLegacyAccountSettingsIfNeeded()
            repairAccountSettingsFromLegacyListsIfNeeded()
            ensureAccountSettingsForKnownAccounts()
            syncAccountSettingsFromTransactionsForAllAccounts()
            onDone(result)
        }
    }

    fun checkForOptionalUpdate(onDone: (AppUpdateCheckResult) -> Unit) {
        viewModelScope.launch {
            if (!gateway.isConfigured()) {
                onDone(AppUpdateCheckResult.Error("Atualização remota não configurada."))
                return@launch
            }

            try {
                val latest = gateway.fetchLatestAppUpdate()
                if (latest == null) {
                    onDone(AppUpdateCheckResult.Unavailable("Nenhuma atualização publicada no momento."))
                    return@launch
                }

                val updateUrl = latest.downloadUrl?.takeIf { it.isNotBlank() }
                    ?: latest.storeUrl?.takeIf { it.isNotBlank() }
                if (updateUrl.isNullOrBlank()) {
                    onDone(AppUpdateCheckResult.Error("Há uma versão publicada, mas sem link de atualização."))
                    return@launch
                }

                val installedVersionCode = readInstalledVersionCode()
                if (latest.versionCode <= installedVersionCode) {
                    onDone(AppUpdateCheckResult.Unavailable("Você já está na versão mais recente (${readInstalledVersionName()})."))
                    return@launch
                }

                val targetVersionName = latest.versionName.ifBlank { latest.versionCode.toString() }
                val message = latest.message?.takeIf { it.isNotBlank() }
                    ?: "Há uma nova versão disponível: ${readInstalledVersionName()} -> $targetVersionName."

                onDone(
                    AppUpdateCheckResult.Available(
                        AppUpdatePrompt(
                            versionCode = latest.versionCode,
                            versionName = targetVersionName,
                            updateUrl = updateUrl,
                            title = latest.title?.takeIf { it.isNotBlank() } ?: "Atualização disponível",
                            message = message,
                            isRequired = latest.isRequired
                        )
                    )
                )
            } catch (error: SupabaseException) {
                onDone(AppUpdateCheckResult.Error(error.message ?: "Não foi possível verificar atualizações."))
            } catch (_: Exception) {
                onDone(AppUpdateCheckResult.Error("Não foi possível verificar atualizações."))
            }
        }
    }

    fun saveTransaction(
        currentId: Long?,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        paymentMethod: String,
        installments: Int,
        cardPaymentDateMillis: Long?,
        notes: String,
        dateMillis: Long,
        categoryMonthlyLimit: Double?,
        onDone: (String?) -> Unit = {}
    ) {
        val activeAccountId = activePerson.value?.id ?: selectedAccountId.value ?: return
        if (selectedAccountId.value != activeAccountId) {
            selectedAccountId.value = activeAccountId
        }
        val baseItem = FinancialTransactionEntity(
            id = currentId ?: 0,
            accountId = activeAccountId,
            title = title.trim(),
            amount = amount,
            type = type,
            category = category.trim(),
            paymentMethod = paymentMethod.trim(),
            installments = installments,
            installmentNumber = 1,
            originalTotalAmount = amount,
            cardPaymentDateMillis = cardPaymentDateMillis,
            notes = notes.trim(),
            dateMillis = dateMillis
        )

        viewModelScope.launch {
            val affectedMonths = linkedSetOf<LocalDate>()
            val itemsToSync = mutableListOf<FinancialTransactionEntity>()

            if (currentId == null) {
                val safeInstallments = installments.coerceAtLeast(1)
                if (safeInstallments == 1) {
                    affectedMonths.add(millisToLocalDate(baseItem.dateMillis).withDayOfMonth(1))
                    val payload = baseItem.copy(
                        installments = 1,
                        installmentNumber = 1,
                        originalTotalAmount = amount
                    )
                    val newId = transactionDao.insert(payload)
                    itemsToSync += payload.copy(id = newId)
                } else {
                    val installmentAmounts = splitInstallmentAmounts(amount, safeInstallments)
                    val baseLaunchDate = millisToLocalDate(dateMillis)
                    val baseCardDate = cardPaymentDateMillis?.let(::millisToLocalDate)

                    repeat(safeInstallments) { index ->
                        val launchDate = baseLaunchDate.plusMonths(index.toLong())
                        val cardDate = baseCardDate?.plusMonths(index.toLong())
                        affectedMonths.add(launchDate.withDayOfMonth(1))
                        val payload = baseItem.copy(
                            id = 0,
                            amount = installmentAmounts[index],
                            installments = safeInstallments,
                            installmentNumber = index + 1,
                            originalTotalAmount = amount,
                            cardPaymentDateMillis = cardDate?.let(::localDateToMillis),
                            dateMillis = localDateToMillis(launchDate)
                        )
                        val newId = transactionDao.insert(payload)
                        itemsToSync += payload.copy(id = newId)
                    }
                }
            } else {
                affectedMonths.add(millisToLocalDate(baseItem.dateMillis).withDayOfMonth(1))
                transactionDao.update(baseItem)
                itemsToSync += baseItem
            }

            syncService.pushTransactions(itemsToSync)
            syncAccountSettingsFromTransactions(activeAccountId)

            var warning: String? = null
            if (type == TransactionType.DESPESA && (categoryMonthlyLimit ?: 0.0) > 0.0) {
                val formatter = DateTimeFormatter.ofPattern("MM/yyyy")
                val limit = categoryMonthlyLimit ?: 0.0
                for (monthStart in affectedMonths) {
                    val startMillis = localDateToMillis(monthStart)
                    val endMillis = localDateToMillis(monthStart.plusMonths(1)) - 1
                    val monthExpense = transactionDao.sumExpenseByAccountCategoryAndRange(
                        activeAccountId,
                        category.trim(),
                        startMillis,
                        endMillis
                    )
                    if (monthExpense > limit) {
                        warning =
                            "Valor maximo mensal da categoria ${category.trim()} ultrapassado em ${monthStart.format(formatter)}."
                        break
                    }
                }
            }
            onDone(warning)
        }
    }

    fun deleteTransaction(item: FinancialTransactionEntity) {
        viewModelScope.launch {
            transactionDao.delete(item)
            syncAccountSettingsFromTransactions(item.accountId)
            syncService.deleteTransaction(item.id)
        }
    }

    fun savePerson(
        currentId: Long?,
        name: String,
        phone: String,
        email: String,
        onDone: (Boolean, Long?, String?) -> Unit = { _, _, _ -> }
    ) {
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = email.trim()
        if (cleanName.isBlank() && cleanPhone.isBlank() && cleanEmail.isBlank()) {
            onDone(false, null, "Preencha ao menos um dado da conta.")
            return
        }

        viewModelScope.launch {
            try {
                val existing = currentId?.let { personDao.findById(it) }
                val removedDefaultIds = mutableListOf<Long>()
                personDao.clearActive()
                var savedAccountId = existing?.id
                val payload = PersonEntity(
                    id = existing?.id ?: 0,
                    name = if (cleanName.isNotBlank()) cleanName else existing?.name.orEmpty(),
                    phone = cleanPhone.ifBlank { existing?.phone?.takeIf { it.isNotBlank() } },
                    email = cleanEmail.ifBlank { existing?.email?.takeIf { it.isNotBlank() } },
                    isActive = true,
                    updatedAt = System.currentTimeMillis()
                )
                if (existing == null) {
                    val newId = personDao.insert(payload.copy(id = 0))
                    savedAccountId = newId
                    personDao.findDefaultAccounts()
                        .filter { it.id != newId }
                        .forEach { defaultAccount ->
                            transactionDao.reassignAccount(defaultAccount.id, newId)
                            mergeAccountSettings(defaultAccount.id, newId)
                            accountSettingsDao.deleteByAccount(defaultAccount.id)
                            personDao.deleteById(defaultAccount.id)
                            removedDefaultIds += defaultAccount.id
                        }
                    ensureAccountSettingsForAccount(newId)
                    syncAccountSettingsFromTransactions(newId)
                    selectedAccountId.value = newId
                } else {
                    personDao.update(payload)
                    savedAccountId = payload.id
                    ensureAccountSettingsForAccount(payload.id)
                    syncAccountSettingsFromTransactions(payload.id)
                    selectedAccountId.value = payload.id
                }

                syncService.pushPeople(personDao.listAll())
                removedDefaultIds.forEach { syncService.deletePerson(it) }
                onDone(true, savedAccountId, null)
            } catch (_: SQLiteConstraintException) {
                onDone(false, null, "Telefone ou email ja cadastrado em outra conta.")
            } catch (_: Exception) {
                onDone(false, null, "Nao foi possivel salvar a conta.")
            }
        }
    }

    suspend fun findPersonByAny(name: String, phone: String, email: String): PersonEntity? {
        return personDao.findByAny(name.trim(), phone.trim(), email.trim())
    }

    fun setActiveAccount(accountId: Long) {
        viewModelScope.launch {
            val account = personDao.findById(accountId) ?: return@launch
            personDao.clearActive()
            personDao.update(
                account.copy(
                    isActive = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            ensureAccountSettingsForAccount(accountId)
            syncAccountSettingsFromTransactions(accountId)
            selectedAccountId.value = accountId
            syncService.pushPeople(personDao.listAll())
        }
    }

    fun deleteAccount(accountId: Long, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val currentAccounts = accounts.value
            val account = currentAccounts.firstOrNull { it.id == accountId } ?: run {
                onDone(false)
                return@launch
            }
            val fallback = currentAccounts.firstOrNull { it.id != accountId } ?: run {
                onDone(false)
                return@launch
            }

            transactionDao.deleteByAccount(account.id)
            accountSettingsDao.deleteByAccount(account.id)
            personDao.deleteById(account.id)
            personDao.clearActive()
            personDao.update(
                fallback.copy(
                    isActive = true,
                    updatedAt = System.currentTimeMillis()
                )
            )
            selectedAccountId.value = fallback.id
            syncService.deletePerson(accountId)
            syncService.pushPeople(personDao.listAll())
            onDone(true)
        }
    }

    fun clearAccountHistory(accountId: Long, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                transactionDao.deleteByAccount(accountId)
                syncService.deleteTransactionsByAccount(accountId)
                onDone(true)
            } catch (_: Exception) {
                onDone(false)
            }
        }
    }

    fun setVisualizacaoModo(modo: VisualizacaoModo) {
        _visualizacaoModo.value = modo
        prefs.edit().putString("visualizacao_modo", modo.name).apply()
    }

    private suspend fun ensureDefaultAccount() {
        val current = personDao.findActive()
        if (current != null) {
            selectedAccountId.value = current.id
            return
        }

        val existingAccounts = personDao.listAll()
        if (existingAccounts.isNotEmpty()) {
            val first = existingAccounts.first()
            personDao.update(first.copy(isActive = true, updatedAt = System.currentTimeMillis()))
            ensureAccountSettingsForAccount(first.id)
            selectedAccountId.value = first.id
            return
        }

        val defaultId = personDao.insert(
            PersonEntity(
                name = "Cadastro",
                phone = null,
                email = null,
                isActive = true,
                updatedAt = System.currentTimeMillis()
            )
        )
        ensureAccountSettingsForAccount(defaultId)
        selectedAccountId.value = defaultId
    }

    fun saveAccountSettings(
        accountId: Long,
        expenseCategories: List<String>,
        incomeCategories: List<String>,
        paymentMethods: List<String>,
        paymentMethodCardConfigsRaw: String,
        expenseCategoryLimitsRaw: String
    ) {
        viewModelScope.launch {
            accountSettingsDao.upsert(
                AccountSettingsEntity(
                    accountId = accountId,
                    expenseCategories = encodeStringList(expenseCategories),
                    incomeCategories = encodeStringList(incomeCategories),
                    paymentMethods = encodeStringList(paymentMethods),
                    paymentMethodCardConfigs = paymentMethodCardConfigsRaw,
                    expenseCategoryLimits = expenseCategoryLimitsRaw,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    fun recoverLegacyAccountSettings(
        accountId: Long,
        onDone: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val account = personDao.findById(accountId)
            if (account == null) {
                onDone(false, "Conta nao encontrada.")
                return@launch
            }

            val accounts = personDao.listAll()
            val legacyOwnerId = resolveLegacyOwnerId(accounts)
            val isLegacyOwner = accountId == legacyOwnerId
            val existing = accountSettingsDao.findByAccount(accountId) ?: defaultAccountSettings(accountId)

            val scopedExpense = readLegacyList(prefs, accountScopedKey("expense_categories", accountId))
            val scopedIncome = readLegacyList(prefs, accountScopedKey("income_categories", accountId))
            val scopedPayment = readLegacyList(prefs, accountScopedKey("payment_methods", accountId))
            val globalExpense = if (isLegacyOwner) readLegacyList(prefs, "expense_categories") else emptyList()
            val globalIncome = if (isLegacyOwner) readLegacyList(prefs, "income_categories") else emptyList()
            val globalPayment = if (isLegacyOwner) readLegacyList(prefs, "payment_methods") else emptyList()
            val expenseCandidates = readLegacyListsByPrefix(prefs, "expense_categories_account_")
            val incomeCandidates = readLegacyListsByPrefix(prefs, "income_categories_account_")
            val paymentCandidates = readLegacyListsByPrefix(prefs, "payment_methods_account_")

            val repairedExpense = chooseLegacyRecoveryList(
                directValues = scopedExpense + globalExpense,
                candidateLists = expenseCandidates,
                fallback = decodeStringList(existing.expenseCategories)
            )
            val repairedIncome = chooseLegacyRecoveryList(
                directValues = scopedIncome + globalIncome,
                candidateLists = incomeCandidates,
                fallback = decodeStringList(existing.incomeCategories)
            )
            val repairedPayment = chooseLegacyRecoveryList(
                directValues = scopedPayment + globalPayment,
                candidateLists = paymentCandidates,
                fallback = decodeStringList(existing.paymentMethods)
            )

            val legacyCardConfigs = (
                if (isLegacyOwner) readLegacyCardConfigMap(prefs, "payment_method_card_configs") else emptyMap()
                ) + readLegacyCardConfigMap(prefs, accountScopedKey("payment_method_card_configs", accountId)) +
                chooseLegacyRecoveryMap(
                    directValues = emptyMap(),
                    candidateMaps = readLegacyCardConfigMapsByPrefix(prefs, "payment_method_card_configs_account_"),
                    fallback = emptyMap()
                )
            val existingCardConfigs = decodeCardConfigMap(existing.paymentMethodCardConfigs)
            val repairedCardConfigs = repairedPayment
                .filter(::isCardPaymentMethodName)
                .associateWith { method ->
                    legacyCardConfigs[method]
                        ?: existingCardConfigs[method]
                        ?: CardPaymentConfigValue(closingDay = 25, paymentDay = 5)
                }

            val legacyLimits = (
                if (isLegacyOwner) readLegacyDoubleMap(prefs, "expense_category_limits") else emptyMap()
                ) + readLegacyDoubleMap(prefs, accountScopedKey("expense_category_limits", accountId)) +
                chooseLegacyRecoveryMap(
                    directValues = emptyMap(),
                    candidateMaps = readLegacyDoubleMapsByPrefix(prefs, "expense_category_limits_account_"),
                    fallback = emptyMap()
                )
            val repairedLimits = chooseLegacyRecoveryMap(
                directValues = legacyLimits,
                candidateMaps = emptyList(),
                fallback = decodeDoubleMap(existing.expenseCategoryLimits)
            ).filterKeys { it in repairedExpense }

            val foundLegacyEntries = scopedExpense.size + scopedIncome.size + scopedPayment.size +
                globalExpense.size + globalIncome.size + globalPayment.size +
                expenseCandidates.sumOf { it.size } +
                incomeCandidates.sumOf { it.size } +
                paymentCandidates.sumOf { it.size } +
                legacyCardConfigs.size + legacyLimits.size

            val shouldApplyScreenshotRecovery =
                foundLegacyEntries == 0 &&
                    account.email?.trim()?.lowercase() == SCREENSHOT_RECOVERY_ACCOUNT_EMAIL
            val finalExpense = if (shouldApplyScreenshotRecovery) {
                sortNamedValues(repairedExpense + SCREENSHOT_EXPENSE_CATEGORIES)
            } else {
                repairedExpense.ifEmpty { DEFAULT_EXPENSE_CATEGORIES }
            }
            val finalIncome = if (shouldApplyScreenshotRecovery) {
                repairedIncome.ifEmpty { DEFAULT_INCOME_CATEGORIES }
            } else {
                repairedIncome.ifEmpty { DEFAULT_INCOME_CATEGORIES }
            }
            val finalPayment = if (shouldApplyScreenshotRecovery) {
                sortNamedValues(repairedPayment + SCREENSHOT_PAYMENT_METHODS)
            } else {
                repairedPayment.ifEmpty { DEFAULT_PAYMENT_METHODS }
            }
            val finalCardConfigs = finalPayment
                .filter(::isCardPaymentMethodName)
                .associateWith { method ->
                    when {
                        method.equals("Cartao Master", ignoreCase = true) ->
                            repairedCardConfigs[method] ?: CardPaymentConfigValue(11, 17)
                        else ->
                            repairedCardConfigs[method]
                                ?: existingCardConfigs[method]
                                ?: CardPaymentConfigValue(closingDay = 25, paymentDay = 5)
                    }
                }

            accountSettingsDao.upsert(
                existing.copy(
                    expenseCategories = encodeStringList(finalExpense),
                    incomeCategories = encodeStringList(finalIncome),
                    paymentMethods = encodeStringList(finalPayment),
                    paymentMethodCardConfigs = encodeCardConfigMap(finalCardConfigs),
                    expenseCategoryLimits = encodeDoubleMap(repairedLimits.filterKeys { it in finalExpense }),
                    updatedAt = System.currentTimeMillis()
                )
            )

            onDone(
                true,
                if (foundLegacyEntries > 0) {
                    "Recuperação concluída com dados legados encontrados no aparelho."
                } else if (shouldApplyScreenshotRecovery) {
                    "Recuperação concluída com as listas extraídas das capturas."
                } else {
                    "Nenhuma lista antiga foi encontrada no aparelho para essa conta."
                }
            )
        }
    }

    private suspend fun ensureAccountSettingsForKnownAccounts() {
        personDao.listAll().forEach { account ->
            ensureAccountSettingsForAccount(account.id)
        }
    }

    private suspend fun syncAccountSettingsFromTransactionsForAllAccounts() {
        personDao.listAll().forEach { account ->
            syncAccountSettingsFromTransactions(account.id)
        }
    }

    private suspend fun syncAccountSettingsFromTransactions(accountId: Long) {
        if (accountId <= 0L) return
        val existing = accountSettingsDao.findByAccount(accountId) ?: defaultAccountSettings(accountId)
        val accountTransactions = transactionDao.listByAccount(accountId)

        val expenseFromTransactions = accountTransactions
            .asSequence()
            .filter { it.type == TransactionType.DESPESA }
            .map { it.category }
            .toList()
        val incomeFromTransactions = accountTransactions
            .asSequence()
            .filter { it.type == TransactionType.RECEITA }
            .map { it.category }
            .toList()
        val paymentFromTransactions = accountTransactions
            .asSequence()
            .map { it.paymentMethod }
            .toList()

        val mergedExpenseCategories = sortNamedValues(
            decodeStringList(existing.expenseCategories) + expenseFromTransactions
        ).ifEmpty { DEFAULT_EXPENSE_CATEGORIES }
        val mergedIncomeCategories = sortNamedValues(
            decodeStringList(existing.incomeCategories) + incomeFromTransactions
        ).ifEmpty { DEFAULT_INCOME_CATEGORIES }
        val mergedPaymentMethods = sortNamedValues(
            decodeStringList(existing.paymentMethods) + paymentFromTransactions
        ).ifEmpty { DEFAULT_PAYMENT_METHODS }

        val existingCardConfigs = decodeCardConfigMap(existing.paymentMethodCardConfigs)
        val mergedCardConfigs = mergedPaymentMethods
            .filter(::isCardPaymentMethodName)
            .associateWith { method ->
                when {
                    method.equals("Cartao Master", ignoreCase = true) ->
                        existingCardConfigs[method] ?: CardPaymentConfigValue(11, 17)
                    else ->
                        existingCardConfigs[method] ?: CardPaymentConfigValue(25, 5)
                }
            }
        val mergedLimits = decodeDoubleMap(existing.expenseCategoryLimits)
            .filterKeys { it in mergedExpenseCategories }

        accountSettingsDao.upsert(
            existing.copy(
                expenseCategories = encodeStringList(mergedExpenseCategories),
                incomeCategories = encodeStringList(mergedIncomeCategories),
                paymentMethods = encodeStringList(mergedPaymentMethods),
                paymentMethodCardConfigs = encodeCardConfigMap(mergedCardConfigs),
                expenseCategoryLimits = encodeDoubleMap(mergedLimits),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun ensureAccountSettingsForAccount(accountId: Long) {
        if (accountId <= 0L || accountSettingsDao.findByAccount(accountId) != null) return
        accountSettingsDao.upsert(defaultAccountSettings(accountId))
    }

    private suspend fun mergeAccountSettings(fromAccountId: Long, toAccountId: Long) {
        if (fromAccountId <= 0L || toAccountId <= 0L) return
        val source = accountSettingsDao.findByAccount(fromAccountId) ?: return
        val target = accountSettingsDao.findByAccount(toAccountId) ?: defaultAccountSettings(toAccountId)
        accountSettingsDao.upsert(
            target.copy(
                expenseCategories = encodeStringList(
                    decodeStringList(target.expenseCategories) + decodeStringList(source.expenseCategories)
                ),
                incomeCategories = encodeStringList(
                    decodeStringList(target.incomeCategories) + decodeStringList(source.incomeCategories)
                ),
                paymentMethods = encodeStringList(
                    decodeStringList(target.paymentMethods) + decodeStringList(source.paymentMethods)
                ),
                paymentMethodCardConfigs = encodeCardConfigMap(
                    decodeCardConfigMap(target.paymentMethodCardConfigs) +
                        decodeCardConfigMap(source.paymentMethodCardConfigs)
                ),
                expenseCategoryLimits = encodeDoubleMap(
                    decodeDoubleMap(target.expenseCategoryLimits) +
                        decodeDoubleMap(source.expenseCategoryLimits)
                ),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private suspend fun migrateLegacyAccountSettingsIfNeeded() {
        val accounts = personDao.listAll()
        if (accounts.isEmpty()) return

        val legacyOwnerId = resolveLegacyOwnerId(accounts)

        val alreadyMigrated = prefs.getBoolean(KEY_ACCOUNT_SETTINGS_DB_MIGRATED, false)
        if (alreadyMigrated) return

        accounts.forEach { account ->
            if (accountSettingsDao.findByAccount(account.id) != null) return@forEach
            val accountTransactions = transactionDao.listByAccount(account.id)
            val includeLegacy = account.id == legacyOwnerId
            val expenseCategories = mergeNamedValues(
                defaults = DEFAULT_EXPENSE_CATEGORIES,
                scoped = readLegacyList(prefs, accountScopedKey("expense_categories", account.id)),
                legacy = if (includeLegacy) readLegacyList(prefs, "expense_categories") else emptyList(),
                fromTransactions = accountTransactions
                    .filter { it.type == TransactionType.DESPESA }
                    .map { it.category }
            )
            val incomeCategories = mergeNamedValues(
                defaults = DEFAULT_INCOME_CATEGORIES,
                scoped = readLegacyList(prefs, accountScopedKey("income_categories", account.id)),
                legacy = if (includeLegacy) readLegacyList(prefs, "income_categories") else emptyList(),
                fromTransactions = accountTransactions
                    .filter { it.type == TransactionType.RECEITA }
                    .map { it.category }
            )
            val paymentMethods = mergeNamedValues(
                defaults = DEFAULT_PAYMENT_METHODS,
                scoped = readLegacyList(prefs, accountScopedKey("payment_methods", account.id)),
                legacy = if (includeLegacy) readLegacyList(prefs, "payment_methods") else emptyList(),
                fromTransactions = accountTransactions.map { it.paymentMethod }
            )
            val cardConfigs = (
                if (includeLegacy) readLegacyCardConfigMap(prefs, "payment_method_card_configs") else emptyMap()
                ) + readLegacyCardConfigMap(prefs, accountScopedKey("payment_method_card_configs", account.id))
            val normalizedCardConfigs = paymentMethods
                .filter(::isCardPaymentMethodName)
                .associateWith { method ->
                    cardConfigs[method] ?: CardPaymentConfigValue(closingDay = 25, paymentDay = 5)
                }
            val categoryLimits = (
                if (includeLegacy) readLegacyDoubleMap(prefs, "expense_category_limits") else emptyMap()
                ) + readLegacyDoubleMap(prefs, accountScopedKey("expense_category_limits", account.id))

            accountSettingsDao.upsert(
                AccountSettingsEntity(
                    accountId = account.id,
                    expenseCategories = encodeStringList(expenseCategories),
                    incomeCategories = encodeStringList(incomeCategories),
                    paymentMethods = encodeStringList(paymentMethods),
                    paymentMethodCardConfigs = encodeCardConfigMap(normalizedCardConfigs),
                    expenseCategoryLimits = encodeDoubleMap(categoryLimits),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        prefs.edit()
            .putBoolean(KEY_ACCOUNT_SETTINGS_DB_MIGRATED, true)
            .putLong(KEY_LEGACY_USER_DATA_OWNER_ACCOUNT_ID, legacyOwnerId)
            .apply()
    }

    private suspend fun repairAccountSettingsFromLegacyListsIfNeeded() {
        if (prefs.getBoolean(KEY_ACCOUNT_SETTINGS_DB_REPAIRED, false)) return

        val accounts = personDao.listAll()
        if (accounts.isEmpty()) return

        val legacyOwnerId = prefs.getLong(KEY_LEGACY_USER_DATA_OWNER_ACCOUNT_ID, 0L)
            .takeIf { persistedId -> persistedId > 0L && accounts.any { it.id == persistedId } }

        accounts.forEach { account ->
            val existing = accountSettingsDao.findByAccount(account.id) ?: return@forEach
            val isLegacyOwner = account.id == legacyOwnerId

            val scopedExpense = readLegacyList(prefs, accountScopedKey("expense_categories", account.id))
            val scopedIncome = readLegacyList(prefs, accountScopedKey("income_categories", account.id))
            val scopedPayment = readLegacyList(prefs, accountScopedKey("payment_methods", account.id))
            val globalExpense = if (isLegacyOwner) readLegacyList(prefs, "expense_categories") else emptyList()
            val globalIncome = if (isLegacyOwner) readLegacyList(prefs, "income_categories") else emptyList()
            val globalPayment = if (isLegacyOwner) readLegacyList(prefs, "payment_methods") else emptyList()

            val repairedExpense = if (
                scopedExpense.isNotEmpty() || globalExpense.isNotEmpty()
            ) {
                sortNamedValues(scopedExpense + globalExpense)
            } else {
                decodeStringList(existing.expenseCategories).ifEmpty { DEFAULT_EXPENSE_CATEGORIES }
            }
            val repairedIncome = if (
                scopedIncome.isNotEmpty() || globalIncome.isNotEmpty()
            ) {
                sortNamedValues(scopedIncome + globalIncome)
            } else {
                decodeStringList(existing.incomeCategories).ifEmpty { DEFAULT_INCOME_CATEGORIES }
            }
            val repairedPayment = if (
                scopedPayment.isNotEmpty() || globalPayment.isNotEmpty()
            ) {
                sortNamedValues(scopedPayment + globalPayment)
            } else {
                decodeStringList(existing.paymentMethods).ifEmpty { DEFAULT_PAYMENT_METHODS }
            }

            val legacyCardConfigs = (
                if (isLegacyOwner) readLegacyCardConfigMap(prefs, "payment_method_card_configs") else emptyMap()
                ) + readLegacyCardConfigMap(prefs, accountScopedKey("payment_method_card_configs", account.id))
            val existingCardConfigs = decodeCardConfigMap(existing.paymentMethodCardConfigs)
            val repairedCardConfigs = repairedPayment
                .filter(::isCardPaymentMethodName)
                .associateWith { method ->
                    legacyCardConfigs[method]
                        ?: existingCardConfigs[method]
                        ?: CardPaymentConfigValue(closingDay = 25, paymentDay = 5)
                }

            val legacyLimits = (
                if (isLegacyOwner) readLegacyDoubleMap(prefs, "expense_category_limits") else emptyMap()
                ) + readLegacyDoubleMap(prefs, accountScopedKey("expense_category_limits", account.id))
            val repairedLimits = if (legacyLimits.isNotEmpty()) {
                legacyLimits.filterKeys { it in repairedExpense }
            } else {
                decodeDoubleMap(existing.expenseCategoryLimits).filterKeys { it in repairedExpense }
            }

            accountSettingsDao.upsert(
                existing.copy(
                    expenseCategories = encodeStringList(repairedExpense),
                    incomeCategories = encodeStringList(repairedIncome),
                    paymentMethods = encodeStringList(repairedPayment),
                    paymentMethodCardConfigs = encodeCardConfigMap(repairedCardConfigs),
                    expenseCategoryLimits = encodeDoubleMap(repairedLimits),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        prefs.edit().putBoolean(KEY_ACCOUNT_SETTINGS_DB_REPAIRED, true).apply()
    }

    private fun defaultAccountSettings(accountId: Long): AccountSettingsEntity {
        val defaultCardConfigs = DEFAULT_PAYMENT_METHODS
            .filter(::isCardPaymentMethodName)
            .associateWith { CardPaymentConfigValue(closingDay = 25, paymentDay = 5) }
        return AccountSettingsEntity(
            accountId = accountId,
            expenseCategories = encodeStringList(DEFAULT_EXPENSE_CATEGORIES),
            incomeCategories = encodeStringList(DEFAULT_INCOME_CATEGORIES),
            paymentMethods = encodeStringList(DEFAULT_PAYMENT_METHODS),
            paymentMethodCardConfigs = encodeCardConfigMap(defaultCardConfigs),
            expenseCategoryLimits = "",
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun resolveLegacyOwnerId(accounts: List<PersonEntity>): Long {
        return prefs.getLong(KEY_LEGACY_USER_DATA_OWNER_ACCOUNT_ID, 0L)
            .takeIf { persistedId -> persistedId > 0L && accounts.any { it.id == persistedId } }
            ?: selectedAccountId.value
            ?: accounts.firstOrNull { it.isActive }?.id
            ?: accounts.first().id
    }

    private fun readInstalledVersionCode(): Int {
        val packageInfo = getApplication<Application>().packageManager.getPackageInfo(
            getApplication<Application>().packageName,
            PackageManager.PackageInfoFlags.of(0)
        )
        return packageInfo.longVersionCode.toInt()
    }

    private fun readInstalledVersionName(): String {
        return getApplication<Application>().packageManager.getPackageInfo(
            getApplication<Application>().packageName,
            PackageManager.PackageInfoFlags.of(0)
        ).versionName ?: "desconhecida"
    }
}

private const val SETTINGS_LIST_SEP = "|||"
private const val KEY_ACCOUNT_SETTINGS_DB_MIGRATED = "account_settings_db_migrated_v1"
private const val KEY_ACCOUNT_SETTINGS_DB_REPAIRED = "account_settings_db_repaired_v2"
private const val KEY_LEGACY_USER_DATA_OWNER_ACCOUNT_ID = "legacy_user_data_owner_account_id"
private val DEFAULT_EXPENSE_CATEGORIES =
    listOf("Alimentacao", "Contas", "Lazer", "Saude", "Transporte")
private val DEFAULT_INCOME_CATEGORIES =
    listOf("Salario", "Freelance", "Investimentos", "Vendas", "Outras Receitas")
private val DEFAULT_PAYMENT_METHODS = listOf("Dinheiro", "Pix", "Cartao", "Fiado")
private const val SCREENSHOT_RECOVERY_ACCOUNT_EMAIL = "joaodavim1967@gmail.com"
private val SCREENSHOT_EXPENSE_CATEGORIES = listOf(
    "Acougue",
    "Alimentacao",
    "Aluguel",
    "Combustivel",
    "Contas",
    "Hortifruti",
    "Lazer",
    "Light",
    "Mercado",
    "Outros",
    "Pensao",
    "Pizzaria",
    "Saude",
    "Tel e Internet",
    "Transporte",
    "Veiculos mecanica",
    "impostos taxas banco"
)
private val SCREENSHOT_PAYMENT_METHODS = listOf(
    "App",
    "Cartao",
    "Cartao Master",
    "Cartao Mercado Pago",
    "Cartao Nubank",
    "Dinheiro",
    "Fiado",
    "Outros",
    "Pix",
    "Smartsis",
    "Taxi (diaria)"
)

private data class CardPaymentConfigValue(
    val closingDay: Int,
    val paymentDay: Int
)

private fun accountScopedKey(baseKey: String, accountId: Long): String {
    return "${baseKey}_account_$accountId"
}

private fun encodeStringList(values: List<String>): String {
    return values
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.lowercase() }
        .joinToString(SETTINGS_LIST_SEP)
}

private fun decodeStringList(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    return raw.split(SETTINGS_LIST_SEP)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.lowercase() }
}

private fun encodeDoubleMap(values: Map<String, Double>): String {
    return values
        .filter { it.key.isNotBlank() && it.value > 0.0 }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .entries
        .joinToString(SETTINGS_LIST_SEP) { "${it.key}::${it.value}" }
}

private fun decodeDoubleMap(raw: String): Map<String, Double> {
    if (raw.isBlank()) return emptyMap()
    return raw.split(SETTINGS_LIST_SEP)
        .mapNotNull { entry ->
            val parts = entry.split("::", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val name = parts[0].trim()
            val value = parts[1].trim().toDoubleOrNull()
            if (name.isBlank() || value == null || value <= 0.0) null else name to value
        }
        .toMap()
}

private fun encodeCardConfigMap(values: Map<String, CardPaymentConfigValue>): String {
    return values
        .filter { it.key.isNotBlank() && it.value.closingDay in 1..31 && it.value.paymentDay in 1..31 }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .entries
        .joinToString(SETTINGS_LIST_SEP) {
            "${it.key}::${it.value.closingDay}::${it.value.paymentDay}"
        }
}

private fun decodeCardConfigMap(raw: String): Map<String, CardPaymentConfigValue> {
    if (raw.isBlank()) return emptyMap()
    return raw.split(SETTINGS_LIST_SEP)
        .mapNotNull { entry ->
            val parts = entry.split("::", limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val method = parts[0].trim()
            val closingDay = parts[1].trim().toIntOrNull()
            val paymentDay = parts[2].trim().toIntOrNull()
            if (method.isBlank() || closingDay !in 1..31 || paymentDay !in 1..31) {
                null
            } else {
                method to CardPaymentConfigValue(closingDay = closingDay!!, paymentDay = paymentDay!!)
            }
        }
        .toMap()
}

private fun mergeNamedValues(
    defaults: List<String>,
    scoped: List<String>,
    legacy: List<String>,
    fromTransactions: List<String>
): List<String> {
    return (scoped + legacy + fromTransactions + defaults)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun sortNamedValues(values: List<String>): List<String> {
    return values
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.lowercase() }
}


private fun isCardPaymentMethodName(paymentMethod: String): Boolean {
    val normalized = java.text.Normalizer
        .normalize(paymentMethod.lowercase(), java.text.Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
    return normalized.contains("cartao")
}

private fun readLegacyList(
    prefs: android.content.SharedPreferences,
    key: String
): List<String> {
    val fromSet = runCatching {
        prefs.getStringSet(key, null)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
    }.getOrNull()
    if (!fromSet.isNullOrEmpty()) return fromSet.distinct()

    val raw = prefs.getString(key, null)?.trim().orEmpty()
    if (raw.isBlank()) return emptyList()
    return decodeStringList(raw)
}

private fun readLegacyDoubleMap(
    prefs: android.content.SharedPreferences,
    key: String
): Map<String, Double> {
    return decodeDoubleMap(prefs.getString(key, null)?.trim().orEmpty())
}

private fun readLegacyCardConfigMap(
    prefs: android.content.SharedPreferences,
    key: String
): Map<String, CardPaymentConfigValue> {
    return decodeCardConfigMap(prefs.getString(key, null)?.trim().orEmpty())
}

private fun readLegacyListsByPrefix(
    prefs: android.content.SharedPreferences,
    prefix: String
): List<List<String>> {
    return prefs.all.keys
        .filter { it.startsWith(prefix) }
        .map { key -> readLegacyList(prefs, key) }
        .filter { it.isNotEmpty() }
}

private fun readLegacyDoubleMapsByPrefix(
    prefs: android.content.SharedPreferences,
    prefix: String
): List<Map<String, Double>> {
    return prefs.all.keys
        .filter { it.startsWith(prefix) }
        .map { key -> readLegacyDoubleMap(prefs, key) }
        .filter { it.isNotEmpty() }
}

private fun readLegacyCardConfigMapsByPrefix(
    prefs: android.content.SharedPreferences,
    prefix: String
): List<Map<String, CardPaymentConfigValue>> {
    return prefs.all.keys
        .filter { it.startsWith(prefix) }
        .map { key -> readLegacyCardConfigMap(prefs, key) }
        .filter { it.isNotEmpty() }
}

private fun chooseLegacyRecoveryList(
    directValues: List<String>,
    candidateLists: List<List<String>>,
    fallback: List<String>
): List<String> {
    val direct = sortNamedValues(directValues)
    if (direct.isNotEmpty()) return direct
    val bestCandidate = candidateLists
        .map(::sortNamedValues)
        .maxByOrNull { it.size }
    return if (!bestCandidate.isNullOrEmpty()) bestCandidate else sortNamedValues(fallback)
}

private fun <K, V> chooseLegacyRecoveryMap(
    directValues: Map<K, V>,
    candidateMaps: List<Map<K, V>>,
    fallback: Map<K, V>
): Map<K, V> {
    if (directValues.isNotEmpty()) return directValues
    val bestCandidate = candidateMaps.maxByOrNull { it.size }
    return if (!bestCandidate.isNullOrEmpty()) bestCandidate else fallback
}

private fun splitInstallmentAmounts(total: Double, count: Int): List<Double> {
    val totalBig = BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP)
    val countBig = BigDecimal.valueOf(count.toLong())
    val perInstallment = totalBig.divide(countBig, 2, RoundingMode.DOWN)
    val amounts = MutableList(count) { perInstallment }
    val allocated = perInstallment.multiply(countBig)
    val remainder = totalBig.subtract(allocated)
    amounts[count - 1] = amounts[count - 1].add(remainder)
    return amounts.map { it.setScale(2, RoundingMode.HALF_UP).toDouble() }
}

private fun millisToLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun localDateToMillis(date: LocalDate): Long {
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}
