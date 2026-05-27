package com.example.elderreminder

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class BloodPressureDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_BLOOD_PRESSURE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BLOOD_PRESSURE")
        onCreate(db)
    }

    fun saveBloodPressure(date: String, period: String, systolic: Int, diastolic: Int, heartRate: Int) {
        val db = writableDatabase
        // Delete existing one if we are updating (or use INSERT OR REPLACE)
        db.delete(
            TABLE_BLOOD_PRESSURE,
            "$COLUMN_DATE = ? AND $COLUMN_PERIOD = ?",
            arrayOf(date, period)
        )

        val values = ContentValues().apply {
            put(COLUMN_DATE, date)
            put(COLUMN_PERIOD, period)
            put(COLUMN_SYSTOLIC, systolic)
            put(COLUMN_DIASTOLIC, diastolic)
            put(COLUMN_HEART_RATE, heartRate)
            put(COLUMN_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLE_BLOOD_PRESSURE, null, values)
    }

    fun getBloodPressure(date: String, period: String): BloodPressureRecord? {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BLOOD_PRESSURE,
            null,
            "$COLUMN_DATE = ? AND $COLUMN_PERIOD = ?",
            arrayOf(date, period),
            null, null, null
        )
        cursor.use { c ->
            if (c.moveToFirst()) {
                return BloodPressureRecord(
                    id = c.getLong(c.getColumnIndexOrThrow(COLUMN_ID)),
                    date = c.getString(c.getColumnIndexOrThrow(COLUMN_DATE)),
                    period = c.getString(c.getColumnIndexOrThrow(COLUMN_PERIOD)),
                    systolic = c.getInt(c.getColumnIndexOrThrow(COLUMN_SYSTOLIC)),
                    diastolic = c.getInt(c.getColumnIndexOrThrow(COLUMN_DIASTOLIC)),
                    heartRate = c.getInt(c.getColumnIndexOrThrow(COLUMN_HEART_RATE)),
                    timestamp = c.getLong(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                )
            }
        }
        return null
    }

    fun getRecordsForDate(date: String): List<BloodPressureRecord> {
        val list = mutableListOf<BloodPressureRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BLOOD_PRESSURE,
            null,
            "$COLUMN_DATE = ?",
            arrayOf(date),
            null, null, null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    BloodPressureRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COLUMN_ID)),
                        date = c.getString(c.getColumnIndexOrThrow(COLUMN_DATE)),
                        period = c.getString(c.getColumnIndexOrThrow(COLUMN_PERIOD)),
                        systolic = c.getInt(c.getColumnIndexOrThrow(COLUMN_SYSTOLIC)),
                        diastolic = c.getInt(c.getColumnIndexOrThrow(COLUMN_DIASTOLIC)),
                        heartRate = c.getInt(c.getColumnIndexOrThrow(COLUMN_HEART_RATE)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    )
                )
            }
        }
        return list
    }

    fun getRecentRecords(limit: Int = 21): List<BloodPressureRecord> {
        val list = mutableListOf<BloodPressureRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_BLOOD_PRESSURE,
            null,
            null, null,
            null, null,
            "$COLUMN_DATE DESC, $COLUMN_TIMESTAMP DESC",
            limit.toString()
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                list.add(
                    BloodPressureRecord(
                        id = c.getLong(c.getColumnIndexOrThrow(COLUMN_ID)),
                        date = c.getString(c.getColumnIndexOrThrow(COLUMN_DATE)),
                        period = c.getString(c.getColumnIndexOrThrow(COLUMN_PERIOD)),
                        systolic = c.getInt(c.getColumnIndexOrThrow(COLUMN_SYSTOLIC)),
                        diastolic = c.getInt(c.getColumnIndexOrThrow(COLUMN_DIASTOLIC)),
                        heartRate = c.getInt(c.getColumnIndexOrThrow(COLUMN_HEART_RATE)),
                        timestamp = c.getLong(c.getColumnIndexOrThrow(COLUMN_TIMESTAMP))
                    )
                )
            }
        }
        return list
    }

    companion object {
        const val DATABASE_NAME = "elder_blood_pressure.db"
        const val DATABASE_VERSION = 1

        const val TABLE_BLOOD_PRESSURE = "blood_pressures"
        const val COLUMN_ID = "_id"
        const val COLUMN_DATE = "date"
        const val COLUMN_PERIOD = "period"
        const val COLUMN_SYSTOLIC = "systolic"
        const val COLUMN_DIASTOLIC = "diastolic"
        const val COLUMN_HEART_RATE = "heart_rate"
        const val COLUMN_TIMESTAMP = "timestamp"

        private const val CREATE_TABLE_BLOOD_PRESSURE = """
            CREATE TABLE $TABLE_BLOOD_PRESSURE (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_DATE TEXT NOT NULL,
                $COLUMN_PERIOD TEXT NOT NULL,
                $COLUMN_SYSTOLIC INTEGER NOT NULL,
                $COLUMN_DIASTOLIC INTEGER NOT NULL,
                $COLUMN_HEART_RATE INTEGER NOT NULL,
                $COLUMN_TIMESTAMP INTEGER NOT NULL
            )
        """
    }
}

data class BloodPressureRecord(
    val id: Long,
    val date: String,     // "YYYY-MM-DD"
    val period: String,   // "早晨", "中午", "晚上"
    val systolic: Int,
    val diastolic: Int,
    val heartRate: Int,
    val timestamp: Long
)
