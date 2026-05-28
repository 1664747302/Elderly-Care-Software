package com.example.elderreminder

import android.content.Context
import android.graphics.*
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HealthReportRenderer {

    fun generateAndSaveReport(context: Context): File? {
        val width = 800
        val height = 1800
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. 背景色
        val bgPaint = Paint().apply {
            color = 0xFFFAFAF7.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. 头部板块 (深绿色背景)
        val headerPaint = Paint().apply {
            color = 0xFF1F6B45.toInt()
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 200f, headerPaint)

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 34f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        canvas.drawText("老人关怀 · 健康与用眼周报", 40f, 85f, titlePaint)

        val subtitlePaint = Paint().apply {
            color = 0xFFDDDDDD.toInt()
            textSize = 18f
            isAntiAlias = true
        }
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        canvas.drawText("报告生成时间: $todayStr (包含今日三次血压与近7天趋势)", 40f, 140f, subtitlePaint)

        val sectionTitlePaint = Paint().apply {
            color = 0xFF1F6B45.toInt()
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val sectionLinePaint = Paint().apply {
            color = 0xFFCCCCCC.toInt()
            strokeWidth = 2f
            style = Paint.Style.STROKE
        }

        // ------------------ 3. 今日血压自测记录 ------------------
        canvas.drawText("📊 今日血压自测记录 ($todayStr)", 40f, 260f, sectionTitlePaint)
        canvas.drawLine(40f, 280f, 760f, 280f, sectionLinePaint)

        val bpDb = BloodPressureDbHelper(context)
        val morningRec = bpDb.getBloodPressure(todayStr, "早晨")
        val noonRec = bpDb.getBloodPressure(todayStr, "中午")
        val eveningRec = bpDb.getBloodPressure(todayStr, "晚上")

        val appSettings = AppSettings(context)
        val sysMin = appSettings.bpSystolicMin
        val sysMax = appSettings.bpSystolicMax
        val diaMin = appSettings.bpDiastolicMin
        val diaMax = appSettings.bpDiastolicMax

        fun evaluateBloodPressure(systolic: Int, diastolic: Int): String {
            return when {
                systolic > sysMax || diastolic > diaMax -> "偏高 🔴"
                systolic < sysMin || diastolic < diaMin -> "偏低 🔵"
                systolic in (sysMax - 19)..sysMax || diastolic in (diaMax - 9)..diaMax -> "正常偏高 🟡"
                else -> "正常 🟢"
            }
        }

        val textPaint = Paint().apply {
            color = 0xFF333333.toInt()
            textSize = 20f
            isAntiAlias = true
        }

        var yPos = 320f
        val mStr = morningRec?.let { "🌅 早晨: ${it.systolic} / ${it.diastolic} mmHg (心率 ${it.heartRate}) — ${evaluateBloodPressure(it.systolic, it.diastolic)}" } ?: "🌅 早晨: （今天自测数据未录入）"
        canvas.drawText(mStr, 60f, yPos, textPaint)

        yPos += 45f
        val nStr = noonRec?.let { "☀️ 中午: ${it.systolic} / ${it.diastolic} mmHg (心率 ${it.heartRate}) — ${evaluateBloodPressure(it.systolic, it.diastolic)}" } ?: "☀️ 中午: （今天自测数据未录入）"
        canvas.drawText(nStr, 60f, yPos, textPaint)

        yPos += 45f
        val eStr = eveningRec?.let { "🌙 晚上: ${it.systolic} / ${it.diastolic} mmHg (心率 ${it.heartRate}) — ${evaluateBloodPressure(it.systolic, it.diastolic)}" } ?: "🌙 晚上: （今天自测数据未录入）"
        canvas.drawText(eStr, 60f, yPos, textPaint)


        // ------------------ 4. 近七天血压趋势图 ------------------
        canvas.drawText("📈 近七天平均血压趋势图", 40f, 500f, sectionTitlePaint)
        canvas.drawLine(40f, 520f, 760f, 520f, sectionLinePaint)

        // 图例 legend
        val legendPaint = Paint().apply {
            textSize = 15f
            isAntiAlias = true
        }
        canvas.drawRect(520f, 485f, 538f, 498f, Paint().apply { color = 0xFFFF4D4D.toInt(); style = Paint.Style.FILL })
        canvas.drawText("收缩压(高压)", 543f, 498f, legendPaint.apply { color = 0xFF555555.toInt() })

        canvas.drawRect(650f, 485f, 668f, 498f, Paint().apply { color = 0xFF3A86FF.toInt(); style = Paint.Style.FILL })
        canvas.drawText("舒张压(低压)", 673f, 498f, legendPaint.apply { color = 0xFF555555.toInt() })

        // 获取过去7天的日平均血压
        val listBP = mutableListOf<Pair<String, Pair<Int, Int>>>()
        val tempCal = Calendar.getInstance()
        tempCal.add(Calendar.DAY_OF_YEAR, -6)
        val chartDateFormatter = SimpleDateFormat("MM-dd", Locale.getDefault())

        for (i in 0 until 7) {
            val queryDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(tempCal.time)
            val labelStr = chartDateFormatter.format(tempCal.time)
            val dayRecords = bpDb.getRecordsForDate(queryDateStr)
            if (dayRecords.isEmpty()) {
                listBP.add(Pair(labelStr, Pair(0, 0)))
            } else {
                val avgSys = dayRecords.map { it.systolic }.average().toInt()
                val avgDia = dayRecords.map { it.diastolic }.average().toInt()
                listBP.add(Pair(labelStr, Pair(avgSys, avgDia)))
            }
            tempCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 绘制血压图表
        val graphLeft = 100f
        val graphRight = 740f
        val graphTop = 570f
        val graphBottom = 810f
        val graphWidth = graphRight - graphLeft
        val graphHeight = graphBottom - graphTop
        val maxValBP = 200f

        val gridPaint = Paint().apply {
            color = 0xFFE5E5E5.toInt()
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val axisLabelPaint = Paint().apply {
            color = 0xFF666666.toInt()
            textSize = 15f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        // 网格线
        for (yVal in listOf(50, 100, 150, 200)) {
            val gridY = graphBottom - (yVal.toFloat() / maxValBP) * graphHeight
            canvas.drawLine(graphLeft, gridY, graphRight, gridY, gridPaint)
            canvas.drawText("$yVal", graphLeft - 10f, gridY + 5f, axisLabelPaint)
        }
        // 底线
        canvas.drawLine(graphLeft, graphBottom, graphRight, graphBottom, sectionLinePaint)

        val stepBPX = graphWidth / 7f
        val barBPWidth = stepBPX * 0.28f

        val barHighPaint = Paint().apply { color = 0xFFFF4D4D.toInt(); style = Paint.Style.FILL }
        val barLowPaint = Paint().apply { color = 0xFF3A86FF.toInt(); style = Paint.Style.FILL }
        val valTextPaint = Paint().apply {
            color = 0xFF333333.toInt()
            textSize = 13f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
        }
        val dateLabelPaint = Paint().apply {
            color = 0xFF555555.toInt()
            textSize = 15f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        for (i in 0 until 7) {
            val dayData = listBP[i]
            val dateLabel = dayData.first
            val highVal = dayData.second.first
            val lowVal = dayData.second.second

            val centerX = graphLeft + (i * stepBPX) + (stepBPX / 2f)

            if (highVal > 0 && lowVal > 0) {
                // 高压柱子
                val highH = (highVal.toFloat() / maxValBP) * graphHeight
                val highX1 = centerX - barBPWidth - 2f
                val highX2 = centerX - 2f
                val highY = graphBottom - highH
                canvas.drawRect(highX1, highY, highX2, graphBottom, barHighPaint)
                canvas.drawText("$highVal", (highX1+highX2)/2f, highY - 5f, valTextPaint)

                // 低压柱子
                val lowH = (lowVal.toFloat() / maxValBP) * graphHeight
                val lowX1 = centerX + 2f
                val lowX2 = centerX + barBPWidth + 2f
                val lowY = graphBottom - lowH
                canvas.drawRect(lowX1, lowY, lowX2, graphBottom, barLowPaint)
                canvas.drawText("$lowVal", (lowX1+lowX2)/2f, lowY - 5f, valTextPaint)
            } else {
                canvas.drawText("缺记录", centerX, graphBottom - 20f, Paint().apply {
                    color = 0xFF999999.toInt()
                    textSize = 13f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                })
            }

            canvas.drawText(dateLabel, centerX, graphBottom + 25f, dateLabelPaint)
        }


        // ------------------ 5. 近七天用眼习惯周报 ------------------
        canvas.drawText("👁️ 近七天护眼提醒周报", 40f, 890f, sectionTitlePaint)
        canvas.drawLine(40f, 910f, 760f, 910f, sectionLinePaint)

        val reminderDb = ReminderHistoryDbHelper(context)
        val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
        val records = reminderDb.getRemindersSince(sevenDaysAgo)

        val totalCount = records.size
        val appCounts = records.groupBy { it.packageName }.mapValues { it.value.size }
        val mostUsedAppPackage = appCounts.maxByOrNull { it.value }?.key ?: "无"
        val mostUsedAppName = if (mostUsedAppPackage == "无") "无" else {
            try {
                val pm = context.packageManager
                val info = pm.getApplicationInfo(mostUsedAppPackage, 0)
                pm.getApplicationLabel(info).toString()
            } catch (e: Exception) {
                mostUsedAppPackage
            }
        }

        // 计算每日提醒频次趋势
        val dailyRemCounts = mutableListOf<Pair<String, Int>>()
        for (i in 6 downTo 0) {
            val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
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
            val label = chartDateFormatter.format(Date(startOfDay))
            val dayCount = records.count { it.timestamp in startOfDay..endOfDay }
            dailyRemCounts.add(Pair(label, dayCount))
        }

        val busiestDay = dailyRemCounts.maxByOrNull { it.second }
        val busiestDayText = busiestDay?.let { "${it.first} (超时提醒计 ${it.second}次)" } ?: "无"

        var yPosRem = 950f
        canvas.drawText("• 超时疲劳提醒总次数:  ${totalCount} 次", 60f, yPosRem, textPaint)
        yPosRem += 45f
        canvas.drawText("• 最常超时使用的应用:  $mostUsedAppName", 60f, yPosRem, textPaint)
        yPosRem += 45f
        canvas.drawText("• 单日超时次数最多日期: $busiestDayText", 60f, yPosRem, textPaint)


        // ------------------ 6. 每日超时提醒频次趋势图 ------------------
        canvas.drawText("📊 每日超时提醒频次趋势图", 40f, 1120f, sectionTitlePaint)
        canvas.drawLine(40f, 1140f, 760f, 1140f, sectionLinePaint)

        val graphRemLeft = 100f
        val graphRemRight = 740f
        val graphRemTop = 1190f
        val graphRemBottom = 1430f
        val graphRemWidth = graphRemRight - graphRemLeft
        val graphRemHeight = graphRemBottom - graphRemTop

        val maxRemCount = (dailyRemCounts.maxOfOrNull { it.second } ?: 1).coerceAtLeast(4).toFloat()

        val interval = maxRemCount / 4.0
        val gridValues = listOf(
            Math.round(interval * 1).toInt(),
            Math.round(interval * 2).toInt(),
            Math.round(interval * 3).toInt(),
            Math.round(interval * 4).toInt()
        ).distinct()

        // 绘制用眼图表网格
        for (yVal in gridValues) {
            if (yVal == 0) continue
            val gridY = graphRemBottom - (yVal.toFloat() / maxRemCount) * graphRemHeight
            canvas.drawLine(graphRemLeft, gridY, graphRemRight, gridY, gridPaint)
            canvas.drawText("$yVal", graphRemLeft - 10f, gridY + 5f, axisLabelPaint)
        }
        canvas.drawLine(graphRemLeft, graphRemBottom, graphRemRight, graphRemBottom, sectionLinePaint)

        val barRemPaint = Paint().apply {
            color = 0xFF1F6B45.toInt() // 绿色
            style = Paint.Style.FILL
        }

        val stepRemX = graphRemWidth / 7f
        val barRemWidth = stepRemX * 0.45f

        for (i in 0 until 7) {
            val dayData = dailyRemCounts[i]
            val dateLabel = dayData.first
            val countVal = dayData.second

            val centerX = graphRemLeft + (i * stepRemX) + (stepRemX / 2f)

            if (countVal > 0) {
                val barH = (countVal.toFloat() / maxRemCount) * graphRemHeight
                val x1 = centerX - barRemWidth / 2f
                val x2 = centerX + barRemWidth / 2f
                val y = graphRemBottom - barH
                canvas.drawRect(x1, y, x2, graphRemBottom, barRemPaint)
                canvas.drawText("$countVal", centerX, y - 5f, valTextPaint)
            } else {
                canvas.drawText("0", centerX, graphRemBottom - 5f, dateLabelPaint.apply { textSize = 13f })
            }

            canvas.drawText(dateLabel, centerX, graphRemBottom + 25f, dateLabelPaint.apply { textSize = 15f })
        }


        // ------------------ 7. 尾部信息 ------------------
        canvas.drawLine(40f, 1540f, 760f, 1540f, sectionLinePaint)

        val footerTitlePaint = Paint().apply {
            color = 0xFF1F6B45.toInt()
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("--- 老人关怀陪伴您的每一天 ---", 400f, 1590f, footerTitlePaint)

        val footerTextPaint = Paint().apply {
            color = 0xFF777777.toInt()
            textSize = 15f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("本数据报告完全由手机在本地安全记录并生成,", 400f, 1635f, footerTextPaint)
        canvas.drawText("绝不上传云端，保障个人及家人隐私安全。", 400f, 1665f, footerTextPaint)

        val disclaimerPaint = Paint().apply {
            color = 0xFF999999.toInt()
            textSize = 13f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText("免责声明：本分析展示内容仅作为日常参考，不替代专业门诊诊疗！", 400f, 1720f, disclaimerPaint)


        // 8. 保存图片到本地 shared_images cache 目录
        return try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "health_report_${System.currentTimeMillis()}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            bitmap.recycle()
        }
    }
}
