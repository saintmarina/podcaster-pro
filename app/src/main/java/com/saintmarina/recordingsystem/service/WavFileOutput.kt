package com.saintmarina.recordingsystem.service

import android.util.Log
import com.saintmarina.recordingsystem.Util.nanosToSec
import com.saintmarina.recordingsystem.Util.prettyDuration
import java.io.Closeable
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.SimpleDateFormat
import java.util.*

const val HEADER_SIZE: Int = 44
const val BITS_PER_SAMPLE: Short = 16
const val NUM_CHANNELS: Short = 1


// TODO rename the file
// TODO name: 2020 May 1st (3 mins).wav
const val FILE_NAME_FMT: String = "d MMM yyyy'.wav'"
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

    private fun createDatedFile() : FileOutputStream {
        // Creating Recording directory if it doesn't exist
        val recordingsDir = File(localDir)
        recordingsDir.mkdirs()


        // we loop until we find a non-existing filename which could happen when the
        // user presses start/stop/start quickly
        for (i in 0..Int.MAX_VALUE) {
            var filename = getCurrentDateTime().toString(FILE_NAME_FMT)
            if (i > 0)
                filename = filename.replace(".", " ($i).")
            file = File(recordingsDir, filename)
            if (!file.exists())
                break
        }

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

    fun addDurationToFileName(duration: Long) {
        val newFileName = file.name.replace(".", " (${prettyDuration(nanosToSec(duration))}).")
        Log.d(TAG, "newFileName = $newFileName")
        Log.d(TAG, "file parent = ${file.parent}")
        Log.d(TAG, "file path = ${file.path}")
        val newFile = File(file.parent, "/$newFileName")
        val renameSuccess = file.renameTo(newFile)
        Log.d(TAG, "rename Success = $renameSuccess")
        this.file = newFile
        Log.d(TAG, "this.file = ${this.file}")
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
        Log.d(TAG, "time ${Calendar.getInstance().time}")
        return Calendar.getInstance().time
    }
}