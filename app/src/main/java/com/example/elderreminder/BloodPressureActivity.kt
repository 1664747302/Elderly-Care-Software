package com.example.elderreminder

import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dbHelper = BloodPressureDbHelper(this)
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

        titleRow.addView(TextView(this).apply {
            text = "血压自测"
            textSize = 28f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Return Button
        titleRow.addView(Button(this).apply {
            text = "返回"
            textSize = 16f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            setBackgroundColor(0x00000000) // Transparent
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))

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

        // Recent Records Title
        root.addView(TextView(this).apply {
            text = "最近血压记录 (历史列表)"
            textSize = 21f
            setTextColor(ContextCompat.getColor(this@BloodPressureActivity, R.color.brand_green_dark))
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(10))
        })

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

        // Update Morning, Noon, Evening cards
        cardsContainer.removeAllViews()
        val periods = listOf(
            PeriodConfig("早晨", "🌅 早晨自测 (起床后/早餐前)", "90-139 / 60-89"),
            PeriodConfig("中午", "☀️ 中午自测 (饭前半小时/饭后)", "90-139 / 60-89"),
            PeriodConfig("晚上", "🌙 晚上自测 (临睡前半小时)", "90-139 / 60-89")
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
            "一般在 90 - 139 之间",
            existing?.systolic?.toString() ?: "120",
            InputType.TYPE_CLASS_NUMBER
        )
        dialogLayout.addView(sysPanel)

        // Input 2: Low Pressure
        val (diaPanel, diaInput) = createDialogInputField(
            this,
            "低压 (舒张压) mmHg:",
            "一般在 60 - 89 之间",
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
        return when {
            systolic >= 140 || diastolic >= 90 -> BPStatus.HIGH
            systolic < 90 || diastolic < 60 -> BPStatus.LOW
            systolic in 120..139 || diastolic in 80..89 -> BPStatus.PRE_HIGH
            else -> BPStatus.NORMAL
        }
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
