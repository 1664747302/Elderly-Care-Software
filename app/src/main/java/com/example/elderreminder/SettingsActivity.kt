package com.example.elderreminder

import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.io.File
import android.media.MediaRecorder
import android.media.MediaPlayer
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.graphics.Typeface

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: AppSettings
    private lateinit var reminderMinutesInput: EditText
    private lateinit var repeatedReminderIntervalInput: EditText
    private lateinit var reminderTextInput: EditText
    private lateinit var pinInput: EditText
    private lateinit var voiceCheckbox: CheckBox
    private lateinit var curfewCheckbox: CheckBox
    private lateinit var curfewStartInput: EditText
    private lateinit var curfewEndInput: EditText
    private lateinit var deepseekApiKeyInput: EditText
    private lateinit var deepseekApiUrlInput: EditText
    
    private lateinit var customAudioCheckbox: CheckBox
    private lateinit var recordRegularButton: Button
    private lateinit var playRegularButton: Button
    private lateinit var recordCurfewButton: Button
    private lateinit var playCurfewButton: Button
    
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false
    private lateinit var regularAudioFile: File
    private lateinit var curfewAudioFile: File
    private var currentRecordingFile: File? = null
    private var currentPlayingFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        regularAudioFile = SpeechReminder.getCustomAudioFile(this, isCurfew = false)
        curfewAudioFile = SpeechReminder.getCustomAudioFile(this, isCurfew = true)
        setContentView(buildContent())
    }

    private fun buildContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.warm_background))
        }

        // Title Row
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(20))
        }

        val leftBtn = Button(this).apply {
            text = "返回"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener { finish() }
        }
        titleRow.addView(leftBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val centerTitle = TextView(this).apply {
            text = "家人设置"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        titleRow.addView(centerTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val rightBtn = Button(this).apply {
            text = "首页"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener {
                val intent = Intent(this@SettingsActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
        titleRow.addView(rightBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(titleRow)
        root.addView(label("提醒间隔（分钟，15 到 180）"))
        reminderMinutesInput = editText(settings.reminderMinutes.toString(), InputType.TYPE_CLASS_NUMBER)
        root.addView(reminderMinutesInput)

        root.addView(label("重复提醒间隔（若超时后未停止使用，每隔几分钟提醒一次）"))
        repeatedReminderIntervalInput = editText(settings.repeatedReminderIntervalMinutes.toString(), InputType.TYPE_CLASS_NUMBER)
        root.addView(repeatedReminderIntervalInput)

        root.addView(label("语音提醒"))
        voiceCheckbox = CheckBox(this).apply {
            text = "开启语音朗读"
            textSize = 20f
            isChecked = settings.voiceEnabled
        }
        root.addView(voiceCheckbox)

        // 家人录音小组件
        root.addView(label("家人录音提醒 (自定义录音)"))
        customAudioCheckbox = CheckBox(this).apply {
            text = "启用家人录音(若无录音则降级为文字朗读)"
            textSize = 20f
            isChecked = settings.customAudioEnabled
        }
        root.addView(customAudioCheckbox)

        // 常规提醒录音面板
        root.addView(subLabel("连续使用常规提醒录音："))
        val regularPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        recordRegularButton = Button(this).apply {
            text = "录制常规录音"
            textSize = 18f
            setOnClickListener { toggleRecording(isCurfew = false) }
        }
        regularPanel.addView(recordRegularButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(8) })

        playRegularButton = Button(this).apply {
            text = "播放常规录音"
            textSize = 18f
            isEnabled = regularAudioFile.exists()
            setOnClickListener { togglePlayback(isCurfew = false) }
        }
        regularPanel.addView(playRegularButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        
        root.addView(regularPanel)

        // 宵禁提醒录音面板
        root.addView(subLabel("宵禁限制提醒录音："))
        val curfewPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        recordCurfewButton = Button(this).apply {
            text = "录制宵禁录音"
            textSize = 18f
            setOnClickListener { toggleRecording(isCurfew = true) }
        }
        curfewPanel.addView(recordCurfewButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(8) })

        playCurfewButton = Button(this).apply {
            text = "播放宵禁录音"
            textSize = 18f
            isEnabled = curfewAudioFile.exists()
            setOnClickListener { togglePlayback(isCurfew = true) }
        }
        curfewPanel.addView(playCurfewButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        
        root.addView(curfewPanel)

        // 防杀保活教程面板
        root.addView(label("后台防杀/保活配置教程"))
        val tutorialText = TextView(this).apply {
            text = "【华为/荣耀手机保活设置指南】\n" +
                   "1. 开启通知权限：确保应用可以弹出大字提醒通知。\n" +
                   "2. 电池手动管理：请前往 手机设置 -> 电池 -> 应用启动管理 -> 找到“老人护眼提醒” -> 关掉“自动管理” -> 开启“允许自启动”、“允许后台活动”。\n" +
                   "3. 忽略电池优化：前往 手机设置 -> 应用 -> 权限管理 -> 特殊访问权限 -> 电池优化 -> 设为“不允许优化”。\n" +
                   "4. 锁定前台任务：在多任务后台界面中，向下拉动“老人护眼提醒”将其锁定（出现一把锁标志），防止被一键清理。"
            textSize = 16f
            setTextColor(0xFF555555.toInt())
            setLineSpacing(0f, 1.25f)
            setBackgroundResource(R.drawable.status_panel)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }
        root.addView(tutorialText)

        root.addView(label("系统使用权限设置"))
        root.addView(Button(this).apply {
            text = "开启使用情况权限"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.brand_green_dark))
            setOnClickListener { 
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(12) })

        root.addView(Button(this).apply {
            text = "系统通知设置"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.brand_green_dark))
            setOnClickListener { 
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.parse("package:$packageName"))
                startActivity(intent)
            }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(12) })

        root.addView(label("深夜防沉迷 (夜间宵禁)"))
        curfewCheckbox = CheckBox(this).apply {
            text = "启用夜间宵禁限制"
            textSize = 20f
            isChecked = settings.curfewEnabled
        }
        root.addView(curfewCheckbox)

        root.addView(label("宵禁起始时间 (24小时制小时阶，例如 22)"))
        curfewStartInput = editText(settings.curfewStartHour.toString(), InputType.TYPE_CLASS_NUMBER)
        root.addView(curfewStartInput)

        root.addView(label("宵禁截止时间 (24小时制小时阶，例如 6)"))
        curfewEndInput = editText(settings.curfewEndHour.toString(), InputType.TYPE_CLASS_NUMBER)
        root.addView(curfewEndInput)

        root.addView(label("提醒文案"))
        reminderTextInput = editText(settings.reminderText, InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE)
        reminderTextInput.minLines = 3
        root.addView(reminderTextInput)

        root.addView(label("DeepSeek API 密钥 (用于生成血压健康分析级评价)"))
        deepseekApiKeyInput = editText(settings.deepseekApiKey, InputType.TYPE_CLASS_TEXT)
        deepseekApiKeyInput.hint = "填写以开启血压智能评估（例如：sk-...）"
        root.addView(deepseekApiKeyInput)

        root.addView(label("DeepSeek API 自定义接口地址 (支持中转域名)"))
        deepseekApiUrlInput = editText(settings.deepseekApiUrl, InputType.TYPE_CLASS_TEXT)
        deepseekApiUrlInput.hint = "默认：https://api.deepseek.com/v1"
        root.addView(deepseekApiUrlInput)

        root.addView(label("新的家人 PIN"))
        pinInput = editText(settings.pin, InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        root.addView(pinInput)

        root.addView(Button(this).apply {
            text = "保存设置"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            background = ContextCompat.getDrawable(this@SettingsActivity, R.drawable.button_primary)
            setOnClickListener { saveSettings() }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { topMargin = dp(18) })

        return ScrollView(this).apply { addView(root) }
    }

    private fun saveSettings() {
        val minutes = reminderMinutesInput.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_REMINDER_MINUTES
        val repeatedInterval = repeatedReminderIntervalInput.text.toString().toIntOrNull() ?: AppSettings.DEFAULT_REPEATED_REMINDER_INTERVAL_MINUTES
        val pin = pinInput.text.toString().ifBlank { AppSettings.DEFAULT_PIN }
        val startHour = curfewStartInput.text.toString().toIntOrNull() ?: 22
        val endHour = curfewEndInput.text.toString().toIntOrNull() ?: 6

        settings.reminderMinutes = minutes
        settings.repeatedReminderIntervalMinutes = repeatedInterval
        settings.voiceEnabled = voiceCheckbox.isChecked
        settings.customAudioEnabled = customAudioCheckbox.isChecked
        settings.curfewEnabled = curfewCheckbox.isChecked
        settings.curfewStartHour = startHour
        settings.curfewEndHour = endHour
        settings.reminderText = reminderTextInput.text.toString()
        settings.deepseekApiKey = deepseekApiKeyInput.text.toString().trim()
        val apiUrl = deepseekApiUrlInput.text.toString().trim()
        settings.deepseekApiUrl = if (apiUrl.isBlank()) "https://api.deepseek.com/v1" else apiUrl
        settings.pin = pin
        ReminderScheduler.schedule(this)

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun toggleRecording(isCurfew: Boolean) {
        if (isRecording) {
            val targetFile = if (isCurfew) curfewAudioFile else regularAudioFile
            if (currentRecordingFile == targetFile) {
                stopRecording()
            } else {
                Toast.makeText(this, "有其他录音正在进行", Toast.LENGTH_SHORT).show()
            }
        } else {
            // 请求麦克风录音权限
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), if (isCurfew) 102 else 101)
                return
            }
            startRecording(isCurfew)
        }
    }

    private fun startRecording(isCurfew: Boolean) {
        val file = if (isCurfew) curfewAudioFile else regularAudioFile
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            currentRecordingFile = file
            
            if (isCurfew) {
                recordCurfewButton.text = "停止录音"
                recordRegularButton.isEnabled = false
                playCurfewButton.isEnabled = false
                playRegularButton.isEnabled = false
            } else {
                recordRegularButton.text = "停止录音"
                recordCurfewButton.isEnabled = false
                playRegularButton.isEnabled = false
                playCurfewButton.isEnabled = false
            }
            Toast.makeText(this, "正在录音，请贴近麦克风说话...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "录音初始化失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            currentRecordingFile = null
            
            recordRegularButton.text = "录制常规录音"
            recordCurfewButton.text = "录制宵禁录音"
            
            recordRegularButton.isEnabled = true
            recordCurfewButton.isEnabled = true
            
            playRegularButton.isEnabled = regularAudioFile.exists()
            playCurfewButton.isEnabled = curfewAudioFile.exists()
            
            Toast.makeText(this, "录音保存成功！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun togglePlayback(isCurfew: Boolean) {
        if (!isPlaying) {
            startPlayback(isCurfew)
        } else {
            stopPlayback()
        }
    }

    private fun startPlayback(isCurfew: Boolean) {
        val file = if (isCurfew) curfewAudioFile else regularAudioFile
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopPlayback() }
            }
            isPlaying = true
            currentPlayingFile = file
            
            if (isCurfew) {
                playCurfewButton.text = "停止播放"
                playRegularButton.isEnabled = false
                recordCurfewButton.isEnabled = false
                recordRegularButton.isEnabled = false
            } else {
                playRegularButton.text = "停止播放"
                playCurfewButton.isEnabled = false
                recordRegularButton.isEnabled = false
                recordCurfewButton.isEnabled = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "音频播放失败", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopPlayback() {
        try {
            mediaPlayer?.apply {
                stop()
                release()
            }
            mediaPlayer = null
            isPlaying = false
            currentPlayingFile = null
            
            playRegularButton.text = "播放常规录音"
            playCurfewButton.text = "播放宵禁录音"
            
            recordRegularButton.isEnabled = true
            recordCurfewButton.isEnabled = true
            playRegularButton.isEnabled = regularAudioFile.exists()
            playCurfewButton.isEnabled = curfewAudioFile.exists()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (requestCode == 101) {
                startRecording(isCurfew = false)
            } else if (requestCode == 102) {
                startRecording(isCurfew = true)
            }
        } else {
            Toast.makeText(this, "需要麦克风权限才能录音", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        mediaPlayer?.release()
    }

    private fun title(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 30f
            setTextColor(ContextCompat.getColor(this@SettingsActivity, R.color.brand_green_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(20))
        }

    private fun label(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 19f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(16), 0, dp(6))
        }

    private fun subLabel(text: String): TextView =
        TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(0xFF555555.toInt())
            setPadding(dp(4), dp(8), 0, dp(4))
        }

    private fun editText(value: String, inputTypeValue: Int): EditText =
        EditText(this).apply {
            setText(value)
            textSize = 20f
            inputType = inputTypeValue
            setSingleLine(false)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
