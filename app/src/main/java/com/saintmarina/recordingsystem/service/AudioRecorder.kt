package com.saintmarina.recordingsystem.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import java.io.Closeable

private const val TAG = "AudioRecorder"
const val NANOS_IN_SEC: Long = 1_000_000_000
const val INIT_TIMEOUT: Long = 5 * NANOS_IN_SEC

const val AUDIO_SOURCE: Int = MediaRecorder.AudioSource.MIC
const val SAMPLE_RATE: Int = 48000
const val CHANNEL: Int = AudioFormat.CHANNEL_IN_MONO
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
const val ENCODING: Int = AudioFormat.ENCODING_PCM_FLOAT
const val BUFFER_SIZE: Int = 1 * 1024 * 1024 // 2MB seems okay, 3MB makes AudioFlinger die with error -12 (ENOMEM) error
const val PUMP_BUF_SIZE: Int = 1*1024

@RequiresApi(Build.VERSION_CODES.P)
class AudioRecorder : Closeable, Thread() {
    var outputFile: WavFileOutput? = null
        @Synchronized set // Thread safe. Protects the outputFile to be set while it's being written to
        @Synchronized get

    private var terminationRequested: Boolean = false
    var peak: Float = 0F
    
    var onError: (() -> Unit)? = null
    var error: String = ""
        set(value) {
            field = value
            onError?.invoke()
        }

    init {
        name = "AudioRecorder pump"
        start()
    }

    override fun run() {
        val recorder = try {
            initRecorder()
        } catch (e: Exception) {
            Log.e(TAG, "$e")
            error = "${e.message}"
            return
        }

        try {
            mainLoop(recorder)
        } catch (e: Exception) {
            Log.e(TAG, "Audio capture failure: $e")
            error = "Audio capture failure: ${e.message}"
        }

        peak = 0F
        recorder.stop()
        recorder.release()
        Log.i(TAG, "Audio recorder stopped recording")
    }

    private fun initRecorder(): AudioRecord {
        /*
         * Sometimes the initialization of AudioRecord fails with ENOMEM
         * So we keep trying to initialize it with a 5 second timeout
         */
        val startTime = System.nanoTime()
        while (System.nanoTime() - startTime < INIT_TIMEOUT) {
            val recorder = AudioRecord(
                AUDIO_SOURCE,
                SAMPLE_RATE,
                CHANNEL,
                ENCODING,
                BUFFER_SIZE
            )

            if (recorder.state == AudioRecord.STATE_INITIALIZED) {
                recorder.startRecording()
                Log.i(TAG, "Audio recorder initialization successful")
                return recorder
            }
            Log.e(TAG, "Audio recorder initialization FAILED. Retrying")
            sleep(100)
        }

        throw IllegalStateException("AudioRecord failed to initialize")
    }

    private fun mainLoop(recorder: AudioRecord) {
        val buf = FloatArray(PUMP_BUF_SIZE)
        while (!terminationRequested) {
            val len = safeAudioRecordRead(recorder, buf)
            maybeWriteFile(buf, len)
            peak = getPeak(buf, len)
        }
    }

    private fun safeAudioRecordRead(recorder: AudioRecord, buf: FloatArray): Int {
        val len = recorder.read(buf, 0, buf.size, AudioRecord.READ_BLOCKING)
        if (len <= 0)
            throw IllegalStateException("AudioRecord.read() failed with $len")
        return len
    }

    @Synchronized private fun maybeWriteFile(buf: FloatArray, len: Int) {
        outputFile?.write(buf, len)
    }

    override fun close() {
        terminationRequested = true
        join()
    }

    private fun getPeak(buf: FloatArray, len: Int): Float {
        var maxValue = 0F
        buf.take(len).forEach {
            var value: Float = it

            if (value < 0)
                value = (-value)

            if (maxValue < value)
                maxValue = value
        }
        return maxValue
    }
}