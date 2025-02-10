package com.example.anti_theft
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.IBinder
//import android.os.PowerManager
//import androidx.core.app.NotificationCompat
//
//class MyForegroundService : Service() {
//
//    private var wakeLock: PowerManager.WakeLock? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        acquireWakeLock()
//        createNotificationChannel()
//        startForegroundServiceWithNotification()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        // Thêm logic xử lý nếu cần (ví dụ: xử lý cảm biến hoặc âm thanh)
//        return START_STICKY
//    }
//
//    override fun onDestroy() {
//        releaseWakeLock()
//        super.onDestroy()
//    }
//
//    override fun onBind(intent: Intent?): IBinder? {
//        return null
//    }
//
//    private fun acquireWakeLock() {
//        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AntiTheft::WakeLock")
//        wakeLock?.acquire()
//    }
//
//    private fun releaseWakeLock() {
//        wakeLock?.let {
//            if (it.isHeld) {
//                it.release()
//            }
//        }
//    }
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val channelId = "ForegroundServiceChannel"
//            val channelName = "Anti-Theft Service"
//            // Dùng IMPORTANCE_LOW để không làm phiền người dùng quá mức
//            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
//            val notificationManager = getSystemService(NotificationManager::class.java)
//            notificationManager?.createNotificationChannel(channel)
//        }
//    }
//
//    private fun startForegroundServiceWithNotification() {
//        val channelId = "ForegroundServiceChannel"
//        val notification: Notification = NotificationCompat.Builder(this, channelId)
//            .setContentTitle("Anti-Theft Active")
//            .setContentText("Your phone is being monitored for movement.")
//            .setSmallIcon(R.drawable.ic_launcher_foreground) // Đổi thành icon của bạn
//            .setPriority(NotificationCompat.PRIORITY_LOW)
//            .setOngoing(true) // Ngăn thông báo bị vuốt tắt
//            .build()
//
//        startForeground(1, notification)
//    }
//}
