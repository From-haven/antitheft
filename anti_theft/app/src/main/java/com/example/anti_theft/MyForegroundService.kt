package com.example.anti_theft

import android.app.*
import android.content.Intent
import android.hardware.SensorEventListener
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import android.os.PowerManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.media.MediaPlayer
//count time
import android.os.CountDownTimer
import kotlin.math.abs
import android.media.RingtoneManager
import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioManager
//list choose file
import android.os.Build
import java.io.FileNotFoundException


class MyForegroundService : Service(), SensorEventListener {

    private val CHANNEL_ID = "my_foreground_channel"
    //acerlerator sensor
    //value of sensor accelerator
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    //value of time countdown
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 15000 // Biến lưu thời gian đếm ngược (10 giây)
    private var isRunning = false
    private var saveTime : Long = 15000

    //lọc tốc độ đọc cảm biến
    private val alpha = 0.8f // Hệ số làm mượt (0.8 là giá trị tối ưu)
    private var gravity = floatArrayOf(0f, 0f, 0f) // Lưu giá trị đã lọc
    //algorithm
    private var X : Float = 0.00f
    private var Y : Float = 0.00f
    private var Z : Float = 0.00f

    private var lastUpdateTime = 0L  // Lưu thời gian cập nhật cuối
    private val updateInterval = 300  // Giảm tốc độ cập nhật (milliseconds)
    private var valuelist: Int = 1   // value default

    private lateinit var mediaPlayer: MediaPlayer   //điểu khiển âm thanh
    private var ringtone: android.media.Ringtone? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var seekbarValue: Int = 4

    //read From data (set time and valuelist)
//    val content = openFileInput("data.txt").bufferedReader().use { it.readText() }
//    val lines = content.lines()

//    saveTime  = lines[0].split("=")[1].toLong()
//    private var timeLeftInMillis: Long = saveTime
//    var valuelist: Int = lines[1].split("=")[1].toInt()



    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundWithNotification()

        readDataFromFile()

        //sensor acelerator
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        // Đăng ký lắng nghe cảm biến khi Activity hoạt động

        startNewTimer()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // Khởi tạo wakelock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "AntiTheftApp::MyWakelockTag"
        )
        wakeLock?.acquire()


        if (valuelist == 1) {
            val afd = resources.openRawResourceFd(R.raw.sound_alarm)
            mediaPlayer = MediaPlayer()

            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)  // Quan trọng: sử dụng đúng kênh âm thanh báo thức
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mediaPlayer.isLooping  = true
            mediaPlayer.prepare()
        }else if(valuelist == 2){
            //alarm system
            val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val ringtoneUri = alarmUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)


            //other alarm_sound system
            val audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
            val result = audioManager.requestAudioFocus(
                { /* Audio focus change listener */ },
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
            )
        }else if(valuelist == 3) {
            val afd = resources.openRawResourceFd(R.raw.iphone_original_ringtone)
            mediaPlayer = MediaPlayer()

            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()

            mediaPlayer.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)  // Quan trọng: sử dụng đúng kênh âm thanh báo thức
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            mediaPlayer.isLooping = true
            mediaPlayer.prepare()
        }

//        mediaPlayer = MediaPlayer.create(this, R.raw.sound_alarm)
//        mediaPlayer.setAudioAttributes(
//            AudioAttributes.Builder()
//                .setUsage(AudioAttributes.USAGE_ALARM)
//                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
//                .build()
//        )// mediaPlayer.isLooping = true  // Nếu cần lặp lại như báo thức



    }
    interface SensorCallback {
        fun onSensorData(x: Float, y: Float, z: Float, running: Boolean, timeLeft: Long)
    }
    private var callback: SensorCallback? = null
    fun setCallback(cb: SensorCallback) {
        callback = cb
    }

    private fun updateNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anti_theft")
            .setContentText("count down timer: ${timeLeftInMillis/1000}")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setOngoing(true) // Không cho người dùng vuốt tắt
            .build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1, notification) // cập nhật ID 1 (cùng ID với startForeground)
    }


    private fun startForegroundWithNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Anti_theft")
            .setContentText("count down timer: ${timeLeftInMillis/1000}")
            .setSmallIcon(R.mipmap.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true) // Không cho người dùng vuốt tắt
            .build()

        startForeground(1, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Foreground Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // TODO: your logic here
        return START_STICKY
    }


    override fun onDestroy() {
        super.onDestroy()
        // Nếu bạn đã đăng ký sensor listener

        // Nếu bạn đã sử dụng wakelock
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
        }
        sensorManager.unregisterListener(this)
        stopSound()
        countDownTimer?.cancel()
        Log.d("MyForegroundService", "Service destroyed")
    }


    override fun onSensorChanged(event: SensorEvent?) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdateTime >= updateInterval) {
            lastUpdateTime = currentTime

            event?.let {
                val rawX = it.values[0]
                val rawY = it.values[1]
                val rawZ = it.values[2]

                // Áp dụng bộ lọc trung bình
                gravity[0] = alpha * gravity[0] + (1 - alpha) * rawX
                gravity[1] = alpha * gravity[1] + (1 - alpha) * rawY
                gravity[2] = alpha * gravity[2] + (1 - alpha) * rawZ

                val checkX = gravity[0]
                val checkY = gravity[1]
                val checkZ = gravity[2]

                callback?.onSensorData(X, Y, Z,isRunning,timeLeftInMillis)

                //algorithm in this function are dangerous

                if(!isRunning) { //check before system is really turn on alarm
                    if(abs((checkX - X)) > 0.003f*(101-seekbarValue) && abs((checkY - Y)) > 0.003f*(101-seekbarValue) && abs((checkZ - Z)) > 0.001f*(101-seekbarValue))
                    {
                        resetTimer()
                        startNewTimer()
                    }
                    X = checkX
                    Y = checkY
                    Z = checkZ

                }else{
                    if(abs((checkX - X)) > 0.002f*(101-seekbarValue) && abs((checkY - Y)) > 0.002f*(101-seekbarValue) && abs((checkZ - Z)) > 0.001f*(101-seekbarValue))
                    {
                        playSound()
                        Log.d("prepare play sound","~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~")
                    }else{
                        X = checkX
                        Y = checkY
                        Z = checkZ
                    }
                }
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): MyForegroundService = this@MyForegroundService
    }
    private val binder = LocalBinder()

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Không cần xử lý trong trường hợp này
    }
    //override nay no đang mâu thuẫn với ondestroy
//    override fun onPause() {
//        super.onPause()
//        // Hủy đăng ký cảm biến khi Activity bị dừng để tiết kiệm pin
//        sensorManager.unregisterListener(this)
//    }

    private fun startNewTimer() {
        countDownTimer?.cancel() // Hủy nếu đang chạy
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateNotification()
                Log.d("countdowntime","------------------${timeLeftInMillis/1000}----------------")
                //updateText()
            }

            override fun onFinish() {
                isRunning = true
            }
        }.start()
    } //giống start timer

    private fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = saveTime // Reset về 10 giây
        //updateText()
    }

    private fun playSound() {
        if(valuelist == 1 || valuelist == 3) {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start() // 🔊 Play audio
            }
        }else {
            ringtone?.play()
        }
    }
    //set user's sound


    private fun stopSound() {
        if(valuelist == 1 || valuelist == 3) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop() // 🔊 Play audio
//                mediaPlayer.reset()
//                mediaPlayer.prepare()
            }
        }else{
            ringtone?.stop()
        }
    }

    //gửi dữ liệu

    //get file on input
    private fun readDataFromFile() {

        val content = openFileInput("data.txt").bufferedReader().use { it.readText() }
        val lines = content.lines()

        if (lines.size >= 2) {
            val saveTime_1 = lines[0].split("=").getOrNull(1)?.toLongOrNull()
            val valueList_1 = lines[1].split("=").getOrNull(1)?.toIntOrNull()
            val seekbarValue_1 = lines[2].split("=").getOrNull(1)?.toIntOrNull()
            //have a poiter themeslist at line 3 (lines[2])
            if (saveTime_1 != null && valueList_1 != null && seekbarValue_1 != null) {
                saveTime = saveTime_1
                timeLeftInMillis = saveTime
                valuelist = valueList_1
                seekbarValue = seekbarValue_1
            }

            } else {
            Log.e("ReadFile", "File không đủ 2 dòng")
        }
    }
    
    override fun onBind(intent: Intent?): IBinder? = binder
}

