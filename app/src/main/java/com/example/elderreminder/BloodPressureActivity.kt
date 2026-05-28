package com.example.elderreminder

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BloodPressureActivity : AppCompatActivity() {

    private lateinit var dbHelper: BloodPressureDbHelper
    private val selectedCalendar: Calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormatter = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    private val weekFormatter = SimpleDateFormat("EEEE", Locale.CHINA)

    // UI Elements that need to be refreshed
    private lateinit var dateDisplayView: TextView
    private lateinit var cardsContainer: LinearLayout
    private lateinit var historyContainer: LinearLayout

    private lateinit var settings: AppSettings
    private lateinit var aiButton: Button
    private lateinit var aiResultView: TextView
    private lateinit var chartView: BloodPressureChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = BloodPressureDbHelper(this)
        settings = AppSettings(this)
        setContentView(buildContent())
        refreshData()
    }

    private fun buildContent(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(22), dp(18), dp(22))
            setBackgroundColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.warm_background))
        }

        // Title Row
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(14))
        }

        val leftBtn = Button(this).apply {
            text = "返回"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener { finish() }
        }
        titleRow.addView(leftBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        val centerTitle = TextView(this).apply {
            text = "血压自测"
            textSize = 24f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        titleRow.addView(centerTitle, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val rightBtn = Button(this).apply {
            text = "首页"
            textSize = 18f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener {
                val intent = Intent(this@BloodPressureActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                finish()
            }
        }
        titleRow.addView(rightBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(titleRow)

        // Date selection band
        val dateBand = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.status_panel)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        val prevBtn = Button(this).apply {
            text = "◀ 前一天"
            textSize = 17f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green))
            setBackgroundColor(0x00000000)
            setOnClickListener {
                selectedCalendar.add(Calendar.DAY_OF_YEAR, -1)
                refreshData()
            }
        }
        dateBand.addView(prevBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        dateDisplayView = TextView(this).apply {
            text = ""
            textSize = 19f
            setTextColor(0xFF333333.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setOnClickListener {
                showDatePicker()
            }
        }
        dateBand.addView(dateDisplayView, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f))

        val nextBtn = Button(this).apply {
            text = "后一天 ▶"
            textSize = 17f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green))
            setBackgroundColor(0x00000000)
            setOnClickListener {
                selectedCalendar.add(Calendar.DAY_OF_YEAR, 1)
                refreshData()
            }
        }
        dateBand.addView(nextBtn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        root.addView(dateBand, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(18) })

        // 智能血压健康评估卡片
        val aiCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.status_panel)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val aiTitleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        aiTitleRow.addView(TextView(this).apply {
            text = "🤖 30天血压趋势智能评估"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        
        aiCard.addView(aiTitleRow)

        // Row above the report (aiResultView) containing two parallel entry buttons
        val entryRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }

        val fillProfileBtn = Button(this).apply {
            text = "填写资料 📝"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.button_primary)
            // Make background semi-transparent green or light gray to make it parallel and parallel styled
            val gd = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x1A1F6B45) // 10% alpha of brand_green
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), 0xFF1F6B45.toInt())
            }
            background = gd
            setOnClickListener {
                showFillProfileDialog()
            }
        }
        entryRow.addView(fillProfileBtn, LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(8) })

        val customizeRangeBtn = Button(this).apply {
            text = "安全范围 ⚙️"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            val gd = android.graphics.drawable.GradientDrawable().apply {
                setColor(0x1A1F6B45) // 10% alpha of brand_green
                cornerRadius = dp(6).toFloat()
                setStroke(dp(1), 0xFF1F6B45.toInt())
            }
            background = gd
            setOnClickListener {
                showCustomizeRangeDialog()
            }
        }
        entryRow.addView(customizeRangeBtn, LinearLayout.LayoutParams(0, dp(44), 1f))

        aiCard.addView(entryRow, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(8)
            bottomMargin = dp(10)
        })

        aiCard.addView(View(this).apply {
            setBackgroundColor(0xFFEEEEEE.toInt())
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(1)
        ).apply {
            bottomMargin = dp(10)
        })

        aiResultView = TextView(this).apply {
            text = if (settings.lastAiEvaluationResult.isNotBlank()) {
                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(settings.lastAiEvaluationTimeMillis))
                "【上次评估时间：$timeStr】\n\n${settings.lastAiEvaluationResult}"
            } else {
                "点击下方按钮，开始使用 AI 智能分析评估近30天的血压/心率趋势（需要至少7天有自测数据并且在家人设置中配置了有效的 DeepSeek API 密钥）。"
            }
            textSize = 15f
            setTextColor(0xFF333333.toInt())
        }
        aiCard.addView(aiResultView)

        aiButton = Button(this).apply {
            text = "获取 AI 健康评价"
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.button_primary)
            setOnClickListener {
                runAiEvaluation(aiButton, aiResultView)
            }
        }
        aiCard.addView(aiButton, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(14) })

        root.addView(aiCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(18) })

        // 近七天血压趋势图表卡片
        val chartCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.status_panel)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val chartTitleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        chartTitleRow.addView(TextView(this).apply {
            text = "📊 近七天血压趋势 (平均每日)"
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val legendLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Systolic legend
        legendLayout.addView(View(this).apply {
            setBackgroundColor(0xFFD32F2F.toInt())
        }, LinearLayout.LayoutParams(dp(12), dp(12)).apply { rightMargin = dp(4) })
        legendLayout.addView(TextView(this).apply {
            text = "高压"
            textSize = 13f
            setTextColor(0xFF555555.toInt())
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(10) })

        // Diastolic legend
        legendLayout.addView(View(this).apply {
            setBackgroundColor(0xFF1976D2.toInt())
        }, LinearLayout.LayoutParams(dp(12), dp(12)).apply { rightMargin = dp(4) })
        legendLayout.addView(TextView(this).apply {
            text = "低压"
            textSize = 13f
            setTextColor(0xFF555555.toInt())
        })

        chartTitleRow.addView(legendLayout, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
        chartCard.addView(chartTitleRow)

        chartCard.addView(View(this).apply {
            setBackgroundColor(0xFFEEEEEE.toInt())
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(1)
        ).apply {
            topMargin = dp(8)
            bottomMargin = dp(10)
        })

        chartView = BloodPressureChartView(this)
        chartCard.addView(chartView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(180)
        ))

        root.addView(chartCard, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(18) })

        // Cards Container for Morning, Noon, Evening options
        cardsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(cardsContainer)

        // Divider
        val divider = View(this).apply {
            setBackgroundColor(0xFFDDDDDD.toInt())
        }
        root.addView(divider, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(1)
        ).apply {
            topMargin = dp(24)
            bottomMargin = dp(16)
        })

        // Recent Records Title Row with fold/unfold button
        val historyTitleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))
        }

        val historyTitleTv = TextView(this).apply {
            text = "最近血压记录 (历史列表)"
            textSize = 21f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
        }
        historyTitleRow.addView(historyTitleTv, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val foldBtn = Button(this).apply {
            text = "收起 📁"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000)
            setOnClickListener {
                if (historyContainer.visibility == View.VISIBLE) {
                    historyContainer.visibility = View.GONE
                    text = "展开 📂"
                } else {
                    historyContainer.visibility = View.VISIBLE
                    text = "收起 📁"
                }
            }
        }
        historyTitleRow.addView(foldBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

        root.addView(historyTitleRow)

        // History container list
        historyContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(historyContainer)

        return ScrollView(this).apply {
            addView(root)
        }
    }

    private fun refreshData() {
        val dateStr = dateFormatter.format(selectedCalendar.time)
        val displayDate = displayDateFormatter.format(selectedCalendar.time)
        val weekday = weekFormatter.format(selectedCalendar.time)

        // Update date display
        dateDisplayView.text = "$displayDate\n($weekday) 📅"

        // Update 7-day average chart
        val chartData = mutableListOf<DailyAverage>()
        val tempCal = selectedCalendar.clone() as Calendar
        tempCal.add(Calendar.DAY_OF_YEAR, -6)
        val chartDateFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())

        for (i in 0 until 7) {
            val queryDateStr = dateFormatter.format(tempCal.time)
            val labelStr = chartDateFormatter.format(tempCal.time)
            val dayRecords = dbHelper.getRecordsForDate(queryDateStr)
            if (dayRecords.isEmpty()) {
                chartData.add(DailyAverage(labelStr, 0, 0))
            } else {
                val avgSys = dayRecords.map { it.systolic }.average().toInt()
                val avgDia = dayRecords.map { it.diastolic }.average().toInt()
                chartData.add(DailyAverage(labelStr, avgSys, avgDia))
            }
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }
        chartView.setData(chartData)

        // Update Morning, Noon, Evening cards
        cardsContainer.removeAllViews()
        val sysRangeStr = "${settings.bpSystolicMin}-${settings.bpSystolicMax}"
        val diaRangeStr = "${settings.bpDiastolicMin}-${settings.bpDiastolicMax}"
        val rangeDesc = "$sysRangeStr / $diaRangeStr"

        val periods = listOf(
            PeriodConfig("早晨", "🌅 早晨自测 (起床后/早餐前)", rangeDesc),
            PeriodConfig("中午", "☀️ 中午自测 (饭前半小时/饭后)", rangeDesc),
            PeriodConfig("晚上", "🌙 晚上自测 (临睡前半小时)", rangeDesc)
        )

        for (config in periods) {
            val record = dbHelper.getBloodPressure(dateStr, config.key)
            val cardView = buildPeriodCard(config, record)
            cardsContainer.addView(cardView, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = dp(14) })
        }

        // Update history section
        refreshHistory()
    }

    private fun buildPeriodCard(config: PeriodConfig, record: BloodPressureRecord?): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.status_panel)
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        // Card Title Block
        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        titleRow.addView(TextView(this).apply {
            text = config.title
            textSize = 20f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        if (record != null) {
            val evaluation = evaluateBloodPressure(record.systolic, record.diastolic)
            val statusTag = TextView(this).apply {
                text = evaluation.label
                textSize = 14f
                setTextColor(0xFFFFFFFF.toInt())
                setBackgroundColor(evaluation.color)
                setPadding(dp(8), dp(4), dp(8), dp(4))
                gravity = Gravity.CENTER
            }
            titleRow.addView(statusTag, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        }

        card.addView(titleRow)

        // Divider in card
        card.addView(View(this).apply {
            setBackgroundColor(0xFFEEEEEE.toInt())
        }, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(1)
        ).apply {
            topMargin = dp(8)
            bottomMargin = dp(10)
        })

        // Content Area
        if (record == null) {
            card.addView(TextView(this).apply {
                text = "今日该时段暂无测量记录。"
                textSize = 17f
                setTextColor(0xFF777777.toInt())
                setPadding(0, 0, 0, dp(12))
            })
            card.addView(Button(this).apply {
                text = "填写测量数据"
                textSize = 18f
                setTextColor(0xFFFFFFFF.toInt())
                background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.button_primary)
                setOnClickListener {
                    showFillDialog(config.key, null)
                }
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        } else {
            // Display blood pressure values in large font
            val dataRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, 0, 0, dp(10))
            }

            // High pressure
            val systolicLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            systolicLayout.addView(TextView(this).apply {
                text = "${record.systolic}"
                textSize = 34f
                setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
                typeface = Typeface.DEFAULT_BOLD
            })
            systolicLayout.addView(TextView(this).apply {
                text = "高压(收缩压)"
                textSize = 14f
                setTextColor(0xFF666666.toInt())
            })
            dataRow.addView(systolicLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            // Low pressure
            val diastolicLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            diastolicLayout.addView(TextView(this).apply {
                text = "${record.diastolic}"
                textSize = 34f
                setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
                typeface = Typeface.DEFAULT_BOLD
            })
            diastolicLayout.addView(TextView(this).apply {
                text = "低压(舒张压)"
                textSize = 14f
                setTextColor(0xFF666666.toInt())
            })
            dataRow.addView(diastolicLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            // Heart rate
            val heartRateLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }
            heartRateLayout.addView(TextView(this).apply {
                text = "${record.heartRate}"
                textSize = 34f
                setTextColor(0xFFE91E63.toInt()) // Pink/Red indicator
                typeface = Typeface.DEFAULT_BOLD
            })
            heartRateLayout.addView(TextView(this).apply {
                text = "心率(次/分)"
                textSize = 14f
                setTextColor(0xFF666666.toInt())
            })
            dataRow.addView(heartRateLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            card.addView(dataRow)

            // Evaluation advice
            val evaluation = evaluateBloodPressure(record.systolic, record.diastolic)
            card.addView(TextView(this).apply {
                text = "温馨提醒: ${evaluation.tips}"
                textSize = 16f
                setTextColor(0xFF444444.toInt())
                setPadding(dp(4), 0, 0, dp(12))
            })

            // Action Row
            val actionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            actionRow.addView(Button(this).apply {
                text = "修改数据"
                textSize = 17f
                setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
                setBackgroundColor(0xFFE8F5E9.toInt())
                setOnClickListener {
                    showFillDialog(config.key, record)
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(8) })

            actionRow.addView(Button(this).apply {
                text = "重新录入"
                textSize = 17f
                setTextColor(0xFFFFFFFF.toInt())
                background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.button_primary)
                setOnClickListener {
                    showFillDialog(config.key, null)
                }
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            card.addView(actionRow)
        }

        return card
    }

    private fun refreshHistory() {
        historyContainer.removeAllViews()
        val recentList = dbHelper.getRecentRecords(15)

        if (recentList.isEmpty()) {
            historyContainer.addView(TextView(this).apply {
                text = "暂无历史自测记录，点上面按钮开始记录吧！"
                textSize = 17f
                setTextColor(0xFF666666.toInt())
                setPadding(dp(8), dp(8), dp(8), dp(8))
            })
        } else {
            for (rec in recentList) {
                val item = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.status_panel)
                    setPadding(dp(12), dp(10), dp(12), dp(10))
                    gravity = Gravity.CENTER_VERTICAL
                }

                // Left: date & time Period
                val descLayout = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                }
                descLayout.addView(TextView(this).apply {
                    text = rec.date
                    textSize = 16f
                    setTextColor(0xFF555555.toInt())
                })
                descLayout.addView(TextView(this).apply {
                    text = "${rec.period}自测"
                    textSize = 18f
                    setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
                    typeface = Typeface.DEFAULT_BOLD
                })
                item.addView(descLayout, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                // Center: numeric readings
                val readingsText = TextView(this).apply {
                    text = "高压:${rec.systolic}  低压:${rec.diastolic}  心率:${rec.heartRate}"
                    textSize = 18f
                    setTextColor(0xFF333333.toInt())
                    typeface = Typeface.DEFAULT_BOLD
                }
                item.addView(readingsText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 2f))

                // Right: status indicator
                val evaluation = evaluateBloodPressure(rec.systolic, rec.diastolic)
                val statusInd = TextView(this).apply {
                    text = evaluation.label
                    textSize = 14f
                    setTextColor(0xFFFFFFFF.toInt())
                    setBackgroundColor(evaluation.color)
                    setPadding(dp(6), dp(3), dp(6), dp(3))
                }
                item.addView(statusInd, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ))

                historyContainer.addView(item, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = dp(8) })
            }
        }
    }

    private fun showFillProfileDialog() {
        val diseasesList = settings.userChronicDiseases.split("、", ",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        val mainContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        // Basic Profile Info inputs
        val (agePanel, ageInput) = createDialogInputField(
            this,
            "使用人员年龄 (岁) :",
            "请输入使用人员年龄，例如: 72",
            settings.userAge,
            InputType.TYPE_CLASS_NUMBER
        )
        mainContainer.addView(agePanel)

        val (genderPanel, genderInput) = createDialogInputField(
            this,
            "使用人员性别 :",
            "请输入男/女或不填",
            settings.userGender,
            InputType.TYPE_CLASS_TEXT
        )
        mainContainer.addView(genderPanel)

        // Chronic disease tags container
        val diseaseCardLabel = TextView(this).apply {
            text = "🩺 基础疾病与医学史 (可卡片式增删) :"
            textSize = 17f
            setTextColor(0xFF333333.toInt())
            setPadding(0, dp(10), 0, dp(6))
        }
        mainContainer.addView(diseaseCardLabel)

        // The programmatic FlowLayout/Flex container for tag cards
        val tagContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        fun drawDiseaseTags() {
            tagContainer.removeAllViews()
            if (diseasesList.isEmpty()) {
                tagContainer.addView(TextView(this@BloodPressureActivity).apply {
                    text = "暂未添加任何基础疾病（例如：高血压3级、糖尿病2型）。"
                    textSize = 15f
                    setTextColor(0xFF888888.toInt())
                    setPadding(0, dp(4), 0, dp(8))
                })
            } else {
                for (disease in diseasesList) {
                    val tagRow = LinearLayout(this@BloodPressureActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = android.graphics.drawable.GradientDrawable().apply {
                            setColor(0x0D000000) // 5% dark alpha gray
                            cornerRadius = dp(6).toFloat()
                            setStroke(dp(1), 0xFFDDDDDD.toInt())
                        }
                        setPadding(dp(12), dp(8), dp(12), dp(8))
                    }
                    val tagText = TextView(this@BloodPressureActivity).apply {
                        text = disease
                        textSize = 16f
                        setTextColor(0xFF333333.toInt())
                    }
                    tagRow.addView(tagText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                    // Edit Button
                    val editBtn = TextView(this@BloodPressureActivity).apply {
                        text = "✏️ 修改"
                        textSize = 15f
                        setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green))
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        setSingleLine(true)
                        setOnClickListener {
                            val editInput = EditText(this@BloodPressureActivity).apply {
                                setText(disease)
                                setSingleLine(true)
                                selectAll()
                            }
                            AlertDialog.Builder(this@BloodPressureActivity)
                                .setTitle("修改基础疾病")
                                .setView(editInput)
                                .setPositiveButton("确认") { _, _ ->
                                    val newText = editInput.text.toString().trim()
                                    if (newText.isNotEmpty()) {
                                        val idx = diseasesList.indexOf(disease)
                                        if (idx != -1) {
                                            diseasesList[idx] = newText
                                            drawDiseaseTags()
                                        }
                                    }
                                }
                                .setNegativeButton("取消", null)
                                .show()
                        }
                    }
                    tagRow.addView(editBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

                    // Delete Button
                    val delBtn = TextView(this@BloodPressureActivity).apply {
                        text = "❌ 删除"
                        textSize = 15f
                        setTextColor(0xFFD32F2F.toInt())
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        setSingleLine(true)
                        setOnClickListener {
                            diseasesList.remove(disease)
                            drawDiseaseTags()
                        }
                    }
                    tagRow.addView(delBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

                    tagContainer.addView(tagRow, LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = dp(6) })
                }
            }
        }

        // Draw initial tag state
        drawDiseaseTags()
        mainContainer.addView(tagContainer)

        // Add a new disease prompt row
        val addRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(10))
        }
        val addInput = EditText(this).apply {
            hint = "添加新疾病，如：高血压3级"
            textSize = 16f
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        addRow.addView(addInput, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val addBtn = Button(this).apply {
            text = "添加 ➕"
            textSize = 15f
            setTextColor(0xFFFFFFFF.toInt())
            background = ContextCompat.getDrawable(this@BloodPressureActivity, R.drawable.button_primary)
            setOnClickListener {
                val newDisease = addInput.text.toString().trim()
                if (newDisease.isNotEmpty()) {
                    if (!diseasesList.contains(newDisease)) {
                        diseasesList.add(newDisease)
                        drawDiseaseTags()
                    }
                    addInput.setText("")
                }
            }
        }
        addRow.addView(addBtn, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
            leftMargin = dp(8)
        })
        mainContainer.addView(addRow)

        AlertDialog.Builder(this)
            .setTitle("填写使用人员资料")
            .setView(mainContainer)
            .setPositiveButton("保存资料", null)
            .setNegativeButton("取消", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark)
                )
                getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(0xFF777777.toInt())

                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val age = ageInput.text.toString().trim()
                    val gender = genderInput.text.toString().trim()
                    val diseasesMerged = diseasesList.joinToString("、")

                    settings.userAge = age
                    settings.userGender = gender
                    settings.userChronicDiseases = diseasesMerged

                    Toast.makeText(this@BloodPressureActivity, "资料保存完成", Toast.LENGTH_SHORT).show()
                    dismiss()
                }
            }
    }

    private fun showCustomizeRangeDialog() {
        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        val (sysMinPanel, sysMinInput) = createDialogInputField(
            this,
            "收缩压 (高压) 正常下限 (mmHg) :",
            "默认值: 90",
            settings.bpSystolicMin.toString(),
            InputType.TYPE_CLASS_NUMBER
        )
        dialogLayout.addView(sysMinPanel)

        val (sysMaxPanel, sysMaxInput) = createDialogInputField(
            this,
            "收缩压 (高压) 正常上限 (mmHg) :",
            "默认值: 139",
            settings.bpSystolicMax.toString(),
            InputType.TYPE_CLASS_NUMBER
        )
        dialogLayout.addView(sysMaxPanel)

        val (diaMinPanel, diaMinInput) = createDialogInputField(
            this,
            "舒张压 (低压) 正常下限 (mmHg) :",
            "默认值: 60",
            settings.bpDiastolicMin.toString(),
            InputType.TYPE_CLASS_NUMBER
        )
        dialogLayout.addView(diaMinPanel)

        val (diaMaxPanel, diaMaxInput) = createDialogInputField(
            this,
            "舒张压 (低压) 正常上限 (mmHg) :",
            "默认值: 89",
            settings.bpDiastolicMax.toString(),
            InputType.TYPE_CLASS_NUMBER
        )
        dialogLayout.addView(diaMaxPanel)

        AlertDialog.Builder(this)
            .setTitle("自定义血压安全范围")
            .setView(dialogLayout)
            .setPositiveButton("确认保存", null)
            .setNegativeButton("恢复默认", null)
            .show()
            .apply {
                getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark)
                )
                getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(0xFF777777.toInt())

                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val sMin = sysMinInput.text.toString().toIntOrNull()
                    val sMax = sysMaxInput.text.toString().toIntOrNull()
                    val dMin = diaMinInput.text.toString().toIntOrNull()
                    val dMax = diaMaxInput.text.toString().toIntOrNull()

                    if (sMin == null || sMax == null || dMin == null || dMax == null) {
                        Toast.makeText(this@BloodPressureActivity, "请完整填写所有范围数值", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (sMin !in 40..260 || sMax !in 40..260 || dMin !in 30..180 || dMax !in 30..180) {
                        Toast.makeText(this@BloodPressureActivity, "填写的数值超出安全调节区间，请重新输入", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (sMin >= sMax) {
                        Toast.makeText(this@BloodPressureActivity, "高压下限不能高于或等于上限", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    if (dMin >= dMax) {
                        Toast.makeText(this@BloodPressureActivity, "低压下限不能高于或等于上限", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    settings.bpSystolicMin = sMin
                    settings.bpSystolicMax = sMax
                    settings.bpDiastolicMin = dMin
                    settings.bpDiastolicMax = dMax

                    Toast.makeText(this@BloodPressureActivity, "血压安全范围配置完成", Toast.LENGTH_SHORT).show()
                    refreshData()
                    dismiss()
                }

                getButton(DialogInterface.BUTTON_NEGATIVE).setOnClickListener {
                    settings.bpSystolicMin = 90
                    settings.bpSystolicMax = 139
                    settings.bpDiastolicMin = 60
                    settings.bpDiastolicMax = 89
                    Toast.makeText(this@BloodPressureActivity, "已成功恢复默认范围 (90-139 / 60-89)", Toast.LENGTH_SHORT).show()
                    refreshData()
                    dismiss()
                }
            }
    }

    private fun showFillDialog(periodKey: String, existing: BloodPressureRecord?) {
        val dateStr = dateFormatter.format(selectedCalendar.time)
        val displayDate = displayDateFormatter.format(selectedCalendar.time)

        val dialogLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(10), dp(20), dp(10))
        }

        // Title description inside dialog
        dialogLayout.addView(TextView(this).apply {
            text = "记录日期: $displayDate ($periodKey)"
            textSize = 18f
            setTextColor(0xFF555555.toInt())
            setPadding(0, 0, 0, dp(12))
        })

        // Input 1: High Pressure
        val (sysPanel, sysInput) = createDialogInputField(
            this,
            "高压 (收缩压) mmHg:",
            "推荐范围 ${settings.bpSystolicMin} - ${settings.bpSystolicMax}",
            existing?.systolic?.toString() ?: "120",
            InputType.TYPE_CLASS_NUMBER
        )
        dialogLayout.addView(sysPanel)

        // Input 2: Low Pressure
        val (diaPanel, diaInput) = createDialogInputField(
            this,
            "低压 (舒张压) mmHg:",
            "推荐范围 ${settings.bpDiastolicMin} - ${settings.bpDiastolicMax}",
            existing?.diastolic?.toString() ?: "80",
            InputType.TYPE_CLASS_NUMBER
        )
        dialogLayout.addView(diaPanel)

        // Input 3: Heart Rate
        val (hrPanel, hrInput) = createDialogInputField(
            this,
            "心率 (次/分钟) :",
            "一般在 60 - 100 之间",
            existing?.heartRate?.toString() ?: "75",
            InputType.TYPE_CLASS_NUMBER
        )
        dialogLayout.addView(hrPanel)

        AlertDialog.Builder(this)
            .setTitle("${if (existing != null) "修改" else "填写"}血压数据 ($periodKey)")
            .setView(dialogLayout)
            .setPositiveButton("确认保存", null) // Set to null first to override dismiss behaviour on validation failure
            .setNegativeButton("取消", null)
            .show()
            .apply {
                // Ensure text colors on action buttons
                getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(
                    ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark)
                )
                getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(0xFF777777.toInt())

                // Override positive button onClick to perform input validation
                getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                    val sysStr = sysInput.text.toString().trim()
                    val diaStr = diaInput.text.toString().trim()
                    val hrStr = hrInput.text.toString().trim()

                    val sys = sysStr.toIntOrNull()
                    val dia = diaStr.toIntOrNull()
                    val hr = hrStr.toIntOrNull()

                    if (sys == null || dia == null || hr == null) {
                        Toast.makeText(this@BloodPressureActivity, "请完整填写三个数值", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Bounds checking
                    if (sys !in 40..260) {
                        Toast.makeText(this@BloodPressureActivity, "高压范围应为 40 - 260 mmHg", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (dia !in 30..180) {
                        Toast.makeText(this@BloodPressureActivity, "低压范围应为 30 - 180 mmHg", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (hr !in 30..220) {
                        Toast.makeText(this@BloodPressureActivity, "心率范围应为 30 - 220 次/分", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    if (sys < dia) {
                        Toast.makeText(this@BloodPressureActivity, "高压（收缩压）不能低于低压（舒张压）", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Save to database
                    dbHelper.saveBloodPressure(dateStr, periodKey, sys, dia, hr)
                    Toast.makeText(this@BloodPressureActivity, "保存成功", Toast.LENGTH_SHORT).show()
                    
                    // Refresh and dismiss
                    refreshData()
                    dismiss()
                }
            }
    }

    private fun createDialogInputField(
        context: Context,
        labelText: String,
        hintText: String,
        initValue: String,
        inputTypeValue: Int
    ): Pair<LinearLayout, EditText> {
        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, dp(10))
        }
        layout.addView(TextView(context).apply {
            text = labelText
            textSize = 17f
            setTextColor(0xFF333333.toInt())
            setPadding(0, 0, 0, dp(4))
        })
        val input = EditText(context).apply {
            setText(initValue)
            hint = hintText
            textSize = 19f
            inputType = inputTypeValue
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        layout.addView(input)
        return Pair(layout, input)
    }

    private fun showDatePicker() {
        val year = selectedCalendar.get(Calendar.YEAR)
        val month = selectedCalendar.get(Calendar.MONTH)
        val day = selectedCalendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(this, { _, yr, mn, dy ->
            selectedCalendar.set(Calendar.YEAR, yr)
            selectedCalendar.set(Calendar.MONTH, mn)
            selectedCalendar.set(Calendar.DAY_OF_MONTH, dy)
            refreshData()
        }, year, month, day).show()
    }

    private fun evaluateBloodPressure(systolic: Int, diastolic: Int): BPStatus {
        val sysMin = settings.bpSystolicMin
        val sysMax = settings.bpSystolicMax
        val diaMin = settings.bpDiastolicMin
        val diaMax = settings.bpDiastolicMax

        return when {
            systolic > sysMax || diastolic > diaMax -> BPStatus.HIGH
            systolic < sysMin || diastolic < diaMin -> BPStatus.LOW
            systolic in (sysMax - 19)..sysMax || diastolic in (diaMax - 9)..diaMax -> BPStatus.PRE_HIGH
            else -> BPStatus.NORMAL
        }
    }

    private fun getRecent30DaysDataForAI(dbHelper: BloodPressureDbHelper): String {
        val records = dbHelper.getRecentRecords(limit = 90)
        if (records.isEmpty()) return "无数据"

        val sb = java.lang.StringBuilder()
        val grouped = records.groupBy { it.date }.toSortedMap()

        grouped.forEach { (date, dayRecords) ->
            val recordStrings = dayRecords.map { rec ->
                "${rec.period}:${rec.systolic}/${rec.diastolic}mmHg(心率:${rec.heartRate})"
            }
            sb.append("$date [${recordStrings.joinToString(", ")}]\n")
        }
        return sb.toString()
    }

    private fun runAiEvaluation(button: Button, resultText: TextView) {
        val apiKey = settings.deepseekApiKey
        if (apiKey.isBlank()) {
            Toast.makeText(this, "请让家人先在‘家人设置’中配置有效的 DeepSeek API 密钥和接口地址", Toast.LENGTH_LONG).show()
            return
        }

        val records = dbHelper.getRecentRecords(limit = 90)
        val grouped = records.groupBy { it.date }
        if (grouped.size < 7) {
            Toast.makeText(this, "目前数据量不足（建议至少记录 7 天以上），AI 无法准确分析趋势，请继续坚持记录", Toast.LENGTH_LONG).show()
            return
        }

        val now = System.currentTimeMillis()
        val lastTime = settings.lastAiEvaluationTimeMillis
        val isCooldowned = (now - lastTime) >= 24 * 60 * 60 * 1000L
        if (!isCooldowned && settings.lastAiEvaluationResult.isNotBlank()) {
            Toast.makeText(this, "今天已经分析评估过了，每天仅限科学评估一次，请明天再试", Toast.LENGTH_LONG).show()
            return
        }

        button.isEnabled = false
        button.text = "正在咨询 AI 医生，请稍候..."
        resultText.text = "正在整理您的血压历史，并生成深度趋势分析，大概需要十多秒，请不要退出页面..."

        val dataStr = getRecent30DaysDataForAI(dbHelper)

        val thread = Thread {
            try {
                var baseUrl = settings.deepseekApiUrl
                if (!baseUrl.endsWith("/")) {
                    baseUrl += "/"
                }
                val urlConnection = java.net.URL(baseUrl + "chat/completions").openConnection() as java.net.HttpURLConnection
                urlConnection.requestMethod = "POST"
                urlConnection.connectTimeout = 30000
                urlConnection.readTimeout = 30000
                urlConnection.doOutput = true
                urlConnection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                urlConnection.setRequestProperty("Authorization", "Bearer $apiKey")

                val systemPrompt = "你是一位资深的心血管医生。请阅读用户近 30 天的血压和心率变化数据，并按以下严格的格式输出近期健康评分与建议。不要多余的寒暄，直接输出以下结构：\\\\n\\\\n### 📊 近期综合健康评分：[请评估一个0-100的分数，并说明主因]\\\\n\\\\n### 📈 数值趋势特征：\\\\n- [如：清晨血压偏高/波动较大/心率平稳等]\\\\n\\\\n### 🍎 针对性作息与饮食建议：\\\\n1. [建议1]\\\\n2. [建议2]\\\\n\\\\n免责声明：此评估基于历史数据生成，仅供参考，不作为确诊与药物治疗依据。"
                
                val profileInfo = StringBuilder()
                if (settings.userAge.isNotEmpty()) {
                    profileInfo.append("年龄：${settings.userAge}岁；")
                }
                if (settings.userGender.isNotEmpty()) {
                    profileInfo.append("性别：${settings.userGender}；")
                }
                if (settings.userChronicDiseases.isNotEmpty()) {
                    profileInfo.append("基础病与医学史：${settings.userChronicDiseases}；")
                }
                val profileStr = if (profileInfo.isNotEmpty()) "使用人员基本信息：${profileInfo.toString()}\\n" else ""

                val safeUserContent = dataStr.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "")
                
                val safeProfileStr = profileStr.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "")

                val model = settings.deepseekModel.ifBlank { "deepseek-chat" }
                val jsonBody = "{\"model\":\"$model\",\"messages\":[{\"role\":\"system\",\"content\":\"$systemPrompt\"},{\"role\":\"user\",\"content\":\"${safeProfileStr}近30天测得的血压数据如下：\\\\n$safeUserContent\"}],\"temperature\":0.3}"

                urlConnection.outputStream.use { os ->
                    val bytes = jsonBody.toByteArray(Charsets.UTF_8)
                    os.write(bytes, 0, bytes.size)
                }

                val responseCode = urlConnection.responseCode
                if (responseCode == 200) {
                    val responseText = urlConnection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    
                    // Simple parse content from JSON to avoid large dependency
                    val contentKey = "\"content\":\""
                    val contentStartIdx = responseText.indexOf(contentKey)
                    if (contentStartIdx != -1) {
                        val start = contentStartIdx + contentKey.length
                        var end = start
                        val resultSb = java.lang.StringBuilder()
                        var escaped = false
                        while (end < responseText.length) {
                            val char = responseText[end]
                            if (escaped) {
                                when (char) {
                                    'n' -> resultSb.append('\n')
                                    't' -> resultSb.append('\t')
                                    'r' -> resultSb.append('\r')
                                    '\\' -> resultSb.append('\\')
                                    '"' -> resultSb.append('"')
                                    else -> resultSb.append(char)
                                }
                                escaped = false
                            } else if (char == '\\') {
                                escaped = true
                            } else if (char == '"') {
                                break
                            } else {
                                resultSb.append(char)
                            }
                            end++
                        }

                        val parsedResult = resultSb.toString().trim()
                        if (parsedResult.isNotBlank()) {
                            runOnUiThread {
                                settings.lastAiEvaluationResult = parsedResult
                                settings.lastAiEvaluationTimeMillis = System.currentTimeMillis()
                                val timeStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(settings.lastAiEvaluationTimeMillis))
                                resultText.text = "【评估时间：$timeStr】\n\n$parsedResult"
                                button.isEnabled = true
                                button.text = "获取 AI 健康评价"
                                Toast.makeText(this@BloodPressureActivity, "智能评估生成成功！", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            throw Exception("AI 未返回有效内容")
                        }
                    } else {
                        throw Exception("JSON 响应结构异常")
                    }
                } else {
                    val errorStream = urlConnection.errorStream
                    val errorText = errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: ""
                    throw Exception("HTTP $responseCode: $errorText")
                }
            } catch (e: Exception) {
                runOnUiThread {
                    button.isEnabled = true
                    button.text = "重新获取 AI 健康评价"
                    resultText.text = "【评估失败】\n\n原因：${e.message}\n\n建议：请检查是否连接了网络，或者在“家人设置”中仔细校验 DeepSeek API 密钥和接口路由是否配置无误。"
                    Toast.makeText(this@BloodPressureActivity, "评估分析失败：${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        thread.start()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class PeriodConfig(
        val key: String,
        val title: String,
        val defaultRange: String
    )

    private enum class BPStatus(val label: String, val color: Int, val tips: String) {
        NORMAL("血压正常", 0xFF2E7D32.toInt(), "您的血压很棒，请继续保持！"), // Dark Green
        PRE_HIGH("正常偏高", 0xFFEF6C00.toInt(), "数值处于正常高值范围，请适度控制食盐摄入。"), // Dark Orange
        HIGH("血压偏高", 0xFFC62828.toInt(), "血压偏高，建议多躺下休息！如持续偏高请咨询医生意见。"), // Dark Red
        LOW("血压偏低", 0xFF1565C0.toInt(), "血压有些偏低，请适度补充营养与水分，站立时动作要放缓下身。") // Dark Blue
    }
}

data class DailyAverage(
    val dateLabel: String,
    val avgSystolic: Int,
    val avgDiastolic: Int
)

class BloodPressureChartView(context: Context) : View(context) {
    private var chartData: List<DailyAverage> = emptyList()

    private val paintSystolic = Paint().apply {
        color = 0xFFD32F2F.toInt() // Red for systolic
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintDiastolic = Paint().apply {
        color = 0xFF1976D2.toInt() // Blue for diastolic
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val paintLine = Paint().apply {
        color = 0xFFCCCCCC.toInt()
        strokeWidth = dpToPx(1f)
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val paintLabel = Paint().apply {
        color = 0xFF555555.toInt()
        textSize = dpToPx(12f)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    private val paintValText = Paint().apply {
        color = 0xFF333333.toInt()
        textSize = dpToPx(10f)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun setData(data: List<DailyAverage>) {
        this.chartData = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (chartData.isEmpty()) return

        val w = width.toFloat()
        val h = height.toFloat()

        val paddingLeft = dpToPx(20f)
        val paddingRight = dpToPx(20f)
        val paddingTop = dpToPx(20f)
        val paddingBottom = dpToPx(25f)

        val graphWidth = w - paddingLeft - paddingRight
        val graphHeight = h - paddingTop - paddingBottom

        // Draw baseline
        canvas.drawLine(paddingLeft, h - paddingBottom, w - paddingRight, h - paddingBottom, paintLine)

        val count = chartData.size
        val stepX = graphWidth / count

        // Max possible scale is 200 mmHg
        val maxVal = 200f

        for (i in 0 until count) {
            val item = chartData[i]
            val x = paddingLeft + (i * stepX) + (stepX / 2)

            if (item.avgSystolic > 0 && item.avgDiastolic > 0) {
                // Width of bars: we draw two adjacent bars
                val barWidth = stepX * 0.35f
                val spacingVal = stepX * 0.05f

                // Systolic bar
                val sysPct = item.avgSystolic.toFloat() / maxVal
                val sysBarHeight = graphHeight * sysPct
                val sysY = h - paddingBottom - sysBarHeight
                val sysLeft = x - barWidth - spacingVal
                val sysRight = x - spacingVal
                canvas.drawRect(sysLeft, sysY, sysRight, h - paddingBottom, paintSystolic)

                // Diastolic bar
                val diaPct = item.avgDiastolic.toFloat() / maxVal
                val diaBarHeight = graphHeight * diaPct
                val diaY = h - paddingBottom - diaBarHeight
                val diaLeft = x + spacingVal
                val diaRight = x + barWidth + spacingVal
                canvas.drawRect(diaLeft, diaY, diaRight, h - paddingBottom, paintDiastolic)

                // Value labels on top of bars
                canvas.drawText(item.avgSystolic.toString(), x - barWidth/2 - spacingVal, sysY - dpToPx(3f), paintValText)
                canvas.drawText(item.avgDiastolic.toString(), x + barWidth/2 + spacingVal, diaY - dpToPx(3f), paintValText)
            } else {
                // Draw "空" for missing data
                canvas.drawText("空", x, h - paddingBottom - graphHeight / 2, paintValText)
            }

            // Date label
            canvas.drawText(item.dateLabel, x, h - paddingBottom + dpToPx(18f), paintLabel)
        }
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
