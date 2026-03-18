package com.financeiro.financeiro

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

data class GoogleAccountProfile(
    val email: String,
    val displayName: String,
    val idToken: String
)

class GoogleSignInManager(
    private val context: Context
) {
    private val credentialManager = CredentialManager.create(context)

    suspend fun signIn(): GoogleSignInOutcome {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            return GoogleSignInOutcome.Error(
                "Configure o GOOGLE_WEB_CLIENT_ID para liberar o login com a conta Google do aparelho."
            )
        }

        return try {
            val response = tryAnyDeviceAccount()
                ?: return GoogleSignInOutcome.Error(
                    "Nenhuma conta Google disponível para este aplicativo no aparelho."
                )
            parseGoogleCredential(response)
        } catch (_: GetCredentialCancellationException) {
            GoogleSignInOutcome.Error("Login com Google cancelado.")
        } catch (e: GetCredentialException) {
            GoogleSignInOutcome.Error(
                e.errorMessage?.toString() ?: "Não foi possível entrar com Google neste aparelho."
            )
        }
    }

    private suspend fun tryAnyDeviceAccount(): GetCredentialResponse? {
        return runCatching {
            credentialManager.getCredential(
                context = context,
                request = buildRequest()
            )
        }.getOrElse { error ->
            when (error) {
                is NoCredentialException -> null
                else -> throw error
            }
        }
    }

    suspend fun signOut() {
        runCatching {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        }.getOrElse { error ->
            if (error !is ClearCredentialException) throw error
        }
    }

    private fun parseGoogleCredential(response: GetCredentialResponse): GoogleSignInOutcome {
        val credential = response.credential
        if (credential !is CustomCredential) {
            return GoogleSignInOutcome.Error("Resposta de login Google não reconhecida.")
        }
        if (
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL &&
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL
        ) {
            return GoogleSignInOutcome.Error("Tipo de credencial Google inesperado.")
        }

        return try {
            val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val email = googleCredential.id
            val displayName = googleCredential.displayName?.takeIf { it.isNotBlank() } ?: email
            val idToken = googleCredential.idToken
            if (idToken.isBlank()) {
                return GoogleSignInOutcome.Error("O Google não retornou o token necessário para o Supabase.")
            }
            GoogleSignInOutcome.Success(
                GoogleAccountProfile(
                    email = email,
                    displayName = displayName,
                    idToken = idToken
                )
            )
        } catch (_: GoogleIdTokenParsingException) {
            GoogleSignInOutcome.Error("Não foi possível interpretar a conta Google selecionada.")
        }
    }

    private fun buildRequest(): GetCredentialRequest {
        val option = GetSignInWithGoogleOption.Builder(
            BuildConfig.GOOGLE_WEB_CLIENT_ID
        )
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()
    }
}

sealed interface GoogleSignInOutcome {
    data class Success(val profile: GoogleAccountProfile) : GoogleSignInOutcome
    data class Error(val message: String) : GoogleSignInOutcome
}
