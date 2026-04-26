package com.qrscanner.sheets

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ScanRecord(val id: Long, val timestamp: String, val scannedId: String)

class ScanDatabase(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "scans.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE scans (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "timestamp TEXT NOT NULL, " +
                "scanned_id TEXT NOT NULL)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS scans")
        onCreate(db)
    }

    fun insert(timestamp: String, scannedId: String) {
        val cv = ContentValues().apply {
            put("timestamp", timestamp)
            put("scanned_id", scannedId)
        }
        writableDatabase.insert("scans", null, cv)
    }

    fun getAll(): List<ScanRecord> {
        val list = mutableListOf<ScanRecord>()
        readableDatabase.rawQuery(
            "SELECT id, timestamp, scanned_id FROM scans ORDER BY id ASC", null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    ScanRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2))
                )
            }
        }
        return list
    }

    fun deleteAll() {
        writableDatabase.delete("scans", null, null)
    }

    fun count(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM scans", null).use { cursor ->
            cursor.moveToFirst()
            return cursor.getInt(0)
        }
    }
}
