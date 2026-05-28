package com.example.elderreminder

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

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

        // 一键分享健康数据 (微信) button
        root.addView(primaryButton("告诉家人我的状况") {
            tryShareReport()
        })

        // 一键导出血压 button
        root.addView(primaryButton("告诉医生我的状况") {
            showExportDialog()
        })

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            addView(root)
        }
        mainLayout.addView(scrollView)

        // 首页最底部的智能脑舱（大模型相关设置单独入口）
        val bottomSettingBtn = Button(this).apply {
            text = "⚙️ 智能大模型设置"
            textSize = 19f
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000) // 透明背景
            setOnClickListener {
                startActivity(Intent(this@MainActivity, DeepseekSettingsActivity::class.java))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
                topMargin = dp(8)
            }
        }
        mainLayout.addView(bottomSettingBtn)

        return mainLayout
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

    private fun tryShareReport() {
        val file = HealthReportRenderer.generateAndSaveReport(this)
        if (file == null) {
            Toast.makeText(this, "生成健康数据报告失败", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                this,
                "com.example.elderreminder.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // 明确针对微信分享 (微信有两个主流包，微信本体: com.tencent.mm)
                // 我们可以使用系统的分享选择器，但在此提供针对微信/默认的选择
                `package` = "com.tencent.mm"
            }

            // 我们尝试直接拉起微信分享
            try {
                startActivity(shareIntent)
            } catch (ex: Exception) {
                // 如果微信未安装，则降级为使用系统的通用分享界面，让用户能够选择其他 App (例如发送给微信好友)
                val chooserIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "分享健康报告")
                startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "分享报告出错: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun showExportDialog() {
        val options = arrayOf("近一个月", "近三个月", "近一年")
        AlertDialog.Builder(this)
            .setTitle("选择要导出的血压数据时间范围")
            .setItems(options) { dialog, which ->
                val rangeType = which + 1
                val rangeName = options[which]
                exportAndShareBloodPressure(rangeType, rangeName)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun exportAndShareBloodPressure(rangeType: Int, rangeName: String) {
        // 读取并按日均值过滤/合并血压与心率数据
        val dailyRecords = BloodPressureExporter.getGroupedDailyRecords(this, rangeType)
        if (dailyRecords.isEmpty()) {
            Toast.makeText(this, "在选定的时间段内没有血压和心率自测数据", Toast.LENGTH_LONG).show()
            return
        }

        // 导出为 HTML/Excel 复合格式的文件（后缀采用 .xls 确保腾讯文档能识别为电子表格并直接打开预览）
        val file = BloodPressureExporter.exportToExcelXls(this, dailyRecords, rangeName)
        if (file == null || !file.exists()) {
            Toast.makeText(this, "生成数据表格文件失败", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val contentUri: Uri = FileProvider.getUriForFile(
                this,
                "com.example.elderreminder.fileprovider",
                file
            )

            // 拉起分享 Intent (微信优先，系统分享兜底)
            // MIME 类型使用 "application/vnd.ms-excel"，让微信和系统彻底将其识别为原生 Excel 文档！
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.ms-excel"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                `package` = "com.tencent.mm"
            }

            try {
                startActivity(shareIntent)
            } catch (ex: Exception) {
                // 如果微信未安装，则降级为使用系统的通用分享界面，让用户能够选择其他 App
                val chooserIntent = Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.ms-excel"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "分享血压心率数据")
                startActivity(chooserIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "分享数据表格出错: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
