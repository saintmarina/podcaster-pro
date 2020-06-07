package com.saintmarina.recordingsystem.Service

import android.content.Context
import android.media.MediaPlayer
import com.saintmarina.recordingsystem.R

class SoundEffect(val context: Context) {
    private var startSound: MediaPlayer = MediaPlayer.create(context, R.raw.start_recording)
    private var stopSound: MediaPlayer = MediaPlayer.create(context, R.raw.stop_recording)

    fun playStartSound() {
        startSound.start()
    }

    fun playStopSound() {
        stopSound.start()
    }

    fun releaseSoundEffects() {
        startSound.release()
        stopSound.release()
    }
}