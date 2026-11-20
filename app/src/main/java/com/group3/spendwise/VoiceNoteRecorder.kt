
package com.group3.spendwise

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.SystemClock
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.UUID

class VoiceNoteRecorder(context: Context) {

    companion object {
        private const val TAG = "SpendWiseVoice"
    }

    private val appContext = context.applicationContext

    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null
    private var recordingFile: File? = null
    private var recordingStartedAt = 0L

    val isRecording: Boolean
        get() = recorder != null

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            MediaRecorder()
        }
    }

    fun startRecording() {
        check(!isRecording) {
            "A recording is already in progress."
        }

        stopPlayback()

        val directory = File(
            appContext.filesDir,
            "voice_notes"
        )

        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Could not create voice notes folder.")
        }

        val file = File(
            directory,
            "voice_${UUID.randomUUID()}.m4a"
        )

        val newRecorder = createRecorder()

        try {
            newRecorder.setAudioSource(
                MediaRecorder.AudioSource.MIC
            )

            newRecorder.setOutputFormat(
                MediaRecorder.OutputFormat.MPEG_4
            )

            newRecorder.setAudioEncoder(
                MediaRecorder.AudioEncoder.AAC
            )

            newRecorder.setAudioEncodingBitRate(128000)
            newRecorder.setAudioSamplingRate(44100)
            newRecorder.setOutputFile(file.absolutePath)

            newRecorder.prepare()
            newRecorder.start()

            // The first amplitude reading normally returns zero.
            // Call once now so the next reading measures the recording.
            newRecorder.maxAmplitude

            recorder = newRecorder
            recordingFile = file
            recordingStartedAt = SystemClock.elapsedRealtime()

            Log.i(
                TAG,
                "RECORD STARTED: ${file.name}"
            )

        } catch (e: Exception) {
            Log.e(TAG, "RECORD START FAILED", e)

            newRecorder.release()
            file.delete()

            throw e
        }
    }

    fun stopRecording(): String? {
        val activeRecorder = recorder ?: return null
        val file = recordingFile

        val durationMs =
            SystemClock.elapsedRealtime() - recordingStartedAt

        // Measure microphone input before stopping the recorder.
        val peakAmplitude = try {
            activeRecorder.maxAmplitude
        } catch (e: Exception) {
            Log.e(TAG, "Amplitude check failed", e)
            -1
        }

        recorder = null
        recordingFile = null

        return try {
            activeRecorder.stop()

            val fileSize = file?.length() ?: 0L

            Log.i(
                TAG,
                "RECORD STOPPED: " +
                        "duration=${durationMs}ms, " +
                        "peakAmplitude=$peakAmplitude, " +
                        "fileSize=$fileSize bytes"
            )

            if (file != null && file.exists() && fileSize > 0L) {
                if (peakAmplitude == 0) {
                    Log.w(
                        TAG,
                        "NO AUDIO SIGNAL DETECTED: Check microphone input."
                    )
                }

                file.absolutePath
            } else {
                file?.delete()
                null
            }

        } catch (e: RuntimeException) {
            Log.e(TAG, "RECORD STOP FAILED", e)
            file?.delete()
            null

        } finally {
            activeRecorder.release()
        }
    }

    fun cancelRecording() {
        val activeRecorder = recorder
        val file = recordingFile

        recorder = null
        recordingFile = null

        if (activeRecorder != null) {
            try {
                activeRecorder.stop()
            } catch (_: RuntimeException) {
                // Ignore incomplete recordings.
            } finally {
                activeRecorder.release()
            }
        }

        file?.delete()
    }

    fun playRecording(
        filePath: String,
        onFinished: () -> Unit = {}
    ) {
        stopPlayback()

        val file = File(filePath)

        Log.i(
            TAG,
            "PLAY REQUEST: " +
                    "exists=${file.exists()}, " +
                    "fileSize=${if (file.exists()) file.length() else 0L} bytes"
        )

        if (!file.exists() || !file.isFile) {
            throw IOException("Voice note file not found.")
        }

        val newPlayer = MediaPlayer()

        try {
            newPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(
                        AudioAttributes.CONTENT_TYPE_SPEECH
                    )
                    .build()
            )

            FileInputStream(file).use { input ->
                newPlayer.setDataSource(input.fd)
            }

            newPlayer.setOnCompletionListener {
                Log.i(TAG, "PLAYBACK COMPLETED")
                stopPlayback()
                onFinished()
            }

            newPlayer.setOnErrorListener { _, what, extra ->
                Log.e(
                    TAG,
                    "PLAYBACK ERROR: what=$what extra=$extra"
                )
                stopPlayback()
                onFinished()
                true
            }

            newPlayer.prepare()

            Log.i(
                TAG,
                "PLAYBACK PREPARED: duration=${newPlayer.duration}ms"
            )

            player = newPlayer
            newPlayer.start()

            Log.i(TAG, "PLAYBACK STARTED")

        } catch (e: Exception) {
            Log.e(TAG, "PLAYBACK FAILED", e)

            player = null
            newPlayer.release()

            throw e
        }
    }

    fun stopPlayback() {
        val activePlayer = player
        player = null
        activePlayer?.release()
    }

    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
