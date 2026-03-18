package com.financeiro.financeiro

import com.financeiro.financeiro.data.FinancialTransactionEntity
import com.financeiro.financeiro.data.PersonEntity
import com.financeiro.financeiro.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class SupabaseSessionData(
    val accessToken: String,
    val refreshToken: String,
    val userId: String,
    val expiresAtEpochSeconds: Long?,
    val email: String?,
    val phone: String?
)

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val downloadUrl: String?,
    val storeUrl: String?,
    val title: String?,
    val message: String?,
    val isRequired: Boolean
)

class SupabaseException(message: String) : Exception(message)

class SupabaseGateway {
    fun isConfigured(): Boolean {
        return BuildConfig.SUPABASE_URL.isNotBlank() && BuildConfig.SUPABASE_ANON_KEY.isNotBlank()
    }

    suspend fun signUpWithEmail(email: String, password: String): SupabaseSessionData? =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("email", email)
                .put("password", password)
            parseSessionFromAuthResponse(
                request(
                    method = "POST",
                    path = "/auth/v1/signup",
                    body = payload.toString()
                )
            )
        }

    suspend fun signUpWithPhone(phone: String, password: String): SupabaseSessionData? =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("phone", phone)
                .put("password", password)
            parseSessionFromAuthResponse(
                request(
                    method = "POST",
                    path = "/auth/v1/signup",
                    body = payload.toString()
                )
            )
        }

    suspend fun signInWithEmail(email: String, password: String): SupabaseSessionData =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("email", email)
                .put("password", password)
            parseRequiredSession(
                request(
                    method = "POST",
                    path = "/auth/v1/token",
                    query = mapOf("grant_type" to "password"),
                    body = payload.toString()
                )
            )
        }

    suspend fun signInWithPhone(phone: String, password: String): SupabaseSessionData =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("phone", phone)
                .put("password", password)
            parseRequiredSession(
                request(
                    method = "POST",
                    path = "/auth/v1/token",
                    query = mapOf("grant_type" to "password"),
                    body = payload.toString()
                )
            )
        }

    suspend fun resetPasswordForEmail(email: String, redirectTo: String? = null) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("email", email)
        request(
            method = "POST",
            path = "/auth/v1/recover",
            query = redirectTo?.let { mapOf("redirect_to" to it) }.orEmpty(),
            body = payload.toString()
        )
    }

    suspend fun updatePassword(accessToken: String, newPassword: String) = withContext(Dispatchers.IO) {
        val payload = JSONObject().put("password", newPassword)
        request(
            method = "PUT",
            path = "/auth/v1/user",
            body = payload.toString(),
            bearerToken = accessToken
        )
    }

    suspend fun verifyRecoveryCode(email: String, token: String): SupabaseSessionData =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("email", email)
                .put("type", "recovery")
                .put("token", token)
            parseRequiredSession(
                request(
                    method = "POST",
                    path = "/auth/v1/verify",
                    body = payload.toString()
                )
            )
        }

    suspend fun signInWithGoogleIdToken(idToken: String, nonce: String?): SupabaseSessionData =
        withContext(Dispatchers.IO) {
            val payload = JSONObject()
                .put("provider", "google")
                .put("id_token", idToken)
            if (!nonce.isNullOrBlank()) {
                payload.put("nonce", nonce)
            }
            parseRequiredSession(
                request(
                    method = "POST",
                    path = "/auth/v1/token",
                    query = mapOf("grant_type" to "id_token"),
                    body = payload.toString()
                )
            )
        }

    suspend fun refreshSession(refreshToken: String): SupabaseSessionData =
        withContext(Dispatchers.IO) {
            val payload = JSONObject().put("refresh_token", refreshToken)
            parseRequiredSession(
                request(
                    method = "POST",
                    path = "/auth/v1/token",
                    query = mapOf("grant_type" to "refresh_token"),
                    body = payload.toString()
                )
            )
        }

    suspend fun signOut(accessToken: String) {
        withContext(Dispatchers.IO) {
            request(
                method = "POST",
                path = "/auth/v1/logout",
                bearerToken = accessToken
            )
        }
    }

    suspend fun fetchPeople(accessToken: String): List<PersonEntity> = withContext(Dispatchers.IO) {
        val response = request(
            method = "GET",
            path = "/rest/v1/people",
            query = mapOf(
                "select" to "id,name,phone,email,is_active,updated_at",
                "order" to "is_active.desc,updated_at.desc,id.asc"
            ),
            bearerToken = accessToken
        )
        val array = JSONArray(response)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            PersonEntity(
                id = item.optLong("id"),
                name = item.optString("name"),
                phone = item.optNullableString("phone"),
                email = item.optNullableString("email"),
                isActive = item.optBoolean("is_active"),
                updatedAt = item.optLong("updated_at")
            )
        }
    }

    suspend fun fetchTransactions(accessToken: String): List<FinancialTransactionEntity> =
        withContext(Dispatchers.IO) {
            val response = request(
                method = "GET",
                path = "/rest/v1/transactions",
                query = mapOf(
                    "select" to "id,account_id,title,amount,type,category,payment_method,installments,installment_number,original_total_amount,card_payment_date_millis,notes,date_millis",
                    "order" to "date_millis.desc,id.desc"
                ),
                bearerToken = accessToken
            )
            val array = JSONArray(response)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                FinancialTransactionEntity(
                    id = item.optLong("id"),
                    accountId = item.optLong("account_id"),
                    title = item.optString("title"),
                    amount = item.optDouble("amount"),
                    type = TransactionType.valueOf(item.optString("type")),
                    category = item.optString("category"),
                    paymentMethod = item.optString("payment_method"),
                    installments = item.optInt("installments"),
                    installmentNumber = item.optInt("installment_number"),
                    originalTotalAmount = item.optDouble("original_total_amount"),
                    cardPaymentDateMillis = item.optNullableLong("card_payment_date_millis"),
                    notes = item.optString("notes"),
                    dateMillis = item.optLong("date_millis")
                )
            }
        }

    suspend fun upsertPeople(
        accessToken: String,
        userId: String,
        items: List<PersonEntity>
    ) {
        if (items.isEmpty()) return
        withContext(Dispatchers.IO) {
            val body = JSONArray().apply {
                items.forEach { item ->
                    put(
                        JSONObject()
                            .put("id", item.id)
                            .put("owner_id", userId)
                            .put("name", item.name)
                            .put("phone", item.phone?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                            .put("email", item.email?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                            .put("is_active", item.isActive)
                            .put("updated_at", item.updatedAt)
                    )
                }
            }
            request(
                method = "POST",
                path = "/rest/v1/people",
                query = mapOf("on_conflict" to "id"),
                body = body.toString(),
                bearerToken = accessToken,
                prefer = "resolution=merge-duplicates,return=minimal"
            )
        }
    }

    suspend fun upsertTransactions(
        accessToken: String,
        userId: String,
        items: List<FinancialTransactionEntity>
    ) {
        if (items.isEmpty()) return
        withContext(Dispatchers.IO) {
            val body = JSONArray().apply {
                items.forEach { item ->
                    put(
                        JSONObject()
                            .put("id", item.id)
                            .put("owner_id", userId)
                            .put("account_id", item.accountId)
                            .put("title", item.title)
                            .put("amount", item.amount)
                            .put("type", item.type.name)
                            .put("category", item.category)
                            .put("payment_method", item.paymentMethod)
                            .put("installments", item.installments)
                            .put("installment_number", item.installmentNumber)
                            .put("original_total_amount", item.originalTotalAmount)
                            .put(
                                "card_payment_date_millis",
                                item.cardPaymentDateMillis ?: JSONObject.NULL
                            )
                            .put("notes", item.notes)
                            .put("date_millis", item.dateMillis)
                    )
                }
            }
            request(
                method = "POST",
                path = "/rest/v1/transactions",
                query = mapOf("on_conflict" to "id"),
                body = body.toString(),
                bearerToken = accessToken,
                prefer = "resolution=merge-duplicates,return=minimal"
            )
        }
    }

    suspend fun deletePerson(accessToken: String, personId: Long) {
        withContext(Dispatchers.IO) {
            request(
                method = "DELETE",
                path = "/rest/v1/people",
                query = mapOf("id" to "eq.$personId"),
                bearerToken = accessToken
            )
        }
    }

    suspend fun deleteTransaction(accessToken: String, transactionId: Long) {
        withContext(Dispatchers.IO) {
            request(
                method = "DELETE",
                path = "/rest/v1/transactions",
                query = mapOf("id" to "eq.$transactionId"),
                bearerToken = accessToken
            )
        }
    }

    suspend fun deleteTransactionsByAccount(accessToken: String, accountId: Long) {
        withContext(Dispatchers.IO) {
            request(
                method = "DELETE",
                path = "/rest/v1/transactions",
                query = mapOf("account_id" to "eq.$accountId"),
                bearerToken = accessToken
            )
        }
    }

    suspend fun fetchLatestAppUpdate(): AppUpdateInfo? = withContext(Dispatchers.IO) {
        val response = request(
            method = "GET",
            path = "/rest/v1/app_updates",
            query = mapOf(
                "select" to "version_code,version_name,download_url,store_url,title,message,is_required",
                "is_active" to "eq.true",
                "order" to "version_code.desc",
                "limit" to "1"
            )
        )
        val array = JSONArray(response)
        if (array.length() == 0) return@withContext null
        val item = array.getJSONObject(0)
        AppUpdateInfo(
            versionCode = item.optInt("version_code"),
            versionName = item.optString("version_name").ifBlank { item.optInt("version_code").toString() },
            downloadUrl = item.optNullableString("download_url"),
            storeUrl = item.optNullableString("store_url"),
            title = item.optNullableString("title"),
            message = item.optNullableString("message"),
            isRequired = item.optBoolean("is_required")
        )
    }

    private fun parseRequiredSession(response: String): SupabaseSessionData {
        return parseSessionFromAuthResponse(response)
            ?: throw SupabaseException("Sessão Supabase não retornada pela API.")
    }

    private fun parseSessionFromAuthResponse(response: String): SupabaseSessionData? {
        val json = JSONObject(response)
        val accessToken = json.optString("access_token")
        val refreshToken = json.optString("refresh_token")
        if (accessToken.isBlank() || refreshToken.isBlank()) return null

        val user = json.optJSONObject("user")
        return SupabaseSessionData(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = user?.optString("id").orEmpty(),
            expiresAtEpochSeconds = json.optNullableLong("expires_at"),
            email = user?.optNullableString("email"),
            phone = user?.optNullableString("phone")
        )
    }

    private fun request(
        method: String,
        path: String,
        query: Map<String, String> = emptyMap(),
        body: String? = null,
        bearerToken: String? = null,
        prefer: String? = null
    ): String {
        if (!isConfigured()) {
            throw SupabaseException("Configure SUPABASE_URL e SUPABASE_ANON_KEY no local.properties.")
        }

        val connection = (URL(buildUrl(path, query)).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            setRequestProperty("Accept", "application/json")
            if (bearerToken != null) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            if (prefer != null) {
                setRequestProperty("Prefer", prefer)
            }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }

        return try {
            if (body != null) {
                OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8).use { writer ->
                    writer.write(body)
                }
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val payload = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                .orEmpty()

            if (code !in 200..299) {
                throw SupabaseException(extractErrorMessage(payload))
            }
            payload
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(path: String, query: Map<String, String>): String {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        if (query.isEmpty()) return "$base$path"
        val encodedQuery = query.entries.joinToString("&") { (key, value) ->
            "${encode(key)}=${encode(value)}"
        }
        return "$base$path?$encodedQuery"
    }

    private fun encode(value: String): String {
        return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    }

    private fun extractErrorMessage(payload: String): String {
        if (payload.isBlank()) return "Falha na comunicação com o Supabase."
        return runCatching {
            val json = JSONObject(payload)
            json.optNullableString("msg")
                ?: json.optNullableString("message")
                ?: json.optNullableString("error_description")
                ?: json.optNullableString("error")
                ?: payload
        }.getOrElse { payload }
    }
}

private fun JSONObject.optNullableString(key: String): String? {
    return if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
}

private fun JSONObject.optNullableLong(key: String): Long? {
    return if (!has(key) || isNull(key)) null else optLong(key)
}
