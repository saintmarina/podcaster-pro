package com.example.recordingsystem

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.util.Log
import java.lang.IllegalStateException
import java.lang.Math.abs
import kotlin.reflect.typeOf

const val audioSource: Int = MediaRecorder.AudioSource.MIC
const val sampleRate: Int = 44100
const val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO
const val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
const val bufferSize: Int = 1 * 1024 * 1024 // 2MB seems okay, 3MB makes AudioFlinger dies with error -12 (ENOMEM) error
const val LOG_TAG = "AudioRecorder"

class AudioRecorder {
    var stopRequested = false
    var bufSize: Int = 1*1024
    var thread: Thread? = null
    var peak: Short = 0

    val isRecording: Boolean
        get() {
            return thread != null;
        }

    fun startRecording() {
        if (thread != null)
            throw IllegalStateException("start() was called twice in a row without calling stop()")

        stopRequested = false
        thread = Thread {
            val recorder = initRecorder()
            recorder.startRecording()

            val buf = ShortArray(bufSize)
            while (!stopRequested) {
                val len = safeAudioRecordRead(recorder, buf)
                peak = getPeak(buf, len)
                Log.i(LOG_TAG, "Peak is $peak")
            }
        }
        thread!!.apply {
            name = "AudioRecorder pump"
            start()
        }
        Log.i("State", "Started!")
    }

    private fun initRecorder(): AudioRecord {
        val recorder = AudioRecord(
            audioSource,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED)
            throw IllegalStateException("AudioRecord failed to initialize")

        return recorder
    }

    private fun safeAudioRecordRead(recorder: AudioRecord, buf: ShortArray): Int {
        val len = recorder.read(buf, 0, bufSize)

        if (len <= 0)
            throw IllegalStateException("AudioRecord.read() failed with $len")

        return len
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

    fun writeAudioDataToFile() {
        //TODO
    }

    fun stop() {
        if (thread == null || stopRequested)
            throw IllegalStateException("stop() was called twice in a row without calling start()")

        Log.i(LOG_TAG, "Stop requested")
        stopRequested = true

        thread!!.join()
        thread = null

        Log.i(LOG_TAG, "Thread stopped")
    }
}

/*

private void writeAudioDataToFile(){
    byte data[] = new byte[bufferSize];
    String filename = getTempFilename();
    FileOutputStream os = null;

    try {
        os = new FileOutputStream(filename);
    } catch (FileNotFoundException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }

    int read = 0;

    if(null != os){
        while(isRecording){
            read = recorder.read(data, 0, bufferSize);

            if(AudioRecord.ERROR_INVALID_OPERATION != read){
                try {
                    os.write(data);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

private void stopRecording(){
if(null != recorder){
isRecording = false;

int i = recorder.getState();
if(i==1)
recorder.stop();
recorder.release();

recorder = null;
recordingThread = null;
}

 */



/*

fun start() {
        stop()

        thread = Thread{
            isRunning = true
            var count: Int = 0
            var audiorec = AudioRecord(10MB);
            var audiofile = Storage.create_file("20120-123-123.wav")
            var buffer: ShortArray = 4k;
            while (isRunning) {
                var buffer_size = audiorec.read(buffer)
                audiofile.write(buffer, buffer_size)
            }
        }
        thread?.start()
        Log.i("State", "Started!")
    }
 */
    /*

    private var RECORDER_BPP: Int = 16


    private var AUDIO_RECORDER_FILE_EXT_WAV: String = ".wav"
    private var AUDIO_RECORDER_FOLDER: String = "AudioRecorder"
    private var AUDIO_RECORDER_TEMP_FILE: String = "record_temp.raw"

    private var recordingThread: Thread? = null;
    private var isRecording: Boolean = false

    lateinit var recorder: AudioRecord;
    lateinit var audioData: ShortArray

    var bufferData: Array<Int>? = null
    var bytesRecorded: Int = 0;

    run()
    {
        Thread.sleep(1000)
    }

    audioData = ShortArray(bufferSize) //short array that pcm data is put into.

    /*    output = File("/sdcard/test1.mp4a")


    fun getFilename(): String {
        var file: File = File("/sdcard/",AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() +
                AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }*/

    fun startOrStopRecording(): Unit {
        recorder = AudioRecord(MediaRecorder.AudioSource.MIC,
        RECORDER_SAMPLERATE,
        RECORDER_CHANNELS,
        RECORDER_AUDIO_ENCODING,
        bufferSize)


        isRecording = true;

        recordingThread =  Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");

        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;
        if (null != os) {
            while(isRecording) {
                read = recorder.read(data, 0, bufferSize);
                if (read > 0){
                }

                if (AudioRecord.ERROR_INVALID_OPERATION != read) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stopRecording() {
        if (null != recorder){
            isRecording = false;

            int i = recorder.getState();
            if (i==1)
                recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(),getFilename());
        deleteTempFile();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                longSampleRate, channels, byteRate);

            while(in.read(data) != -1) {
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
    FileOutputStream out, long totalAudioLen,
    long totalDataLen, long longSampleRate, int channels,
    long byteRate) throws IOException
    {
        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(v.getId()){
                case R.id.btnStart:{
                    AppLog.logString("Start Recording");

                    enableButtons(true);
                    startRecording();

                    break;
                }
                case R.id.btnStop:{
                    AppLog.logString("Start Recording");

                    enableButtons(false);
                    stopRecording();

                    break;
                }
            }
        }
    };
}
*/
