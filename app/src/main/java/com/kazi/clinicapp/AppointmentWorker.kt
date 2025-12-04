package com.kazi.clinicapp

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AppointmentWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    companion object {
        // input keys
        const val KEY_PAYLOAD_JSON = "payloadJson"
        const val KEY_SCHEDULED_ISO = "scheduledIso"
        const val KEY_ATTEMPT = "attempt" // integer attempt counter starting at 0
        // notification id
        const val NOTIF_ID = 2001
    }

    override suspend fun doWork(): Result {
        val context = applicationContext

        // read payload from input (or fallback to minimal)
        val payloadJson = inputData.getString(KEY_PAYLOAD_JSON) ?: buildFallbackPayload()

        // attempt counter (we will manage retries manually)
        var attempt = inputData.getInt(KEY_ATTEMPT, 0)

        // max retries after first attempt = 2 (total attempts = attempt 0, retry1, retry2)
        val maxRetries = 2

        // Notify start
        NotificationHelper.showNotification(context, NOTIF_ID, "Appointment worker", "Starting attempt ${attempt + 1}")

        while (true) {
            // perform request
            NotificationHelper.showNotification(context, NOTIF_ID, "Appointment worker", "Attempt ${attempt + 1}: sending...")
            val resultString = try {
                NetworkUtils.sendPostRequest(context, payloadJson)
            } catch (e: Exception) {
                "Error: ${e.message ?: "unknown"}"
            }

            // If resultString starts with "Error:" consider failure; otherwise success.
            val isError = resultString.startsWith("Error:", ignoreCase = true)

            if (!isError) {
                // success
                NotificationHelper.showNotification(context, NOTIF_ID, "Appointment success", "Sent successfully: ${shorten(resultString)}")
                return Result.success()
            } else {
                // failure
                attempt++
                NotificationHelper.showNotification(context, NOTIF_ID, "Appointment failed", "Attempt $attempt failed: ${shorten(resultString)}")

                if (attempt > maxRetries) {
                    // give up
                    NotificationHelper.showNotification(context, NOTIF_ID, "Appointment failure", "All attempts failed")
                    return Result.failure()
                } else {
                    // wait 30 seconds and try again
                    delay(30_000L)
                    // build updated WorkData if you were re-enqueueing; but here we're in-loop so continue
                    continue
                }
            }
        }
    }

    private fun buildFallbackPayload(): String {
        // Minimal fallback payload — adjust to match server expected shape
        val visitDate = LocalDateTime.now().toLocalDate().toString()
        val payload = JsonUtils.buildJson(
            visitDate = visitDate,
            drCode = "0164",
            drName = "Prof.Dr. Jobaida Sultana , MBBS (DMC), FCPS (Gynae), MS (Gynae)",
            patientName = "Unknown",
            mobileNo = "Unknown",
            dob = "1970-01-01",
            ageYears = 0,
            ageMonths = 0,
            ageDays = 0,
            sex = "Unknown",
            visitType = ""
        )
        return payload
    }

    private fun shorten(s: String, max: Int = 180): String {
        return if (s.length <= max) s else s.take(max) + "..."
    }
}