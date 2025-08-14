package com.example.anti_theft

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.navigation.NavigationView
import java.util.Locale

class Settings: AppCompatActivity() {
    //UI/UX
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: MaterialToolbar
    private lateinit var toggle: ActionBarDrawerToggle

    //convert
    //algorithm

    private lateinit var listofbutton: Button
    private lateinit var textofsellectsound: TextView
    private var valuelist: Int = 1   // value default

    //themes
    private var themeslist: Int = 1   // themes default
    private lateinit var setthemes: Button
    private lateinit var yourlanguage: Button

    private var countDownTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 15000 // Biến lưu thời gian đếm ngược (10 giây)
    private var saveTime : Long = 15000

    private lateinit var editTextofTime: EditText
    private lateinit var getTimeButton: Button
    private lateinit var openPDFButton: Button
    private lateinit var mySwitch_frist: Switch
    private lateinit var textsettimeinit: TextView

    //list of soundalarm
    val options = arrayOf("sound alarm (default)", "sound your alarm system","iphone call","set your alarm")
    val optionthemes = arrayOf("your system Theme (default)", "light Theme","dark Theme")
    val optionlanguage = arrayOf("English (default)","Vietnamese")

    //seekbar
    private lateinit var seekbar: SeekBar
    private lateinit var textViewSeekBar: TextView
    private var seekbarValue : Int = 4
    //beta

    //set language
    private val prefs by lazy { getSharedPreferences("settings", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings) // layout mới

        //set themes


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
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }

                R.id.nav_settings -> {
                    //no need to do anything
                }
            }
            drawerLayout.closeDrawers()
            true
        }
        //convert
        editTextofTime = findViewById(R.id.inputField)
        listofbutton = findViewById(R.id.button2)
        openPDFButton = findViewById(R.id.button)
        textofsellectsound = findViewById(R.id.textView4)
        getTimeButton = findViewById(R.id.button3)
        textsettimeinit = findViewById(R.id.texsettimeinit)
        //
        setthemes = findViewById(R.id.yourthemes)
        yourlanguage = findViewById(R.id.yourlanguage)

        //seekbar
        seekbar = findViewById(R.id.seekBar)
        textViewSeekBar = findViewById(R.id.textView2)

        listofbutton.setOnClickListener{
            // Tạo AlertDialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("choose your sound")
            builder.setItems(options) { _, which ->
                if(which + 1 == 4)
                {
                    Toast.makeText(this, "the function is being beta test :((", Toast.LENGTH_SHORT).show()
                }else {
                    val selectedOption = options[which]
                    valuelist = which + 1
                    textofsellectsound.text = "File name: $selectedOption"
                    SavetoFile(timeLeftInMillis, valuelist, seekbarValue,themeslist)
                }
            }
            builder.show()
        }

        // Theo dõi khi người dùng kéo
        seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textViewSeekBar.text = "sensitive: $progress"
                seekbarValue = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // khi bắt đầu kéo
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                updateText()
                SavetoFile(saveTime,valuelist,seekbarValue,themeslist)
            }
        })
        //sử lí nút

        openPDFButton.setOnClickListener {
            openPdfFromUrl(
                this,
                getString(R.string.pdfhowtouse))
        }

        //update Text
        readDataFromFile()
        updateText()
        loadThemes(themeslist)
        //read value at the first time
        textViewSeekBar.text = getString(R.string.sensitive) + "$seekbarValue"
        seekbar.progress = seekbarValue

        //button settime
        getTimeButton.setOnClickListener {
            val settimeoninput = getTimeFromInput()
            if(settimeoninput != 0L)
            {
                if(settimeoninput < 3L)
                {
                    Toast.makeText(this, "time shouldn't sorter than 3 second", Toast.LENGTH_SHORT).show()
                }else{
                    updateTime(settimeoninput * 1000)
                    SavetoFile(settimeoninput * 1000, valuelist,seekbarValue,themeslist)
                }
            }else{
                Toast.makeText(this, "time shouldn't equal to zero or error systax", Toast.LENGTH_SHORT).show()
            }
        }
        //settheme
        setthemes.setOnClickListener {
            // Tạo AlertDialog
            val builder = AlertDialog.Builder(this)
            builder.setTitle("set your themes")
            builder.setItems(optionthemes) { _, which ->
                if(which == 0) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                    themeslist = 1
                }else if(which == 1){
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    themeslist = 2
                }else{
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    themeslist = 3
                }
                SavetoFile(saveTime,valuelist,seekbarValue,themeslist)
                loadThemes(themeslist)
            }
            builder.show()
        }

        yourlanguage.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Set your language")
            builder.setItems(optionlanguage) { _, which ->
                if(which == 0) {
                    setLocale("en")
                    saveLanguage("en")
                }else{
                    setLocale("vi")
                    saveLanguage("vi")
                }

            }
            builder.show()
        }
        //updateTime(4000)//this function is to te

        //sử lí bộ đếm giờ
        //startnewtimer là khởi tạo lại từ đầu. starttimer là tiếp tục chạy,resume chưa sài(dùng để code thuật toán di chuyển)

        //test module
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
        textofsellectsound.text = getString(R.string.file_name) + " ${options[valuelist - 1]}"
        textsettimeinit.text = getString(R.string.time_set) + " ${timeLeftInMillis / 1000}s"
        textViewSeekBar.text = getString(R.string.sensitive) + " $seekbarValue"

    }

    private fun updateTime(newTimeInMillis: Long) {
        countDownTimer?.cancel() // Hủy bộ đếm nếu đang chạy
        saveTime = newTimeInMillis
        timeLeftInMillis = newTimeInMillis // Cập nhật thời gian mới
        updateText() // Cập nhật giao diện
        //  cái này nó liên quan tới nhúng nên khi mà ba code thì bạn sẽ ko hiểu cách n hoạt động và thực vậy
        //tôi đã ngồi đây hơn hai tiếng chỉ để sửa 2 bug (đổi đơn vị và cái nhúng đó), về phần nhúng thì do nó là class
        // sâu trong máy, và bằng chứng là tôi đã không tắt đi hàm startNewTimer() tôi đã thử đi thử lại nhiều lần và tôi đã nhận ra điều đó
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

    fun loadThemes(themes: Int) {
        if(themes == 1) {
            setthemes.text = getString(R.string.your_themes) + getString(R.string.theme_system_set)
        }else if(themes == 2) {
            setthemes.text = getString(R.string.your_themes) + getString(R.string.theme_day)
        }else{
            setthemes.text = getString(R.string.your_themes) + getString(R.string.theme_dark)
        }
    }
}