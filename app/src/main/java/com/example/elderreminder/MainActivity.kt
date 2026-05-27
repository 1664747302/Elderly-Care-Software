package com.example.elderreminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildContent())
        requestNotificationPermissionIfNeeded()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.warm_background))
        }

        root.addView(TextView(this).apply {
            text = "老人关怀"
            textSize = 31f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.brand_green_dark))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(18))
        })

        // 护眼项目入口 button
        root.addView(primaryButton("护眼项目") {
            startActivity(Intent(this, EyeCareActivity::class.java))
        })

        // 血压自测入口 button
        root.addView(primaryButton("血压自测") {
            startActivity(Intent(this, BloodPressureActivity::class.java))
        })

        return ScrollView(this).apply {
            addView(root)
        }
    }

    private fun primaryButton(text: String, onClick: () -> Unit): Button =
        Button(this).apply {
            this.text = text
            textSize = 22f
            setTextColor(0xFFFFFFFF.toInt())
            background = ContextCompat.getDrawable(this@MainActivity, R.drawable.button_primary)
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply {
                bottomMargin = dp(16)
                topMargin = dp(8)
            }
        }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 10)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
