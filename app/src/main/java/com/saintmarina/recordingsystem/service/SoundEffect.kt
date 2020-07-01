package com.saintmarina.recordingsystem.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.saintmarina.recordingsystem.R
import java.lang.RuntimeException

private const val TAG = "SoundEffect"

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class SoundEffect(val context: Context) {
    private val sound = SoundPool.Builder().setMaxStreams(5).build()

    private val bipbip = sound.load(context, R.raw.bipbip, 1)
    private val vase = sound.load(context, R.raw.vase, 1)
    private val gong = sound.load(context, R.raw.gong, 1)

    fun playStartSound() {
        Log.i(TAG, "playStart()")
        sound.play(vase, 1f, 1f, 1, 0, 1f)
    }

    fun playStopSound() {
        Log.i(TAG, "playStopSound()")
        sound.play(gong, 1f, 1f, 1, 0, 1f)
    }

    fun playPauseSound() {
        Log.i(TAG, "playPauseSound()")
        sound.play(bipbip, 1f, 1f, 1, 0, 1f)
    }

    fun playResumeSound() {
        Log.i(TAG, "playResumeSound()")
        sound.play(bipbip, 1f, 1f, 1, 0, 1f)
    }

    fun releaseSoundEffects() {
        Log.i(TAG, "release()")
        sound.release()
    }
}