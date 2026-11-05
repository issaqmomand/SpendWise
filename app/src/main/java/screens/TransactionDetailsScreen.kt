package com.group3.spendwise.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.group3.spendwise.VoiceNoteRecorder
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Keep the original light colours and follow SpendWise's active MaterialTheme in dark mode.
private data class DetailsPalette(
    val background: Color,
    val foreground: Color,
    val accent: Color,
    val sage: Color,
    val muted: Color,
    val divider: Color,
    val error: Color,
    val surface: Color,
    val summaryBackground: Color,
    val summaryText: Color,
    val buttonText: Color
)

@Composable
private fun detailsPalette(): DetailsPalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f

    return if (isDark) {
        DetailsPalette(
            background = scheme.background,
            foreground = scheme.onSurface,
            accent = scheme.primary,
            sage = scheme.secondaryContainer,
            muted = scheme.onSurfaceVariant,
            divider = scheme.outlineVariant,
            error = scheme.error,
            surface = scheme.surface,
            summaryBackground = scheme.primaryContainer,
            summaryText = scheme.onPrimaryContainer,
            buttonText = scheme.onPrimary
        )
    } else {
        DetailsPalette(
            background = Color(0xFFF6F7F4),
            foreground = Color(0xFF203846),
            accent = Color(0xFF477A62),
            sage = Color(0xFFEAF1E9),
            muted = Color(0xFF7A8880),
            divider = Color(0xFFE7ECE6),
            error = Color(0xFFC7554E),
            surface = Color.White,
            summaryBackground = Color(0xFF203846),
            summaryText = Color.White,
            buttonText = Color.White
        )
    }
}

@Composable
fun TransactionDetailsScreen(
    transaction: SpendTransaction,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onViewReceipt: (String) -> Unit
) {
    val context = LocalContext.current
    val colors = detailsPalette()
    val audioPlayer = remember(context, transaction.id) {
        VoiceNoteRecorder(context)
    }
    var isPlaying by remember(transaction.id) { mutableStateOf(false) }
    var playbackError by remember(transaction.id) { mutableStateOf("") }

    // Audio stops when navigating away from this transaction.
    DisposableEffect(audioPlayer) {
        onDispose { audioPlayer.release() }
    }

    val isExpense = transaction.kind == TransactionKind.EXPENSE
    val formattedAmount = String.format(Locale.CANADA, "%,.2f", transaction.amount)
    val amountText = if (isExpense) "-$$formattedAmount" else "+$$formattedAmount"
    val formattedDate = remember(transaction.date) {
        runCatching {
            LocalDate.parse(transaction.date).format(
                DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)
            )
        }.getOrElse { transaction.date }
    }
    val voicePath = transaction.voiceNotePath?.takeIf { it.isNotBlank() }
    val hasVoiceNote = voicePath != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp)
            .padding(top = 12.dp, bottom = 25.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = colors.foreground)
            }
            Spacer(Modifier.width(6.dp))
            Text(
                "Transaction Details",
                color = colors.foreground,
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(Modifier.height(13.dp))

        // Transaction summary.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.summaryBackground, RoundedCornerShape(17.dp))
                .padding(vertical = 14.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(colors.sage, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isExpense) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.height(7.dp))
            Text(
                if (isExpense) "Expense" else "Income",
                fontSize = 12.sp,
                color = colors.summaryText.copy(alpha = 0.88f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                amountText,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                color = colors.summaryText
            )
            Spacer(Modifier.height(3.dp))
            Text(
                transaction.description,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.summaryText
            )
            Spacer(Modifier.height(4.dp))
            Text(
                transaction.category,
                fontSize = 12.sp,
                color = colors.summaryText.copy(alpha = 0.83f)
            )
        }

        Spacer(Modifier.height(9.dp))

        DetailsCard {
            Text(
                "Transaction Information",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.foreground
            )
            Spacer(Modifier.height(8.dp))
            InfoRow("Description", transaction.description)
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            InfoRow("Category", transaction.category)
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            InfoRow("Date", formattedDate)
            HorizontalDivider(color = colors.divider, thickness = 1.dp)
            InfoRow("Type", if (isExpense) "Expense" else "Income")
        }

        Spacer(Modifier.height(9.dp))

        DetailsCard {
            Text(
                "Notes",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.foreground
            )
            Spacer(Modifier.height(8.dp))
            Text(
                transaction.notes.ifBlank { "No notes added." },
                fontSize = 12.sp,
                color = if (transaction.notes.isBlank()) colors.muted else colors.foreground
            )
        }

        Spacer(Modifier.height(9.dp))

        DetailsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "Receipt Attachment",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.foreground
                )
            }
            Spacer(Modifier.height(8.dp))
            if (transaction.receiptUri.isNullOrBlank()) {
                Text("No receipt attached.", color = colors.muted, fontSize = 12.sp)
            } else {
                TextButton(
                    onClick = { onViewReceipt(transaction.receiptUri.orEmpty()) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Text("View Receipt", color = colors.accent, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(9.dp))

        // Voice notes can be played here but changed only via Edit.
        DetailsCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Mic,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(7.dp))
                Text(
                    "Voice Note",
                    color = colors.foreground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            if (!hasVoiceNote) {
                Text(
                    "No voice note attached.",
                    fontSize = 12.sp,
                    color = colors.muted
                )
            } else {
                OutlinedButton(
                    onClick = {
                        if (isPlaying) {
                            audioPlayer.stopPlayback()
                            isPlaying = false
                            playbackError = ""
                        } else {
                            try {
                                val file = File(voicePath!!)
                                if (!file.exists()) {
                                    playbackError = "Recording file not found."
                                } else {
                                    audioPlayer.playRecording(voicePath) {
                                        isPlaying = false
                                    }
                                    isPlaying = true
                                    playbackError = ""
                                }
                            } catch (_: Exception) {
                                isPlaying = false
                                playbackError = "Unable to play voice note."
                            }
                        }
                    },
                    modifier = Modifier.height(32.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 0.dp
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, colors.accent),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent)
                ) {
                    Icon(
                        if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(if (isPlaying) "Stop" else "Play Recording", fontSize = 12.sp)
                }
            }

            if (playbackError.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Text(playbackError, color = colors.error, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(11.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Button(
                onClick = {
                    audioPlayer.stopPlayback()
                    onEdit()
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.buttonText
                )
            ) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Edit", fontSize = 13.sp)
            }
            OutlinedButton(
                onClick = {
                    audioPlayer.stopPlayback()
                    onDelete()
                },
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error)
            ) {
                Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(6.dp))
                Text("Delete", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun DetailsCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = detailsPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(18.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        content = content
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    val colors = detailsPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            fontSize = 12.sp,
            color = colors.muted
        )
        Text(
            value,
            modifier = Modifier.weight(1.15f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.foreground
        )
    }
}

