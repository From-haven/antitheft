package com.example.anti_theft

import android.Manifest
import android.content.Context
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.widget.Switch
import android.widget.Toast
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.anti_theft.ui.theme.Anti_theftTheme
import android.widget.SeekBar
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

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

    private lateinit var listofbutton: Button
    private lateinit var textofsellectsound: TextView
    private var valuelist: Int = 1   // value default

    private lateinit var editTextofTime: EditText
    private lateinit var getTimeButton: Button
    private lateinit var openPDFButton: Button
    private lateinit var mySwitch_frist: Switch
    private lateinit var textsettimeinit: TextView
    private var ringtone: android.media.Ringtone? = null
    private lateinit var wakeLock: PowerManager.WakeLock
    //list of soundalarm
    val options = arrayOf("sound alarm", "sound your alarm system")
    //seekbar
    private lateinit var seekbar: SeekBar
    private lateinit var textViewSeekBar: TextView
    private var seekbarValue : Int = 4
    //beta
    private lateinit var everyTimeSwitch: Switch
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
                        textoftime.text = "Time left: ${timeLeft / 1000} s"
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


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AntiTheft::Lock")

        textView = findViewById(R.id.textview8) // Kết nối TextView trong XML
        textoftime = findViewById(R.id.textView7)
        textsettimeinit = findViewById(R.id.textView3)
        textofstatus = findViewById(R.id.textView6)
        textofsellectsound = findViewById(R.id.textView5)
        //val textView = findViewById<TextView>(R.id.textView6) // connect textView status sound alarm
        editTextofTime = findViewById(R.id.inputField)
        getTimeButton = findViewById(R.id.button3)
        listofbutton = findViewById(R.id.button2)
        openPDFButton = findViewById(R.id.button)

        //seekbar
        seekbar = findViewById(R.id.seekBar)
        textViewSeekBar = findViewById(R.id.textView2)
        //alarm system
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtoneUri = alarmUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)

        listofbutton.setOnClickListener{

            // Tạo AlertDialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("choose your sound")
            builder.setItems(options) { _, which ->
                val selectedOption = options[which]
                valuelist = which + 1
                textofsellectsound.text = "Name file: $selectedOption"
                SavetoFile(timeLeftInMillis,valuelist,seekbarValue)
            }
            builder.show()
        }

        //kiểm tra có quyền thông báo chưa
        checkAndRequestPermissions()
        //connect switch in xml
        mySwitch_frist = findViewById(R.id.switch1)

        //savefile before readfile to ignore crash with reason file not found
        try{
            readDataFromFile()
        } catch (e: Exception) {
            SavetoFile(saveTime,valuelist,seekbarValue)
        }
        //read file
        //readDataFromFile()

        //seekbar progess
        // Đọc giá trị ban đầu:
        textViewSeekBar.text = "Unsensitive: $seekbarValue"
        seekbar.progress = seekbarValue

        // Theo dõi khi người dùng kéo
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textViewSeekBar.text = "Unsensitive: $progress"
                seekbarValue = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // khi bắt đầu kéo
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateText()
                SavetoFile(saveTime,valuelist,seekbarValue)
            }
        })

        // sử lí nút
        mySwitch_frist.setOnCheckedChangeListener { _, isChecked ->
            val intent = Intent(this, MyForegroundService::class.java)
            if (isChecked) {
                Toast.makeText(this, "start!", Toast.LENGTH_SHORT).show()
                //startNewTimer()
                //wakeLock.acquire()
                //startService(intent)
                ContextCompat.startForegroundService(this, intent)
                start()
                //startService(Intent(this, MyForegroundService::class.java))

            } else {
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
        }//~~~
        openPDFButton.setOnClickListener {
            openPdfFromUrl(this, "https://drive.google.com/viewerng/viewer?embedded=true&url=https%3A%2F%2Fdrive.google.com%2Fuc%3Fexport%3Ddownload%26id%3D1JxOQwGCgecg5CxIJhaTlgHCXVTFtiqRz")
        }
        //update Text
        updateText()

        //button settime
        getTimeButton.setOnClickListener {
            val settimeoninput = getTimeFromInput()
            if(settimeoninput != 0L)
            {
                if(settimeoninput <= 3L)
                {
                    Toast.makeText(this, "time shouldn't sorter than 3 second", Toast.LENGTH_SHORT).show()
                }else{
                    mySwitch_frist.isChecked = false
                    updateTime(settimeoninput * 1000)
                    SavetoFile(settimeoninput * 1000, valuelist,seekbarValue)
                }
            }else{
                Toast.makeText(this, "time shouldn't equal to zero or error systax", Toast.LENGTH_SHORT).show()
            }
        }
        //updateTime(4000)//this function is to te

        //sử lí bộ đếm giờ
        //startnewtimer là khởi tạo lại từ đầu. starttimer là tiếp tục chạy,resume chưa sài(dùng để code thuật toán di chuyển)

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

    private fun getTimeFromInput(): Long {
        val inputText = editTextofTime.text.toString().trim()
        return if (inputText.isNotEmpty()) {
            editTextofTime.text.clear()
            inputText.toLongOrNull() ?: 0L
        } else {
            0L
        }
    }

    //sử lí bộ đếm giờ
    private fun updateText() {
        textoftime.text = "Time left: ${timeLeftInMillis / 1000} s"
        textofsellectsound.text = "Name file: ${options[valuelist - 1]}"
        textsettimeinit.text = "Time set: ${timeLeftInMillis / 1000}s"
        textViewSeekBar.text = "Unsensitive: $seekbarValue"
        mySwitch_frist.isChecked = false

    }

    private fun updateTime(newTimeInMillis: Long) {
        countDownTimer?.cancel() // Hủy bộ đếm nếu đang chạy
        saveTime = newTimeInMillis
        timeLeftInMillis = newTimeInMillis // Cập nhật thời gian mới
        updateText() // Cập nhật giao diện
        //resetTimer()
//        startNewTimer()
        mySwitch_frist.isChecked = false
        //pauseTimer()
        //onPause()
        textsettimeinit.text = "Time set: ${timeLeftInMillis / 1000}s"
        //my experience:
        //  cái này nó liên quan tới nhúng nên khi mà ba code thì bạn sẽ ko hiểu cách n hoạt động và thực vậy
        //tôi đã ngồi đây hơn hai tiếng chỉ để sửa 2 bug (đổi đơn vị và cái nhúng đó), về phần nhúng thì do nó là class
        // sâu trong máy, và bằng chứng là tôi đã không tắt đi hàm startNewTimer() tôi đã thử đi thử lại nhiều lần và tôi đã nhận ra điều đó
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

        // Kiểm tra quyền vị trí (cần cho foreground service location)
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED) {
//            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
//        }
//
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
//            != PackageManager.PERMISSION_GRANTED) {
//            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
//        }

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

    private fun SavetoFile(SaveTime : Long,Vallist : Int, Sensitive: Int) {
        val data = "SaveTime=${SaveTime}\nVallist=${Vallist}\nSensitive=$Sensitive"
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
    //test module

}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Anti_theftTheme {
        Greeting("Android")
    }
}