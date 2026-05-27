package com.example.elderreminder

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class BloodPressureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.warm_background))
        }

        root.addView(TextView(this).apply {
            text = "血压自测"
            textSize = 31f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(18))
        })

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.status_panel)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        panel.addView(TextView(this).apply {
            text = "功能开发中"
            textSize = 23f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        panel.addView(TextView(this).apply {
            text = "血压自助记和趋势记录功能即将上线，敬请期待！"
            textSize = 18f
            setTextColor(0xFF444444.toInt())
            setPadding(0, dp(12), 0, 0)
        })

        root.addView(panel)

        return ScrollView(this).apply {
            addView(root)
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
