
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

/**
 * Records and plays back voice notes that can be attached to a transaction.
 * Recordings are saved as .m4a files in the app's private storage (filesDir/voice_notes),
 * so other apps can't access them. Call [release] when the screen using this is closed.
 */
class VoiceNoteRecorder(context: Context) {

    companion object {
        private const val TAG = "SpendWiseVoice"
    }

    private val appContext = context.applicationContext

    // Active recorder, or null when not recording.
    private var recorder: MediaRecorder? = null
    // Active player, or null when nothing is playing.
    private var player: MediaPlayer? = null
    // File the current recording is being written to.
    private var recordingFile: File? = null
    // Time the recording started, used to log its length.
    private var recordingStartedAt = 0L

    val isRecording: Boolean
        get() = recorder != null

    val isPlaying: Boolean
        get() = player?.isPlaying == true

    @Suppress("DEPRECATION")
    // MediaRecorder(Context) was added in Android 12 (S); older versions use the deprecated no-arg constructor.
    private fun createRecorder(): MediaRecorder {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(appContext)
        } else {
            MediaRecorder()
        }
    }

    /**
     * Starts recording from the microphone into a new uniquely named file.
     * Requires the RECORD_AUDIO permission. Throws if a recording is already running or setup fails.
     */
    fun startRecording() {
        check(!isRecording) {
            "A recording is already in progress."
        }

        // Stop any playback so it doesn't get picked up by the microphone.
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

        // Configure the recorder: mic input, MP4 container, AAC audio at 128 kbps / 44.1 kHz.
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

        // Clean up the recorder and the half-written file if anything went wrong.
        } catch (e: Exception) {
            Log.e(TAG, "RECORD START FAILED", e)

            newRecorder.release()
            file.delete()

            throw e
        }
    }

    /**
     * Stops recording and returns the saved file's absolute path,
     * or null if nothing was recorded or saving failed (the file is deleted in that case).
     */
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

        // Clear the fields first so isRecording becomes false even if stop() throws.
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

            // Only keep the file if it actually contains data.
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

        // Always free the recorder's native resources.
        } finally {
            activeRecorder.release()
        }
    }

    /** Stops the current recording without keeping it, and deletes the file. */
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

    /**
     * Plays the voice note saved at [filePath].
     * @param onFinished Called when playback ends or fails, so the UI can reset its play button.
     */
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

            // Pass a file descriptor so MediaPlayer can read our private file directly.
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

            // prepare() loads the file synchronously; fine here because voice notes are short.
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

    /** Stops and releases the media player, if one is active. */
    fun stopPlayback() {
        val activePlayer = player
        player = null
        activePlayer?.release()
    }

    /** Stops everything and frees resources. Call when the owning screen is disposed. */
    fun release() {
        cancelRecording()
        stopPlayback()
    }
}
