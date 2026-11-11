package com.group3.spendwise.ui.components

import com.group3.spendwise.R
import androidx.compose.ui.res.stringResource
import com.group3.spendwise.ui.components.localizedCategory

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.group3.spendwise.VoiceNoteRecorder

private val voiceGreen = Color(0xFF477A62)
private val voiceNavy = Color(0xFF203846)
private val voiceMuted = Color(0xFF7A8880)
private val voiceRed = Color(0xFFC7554E)
private val voiceSage = Color(0xFFEAF1E9)

@Composable
fun VoiceNoteSection(
    voiceNotePath: String?,
    onVoiceNotePathChange: (String?) -> Unit
) {
    val context = LocalContext.current
    val audioRecorder = remember(context) {
        VoiceNoteRecorder(context)
    }

    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    DisposableEffect(audioRecorder) {
        onDispose {
            audioRecorder.release()
        }
    }

    fun beginRecording() {
        try {
            audioRecorder.stopPlayback()
            isPlaying = false

            audioRecorder.startRecording()
            isRecording = true
            message = context.getString(R.string.voice_recording)
        } catch (e: Exception) {
            isRecording = false
            message = context.getString(R.string.voice_start_error)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            beginRecording()
        } else {
            message = context.getString(R.string.voice_permission_error)
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(R.string.voice_optional),
            color = voiceMuted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(6.dp))

        when {
            isRecording -> {
                Button(
                    onClick = {
                        val savedPath = audioRecorder.stopRecording()
                        isRecording = false

                        if (savedPath != null) {
                            onVoiceNotePathChange(savedPath)
                            message = context.getString(R.string.voice_recorded)
                        } else {
                            message = context.getString(R.string.voice_short_error)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = voiceRed
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Stop,
                        contentDescription = null
                    )
                    Text(
                        text = stringResource(R.string.voice_stop_recording),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.voice_mic_active),
                    color = voiceRed,
                    fontSize = 12.sp
                )
            }

            voiceNotePath.isNullOrBlank() -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            voiceSage,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable {
                            val hasPermission =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                beginRecording()
                            } else {
                                permissionLauncher.launch(
                                    Manifest.permission.RECORD_AUDIO
                                )
                            }
                        }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Mic,
                        contentDescription = null,
                        tint = voiceGreen
                    )

                    Text(
                        text = stringResource(R.string.voice_add),
                        color = voiceGreen,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            else -> {
                Text(
                    text = stringResource(R.string.voice_added),
                    color = voiceGreen,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            if (isPlaying) {
                                audioRecorder.stopPlayback()
                                isPlaying = false
                                message = ""
                            } else {
                                try {
                                    audioRecorder.playRecording(voiceNotePath) {
                                        isPlaying = false
                                    }
                                    isPlaying = true
                                    message = ""
                                } catch (e: Exception) {
                                    isPlaying = false
                                    message = context.getString(R.string.details_voice_error)
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = voiceGreen
                        )
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                            contentDescription = null
                        )
                        Text(
                            text = if (isPlaying) stringResource(R.string.voice_stop) else stringResource(R.string.voice_play),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    TextButton(
                        onClick = {
                            audioRecorder.stopPlayback()
                            isPlaying = false

                            val hasPermission =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED

                            if (hasPermission) {
                                beginRecording()
                            } else {
                                permissionLauncher.launch(
                                    Manifest.permission.RECORD_AUDIO
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = voiceGreen
                        )
                        Text(
                            text = stringResource(R.string.voice_redo),
                            color = voiceGreen
                        )
                    }

                    TextButton(
                        onClick = {
                            audioRecorder.stopPlayback()
                            isPlaying = false
                            onVoiceNotePathChange(null)
                            message = context.getString(R.string.voice_removed)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = voiceRed
                        )
                        Text(
                            text = stringResource(R.string.voice_remove),
                            color = voiceRed
                        )
                    }
                }
            }
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(6.dp))

            Text(
                text = message,
                color = if (isRecording) voiceRed else voiceMuted,
                fontSize = 12.sp
            )
        }
    }
}