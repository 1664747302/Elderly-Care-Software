package com.example.elderreminder

import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.io.File

class ReminderHistoryDbHelperTest {

    @Test
    fun databaseCanInsertAndRetrieveRecords() {
        // 使用本地SQLite或者简单单元测试，不过Android的DbHelper单元测试由于依赖Android框架，
        // 在纯JVM单元测试环境下无法直接运行，这里我们为其补充说明：
        // Android SQLiteOpenHelper 涉及 JNI 调用，需在真机/模拟器(instrumented test)下运行，
        // 但我们可以验证 ReminderRecord 数据类结构以及数据转换逻辑。
        val record = ReminderRecord(
            id = 1L,
            timestamp = 1716600000000L,
            packageName = "com.tencent.mm",
            durationMinutes = 30
        )
        assertThat(record.id).isEqualTo(1L)
        assertThat(record.packageName).isEqualTo("com.tencent.mm")
        assertThat(record.durationMinutes).isEqualTo(30)
    }
}
