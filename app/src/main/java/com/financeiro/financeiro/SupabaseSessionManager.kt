package com.financeiro.financeiro

import android.content.Context
import android.net.Uri
import android.util.Patterns
import org.json.JSONObject
import java.util.Base64
import java.util.Locale

class SupabaseSessionManager(
    context: Context
) {
    private val store = LocalAccessStore(context)
    private val gateway = SupabaseGateway()

    fun resetSessionForLaunch() {
        store.resetSessionForLaunch()
    }

    fun readAccessState(): LocalAccessState {
        return store.readState()
    }

    fun completeBiometricLogin() {
        store.completeBiometricLogin()
    }

    fun isConfigured(): Boolean = gateway.isConfigured()

    suspend fun signUp(identifier: String, password: String, biometricEnabled: Boolean): String? {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return "A senha precisa ter ao menos $MIN_PASSWORD_LENGTH caracteres."
        }
        val normalized = normalizeEmail(identifier)
            ?: return "Informe um email válido."
        if (!gateway.isConfigured()) {
            return "Configure SUPABASE_URL e SUPABASE_ANON_KEY no local.properties."
        }

        return runCatching {
            val session = gateway.signUpWithEmail(normalized, password)

            if (session == null) {
                store.savePendingIdentifier(
                    identifierDisplay = identifier.trim(),
                    normalizedIdentifier = normalized,
                    authProvider = PROVIDER_EMAIL,
                    biometricEnabled = biometricEnabled
                )
                return@runCatching "Conta criada. Se o Supabase exigir confirmação, valide e depois entre."
            }

            store.saveSession(
                session = session,
                identifierDisplay = identifier.trim().ifBlank {
                    session.email ?: normalized
                },
                normalizedIdentifier = normalized,
                authProvider = PROVIDER_EMAIL,
                biometricEnabled = biometricEnabled
            )
            null
        }.getOrElse { error ->
            error.toFriendlyAuthMessage()
        }
    }

    suspend fun signIn(identifier: String, password: String, biometricEnabled: Boolean): String? {
        if (password.isBlank()) return "Informe a senha."
        val normalized = normalizeEmail(identifier)
            ?: return "Informe um email válido."
        if (!gateway.isConfigured()) {
            return "Configure SUPABASE_URL e SUPABASE_ANON_KEY no local.properties."
        }

        return runCatching {
            val session = gateway.signInWithEmail(normalized, password)
            store.saveSession(
                session = session,
                identifierDisplay = identifier.trim().ifBlank {
                    session.email ?: normalized
                },
                normalizedIdentifier = normalized,
                authProvider = PROVIDER_EMAIL,
                biometricEnabled = biometricEnabled
            )
            null
        }.getOrElse { error ->
            error.toFriendlyAuthMessage()
        }
    }

    suspend fun sendPasswordRecovery(email: String): String? {
        val normalized = normalizeEmail(email)
            ?: return "Informe um email válido."
        if (!gateway.isConfigured()) {
            return "Configure SUPABASE_URL e SUPABASE_ANON_KEY no local.properties."
        }
        val now = System.currentTimeMillis()
        val lastAttempt = store.readLastPasswordRecoveryAt()
        val remainingMillis = PASSWORD_RECOVERY_COOLDOWN_MILLIS - (now - lastAttempt)
        if (remainingMillis > 0) {
            val remainingSeconds = (remainingMillis / 1000L).coerceAtLeast(1L)
            return "Aguarde $remainingSeconds s antes de pedir outro email de recuperação."
        }

        return runCatching {
            gateway.resetPasswordForEmail(
                email = normalized,
                redirectTo = PASSWORD_RECOVERY_REDIRECT_URL
            )
            store.markPasswordRecoveryRequested(email = normalized, nowMillis = now)
            null
        }.getOrElse { error ->
            error.toFriendlyAuthMessage()
        }
    }

    fun parsePasswordRecoveryLink(rawLink: String?): PasswordRecoveryLinkData? {
        if (rawLink.isNullOrBlank()) return null
        val uri = Uri.parse(rawLink)
        val values = parseParams(uri.fragment) + parseParams(uri.query)
        val isRecoveryLink =
            values["type"]?.equals("recovery", ignoreCase = true) == true ||
                (uri.scheme == "financeiro" && uri.host == "auth" && uri.path == "/reset-password")
        if (!isRecoveryLink) return null
        val accessToken = values["access_token"].orEmpty()
        val recoveryEmail = values["email"]
            ?.takeIf { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
            ?: values["user_email"]?.takeIf { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
            ?: extractEmailFromJwt(accessToken)
            ?: store.readLastPasswordRecoveryEmail().takeIf {
                Patterns.EMAIL_ADDRESS.matcher(it).matches()
            }
        return PasswordRecoveryLinkData(
            accessToken = accessToken.takeIf { it.isNotBlank() },
            email = recoveryEmail,
            requiresRecoveryCode = accessToken.isBlank()
        )
    }

    suspend fun completePasswordRecovery(
        recovery: PasswordRecoveryLinkData?,
        recoveryCode: String?,
        recoveryEmail: String?,
        newPassword: String
    ): String? {
        if (newPassword.length < MIN_PASSWORD_LENGTH) {
            return "A senha precisa ter ao menos $MIN_PASSWORD_LENGTH caracteres."
        }
        if (!gateway.isConfigured()) {
            return "Configure SUPABASE_URL e SUPABASE_ANON_KEY no local.properties."
        }

        return runCatching {
            val accessToken = recovery?.accessToken ?: run {
                val code = recoveryCode?.trim().orEmpty()
                if (code.isBlank()) {
                    throw SupabaseException("Informe o código de recuperação enviado por email.")
                }
                val email = (recovery?.email ?: recoveryEmail).orEmpty().trim().lowercase(Locale.getDefault())
                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    throw SupabaseException("Informe o email da recuperação para validar o código.")
                }
                gateway.verifyRecoveryCode(email, code).accessToken
            }
            gateway.updatePassword(
                accessToken = accessToken,
                newPassword = newPassword
            )
            null
        }.getOrElse { error ->
            error.toFriendlyAuthMessage()
        }
    }

    suspend fun signInWithGoogle(
        profile: GoogleAccountProfile,
        biometricEnabled: Boolean
    ): String? {
        if (!gateway.isConfigured()) {
            return "Configure SUPABASE_URL e SUPABASE_ANON_KEY no local.properties."
        }

        return runCatching {
            val session = gateway.signInWithGoogleIdToken(
                idToken = profile.idToken,
                nonce = null
            )
            val identifierDisplay = profile.displayName.ifBlank {
                session.email ?: profile.email
            }
            val normalizedIdentifier = profile.email.trim().lowercase(Locale.getDefault())
            store.saveSession(
                session = session,
                identifierDisplay = identifierDisplay,
                normalizedIdentifier = normalizedIdentifier,
                authProvider = PROVIDER_GOOGLE,
                biometricEnabled = biometricEnabled
            )
            null
        }.getOrElse { error ->
            error.toFriendlyAuthMessage()
        }
    }

    suspend fun ensureSession(): SupabaseSessionData? {
        val current = store.readSession() ?: return null
        if (!gateway.isConfigured()) return current
        if (!shouldRefresh(current)) return current

        return runCatching {
            val refreshed = gateway.refreshSession(current.refreshToken)
            store.updateSession(refreshed)
            refreshed
        }.getOrElse {
            null
        }
    }

    suspend fun signOut() {
        val session = store.readSession()
        if (session != null && gateway.isConfigured()) {
            runCatching { gateway.signOut(session.accessToken) }
        }
        store.signOut(clearRemoteSession = true)
    }

    private fun shouldRefresh(session: SupabaseSessionData): Boolean {
        val expiresAt = session.expiresAtEpochSeconds ?: return false
        val now = System.currentTimeMillis() / 1000
        return expiresAt <= now + 60
    }

    private fun normalizeEmail(identifier: String): String? {
        val raw = identifier.trim()
        if (raw.isBlank()) return null

        val normalized = raw.lowercase(Locale.getDefault())
        return normalized.takeIf { Patterns.EMAIL_ADDRESS.matcher(it).matches() }
    }

    private fun parseParams(source: String?): Map<String, String> {
        if (source.isNullOrBlank()) return emptyMap()
        return source.split("&")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                val key = parts.getOrNull(0)?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val value = Uri.decode(parts.getOrNull(1).orEmpty())
                key to value
            }
            .toMap()
    }

    private fun extractEmailFromJwt(accessToken: String): String? {
        return runCatching {
            val parts = accessToken.split(".")
            if (parts.size < 2) return null
            val payload = parts[1]
            val padding = "=".repeat((4 - payload.length % 4) % 4)
            val decoded = Base64.getUrlDecoder().decode(payload + padding)
            JSONObject(String(decoded)).optString("email").takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun Throwable.toFriendlyAuthMessage(): String {
        val rawMessage = this.message.orEmpty()
        return when {
            rawMessage.contains("email not confirmed", ignoreCase = true) ->
                "Confirme o email no Supabase antes de entrar."
            rawMessage.contains("invalid login credentials", ignoreCase = true) ->
                "Link ou código de recuperação inválido. Peça um novo email."
            rawMessage.contains("rate limit", ignoreCase = true) ||
                rawMessage.contains("too many requests", ignoreCase = true) ||
                rawMessage.contains("security purposes", ignoreCase = true) ||
                rawMessage.contains("only request this", ignoreCase = true) ||
                rawMessage.contains("429") ->
                "Limite de tentativas excedido. Aguarde um pouco antes de pedir outro email."
            rawMessage.isBlank() -> "Não foi possível autenticar no Supabase."
            else -> rawMessage
        }
    }

    companion object {
        private const val MIN_PASSWORD_LENGTH = 6
        private const val PASSWORD_RECOVERY_COOLDOWN_MILLIS = 60_000L
        private const val PROVIDER_EMAIL = "email"
        private const val PROVIDER_GOOGLE = "google"
        const val PASSWORD_RECOVERY_REDIRECT_URL = "financeiro://auth/reset-password"
    }
}

data class PasswordRecoveryLinkData(
    val accessToken: String?,
    val email: String?,
    val requiresRecoveryCode: Boolean
)
