package com.example.recordingsystem

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

const val FILE_NAME_FMT: String = "yyyy-MM-dd_HH-mm-ss'.wav'"
//const val FILE_NAME_FMT: String = "'test.wav'"
const val RECORDINGS_DIR_PATH: String = "/sdcard/Recordings/"

class WavFileOutput: Closeable {
    var file: FileOutputStream

    private fun getDataSize(): Int {
        return file.channel.position().toInt() - HEADER_SIZE
    }

    init {
        file = createDatedFile()
        file.channel.position(HEADER_SIZE.toLong())
    }

    override fun close() {
        val header = generateWavHeader(getDataSize())
        file.channel.write(header, 0)
        file.flush()
        file.close()
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
                putInt(SAMPLE_RATE * BITS_PER_SAMPLE/8 * NUM_CHANNELS)
                putShort((BITS_PER_SAMPLE/8 * NUM_CHANNELS).toShort())
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
        file.channel.write(byteBuf)
    }

    private fun createDatedFile() : FileOutputStream {
        //Filename in a date format
        val date = getCurrentDateTime()
        val filename = date.toString(FILE_NAME_FMT)

        // Creating Recording directory if it doesn't exist
        val recordingsDir = File(RECORDINGS_DIR_PATH)
        recordingsDir.mkdirs()

        // Create a File
        val outputFile = File(recordingsDir, filename)

        return FileOutputStream(outputFile)
    }

    fun Date.toString(format: String, locale: Locale = Locale.getDefault()): String {
        val formatter = SimpleDateFormat(format, locale)
        return formatter.format(this)
    }

    fun getCurrentDateTime(): Date {
        return Calendar.getInstance().time
    }
}