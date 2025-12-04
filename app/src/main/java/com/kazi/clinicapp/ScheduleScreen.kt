package com.kazi.clinicapp

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.firstOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var targetDate by rememberSaveable { mutableStateOf(LocalDate.now()) }
    var targetHour by rememberSaveable { mutableStateOf(8) }
    var targetMinute by rememberSaveable { mutableStateOf(0) }

    var scheduledIso by rememberSaveable { mutableStateOf<String?>(null) }
    var statusText by rememberSaveable { mutableStateOf("Not scheduled") }

    var remainingSeconds by remember { mutableStateOf<Long?>(null) }
    var ticking by remember { mutableStateOf(false) }
    var pulse by remember { mutableStateOf(false) }

    var incomingPayload by rememberSaveable { mutableStateOf<String?>(null) }

    // helper to update countdown state
    fun updateCountdownFromIso(isoString: String) {
        try {
            val target = ZonedDateTime.parse(isoString)
            val now = ZonedDateTime.now(ZoneId.systemDefault())
            val seconds = Duration.between(now, target).seconds
            if (seconds > 0) {
                remainingSeconds = seconds
                statusText = "Pending"
                ticking = true
            } else {
                remainingSeconds = 0
                statusText = "Time passed"
                ticking = false
            }
        } catch (e: Exception) {
            statusText = "Invalid saved schedule"
            ticking = false
        }
    }

    // load incoming payload (from Send) and persisted schedule
    LaunchedEffect(Unit) {
        incomingPayload = SchedulePayloadHolder.latestPayload
        SchedulePayloadHolder.latestPayload = null

        SchedulePayloadHolder.latestVisitDate?.let { isoDate ->
            try { targetDate = LocalDate.parse(isoDate) } catch (_: Exception) {}
            SchedulePayloadHolder.latestVisitDate = null
        }

        val saved = readOneTimeSchedule(context)
        if (!saved.isNullOrBlank()) {
            scheduledIso = saved
            updateCountdownFromIso(saved)
        }
    }

    // countdown ticker loop
    LaunchedEffect(ticking) {
        while (ticking && (remainingSeconds ?: 0L) > 0L) {
            delay(1000L)
            remainingSeconds = (remainingSeconds ?: 0L) - 1L
            pulse = !pulse
            // also update an ongoing notification so the user sees countdown outside app
            scheduledIso?.let { iso ->
                val secs = remainingSeconds ?: 0L
                NotificationHelper.showCountdownNotification(context, 9999, "Scheduled Appointment", secs)
            }
            if ((remainingSeconds ?: 0L) <= 0L) {
                statusText = "Running / Reached"
                ticking = false
            }
        }
    }

    // pickers
    val cal = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, y, m, d -> targetDate = LocalDate.of(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    )
    val timePicker = TimePickerDialog(
        context,
        { _, h, m -> targetHour = h; targetMinute = m },
        targetHour,
        targetMinute,
        false
    )

    val scale by animateFloatAsState(targetValue = if (pulse) 1.08f else 1.00f, animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing))
    val warning = (remainingSeconds ?: Long.MAX_VALUE) in 0..59
    val countdownColor = if (warning) androidx.compose.ui.graphics.Color(0xFFEF5350) else MaterialTheme.colorScheme.primary

    Column(modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(16.dp)
    ) {
        Text("Schedule Appointment Hit (One-Time)", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(6.dp)) {
            Column(Modifier.padding(16.dp)) {

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = targetDate.toString(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Visit Date") },
                        trailingIcon = {
                            IconButton(onClick = { datePicker.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = "%02d:%02d".format(targetHour, targetMinute),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hit Time") },
                        trailingIcon = {
                            IconButton(onClick = { timePicker.show() }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                        },
                        modifier = Modifier.width(130.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            // Use incoming payload if provided; otherwise build from saved defaults
                            scope.launch {
                                val payload = incomingPayload ?: run {
                                    val patient = DataStoreManager.patientNameFlow(context).firstOrNull() ?: ""
                                    val mobile = DataStoreManager.mobileFlow(context).firstOrNull() ?: ""
                                    val dob = DataStoreManager.dobFlow(context).firstOrNull() ?: "1970-01-01"
                                    val sex = DataStoreManager.sexFlow(context).firstOrNull() ?: "Female"
                                    val vt = DataStoreManager.visitTypeFlow(context).firstOrNull() ?: ""
                                    val age = computeFullAge(dob)
                                    JsonUtils.buildJson(targetDate.toString(), "0164", "Prof.Dr. Jobaida Sultana , MBBS (DMC), FCPS (Gynae), MS (Gynae)", patient, mobile, dob, age.first, age.second, age.third, sex, vt)
                                }

                                val targetDT = targetDate.atTime(targetHour, targetMinute)
                                val zdt = ZonedDateTime.of(targetDT, ZoneId.systemDefault())
                                val now = ZonedDateTime.now(ZoneId.systemDefault())
                                if (zdt.isBefore(now)) {
                                    Toast.makeText(context, "Pick a future time", Toast.LENGTH_SHORT).show()
                                    return@launch
                                }

                                scheduleOneTimeAt(context, zdt, payload)
                                val iso = zdt.format(DateTimeFormatter.ISO_ZONED_DATE_TIME)
                                saveOneTimeSchedule(context, iso)
                                scheduledIso = iso
                                updateCountdownFromIso(iso)
                                incomingPayload = null
                                Toast.makeText(context, "Scheduled for $zdt", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Schedule Hit")
                    }

                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            cancelOneTimeSchedule(context)
                            clearOneTimeSchedule(context)
                            scheduledIso = null
                            remainingSeconds = null
                            ticking = false
                            pulse = false
                            statusText = "Not scheduled"
                        }
                    ) {
                        Text("Cancel")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Status: $statusText")

                Spacer(Modifier.height(8.dp))

                scheduledIso?.let {
                    val pretty = try {
                        ZonedDateTime.parse(it).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    } catch (e: Exception) {
                        it
                    }
                    Text("Scheduled For: $pretty")
                }

                Spacer(Modifier.height(12.dp))

                remainingSeconds?.let { sec ->
                    val h = sec / 3600
                    val m = (sec % 3600) / 60
                    val s = sec % 60
                    Text(
                        "Time Left: %02d:%02d:%02d".format(h, m, s),
                        style = MaterialTheme.typography.headlineSmall,
                        color = countdownColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .wrapContentWidth(Alignment.CenterHorizontally)
                    )
                } ?: Text("No countdown")
            }
        }
    }
}

// ----------------------- Helpers stored here so ScheduleScreen compiles -----------------------

/**
 * Persist a one-time schedule ISO string so the app remembers it after restart.
 */
private fun saveOneTimeSchedule(context: Context, isoZonedString: String) {
    try {
        context.openFileOutput("one_time_schedule.txt", Context.MODE_PRIVATE).use { fos ->
            fos.write(isoZonedString.toByteArray())
        }
    } catch (_: Exception) { /* ignore */ }
}

/**
 * Read persisted one-time schedule ISO string or null.
 */
private suspend fun readOneTimeSchedule(context: Context): String? {
    return withContext(Dispatchers.IO) {
        try {
            context.openFileInput("one_time_schedule.txt").bufferedReader().use { it.readText().trim() }.ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * Clear persisted one-time schedule.
 */
private fun clearOneTimeSchedule(context: Context) {
    try {
        context.openFileOutput("one_time_schedule.txt", Context.MODE_PRIVATE).use { it.write(ByteArray(0)) }
    } catch (_: Exception) { /* ignore */ }
}

/**
 * Enqueue a WorkManager one-time request that will run at the provided ZonedDateTime.
 * Input: payloadJson (string), scheduledIso (string), attempt (int default 0)
 */
fun scheduleOneTimeAt(context: Context, zonedDateTime: ZonedDateTime, payloadJson: String) {
    val now = ZonedDateTime.now(ZoneId.systemDefault())
    val delayMillis = java.time.Duration.between(now, zonedDateTime).toMillis()
    if (delayMillis <= 0) return

    val input = androidx.work.Data.Builder()
        .putString(AppointmentWorker.KEY_PAYLOAD_JSON, payloadJson)
        .putString(AppointmentWorker.KEY_SCHEDULED_ISO, zonedDateTime.toString())
        .putInt(AppointmentWorker.KEY_ATTEMPT, 0)
        .build()

    val work = androidx.work.OneTimeWorkRequestBuilder<AppointmentWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .setInputData(input)
        .addTag("one_time_appointment")
        .build()

    WorkManager.getInstance(context).enqueueUniqueWork(
        "one_time_appointment_unique",
        androidx.work.ExistingWorkPolicy.REPLACE,
        work
    )
}

/**
 * Cancel the scheduled one-time work (if exists).
 */
fun cancelOneTimeSchedule(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork("one_time_appointment_unique")
}