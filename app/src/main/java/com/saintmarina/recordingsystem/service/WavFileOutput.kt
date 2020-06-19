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
// TODO year comes first for sorting, then month, then day
private const val FILE_NAME_FMT: String = "d MMM yyyy"
private const val TAG = "WavFileOutput"

class WavFileOutput(private val recordingDir: File): Closeable {
    // TODO both should be private
    private var output: FileOutputStream
    var file: File

    // TODO reorder functions in something more sensible

    private fun getDataSize(): Int {
        return output.channel.position().toInt() - HEADER_SIZE
    }

    init {
        // TODO save getBaseName() in instance variable, so you can reuse it in renameToDatedFile().
        val tempFileName = "${getBaseName()}_recovery_file_${getRandomString(4)}.wav" // TODO that 4 should be const
        // recordingDir already exists, because it is created in CheckPermissionsActivity
        file = File(recordingDir, tempFileName)
        Log.i(TAG, "WaveFileOutput $file created")

        output = FileOutputStream(file)
        output.channel.position(HEADER_SIZE.toLong())
    }

    private fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9') // TODO make const
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    override fun close() {
        val header = generateWavHeader(getDataSize())
        output.channel.write(header, 0)
        output.flush()
        output.close()
        Log.i(TAG, "WaveFileOutput $file closed")
    }

    private fun numWavFilesStartingWith(basename: String): Int {
        return recordingDir.walk()
            .filter { it.name.startsWith(basename) && it.name.endsWith(".wav") } // TODO once database is done, take out the .wav filters
            .count()
    }

    fun renameToDatedFile(duration: Long) {
        val baseName = getBaseName()
        val fileIndex = numWavFilesStartingWith(baseName) + 1
        val prettyDuration = prettyDuration(nanosToSec(duration))
        val fileName = "$baseName ($fileIndex) ($prettyDuration).wav"
        val newFile = File(recordingDir, fileName)
        if (!file.renameTo(newFile))
            throw Exception("Failed to rename file. Contact the developer.") // TODO The UI should be the one responsible to put this error message (dev is Nico or Anna)
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
                putShort(1) // PCM // TODO 3 would mean float
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

    private fun getBaseName(): String {
        val locale: Locale = Locale.getDefault()
        val formatter = SimpleDateFormat(FILE_NAME_FMT, locale)
        return formatter.format(Calendar.getInstance().time)
    }
}