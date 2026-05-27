package com.example.elderreminder

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportActivity : AppCompatActivity() {

    private lateinit var dbHelper: ReminderHistoryDbHelper
    private var records: List<ReminderRecord> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = ReminderHistoryDbHelper(this)
        
        // 获取过去7天的提醒记录
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        records = dbHelper.getRemindersSince(sevenDaysAgo)
        
        setContentView(buildContent())
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
            setBackgroundColor(ContextCompat.getColor(this@ReportActivity, R.color.warm_background))
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
            setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener { finish() }
        }
        titleRow.addView(leftBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val centerTitle = TextView(this).apply {
            text = "本地视力与习惯周报"
            textSize = 22f
            setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        titleRow.addView(centerTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val rightBtn = Button(this).apply {
            text = "首页"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener {
                val intent = Intent(this@ReportActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
        titleRow.addView(rightBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(titleRow)

        root.addView(TextView(this).apply {
            text = "此报告完全生成并在本地保存，绝不上传至任何服务器，保障家人隐私。"
            textSize = 15f
            setTextColor(0xFF777777.toInt())
            setPadding(0, 0, 0, dp(20))
        })

        // 信息汇总面版
        val summaryPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@ReportActivity, R.drawable.status_panel)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val totalCount = records.size
        val appCounts = records.groupBy { it.packageName }.mapValues { it.value.size }
        val mostUsedAppPackage = appCounts.maxByOrNull { it.value }?.key ?: "无"
        val mostUsedAppName = getAppName(mostUsedAppPackage)

        summaryPanel.addView(TextView(this).apply {
            text = "守护汇总 (过去 7 天)"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        })

        summaryPanel.addView(TextView(this).apply {
            text = "• 疲劳提醒总次数: ${totalCount} 次"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, dp(6))
        })

        summaryPanel.addView(TextView(this).apply {
            text = "• 最常超时使用的应用: $mostUsedAppName"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, dp(6))
        })

        // 计算每日提醒频次趋势
        val dailyCounts = getPastSevenDaysCounts()
        val busiestDay = dailyCounts.maxByOrNull { it.second }
        val busiestDayText = busiestDay?.let { "${it.first} (${it.second}次)" } ?: "无"
        summaryPanel.addView(TextView(this).apply {
            text = "• 单日超时次数最多: $busiestDayText"
            textSize = 18f
            setTextColor(0xFF333333.toInt())
        })

        root.addView(summaryPanel, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(24) })

        // 柱状图区域
        root.addView(TextView(this).apply {
            text = "每日超时提醒频次趋势"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        })

        val chartView = BarChartView(this, dailyCounts)
        root.addView(chartView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(200)
        ).apply { bottomMargin = dp(24) })

        // 详细历史列表标题
        root.addView(TextView(this).apply {
            text = "超时提醒详细事件"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(12))
        })

        // 详细记录展示
        val listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        if (records.isEmpty()) {
            listContainer.addView(TextView(this).apply {
                text = "过去 7 天内无提醒记录，家人的用眼习惯非常好！"
                textSize = 18f
                setTextColor(0xFF555555.toInt())
                setPadding(dp(12), dp(12), dp(12), dp(12))
            })
        } else {
            val sdf = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
            records.sortedByDescending { it.timestamp }.forEach { record ->
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    background = ContextCompat.getDrawable(this@ReportActivity, R.drawable.status_panel)
                }

                item.addView(TextView(this).apply {
                    text = "提醒时间: ${sdf.format(Date(record.timestamp))}"
                    textSize = 17f
                    setTextColor(0xFF333333.toInt())
                    typeface = Typeface.DEFAULT_BOLD
                })

                val appName = getAppName(record.packageName)
                item.addView(TextView(this).apply {
                    text = "超时应用: $appName (${record.packageName})"
                    textSize = 16f
                    setTextColor(0xFF666666.toInt())
                    setPadding(0, dp(4), 0, 0)
                })

                listContainer.addView(item, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(10) })
            }
        }

        root.addView(listContainer)

        // 返回按钮
        root.addView(Button(this).apply {
            text = "返回"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            background = ContextCompat.getDrawable(this@ReportActivity, R.drawable.button_primary)
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(20); bottomMargin = dp(30) })

        return ScrollView(this).apply { addView(root) }
    }

    private fun getPastSevenDaysCounts(): List<Pair<String, Int>> {
        val calendar = Calendar.getInstance()
        val result = mutableListOf<Pair<String, Int>>()
        val sdf = SimpleDateFormat("MM-dd", Locale.getDefault())

        // 预填充最近七天的日期列表
        val dates = mutableListOf<Pair<String, LongRange>>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val startOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val endOfDay = cal.apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            val label = sdf.format(Date(startOfDay))
            dates.add(Pair(label, startOfDay..endOfDay))
        }

        for ((label, range) in dates) {
            val count = records.count { it.timestamp in range }
            result.add(Pair(label, count))
        }

        return result
    }

    private fun getAppName(packageName: String): String {
        if (packageName == "未知" || packageName == "无") return packageName
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    // 简单原生 Canvas 高清绘制柱状图类，避免引入第三方图表依赖
    private class BarChartView(context: Context, private val data: List<Pair<String, Int>>) : View(context) {
        private val paintBar = Paint().apply {
            color = ContextCompat.getColor(context, R.color.brand_green)
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val paintText = Paint().apply {
            color = 0xFF555555.toInt()
            textSize = dpToPx(13f)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        private val paintLabel = Paint().apply {
            color = 0xFF222222.toInt()
            textSize = dpToPx(14f)
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        private val paintGrid = Paint().apply {
            color = 0xFFDDDDDD.toInt()
            strokeWidth = dpToPx(1f)
            style = Paint.Style.STROKE
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val w = width.toFloat()
            val h = height.toFloat()

            val paddingLeft = dpToPx(20f)
            val paddingRight = dpToPx(20f)
            val paddingTop = dpToPx(20f)
            val paddingBottom = dpToPx(30f)

            val graphWidth = w - paddingLeft - paddingRight
            val graphHeight = h - paddingTop - paddingBottom

            val maxVal = (data.maxOfOrNull { it.second } ?: 1).coerceAtLeast(4)

            // 画底线
            canvas.drawLine(paddingLeft, h - paddingBottom, w - paddingRight, h - paddingBottom, paintGrid)

            val barCount = data.size
            if (barCount == 0) return

            val stepX = graphWidth / barCount
            val barWidth = stepX * 0.6f

            for (i in 0 until barCount) {
                val item = data[i]
                val value = item.second

                // 计算柱状图 x/y
                val x = paddingLeft + (i * stepX) + (stepX / 2)
                val pct = value.toFloat() / maxVal.toFloat()
                val barHeight = graphHeight * pct
                val y = h - paddingBottom - barHeight

                // 画柱子
                canvas.drawRect(x - barWidth / 2, y, x + barWidth / 2, h - paddingBottom, paintBar)

                // 画柱顶数值 (如果是0就画个0或者稍微避让)
                canvas.drawText(value.toString(), x, y - dpToPx(5f), paintLabel)

                // 画日期标签
                canvas.drawText(item.first, x, h - paddingBottom + dpToPx(20f), paintText)
            }
        }

        private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
    }
}
