package com.financeiro.financeiro

import android.content.Context

data class LocalAccessState(
    val hasAccount: Boolean,
    val isSignedIn: Boolean,
    val identifier: String,
    val biometricEnabled: Boolean,
    val authProvider: String
)

class LocalAccessStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun resetSessionForLaunch() {
        prefs.edit().putBoolean(KEY_SESSION_ACTIVE, false).apply()
    }

    fun readState(): LocalAccessState {
        val normalizedIdentifier = prefs.getString(KEY_IDENTIFIER_NORMALIZED, "").orEmpty()
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        return LocalAccessState(
            hasAccount = normalizedIdentifier.isNotBlank() || accessToken.isNotBlank(),
            isSignedIn = prefs.getBoolean(KEY_SESSION_ACTIVE, false) && accessToken.isNotBlank(),
            identifier = prefs.getString(KEY_IDENTIFIER_DISPLAY, "").orEmpty(),
            biometricEnabled = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false),
            authProvider = prefs.getString(KEY_AUTH_PROVIDER, PROVIDER_LOCAL).orEmpty()
        )
    }

    fun readSession(): SupabaseSessionData? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty()
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, "").orEmpty()
        val userId = prefs.getString(KEY_USER_ID, "").orEmpty()
        if (accessToken.isBlank() || refreshToken.isBlank() || userId.isBlank()) return null

        return SupabaseSessionData(
            accessToken = accessToken,
            refreshToken = refreshToken,
            userId = userId,
            expiresAtEpochSeconds = prefs.getLong(KEY_EXPIRES_AT, 0L).takeIf { it > 0L },
            email = prefs.getString(KEY_SESSION_EMAIL, null),
            phone = prefs.getString(KEY_SESSION_PHONE, null)
        )
    }

    fun savePendingIdentifier(
        identifierDisplay: String,
        normalizedIdentifier: String,
        authProvider: String,
        biometricEnabled: Boolean
    ) {
        prefs.edit()
            .putString(KEY_IDENTIFIER_DISPLAY, identifierDisplay)
            .putString(KEY_IDENTIFIER_NORMALIZED, normalizedIdentifier)
            .putString(KEY_AUTH_PROVIDER, authProvider)
            .putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled)
            .putBoolean(KEY_SESSION_ACTIVE, false)
            .apply()
    }

    fun saveSession(
        session: SupabaseSessionData,
        identifierDisplay: String,
        normalizedIdentifier: String,
        authProvider: String,
        biometricEnabled: Boolean
    ) {
        prefs.edit()
            .putString(KEY_IDENTIFIER_DISPLAY, identifierDisplay)
            .putString(KEY_IDENTIFIER_NORMALIZED, normalizedIdentifier)
            .putString(KEY_AUTH_PROVIDER, authProvider)
            .putBoolean(KEY_BIOMETRIC_ENABLED, biometricEnabled)
            .putBoolean(KEY_SESSION_ACTIVE, true)
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds ?: 0L)
            .putString(KEY_SESSION_EMAIL, session.email)
            .putString(KEY_SESSION_PHONE, session.phone)
            .apply()
    }

    fun updateSession(session: SupabaseSessionData) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, session.accessToken)
            .putString(KEY_REFRESH_TOKEN, session.refreshToken)
            .putString(KEY_USER_ID, session.userId)
            .putLong(KEY_EXPIRES_AT, session.expiresAtEpochSeconds ?: 0L)
            .putString(KEY_SESSION_EMAIL, session.email)
            .putString(KEY_SESSION_PHONE, session.phone)
            .apply()
    }

    fun readLastPasswordRecoveryAt(): Long {
        return prefs.getLong(KEY_LAST_PASSWORD_RECOVERY_AT, 0L)
    }

    fun readLastPasswordRecoveryEmail(): String {
        return prefs.getString(KEY_LAST_PASSWORD_RECOVERY_EMAIL, "").orEmpty()
    }

    fun markPasswordRecoveryRequested(
        email: String,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        prefs.edit()
            .putLong(KEY_LAST_PASSWORD_RECOVERY_AT, nowMillis)
            .putString(KEY_LAST_PASSWORD_RECOVERY_EMAIL, email)
            .apply()
    }

    fun completeBiometricLogin() {
        val hasToken = prefs.getString(KEY_ACCESS_TOKEN, "").orEmpty().isNotBlank()
        prefs.edit().putBoolean(KEY_SESSION_ACTIVE, hasToken).apply()
    }

    fun signOut(clearRemoteSession: Boolean = false) {
        val editor = prefs.edit().putBoolean(KEY_SESSION_ACTIVE, false)
        if (clearRemoteSession) {
            editor.remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .remove(KEY_USER_ID)
                .remove(KEY_EXPIRES_AT)
                .remove(KEY_SESSION_EMAIL)
                .remove(KEY_SESSION_PHONE)
        }
        editor.apply()
    }

    companion object {
        private const val PREFS_NAME = "local_access"
        private const val KEY_IDENTIFIER_DISPLAY = "identifier_display"
        private const val KEY_IDENTIFIER_NORMALIZED = "identifier_normalized"
        private const val KEY_SESSION_ACTIVE = "session_active"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_AUTH_PROVIDER = "auth_provider"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_EXPIRES_AT = "expires_at"
        private const val KEY_SESSION_EMAIL = "session_email"
        private const val KEY_SESSION_PHONE = "session_phone"
        private const val KEY_LAST_PASSWORD_RECOVERY_AT = "last_password_recovery_at"
        private const val KEY_LAST_PASSWORD_RECOVERY_EMAIL = "last_password_recovery_email"
        private const val PROVIDER_LOCAL = "local"
    }
}
