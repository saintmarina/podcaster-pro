package com.example.recordingsystem

import android.content.Context
import android.media.MediaPlayer
import android.view.View

class SoundEffect(val context: Context) {
    private var startSound: MediaPlayer? = null
    private var stopSound: MediaPlayer? = null

    fun playStartSound() {
        if (startSound == null)
            startSound = MediaPlayer.create(context, R.raw.start_recording)

        startSound!!.start()
    }

    fun playStopSound() {
        if (stopSound == null)
            stopSound = MediaPlayer.create(context, R.raw.stop_recording)

        stopSound!!.start()
    }

    fun releaseSoundEffects() {
        startSound!!.release()
        stopSound!!.release()
    }


}