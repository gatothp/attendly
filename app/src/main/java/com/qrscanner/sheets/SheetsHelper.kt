package com.qrscanner.sheets

import androidx.annotation.WorkerThread
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object SheetsHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    /** Append a single row immediately after scanning (direct-to-sheet mode). */
    @WorkerThread
    fun appendRow(
        scriptUrl: String,
        sheetName: String,
        timestamp: String,
        id: String
    ): Result<Unit> {
        val bodyJson = JSONObject()
            .put("sheetName", sheetName)
            .put("timestamp", timestamp)
            .put("id", id)
            .toString()
        return post(scriptUrl, bodyJson)
    }

    /** Append multiple rows in a single request (batch upload from local storage). */
    @WorkerThread
    fun appendRows(
        scriptUrl: String,
        sheetName: String,
        records: List<ScanRecord>
    ): Result<Unit> {
        val rowsArray = JSONArray()
        for (record in records) {
            rowsArray.put(
                JSONObject()
                    .put("timestamp", record.timestamp)
                    .put("id", record.scannedId)
            )
        }
        val bodyJson = JSONObject()
            .put("sheetName", sheetName)
            .put("rows", rowsArray)
            .toString()
        return post(scriptUrl, bodyJson)
    }

    // -------------------------------------------------------------------------

    private fun post(scriptUrl: String, bodyJson: String): Result<Unit> {
        val request = Request.Builder()
            .url(scriptUrl)
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val status = json?.optString("status") ?: "success"
                    if (status == "error") {
                        Result.failure(Exception(json?.optString("message") ?: "Apps Script error"))
                    } else {
                        Result.success(Unit)
                    }
                } else {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    Result.failure(Exception("HTTP ${response.code}: $errorBody"))
                }
            }
        } catch (e: IOException) {
            Result.failure(e)
        }
    }
}
