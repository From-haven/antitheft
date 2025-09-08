package com.example.anti_theft

import android.Manifest
import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
//count time
import android.os.CountDownTimer
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
//list choose file
import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import java.util.Locale
import android.provider.Settings

class MainActivity : AppCompatActivity() {
    //value of sensor accelerator
    private lateinit var textView: TextView

    //value of time countdown
    private lateinit var textoftime: TextView
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 15000 // Biến lưu thời gian đếm ngược (10 giây)
    private var saveTime : Long = 15000

    //lọc tốc độ đọc cảm biến
    //algorithm
    private var isstatus = false
    private lateinit var textofstatus: TextView
    private var valuelist: Int = 1   // value default

    //themeslist
    private var themeslist: Int = 1

    private lateinit var openPDFButton: Button
    private var ringtone: android.media.Ringtone? = null
    private lateinit var wakeLock: PowerManager.WakeLock
    //list of soundalarm
    val options = arrayOf("sound alarm", "sound your alarm system")
    //seekbar
    private var seekbarValue : Int = 4
    //beta
    private val REQUEST_CODE_ALL = 101

    //reciving fro foreGroundservice
    private var myService: MyForegroundService? = null
    private var isBound = false
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MyForegroundService.LocalBinder
            myService = binder.getService()
            isBound = true

            // Ví dụ: đăng callback để nhận dữ liệu
            myService?.setCallback(object : MyForegroundService.SensorCallback {
                override fun onSensorData(x: Float, y: Float, z: Float, isrunning: Boolean, timeLeft: Long) {
                    runOnUiThread {
                        //build UI at here
                        textView.text = "accerlerator:\nX: $x\nY: $y\nZ: $z"
                        textoftime.text = getString(R.string.time_left) + " ${timeLeft / 1000}s"
                        if(isrunning) {
                            statusOn()
                        }else{
                            statusOff()
                        }
                    }
                }
            })
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
            myService = null
        }
    }

    //UI/UX
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var toggle: ActionBarDrawerToggle
    //FAB
    private lateinit var fab: FloatingActionButton
    private var isOn = false
    //language
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //savefile before readfile to ignore crash with reason file not found
        try{
            readDataFromFile()
            loadLanguage_first()
            setThemes()
        } catch (e: Exception) {
            SavetoFile(saveTime,valuelist,seekbarValue,themeslist)
            saveLanguage("en")
        }
        setContentView(R.layout.activity_main)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AntiTheft::Lock")

        textView = findViewById(R.id.textview8) // Kết nối TextView trong XML
        textoftime = findViewById(R.id.textView7)
        textofstatus = findViewById(R.id.textView6)
        openPDFButton = findViewById(R.id.howtouse)
        openPDFButton.setOnClickListener {
            openPdfFromUrl(this, getString(R.string.pdfhowtouse))
        }

        //alarm system
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtoneUri = alarmUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)

        //UI/UX
        // Ánh xạ view
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        toolbar = findViewById(R.id.toolbar)

        // Gắn toolbar
        setSupportActionBar(toolbar)

        // Setup toggle cho Drawer
        toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Xử lý khi click vào menu item
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    //no need to do anything
                }

                R.id.nav_settings -> {
                    pause()
                    stopService(Intent(this, MyForegroundService::class.java))
                    statusOff()
                    val intent = Intent(this, Settings::class.java)
                    startActivity(intent)
                }
            }
            drawerLayout.closeDrawers()
            true
        }
        //FAB
        fab = findViewById(R.id.fab)
        fab.setOnClickListener {
            isOn = !isOn
            if (isOn) {
                // Trạng thái bật
                fab.backgroundTintList = ContextCompat.getColorStateList(this, R.color.FABon)
                //logic here
                Toast.makeText(this, "start!", Toast.LENGTH_SHORT).show()
                //startNewTimer()
                //wakeLock.acquire()
                //startService(intent)
                ContextCompat.startForegroundService(this, intent)
                start()
                //startService(Intent(this, MyForegroundService::class.java))
            } else {
                // Trạng thái tắt
                fab.backgroundTintList = ContextCompat.getColorStateList(this, R.color.FABoff)
                //logic here
                Toast.makeText(this, "end!", Toast.LENGTH_SHORT).show()
                //resetTimer()

                //statusOff()
                //wakeLock.release()
                //stopSound()
                //stopService(intent)
                pause()
                stopService(Intent(this, MyForegroundService::class.java))
                statusOff()
            }
        }

        //kiểm tra có quyền thông báo chưa
        checkAndRequestPermissions()

        //update Text
        updateText()

        //test module


    }

    //~~~
    //~~~
    //~~~
    //~~~
    //~~~

//    override fun onStart() {
//        super.onStart()
//        Intent(this, MyForegroundService::class.java).also { intent ->
//            bindService(intent, connection, Context.BIND_AUTO_CREATE)
//        }
//    }
//    override fun onStop() {
//        super.onStop()
//        if (isBound) {
//            unbindService(connection)
//            isBound = false
//        }
//    }
    private fun start() {
        Intent(this, MyForegroundService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }
    private fun pause() {
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()
    }

    //sử lí bộ đếm giờ
    private fun updateText() {
        val texttest = getString(R.string.time_left) + " ${timeLeftInMillis / 1000}s"
        textoftime.text = texttest
//        textsettimeinit.text = "Time set: ${timeLeftInMillis / 1000}s" /////

    }


    private fun statusOn() {
        textofstatus.setTextColor(Color.RED)
        textofstatus.text = "On"
        isstatus = true
    }
    private fun statusOff() {
        textofstatus.setTextColor(Color.GREEN)
        textofstatus.text = "Off"
        isstatus = false
    }

    // đây là cdoe kiểm tra đã có quyền chưa
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Android 13+ cần quyền thông báo
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        //quyền sử dụng chay nen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val powerManager = getSystemService(PowerManager::class.java)
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        // Nếu còn quyền nào chưa được cấp, yêu cầu cấp
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_ALL
            )
        }
    }
    // (Tùy chọn) Xử lý kết quả nếu cần
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_ALL) {
            // Kiểm tra kết quả từng quyền nếu muốn
        }
    }

    private fun SavetoFile(SaveTime : Long,Vallist : Int, Sensitive: Int,Themeslist: Int) {
        val data = "SaveTime=${SaveTime}\nVallist=${Vallist}\nSensitive=$Sensitive\nThemes=$Themeslist"
        openFileOutput("data.txt", Context.MODE_PRIVATE).use {
            it.write(data.toByteArray())
        }
    }

    //đọc file đã lưu trước đó (có một cái tương tự trong MyForeGroundservice)
    private fun readDataFromFile() {

        val content = openFileInput("data.txt").bufferedReader().use { it.readText() }
        val lines = content.lines()

        if (lines.size >= 2) {
            val saveTime_1 = lines[0].split("=").getOrNull(1)?.toLongOrNull()
            val valueList_1 = lines[1].split("=").getOrNull(1)?.toIntOrNull()
            val seekbarValue_1 = lines[2].split("=").getOrNull(1)?.toIntOrNull()
            val themeslist_1 = lines[3].split("=").getOrNull(1)?.toIntOrNull()
            if (saveTime_1 != null && valueList_1 != null && seekbarValue_1 != null) {
                saveTime = saveTime_1
                timeLeftInMillis = saveTime
                valuelist = valueList_1
                seekbarValue = seekbarValue_1
                if (themeslist_1 != null) {
                    themeslist = themeslist_1
                }//nó kiểm tra null nhưng mà mấy cái trước có kiểm tra đâu?
            }

        } else {
            Log.e("ReadFile", "File không đủ 2 dòng")
        }
    }
    // open file how to use pdf
    fun openPdfFromUrl(context: Context, pdfUrl: String) {

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
        intent.setPackage("com.android.chrome") // Mở bằng Chrome nếu có

        val builder = AlertDialog.Builder(context)
        builder.setTitle("open file how to use anti-theft PDF")
        builder.setMessage("Do you want to open this file pdf in chrome ?")
        builder.setPositiveButton("Mở") { _, _ ->
            try {
                startActivity(intent)
            } catch (e: Exception) {
                // Nếu máy không có Chrome thì mở bằng trình duyệt mặc định
                val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                startActivity(fallbackIntent)
            }
        }
        builder.setNegativeButton("Hủy", null)
        builder.show()
    }
    //this is UI/UX
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
    //test module
    //set your language
    fun saveLanguage(lang: String) {
        prefs.edit().putString("app_lang", lang).apply()
    }

    fun loadLanguage() {
        val lang = prefs.getString("app_lang", "en") // default English
        setLocale(lang ?: "en")
    }

    fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)

        // Optional: restart activity to apply changes
        recreate()
    }

    fun loadLanguage_first() {
        val lang = prefs.getString("app_lang", "en") // default English
        setLocale_first(lang ?: "en")
    }

    fun setLocale_first(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)

        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun setThemes() {
        if(themeslist == 1){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }else if(themeslist == 2 ){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
    }
}