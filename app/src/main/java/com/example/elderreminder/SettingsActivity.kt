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

class SettingsActivity : AppCompatActivity() {
    private lateinit var settings: AppSettings
    private lateinit var reminderMinutesInput: EditText
    private lateinit var reminderTextInput: EditText
    private lateinit var pinInput: EditText
    private lateinit var voiceCheckbox: CheckBox
    private lateinit var curfewCheckbox: CheckBox
    private lateinit var curfewStartInput: EditText
    private lateinit var curfewEndInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
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
        settings.curfewEnabled = curfewCheckbox.isChecked
        settings.curfewStartHour = startHour
        settings.curfewEndHour = endHour
        settings.reminderText = reminderTextInput.text.toString()
        settings.pin = pin
        ReminderScheduler.schedule(this)

        Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        finish()
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
