package com.financeiro.financeiro

import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.financeiro.financeiro.data.FinancialTransactionEntity
import com.financeiro.financeiro.data.PersonEntity
import com.financeiro.financeiro.data.TransactionType
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val moneyFormat = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}
private val fullDateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val shortDateFormat = DateTimeFormatter.ofPattern("dd/MM/yy")

private object FinanceUiThemeState {
    var darkMode by mutableStateOf(false)
}

private val headerBlue: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF9CC0FF) else Color(0xFF2A2F92)

private val incomeGreen: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF7EE18E) else Color(0xFF53C266)

private val expenseRed: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFFFF7A8A) else Color(0xFFEA4A5B)

private val bgGray: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF0F172A) else Color(0xFFF1F1F4)

private val chipGray: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF243041) else Color(0xFFE9E9ED)

private val appSurface: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF111827) else Color.White

private val appSurfaceAlt: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF1F2937) else Color(0xFFF6F7FB)

private val appSurfaceSoft: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF162133) else Color(0xFFF9F8FC)

private val appTextPrimary: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFFF3F4F6) else Color(0xFF1F2430)

private val appTextSecondary: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF9CA3AF) else Color.Gray

private val appPrimaryTintBg: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF1B2A44) else Color(0xFFEFF4FF)

private val appDangerTintBg: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF3B1F28) else Color(0xFFFFEEF0)

private val appSuccessTintBg: Color
    get() = if (FinanceUiThemeState.darkMode) Color(0xFF183527) else Color(0xFFE9F8EC)

@Composable
private fun financeOutlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = headerBlue,
    unfocusedBorderColor = appTextSecondary,
    disabledBorderColor = appTextSecondary,
    focusedTextColor = appTextPrimary,
    unfocusedTextColor = appTextPrimary,
    disabledTextColor = appTextPrimary,
    cursorColor = headerBlue,
    focusedLabelColor = headerBlue,
    unfocusedLabelColor = appTextSecondary,
    disabledLabelColor = appTextSecondary,
    focusedPlaceholderColor = appTextSecondary,
    unfocusedPlaceholderColor = appTextSecondary,
    disabledPlaceholderColor = appTextSecondary,
    focusedLeadingIconColor = appTextSecondary,
    unfocusedLeadingIconColor = appTextSecondary,
    disabledLeadingIconColor = appTextSecondary,
    focusedTrailingIconColor = appTextSecondary,
    unfocusedTrailingIconColor = appTextSecondary,
    disabledTrailingIconColor = appTextSecondary,
    focusedContainerColor = appSurface,
    unfocusedContainerColor = appSurface,
    disabledContainerColor = appSurface
)

private const val LIST_SEP = "|||"
private const val ACCOUNT_PHOTO_KEY_PREFIX = "account_photo_uri_"
private const val LEGACY_USER_DATA_OWNER_ACCOUNT_KEY = "legacy_user_data_owner_account_id"

private data class CardPaymentConfig(
    val closingDay: Int,
    val paymentDay: Int
)

private fun accountPhotoKey(accountId: Long): String = "$ACCOUNT_PHOTO_KEY_PREFIX$accountId"

private fun accountScopedKey(baseKey: String, accountId: Long?): String {
    val safeAccountId = accountId ?: 0L
    return "${baseKey}_account_$safeAccountId"
}

private fun loadAccountPhotoUri(
    prefs: android.content.SharedPreferences,
    accountId: Long?
): String? {
    if (accountId == null || accountId <= 0L) return null
    return prefs.getString(accountPhotoKey(accountId), null)?.takeIf { it.isNotBlank() }
}

private fun saveAccountPhotoUri(
    prefs: android.content.SharedPreferences,
    accountId: Long,
    photoUri: String?
) {
    if (accountId <= 0L) return
    val editor = prefs.edit()
    if (photoUri.isNullOrBlank()) {
        editor.remove(accountPhotoKey(accountId))
    } else {
        editor.putString(accountPhotoKey(accountId), photoUri)
    }
    editor.apply()
}

private fun openExternalLink(context: Context, rawUrl: String): Boolean {
    val normalizedUrl = rawUrl.trim().let { url ->
        when {
            url.startsWith("http://", ignoreCase = true) -> url
            url.startsWith("https://", ignoreCase = true) -> url
            else -> "https://$url"
        }
    }
    val targetIntent = Intent(Intent.ACTION_VIEW, Uri.parse(normalizedUrl)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooserIntent = Intent.createChooser(targetIntent, "Abrir atualização").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return runCatching {
        context.startActivity(chooserIntent)
    }.isSuccess
}

private fun copyTextToClipboard(context: Context, label: String, value: String): Boolean {
    return runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    }.isSuccess
}

private fun loadAccountPhotoBitmap(context: Context, uriString: String?): ImageBitmap? {
    if (uriString.isNullOrBlank()) return null
    return runCatching {
        val uri = Uri.parse(uriString)
        loadBitmapRespectingOrientation(context, uri)?.asImageBitmap()
    }.getOrNull()
}

private fun loadBitmapRespectingOrientation(context: Context, uri: Uri): Bitmap? {
    val bitmap = when (uri.scheme?.lowercase(Locale.getDefault())) {
        "file" -> BitmapFactory.decodeFile(uri.path)
        else -> context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream)
        }
    } ?: return null

    val rotation = when (uri.scheme?.lowercase(Locale.getDefault())) {
        "file" -> uri.path?.let(::readBitmapRotationFromFile) ?: 0f
        else -> readBitmapRotationFromContentUri(context, uri)
    }
    return rotateBitmapIfNeeded(bitmap, rotation)
}

private fun readBitmapRotationFromFile(path: String): Float {
    return runCatching {
        when (ExifInterface(path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)
}

private fun readBitmapRotationFromContentUri(context: Context, uri: Uri): Float {
    return runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        } ?: 0f
    }.getOrDefault(0f)
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, rotation: Float): Bitmap {
    if (rotation == 0f) return bitmap
    val matrix = Matrix().apply { postRotate(rotation) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun saveAccountPhotoBitmap(
    context: Context,
    accountId: Long,
    bitmap: Bitmap
): String? {
    if (accountId <= 0L) return null
    return runCatching {
        val normalizedBitmap = normalizeCapturedAccountPhoto(bitmap)
        val photoDir = File(context.filesDir, "account_photos").apply { mkdirs() }
        val photoFile = File(photoDir, "account_$accountId.jpg")
        FileOutputStream(photoFile).use { output ->
            normalizedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
        }
        Uri.fromFile(photoFile).toString()
    }.getOrNull()
}

private fun normalizeCapturedAccountPhoto(bitmap: Bitmap): Bitmap {
    if (bitmap.width <= bitmap.height) return bitmap
    return rotateBitmapIfNeeded(bitmap, 90f)
}

@Composable
private fun rememberAccountPhotoBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    return remember(uriString, context) {
        loadAccountPhotoBitmap(context, uriString)
    }
}

@Composable
private fun AccountAvatar(
    accountName: String,
    photoUri: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val bitmap = rememberAccountPhotoBitmap(photoUri)
    val initials = remember(accountName) {
        accountName
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.take(1) }
            .uppercase(Locale.getDefault())
            .ifBlank { "C" }
    }

    val avatarModifier = modifier
        .size(size)
        .let { base ->
            if (onClick != null) base.clip(CircleShape).clickable(onClick = onClick) else base
        }

    Surface(
        modifier = avatarModifier,
        shape = CircleShape,
        color = if (FinanceUiThemeState.darkMode) Color(0x33243141) else Color(0x33FFFFFF)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto da conta",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = initials,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun isCardPaymentMethod(paymentMethod: String): Boolean {
    if (paymentMethod.isBlank()) return false
    val normalized = Normalizer
        .normalize(paymentMethod.lowercase(Locale.getDefault()), Normalizer.Form.NFD)
        .replace("\\p{M}+".toRegex(), "")
    return normalized.contains("cartao")
}

private fun referenceDateMillisForFuture(item: FinancialTransactionEntity): Long {
    return if (isCardPaymentMethod(item.paymentMethod)) {
        item.cardPaymentDateMillis ?: item.dateMillis
    } else {
        item.dateMillis
    }
}

private fun orderFutureItems(items: List<FinancialTransactionEntity>): List<FinancialTransactionEntity> {
    return items.sortedWith(
        compareBy<FinancialTransactionEntity> { referenceDateMillisForFuture(it) }
            .thenBy { it.dateMillis }
            .thenBy { it.id }
    )
}

private fun isTransactionConcluded(
    item: FinancialTransactionEntity,
    concludedDateByTransactionId: Map<Long, Long>,
    todayStartMillis: Long
): Boolean {
    if (isCardPaymentMethod(item.paymentMethod)) {
        return item.id in concludedDateByTransactionId.keys
    }
    return item.dateMillis <= todayStartMillis || item.id in concludedDateByTransactionId.keys
}

private fun isFutureTransaction(
    item: FinancialTransactionEntity,
    concludedDateByTransactionId: Map<Long, Long>,
    todayStartMillis: Long
): Boolean {
    if (item.id in concludedDateByTransactionId.keys) return false
    return if (isCardPaymentMethod(item.paymentMethod)) {
        true
    } else {
        item.dateMillis > todayStartMillis
    }
}

private fun shouldAppearInHistory(
    item: FinancialTransactionEntity,
    concludedDateByTransactionId: Map<Long, Long>,
    todayStartMillis: Long
): Boolean {
    return !isFutureTransaction(
        item = item,
        concludedDateByTransactionId = concludedDateByTransactionId,
        todayStartMillis = todayStartMillis
    )
}

private fun resolvedConclusionDateMillis(
    item: FinancialTransactionEntity,
    concludedDateByTransactionId: Map<Long, Long>,
    todayStartMillis: Long
): Long? {
    if (isCardPaymentMethod(item.paymentMethod)) {
        return concludedDateByTransactionId[item.id]
    }
    return concludedDateByTransactionId[item.id] ?: item.dateMillis.takeIf { it <= todayStartMillis }
}

private fun clampDayOfMonth(date: LocalDate, day: Int): LocalDate {
    val safeDay = day.coerceAtLeast(1).coerceAtMost(date.lengthOfMonth())
    return date.withDayOfMonth(safeDay)
}

private fun computeCardPaymentDate(
    launchDate: LocalDate,
    closingDay: Int,
    paymentDay: Int
): LocalDate {
    val closeDate = clampDayOfMonth(launchDate, closingDay)
    val currentMonthPaymentDate = clampDayOfMonth(launchDate, paymentDay)
    return if (!launchDate.isAfter(closeDate)) {
        if (!currentMonthPaymentDate.isBefore(launchDate)) {
            currentMonthPaymentDate
        } else {
            clampDayOfMonth(launchDate.plusMonths(1), paymentDay)
        }
    } else {
        clampDayOfMonth(launchDate.plusMonths(1), paymentDay)
    }
}

private fun displayAccountName(raw: String): String {
    return if (raw.equals("Conta Principal", ignoreCase = true)) "Cadastro" else raw
}

private fun parseReminderTimeOrNull(raw: String): DailyReminderTime? {
    val digits = raw.filter(Char::isDigit)
    if (digits.length != 4) return null
    val hour = digits.take(2).toIntOrNull() ?: return null
    val minute = digits.drop(2).toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return DailyReminderTime(hour = hour, minute = minute)
}

private fun loadPersistedList(
    prefs: android.content.SharedPreferences,
    key: String,
    defaults: List<String>
): List<String> {
    val fromSet = runCatching {
        prefs.getStringSet(key, null)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
    }.getOrNull()
    if (!fromSet.isNullOrEmpty()) return fromSet.sorted()

    val raw = prefs.getString(key, null)?.trim().orEmpty()
    if (raw.isBlank()) return defaults
    return raw
        .split(LIST_SEP)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .ifEmpty { defaults }
}

private fun decodeStoredList(raw: String, defaults: List<String>): List<String> {
    if (raw.isBlank()) {
        return defaults
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase(Locale.getDefault()) }
    }
    return raw.split(LIST_SEP)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.lowercase(Locale.getDefault()) }
        .ifEmpty { defaults }
}

private fun encodeStoredList(values: List<String>): String {
    return values
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sortedBy { it.lowercase(Locale.getDefault()) }
        .joinToString(LIST_SEP)
}

private fun loadPersistedListWithLegacyFallback(
    prefs: android.content.SharedPreferences,
    scopedKey: String,
    legacyKey: String,
    defaults: List<String>,
    includeLegacy: Boolean
): List<String> {
    val scoped = loadPersistedList(prefs, scopedKey, emptyList())
    val legacy = if (includeLegacy) loadPersistedList(prefs, legacyKey, emptyList()) else emptyList()
    return (scoped + legacy + defaults)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun savePersistedList(
    prefs: android.content.SharedPreferences,
    key: String,
    values: List<String>
) {
    val cleaned = values.map { it.trim() }.filter { it.isNotBlank() }.toSet()
    prefs.edit()
        .putStringSet(key, cleaned)
        .putString(key, cleaned.joinToString(LIST_SEP))
        .commit()
}

private fun loadPersistedLongList(
    prefs: android.content.SharedPreferences,
    key: String
): List<Long> {
    val raw = prefs.getString(key, null)?.trim().orEmpty()
    if (raw.isBlank()) return emptyList()
    return raw.split(LIST_SEP)
        .mapNotNull { it.trim().toLongOrNull() }
        .distinct()
}

private fun savePersistedLongList(
    prefs: android.content.SharedPreferences,
    key: String,
    values: List<Long>
) {
    val cleaned = values.distinct()
    prefs.edit()
        .putString(key, cleaned.joinToString(LIST_SEP))
        .commit()
}

private fun loadPersistedLongMap(
    prefs: android.content.SharedPreferences,
    key: String
): Map<Long, Long> {
    val raw = prefs.getString(key, null)?.trim().orEmpty()
    if (raw.isBlank()) return emptyMap()
    return raw
        .split(LIST_SEP)
        .mapNotNull { entry ->
            val parts = entry.split("::", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val id = parts[0].trim().toLongOrNull()
            val millis = parts[1].trim().toLongOrNull()
            if (id == null || millis == null) null else id to millis
        }
        .toMap()
}

private fun savePersistedLongMap(
    prefs: android.content.SharedPreferences,
    key: String,
    values: Map<Long, Long>
) {
    val raw = values.entries
        .sortedBy { it.key }
        .joinToString(LIST_SEP) { "${it.key}::${it.value}" }
    prefs.edit().putString(key, raw).commit()
}

private fun loadPersistedOrderedList(
    prefs: android.content.SharedPreferences,
    key: String
): List<String> {
    val raw = prefs.getString(key, null)?.trim().orEmpty()
    if (raw.isBlank()) return emptyList()
    return raw.split(LIST_SEP).map { it.trim() }.filter { it.isNotBlank() }
}

private fun savePersistedOrderedList(
    prefs: android.content.SharedPreferences,
    key: String,
    values: List<String>
) {
    prefs.edit().putString(key, values.joinToString(LIST_SEP)).commit()
}

private fun loadPersistedDoubleMap(
    prefs: android.content.SharedPreferences,
    key: String
): Map<String, Double> {
    val raw = prefs.getString(key, null)?.trim().orEmpty()
    return decodeStoredDoubleMap(raw)
}

private fun decodeStoredDoubleMap(raw: String): Map<String, Double> {
    if (raw.isBlank()) return emptyMap()
    return raw
        .split(LIST_SEP)
        .mapNotNull { entry ->
            val parts = entry.split("::", limit = 2)
            if (parts.size != 2) return@mapNotNull null
            val category = parts[0].trim()
            val value = parts[1].trim().toDoubleOrNull()
            if (category.isBlank() || value == null || value <= 0.0) null else category to value
        }
        .toMap()
}

private fun loadPersistedDoubleMapWithLegacyFallback(
    prefs: android.content.SharedPreferences,
    scopedKey: String,
    legacyKey: String,
    includeLegacy: Boolean
): Map<String, Double> {
    val legacy = if (includeLegacy) loadPersistedDoubleMap(prefs, legacyKey) else emptyMap()
    return legacy + loadPersistedDoubleMap(prefs, scopedKey)
}

private fun savePersistedDoubleMap(
    prefs: android.content.SharedPreferences,
    key: String,
    values: Map<String, Double>
) {
    val raw = values
        .filter { it.key.isNotBlank() && it.value > 0.0 }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .entries
        .joinToString(LIST_SEP) { "${it.key}::${it.value}" }
    prefs.edit().putString(key, raw).commit()
}


private fun loadPersistedCardPaymentConfigMap(
    prefs: android.content.SharedPreferences,
    key: String
): Map<String, CardPaymentConfig> {
    val raw = prefs.getString(key, null)?.trim().orEmpty()
    return decodeStoredCardPaymentConfigMap(raw)
}

private fun decodeStoredCardPaymentConfigMap(raw: String): Map<String, CardPaymentConfig> {
    if (raw.isBlank()) return emptyMap()
    return raw
        .split(LIST_SEP)
        .mapNotNull { entry ->
            val parts = entry.split("::", limit = 3)
            if (parts.size != 3) return@mapNotNull null
            val method = parts[0].trim()
            val closingDay = parts[1].trim().toIntOrNull()
            val paymentDay = parts[2].trim().toIntOrNull()
            if (method.isBlank() || closingDay !in 1..31 || paymentDay !in 1..31) null
            else method to CardPaymentConfig(closingDay!!, paymentDay!!)
        }
        .toMap()
}

private fun loadPersistedCardPaymentConfigMapWithLegacyFallback(
    prefs: android.content.SharedPreferences,
    scopedKey: String,
    legacyKey: String,
    includeLegacy: Boolean
): Map<String, CardPaymentConfig> {
    val legacy = if (includeLegacy) loadPersistedCardPaymentConfigMap(prefs, legacyKey) else emptyMap()
    return legacy + loadPersistedCardPaymentConfigMap(prefs, scopedKey)
}

private fun hasPersistedListData(
    prefs: android.content.SharedPreferences,
    key: String
): Boolean {
    val values = prefs.getStringSet(key, null)
    if (!values.isNullOrEmpty() && values.any { it.isNotBlank() }) return true
    return prefs.getString(key, null)?.any { !it.isWhitespace() } == true
}

private fun hasPersistedMapData(
    prefs: android.content.SharedPreferences,
    key: String
): Boolean {
    return prefs.getString(key, null)?.any { !it.isWhitespace() } == true
}

private fun savePersistedCardPaymentConfigMap(
    prefs: android.content.SharedPreferences,
    key: String,
    values: Map<String, CardPaymentConfig>
) {
    val raw = encodeStoredCardPaymentConfigMap(values)
    prefs.edit().putString(key, raw).commit()
}

private fun encodeStoredDoubleMap(values: Map<String, Double>): String {
    return values
        .filter { it.key.isNotBlank() && it.value > 0.0 }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .entries
        .joinToString(LIST_SEP) { "${it.key}::${it.value}" }
}

private fun encodeStoredCardPaymentConfigMap(values: Map<String, CardPaymentConfig>): String {
    val raw = values
        .filter { it.key.isNotBlank() && it.value.closingDay in 1..31 && it.value.paymentDay in 1..31 }
        .toSortedMap(String.CASE_INSENSITIVE_ORDER)
        .entries
        .joinToString(LIST_SEP) { "${it.key}::${it.value.closingDay}::${it.value.paymentDay}" }
    return raw
}
private enum class HistoryTypeFilter {
    TODOS,
    RECEITAS,
    DESPESAS
}

private enum class TelaAtiva {
    LANCAMENTOS,
    EXTRATO,
    QUADRO
}

@Composable
fun FinanceApp(
    viewModel: FinanceViewModel,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit = {}
) {
    SideEffect {
        FinanceUiThemeState.darkMode = isDarkTheme
    }
    val context = LocalContext.current
    val appVersionShort = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("1.6")
    }
    val prefs = remember { context.getSharedPreferences("financeiro_prefs", Context.MODE_PRIVATE) }
    val allItems by viewModel.transactions.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val activePerson by viewModel.activePerson.collectAsState()
    val activeAccountSettings by viewModel.accountSettings.collectAsState()
    val activeAccountScopedId = activePerson?.id

    val defaultExpenseCategories = listOf("Alimentacao", "Contas", "Lazer", "Saude", "Transporte")
    val defaultIncomeCategories = listOf("Salario", "Freelance", "Investimentos", "Vendas", "Outras Receitas")
    val defaultPaymentMethods = listOf("Dinheiro", "Pix", "Cartao", "Fiado")
    val expenseCategories = remember(activeAccountScopedId) { mutableStateListOf<String>() }
    val incomeCategories = remember(activeAccountScopedId) { mutableStateListOf<String>() }
    val paymentMethods = remember(activeAccountScopedId) { mutableStateListOf<String>() }
    val paymentMethodCardConfigs = remember(activeAccountScopedId) { androidx.compose.runtime.mutableStateMapOf<String, CardPaymentConfig>() }
    val expenseCategoryLimits = remember(activeAccountScopedId) { androidx.compose.runtime.mutableStateMapOf<String, Double>() }
    var accountSettingsHydrated by remember(activeAccountScopedId) { mutableStateOf(false) }
    val defaultScreenOrder = listOf(TelaAtiva.EXTRATO, TelaAtiva.LANCAMENTOS, TelaAtiva.QUADRO)
    val persistedOrderNames = remember { loadPersistedOrderedList(prefs, "screen_order") }
    val initialScreenOrder = remember {
        val mapped = persistedOrderNames
            .mapNotNull { name -> runCatching { TelaAtiva.valueOf(name) }.getOrNull() }
            .distinct()
            .toMutableList()
        defaultScreenOrder.forEach { if (it !in mapped) mapped.add(it) }
        mapped.toList()
    }
    val screenOrder = remember {
        mutableStateListOf<TelaAtiva>().apply { addAll(initialScreenOrder) }
    }

    fun persistAccountSettingsIfChanged(
        expenseValues: List<String> = expenseCategories.toList(),
        incomeValues: List<String> = incomeCategories.toList(),
        paymentValues: List<String> = paymentMethods.toList(),
        cardConfigValues: Map<String, CardPaymentConfig> = paymentMethodCardConfigs.toMap().filterKeys { it in paymentValues },
        limitValues: Map<String, Double> = expenseCategoryLimits.toMap().filterKeys { it in expenseValues }
    ) {
        val accountId = activeAccountScopedId ?: return
        if (!accountSettingsHydrated) return
        val currentAccountSettings = activeAccountSettings
        if (currentAccountSettings == null || currentAccountSettings.accountId != accountId) return
        val encodedExpense = encodeStoredList(expenseValues)
        val encodedIncome = encodeStoredList(incomeValues)
        val encodedPayment = encodeStoredList(paymentValues)
        val encodedCardConfigs = encodeStoredCardPaymentConfigMap(cardConfigValues)
        val encodedLimits = encodeStoredDoubleMap(limitValues)
        if (
            currentAccountSettings.expenseCategories == encodedExpense &&
            currentAccountSettings.incomeCategories == encodedIncome &&
            currentAccountSettings.paymentMethods == encodedPayment &&
            currentAccountSettings.paymentMethodCardConfigs == encodedCardConfigs &&
            currentAccountSettings.expenseCategoryLimits == encodedLimits
        ) {
            return
        }
        viewModel.saveAccountSettings(
            accountId = accountId,
            expenseCategories = expenseValues,
            incomeCategories = incomeValues,
            paymentMethods = paymentValues,
            paymentMethodCardConfigsRaw = encodedCardConfigs,
            expenseCategoryLimitsRaw = encodedLimits
        )
    }

    LaunchedEffect(expenseCategories, accountSettingsHydrated, activeAccountScopedId) {
        if (!accountSettingsHydrated || activeAccountScopedId == null) return@LaunchedEffect
        snapshotFlow { expenseCategories.toList() }
            .distinctUntilChanged()
            .collect { values ->
                persistAccountSettingsIfChanged(expenseValues = values)
            }
    }
    LaunchedEffect(incomeCategories, accountSettingsHydrated, activeAccountScopedId) {
        if (!accountSettingsHydrated || activeAccountScopedId == null) return@LaunchedEffect
        snapshotFlow { incomeCategories.toList() }
            .distinctUntilChanged()
            .collect { values ->
                persistAccountSettingsIfChanged(incomeValues = values)
            }
    }
    LaunchedEffect(paymentMethods, accountSettingsHydrated, activeAccountScopedId) {
        if (!accountSettingsHydrated || activeAccountScopedId == null) return@LaunchedEffect
        snapshotFlow { paymentMethods.toList() }
            .distinctUntilChanged()
            .collect { values ->
                persistAccountSettingsIfChanged(paymentValues = values)
            }
    }
    LaunchedEffect(paymentMethodCardConfigs, paymentMethods, accountSettingsHydrated, activeAccountScopedId) {
        if (!accountSettingsHydrated || activeAccountScopedId == null) return@LaunchedEffect
        snapshotFlow {
            paymentMethodCardConfigs
                .toMap()
                .filterKeys { it in paymentMethods }
        }
            .distinctUntilChanged()
            .collect { cfgs ->
            persistAccountSettingsIfChanged(cardConfigValues = cfgs)
        }
    }
    LaunchedEffect(expenseCategoryLimits, expenseCategories, accountSettingsHydrated, activeAccountScopedId) {
        if (!accountSettingsHydrated || activeAccountScopedId == null) return@LaunchedEffect
        snapshotFlow {
            expenseCategoryLimits
                .toMap()
                .filterKeys { it in expenseCategories }
        }
            .distinctUntilChanged()
            .collect { limits ->
            persistAccountSettingsIfChanged(limitValues = limits)
        }
    }
    LaunchedEffect(
        activeAccountScopedId,
        activeAccountSettings?.accountId,
        activeAccountSettings?.expenseCategories,
        activeAccountSettings?.incomeCategories,
        activeAccountSettings?.paymentMethods,
        activeAccountSettings?.paymentMethodCardConfigs,
        activeAccountSettings?.expenseCategoryLimits
    ) {
        accountSettingsHydrated = false
        if (activeAccountScopedId == null) return@LaunchedEffect
        val currentAccountSettings = activeAccountSettings ?: return@LaunchedEffect
        if (currentAccountSettings.accountId != activeAccountScopedId) return@LaunchedEffect

        val targetExpenseCategories =
            decodeStoredList(currentAccountSettings.expenseCategories, defaultExpenseCategories)
        val targetIncomeCategories =
            decodeStoredList(currentAccountSettings.incomeCategories, defaultIncomeCategories)
        val targetPaymentMethods =
            decodeStoredList(currentAccountSettings.paymentMethods, defaultPaymentMethods)
        val targetPaymentMethodCardConfigs =
            decodeStoredCardPaymentConfigMap(currentAccountSettings.paymentMethodCardConfigs).toMutableMap().apply {
                if (keys.none(::isCardPaymentMethod)) {
                    val defaultCardMethod = targetPaymentMethods.firstOrNull(::isCardPaymentMethod)
                    if (!defaultCardMethod.isNullOrBlank()) {
                        this[defaultCardMethod] = CardPaymentConfig(25, 5)
                    }
                }
            }
        val targetExpenseCategoryLimits =
            decodeStoredDoubleMap(currentAccountSettings.expenseCategoryLimits)

        if (expenseCategories.toList() != targetExpenseCategories) {
            expenseCategories.clear()
            expenseCategories.addAll(targetExpenseCategories)
        }
        if (incomeCategories.toList() != targetIncomeCategories) {
            incomeCategories.clear()
            incomeCategories.addAll(targetIncomeCategories)
        }
        if (paymentMethods.toList() != targetPaymentMethods) {
            paymentMethods.clear()
            paymentMethods.addAll(targetPaymentMethods)
        }
        if (paymentMethodCardConfigs.toMap() != targetPaymentMethodCardConfigs) {
            paymentMethodCardConfigs.clear()
            paymentMethodCardConfigs.putAll(targetPaymentMethodCardConfigs)
        }
        if (expenseCategoryLimits.toMap() != targetExpenseCategoryLimits) {
            expenseCategoryLimits.clear()
            expenseCategoryLimits.putAll(targetExpenseCategoryLimits)
        }
        accountSettingsHydrated = true
    }
    LaunchedEffect(screenOrder) {
        snapshotFlow { screenOrder.toList() }
            .collect { order ->
                savePersistedOrderedList(
                    prefs,
                    "screen_order",
                    order.map { it.name }
                )
            }
    }
    var editingItem by remember { mutableStateOf<FinancialTransactionEntity?>(null) }
    var startDateText by remember { mutableStateOf("") }
    var endDateText by remember { mutableStateOf("") }
    var historyTypeFilter by remember { mutableStateOf(HistoryTypeFilter.TODOS) }
    var historyCategoryFilter by remember { mutableStateOf("") }
    var historyPaymentFilter by remember { mutableStateOf("") }
    var futureStartDateText by remember { mutableStateOf("") }
    var futureEndDateText by remember { mutableStateOf("") }
    var futureTypeFilter by remember { mutableStateOf(HistoryTypeFilter.TODOS) }
    var futureCategoryFilter by remember { mutableStateOf("") }
    var futurePaymentFilter by remember { mutableStateOf("") }
    val futureSelectedIds = remember { mutableStateListOf<Long>() }
    var formResetTrigger by remember { mutableStateOf(0) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showPersonDialog by remember { mutableStateOf(false) }
    var checkingForAppUpdate by remember { mutableStateOf(false) }
    var pendingAppUpdate by remember { mutableStateOf<AppUpdatePrompt?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<FinancialTransactionEntity?>(null) }
    var pendingClearAllFromActiveAccount by remember { mutableStateOf(false) }
    var monthlyLimitAlertMessage by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationScheduler.isDailyReminderEnabled(context))
    }
    var notificationTime by remember {
        mutableStateOf(NotificationScheduler.getDailyReminderTime(context))
    }
    var activeAccountPhotoUri by remember(activePerson?.id) {
        mutableStateOf(loadAccountPhotoUri(prefs, activePerson?.id))
    }
    var showAccountPhotoOptions by remember { mutableStateOf(false) }
    var telaAtiva by remember(screenOrder) { mutableStateOf(screenOrder.firstOrNull() ?: TelaAtiva.EXTRATO) }
    val startDateMillis = parseDateStartMillisOrNull(startDateText)
    val endDateMillis = parseDateEndMillisOrNull(endDateText)
    val futureStartDateMillis = parseDateStartMillisOrNull(futureStartDateText)
    val futureEndDateMillis = parseDateEndMillisOrNull(futureEndDateText)
    val todayStartMillis = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val concludedDatesPrefKey = "concluded_dates_${activePerson?.id ?: 0L}"
    val legacyPaidIdsPrefKey = "paid_transaction_ids_${activePerson?.id ?: 0L}"
    val concludedDateByTransactionId = remember(activePerson?.id) {
        androidx.compose.runtime.mutableStateMapOf<Long, Long>().apply {
            putAll(loadPersistedLongMap(prefs, concludedDatesPrefKey))
        }
    }

    LaunchedEffect(concludedDateByTransactionId, concludedDatesPrefKey) {
        snapshotFlow { concludedDateByTransactionId.toMap() }
            .collect { savePersistedLongMap(prefs, concludedDatesPrefKey, it) }
    }
    LaunchedEffect(notificationsEnabled, notificationTime) {
        NotificationScheduler.updateDailyReminderSettings(context, notificationsEnabled, notificationTime)
    }
    LaunchedEffect(activePerson?.id) {
        activeAccountPhotoUri = loadAccountPhotoUri(prefs, activePerson?.id)
    }
    LaunchedEffect(activePerson?.id) {
        startDateText = ""
        endDateText = ""
        historyTypeFilter = HistoryTypeFilter.TODOS
        historyCategoryFilter = ""
        historyPaymentFilter = ""
        futureStartDateText = ""
        futureEndDateText = ""
        futureTypeFilter = HistoryTypeFilter.TODOS
        futureCategoryFilter = ""
        futurePaymentFilter = ""
        futureSelectedIds.clear()
        editingItem = null
    }
    val activeAccountGalleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val accountId = activePerson?.id
        if (accountId == null || uri == null) return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
        val uriString = uri.toString()
        saveAccountPhotoUri(prefs, accountId, uriString)
        activeAccountPhotoUri = uriString
    }
    val activeAccountCameraPicker = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        val accountId = activePerson?.id
        if (accountId == null || bitmap == null) return@rememberLauncherForActivityResult
        val savedUri = saveAccountPhotoBitmap(context, accountId, bitmap) ?: return@rememberLauncherForActivityResult
        saveAccountPhotoUri(prefs, accountId, savedUri)
        activeAccountPhotoUri = savedUri
    }
    LaunchedEffect(allItems, todayStartMillis) {
        val legacyPaidIds = loadPersistedLongList(prefs, legacyPaidIdsPrefKey).toSet()
        if (legacyPaidIds.isNotEmpty()) {
            legacyPaidIds.forEach { id ->
                if (id !in concludedDateByTransactionId.keys) {
                    val launchDate = allItems.firstOrNull { it.id == id }?.dateMillis ?: todayStartMillis
                    concludedDateByTransactionId[id] = launchDate
                }
            }
            prefs.edit().remove(legacyPaidIdsPrefKey).apply()
        }
    }

    val visibleItems = allItems
        .filter { item ->
            shouldAppearInHistory(
                item = item,
                concludedDateByTransactionId = concludedDateByTransactionId,
                todayStartMillis = todayStartMillis
            )
        }
        .filter { item ->
            val itemReferenceDate = referenceDateMillisForFuture(item)
            (startDateMillis == null || itemReferenceDate >= startDateMillis) &&
                (endDateMillis == null || itemReferenceDate <= endDateMillis)
        }
        .filter { item ->
            val typeOk = when (historyTypeFilter) {
                HistoryTypeFilter.TODOS -> true
                HistoryTypeFilter.RECEITAS -> item.type == TransactionType.RECEITA
                HistoryTypeFilter.DESPESAS -> item.type == TransactionType.DESPESA
            }
            val categoryOk =
                historyCategoryFilter.isBlank() || item.category.contains(historyCategoryFilter, true)
            val paymentOk =
                historyPaymentFilter.isBlank() || item.paymentMethod.contains(historyPaymentFilter, true)
            typeOk && categoryOk && paymentOk
        }
        .sortedByDescending { referenceDateMillisForFuture(it) }

    val totalIncome = visibleItems.filter { it.type == TransactionType.RECEITA }.sumOf { it.amount }
    val totalExpense = visibleItems.filter { it.type == TransactionType.DESPESA }.sumOf { it.amount }
    val totalBalance = totalIncome - totalExpense
    val totalAllIncome = allItems.filter { it.type == TransactionType.RECEITA }.sumOf { it.amount }
    val totalAllExpense = allItems.filter { it.type == TransactionType.DESPESA }.sumOf { it.amount }
    val totalAllBalance = totalAllIncome - totalAllExpense
    val futurePendingItems = allItems
        .filter { item ->
            !shouldAppearInHistory(
                item = item,
                concludedDateByTransactionId = concludedDateByTransactionId,
                todayStartMillis = todayStartMillis
            )
        }
    val futureVisibleItems = futurePendingItems
        .filter { item ->
            val itemReferenceDate = referenceDateMillisForFuture(item)
            (futureStartDateMillis == null || itemReferenceDate >= futureStartDateMillis) &&
                (futureEndDateMillis == null || itemReferenceDate <= futureEndDateMillis)
        }
        .filter { item ->
            val typeOk = when (futureTypeFilter) {
                HistoryTypeFilter.TODOS -> true
                HistoryTypeFilter.RECEITAS -> item.type == TransactionType.RECEITA
                HistoryTypeFilter.DESPESAS -> item.type == TransactionType.DESPESA
            }
            val categoryOk =
                futureCategoryFilter.isBlank() || item.category.contains(futureCategoryFilter, true)
            val paymentOk =
                futurePaymentFilter.isBlank() || item.paymentMethod.contains(futurePaymentFilter, true)
            typeOk && categoryOk && paymentOk
        }
        .let { orderFutureItems(it) }
    LaunchedEffect(futureVisibleItems) {
        val visibleIds = futureVisibleItems.map { it.id }.toSet()
        futureSelectedIds.removeAll { it !in visibleIds }
    }

    val periodIncomeByCategory = visibleItems
        .filter { it.type == TransactionType.RECEITA }
        .groupBy { it.category.ifBlank { "Sem categoria" } }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    val periodExpenseByCategory = visibleItems
        .filter { it.type == TransactionType.DESPESA }
        .groupBy { it.category.ifBlank { "Sem categoria" } }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    Scaffold(
        containerColor = bgGray,
        bottomBar = {
            DualScreenBottomBar(
                telaAtiva = telaAtiva,
                screenOrder = screenOrder,
                onTelaChange = { telaAtiva = it }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(telaAtiva, screenOrder) {
                    var totalDrag = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { totalDrag = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            totalDrag += dragAmount
                        },
                        onDragEnd = {
                            val threshold = 90f
                            val currentIndex = screenOrder.indexOf(telaAtiva)
                            if (abs(totalDrag) >= threshold && currentIndex >= 0) {
                                if (totalDrag < 0 && currentIndex < screenOrder.lastIndex) {
                                    telaAtiva = screenOrder[currentIndex + 1]
                                } else if (totalDrag > 0 && currentIndex > 0) {
                                    telaAtiva = screenOrder[currentIndex - 1]
                                }
                            }
                            totalDrag = 0f
                        },
                        onDragCancel = { totalDrag = 0f }
                    )
                },
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HeaderBlock(
                    activeAccountName = displayAccountName(activePerson?.name.orEmpty()),
                    activeAccountPhotoUri = activeAccountPhotoUri,
                    onMenuClick = { showPersonDialog = true },
                    onPhotoClick = {
                        if (activePerson?.id == null) {
                            showPersonDialog = true
                        } else {
                            showAccountPhotoOptions = true
                        }
                    }
                )
            }
            if (telaAtiva == TelaAtiva.LANCAMENTOS) {
                item {
                    TransactionFormCard(
                        initialItem = editingItem,
                        resetTrigger = formResetTrigger,
                        expenseCategories = expenseCategories,
                        incomeCategories = incomeCategories,
                        paymentMethods = paymentMethods,
                        paymentMethodCardConfigs = paymentMethodCardConfigs,
                        expenseCategoryLimits = expenseCategoryLimits,
                        onCancelEdit = { editingItem = null },
                        onSave = { id, title, amount, type, category, payment, installments, cardPaymentDateMillis, notes, dateMillis ->
                            val categoryMonthlyLimit =
                                if (type == TransactionType.DESPESA) expenseCategoryLimits[category.trim()] else null
                            viewModel.saveTransaction(
                                id,
                                title,
                                amount,
                                type,
                                category,
                                payment,
                                installments,
                                cardPaymentDateMillis,
                                notes,
                                dateMillis,
                                categoryMonthlyLimit
                            ) { warning ->
                                if (!warning.isNullOrBlank()) {
                                    monthlyLimitAlertMessage = warning
                                }
                            }
                            Toast.makeText(
                                context,
                                "Lançamento salvo com sucesso.",
                                Toast.LENGTH_SHORT
                            ).show()
                            editingItem = null
                            startDateText = ""
                            endDateText = ""
                            formResetTrigger++
                            telaAtiva = TelaAtiva.EXTRATO
                        }
                    )
                }
            }
            if (telaAtiva == TelaAtiva.EXTRATO) {
                item {
                    CategoryTotalsChartCard(
                        title = "RECEITAS DO PERIODO POR CATEGORIA",
                        entries = periodIncomeByCategory,
                        totalAmount = periodIncomeByCategory.sumOf { it.second },
                        barColor = incomeGreen,
                        selectedCategory = if (historyTypeFilter == HistoryTypeFilter.RECEITAS) historyCategoryFilter else "",
                        onCategoryClick = { category ->
                            historyTypeFilter = HistoryTypeFilter.RECEITAS
                            historyCategoryFilter = category
                            historyPaymentFilter = ""
                        }
                    )
                }
                item {
                    CategoryTotalsChartCard(
                        title = "DESPESAS DO PERIODO POR CATEGORIA",
                        entries = periodExpenseByCategory,
                        totalAmount = periodExpenseByCategory.sumOf { it.second },
                        barColor = expenseRed,
                        selectedCategory = if (historyTypeFilter == HistoryTypeFilter.DESPESAS) historyCategoryFilter else "",
                        onCategoryClick = { category ->
                            historyTypeFilter = HistoryTypeFilter.DESPESAS
                            historyCategoryFilter = category
                            historyPaymentFilter = ""
                        }
                    )
                }
                item {
                    PeriodFilterCard(
                        startDateText = startDateText,
                        onStartDateChange = { startDateText = it },
                        endDateText = endDateText,
                        onEndDateChange = { endDateText = it },
                        selectedType = historyTypeFilter,
                        onTypeChange = { historyTypeFilter = it },
                        selectedCategory = historyCategoryFilter,
                        onCategoryChange = { historyCategoryFilter = it },
                        selectedPayment = historyPaymentFilter,
                        onPaymentChange = { historyPaymentFilter = it },
                        expenseCategories = expenseCategories.sorted(),
                        incomeCategories = incomeCategories.sorted(),
                        paymentOptions = paymentMethods.sorted()
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Extrato", color = headerBlue, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    startDateText = ""
                                    endDateText = ""
                                    historyTypeFilter = HistoryTypeFilter.TODOS
                                    historyCategoryFilter = ""
                                    historyPaymentFilter = ""
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = headerBlue)
                            ) {
                                Text("Limpar filtros", fontWeight = FontWeight.SemiBold)
                            }
                            IconButton(
                                onClick = { pendingClearAllFromActiveAccount = true },
                                colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                    containerColor = appDangerTintBg,
                                    contentColor = expenseRed
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Limpar todos os lancamentos da conta",
                                    tint = expenseRed
                                )
                            }
                        }
                    }
                }
                if (visibleItems.isEmpty()) {
                    item {
                        Text(
                            "Sem lancamentos para o filtro selecionado.",
                            color = appTextSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }
                } else {
                    items(visibleItems) { item ->
                        HistoryCard(
                            item = item,
                            isConcluded = isTransactionConcluded(
                                item = item,
                                concludedDateByTransactionId = concludedDateByTransactionId,
                                todayStartMillis = todayStartMillis
                            ),
                            onEdit = {
                                editingItem = item
                                telaAtiva = TelaAtiva.LANCAMENTOS
                            },
                            onDelete = { pendingDeleteItem = item }
                        )
                    }
                }
            }
            if (telaAtiva == TelaAtiva.QUADRO) {
                item {
                    SummaryCard(
                        balance = totalAllBalance,
                        income = totalAllIncome,
                        expense = totalAllExpense
                    )
                }
                item {
                    PeriodFilterCard(
                        startDateText = futureStartDateText,
                        onStartDateChange = { futureStartDateText = it },
                        endDateText = futureEndDateText,
                        onEndDateChange = { futureEndDateText = it },
                        selectedType = futureTypeFilter,
                        onTypeChange = { futureTypeFilter = it },
                        selectedCategory = futureCategoryFilter,
                        onCategoryChange = { futureCategoryFilter = it },
                        selectedPayment = futurePaymentFilter,
                        onPaymentChange = { futurePaymentFilter = it },
                        expenseCategories = expenseCategories.sorted(),
                        incomeCategories = incomeCategories.sorted(),
                        paymentOptions = paymentMethods.sorted()
                    )
                }
                item {
                    val futureTotalFiltered = futureVisibleItems.sumOf { it.amount }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total filtrado", fontWeight = FontWeight.SemiBold, color = appTextPrimary)
                        Text(
                            String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", futureTotalFiltered),
                            color = headerBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                item {
                    FutureLaunchesCard(
                        items = futureVisibleItems,
                        paidIds = concludedDateByTransactionId.keys,
                        selectedIds = futureSelectedIds.toSet(),
                        onToggleSelected = { itemId, selected ->
                            if (selected) {
                                if (itemId !in futureSelectedIds) futureSelectedIds.add(itemId)
                            } else {
                                futureSelectedIds.remove(itemId)
                            }
                        },
                        onSelectAllVisible = {
                            futureSelectedIds.clear()
                            futureSelectedIds.addAll(futureVisibleItems.map { it.id })
                        },
                        onClearSelection = { futureSelectedIds.clear() },
                        onConfirmSelected = { selectedIds ->
                            val nowMillis = LocalDate.now()
                                .atStartOfDay(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()
                            var concludedCount = 0
                            selectedIds.forEach { selectedId ->
                                if (selectedId !in concludedDateByTransactionId.keys) {
                                    concludedDateByTransactionId[selectedId] = nowMillis
                                    concludedCount++
                                }
                            }
                            futureSelectedIds.removeAll(selectedIds.toSet())
                            if (concludedCount > 0) {
                                Toast.makeText(
                                    context,
                                    "$concludedCount lancamentos concluidos.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        onEdit = { item ->
                            editingItem = item
                            telaAtiva = TelaAtiva.LANCAMENTOS
                        },
                        onDelete = { item -> pendingDeleteItem = item },
                        onConfirmPaid = { item ->
                            if (item.id !in concludedDateByTransactionId.keys) {
                                concludedDateByTransactionId[item.id] = LocalDate.now()
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                            }
                            Toast.makeText(context, "Lancamento confirmado como pago.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
    if (showShareDialog) {
        ShareHistoryDialog(
            onDismiss = { showShareDialog = false },
            onShareCsv = {
                showShareDialog = false
                shareHistory(
                    context = context,
                    account = activePerson,
                    items = allItems,
                    format = ExportFormat.CSV,
                    concludedDateByTransactionId = concludedDateByTransactionId.toMap(),
                    expenseCategoryLimits = expenseCategoryLimits.toMap()
                )
            },
            onShareExcel = {
                showShareDialog = false
                shareHistory(
                    context = context,
                    account = activePerson,
                    items = allItems,
                    format = ExportFormat.EXCEL,
                    concludedDateByTransactionId = concludedDateByTransactionId.toMap(),
                    expenseCategoryLimits = expenseCategoryLimits.toMap()
                )
            }
        )
    }
    if (showAccountPhotoOptions) {
        AlertDialog(
            onDismissRequest = { showAccountPhotoOptions = false },
            containerColor = appSurface,
            title = { Text("Foto da conta") },
            text = {
                Text("Escolha uma foto da galeria ou abra a câmera para atualizar a conta em uso.")
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            showAccountPhotoOptions = false
                            activeAccountGalleryPicker.launch(arrayOf("image/*"))
                        }
                    ) { Text("Galeria") }
                    TextButton(
                        onClick = {
                            showAccountPhotoOptions = false
                            activeAccountCameraPicker.launch(null)
                        }
                    ) { Text("Câmera") }
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!activeAccountPhotoUri.isNullOrBlank() && activePerson?.id != null) {
                        TextButton(
                            onClick = {
                                saveAccountPhotoUri(prefs, activePerson!!.id, null)
                                activeAccountPhotoUri = null
                                showAccountPhotoOptions = false
                            }
                        ) { Text("Remover", color = expenseRed) }
                    }
                    TextButton(onClick = { showAccountPhotoOptions = false }) { Text("Cancelar") }
                }
            }
        )
    }
    if (showPersonDialog) {
        PersonFormDialog(
            accounts = accounts,
            activeAccountId = activePerson?.id,
            initialPerson = activePerson,
            notificationsEnabled = notificationsEnabled,
            onNotificationsEnabledChange = { notificationsEnabled = it },
            notificationTime = notificationTime,
            onNotificationTimeChange = { notificationTime = it },
            onDismiss = { showPersonDialog = false },
            onLogout = onLogout,
            onSave = { id, name, phone, email, photoUri ->
                viewModel.savePerson(id, name, phone, email) { ok, savedId, message ->
                    if (ok) {
                        if (savedId != null) {
                            saveAccountPhotoUri(prefs, savedId, photoUri)
                            if (savedId == activePerson?.id || savedId == viewModel.activePerson.value?.id) {
                                activeAccountPhotoUri = photoUri
                            }
                        }
                        Toast.makeText(context, "Conta salva com sucesso.", Toast.LENGTH_SHORT).show()
                        showPersonDialog = false
                    } else if (!message.isNullOrBlank()) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onUpdateVersion = {
                if (checkingForAppUpdate) return@PersonFormDialog
                checkingForAppUpdate = true
                viewModel.checkForOptionalUpdate { result ->
                    checkingForAppUpdate = false
                    when (result) {
                        is AppUpdateCheckResult.Available -> {
                            pendingAppUpdate = result.update
                            showPersonDialog = false
                        }
                        is AppUpdateCheckResult.Unavailable -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                        is AppUpdateCheckResult.Error -> {
                            Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            onDeleteAccount = { accountId ->
                viewModel.deleteAccount(accountId) { ok ->
                    Toast.makeText(
                        context,
                        if (ok) "Conta excluida com sucesso." else "Nao foi possivel excluir a conta.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onClearHistory = { accountId ->
                viewModel.clearAccountHistory(accountId) { ok ->
                    if (ok) concludedDateByTransactionId.clear()
                    Toast.makeText(
                        context,
                        if (ok) "Historico da conta limpo com sucesso." else "Nao foi possivel limpar o historico.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onSetActive = { accountId ->
                viewModel.setActiveAccount(accountId)
                activeAccountPhotoUri = loadAccountPhotoUri(prefs, accountId)
                showPersonDialog = false
            },
            onFindByAny = { name, phone, email -> viewModel.findPersonByAny(name, phone, email) },
            resolveAccountPhotoUri = { accountId -> loadAccountPhotoUri(prefs, accountId) },
            onShareAccount = {
                showPersonDialog = false
                showShareDialog = true
            },
            isDarkTheme = isDarkTheme,
            onThemeChange = onThemeChange,
            screenOrder = screenOrder,
            onMoveScreenUp = { screen ->
                val index = screenOrder.indexOf(screen)
                if (index > 0) {
                    screenOrder.removeAt(index)
                    screenOrder.add(index - 1, screen)
                }
            },
            onMoveScreenDown = { screen ->
                val index = screenOrder.indexOf(screen)
                if (index >= 0 && index < screenOrder.lastIndex) {
                    screenOrder.removeAt(index)
                    screenOrder.add(index + 1, screen)
                }
            },
            isCheckingUpdate = checkingForAppUpdate
        )
    }
    pendingAppUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { pendingAppUpdate = null },
            containerColor = appSurface,
            title = { Text(update.title) },
            text = { Text(update.message) },
            dismissButton = {
                TextButton(onClick = { pendingAppUpdate = null }) { Text("Depois") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val opened = openExternalLink(context, update.updateUrl)
                        if (!opened) {
                            val copied = copyTextToClipboard(context, "link_atualizacao", update.updateUrl.trim())
                            Toast.makeText(
                                context,
                                if (copied) {
                                    "Nao foi possivel abrir o link. O endereco foi copiado."
                                } else {
                                    "Nao foi possivel abrir o link de atualizacao."
                                },
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        pendingAppUpdate = null
                    }
                ) { Text("Atualizar agora", color = headerBlue) }
            }
        )
    }
    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            containerColor = appSurface,
            title = { Text("Confirmar exclusao") },
            text = { Text("Deseja excluir este lancamento do historico?") },
            dismissButton = {
                TextButton(onClick = { pendingDeleteItem = null }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        concludedDateByTransactionId.remove(item.id)
                        viewModel.deleteTransaction(item)
                        pendingDeleteItem = null
                    }
                ) { Text("Excluir", color = expenseRed) }
            }
        )
    }
    if (pendingClearAllFromActiveAccount) {
        AlertDialog(
            onDismissRequest = { pendingClearAllFromActiveAccount = false },
            containerColor = appSurface,
            title = { Text("Confirmar limpeza") },
            text = {
                Text(
                    "Deseja apagar todos os lancamentos da conta atual? Essa acao nao pode ser desfeita."
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingClearAllFromActiveAccount = false }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val accountId = activePerson?.id
                        if (accountId != null) {
                            viewModel.clearAccountHistory(accountId) { ok ->
                                if (ok) concludedDateByTransactionId.clear()
                                Toast.makeText(
                                    context,
                                    if (ok) "Historico da conta limpo com sucesso." else "Nao foi possivel limpar o historico.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        pendingClearAllFromActiveAccount = false
                    }
                ) { Text("Apagar tudo", color = expenseRed) }
            }
        )
    }
    monthlyLimitAlertMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { monthlyLimitAlertMessage = null },
            containerColor = appSurface,
            title = { Text("Alerta") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { monthlyLimitAlertMessage = null }) { Text("OK") }
            }
        )
    }
}

@Composable
private fun HeaderBlock(
    activeAccountName: String,
    activeAccountPhotoUri: String?,
    onMenuClick: () -> Unit,
    onPhotoClick: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBlue)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        val compactHeader = maxWidth < 360.dp
        val accountLabel = "Conta: ${activeAccountName.ifBlank { "Sem conta" }}"
        val headerTitleColor = if (FinanceUiThemeState.darkMode) Color(0xFF0F172A) else Color.White
        val accountLabelColor = if (FinanceUiThemeState.darkMode) Color(0xFF1E293B) else Color(0xFFC8C9E8)
        val actionButtonSize = if (compactHeader) 50.dp else 58.dp
        val titleStyle = if (compactHeader) {
            androidx.compose.material3.MaterialTheme.typography.titleMedium
        } else {
            androidx.compose.material3.MaterialTheme.typography.titleLarge
        }

        if (compactHeader) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (FinanceUiThemeState.darkMode) Color(0x33243141) else Color(0x33FFFFFF),
                        modifier = Modifier.size(actionButtonSize)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            IconButton(onClick = onMenuClick) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                            }
                        }
                    }
                    AccountAvatar(
                        accountName = activeAccountName,
                        photoUri = activeAccountPhotoUri,
                        size = actionButtonSize,
                        onClick = onPhotoClick
                    )
                }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Finanças",
                        color = headerTitleColor,
                        fontWeight = FontWeight.ExtraBold,
                        style = titleStyle,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        accountLabel,
                        color = accountLabelColor,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    shape = CircleShape,
                    color = if (FinanceUiThemeState.darkMode) Color(0x33243141) else Color(0x33FFFFFF),
                    modifier = Modifier
                        .size(actionButtonSize)
                        .align(Alignment.TopStart)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 66.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Finanças",
                        color = headerTitleColor,
                        fontWeight = FontWeight.ExtraBold,
                        style = titleStyle,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        accountLabel,
                        color = accountLabelColor,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AccountAvatar(
                    accountName = activeAccountName,
                    photoUri = activeAccountPhotoUri,
                    size = actionButtonSize,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .align(Alignment.TopEnd),
                    onClick = onPhotoClick
                )
            }
        }
    }
}

@Composable
private fun PersonFormDialog(
    accounts: List<PersonEntity>,
    activeAccountId: Long?,
    initialPerson: PersonEntity?,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit,
    notificationTime: DailyReminderTime,
    onNotificationTimeChange: (DailyReminderTime) -> Unit,
    onDismiss: () -> Unit,
    onLogout: () -> Unit,
    onSave: (Long?, String, String, String, String?) -> Unit,
    onUpdateVersion: () -> Unit,
    onDeleteAccount: (Long) -> Unit,
    onClearHistory: (Long) -> Unit,
    onSetActive: (Long) -> Unit,
    onFindByAny: suspend (String, String, String) -> PersonEntity?,
    resolveAccountPhotoUri: (Long?) -> String?,
    onShareAccount: () -> Unit,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    screenOrder: List<TelaAtiva>,
    onMoveScreenUp: (TelaAtiva) -> Unit,
    onMoveScreenDown: (TelaAtiva) -> Unit,
    isCheckingUpdate: Boolean
) {
    val context = LocalContext.current
    val appVersionShort = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrDefault("1.6")
    }
    var personId by remember(initialPerson?.id) { mutableStateOf(initialPerson?.id as Long?) }
    var name by remember(initialPerson?.id) { mutableStateOf(initialPerson?.name.orEmpty()) }
    var phone by remember(initialPerson?.id) { mutableStateOf(initialPerson?.phone.orEmpty()) }
    var email by remember(initialPerson?.id) { mutableStateOf(initialPerson?.email.orEmpty()) }
    var photoUri by remember(initialPerson?.id) { mutableStateOf(resolveAccountPhotoUri(initialPerson?.id)) }
    var error by remember { mutableStateOf<String?>(null) }
    var showAccountFields by remember { mutableStateOf(false) }
    var autoLookupEnabled by remember { mutableStateOf(true) }
    var pendingClearHistoryAccount by remember { mutableStateOf<PersonEntity?>(null) }
    var pendingDeleteAccount by remember { mutableStateOf<PersonEntity?>(null) }
    var notificationTimeInput by remember(notificationTime) {
        mutableStateOf(String.format(Locale.getDefault(), "%02d:%02d", notificationTime.hour, notificationTime.minute))
    }
    var notificationError by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            photoUri = uri.toString()
        }
    }

    androidx.compose.runtime.LaunchedEffect(name, phone, email, personId) {
        if (!autoLookupEnabled) return@LaunchedEffect
        if (personId != null) return@LaunchedEffect
        val cleanName = name.trim()
        val cleanPhone = phone.trim()
        val cleanEmail = email.trim()
        if (cleanName.isBlank() && cleanPhone.isBlank() && cleanEmail.isBlank()) return@LaunchedEffect

        val found = onFindByAny(cleanName, cleanPhone, cleanEmail) ?: return@LaunchedEffect
        if (personId == found.id) return@LaunchedEffect

        personId = found.id
        name = found.name
        phone = found.phone.orEmpty()
        email = found.email.orEmpty()
        photoUri = resolveAccountPhotoUri(found.id)
    }

    androidx.compose.runtime.LaunchedEffect(personId) {
        if (personId != null) {
            photoUri = resolveAccountPhotoUri(personId)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appSurface,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Contas")
                Text(
                    "Versão $appVersionShort",
                    style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                    color = appTextSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Conta em uso: ${
                        displayAccountName(
                            accounts.firstOrNull { it.id == activeAccountId }?.name ?: "Sem conta"
                        )
                    }",
                    color = headerBlue,
                    fontWeight = FontWeight.SemiBold
                )
                accounts.forEach { account ->
                    val isActive = account.id == activeAccountId
                    val canDelete = account.name.equals("Cadastro", ignoreCase = true) ||
                        account.name.equals("Conta Principal", ignoreCase = true)
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = appSurfaceAlt
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    displayAccountName(account.name),
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                )
                                if (isActive) {
                                    Surface(
                                        shape = RoundedCornerShape(99.dp),
                                        color = appSuccessTintBg
                                    ) {
                                        Text(
                                            "Em uso",
                                            color = incomeGreen,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        autoLookupEnabled = true
                                        personId = account.id
                                        name = account.name
                                        phone = account.phone.orEmpty()
                                        email = account.email.orEmpty()
                                        error = null
                                        showAccountFields = true
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = appPrimaryTintBg,
                                        contentColor = headerBlue
                                    )
                                ) {
                                    Text("Editar", fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = { onSetActive(account.id) },
                                    enabled = !isActive,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isActive) appSuccessTintBg else headerBlue,
                                        contentColor = if (isActive) incomeGreen else Color.White,
                                        disabledContainerColor = appSuccessTintBg,
                                        disabledContentColor = incomeGreen
                                    )
                                ) {
                                    Text(if (isActive) "Ativa" else "Acessar", fontWeight = FontWeight.SemiBold)
                                }
                            }
                            if (canDelete) {
                                OutlinedButton(
                                    onClick = { pendingClearHistoryAccount = account },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = headerBlue),
                                    border = BorderStroke(1.dp, Color(0xFFB9C8FF))
                                ) {
                                    Text("Limpar historico", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { pendingDeleteAccount = account },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = expenseRed),
                                    border = BorderStroke(1.dp, Color(0xFFFFC4CB))
                                ) {
                                    Text("Excluir conta", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text("Ações da conta", fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = onShareAccount,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Exportar CSV e Excel", fontWeight = FontWeight.SemiBold)
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = appSurfaceAlt
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Push de lancamentos do dia", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Ative o alerta diario e defina a hora do envio.",
                                    color = appTextSecondary
                                )
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = {
                                    notificationError = null
                                    onNotificationsEnabledChange(it)
                                }
                            )
                        }
                        OutlinedTextField(
                            value = notificationTimeInput,
                            onValueChange = { raw ->
                                val digits = raw.filter(Char::isDigit).take(4)
                                val normalized = when {
                                    digits.length >= 3 -> "${digits.take(2)}:${digits.drop(2)}"
                                    digits.isNotBlank() -> digits
                                    else -> ""
                                }
                                notificationTimeInput = normalized
                                val parsed = parseReminderTimeOrNull(normalized)
                                notificationError = when {
                                    normalized.isBlank() -> "Informe o horário no formato HH:mm."
                                    parsed == null -> "Use o formato HH:mm entre 00:00 e 23:59."
                                    else -> null
                                }
                                if (parsed != null) {
                                    onNotificationTimeChange(parsed)
                                }
                            },
                            label = { Text("Hora do push") },
                            placeholder = { Text("HH:mm") },
                            singleLine = true,
                            enabled = notificationsEnabled,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = financeOutlinedTextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "O envio acontece no horario configurado para lembrar os lancamentos vencendo hoje.",
                            color = appTextSecondary
                        )
                        notificationError?.let { Text(it, color = expenseRed) }
                    }
                }
                HorizontalDivider()
                Text("Dados da conta", fontWeight = FontWeight.SemiBold)
                Button(
                    onClick = {
                        autoLookupEnabled = false
                        personId = null
                        name = ""
                        phone = ""
                        email = ""
                        photoUri = null
                        error = null
                        showAccountFields = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = appPrimaryTintBg,
                        contentColor = headerBlue
                    )
                ) {
                    Text("Nova conta", fontWeight = FontWeight.SemiBold)
                }
                if (showAccountFields) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = appSurfaceAlt
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AccountAvatar(
                                accountName = name.ifBlank { "Conta" },
                                photoUri = photoUri,
                                size = 72.dp,
                                onClick = { photoPicker.launch(arrayOf("image/*")) }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { photoPicker.launch(arrayOf("image/*")) },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Escolher foto", fontWeight = FontWeight.SemiBold)
                                }
                                OutlinedButton(
                                    onClick = { photoUri = null },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = expenseRed),
                                    border = BorderStroke(1.dp, Color(0xFFFFC4CB))
                                ) {
                                    Text("Remover", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome") },
                        singleLine = true,
                        colors = financeOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it.filter { ch -> ch.isDigit() || ch == '+' || ch == '-' || ch == ' ' || ch == '(' || ch == ')' } },
                        label = { Text("Telefone") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = financeOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it.trimStart() },
                        label = { Text("Email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = financeOutlinedTextFieldColors(),
                        modifier = Modifier.fillMaxWidth()
                    )
                    error?.let { Text(it, color = expenseRed) }
                    Button(
                        onClick = {
                            if (name.trim().isBlank()) {
                                error = "Informe o nome."
                            } else {
                                error = null
                                onSave(personId, name.trim(), phone.trim(), email.trim(), photoUri)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = headerBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Salvar cadastro", fontWeight = FontWeight.SemiBold)
                    }
                }
                HorizontalDivider()
                Text("Ordem das telas", fontWeight = FontWeight.SemiBold)
                screenOrder.forEachIndexed { index, screen ->
                    val label = when (screen) {
                        TelaAtiva.LANCAMENTOS -> "Lançamentos"
                        TelaAtiva.EXTRATO -> "Extrato"
                        TelaAtiva.QUADRO -> "Futuro"
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = appSurfaceAlt
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("${index + 1}. $label", fontWeight = FontWeight.Medium)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { onMoveScreenUp(screen) },
                                    enabled = index > 0,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) { Text("Mover para cima") }
                                OutlinedButton(
                                    onClick = { onMoveScreenDown(screen) },
                                    enabled = index < screenOrder.lastIndex,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                                ) { Text("Mover para baixo") }
                            }
                        }
                    }
                }
                HorizontalDivider()
                Text("Tema do app", fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ToggleTypeButton(
                        label = "Claro",
                        selected = !isDarkTheme,
                        selectedColor = headerBlue,
                        onClick = { onThemeChange(false) },
                        modifier = Modifier.weight(1f)
                    )
                    ToggleTypeButton(
                        label = "Escuro",
                        selected = isDarkTheme,
                        selectedColor = Color(0xFF1F2937),
                        onClick = { onThemeChange(true) },
                        modifier = Modifier.weight(1f)
                    )
                }
                HorizontalDivider()
                Text("Atualizar aplicativo", fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = onUpdateVersion,
                    enabled = !isCheckingUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (isCheckingUpdate) "Verificando..." else "Atualizar app",
                        fontWeight = FontWeight.SemiBold
                    )
                }
                HorizontalDivider()
                Text("Sessão", fontWeight = FontWeight.SemiBold)
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onLogout()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = expenseRed),
                    border = BorderStroke(1.dp, Color(0xFFFFC4CB))
                ) {
                    Text("Sair do app", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        confirmButton = {}
    )

    pendingClearHistoryAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingClearHistoryAccount = null },
            containerColor = appSurface,
            title = { Text("Confirmar limpeza") },
            text = {
                Text(
                    "Deseja limpar todo o historico da conta \"${displayAccountName(account.name)}\"? Essa acao nao pode ser desfeita."
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingClearHistoryAccount = null }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory(account.id)
                        pendingClearHistoryAccount = null
                    }
                ) { Text("Limpar", color = expenseRed) }
            }
        )
    }

    pendingDeleteAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { pendingDeleteAccount = null },
            containerColor = appSurface,
            title = { Text("Confirmar exclusao") },
            text = { Text("Deseja excluir a conta \"${displayAccountName(account.name)}\"?") },
            dismissButton = {
                TextButton(onClick = { pendingDeleteAccount = null }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAccount(account.id)
                        pendingDeleteAccount = null
                    }
                ) { Text("Excluir", color = expenseRed) }
            }
        )
    }
}

@Composable
private fun DualScreenBottomBar(
    telaAtiva: TelaAtiva,
    screenOrder: List<TelaAtiva>,
    onTelaChange: (TelaAtiva) -> Unit
) {
    Surface(
        modifier = Modifier
            .navigationBarsPadding()
            .imePadding(),
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        color = appSurface
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            val compactLabels = maxWidth < 380.dp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                screenOrder.forEach { screen ->
                    val label = when (screen) {
                        TelaAtiva.LANCAMENTOS -> if (compactLabels) "Lanç." else "Lançamentos"
                        TelaAtiva.EXTRATO -> "Extrato"
                        TelaAtiva.QUADRO -> "Futuro"
                    }
                    ToggleTypeButton(
                        label = label,
                        selected = telaAtiva == screen,
                        selectedColor = headerBlue,
                        onClick = { onTelaChange(screen) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionFormCard(
    initialItem: FinancialTransactionEntity?,
    resetTrigger: Int,
    expenseCategories: MutableList<String>,
    incomeCategories: MutableList<String>,
    paymentMethods: MutableList<String>,
    paymentMethodCardConfigs: MutableMap<String, CardPaymentConfig>,
    expenseCategoryLimits: MutableMap<String, Double>,
    onCancelEdit: () -> Unit,
    onSave: (
        id: Long?,
        title: String,
        amount: Double,
        type: TransactionType,
        category: String,
        paymentMethod: String,
        installments: Int,
        cardPaymentDateMillis: Long?,
        notes: String,
        dateMillis: Long
    ) -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val descriptionFocusRequester = remember { FocusRequester() }
    val installmentsFocusRequester = remember { FocusRequester() }

    var type by remember(initialItem?.id, resetTrigger) {
        mutableStateOf(initialItem?.type ?: TransactionType.DESPESA)
    }
    var valueText by remember(initialItem?.id, resetTrigger) {
        mutableStateOf(
            initialItem?.amount?.let {
                String.format(Locale.forLanguageTag("pt-BR"), "%.2f", it)
            }.orEmpty()
        )
    }
    var category by remember(initialItem?.id, resetTrigger) {
        mutableStateOf(initialItem?.category.orEmpty())
    }
    var payment by remember(initialItem?.id, resetTrigger) {
        mutableStateOf(initialItem?.paymentMethod.orEmpty())
    }
    var description by remember(initialItem?.id, resetTrigger) {
        mutableStateOf(initialItem?.title.orEmpty())
    }
    var launchDateText by remember(initialItem?.id, resetTrigger) {
        mutableStateOf(
            initialItem?.dateMillis?.let { formatFullDate(it) }
                ?: LocalDate.now().format(fullDateFormat)
        )
    }
    var installmentsText by remember(initialItem?.id, resetTrigger) {
        mutableStateOf((initialItem?.installments ?: 1).toString())
    }
    fun openLaunchDatePicker() {
        val now = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                launchDateText = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                installmentsFocusRequester.requestFocus()
            },
            now.get(Calendar.YEAR),
            now.get(Calendar.MONTH),
            now.get(Calendar.DAY_OF_MONTH)
        ).show()
    }
    val categoryOptions = if (type == TransactionType.RECEITA) {
        incomeCategories.sorted()
    } else {
        expenseCategories.sorted()
    }

    var categoryMenuOpen by remember(initialItem?.id, resetTrigger) { mutableStateOf(false) }
    var paymentMenuOpen by remember(initialItem?.id, resetTrigger) { mutableStateOf(false) }
    var showCategoryDialog by remember(initialItem?.id, resetTrigger) { mutableStateOf(false) }
    var showPaymentDialog by remember(initialItem?.id, resetTrigger) { mutableStateOf(false) }
    var error by remember(initialItem?.id, resetTrigger) { mutableStateOf<String?>(null) }
    var isSubmitting by remember(initialItem?.id, resetTrigger) { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        colors = CardDefaults.cardColors(containerColor = appSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Novo Registro", color = headerBlue, fontWeight = FontWeight.Bold)

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackButtons = maxWidth < 360.dp
                if (stackButtons) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleTypeButton(
                            label = "Despesas",
                            selected = type == TransactionType.DESPESA,
                            selectedColor = expenseRed,
                            onClick = {
                                type = TransactionType.DESPESA
                                if (category !in expenseCategories) category = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ToggleTypeButton(
                            label = "Receitas",
                            selected = type == TransactionType.RECEITA,
                            selectedColor = incomeGreen,
                            onClick = {
                                type = TransactionType.RECEITA
                                if (category !in incomeCategories) category = ""
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ToggleTypeButton(
                            label = "Despesas",
                            selected = type == TransactionType.DESPESA,
                            selectedColor = expenseRed,
                            onClick = {
                                type = TransactionType.DESPESA
                                if (category !in expenseCategories) category = ""
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ToggleTypeButton(
                            label = "Receitas",
                            selected = type == TransactionType.RECEITA,
                            selectedColor = incomeGreen,
                            onClick = {
                                type = TransactionType.RECEITA
                                if (category !in incomeCategories) category = ""
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = valueText,
                onValueChange = { valueText = it.replace(",", ".") },
                label = { Text("Valor") },
                leadingIcon = { Text("$") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { categoryMenuOpen = true }
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = financeOutlinedTextFieldColors()
            )

            DropdownLikeField(
                value = category,
                expanded = categoryMenuOpen,
                onExpandChange = { categoryMenuOpen = it },
                options = categoryOptions,
                includeAddOption = true,
                addOptionText = "+ Gerenciar Categorias",
                placeholder = "Categoria",
                onSelect = {
                    if (it == "__ADD__") {
                        categoryMenuOpen = false
                        showCategoryDialog = true
                    } else {
                        category = it
                        categoryMenuOpen = false
                        paymentMenuOpen = true
                    }
                }
            )

            DropdownLikeField(
                value = payment,
                expanded = paymentMenuOpen,
                onExpandChange = { paymentMenuOpen = it },
                options = paymentMethods,
                includeAddOption = true,
                addOptionText = "+ Gerenciar Pagamentos",
                placeholder = "Pagamento",
                onSelect = {
                    if (it == "__ADD__") {
                        paymentMenuOpen = false
                        showPaymentDialog = true
                    } else {
                        payment = it
                        paymentMenuOpen = false
                        descriptionFocusRequester.requestFocus()
                    }
                }
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descrição") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { openLaunchDatePicker() }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(descriptionFocusRequester),
                shape = RoundedCornerShape(12.dp),
                colors = financeOutlinedTextFieldColors()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { openLaunchDatePicker() }
            ) {
                OutlinedTextField(
                    value = launchDateText,
                    onValueChange = {},
                    label = { Text("Data de vencimento") },
                    singleLine = true,
                    readOnly = true,
                    enabled = false,
                    trailingIcon = {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar data de lancamento")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = financeOutlinedTextFieldColors()
                )
            }

            OutlinedTextField(
                value = installmentsText,
                onValueChange = { installmentsText = it.filter(Char::isDigit) },
                label = { Text("Nr Parcelas") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(installmentsFocusRequester),
                shape = RoundedCornerShape(12.dp),
                colors = financeOutlinedTextFieldColors()
            )
            error?.let { Text(it, color = expenseRed) }

            Button(
                onClick = {
                    if (isSubmitting) return@Button
                    val parsedAmount = valueText.replace(",", ".").toDoubleOrNull()
                    val installments = installmentsText.toIntOrNull() ?: 1
                    val parsedLaunchDate = parseDateOrNull(launchDateText)
                    when {
                        parsedAmount == null || parsedAmount <= 0 -> error = "Valor invalido."
                        parsedLaunchDate == null ->
                            error = "Data de vencimento invalida."
                        installments < 1 -> error = "Parcelas invalidas."
                        else -> {
                            error = null
                            isSubmitting = true
                            val safeDescription = description.ifBlank { "Sem descricao" }
                            val safeCategory = category.ifBlank { "Sem categoria" }
                            val safePayment = payment.ifBlank { "Dinheiro" }
                            val isCardPayment = isCardPaymentMethod(safePayment)
                            val cardConfig = if (isCardPayment) paymentMethodCardConfigs[safePayment] else null
                            if (isCardPayment && cardConfig == null) {
                                error = "Configure fechamento e pagamento na forma de pagamento do cartao."
                                isSubmitting = false
                                return@Button
                            }
                            val launchDateMillis = parsedLaunchDate
                                ?.atStartOfDay(ZoneId.systemDefault())
                                ?.toInstant()
                                ?.toEpochMilli()
                                ?: LocalDate.now().atStartOfDay(ZoneId.systemDefault())
                                    .toInstant().toEpochMilli()
                            val launchDate = parsedLaunchDate ?: LocalDate.now()
                            val cardPaymentMillis = if (isCardPayment) {
                                val resolvedCardDate = computeCardPaymentDate(
                                    launchDate = launchDate,
                                    closingDay = cardConfig?.closingDay ?: 25,
                                    paymentDay = cardConfig?.paymentDay ?: 5
                                )
                                resolvedCardDate
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli()
                            } else null

                            onSave(
                                initialItem?.id,
                                safeDescription,
                                parsedAmount,
                                type,
                                safeCategory,
                                safePayment,
                                installments,
                                cardPaymentMillis,
                                "",
                                launchDateMillis
                            )
                        }
                    }
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = headerBlue,
                    contentColor = Color.White
                ),
                elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(
                    defaultElevation = 2.dp,
                    pressedElevation = 6.dp
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.size(6.dp))
                Text("SALVAR", color = Color.White, fontWeight = FontWeight.Bold)
            }

            if (initialItem != null) {
                TextButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.align(Alignment.End),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = headerBlue)
                ) {
                    Text("Cancelar edicao", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showPaymentDialog) {
        ManagePaymentsDialog(
            paymentMethods = paymentMethods,
            paymentMethodCardConfigs = paymentMethodCardConfigs,
            onDismiss = { showPaymentDialog = false }
        )
    }

    if (showCategoryDialog) {
        ManageCategoriesDialog(
            title = if (type == TransactionType.RECEITA) "Categorias de Receitas" else "Categorias de Despesas",
            categories = if (type == TransactionType.RECEITA) incomeCategories else expenseCategories,
            isExpense = type == TransactionType.DESPESA,
            expenseCategoryLimits = expenseCategoryLimits,
            onDismiss = { showCategoryDialog = false }
        )
    }
}

@Composable
private fun ToggleTypeButton(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
            containerColor = if (selected) selectedColor else appSurfaceAlt,
            contentColor = if (selected) Color.White else appTextPrimary
        ),
        border = if (selected) null else BorderStroke(1.dp, Color(0xFFD8DCE6)),
        elevation = androidx.compose.material3.ButtonDefaults.buttonElevation(
            defaultElevation = if (selected) 2.dp else 0.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun DropdownLikeField(
    value: String,
    placeholder: String,
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    options: List<String>,
    includeAddOption: Boolean,
    addOptionText: String,
    onSelect: (String) -> Unit
) {
    val optionsScrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpandChange(true) }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            label = { Text(placeholder) },
            trailingIcon = {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir")
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = financeOutlinedTextFieldColors()
        )
        androidx.compose.material3.DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandChange(false) },
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(optionsScrollState)
            ) {
                options.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelect(option) }
                    )
                }
            }
            if (includeAddOption) {
                HorizontalDivider()
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(addOptionText, color = headerBlue, fontWeight = FontWeight.SemiBold) },
                    onClick = { onSelect("__ADD__") }
                )
            }
        }
    }
}

@Composable
private fun ManagePaymentsDialog(
    paymentMethods: MutableList<String>,
    paymentMethodCardConfigs: MutableMap<String, CardPaymentConfig>,
    onDismiss: () -> Unit
) {
    var newText by remember { mutableStateOf("") }
    var closingDayText by remember { mutableStateOf("25") }
    var paymentDayText by remember { mutableStateOf("5") }
    var editingMethod by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var editClosingDayText by remember { mutableStateOf("25") }
    var editPaymentDayText by remember { mutableStateOf("5") }
    var pendingDeleteMethod by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val sortedPaymentMethods = paymentMethods.sortedBy { it.lowercase(Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appSurface,
        title = { Text("Pagamentos") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("Novo") },
                    singleLine = true,
                    colors = financeOutlinedTextFieldColors()
                )

                if (isCardPaymentMethod(newText)) {
                    OutlinedTextField(
                        value = closingDayText,
                        onValueChange = { closingDayText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Dia de fechamento do cartao") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = financeOutlinedTextFieldColors()
                    )
                    OutlinedTextField(
                        value = paymentDayText,
                        onValueChange = { paymentDayText = it.filter(Char::isDigit).take(2) },
                        label = { Text("Dia de pagamento do cartao") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = financeOutlinedTextFieldColors()
                    )
                }

                Button(
                    onClick = {
                        val clean = newText.trim()
                        val isCard = isCardPaymentMethod(clean)
                        val closingDay = closingDayText.toIntOrNull()
                        val paymentDay = paymentDayText.toIntOrNull()
                        when {
                            clean.isBlank() -> error = "Informe uma forma de pagamento."
                            clean in paymentMethods -> error = "Forma de pagamento ja existe."
                            isCard && (closingDay == null || closingDay !in 1..31) ->
                                error = "Dia de fechamento invalido."
                            isCard && (paymentDay == null || paymentDay !in 1..31) ->
                                error = "Dia de pagamento invalido."
                            else -> {
                                paymentMethods.add(clean)
                                if (isCard) {
                                    paymentMethodCardConfigs[clean] = CardPaymentConfig(
                                        closingDay = closingDay ?: 25,
                                        paymentDay = paymentDay ?: 5
                                    )
                                }
                                newText = ""
                                closingDayText = "25"
                                paymentDayText = "5"
                                error = null
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = headerBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }

                error?.let { Text(it, color = expenseRed) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedPaymentMethods, key = { it }) { method ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (editingMethod == method) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    label = { Text("Editar pagamento") },
                                    singleLine = true,
                                    colors = financeOutlinedTextFieldColors()
                                )
                                if (isCardPaymentMethod(editText)) {
                                    OutlinedTextField(
                                        value = editClosingDayText,
                                        onValueChange = { editClosingDayText = it.filter(Char::isDigit).take(2) },
                                        label = { Text("Dia de fechamento do cartao") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = financeOutlinedTextFieldColors()
                                    )
                                    OutlinedTextField(
                                        value = editPaymentDayText,
                                        onValueChange = { editPaymentDayText = it.filter(Char::isDigit).take(2) },
                                        label = { Text("Dia de pagamento do cartao") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        colors = financeOutlinedTextFieldColors()
                                    )
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val original = editingMethod ?: return@Button
                                            val clean = editText.trim()
                                            val isCard = isCardPaymentMethod(clean)
                                            val closingDay = editClosingDayText.toIntOrNull()
                                            val paymentDay = editPaymentDayText.toIntOrNull()
                                            when {
                                                clean.isBlank() -> error = "Informe uma forma de pagamento."
                                                clean != original && clean in paymentMethods ->
                                                    error = "Forma de pagamento ja existe."
                                                isCard && (closingDay == null || closingDay !in 1..31) ->
                                                    error = "Dia de fechamento invalido."
                                                isCard && (paymentDay == null || paymentDay !in 1..31) ->
                                                    error = "Dia de pagamento invalido."
                                                else -> {
                                                    val index = paymentMethods.indexOf(original)
                                                    if (index >= 0) paymentMethods[index] = clean
                                                    paymentMethodCardConfigs.remove(original)
                                                    if (isCard) {
                                                        paymentMethodCardConfigs[clean] = CardPaymentConfig(
                                                            closingDay = closingDay ?: 25,
                                                            paymentDay = paymentDay ?: 5
                                                        )
                                                    } else {
                                                        paymentMethodCardConfigs.remove(clean)
                                                    }
                                                    editingMethod = null
                                                    editText = ""
                                                    error = null
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = headerBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Salvar") }
                                    OutlinedButton(
                                        onClick = {
                                            editingMethod = null
                                            editText = ""
                                            error = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Cancelar") }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(method)
                                        val cfg = paymentMethodCardConfigs[method]
                                        if (cfg != null && isCardPaymentMethod(method)) {
                                            Text(
                                                "Fechamento: ${cfg.closingDay} • Pagamento: ${cfg.paymentDay}",
                                                color = appTextSecondary
                                            )
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = {
                                                val cfg = paymentMethodCardConfigs[method]
                                                editingMethod = method
                                                editText = method
                                                editClosingDayText = cfg?.closingDay?.toString() ?: "25"
                                                editPaymentDayText = cfg?.paymentDay?.toString() ?: "5"
                                                error = null
                                            },
                                            colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                                containerColor = appPrimaryTintBg,
                                                contentColor = headerBlue
                                            )
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                                        }
                                        IconButton(
                                            onClick = {
                                                pendingDeleteMethod = method
                                            },
                                            colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                                containerColor = appDangerTintBg,
                                                contentColor = expenseRed
                                            )
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = expenseRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = headerBlue)
            ) { Text("OK", fontWeight = FontWeight.SemiBold) }
        }
    )

    pendingDeleteMethod?.let { method ->
        AlertDialog(
            onDismissRequest = { pendingDeleteMethod = null },
            containerColor = appSurface,
            title = { Text("Confirmar exclusao") },
            text = { Text("Deseja excluir a forma de pagamento \"$method\"?") },
            dismissButton = {
                TextButton(onClick = { pendingDeleteMethod = null }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        paymentMethods.remove(method)
                        paymentMethodCardConfigs.remove(method)
                        pendingDeleteMethod = null
                    }
                ) { Text("Excluir", color = expenseRed) }
            }
        )
    }

}

@Composable
private fun ManageCategoriesDialog(
    title: String,
    categories: MutableList<String>,
    isExpense: Boolean,
    expenseCategoryLimits: MutableMap<String, Double>,
    onDismiss: () -> Unit
) {
    var newText by remember { mutableStateOf("") }
    var newLimitText by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editText by remember { mutableStateOf("") }
    var editLimitText by remember { mutableStateOf("") }
    var pendingDeleteCategory by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val sortedCategories = categories.sortedBy { it.lowercase(Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appSurface,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = newText,
                    onValueChange = { newText = it },
                    label = { Text("Nova") },
                    singleLine = true,
                    colors = financeOutlinedTextFieldColors()
                )
                if (isExpense) {
                    OutlinedTextField(
                        value = newLimitText,
                        onValueChange = {
                            newLimitText = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }
                        },
                        label = { Text("Limite mensal da categoria") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = financeOutlinedTextFieldColors()
                    )
                }
                Button(
                    onClick = {
                        val clean = newText.trim()
                        val parsedLimit = newLimitText.replace(",", ".").toDoubleOrNull()
                        if (isExpense && newLimitText.isNotBlank() && (parsedLimit == null || parsedLimit <= 0.0)) {
                            error = "Limite mensal invalido."
                            return@Button
                        }
                        if (clean.isNotBlank() && !categories.contains(clean)) {
                            categories.add(clean)
                            categories.sortBy { it.lowercase(Locale.getDefault()) }
                            if (isExpense && parsedLimit != null && parsedLimit > 0.0) {
                                expenseCategoryLimits[clean] = parsedLimit
                            }
                            newText = ""
                            newLimitText = ""
                            error = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = headerBlue,
                        contentColor = Color.White
                    )
                ) {
                    Text("Add", fontWeight = FontWeight.SemiBold)
                }
                error?.let { Text(it, color = expenseRed) }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp, max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedCategories, key = { it }) { category ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (editingCategory == category) {
                                OutlinedTextField(
                                    value = editText,
                                    onValueChange = { editText = it },
                                    label = { Text("Editar categoria") },
                                    singleLine = true,
                                    colors = financeOutlinedTextFieldColors()
                                )
                                if (isExpense) {
                                    OutlinedTextField(
                                        value = editLimitText,
                                        onValueChange = {
                                            editLimitText = it.filter { ch -> ch.isDigit() || ch == ',' || ch == '.' }
                                        },
                                        label = { Text("Limite mensal da categoria") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        colors = financeOutlinedTextFieldColors()
                                    )
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            val original = editingCategory ?: return@Button
                                            val clean = editText.trim()
                                            val parsedLimit = editLimitText.replace(",", ".").toDoubleOrNull()
                                            if (isExpense && editLimitText.isNotBlank() && (parsedLimit == null || parsedLimit <= 0.0)) {
                                                error = "Limite mensal invalido."
                                                return@Button
                                            }
                                            if (clean.isBlank()) {
                                                error = "Informe uma categoria."
                                                return@Button
                                            }
                                            if (clean != original && clean in categories) {
                                                error = "Categoria ja existe."
                                                return@Button
                                            }
                                            val index = categories.indexOf(original)
                                            if (index >= 0) categories[index] = clean
                                            categories.sortBy { it.lowercase(Locale.getDefault()) }
                                            if (isExpense) {
                                                expenseCategoryLimits.remove(original)
                                                if (parsedLimit != null && parsedLimit > 0.0) {
                                                    expenseCategoryLimits[clean] = parsedLimit
                                                } else {
                                                    expenseCategoryLimits.remove(clean)
                                                }
                                            }
                                            editingCategory = null
                                            editText = ""
                                            editLimitText = ""
                                            error = null
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = headerBlue,
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Salvar") }
                                    OutlinedButton(
                                        onClick = {
                                            editingCategory = null
                                            editText = ""
                                            editLimitText = ""
                                            error = null
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) { Text("Cancelar") }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp)
                                    ) {
                                        Text(category)
                                        if (isExpense) {
                                            val limit = expenseCategoryLimits[category]
                                            val limitText = if (limit != null) {
                                                "Limite mensal: ${moneyFormat.format(limit)}"
                                            } else {
                                                "Sem limite mensal"
                                            }
                                            Text(limitText, color = appTextSecondary)
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        IconButton(
                                            onClick = {
                                                editingCategory = category
                                                editText = category
                                                editLimitText = expenseCategoryLimits[category]
                                                    ?.let { String.format(Locale.forLanguageTag("pt-BR"), "%.2f", it) }
                                                    .orEmpty()
                                                error = null
                                            },
                                            colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                                containerColor = appPrimaryTintBg,
                                                contentColor = headerBlue
                                            )
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                                        }
                                        IconButton(
                                            onClick = {
                                                pendingDeleteCategory = category
                                            },
                                            colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                                containerColor = appDangerTintBg,
                                                contentColor = expenseRed
                                            )
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = expenseRed)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = headerBlue)
            ) { Text("OK", fontWeight = FontWeight.SemiBold) }
        }
    )

    pendingDeleteCategory?.let { category ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCategory = null },
            containerColor = appSurface,
            title = { Text("Confirmar exclusao") },
            text = { Text("Deseja excluir a categoria \"$category\"?") },
            dismissButton = {
                TextButton(onClick = { pendingDeleteCategory = null }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        categories.remove(category)
                        expenseCategoryLimits.remove(category)
                        pendingDeleteCategory = null
                    }
                ) { Text("Excluir", color = expenseRed) }
            }
        )
    }

}

@Composable
private fun HistoryFilterDialog(
    initialType: HistoryTypeFilter,
    initialCategory: String,
    initialPayment: String,
    expenseCategoryOptions: List<String>,
    incomeCategoryOptions: List<String>,
    paymentOptions: List<String>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
    onApply: (HistoryTypeFilter, String, String) -> Unit
) {
    var type by remember { mutableStateOf(initialType) }
    var category by remember { mutableStateOf(initialCategory) }
    var payment by remember { mutableStateOf(initialPayment) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }
    val visibleCategoryOptions = when (type) {
        HistoryTypeFilter.RECEITAS -> incomeCategoryOptions
        HistoryTypeFilter.DESPESAS -> expenseCategoryOptions
        HistoryTypeFilter.TODOS -> (expenseCategoryOptions + incomeCategoryOptions).distinct().sorted()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appSurface,
        title = { Text("Filtro do Historico") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val stackButtons = maxWidth < 360.dp
                    if (stackButtons) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleTypeButton(
                                label = "Todos",
                                selected = type == HistoryTypeFilter.TODOS,
                                selectedColor = headerBlue,
                                onClick = {
                                    type = HistoryTypeFilter.TODOS
                                    if (category.isNotBlank() && category !in visibleCategoryOptions) category = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ToggleTypeButton(
                                label = "Receitas",
                                selected = type == HistoryTypeFilter.RECEITAS,
                                selectedColor = incomeGreen,
                                onClick = {
                                    type = HistoryTypeFilter.RECEITAS
                                    if (category.isNotBlank() && category !in incomeCategoryOptions) category = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ToggleTypeButton(
                                label = "Despesas",
                                selected = type == HistoryTypeFilter.DESPESAS,
                                selectedColor = expenseRed,
                                onClick = {
                                    type = HistoryTypeFilter.DESPESAS
                                    if (category.isNotBlank() && category !in expenseCategoryOptions) category = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ToggleTypeButton(
                                label = "Todos",
                                selected = type == HistoryTypeFilter.TODOS,
                                selectedColor = headerBlue,
                                onClick = {
                                    type = HistoryTypeFilter.TODOS
                                    if (category.isNotBlank() && category !in visibleCategoryOptions) category = ""
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ToggleTypeButton(
                                label = "Receitas",
                                selected = type == HistoryTypeFilter.RECEITAS,
                                selectedColor = incomeGreen,
                                onClick = {
                                    type = HistoryTypeFilter.RECEITAS
                                    if (category.isNotBlank() && category !in incomeCategoryOptions) category = ""
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ToggleTypeButton(
                                label = "Despesas",
                                selected = type == HistoryTypeFilter.DESPESAS,
                                selectedColor = expenseRed,
                                onClick = {
                                    type = HistoryTypeFilter.DESPESAS
                                    if (category.isNotBlank() && category !in expenseCategoryOptions) category = ""
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryExpanded = true }
                ) {
                    OutlinedTextField(
                        value = if (category.isBlank()) "Todas" else category,
                        onValueChange = {},
                        label = { Text("Categoria") },
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir categorias")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = financeOutlinedTextFieldColors()
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Todas") },
                            onClick = {
                                category = ""
                                categoryExpanded = false
                            }
                        )
                        visibleCategoryOptions.forEach { option ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { paymentExpanded = true }
                ) {
                    OutlinedTextField(
                        value = if (payment.isBlank()) "Todos" else payment,
                        onValueChange = {},
                        label = { Text("Pagamento") },
                        singleLine = true,
                        readOnly = true,
                        enabled = false,
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Abrir pagamentos")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = financeOutlinedTextFieldColors()
                    )
                    androidx.compose.material3.DropdownMenu(
                        expanded = paymentExpanded,
                        onDismissRequest = { paymentExpanded = false }
                    ) {
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text("Todos") },
                            onClick = {
                                payment = ""
                                paymentExpanded = false
                            }
                        )
                        paymentOptions.forEach { option ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    payment = option
                                    paymentExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) { Text("Limpar") }
        },
        confirmButton = {
            TextButton(onClick = { onApply(type, category.trim(), payment.trim()) }) { Text("Aplicar") }
        }
    )
}

@Composable
private fun NotificationSettingsDialog(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    reminderTime: DailyReminderTime,
    onTimeChange: (DailyReminderTime) -> Unit,
    onDismiss: () -> Unit
) {
    var timeInput by remember(reminderTime) {
        mutableStateOf(String.format(Locale.getDefault(), "%02d:%02d", reminderTime.hour, reminderTime.minute))
    }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appSurface,
        title = { Text("Notificações") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ativar lembretes", fontWeight = FontWeight.SemiBold)
                        Text("Receba alertas ao ligar e no horário definido.", color = appTextSecondary)
                    }
                    Switch(
                        checked = enabled,
                        onCheckedChange = onEnabledChange
                    )
                }
                Text("Horário diário", fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = timeInput,
                    onValueChange = {
                        val digits = it.filter(Char::isDigit).take(4)
                        timeInput = when {
                            digits.length >= 3 -> "${digits.take(2)}:${digits.drop(2)}"
                            digits.length >= 1 -> digits
                            else -> ""
                        }
                    },
                    placeholder = { Text("HH:mm") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Insira o horário do lembrete no formato HH:mm.",
                    color = appTextSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )
                error?.let { Text(it, color = expenseRed, modifier = Modifier.padding(top = 4.dp)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsed = parseReminderTimeOrNull(timeInput)
                    if (parsed == null) {
                        error = "Informe um horário válido no formato HH:mm."
                        return@TextButton
                    }
                    onTimeChange(parsed)
                    error = null
                    onDismiss()
                }
            ) { Text("Salvar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun SummaryCard(balance: Double, income: Double, expense: Double) {
    val positiveBlue = headerBlue
    fun colorBySign(value: Double): Color = when {
        value < 0 -> expenseRed
        value > 0 -> positiveBlue
        else -> Color(0xFF6C6C78)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("RESUMO: Todo o historico", color = appTextSecondary, fontWeight = FontWeight.SemiBold)
            Text("Saldo ${moneyFormat.format(balance)}", color = colorBySign(balance), fontWeight = FontWeight.Bold)
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Receitas", color = appTextSecondary)
                    Text("+ ${moneyFormat.format(income)}", color = colorBySign(income), fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Despesas", color = appTextSecondary)
                    Text("- ${moneyFormat.format(expense)}", color = colorBySign(-expense), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun PeriodFilterCard(
    startDateText: String,
    onStartDateChange: (String) -> Unit,
    endDateText: String,
    onEndDateChange: (String) -> Unit,
    selectedType: HistoryTypeFilter,
    onTypeChange: (HistoryTypeFilter) -> Unit,
    selectedCategory: String,
    onCategoryChange: (String) -> Unit,
    selectedPayment: String,
    onPaymentChange: (String) -> Unit,
    expenseCategories: List<String>,
    incomeCategories: List<String>,
    paymentOptions: List<String>
) {
    val context = LocalContext.current
    var categoryExpanded by remember { mutableStateOf(false) }
    var paymentExpanded by remember { mutableStateOf(false) }
    val categoryOptions = when (selectedType) {
        HistoryTypeFilter.RECEITAS -> incomeCategories
        HistoryTypeFilter.DESPESAS -> expenseCategories
        HistoryTypeFilter.TODOS -> (expenseCategories + incomeCategories).distinct().sorted()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("FILTRAR POR PERIODO", color = headerBlue, fontWeight = FontWeight.Bold)
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackButtons = maxWidth < 360.dp
                if (stackButtons) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleTypeButton(
                            label = "Todos",
                            selected = selectedType == HistoryTypeFilter.TODOS,
                            selectedColor = headerBlue,
                            onClick = { onTypeChange(HistoryTypeFilter.TODOS) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ToggleTypeButton(
                            label = "Receitas",
                            selected = selectedType == HistoryTypeFilter.RECEITAS,
                            selectedColor = incomeGreen,
                            onClick = {
                                onTypeChange(HistoryTypeFilter.RECEITAS)
                                if (selectedCategory.isNotBlank() && selectedCategory !in incomeCategories) {
                                    onCategoryChange("")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        ToggleTypeButton(
                            label = "Despesas",
                            selected = selectedType == HistoryTypeFilter.DESPESAS,
                            selectedColor = expenseRed,
                            onClick = {
                                onTypeChange(HistoryTypeFilter.DESPESAS)
                                if (selectedCategory.isNotBlank() && selectedCategory !in expenseCategories) {
                                    onCategoryChange("")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleTypeButton(
                            label = "Todos",
                            selected = selectedType == HistoryTypeFilter.TODOS,
                            selectedColor = headerBlue,
                            onClick = { onTypeChange(HistoryTypeFilter.TODOS) },
                            modifier = Modifier.weight(1f)
                        )
                        ToggleTypeButton(
                            label = "Receitas",
                            selected = selectedType == HistoryTypeFilter.RECEITAS,
                            selectedColor = incomeGreen,
                            onClick = {
                                onTypeChange(HistoryTypeFilter.RECEITAS)
                                if (selectedCategory.isNotBlank() && selectedCategory !in incomeCategories) {
                                    onCategoryChange("")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        ToggleTypeButton(
                            label = "Despesas",
                            selected = selectedType == HistoryTypeFilter.DESPESAS,
                            selectedColor = expenseRed,
                            onClick = {
                                onTypeChange(HistoryTypeFilter.DESPESAS)
                                if (selectedCategory.isNotBlank() && selectedCategory !in expenseCategories) {
                                    onCategoryChange("")
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            if (selectedType != HistoryTypeFilter.TODOS) {
                DropdownLikeField(
                    value = if (selectedCategory.isBlank()) "Todas" else selectedCategory,
                    placeholder = "Categoria",
                    expanded = categoryExpanded,
                    onExpandChange = { categoryExpanded = it },
                    options = listOf("Todas") + categoryOptions,
                    includeAddOption = false,
                    addOptionText = "",
                    onSelect = {
                        onCategoryChange(if (it == "Todas") "" else it)
                        categoryExpanded = false
                    }
                )
                DropdownLikeField(
                    value = if (selectedPayment.isBlank()) "Todos" else selectedPayment,
                    placeholder = "Pagamento",
                    expanded = paymentExpanded,
                    onExpandChange = { paymentExpanded = it },
                    options = listOf("Todos") + paymentOptions,
                    includeAddOption = false,
                    addOptionText = "",
                    onSelect = {
                        onPaymentChange(if (it == "Todos") "" else it)
                        paymentExpanded = false
                    }
                )
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val stackButtons = maxWidth < 380.dp
                if (stackButtons) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val now = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            onStartDateChange(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
                                        },
                                        now.get(Calendar.YEAR),
                                        now.get(Calendar.MONTH),
                                        now.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            OutlinedTextField(
                                value = startDateText,
                                onValueChange = {},
                                label = { Text("Data Inicio") },
                                singleLine = true,
                                readOnly = true,
                                enabled = false,
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar data inicio")
                                },
                                colors = financeOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val now = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            onEndDateChange(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
                                        },
                                        now.get(Calendar.YEAR),
                                        now.get(Calendar.MONTH),
                                        now.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            OutlinedTextField(
                                value = endDateText,
                                onValueChange = {},
                                label = { Text("Data Fim") },
                                singleLine = true,
                                readOnly = true,
                                enabled = false,
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar data fim")
                                },
                                colors = financeOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        OutlinedButton(
                            onClick = {
                                onStartDateChange("")
                                onEndDateChange("")
                                onCategoryChange("")
                                onPaymentChange("")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = headerBlue)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = headerBlue)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("Limpar periodo")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val now = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            onStartDateChange(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
                                        },
                                        now.get(Calendar.YEAR),
                                        now.get(Calendar.MONTH),
                                        now.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            OutlinedTextField(
                                value = startDateText,
                                onValueChange = {},
                                label = { Text("Data Inicio") },
                                singleLine = true,
                                readOnly = true,
                                enabled = false,
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar data inicio")
                                },
                                colors = financeOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    val now = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            onEndDateChange(String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year))
                                        },
                                        now.get(Calendar.YEAR),
                                        now.get(Calendar.MONTH),
                                        now.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                }
                        ) {
                            OutlinedTextField(
                                value = endDateText,
                                onValueChange = {},
                                label = { Text("Data Fim") },
                                singleLine = true,
                                readOnly = true,
                                enabled = false,
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Selecionar data fim")
                                },
                                colors = financeOutlinedTextFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        IconButton(
                            onClick = {
                                onStartDateChange("")
                                onEndDateChange("")
                                onCategoryChange("")
                                onPaymentChange("")
                            },
                            modifier = Modifier.align(Alignment.CenterVertically),
                            colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                containerColor = appPrimaryTintBg,
                                contentColor = headerBlue
                            )
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Limpar periodo", tint = headerBlue)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTotalsChartCard(
    title: String,
    entries: List<Pair<String, Double>>,
    totalAmount: Double,
    barColor: Color,
    selectedCategory: String,
    onCategoryClick: (String) -> Unit
) {
    val maxValue = entries.maxOfOrNull { it.second } ?: 0.0
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = headerBlue, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Text(
                    moneyFormat.format(totalAmount),
                    color = barColor,
                    fontWeight = FontWeight.Bold
                )
            }
            if (entries.isEmpty()) {
                Text("Sem dados no periodo selecionado.", color = appTextSecondary)
            } else {
                entries.forEach { (category, value) ->
                    val fraction = if (maxValue > 0.0) {
                        (value / maxValue).toFloat().coerceIn(0.08f, 1f)
                    } else {
                        0f
                    }
                    val isSelected = selectedCategory.equals(category, ignoreCase = true)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onCategoryClick(category) },
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(category, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                            Text(
                                moneyFormat.format(value),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .background(chipGray, RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction)
                                    .height(8.dp)
                                    .background(
                                        if (isSelected) barColor.copy(alpha = 1f) else barColor.copy(alpha = 0.8f),
                                        RoundedCornerShape(6.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FutureLaunchesCard(
    items: List<FinancialTransactionEntity>,
    paidIds: Set<Long>,
    selectedIds: Set<Long>,
    onToggleSelected: (Long, Boolean) -> Unit,
    onSelectAllVisible: () -> Unit,
    onClearSelection: () -> Unit,
    onConfirmSelected: (List<Long>) -> Unit,
    onEdit: (FinancialTransactionEntity) -> Unit,
    onDelete: (FinancialTransactionEntity) -> Unit,
    onConfirmPaid: (FinancialTransactionEntity) -> Unit
) {
    var pendingConfirmSelectedIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("LANCAMENTOS FUTUROS", color = headerBlue, fontWeight = FontWeight.Bold)
            if (items.isEmpty()) {
                Text("Sem lancamentos futuros.", color = appTextSecondary)
            } else {
                val hasSelection = selectedIds.isNotEmpty()
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onSelectAllVisible,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = headerBlue)
                    ) { Text("Selecionar todos") }
                    OutlinedButton(
                        onClick = onClearSelection,
                        enabled = hasSelection,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = headerBlue)
                    ) { Text("Limpar") }
                }
                Button(
                    onClick = { pendingConfirmSelectedIds = selectedIds.toList() },
                    enabled = hasSelection,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = incomeGreen,
                        contentColor = Color.White
                    )
                ) {
                    Text("Concluir selecionados (${selectedIds.size})", fontWeight = FontWeight.SemiBold)
                }
                items.forEach { item ->
                    val isPaid = item.id in paidIds
                    val isSelected = item.id in selectedIds
                    val valueColor = if (item.type == TransactionType.RECEITA) incomeGreen else expenseRed
                    val isCardPayment = isCardPaymentMethod(item.paymentMethod)
                    val installmentCount = item.installments.coerceAtLeast(1)
                    val installmentNumber = item.installmentNumber.coerceIn(1, installmentCount)
                    val installmentLabel = "$installmentNumber/$installmentCount"
                    val launchDateText = formatFullDate(item.dateMillis)
                    val dueDateText = item.cardPaymentDateMillis?.let(::formatFullDate).orEmpty()
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = appSurfaceSoft)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            onToggleSelected(item.id, checked)
                                        }
                                    )
                                    Text(
                                        item.category,
                                        fontWeight = FontWeight.SemiBold,
                                        color = appTextPrimary
                                    )
                                }
                                Text(
                                    String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", item.amount),
                                    color = valueColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (isCardPayment) {
                                Text(
                                    "${item.paymentMethod} • Parcela $installmentLabel • Lanc: $launchDateText • Venc: ${if (dueDateText.isBlank()) "-" else dueDateText}",
                                    color = appTextSecondary
                                )
                            } else {
                                Text(
                                    "${item.paymentMethod} • Parcela $installmentLabel • Lanc: $launchDateText",
                                    color = appTextSecondary
                                )
                            }
                            Text(item.title, color = appTextPrimary)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = { onEdit(item) },
                                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                        containerColor = appPrimaryTintBg,
                                        contentColor = headerBlue
                                    )
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                                }
                                IconButton(
                                    onClick = { onDelete(item) },
                                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                        containerColor = appDangerTintBg,
                                        contentColor = expenseRed
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = expenseRed)
                                }
                                IconButton(
                                    onClick = { onConfirmPaid(item) },
                                    enabled = !isPaid,
                                    colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                                        containerColor = appSuccessTintBg,
                                        contentColor = incomeGreen,
                                        disabledContainerColor = appSuccessTintBg,
                                        disabledContentColor = incomeGreen
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Concluido", tint = incomeGreen)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (pendingConfirmSelectedIds.isNotEmpty()) {
        val count = pendingConfirmSelectedIds.size
        AlertDialog(
            onDismissRequest = { pendingConfirmSelectedIds = emptyList() },
            containerColor = appSurface,
            title = { Text("Confirmar conclusao") },
            text = {
                Text(
                    if (count == 1) {
                        "Deseja concluir 1 lancamento selecionado?"
                    } else {
                        "Deseja concluir $count lancamentos selecionados?"
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmSelectedIds = emptyList() }) { Text("Cancelar") }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onConfirmSelected(pendingConfirmSelectedIds)
                        pendingConfirmSelectedIds = emptyList()
                    }
                ) { Text("Concluir", color = incomeGreen) }
            }
        )
    }

}

@Composable
private fun HistoryCard(
    item: FinancialTransactionEntity,
    isConcluded: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val concludedColor = if (FinanceUiThemeState.darkMode) Color(0xFF86EFAC) else Color(0xFF2E7D32)
    val textColor = when {
        item.type == TransactionType.RECEITA -> headerBlue
        isConcluded -> concludedColor
        else -> appTextPrimary
    }
    val valueColor = if (item.type == TransactionType.RECEITA) headerBlue else textColor
    val prefix = if (item.type == TransactionType.RECEITA) "+" else "-"
    val isCardPayment = isCardPaymentMethod(item.paymentMethod)
    val installmentCount = item.installments.coerceAtLeast(1)
    val installmentNumber = item.installmentNumber.coerceIn(1, installmentCount)
    val installmentLabel = "Parcela $installmentNumber/$installmentCount de ${moneyFormat.format(item.originalTotalAmount)}"
    val dueDateText = item.cardPaymentDateMillis?.let { formatShortDate(it) } ?: "-"
    val launchDateText = formatShortDate(item.dateMillis)
    val valueLabel = "$prefix ${String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", item.amount)}"
    val statusLabel = if (isConcluded) "Concluido" else "Pendente"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = appSurfaceSoft)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.category, fontWeight = FontWeight.SemiBold, color = textColor)
                if (isCardPayment) {
                    Text(
                        "${item.paymentMethod} • $installmentLabel • Lanc: $launchDateText • Venc: $dueDateText",
                        color = textColor
                    )
                } else {
                    Text("${item.paymentMethod} • $installmentLabel • Lanc: $launchDateText", color = textColor)
                }
                Text(item.title, color = textColor)
                Text("Status: $statusLabel", color = textColor, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(valueLabel, color = valueColor, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(
                        onClick = onEdit,
                        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                            containerColor = appPrimaryTintBg,
                            contentColor = headerBlue
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    IconButton(
                        onClick = onDelete,
                        colors = androidx.compose.material3.IconButtonDefaults.iconButtonColors(
                            containerColor = appDangerTintBg,
                            contentColor = expenseRed
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = expenseRed)
                    }
                }
            }
        }
    }
}

private enum class ExportFormat {
    CSV,
    EXCEL
}

@Composable
private fun ShareHistoryDialog(
    onDismiss: () -> Unit,
    onShareCsv: () -> Unit,
    onShareExcel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = appSurface,
        title = { Text("Compartilhar historico") },
        text = { Text("Escolha o formato de exportacao:") },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onShareCsv) { Text("CSV") }
                TextButton(onClick = onShareExcel) { Text("Excel") }
            }
        }
    )
}

private fun shareHistory(
    context: Context,
    account: PersonEntity?,
    items: List<FinancialTransactionEntity>,
    format: ExportFormat,
    concludedDateByTransactionId: Map<Long, Long>,
    expenseCategoryLimits: Map<String, Double>
) {
    runCatching {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val extension = if (format == ExportFormat.CSV) "csv" else "xlsx"
        val mimeType = if (format == ExportFormat.CSV) {
            "text/csv"
        } else {
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        }
        val file = File(context.cacheDir, "historico_financeiro_$timestamp.$extension")
        if (format == ExportFormat.CSV) {
            file.writeText(buildHistoryExport(account, items, concludedDateByTransactionId, expenseCategoryLimits))
        } else {
            file.writeBytes(buildHistoryXlsx(account, items, concludedDateByTransactionId, expenseCategoryLimits))
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Dados da conta ${displayAccountName(account?.name.orEmpty())}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar dados da conta"))
    }.onFailure {
        Toast.makeText(context, "Nao foi possivel compartilhar os dados da conta.", Toast.LENGTH_SHORT).show()
    }
}

private data class ExportRow(
    val columns: List<String>,
    val highlightConclusionDate: Boolean
)

private fun buildExportRows(
    account: PersonEntity?,
    items: List<FinancialTransactionEntity>,
    concludedDateByTransactionId: Map<Long, Long>,
    expenseCategoryLimits: Map<String, Double>
): List<ExportRow> {
    val todayStartMillis = LocalDate.now()
        .atStartOfDay(ZoneId.systemDefault())
        .toInstant()
        .toEpochMilli()
    val monthCategoryExpenses = items
        .asSequence()
        .filter { it.type == TransactionType.DESPESA }
        .groupBy {
            val monthKey = toLocalDate(it.dateMillis).withDayOfMonth(1).toString()
            "${monthKey}::${it.category.trim().lowercase(Locale.getDefault())}"
        }
        .mapValues { (_, value) -> value.sumOf { it.amount } }

    return items.map { item ->
        val tipo = if (item.type == TransactionType.RECEITA) "Receita" else "Despesa"
        val parcela = "${item.installmentNumber.coerceAtLeast(1)}/${item.installments.coerceAtLeast(1)}"
        val venc = item.cardPaymentDateMillis?.let(::formatFullDate).orEmpty()
        val launchDate = formatFullDate(item.dateMillis)
        val conclusionMillis = resolvedConclusionDateMillis(
            item = item,
            concludedDateByTransactionId = concludedDateByTransactionId,
            todayStartMillis = todayStartMillis
        )
        val conclusionDate = conclusionMillis?.let(::formatFullDate).orEmpty()
        val categoryLimit = if (item.type == TransactionType.DESPESA) {
            expenseCategoryLimits[item.category.trim()]
        } else {
            null
        }
        val monthKey = toLocalDate(item.dateMillis).withDayOfMonth(1).toString()
        val categoryKey = item.category.trim().lowercase(Locale.getDefault())
        val monthCategoryTotal = monthCategoryExpenses["${monthKey}::${categoryKey}"] ?: 0.0
        val exceeded = if (categoryLimit != null) monthCategoryTotal > categoryLimit else false

        ExportRow(
            columns = listOf(
                displayAccountName(account?.name.orEmpty()),
                account?.phone.orEmpty(),
                account?.email.orEmpty(),
                launchDate,
                conclusionDate,
                tipo,
                item.category,
                item.title,
                item.paymentMethod,
                parcela,
                item.amount.toString(),
                item.originalTotalAmount.toString(),
                categoryLimit?.toString().orEmpty(),
                if (categoryLimit == null) "" else if (exceeded) "Sim" else "Nao",
                venc
            ),
            highlightConclusionDate = conclusionDate.isNotBlank() && conclusionDate != launchDate
        )
    }
}

private fun buildHistoryExport(
    account: PersonEntity?,
    items: List<FinancialTransactionEntity>,
    concludedDateByTransactionId: Map<Long, Long>,
    expenseCategoryLimits: Map<String, Double>
): String {
    val sep = ";"
    val header = listOf(
        "Conta",
        "Telefone",
        "Email",
        "Data Lancamento",
        "Data Conclusao",
        "Tipo",
        "Categoria",
        "Descricao",
        "Pagamento",
        "Parcela",
        "Valor Parcela",
        "Valor Total",
        "Valor Maximo",
        "Valor Maximo Ultrapassado",
        "Data Vencimento"
    ).joinToString(sep)

    val body = buildExportRows(account, items, concludedDateByTransactionId, expenseCategoryLimits).map { row ->
        row.columns.joinToString(sep) { escapeForExport(it, sep) }
    }

    val csv = buildString {
        append(header)
        append('\n')
        body.forEachIndexed { idx, line ->
            append(line)
            if (idx < body.lastIndex) append('\n')
        }
    }
    return "\uFEFF$csv"
}

private fun escapeForExport(value: String, sep: String): String {
    val needsQuotes = value.contains(sep) || value.contains('"') || value.contains('\n')
    val escaped = value.replace("\"", "\"\"")
    return if (needsQuotes) "\"$escaped\"" else escaped
}

private fun buildHistoryXlsx(
    account: PersonEntity?,
    items: List<FinancialTransactionEntity>,
    concludedDateByTransactionId: Map<Long, Long>,
    expenseCategoryLimits: Map<String, Double>
): ByteArray {
    val header = listOf(
        "Conta",
        "Telefone",
        "Email",
        "Data Lancamento",
        "Data Conclusao",
        "Tipo",
        "Categoria",
        "Descricao",
        "Pagamento",
        "Parcela",
        "Valor Parcela",
        "Valor Total",
        "Valor Maximo",
        "Valor Maximo Ultrapassado",
        "Data Vencimento"
    )
    val exportRows = buildExportRows(account, items, concludedDateByTransactionId, expenseCategoryLimits)

    val sheetData = buildString {
        append("""<row r="1">""")
        header.forEachIndexed { colIndex, value ->
            val cellRef = excelColumnName(colIndex + 1) + 1
            append("""<c r="$cellRef" t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
        }
        append("</row>")

        exportRows.forEachIndexed { index, row ->
            val rowNumber = index + 2
            append("""<row r="$rowNumber">""")
            row.columns.forEachIndexed { colIndex, value ->
                val cellRef = excelColumnName(colIndex + 1) + rowNumber
                val style = if (colIndex == 1 && row.highlightConclusionDate) """ s="1"""" else ""
                append("""<c r="$cellRef"$style t="inlineStr"><is><t>${escapeXml(value)}</t></is></c>""")
            }
            append("</row>")
        }
    }

    val sheetXml =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>$sheetData</sheetData>
</worksheet>"""

    val workbookXml =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
 xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Historico" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    val workbookRelsXml =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    val rootRelsXml =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    val contentTypesXml =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    val stylesXml =
        """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><color theme="1"/><name val="Calibri"/><family val="2"/></font>
    <font><sz val="11"/><color rgb="FF9C0006"/><name val="Calibri"/><family val="2"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFEB9C"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""

    val bos = ByteArrayOutputStream()
    ZipOutputStream(bos).use { zip ->
        fun add(path: String, content: String) {
            zip.putNextEntry(ZipEntry(path))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        add("[Content_Types].xml", contentTypesXml)
        add("_rels/.rels", rootRelsXml)
        add("xl/workbook.xml", workbookXml)
        add("xl/_rels/workbook.xml.rels", workbookRelsXml)
        add("xl/styles.xml", stylesXml)
        add("xl/worksheets/sheet1.xml", sheetXml)
    }
    return bos.toByteArray()
}

private fun escapeXml(value: String): String {
    return value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")
}

private fun excelColumnName(index: Int): String {
    var n = index
    val sb = StringBuilder()
    while (n > 0) {
        val rem = (n - 1) % 26
        sb.append(('A'.code + rem).toChar())
        n = (n - 1) / 26
    }
    return sb.reverse().toString()
}

private fun parseDateOrNull(input: String): LocalDate? {
    if (input.isBlank()) return null
    return runCatching { LocalDate.parse(input, fullDateFormat) }.getOrNull()
}

private fun parseDateStartMillisOrNull(input: String): Long? {
    val date = parseDateOrNull(input) ?: return null
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

private fun parseDateEndMillisOrNull(input: String): Long? {
    val date = parseDateOrNull(input) ?: return null
    return date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1
}

private fun toLocalDate(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
}

private fun formatFullDate(millis: Long): String = toLocalDate(millis).format(fullDateFormat)

private fun formatShortDate(millis: Long): String = toLocalDate(millis).format(shortDateFormat)

