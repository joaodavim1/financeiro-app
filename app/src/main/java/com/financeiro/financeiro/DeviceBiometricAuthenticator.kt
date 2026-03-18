package com.financeiro.financeiro

import android.annotation.SuppressLint
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.CancellationSignal
import androidx.activity.ComponentActivity

class DeviceBiometricAuthenticator(
    private val activity: ComponentActivity
) {
    private val allowedAuthenticators =
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL

    fun isAvailable(): Boolean {
        val manager = activity.getSystemService(BiometricManager::class.java) ?: return false
        return manager.canAuthenticate(allowedAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun unavailableMessage(): String {
        val manager = activity.getSystemService(BiometricManager::class.java)
            ?: return "Biometria ou senha do aparelho indisponível neste aparelho."
        return when (manager.canAuthenticate(allowedAuthenticators)) {
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                "Configure biometria ou senha do aparelho para liberar esse acesso."
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                "Este aparelho não possui biometria disponível nem bloqueio configurado."
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                "A autenticação do aparelho está indisponível no momento."
            else -> "Não foi possível usar biometria ou senha do aparelho neste aparelho."
        }
    }

    @SuppressLint("MissingPermission")
    fun authenticate(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isAvailable()) {
            onError(unavailableMessage())
            return
        }

        val prompt = BiometricPrompt.Builder(activity)
            .setTitle("Confirmar acesso")
            .setSubtitle("Use biometria ou a senha do aparelho")
            .setDescription("O acesso ao Financeiro sera liberado apos a autenticacao do aparelho.")
            .setAllowedAuthenticators(allowedAuthenticators)
            .build()

        prompt.authenticate(
            CancellationSignal(),
            activity.mainExecutor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    onError(
                        errString?.toString().orEmpty()
                            .ifBlank { "Falha na autenticacao do aparelho." }
                    )
                }

                override fun onAuthenticationFailed() {
                    onError("Autenticacao nao reconhecida. Tente novamente.")
                }
            }
        )
    }
}
