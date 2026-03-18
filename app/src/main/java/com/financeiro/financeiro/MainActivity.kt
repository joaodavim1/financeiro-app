package com.financeiro.financeiro

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.financeiro.financeiro.ui.theme.FinanceiroTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MainActivity : ComponentActivity() {
    private val viewModel: FinanceViewModel by viewModels()
    private lateinit var sessionManager: SupabaseSessionManager
    private lateinit var biometricAuthenticator: DeviceBiometricAuthenticator
    private lateinit var googleSignInManager: GoogleSignInManager
    private val appPrefs by lazy { getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE) }
    private val pendingPasswordRecovery = mutableStateOf<PasswordRecoveryLinkData?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        sessionManager = SupabaseSessionManager(this)
        if (savedInstanceState == null) {
            sessionManager.resetSessionForLaunch()
        }
        biometricAuthenticator = DeviceBiometricAuthenticator(this)
        googleSignInManager = GoogleSignInManager(this)
        pendingPasswordRecovery.value = readRecoveryLink(intent)
        setContent {
            var darkThemeEnabled by remember {
                mutableStateOf(appPrefs.getBoolean(KEY_DARK_THEME_ENABLED, false))
            }
            FinanceiroTheme(darkTheme = darkThemeEnabled, dynamicColor = false) {
                val context = LocalContext.current
                var accessState by remember { mutableStateOf(sessionManager.readAccessState()) }
                var signedInEffectsHandled by rememberSaveable { mutableStateOf(accessState.isSignedIn) }
                val biometricAvailable = remember { biometricAuthenticator.isAvailable() }
                val recoveryData = pendingPasswordRecovery.value

                if (accessState.isSignedIn) {
                    LaunchedEffect(accessState.isSignedIn) {
                        if (!signedInEffectsHandled) {
                            signedInEffectsHandled = true
                            requestNotificationPermissionIfNeeded()
                            viewModel.syncWithCloud { message ->
                                if (!message.isNullOrBlank()) {
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                    FinanceApp(
                        viewModel = viewModel,
                        isDarkTheme = darkThemeEnabled,
                        onThemeChange = { enabled ->
                            darkThemeEnabled = enabled
                            appPrefs.edit().putBoolean(KEY_DARK_THEME_ENABLED, enabled).apply()
                        },
                        onLogout = {
                            lifecycleScope.launch {
                                sessionManager.signOut()
                                googleSignInManager.signOut()
                                accessState = sessionManager.readAccessState()
                                signedInEffectsHandled = false
                                Toast.makeText(context, "Sessão encerrada.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } else {
                    LaunchedEffect(Unit) {
                        signedInEffectsHandled = false
                    }
                    LoginScreen(
                        hasLocalAccess = accessState.hasAccount,
                        biometricAvailable = biometricAvailable,
                        biometricEnabled = accessState.biometricEnabled,
                        savedIdentifier = accessState.identifier,
                        recoveryEmailHint = recoveryData?.email,
                        passwordRecoveryPending = recoveryData != null,
                        onCreateAccess = { identifier, password, enableBiometric ->
                            val message = sessionManager.signUp(
                                identifier = identifier,
                                password = password,
                                biometricEnabled = enableBiometric && biometricAvailable
                            )
                            accessState = sessionManager.readAccessState()
                            if (message == null) {
                                viewModel.syncWithCloud()
                                Toast.makeText(
                                    context,
                                    "Acesso criado com sucesso.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            message
                        },
                        onLogin = { identifier, password, enableBiometric ->
                            val message = sessionManager.signIn(
                                identifier = identifier,
                                password = password,
                                biometricEnabled = enableBiometric && biometricAvailable
                            )
                            accessState = sessionManager.readAccessState()
                            if (message == null) {
                                viewModel.syncWithCloud()
                                Toast.makeText(
                                    context,
                                    "Login realizado com sucesso.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            message
                        },
                        onRecoverPassword = { email ->
                            val message = sessionManager.sendPasswordRecovery(email)
                            if (message == null) {
                                Toast.makeText(
                                    context,
                                    "Link de recuperação enviado por email.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            message
                        },
                        onCompletePasswordRecovery = { newPassword ->
                            val recovery = pendingPasswordRecovery.value
                                ?: return@LoginScreen "Link de recuperação inválido."
                            val message = sessionManager.completePasswordRecovery(
                                recovery = recovery,
                                recoveryCode = null,
                                recoveryEmail = recovery.email ?: accessState.identifier,
                                newPassword = newPassword
                            )
                            if (message == null) {
                                pendingPasswordRecovery.value = null
                                Toast.makeText(
                                    context,
                                    "Senha redefinida. Entre com a nova senha.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            message
                        },
                        onDismissPasswordRecovery = {
                            pendingPasswordRecovery.value = null
                        },
                        onBiometricLogin = {
                            biometricAuthenticator.authenticate(
                                onSuccess = {
                                    sessionManager.completeBiometricLogin()
                                    accessState = sessionManager.readAccessState()
                                    viewModel.syncWithCloud()
                                    Toast.makeText(
                                        context,
                                        "Acesso liberado por biometria.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onError = { message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            )
                        },
                        onSocialLogin = { enableBiometric ->
                            when (val result = googleSignInManager.signIn()) {
                                is GoogleSignInOutcome.Success -> {
                                    val gateMessage = requireDeviceAuthForGoogleLogin()
                                    if (gateMessage != null) {
                                        return@LoginScreen gateMessage
                                    }
                                    val message = sessionManager.signInWithGoogle(
                                        profile = result.profile,
                                        biometricEnabled = enableBiometric && biometricAvailable
                                    )
                                    accessState = sessionManager.readAccessState()
                                    if (message == null) {
                                        viewModel.syncWithCloud()
                                        Toast.makeText(
                                            context,
                                            "Login Google realizado com ${result.profile.email}.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    message
                                }
                                is GoogleSignInOutcome.Error -> result.message
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingPasswordRecovery.value = readRecoveryLink(intent)
    }

    private fun readRecoveryLink(sourceIntent: Intent?): PasswordRecoveryLinkData? {
        val rawLink = sourceIntent?.getStringExtra(SplashActivity.EXTRA_DEEP_LINK)
            ?: sourceIntent?.dataString
        return sessionManager.parsePasswordRecoveryLink(rawLink)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return

        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            1001
        )
    }

    private suspend fun requireDeviceAuthForGoogleLogin(): String? {
        if (!biometricAuthenticator.isAvailable()) {
            return "Para entrar com Google, confirme com biometria ou senha do aparelho."
        }

        return suspendCancellableCoroutine { continuation ->
            biometricAuthenticator.authenticate(
                onSuccess = {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                },
                onError = { message ->
                    if (continuation.isActive) {
                        continuation.resume(
                            message.ifBlank {
                                "Confirme com biometria ou senha do aparelho para entrar com Google."
                            }
                        )
                    }
                }
            )
        }
    }

    companion object {
        private const val APP_PREFS_NAME = "financeiro_prefs"
        private const val KEY_DARK_THEME_ENABLED = "dark_theme_enabled"
    }
}
