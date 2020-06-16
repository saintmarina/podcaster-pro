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
private const val BITS_PER_SAMPLE: Short = 16
private const val NUM_CHANNELS: Short = 1
private const val FILE_NAME_FMT: String = "d MMM yyyy"
private const val TAG = "WavFileOutput"

class WavFileOutput(private val localDir: String): Closeable {
    private var output: FileOutputStream
    lateinit var file: File

    private fun getDataSize(): Int {
        return output.channel.position().toInt() - HEADER_SIZE
    }

    init {
        output = createDatedFile()
        output.channel.position(HEADER_SIZE.toLong())
    }

    fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun createDatedFile() : FileOutputStream {
        // Creating Recording directory if it doesn't exist
        val recordingsDir = File(localDir)
        recordingsDir.mkdirs()

        val baseName = getCurrentDateTime().toString(FILE_NAME_FMT)
        val tempFileName = "${baseName}_recovery_file_${getRandomString(4)}.wav"

        file = File(recordingsDir, tempFileName)
        Log.i(TAG, "WaveFileOutput $file created")
        return FileOutputStream(file)
    }

    override fun close() {
        val header = generateWavHeader(getDataSize())
        output.channel.write(header, 0)
        output.flush()
        output.close()
        Log.i(TAG, "WaveFileOutput $file closed")
    }

    private fun numOfSameDayFiles(basename: String): Int {
        return File(localDir).walk()
            .filter { f -> f.isFile && f.name.startsWith(basename) && f.name.endsWith(".wav") }
            .count()
    }

    fun renameToDatedFile(duration: Long) {
        val baseName = getCurrentDateTime().toString(FILE_NAME_FMT)
        val fileIndex = numOfSameDayFiles(baseName) + 1
        val prettyDuration = prettyDuration(nanosToSec(duration))
        val fileName = "$baseName ($fileIndex) ($prettyDuration).wav"
        val newFile = File(localDir, fileName)
        if (!file.renameTo(newFile))
            throw Exception("Failed to rename file. Contact the developer.")
        this.file = newFile
    }

    private fun generateWavHeader(dataSize: Int): ByteBuffer {
        return ByteBuffer.allocate(HEADER_SIZE)
            .apply {
                order(ByteOrder.LITTLE_ENDIAN)
                putInt(0x46464952) // "RIFF"
                putInt(dataSize + HEADER_SIZE)
                putInt(0x45564157) // "WAVE"
                putInt(0x20746d66) // "fmt "
                putInt(16) // Length of format data
                putShort(1) // PCM
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
    }

    fun write(buf: ShortArray, len: Int) {
        // Sadly, we must do a memory copy due to the endianness
        val byteBuf = ByteBuffer.allocate(2 * len)
            .order(ByteOrder.LITTLE_ENDIAN)
            .apply {
                asShortBuffer().put(buf, 0, len)
            }
        output.channel.write(byteBuf)
    }

    private fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    private fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }
}