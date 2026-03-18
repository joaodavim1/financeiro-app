package com.financeiro.financeiro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private enum class LocalAuthMode {
    ENTRAR,
    CRIAR
}

@Composable
fun LoginScreen(
    hasLocalAccess: Boolean,
    biometricAvailable: Boolean,
    biometricEnabled: Boolean,
    savedIdentifier: String,
    recoveryEmailHint: String?,
    passwordRecoveryPending: Boolean,
    onCreateAccess: suspend (String, String, Boolean) -> String?,
    onLogin: suspend (String, String, Boolean) -> String?,
    onRecoverPassword: suspend (String) -> String?,
    onCompletePasswordRecovery: suspend (String) -> String?,
    onDismissPasswordRecovery: () -> Unit,
    onBiometricLogin: () -> Unit,
    onSocialLogin: suspend (Boolean) -> String?
) {
    val darkMode = MaterialTheme.colorScheme.background.red < 0.2f
    val primaryBlue = if (darkMode) Color(0xFF9CC0FF) else Color(0xFF1E3A8A)
    val brightBlue = if (darkMode) Color(0xFF7AA2FF) else Color(0xFF3B82F6)
    val ink = if (darkMode) Color(0xFFF3F4F6) else Color(0xFF13203A)
    val accent = if (darkMode) Color(0xFF1B2A44) else Color(0xFFEAF2FF)
    val cardBorder = if (darkMode) Color(0xFF31435F) else Color(0xFFD8E4FF)
    val errorColor = if (darkMode) Color(0xFFFF8C99) else Color(0xFFD13F52)
    val googleBorder = if (darkMode) Color(0xFF3B4C67) else Color(0xFFD9DDE7)
    val cardColor = if (darkMode) Color(0xFF111827) else Color.White
    val chipIdle = if (darkMode) Color(0xFF1F2937) else Color(0xFFEFF4FF)
    val backgroundBrush = if (darkMode) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF0B1220), Color(0xFF111827), Color(0xFF1B2A44))
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFE6F0FF), Color(0xFFFFF4D8), Color(0xFFF6F8FF))
        )
    }
    val scope = rememberCoroutineScope()

    var authMode by remember(hasLocalAccess) {
        mutableStateOf(if (hasLocalAccess) LocalAuthMode.ENTRAR else LocalAuthMode.CRIAR)
    }
    var identifier by remember(savedIdentifier, authMode) {
        mutableStateOf(savedIdentifier)
    }
    var password by remember(authMode) { mutableStateOf("") }
    var confirmPassword by remember(authMode) { mutableStateOf("") }
    var useBiometric by remember(biometricEnabled, biometricAvailable) {
        mutableStateOf(biometricEnabled && biometricAvailable)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var showRecoveryDialog by remember { mutableStateOf(false) }
    var recoveryEmail by remember(savedIdentifier) { mutableStateOf(savedIdentifier) }
    var recoveryPassword by remember(passwordRecoveryPending) { mutableStateOf("") }
    var recoveryConfirmPassword by remember(passwordRecoveryPending) { mutableStateOf("") }
    var passwordRecoveryError by remember(passwordRecoveryPending) { mutableStateOf<String?>(null) }
    var passwordFieldFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = cardColor,
                tonalElevation = 2.dp,
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .border(width = 1.dp, color = cardBorder, shape = RoundedCornerShape(30.dp))
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(56.dp),
                        shape = CircleShape,
                        color = accent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("F", color = primaryBlue, fontWeight = FontWeight.Black)
                        }
                    }
                    Text("Acesso inicial", color = ink, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuthModeChip(
                            label = "Entrar",
                            selected = authMode == LocalAuthMode.ENTRAR,
                            onClick = {
                                authMode = LocalAuthMode.ENTRAR
                                error = null
                                info = null
                            }
                        )
                        AuthModeChip(
                            label = "Novo acesso",
                            selected = authMode == LocalAuthMode.CRIAR,
                            onClick = {
                                authMode = LocalAuthMode.CRIAR
                                password = ""
                                confirmPassword = ""
                                error = null
                                info = null
                            }
                        )
                    }

                    OutlinedTextField(
                        value = identifier,
                        onValueChange = {
                            identifier = it.trimStart()
                            error = null
                            info = null
                        },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        colors = authOutlinedTextFieldColors(primaryBlue, brightBlue, darkMode),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            error = null
                            info = null
                        },
                        label = { Text("Senha") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = authOutlinedTextFieldColors(primaryBlue, brightBlue, darkMode),
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { passwordFieldFocused = it.isFocused }
                    )
                    if (
                        authMode == LocalAuthMode.ENTRAR &&
                        passwordFieldFocused &&
                        hasLocalAccess &&
                        biometricEnabled &&
                        biometricAvailable
                    ) {
                        TextButton(
                            onClick = onBiometricLogin,
                            modifier = Modifier.align(Alignment.End),
                            enabled = !loading
                        ) {
                            Text(
                                "Entrar com impressão digital",
                                color = primaryBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (authMode == LocalAuthMode.CRIAR) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                error = null
                                info = null
                            },
                            label = { Text("Confirmar senha") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            colors = authOutlinedTextFieldColors(primaryBlue, brightBlue, darkMode),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Usar impressão digital", color = ink, fontWeight = FontWeight.SemiBold)
                        Switch(
                            checked = useBiometric,
                            onCheckedChange = {
                                if (biometricAvailable) {
                                    useBiometric = it
                                    error = null
                                    info = null
                                }
                            },
                            enabled = biometricAvailable
                        )
                    }
                    if (!biometricAvailable) {
                        Text("Biometria indisponível neste aparelho.", color = errorColor)
                    }

                    if (authMode == LocalAuthMode.ENTRAR) {
                        TextButton(
                            onClick = {
                                recoveryEmail = identifier
                                error = null
                                info = null
                                showRecoveryDialog = true
                            },
                            modifier = Modifier.align(Alignment.End),
                            enabled = !loading
                        ) {
                            Text("Recuperar senha", color = primaryBlue, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    error?.let {
                        Text(it, color = errorColor, fontWeight = FontWeight.SemiBold)
                    }
                    info?.let {
                        Text(it, color = brightBlue, fontWeight = FontWeight.SemiBold)
                    }

                    Button(
                        onClick = {
                            if (loading) return@Button
                            scope.launch {
                                loading = true
                                info = null
                                error = when (authMode) {
                                    LocalAuthMode.CRIAR -> {
                                        when {
                                            password != confirmPassword -> "As senhas não conferem."
                                            else -> onCreateAccess(identifier, password, useBiometric)
                                        }
                                    }
                                    LocalAuthMode.ENTRAR -> onLogin(identifier, password, useBiometric)
                                }
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBlue,
                            contentColor = Color.White
                        ),
                        enabled = !loading
                    ) {
                        Text(
                            if (loading) "Carregando..." else if (authMode == LocalAuthMode.CRIAR) "Criar acesso" else "Entrar",
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (hasLocalAccess && biometricEnabled && biometricAvailable) {
                        OutlinedButton(
                            onClick = onBiometricLogin,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryBlue),
                            enabled = !loading
                        ) {
                            Text("Entrar com impressão digital", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider()
                    OutlinedButton(
                        onClick = {
                            if (loading) return@OutlinedButton
                            scope.launch {
                                loading = true
                                info = null
                                error = onSocialLogin(useBiometric)
                                loading = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, googleBorder),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = cardColor,
                            contentColor = ink
                        ),
                        enabled = !loading
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(26.dp),
                                shape = CircleShape,
                                color = cardColor
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Black)
                                }
                            }
                            Text("Continuar com Google", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }

    if (showRecoveryDialog) {
        PasswordRecoveryDialog(
            email = recoveryEmail,
            onEmailChange = {
                recoveryEmail = it
                error = null
                info = null
            },
            onDismiss = {
                showRecoveryDialog = false
            },
            onConfirm = {
                if (loading) return@PasswordRecoveryDialog
                scope.launch {
                    loading = true
                    val message = onRecoverPassword(recoveryEmail)
                    if (message == null) {
                        showRecoveryDialog = false
                        info = "Email enviado. Abra o link de recuperação no celular para redefinir a senha."
                        error = null
                    } else {
                        error = message
                    }
                    loading = false
                }
            },
            loading = loading,
            primaryBlue = primaryBlue,
            brightBlue = brightBlue,
            darkMode = darkMode,
            ink = ink,
            cardColor = cardColor,
            googleBorder = googleBorder
        )
    }

    if (passwordRecoveryPending) {
        PasswordResetDialog(
            emailHint = recoveryEmailHint,
            password = recoveryPassword,
            confirmPassword = recoveryConfirmPassword,
            onPasswordChange = {
                recoveryPassword = it
                passwordRecoveryError = null
                info = null
            },
            onConfirmPasswordChange = {
                recoveryConfirmPassword = it
                passwordRecoveryError = null
                info = null
            },
            onDismiss = {
                recoveryPassword = ""
                recoveryConfirmPassword = ""
                passwordRecoveryError = null
                onDismissPasswordRecovery()
            },
            onConfirm = {
                if (loading) return@PasswordResetDialog
                scope.launch {
                    loading = true
                    when {
                        recoveryPassword != recoveryConfirmPassword -> {
                            passwordRecoveryError = "As senhas não conferem."
                        }
                        else -> {
                            val message = onCompletePasswordRecovery(recoveryPassword)
                            if (message == null) {
                                recoveryPassword = ""
                                recoveryConfirmPassword = ""
                                passwordRecoveryError = null
                                info = "Senha redefinida. Faça o login com a nova senha."
                                error = null
                            } else {
                                passwordRecoveryError = message
                            }
                        }
                    }
                    loading = false
                }
            },
            errorMessage = passwordRecoveryError,
            loading = loading,
            primaryBlue = primaryBlue,
            brightBlue = brightBlue,
            darkMode = darkMode,
            ink = ink,
            cardColor = cardColor,
            googleBorder = googleBorder
        )
    }
}

@Composable
private fun AuthModeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val darkMode = MaterialTheme.colorScheme.background.red < 0.2f
    val selectedColor = Color(0xFF1E3A8A)
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (selected) selectedColor else if (darkMode) Color(0xFF1F2937) else Color(0xFFEFF4FF)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else if (darkMode) Color(0xFFE5E7EB) else Color(0xFF243057),
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun authOutlinedTextFieldColors(
    primaryBlue: Color,
    brightBlue: Color,
    darkMode: Boolean
) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = brightBlue,
    unfocusedBorderColor = if (darkMode) Color(0xFF4B5563) else Color(0xFFB8C4DD),
    disabledBorderColor = if (darkMode) Color(0xFF4B5563) else Color(0xFFB8C4DD),
    focusedTextColor = if (darkMode) Color.White else Color(0xFF13203A),
    unfocusedTextColor = if (darkMode) Color.White else Color(0xFF13203A),
    disabledTextColor = if (darkMode) Color.White else Color(0xFF13203A),
    cursorColor = brightBlue,
    focusedLabelColor = primaryBlue,
    unfocusedLabelColor = if (darkMode) Color(0xFFD1D5DB) else Color(0xFF5B6780),
    disabledLabelColor = if (darkMode) Color(0xFFD1D5DB) else Color(0xFF5B6780),
    focusedPlaceholderColor = if (darkMode) Color(0xFF9CA3AF) else Color(0xFF7C879E),
    unfocusedPlaceholderColor = if (darkMode) Color(0xFF9CA3AF) else Color(0xFF7C879E),
    disabledPlaceholderColor = if (darkMode) Color(0xFF9CA3AF) else Color(0xFF7C879E),
    focusedLeadingIconColor = if (darkMode) Color(0xFFD1D5DB) else Color(0xFF5B6780),
    unfocusedLeadingIconColor = if (darkMode) Color(0xFFD1D5DB) else Color(0xFF5B6780),
    disabledLeadingIconColor = if (darkMode) Color(0xFFD1D5DB) else Color(0xFF5B6780),
    focusedTrailingIconColor = if (darkMode) Color(0xFFD1D5DB) else Color(0xFF5B6780),
    unfocusedTrailingIconColor = if (darkMode) Color(0xFFD1D5DB) else Color(0xFF5B6780),
    disabledTrailingIconColor = if (darkMode) Color(0xFFD1D5DB) else Color(0xFF5B6780),
    focusedContainerColor = if (darkMode) Color(0xFF111827) else Color.White,
    unfocusedContainerColor = if (darkMode) Color(0xFF111827) else Color.White,
    disabledContainerColor = if (darkMode) Color(0xFF111827) else Color.White
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordRecoveryDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    loading: Boolean,
    primaryBlue: Color,
    brightBlue: Color,
    darkMode: Boolean,
    ink: Color,
    cardColor: Color,
    googleBorder: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardColor,
        title = {
            Text("Recuperar senha", color = ink, fontWeight = FontWeight.Bold)
        },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                colors = authOutlinedTextFieldColors(primaryBlue, brightBlue, darkMode),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryBlue,
                    contentColor = Color.White
                )
            ) {
                Text(if (loading) "Enviando..." else "Enviar")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !loading,
                border = androidx.compose.foundation.BorderStroke(1.dp, googleBorder),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ink)
            ) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordResetDialog(
    emailHint: String?,
    password: String,
    confirmPassword: String,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    errorMessage: String?,
    loading: Boolean,
    primaryBlue: Color,
    brightBlue: Color,
    darkMode: Boolean,
    ink: Color,
    cardColor: Color,
    googleBorder: Color
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardColor,
        title = {
            Text("Definir nova senha", color = ink, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = emailHint?.let { "Conta: $it" } ?: "Informe a nova senha para concluir a recuperação.",
                    color = ink
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("Nova senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = authOutlinedTextFieldColors(primaryBlue, brightBlue, darkMode),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirmar nova senha") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = authOutlinedTextFieldColors(primaryBlue, brightBlue, darkMode),
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let {
                    Text(
                        text = it,
                        color = if (darkMode) Color(0xFFFF8C99) else Color(0xFFD13F52),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !loading,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryBlue,
                    contentColor = Color.White
                )
            ) {
                Text(if (loading) "Salvando..." else "Salvar senha")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !loading,
                border = androidx.compose.foundation.BorderStroke(1.dp, googleBorder),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ink)
            ) {
                Text("Cancelar")
            }
        }
    )
}
