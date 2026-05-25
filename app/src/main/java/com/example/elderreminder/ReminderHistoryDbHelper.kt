package com.example.elderreminder

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ReminderHistoryDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_REMINDERS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_REMINDERS")
        onCreate(db)
    }

    fun insertReminder(timestamp: Long, packageName: String, durationMinutes: Int) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TIMESTAMP, timestamp)
            put(COLUMN_PACKAGE, packageName)
            put(COLUMN_DURATION, durationMinutes)
        }
        db.insert(TABLE_REMINDERS, null, values)
    }

    fun getRemindersSince(sinceMillis: Long): List<ReminderRecord> {
        val list = mutableListOf<ReminderRecord>()
        val db = readableDatabase
        val cursor = db.query(
            TABLE_REMINDERS,
            arrayOf(COLUMN_ID, COLUMN_TIMESTAMP, COLUMN_PACKAGE, COLUMN_DURATION),
            "$COLUMN_TIMESTAMP >= ?",
            arrayOf(sinceMillis.toString()),
            null, null, "$COLUMN_TIMESTAMP ASC"
        )
        cursor.use { c ->
            val idCol = c.getColumnIndexOrThrow(COLUMN_ID)
            val tsCol = c.getColumnIndexOrThrow(COLUMN_TIMESTAMP)
            val pkgCol = c.getColumnIndexOrThrow(COLUMN_PACKAGE)
            val durCol = c.getColumnIndexOrThrow(COLUMN_DURATION)
            while (c.moveToNext()) {
                list.add(
                    ReminderRecord(
                        id = c.getLong(idCol),
                        timestamp = c.getLong(tsCol),
                        packageName = c.getString(pkgCol),
                        durationMinutes = c.getInt(durCol)
                    )
                )
            }
        }
        return list
    }

    companion object {
        const val DATABASE_NAME = "elder_reminder_history.db"
        const val DATABASE_VERSION = 1

        const val TABLE_REMINDERS = "reminders"
        const val COLUMN_ID = "_id"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_PACKAGE = "package_name"
        const val COLUMN_DURATION = "duration_minutes"

        private const val CREATE_TABLE_REMINDERS = """
            CREATE TABLE $TABLE_REMINDERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TIMESTAMP INTEGER NOT NULL,
                $COLUMN_PACKAGE TEXT NOT NULL,
                $COLUMN_DURATION INTEGER NOT NULL
            )
        """
    }
}

data class ReminderRecord(
    val id: Long,
    val timestamp: Long,
    val packageName: String,
    val durationMinutes: Int
)
