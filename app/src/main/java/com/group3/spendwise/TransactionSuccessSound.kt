package com.group3.spendwise

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

// Plays a short confirmation chime when a transaction is saved.
class TransactionSuccessSound(context: Context) {

    // SoundPool is used instead of MediaPlayer because it is better suited for short, low-latency sound effects.
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(1)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    // ID of the loaded sound inside the SoundPool.
    private var soundId = 0
    // True once the sound file has finished loading.
    private var soundLoaded = false
    // Set when play() is called before loading finishes, so the sound plays as soon as it is ready.
    private var pendingPlay = false
    // Prevents using the SoundPool after it has been released.
    private var released = false

    init {
        // SoundPool loads asynchronously: an early play request waits for it.
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (!released && sampleId == soundId && status == 0) {
                soundLoaded = true
                if (pendingPlay) {
                    pendingPlay = false
                    play()
                }
            }
        }
        soundId = soundPool.load(context.applicationContext, R.raw.transaction_success, 1)
    }

    // Call only after Room confirms the transaction was saved or updated.
    fun play() {
        if (released) return
        if (!soundLoaded) {
            pendingPlay = true
            return
        }
        soundPool.play(soundId, 0.65f, 0.65f, 1, 0, 1.0f)
    }

    /** Frees the SoundPool. Call this when the owning screen is disposed. */
    fun release() {
        if (released) return
        released = true
        pendingPlay = false
        soundPool.setOnLoadCompleteListener(null)
        soundPool.release()
    }
}
