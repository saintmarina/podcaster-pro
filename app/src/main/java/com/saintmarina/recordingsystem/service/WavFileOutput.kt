package com.saintmarina.recordingsystem.service

import android.util.Log
import com.saintmarina.recordingsystem.Util.nanosToSec
import com.saintmarina.recordingsystem.Util.prettyDuration
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

private const val HEADER_SIZE: Int = 44
private const val BITS_PER_SAMPLE: Short = 32
private const val NUM_CHANNELS: Short = 1
private const val FILE_NAME_FMT: String = "yyyy MMM d"
private const val TAG = "WavFileOutput"
private const val RANDOM_LEN = 4
private val ALLOWED_CHARS = ('A'..'Z') + ('a'..'z') + ('0'..'9')


class WavFileOutput(private val recordingDir: File): Closeable {
    private var output: FileOutputStream
    var file: File
    private val baseName = getBaseName()

    init {
        // recordingDir already exists, because it is created in service onCreate()

        val tempFileName = "${baseName}_recovery_file_${getRandomString(RANDOM_LEN)}.wav"
        file = File(recordingDir, tempFileName)
        Log.i(TAG, "WaveFileOutput $file created")

        output = FileOutputStream(file)
        output.channel.position(HEADER_SIZE.toLong())
    }

    override fun close() {
        writeWavHeader()
        output.flush()
        output.close()
        Log.i(TAG, "WaveFileOutput $file closed")
    }

    private fun writeWavHeader() {
        val dataSize = output.channel.position().toInt() - HEADER_SIZE

        val header = ByteBuffer.allocate(HEADER_SIZE)
            .apply {
                order(ByteOrder.LITTLE_ENDIAN)
                putInt(0x46464952) // "RIFF"
                putInt(dataSize + HEADER_SIZE)
                putInt(0x45564157) // "WAVE"
                putInt(0x20746d66) // "fmt "
                putInt(16) // Length of format data
                putShort(3) // floating point PCM
                putShort(NUM_CHANNELS)
                putInt(SAMPLE_RATE)
                putInt(SAMPLE_RATE * BITS_PER_SAMPLE /8 * NUM_CHANNELS)
                putShort((BITS_PER_SAMPLE /8 * NUM_CHANNELS).toShort())
                putShort(BITS_PER_SAMPLE)
                putInt(0x61746164) // "data"
                putInt(dataSize)

                assert(position() == HEADER_SIZE)
                position(0)
            }

        output.channel.write(header, 0)
    }

    fun write(buf: FloatArray, len: Int) {
        // Sadly, we must do a memory copy due to the endianness
        val byteBuf = ByteBuffer.allocate(4 * len) // 4 is sizeof(Float)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                asFloatBuffer().put(buf, 0, len)
            }
        output.channel.write(byteBuf)
    }

    fun renameToDatedFile(duration: Long) {
        val fileIndex = numWavFilesStartingWith() + 1
        val prettyDuration = prettyDuration(nanosToSec(duration))
        val fileName = "$baseName ($fileIndex) $prettyDuration.wav"
        val newFile = File(recordingDir, fileName)
        if (!file.renameTo(newFile))
            throw Exception("Failed to rename file")
        this.file = newFile
    }

    private fun numWavFilesStartingWith(): Int {
        return recordingDir.walk()
            .filter { it.name.startsWith(baseName) }
            .count()
    }

    private fun getBaseName(): String {
        val locale: Locale = Locale.getDefault()
        val formatter = SimpleDateFormat(FILE_NAME_FMT, locale)
        return formatter.format(Calendar.getInstance().time)
    }

    private fun getRandomString(length: Int) : String {
        return (1..length)
            .map { ALLOWED_CHARS.random() }
            .joinToString("")
    }
}