package com.financeiro.financeiro

import android.content.Context
import androidx.room.withTransaction
import com.financeiro.financeiro.data.FinanceDatabase
import com.financeiro.financeiro.data.FinancialTransactionEntity
import com.financeiro.financeiro.data.PersonEntity

class SupabaseSyncService(
    context: Context,
    private val database: FinanceDatabase,
    private val sessionManager: SupabaseSessionManager
) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gateway = SupabaseGateway()
    private val personDao = database.personDao()
    private val transactionDao = database.transactionDao()

    suspend fun syncAllOnLogin(): String? {
        if (!gateway.isConfigured()) {
            return "Configure SUPABASE_URL e SUPABASE_ANON_KEY no local.properties."
        }
        val session = sessionManager.ensureSession() ?: return "Faça login novamente para sincronizar."

        return runCatching {
            val previousUserId = prefs.getString(KEY_LAST_SYNCED_USER_ID, null)
            val isDifferentUser = !previousUserId.isNullOrBlank() && previousUserId != session.userId
            if (isDifferentUser) {
                clearLocalData()
            }

            val remotePeople = gateway.fetchPeople(session.accessToken)
            val remoteTransactions = gateway.fetchTransactions(session.accessToken)

            if (remotePeople.isEmpty() && remoteTransactions.isEmpty() && !isDifferentUser) {
                pushAllLocal(session)
            } else {
                replaceLocalData(remotePeople, remoteTransactions)
            }
            prefs.edit().putString(KEY_LAST_SYNCED_USER_ID, session.userId).apply()
            null
        }.getOrElse { error ->
            error.message ?: "Não foi possível sincronizar com o Supabase."
        }
    }

    suspend fun pushPerson(item: PersonEntity) {
        withSession { session ->
            gateway.upsertPeople(session.accessToken, session.userId, listOf(item))
        }
    }

    suspend fun pushPeople(items: List<PersonEntity>) {
        withSession { session ->
            gateway.upsertPeople(session.accessToken, session.userId, items)
        }
    }

    suspend fun deletePerson(personId: Long) {
        withSession { session ->
            gateway.deletePerson(session.accessToken, personId)
        }
    }

    suspend fun pushTransaction(item: FinancialTransactionEntity) {
        withSession { session ->
            gateway.upsertTransactions(session.accessToken, session.userId, listOf(item))
        }
    }

    suspend fun pushTransactions(items: List<FinancialTransactionEntity>) {
        withSession { session ->
            gateway.upsertTransactions(session.accessToken, session.userId, items)
        }
    }

    suspend fun deleteTransaction(transactionId: Long) {
        withSession { session ->
            gateway.deleteTransaction(session.accessToken, transactionId)
        }
    }

    suspend fun deleteTransactionsByAccount(accountId: Long) {
        withSession { session ->
            gateway.deleteTransactionsByAccount(session.accessToken, accountId)
        }
    }

    private suspend fun pushAllLocal(session: SupabaseSessionData) {
        val localPeople = personDao.listAll()
        val localTransactions = transactionDao.listAll()
        gateway.upsertPeople(session.accessToken, session.userId, localPeople)
        gateway.upsertTransactions(session.accessToken, session.userId, localTransactions)
    }

    private suspend fun replaceLocalData(
        people: List<PersonEntity>,
        transactions: List<FinancialTransactionEntity>
    ) {
        database.withTransaction {
            transactionDao.deleteAll()
            personDao.deleteAll()
            if (people.isNotEmpty()) personDao.insertAll(people)
            if (transactions.isNotEmpty()) transactionDao.insertAll(transactions)
        }
    }

    private suspend fun clearLocalData() {
        database.withTransaction {
            transactionDao.deleteAll()
            personDao.deleteAll()
        }
    }

    private suspend fun withSession(block: suspend (SupabaseSessionData) -> Unit) {
        if (!gateway.isConfigured()) return
        val session = sessionManager.ensureSession() ?: return
        runCatching { block(session) }
    }

    companion object {
        private const val PREFS_NAME = "supabase_sync"
        private const val KEY_LAST_SYNCED_USER_ID = "last_synced_user_id"
    }
}
