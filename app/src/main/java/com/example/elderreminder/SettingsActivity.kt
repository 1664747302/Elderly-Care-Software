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

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: AppSettings
    private lateinit var reminderMinutesInput: EditText
    private lateinit var reminderTextInput: EditText
    private lateinit var pinInput: EditText
    private lateinit var voiceCheckbox: CheckBox
    private lateinit var curfewCheckbox: CheckBox
    private lateinit var curfewStartInput: EditText
    private lateinit var curfewEndInput: EditText
    
    private lateinit var customAudioCheckbox: CheckBox
    private lateinit var recordButton: Button
    private lateinit var playButton: Button
    
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var isRecording = false
    private var isPlaying = false
    private lateinit var audioFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        audioFile = SpeechReminder.getCustomAudioFile(this)
        setContentView(buildContent())
    }

    private fun buildContent(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(ContextCompat.getColor(this@SettingsActivity, R.color.warm_background))
        }

        root.addView(title("家人设置"))
        root.addView(label("提醒间隔（分钟，15 到 180）"))
        reminderMinutesInput = editText(settings.reminderMinutes.toString(), InputType.TYPE_CLASS_NUMBER)
        root.addView(reminderMinutesInput)

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

        val recordPanel = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        recordButton = Button(this).apply {
            text = "开始录音"
            textSize = 18f
            setOnClickListener { toggleRecording() }
        }
        recordPanel.addView(recordButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(8) })

        playButton = Button(this).apply {
            text = "播放录音"
            textSize = 18f
            isEnabled = audioFile.exists()
            setOnClickListener { togglePlayback() }
        }
        recordPanel.addView(playButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        
        root.addView(recordPanel)

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
        val pin = pinInput.text.toString().ifBlank { AppSettings.DEFAULT_PIN }
        val startHour = curfewStartInput.text.toString().toIntOrNull() ?: 22
        val endHour = curfewEndInput.text.toString().toIntOrNull() ?: 6

        settings.reminderMinutes = minutes
        settings.voiceEnabled = voiceCheckbox.isChecked
        settings.customAudioEnabled = customAudioCheckbox.isChecked
        settings.curfewEnabled = curfewCheckbox.isChecked
        settings.curfewStartHour = startHour
        settings.curfewEndHour = endHour
        settings.reminderText = reminderTextInput.text.toString()
        settings.pin = pin
        ReminderScheduler.schedule(this)

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun toggleRecording() {
        if (!isRecording) {
            // 请求麦克风录音权限
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.RECORD_AUDIO), 101)
                return
            }
            startRecording()
        } else {
            stopRecording()
        }
    }

    private fun startRecording() {
        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            isRecording = true
            recordButton.text = "停止录音"
            playButton.isEnabled = false
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
            recordButton.text = "开始录音"
            playButton.isEnabled = true
            Toast.makeText(this, "录音保存成功！", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun togglePlayback() {
        if (!isPlaying) {
            startPlayback()
        } else {
            stopPlayback()
        }
    }

    private fun startPlayback() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(audioFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener { stopPlayback() }
            }
            isPlaying = true
            playButton.text = "停止播放"
            recordButton.isEnabled = false
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
            playButton.text = "播放录音"
            recordButton.isEnabled = true
        } catch (e: Exception) {
            e.printStackTrace()
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
