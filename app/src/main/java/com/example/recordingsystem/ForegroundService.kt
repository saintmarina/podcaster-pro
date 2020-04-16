package com.example.recordingsystem

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.AvailableNetworkInfo.PRIORITY_HIGH
import android.util.Log
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat

const val TAG = "FOREGROUND_SERVICE"
const val ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE"

const val ACTION_OPEN_APP = "ACTION_OPEN_APP"
const val KEY_DATA = "KEY_DATA"

private const val CHANNEL_ID: String = "1001"
private const val CHANNEL_NAME: String = "Event Tracker"
private const val SERVICE_ID: Int = 1
private const val ONE_MIN_MILLI: Long = 60000  //1min

// https://stackoverflow.com/questions/3305088/how-to-make-notification-intent-resume-rather-than-making-a-new-intent
// on this page people solving the same problem - "Resuming the application after clicking on the Notification
// the last thing that was done is:
//Changed this:
/*
    var notificationIntent = Intent(this, RecordingSystemActivity::class.java)
    var pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
 */
// To this:
/*
    var notificationIntent = Intent(applicationContext, RecordingSystemActivity::class.java)
    notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    var pendingIntent = PendingIntent.getActivity(applicationContext, 0, notificationIntent, 0)
 */

// The context was changed from Service context to applicationContext
// New flags were set for notificationIntent



class ForegroundService(): Service() {


    override fun onCreate() {
        super.onCreate()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())

        return START_NOT_STICKY
    }

    private fun createContentIntent(): PendingIntent {
        val notificationIntent = Intent(applicationContext, RecordingSystemActivity::class.java)
        notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP

        return PendingIntent.getActivity(applicationContext, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun createNotification(): Notification {
        val pendingIntent = createContentIntent()
        //val customView = RemoteViews(packageName, R.layout.custom_notification)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID).apply {
            setContentTitle("Notification")
            setSmallIcon(R.drawable.ic_stat_name)
            setContentText("Slide down on note to expand")
            setContentIntent(pendingIntent)

           // setContentIntent(pendingIntent)
            //setSound(null)
            //setStyle(NotificationCompat.DecoratedCustomViewStyle())
            priority = PRIORITY_HIGH
            //setContent(customView)
        }
        return notification.build()
    }



    @RequiresApi(Build.VERSION_CODES.O)

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
/*
    private fun getStickyNotification(): Notification {

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            startForeground(SERVICE_ID, getStickyNotification())

            Log.e(TAG, "${isServiceRunningInForeground(this, AudioRecorder::class.java)}")

            return super.onStartCommand(intent, flags, startId)
        }



        fun isServiceRunningInForeground(
            context: Context,
            serviceClass: Class<*>
        ): Boolean {
            val manager =
                context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (serviceClass.name == service.service.className) {
                    if (service.foreground) {
                        return true
                    }
                }
            }
            return false
        }

private fun createNotificationChannel() {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = getString(R.string.channel_name)
        val descriptionText = getString(R.string.channel_description)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

private fun startForegroundServices() {
    createNotificationChannel()

    val title = "Recording System"
    val description = "Recording an audio now"


    var builder = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_stat_name)
        .setContentTitle("My notification")
        .setContentText("Much longer text that cannot fit one line...")
        .setStyle(
            NotificationCompat.BigTextStyle()
            .bigText("Much longer text that cannot fit one line..."))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)

    // Build the notification.
    return builder.build()
}

/*override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

    Log.d(TAG, "ON START COMMAND")

    if (intent != null) {

        when (intent.action) {

            ACTION_STOP_FOREGROUND_SERVICE -> {
                stopService()
            }

            ACTION_OPEN_APP -> openAppHomePage(intent.getStringExtra(KEY_DATA))
        }
    }
    return START_STICKY;
}*/

/*private fun openAppHomePage(value: String) {

    val intent = Intent(applicationContext, RecordingSystemActivity::class.java)
    intent.putExtra(KEY_DATA, value)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}*/

private fun stopService() {
    // Stop foreground service and remove the notification.
    stopForeground(true)
    // Stop the foreground service.
    stopSelf()
}

override fun onDestroy() {
    stopService()
    close()
}
}


const val LOG_TAG = "AudioRecorder"
const val AUDIO_SOURCE: Int = MediaRecorder.AudioSource.MIC
const val SAMPLE_RATE: Int = 48000
const val CHANNEL: Int = AudioFormat.CHANNEL_IN_MONO
const val ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
const val BUFFER_SIZE: Int = 1 * 1024 * 1024 // 2MB seems okay, 3MB makes AudioFlinger die with error -12 (ENOMEM) error
const val PUMP_BUF_SIZE: Int = 1*1024

const val NANOS_IN_SEC: Long = 1_000_000_000
const val INIT_TIMEOUT: Long = 5*NANOS_IN_SEC


@RequiresApi(Build.VERSION_CODES.P)
class AudioRecorder() : Closeable {
    private var thread: Thread
    var outputFile: WavFileOutput? = null
        @Synchronized set
        @Synchronized get

    var terminationRequested: Boolean = false
    var peak: Short = 0

    init {
        val recorder = initRecorder()
        thread = Thread {
            recorder.startRecording()

            val buf = ShortArray(PUMP_BUF_SIZE)
            while (!terminationRequested) {
                val len = safeAudioRecordRead(recorder, buf)
                maybeWriteFile(buf, len)
                peak = getPeak(buf, len)
            }
            recorder.stop()
            recorder.release()
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
            val recorder = AudioRecord(AUDIO_SOURCE, SAMPLE_RATE,
                CHANNEL, ENCODING, BUFFER_SIZE)

            if (recorder.state == AudioRecord.STATE_INITIALIZED)
                return recorder

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























package com.example.recordingsystem

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer
import java.security.AccessController.getContext
import java.text.SimpleDateFormat
import java.util.*

const val LOG_TAG = "AudioRecorder"
const val AUDIO_SOURCE: Int = MediaRecorder.AudioSource.MIC
const val SAMPLE_RATE: Int = 48000
const val CHANNEL: Int = AudioFormat.CHANNEL_IN_MONO
const val ENCODING: Int = AudioFormat.ENCODING_PCM_16BIT
const val BUFFER_SIZE: Int = 1 * 1024 * 1024 // 2MB seems okay, 3MB makes AudioFlinger die with error -12 (ENOMEM) error
const val PUMP_BUF_SIZE: Int = 1*1024

const val NANOS_IN_SEC: Long = 1_000_000_000
const val INIT_TIMEOUT: Long = 5*NANOS_IN_SEC

private const val CHANNEL_ID: String = "1001"
private const val SERVICE_ID: Int = 1


@RequiresApi(Build.VERSION_CODES.P)
class AudioRecorder() : Service(), Closeable {
    private var thread: Thread
    var outputFile: WavFileOutput? = null
        @Synchronized set
        @Synchronized get

    var terminationRequested: Boolean = false
    var peak: Short = 0

    init {
        val recorder = initRecorder()
        thread = Thread {
            recorder.startRecording()

            val buf = ShortArray(PUMP_BUF_SIZE)
            while (!terminationRequested) {
                val len = safeAudioRecordRead(recorder, buf)
                maybeWriteFile(buf, len)
                peak = getPeak(buf, len)
            }
            recorder.stop()
            recorder.release()
        }.apply {
            name = "AudioRecorder pump"
            start()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        val pendingIntent: PendingIntent =
            Intent(this, RecordingSystemActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val notification: Notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording")
            .setContentText("Hello!")
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(pendingIntent)
            .setTicker("Hello World!")
            .build()

        startForeground(SERVICE_ID, notification)

        super.onCreate()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
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
            val recorder = AudioRecord(AUDIO_SOURCE, SAMPLE_RATE,
                CHANNEL, ENCODING, BUFFER_SIZE)

            if (recorder.state == AudioRecord.STATE_INITIALIZED)
                return recorder

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

    private fun stopService() {
        // Stop foreground service and remove the notification.
        stopForeground(true)
        // Stop the foreground service.
        stopSelf()
    }

    override fun onDestroy() {
        stopService()
        close()
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
 */
