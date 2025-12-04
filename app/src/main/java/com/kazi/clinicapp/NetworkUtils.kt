package com.kazi.clinicapp

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object NetworkUtils {
    private val client = OkHttpClient()
    private const val SERVER_URL = "http://103.10.54.154:8020/Appointment/Save"
    private const val HISTORY_FILENAME = "history.jsonl"

    /**
     * Sends POST with jsonBody. Writes a single-line entry to internal history file.
     * Returns a human-readable result string (Status: CODE\nBODY) or "Error: message".
     */
    suspend fun sendPostRequest(context: Context, jsonBody: String): String = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(SERVER_URL)
                .post(jsonBody.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                val code = resp.code
                val body = resp.body?.string() ?: ""
                val result = "Status: $code\n$body"

                // append to history (best-effort)
                try {
                    val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                    val safeResp = body.replace("\n", " ").replace(" | ", " ")
                    val safePayload = jsonBody.replace("\n", " ").replace(" | ", " ")
                    val entry = "$timestamp | $code | $safeResp | $safePayload"
                    context.openFileOutput(HISTORY_FILENAME, Context.MODE_APPEND).use { fos ->
                        fos.write((entry.trim() + "\n").toByteArray())
                    }
                } catch (_: Exception) {
                    // ignore history write errors
                }

                return@withContext result
            }
        } catch (e: Exception) {
            val msg = e.message ?: "Unknown error"
            // log error to history
            try {
                val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                val safeErr = msg.replace("\n", " ").replace(" | ", " ")
                val safePayload = jsonBody.replace("\n", " ").replace(" | ", " ")
                val entry = "$timestamp | ERROR | $safeErr | $safePayload"
                context.openFileOutput(HISTORY_FILENAME, Context.MODE_APPEND).use { fos ->
                    fos.write((entry.trim() + "\n").toByteArray())
                }
            } catch (_: Exception) { /* ignore */ }

            return@withContext "Error: $msg"
        }
    }
}