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
const val INIT_TIMEOUT: Long = 5* NANOS_IN_SEC

const val AUDIO_SOURCE: Int = MediaRecorder.AudioSource.MIC
const val SAMPLE_RATE: Int = 48000
const val CHANNEL: Int = AudioFormat.CHANNEL_IN_MONO
const val ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
const val BUFFER_SIZE: Int = 1 * 1024 * 1024 // 2MB seems okay, 3MB makes AudioFlinger die with error -12 (ENOMEM) error
const val PUMP_BUF_SIZE: Int = 1*1024

@RequiresApi(Build.VERSION_CODES.P)
class AudioRecorder : Closeable {
    private var thread: Thread
    var outputFile: WavFileOutput? = null
        @Synchronized set // Thread safe. Protects the outputFile to be set while it's being written to
        @Synchronized get

    private var terminationRequested: Boolean = false
    var peak: Short = 0

    init {
        val recorder = initRecorder()
        thread = Thread {
            recorder.startRecording()
            Log.i(TAG, "Audio recorder started")
            val buf = ShortArray(PUMP_BUF_SIZE)
            while (!terminationRequested) {
                val len = safeAudioRecordRead(recorder, buf)
                maybeWriteFile(buf, len)
                peak = getPeak(buf, len)
            }
            recorder.stop()
            Log.i(TAG, "Audio recorder stopped recording")
            recorder.release()
            // TODO catch all exceptions and notify the service through a callback. The service needs to stop the recording and show the error on the UI. Once done set peak to 0
        }.apply {
            name = "AudioRecorder pump"
            start()
        }
    }

    @Synchronized private fun maybeWriteFile(buf: ShortArray, len: Int) {
        outputFile?.write(buf, len)
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
                Log.i(TAG, "Audio recorder initialization successful")
                return recorder
            }
            Log.e(TAG, "Audio recorder initialization FAILED. Retrying")

            Thread.sleep(100)
        }
        throw IllegalStateException("AudioRecord failed to initialize")
    }

    private fun safeAudioRecordRead(recorder: AudioRecord, buf: ShortArray): Int {
        val len = recorder.read(buf, 0, buf.size)
        if (len <= 0)
            throw IllegalStateException("AudioRecord.read() failed with $len")

        return len
    }

    override fun close() {
        terminationRequested = true;
        thread.join()
    }

    private fun getPeak(buf: ShortArray, len: Int): Short {
        var maxValue: Short = 0
        buf.take(len).forEach {
            var value: Short = it

            if (value < 0)
                value = (-value).toShort()

            if (maxValue < value)
                maxValue = value
        }
        return maxValue
    }
}