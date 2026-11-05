package com.group3.spendwise.ui.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Notes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.UUID
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val formBackground: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface == Color.White)
        Color(0xFFF6F7F4) else MaterialTheme.colorScheme.background
private val formNavy: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface == Color.White)
        Color(0xFF203846) else MaterialTheme.colorScheme.onBackground
private val formGreen: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface == Color.White)
        Color(0xFF477A62) else MaterialTheme.colorScheme.primary
private val formSage: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface == Color.White)
        Color(0xFFEAF1E9) else MaterialTheme.colorScheme.secondaryContainer
private val formMuted: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface == Color.White)
        Color(0xFF7A8880) else MaterialTheme.colorScheme.onSurfaceVariant
private val formBorder: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface == Color.White)
        Color(0xFFDFE7DF) else MaterialTheme.colorScheme.outline
private val formRed: Color
    @Composable get() = if (MaterialTheme.colorScheme.surface == Color.White)
        Color(0xFFC7554E) else MaterialTheme.colorScheme.error

private val expenseCategories = listOf(
    "Food", "Transportation", "Shopping", "Entertainment",
    "School", "Housing", "Subscriptions", "Other"
)
private val incomeCategories = listOf("Salary", "Gift", "Refund", "Other")

private val formSurface: Color
    @Composable get() = MaterialTheme.colorScheme.surface

@Composable
fun AddTransactionScreen(
    onBack: () -> Unit,
    onSave: (SpendTransaction) -> Unit,
    transactionToEdit: SpendTransaction? = null,
    onDelete: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val editing = transactionToEdit != null

    var kind by rememberSaveable(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.kind?.name ?: TransactionKind.EXPENSE.name)
    }
    var amount by rememberSaveable(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.amount?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }
    var description by rememberSaveable(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.description ?: "")
    }
    var category by rememberSaveable(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.category ?: "")
    }
    var date by rememberSaveable(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.date ?: LocalDate.now().toString())
    }
    var notes by rememberSaveable(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.notes ?: "")
    }
    var receiptUri by rememberSaveable(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.receiptUri)
    }
    var voiceNotePath by rememberSaveable(transactionToEdit?.id) {
        mutableStateOf(transactionToEdit?.voiceNotePath)
    }
    var showReceiptOptions by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf("") }
    var pendingCameraFile by rememberSaveable { mutableStateOf("") }
    var receiptError by rememberSaveable { mutableStateOf("") }
    var categoryExpanded by rememberSaveable { mutableStateOf(false) }
    var attemptedSave by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    val receiptPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some providers only grant temporary access.
            }
            receiptUri = uri.toString()
            receiptError = ""
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri.isNotBlank()) {
            receiptUri = pendingCameraUri
            receiptError = ""
        } else {
            if (pendingCameraFile.isNotBlank()) {
                File(pendingCameraFile).delete()
            }
        }
        pendingCameraUri = ""
        pendingCameraFile = ""
    }

    fun takeReceiptPhoto() {
        try {
            val dir = File(context.filesDir, "receipts")
            if (!dir.exists() && !dir.mkdirs()) {
                error("Could not create receipt folder")
            }
            val imageFile = File(dir, "receipt_${UUID.randomUUID()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.receiptprovider",
                imageFile
            )
            pendingCameraUri = uri.toString()
            pendingCameraFile = imageFile.absolutePath
            cameraLauncher.launch(uri)
        } catch (_: Exception) {
            pendingCameraUri = ""
            pendingCameraFile = ""
            receiptError = "Unable to open camera."
        }
    }

    fun viewReceipt() {
        val uriString = receiptUri ?: return
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(uriString), "image/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            )
            receiptError = ""
        } catch (_: Exception) {
            receiptError = "Unable to open receipt."
        }
    }

    val numericAmount = amount.toDoubleOrNull()
    val canSave = numericAmount != null && numericAmount > 0.0 &&
            description.isNotBlank() && category.isNotBlank()
    val categories = if (kind == TransactionKind.INCOME.name) incomeCategories else expenseCategories

    Column(
        modifier = Modifier.fillMaxSize()
            .background(formBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 23.dp)
            .padding(top = 25.dp, bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Outlined.ArrowBack, "Back", tint = formNavy)
            }
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    if (editing) "Edit Transaction" else "Add Transaction",
                    color = formNavy,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Normal
                )
                Text(
                    if (editing) "Make your record more accurate." else "A little habit. A clearer picture.",
                    color = formMuted,
                    fontSize = 14.sp
                )
            }
            Text("CAD", color = formMuted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                modifier = Modifier.padding(top = 25.dp))
        }

        Spacer(Modifier.height(28.dp))
        Text("Receipt Attachment · optional", color = formMuted, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(formSage, RoundedCornerShape(17.dp))
                .clickable { showReceiptOptions = true }
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CameraAlt, null, tint = formGreen, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(
                if (receiptUri.isNullOrBlank()) "Add Receipt Photo" else "Change Receipt Photo",
                color = formGreen,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        if (!receiptUri.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Text("Receipt attached", fontSize = 12.sp, color = formGreen)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { viewReceipt() }) {
                    Text("View", color = formGreen, fontSize = 13.sp)
                }
                TextButton(onClick = {
                    receiptUri = null
                    receiptError = ""
                }) {
                    Text("Remove", color = formRed, fontSize = 13.sp)
                }
            }
        }
        if (receiptError.isNotBlank()) {
            Text(receiptError, color = formRed, fontSize = 12.sp)
        }

        Spacer(Modifier.height(18.dp))
        Row(
            modifier = Modifier.fillMaxWidth()
                .background(formSage, RoundedCornerShape(18.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            KindOption(
                text = "Expense", selected = kind == TransactionKind.EXPENSE.name,
                icon = Icons.Outlined.ArrowUpward,
                modifier = Modifier.weight(1f)
            ) {
                kind = TransactionKind.EXPENSE.name
                if (category !in expenseCategories) category = ""
            }
            KindOption(
                text = "Income", selected = kind == TransactionKind.INCOME.name,
                icon = Icons.Outlined.ArrowDownward,
                modifier = Modifier.weight(1f)
            ) {
                kind = TransactionKind.INCOME.name
                if (category !in incomeCategories) category = ""
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Amount * · CAD",
            fontSize = 13.sp,
            color = formMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$", fontSize = 51.sp, color = formNavy)
            BasicTextField(
                value = amount,
                onValueChange = { entered ->
                    if (entered.isEmpty() || entered.matches(Regex("\\d{0,9}(\\.\\d{0,2})?"))) {
                        amount = entered
                    }
                },
                modifier = Modifier.width(195.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(formGreen),
                textStyle = TextStyle(color = formNavy, fontSize = 51.sp, textAlign = TextAlign.Center),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.Center) {
                        if (amount.isEmpty()) {
                            Text("0.00", color = formNavy, fontSize = 51.sp)
                        }
                        inner()
                    }
                }
            )
        }

        Spacer(Modifier.height(19.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Description *") },
            placeholder = { Text("What was it for?") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = softInputColors()
        )

        Spacer(Modifier.height(13.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .background(formSurface, RoundedCornerShape(16.dp))
                        .clickable { categoryExpanded = true }
                        .padding(horizontal = 12.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Category *", color = formMuted, fontSize = 12.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            category.ifBlank { "Select category" },
                            color = if (category.isBlank()) formMuted else formNavy,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                    Icon(Icons.Outlined.KeyboardArrowDown, null, tint = formMuted, modifier = Modifier.size(19.dp))
                }
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { option ->
                        DropdownMenuItem(text = { Text(option) }, onClick = {
                            category = option
                            categoryExpanded = false
                        })
                    }
                }
            }
            Row(
                modifier = Modifier.weight(1f)
                    .background(formSurface, RoundedCornerShape(16.dp))
                    .clickable {
                        val current = runCatching { LocalDate.parse(date) }.getOrElse { LocalDate.now() }
                        DatePickerDialog(
                            context,
                            { _, year, month, day -> date = LocalDate.of(year, month + 1, day).toString() },
                            current.year, current.monthValue - 1, current.dayOfMonth
                        ).show()
                    }
                    .padding(horizontal = 12.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Date *", color = formMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(displayFormDate(date), color = formNavy, fontSize = 14.sp, maxLines = 1)
                }
                Icon(Icons.Outlined.CalendarToday, null, tint = formMuted, modifier = Modifier.size(18.dp))
            }
        }

        Spacer(Modifier.height(11.dp))
        Text(
            if (kind == TransactionKind.EXPENSE.name)
                "Food · Transportation · Shopping · Entertainment\nSchool · Housing · Subscriptions · Other"
            else "Salary · Gift · Refund · Other",
            fontSize = 12.sp,
            color = formMuted,
            lineHeight = 19.sp
        )

        Spacer(Modifier.height(14.dp))
        NotesInputField(
            notes = notes,
            onNotesChange = { notes = it }
        )

        Spacer(Modifier.height(15.dp))
        VoiceNoteSection(
            voiceNotePath = voiceNotePath,
            onVoiceNotePathChange = { newPath -> voiceNotePath = newPath }
        )

        if (attemptedSave && !canSave) {
            Spacer(Modifier.height(12.dp))
            Text("Enter an amount, description and category.", color = formRed, fontSize = 13.sp)
        }

        Spacer(Modifier.height(21.dp))
        Button(
            onClick = {
                attemptedSave = true
                if (canSave) {
                    onSave(
                        SpendTransaction(
                            id = transactionToEdit?.id ?: 0L,
                            description = description.trim(),
                            amount = numericAmount!!,
                            category = category,
                            date = date,
                            kind = TransactionKind.valueOf(kind),
                            notes = notes.trim(),
                            receiptUri = receiptUri,
                            voiceNotePath = voiceNotePath
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(57.dp),
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = formGreen)
        ) {
            Text(if (editing) "Save Changes" else "Save Transaction", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }

        if (editing && onDelete != null) {
            Spacer(Modifier.height(9.dp))
            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Icon(Icons.Outlined.DeleteOutline, null, tint = formRed, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(5.dp))
                Text("Delete Transaction", color = formRed)
            }
        }
    }

    if (showReceiptOptions) {
        AlertDialog(
            onDismissRequest = { showReceiptOptions = false },
            title = { Text("Add Receipt Photo", color = formNavy) },
            text = { Text("How would you like to attach a receipt?") },
            confirmButton = {
                TextButton(onClick = {
                    showReceiptOptions = false
                    takeReceiptPhoto()
                }) {
                    Text("Take Photo", color = formGreen)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showReceiptOptions = false
                    receiptPicker.launch(arrayOf("image/*"))
                }) {
                    Text("Choose from Gallery", color = formGreen)
                }
            },
            containerColor = formBackground
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete transaction?", color = formNavy) },
            text = { Text("This transaction will be permanently removed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete?.invoke()
                }) { Text("Delete", color = formRed) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = formGreen) }
            },
            containerColor = formBackground
        )
    }
}


// Notes field without a separate label.
@Composable
private fun NotesInputField(
    notes: String,
    onNotesChange: (String) -> Unit
) {
    val noteShape = RoundedCornerShape(16.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, formBorder, noteShape)
                .background(formSurface, noteShape)
                .padding(start = 15.dp, end = 13.dp, top = 15.dp, bottom = 13.dp),
            verticalAlignment = Alignment.Top
        ) {
            BasicTextField(
                value = notes,
                onValueChange = onNotesChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 43.dp),
                textStyle = TextStyle(
                    color = formNavy,
                    fontSize = 16.sp,
                    lineHeight = 23.sp
                ),
                cursorBrush = SolidColor(formGreen),
                minLines = 2,
                maxLines = 3,
                decorationBox = { innerTextField ->
                    Box {
                        if (notes.isEmpty()) {
                            Text(
                                text = "Add a note...",
                                color = formMuted,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                }
            )
            Spacer(Modifier.width(12.dp))
            Icon(
                imageVector = Icons.Outlined.Notes,
                contentDescription = null,
                tint = formMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun KindOption(
    text: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .background(if (selected) formSurface else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = if (selected) formGreen else formMuted, modifier = Modifier.size(17.dp))
        Spacer(Modifier.width(7.dp))
        Text(text, color = if (selected) formGreen else formMuted,
            fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun softInputColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = formSurface,
    unfocusedContainerColor = formSurface,
    focusedBorderColor = formGreen,
    unfocusedBorderColor = formBorder,
    focusedTextColor = formNavy,
    unfocusedTextColor = formNavy,
    focusedLabelColor = formGreen,
    unfocusedLabelColor = formMuted,
    focusedPlaceholderColor = formMuted,
    unfocusedPlaceholderColor = formMuted,
    cursorColor = formGreen
)

private fun displayFormDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH))
}.getOrElse { value }
