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
import android.graphics.Typeface

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

        // Title Row
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(18))
        }

        val leftBtn = Button(this).apply {
            text = "返回"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@EyeCareActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener { finish() }
        }
        titleRow.addView(leftBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val centerTitle = TextView(this).apply {
            text = "护眼项目"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@EyeCareActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        titleRow.addView(centerTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val rightBtn = Button(this).apply {
            text = "首页"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@EyeCareActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener {
                val intent = Intent(this@EyeCareActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
        titleRow.addView(rightBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(titleRow)

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
            val hasUsage = UsagePermission.hasUsageAccess(this)
            val hasAccessibility = UsagePermission.hasAccessibilityAccess(this)
            if (!hasUsage && !hasAccessibility) {
                AlertDialog.Builder(this)
                    .setTitle("开启守护提醒权限说明")
                    .setMessage("为了能够精准、实时、省电地检测前台应用并提醒休息，本应用支持以下两种方式。强烈推荐开启【无障碍服务】以获得最好的实时闭环效果。\n\n" +
                               "1. 开启无障碍服务 (推荐：秒级响应，无需后台轮询轮检测)\n" +
                               "2. 开启使用情况访问权限 (备用：15分钟定时轮询监测模式)")
                    .setPositiveButton("去开启无障碍") { _, _ ->
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                    .setNegativeButton("去开启使用情况") { _, _ ->
                        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    }
                    .setNeutralButton("取消", null)
                    .show()
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
        val hasAccessibility = UsagePermission.hasAccessibilityAccess(this)
        
        statusText.text = when {
            hasAccessibility -> "状态：无障碍服务已开启 (推荐)"
            hasUsageAccess -> "状态：使用情况权限已开启"
            else -> "状态：需要开启权限"
        }
        
        detailText.text = when {
            hasAccessibility -> {
                "无障碍守护已启动，实时监测前台切换，秒级精准响应且防沉迷效果最好。"
            }
            hasUsageAccess -> {
                "使用情况模式已启用，在后台定期检查使用情况。\n" +
                "提示：强烈建议启用“无障碍服务”以实现秒级精准提醒！点击下方“开启无障碍服务”进行配置。"
            }
            else -> {
                "Android 不允许应用自动开启权限。请按需开启无障碍服务 (推荐) 或使用情况访问权限。"
            }
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
