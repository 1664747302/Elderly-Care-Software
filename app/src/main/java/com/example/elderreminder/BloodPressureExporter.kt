package com.example.elderreminder

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object BloodPressureExporter {

    /**
     * 获取指定范围的开始日期，格式: "yyyy-MM-dd"
     * rangeType: 1 = 近一个月, 2 = 近三个月, 3 = 近一年
     */
    fun getStartDateForRange(rangeType: Int): String {
        val calendar = Calendar.getInstance()
        when (rangeType) {
            1 -> calendar.add(Calendar.MONTH, -1)
            2 -> calendar.add(Calendar.MONTH, -3)
            3 -> calendar.add(Calendar.YEAR, -1)
            else -> calendar.add(Calendar.MONTH, -1) // 默认近一个月
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    /**
     * 从数据库中读取指定时间段的数据，并按天合并为单日均值
     */
    fun getGroupedDailyRecords(context: Context, rangeType: Int): List<DailyBPRecord> {
        val dbHelper = BloodPressureDbHelper(context)
        val startDateStr = getStartDateForRange(rangeType)
        
        // 查询该日期之后的所有血压记录
        val allRecords = dbHelper.getRecordsSince(startDateStr)
        
        // 按日期分组并排序
        val groupedMap = allRecords.groupBy { it.date }.toSortedMap(reverseOrder())
        
        val dailyRecords = mutableListOf<DailyBPRecord>()
        for ((date, records) in groupedMap) {
            val count = records.size
            if (count > 0) {
                val avgSystolic = Math.round(records.map { it.systolic }.average()).toInt()
                val avgDiastolic = Math.round(records.map { it.diastolic }.average()).toInt()
                val avgHeartRate = Math.round(records.map { it.heartRate }.average()).toInt()
                dailyRecords.add(DailyBPRecord(date, avgSystolic, avgDiastolic, avgHeartRate, count))
            }
        }
        return dailyRecords
    }

    /**
     * 将单日数据导出成 .xls 格式的 Excel 表格（使用 HTML-Excel 混合伪装，微信可以直接识别其为 Excel 文档并使用腾讯文档等直接完美预览与打开，且绝不乱码）
     */
    fun exportToExcelXls(context: Context, dailyRecords: List<DailyBPRecord>, rangeName: String): File? {
        return try {
            val cachePath = File(context.cacheDir, "shared_images")
            if (!cachePath.exists()) {
                cachePath.mkdirs()
            }
            
            // 导出的文件名，使用扩展名 .xls
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(cachePath, "血压心率数据_${rangeName}_${timestamp}.xls")
            val outputStream = FileOutputStream(file)
            val writer = outputStream.bufferedWriter(Charsets.UTF_8)
            
            val exportTimeStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            
            // 写入标准的 HTML 格式，使用 Excel-XML 命名空间和 MIME 类型伪装成标准 Excel 工作簿
            // 微信以及腾讯文档等能够瞬间完美解析并作为纯粹电子表格处理，规避由于 csv 引起的多平台乱码与微信无法识别
            writer.write("<html xmlns:o=\"urn:schemas-microsoft-com:office:office\" xmlns:x=\"urn:schemas-microsoft-com:office:excel\" xmlns=\"http://www.w3.org/TR/REC-html40\">\n")
            writer.write("<head>\n")
            writer.write("    <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n")
            writer.write("    <!--[if gte mso 9]><xml><x:ExcelWorkbook><x:ExcelWorksheets><x:ExcelWorksheet><x:Name>血压心率简报</x:Name><x:WorksheetOptions><x:DisplayGridlines/></x:WorksheetOptions></x:ExcelWorksheet></x:ExcelWorksheets></x:ExcelWorkbook></xml><![endif]-->\n")
            writer.write("    <style>\n")
            writer.write("        td {\n")
            writer.write("            mso-number-format:\"\\@\";\n") // 强制为文本格式以防乱码或数字格式变形
            writer.write("            text-align: center;\n")
            writer.write("            border: .5pt solid #cccccc;\n")
            writer.write("            padding: 10px;\n")
            writer.write("        }\n")
            writer.write("        .header {\n")
            writer.write("            background-color: #1F6B45;\n")
            writer.write("            color: #ffffff;\n")
            writer.write("            font-weight: bold;\n")
            writer.write("        }\n")
            writer.write("        .title-row {\n")
            writer.write("            font-size: 16pt;\n")
            writer.write("            font-weight: bold;\n")
            writer.write("            text-align: center;\n")
            writer.write("            background-color: #f4f8f5;\n")
            writer.write("            color: #12432B;\n")
            writer.write("        }\n")
            writer.write("        .info-row {\n")
            writer.write("            text-align: left;\n")
            writer.write("            font-size: 10pt;\n")
            writer.write("            color: #555555;\n")
            writer.write("        }\n")
            writer.write("        .warning-high {\n")
            writer.write("            color: #d32f2f;\n")
            writer.write("            font-weight: bold;\n")
            writer.write("        }\n")
            writer.write("        .warning-low {\n")
            writer.write("            color: #1976d2;\n")
            writer.write("            font-weight: bold;\n")
            writer.write("        }\n")
            writer.write("    </style>\n")
            writer.write("</head>\n")
            writer.write("<body>\n")
            writer.write("    <table>\n")
            
            // 标题行和元数据行 (让腾讯文档在预览时有非常整洁的头部引导)
            writer.write("        <tr><td colspan=\"5\" class=\"title-row\" style=\"height:40px;\">老年关怀 · 血压与心率历史自测表</td></tr>\n")
            writer.write("        <tr><td colspan=\"5\" class=\"info-row\">时间跨度：${rangeName} | 导出时间：${exportTimeStr}</td></tr>\n")
            writer.write("        <tr><td colspan=\"5\" class=\"info-row\">温馨提示：此表格已针对高低压做出了科学判定。您也可以在电脑端使用 Microsoft Excel 完好无损地编辑或打印。</td></tr>\n")
            writer.write("        <tr><td colspan=\"5\" style=\"height:10px;\"></td></tr>\n")
            
            // 表头
            writer.write("        <tr class=\"header\">\n")
            writer.write("            <td style=\"background-color:#1F6B45;color:#ffffff;font-weight:bold;\">日期</td>\n")
            writer.write("            <td style=\"background-color:#1F6B45;color:#ffffff;font-weight:bold;\">平均收缩压 (高压)</td>\n")
            writer.write("            <td style=\"background-color:#1F6B45;color:#ffffff;font-weight:bold;\">平均舒张压 (低压)</td>\n")
            writer.write("            <td style=\"background-color:#1F6B45;color:#ffffff;font-weight:bold;\">平均心率</td>\n")
            writer.write("            <td style=\"background-color:#1F6B45;color:#ffffff;font-weight:bold;\">测量次数</td>\n")
            writer.write("        </tr>\n")
            
            // 数据行
            for (record in dailyRecords) {
                val sysColor = when {
                    record.avgSystolic > 139 -> "color:#d32f2f;font-weight:bold;" // 高压偏高偏红
                    record.avgSystolic < 90 -> "color:#1976d2;font-weight:bold;" // 高压偏低偏蓝
                    else -> "color:#333333;"
                }
                val diaColor = when {
                    record.avgDiastolic > 89 -> "color:#d32f2f;font-weight:bold;" // 低压偏高偏红
                    record.avgDiastolic < 60 -> "color:#1976d2;font-weight:bold;" // 低压偏低偏蓝
                    else -> "color:#333333;"
                }
                
                writer.write("        <tr>\n")
                writer.write("            <td style=\"mso-number-format:'\\@';\">${record.date}</td>\n")
                writer.write("            <td style=\"$sysColor\">${record.avgSystolic} mmHg</td>\n")
                writer.write("            <td style=\"$diaColor\">${record.avgDiastolic} mmHg</td>\n")
                writer.write("            <td>${record.avgHeartRate} 次/分</td>\n")
                writer.write("            <td>${record.count} 次</td>\n")
                writer.write("        </tr>\n")
            }
            
            writer.write("    </table>\n")
            writer.write("</body>\n")
            writer.write("</html>\n")
            
            writer.flush()
            writer.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class DailyBPRecord(
    val date: String,
    val avgSystolic: Int,
    val avgDiastolic: Int,
    val avgHeartRate: Int,
    val count: Int
)
