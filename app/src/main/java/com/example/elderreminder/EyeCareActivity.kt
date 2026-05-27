package com.example.elderreminder

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class EyeCareActivity : AppCompatActivity() {
    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var settings: AppSettings

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        settings = AppSettings(this)
        setContentView(buildContent())
        refreshStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(ContextCompat.getColor(this@EyeCareActivity, R.color.warm_background))
        }

        root.addView(TextView(this).apply {
            text = "护眼项目"
            textSize = 31f
            setTextColor(ContextCompat.getColor(this@EyeCareActivity, R.color.brand_green_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(18))
        })

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@EyeCareActivity, R.drawable.status_panel)
        }
        statusText = TextView(this).apply {
            textSize = 23f
            setTextColor(ContextCompat.getColor(this@EyeCareActivity, R.color.brand_green_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        detailText = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFF444444.toInt())
            setPadding(0, dp(10), 0, 0)
        }
        panel.addView(statusText)
        panel.addView(detailText)
        root.addView(panel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply { bottomMargin = dp(18) })

        root.addView(primaryButton("开始提醒") {
            if (!UsagePermission.hasUsageAccess(this)) {
                Toast.makeText(this, "请先开启使用情况访问权限", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                return@primaryButton
            }
            ReminderScheduler.schedule(this)
            Toast.makeText(this, "已开始守护提醒", Toast.LENGTH_SHORT).show()
            refreshStatus()
        })

        root.addView(secondaryButton("家人设置") {
            showPinDialog {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
        })

        root.addView(secondaryButton("视力与习惯周报") {
            showPinDialog {
                startActivity(Intent(this, ReportActivity::class.java))
            }
        })

        return ScrollView(this).apply {
            addView(root)
        }
    }

    private fun primaryButton(text: String, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            background = ContextCompat.getDrawable(this@EyeCareActivity, R.drawable.button_primary)
            setOnClickListener { onClick() }
            layoutParams = buttonLayoutParams()
        }

    private fun secondaryButton(text: String, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@EyeCareActivity, R.color.brand_green_dark))
            setOnClickListener { onClick() }
            layoutParams = buttonLayoutParams()
        }

    private fun buttonLayoutParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            bottomMargin = dp(12)
        }

    private fun refreshStatus() {
        val hasUsageAccess = UsagePermission.hasUsageAccess(this)
        statusText.text = if (hasUsageAccess) "状态：权限已开启" else "状态：需要开启权限"
        detailText.text = if (hasUsageAccess) {
            "点击“开始提醒”后，应用会在后台定期检查使用情况，并在长时间使用时发出通知和语音提醒。"
        } else {
            "Android 不允许应用自动开启使用情况权限。请进入“家人设置”开启相关权限。"
        }
    }

    private fun showPinDialog(onSuccess: () -> Unit) {
        val input = EditText(this).apply {
            gravity = Gravity.CENTER
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            textSize = 24f
            hint = "输入 PIN"
        }

        AlertDialog.Builder(this)
            .setTitle("家人验证")
            .setMessage("请输入家人 PIN。默认 PIN 是 1234。")
            .setView(input)
            .setNegativeButton("取消", null)
            .setPositiveButton("进入") { _, _ ->
                if (input.text.toString() == settings.pin) {
                    onSuccess()
                } else {
                    Toast.makeText(this, "PIN 不正确", Toast.LENGTH_SHORT).show()
                }
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
