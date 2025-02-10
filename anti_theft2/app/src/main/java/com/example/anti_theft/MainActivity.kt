package com.example.anti_theft

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.Button
import android.media.MediaPlayer
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.padding
//import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.anti_theft.ui.theme.Anti_theftTheme
//count time
import android.os.CountDownTimer
import kotlin.math.abs
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.media.AudioAttributes
import android.media.AudioManager
//list choose file
import android.app.AlertDialog
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi

class MainActivity : AppCompatActivity(), SensorEventListener {
    //value of sensor accelerator
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var textView: TextView

    //value of time countdown
    private lateinit var textoftime: TextView
    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 15000 // Biến lưu thời gian đếm ngược (10 giây)
    private var isRunning = false
    private var saveTime : Long = 15000

    //lọc tốc độ đọc cảm biến
    private val alpha = 0.8f // Hệ số làm mượt (0.8 là giá trị tối ưu)
    private var gravity = floatArrayOf(0f, 0f, 0f) // Lưu giá trị đã lọc
    //algorithm
    private var isstatus = false
    private lateinit var textofstatus: TextView
    private var X : Float = 0.00f
    private var Y : Float = 0.00f
    private var Z : Float = 0.00f

    private var lastUpdateTime = 0L  // Lưu thời gian cập nhật cuối
    private val updateInterval = 300  // Giảm tốc độ cập nhật (milliseconds)
    private lateinit var listofbutton: Button
    private lateinit var textofsellectsound: TextView
    private var valuelist: Int = 1   // value default

    private lateinit var editTextofTime: EditText
    private lateinit var getTimeButton: Button
    private lateinit var mySwitch_frist: Switch
    private lateinit var textsettimeinit: TextView
    private lateinit var mediaPlayer: MediaPlayer   //điểu khiển âm thanh
    private var ringtone: android.media.Ringtone? = null
    private lateinit var wakeLock: PowerManager.WakeLock

    //beta
    private lateinit var everyTimeSwitch: Switch


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
        //alarm system
        val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val ringtoneUri = alarmUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(applicationContext, ringtoneUri)




        //other
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            { /* Audio focus change listener */ },
            AudioManager.STREAM_ALARM,
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
        )
        mediaPlayer = MediaPlayer.create(this, R.raw.sound_alarm)
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )// mediaPlayer.isLooping = true  // Nếu cần lặp lại như báo thức

        // Khởi tạo SensorManager
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        listofbutton.setOnClickListener{
            val options = arrayOf("sound alarm", "sound your alarm system")

            // Tạo AlertDialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("choose your sound")
            builder.setItems(options) { _, which ->
                val selectedOption = options[which]
                valuelist = which + 1
                textofsellectsound.text = "Name file: $selectedOption"
            }
            builder.show()
        }



        // Kiểm tra xem thiết bị có cảm biến gia tốc không
        if (accelerometer == null) {
            textView.text = "Thiết bị không hỗ trợ cảm biến gia tốc"
        }

        //connect switch in xml
        mySwitch_frist = findViewById(R.id.switch1)

        // sử lí nút
        // button init
        if (mySwitch_frist.isChecked) {
            Toast.makeText(this, "Switch đang bật", Toast.LENGTH_SHORT).show()
            startNewTimer()
        } else {
            Toast.makeText(this, "Switch đang tắt", Toast.LENGTH_SHORT).show()
            pauseTimer()
            onPause()
        }

        mySwitch_frist.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(this, "start!", Toast.LENGTH_SHORT).show()
                startNewTimer()
                onResume()
                wakeLock.acquire()
            } else {
                Toast.makeText(this, "end!", Toast.LENGTH_SHORT).show()
                resetTimer()
                onPause()
                statusOff()
                ringtone?.stop()
                wakeLock.release()
                stopSound()
                //stopService(Intent(this, MyForegroundService::class.java))//
            }
        }//~~~

        //button settime
        getTimeButton.setOnClickListener {
            val settimeoninput = getTimeFromInput()
            if(settimeoninput != 0L)
            {
                if(settimeoninput <= 3L)
                {
                    Toast.makeText(this, "time shouldn't sorter than 3 second", Toast.LENGTH_SHORT).show()
                }else{
                    updateTime(settimeoninput * 1000)
                }
            }else{
                Toast.makeText(this, "time shouldn't equal to zero or error systax", Toast.LENGTH_SHORT).show()
            }
        }
        //updateTime(4000)//this function is to text

        //sử lí bộ đếm giờ
        //startnewtimer là khởi tạo lại từ đầu. starttimer là tiếp tục chạy,resume chưa sài(dùng để code thuật toán di chuyển)

        //test module

        //beta test
        everyTimeSwitch = findViewById(R.id.switch2)
        everyTimeSwitch.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "i'm sorry this switch is being beta test ", Toast.LENGTH_SHORT).show()
        }


    }

    //~~~
    //~~~
    //~~~
    //~~~
    //~~~

    override fun onResume() {
        super.onResume()
        // Đăng ký lắng nghe cảm biến khi Activity hoạt động
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onPause() {
        super.onPause()
        // Hủy đăng ký cảm biến khi Activity bị dừng để tiết kiệm pin
        sensorManager.unregisterListener(this)
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

                // Làm tròn số đến 2 chữ số sau dấu phẩy
                val roundedX = String.format("%.2f", gravity[0])
                val roundedY = String.format("%.2f", gravity[1])
                val roundedZ = String.format("%.2f", gravity[2])

                val checkX = roundedX.toFloat()
                val checkY = roundedY.toFloat()
                val checkZ = roundedZ.toFloat()

                textView.text = "accerlerator:\nX: $roundedX\nY: $roundedY\nZ: $roundedZ"

                //algorithm in this function are dangerous
                if(mySwitch_frist.isChecked) {
                    if(!isstatus) { //check before system is really turn on alarm
                        if(abs((checkX - X)) > 0.04f && abs((checkY - Y)) > 0.04f && abs((checkZ - Z)) > 0.04f)
                        {
                            resetTimer()
                            startNewTimer()
                        }
                        X = roundedX.toFloat()
                        Y = roundedY.toFloat()
                        Z = roundedZ.toFloat()
                    }else{
                        if(abs((checkX - X)) > 0.04f && abs((checkY - Y)) > 0.04f && abs((checkZ - Z)) > 0.04f)
                        {
                            playSound()
                        }
                    }
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Không cần xử lý trong trường hợp này
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
    private fun startNewTimer() {
        countDownTimer?.cancel() // Hủy nếu đang chạy
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateText()
            }

            override fun onFinish() {
                textoftime.text = "Time left: 0 s"
                statusOn()
            }
        }.start()
    } //giống start timer

    private fun resetTimer() {
        countDownTimer?.cancel()
        timeLeftInMillis = saveTime // Reset về 10 giây
        updateText()
    }

    private fun updateText() {
        textoftime.text = "Time left: ${timeLeftInMillis / 1000} s"
    }

    private fun updateTime(newTimeInMillis: Long) {
        countDownTimer?.cancel() // Hủy bộ đếm nếu đang chạy
        saveTime = newTimeInMillis
        timeLeftInMillis = newTimeInMillis // Cập nhật thời gian mới
        updateText() // Cập nhật giao diện
        resetTimer()
//        startNewTimer()
        mySwitch_frist.isChecked = false
        pauseTimer()
        onPause()
        textsettimeinit.text = "Time set: ${timeLeftInMillis / 1000}s"
        //my experience:
        //  cái này nó liên quan tới nhúng nên khi mà ba code thì bạn sẽ ko hiểu cách n hoạt động và thực vậy
        //tôi đã ngồi đây hơn hai tiếng chỉ để sửa 2 bug (đổi đơn vị và cái nhúng đó), về phần nhúng thì do nó là class
        // sâu trong máy, và bằng chứng là tôi đã không tắt đi hàm startNewTimer() tôi đã thử đi thử lại nhiều lần và tôi đã nhận ra điều đó
    }

    private fun startTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateText()
            }

            override fun onFinish() {
                textView.text = "Time left 0s"
                isRunning = false
                statusOn()
            }
        }.start()
        isRunning = true
    }//giống startnewtimer() nhưng mà có running bằng true

    private fun pauseTimer() {
        if (isRunning) {
            countDownTimer?.cancel()
            isRunning = false
        }
    }

    private fun resumeTimer() {
        if (!isRunning) startTimer()
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
    private fun playSound() {
        if(valuelist == 1) {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start() // 🔊 Play audio
            }
        }else{
            ringtone?.play()
        }
    }
    private fun stopSound() {
        if(valuelist == 1) {
            if (mediaPlayer.isPlaying) {
                mediaPlayer.stop() // 🔊 Play audio
            }
        }else{
            ringtone?.stop()
        }
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