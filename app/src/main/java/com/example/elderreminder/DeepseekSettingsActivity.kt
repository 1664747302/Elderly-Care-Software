package com.example.elderreminder

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class DeepseekSettingsActivity : AppCompatActivity() {

    private lateinit var settings: AppSettings
    private lateinit var apiKeyInput: EditText
    private lateinit var apiUrlInput: EditText
    private lateinit var modelRadioGroup: RadioGroup
    private lateinit var flashRadioButton: RadioButton
    private lateinit var proRadioButton: RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
            setBackgroundColor(ContextCompat.getColor(this@DeepseekSettingsActivity, R.color.warm_background))
        }

        // Title navigation bar
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }

        val leftBtn = Button(this).apply {
            text = "返回"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@DeepseekSettingsActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener { finish() }
        }
        titleRow.addView(leftBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val centerTitle = TextView(this).apply {
            text = "大模型设置"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@DeepseekSettingsActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        titleRow.addView(centerTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val rightBtn = Button(this).apply {
            text = "首页"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@DeepseekSettingsActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener {
                val intent = Intent(this@DeepseekSettingsActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
        titleRow.addView(rightBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(titleRow)

        // API Key Section
        root.addView(label("DeepSeek API 密钥"))
        apiKeyInput = editText(settings.deepseekApiKey, InputType.TYPE_CLASS_TEXT).apply {
            hint = "填写以开启血压智能评估 (例如: sk-...)"
        }
        root.addView(apiKeyInput)

        // API URL Section
        root.addView(label("DeepSeek API 接口地址"))
        apiUrlInput = editText(settings.deepseekApiUrl, InputType.TYPE_CLASS_TEXT).apply {
            hint = "默认: https://api.deepseek.com/v1"
        }
        root.addView(apiUrlInput)

        // Model Selection Section
        root.addView(label("模型选择"))
        modelRadioGroup = RadioGroup(this).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        flashRadioButton = RadioButton(this).apply {
            text = "deepseek-v4-flash (高速推荐)"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            id = View.generateViewId()
        }
        modelRadioGroup.addView(flashRadioButton)

        proRadioButton = RadioButton(this).apply {
            text = "deepseek-v4-pro (深度分析)"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            id = View.generateViewId()
        }
        modelRadioGroup.addView(proRadioButton)

        // Read saved model settings
        if (settings.deepseekModel == "deepseek-v4-pro") {
            proRadioButton.isChecked = true
        } else {
            flashRadioButton.isChecked = true
        }

        root.addView(modelRadioGroup, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(16) })

        // Save Button
        root.addView(Button(this).apply {
            text = "保存设置"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            background = ContextCompat.getDrawable(this@DeepseekSettingsActivity, R.drawable.button_primary)
            setOnClickListener { saveSettings() }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(18) })

        return ScrollView(this).apply { addView(root) }
    }

    private fun saveSettings() {
        val apiKey = apiKeyInput.text.toString().trim()
        val apiUrl = apiUrlInput.text.toString().trim()
        val model = if (proRadioButton.isChecked) "deepseek-v4-pro" else "deepseek-v4-flash"

        settings.deepseekApiKey = apiKey
        settings.deepseekApiUrl = if (apiUrl.isBlank()) "https://api.deepseek.com/v1" else apiUrl
        settings.deepseekModel = model

        Toast.makeText(this, "大模型设置已完成保存", Toast.LENGTH_SHORT).show()
        finish()
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
