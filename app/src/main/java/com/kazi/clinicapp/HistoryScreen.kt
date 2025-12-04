package com.kazi.clinicapp

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var items by remember { mutableStateOf(listOf<HistoryItem>()) }
    var showing by remember { mutableStateOf<HistoryItem?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Load history on first composition
    LaunchedEffect(Unit) {
        loading = true
        val raw = readHistoryFile(ctx)
        items = raw.mapIndexed { idx, line -> parseHistoryLine(line, idx + 1) }
        loading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                scope.launch {
                    val raw = readHistoryFile(ctx)
                    items = raw.mapIndexed { idx, line -> parseHistoryLine(line, idx + 1) }
                }
            }) {
                Text("Refresh")
            }

            Button(onClick = {
                scope.launch {
                    clearHistoryFile(ctx)
                    items = emptyList()
                }
            }) {
                Text("Clear")
            }
        }

        Spacer(Modifier.height(12.dp))

        when {
            loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            items.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No history yet.")
                }
            }
            else -> {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { entry ->
                        Card(modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showing = entry }) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(entry.patientName ?: entry.rawPayload.take(40), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.height(6.dp))
                                val visitDate = entry.visitDate ?: "—"
                                val mobile = entry.mobileNo ?: "—"
                                val ageText = if (entry.ageYears != null) "${entry.ageYears}y ${entry.ageMonths}m ${entry.ageDays}d" else "—"
                                Text("$visitDate • $mobile • $ageText", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(entry.drName ?: "—", style = MaterialTheme.typography.bodySmall)
                                    Text(if (entry.statusCodeOrError.equals("ERROR", true)) "Error" else "Status: ${entry.statusCodeOrError}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail dialog
    if (showing != null) {
        val it = showing!!
        AlertDialog(
            onDismissRequest = { showing = null },
            confirmButton = { TextButton(onClick = { showing = null }) { Text("Close") } },
            title = { Text("Details") },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Timestamp: ${it.timestamp}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                    Text("Status: ${if (it.statusCodeOrError.equals("ERROR", true)) "Error" else it.statusCodeOrError}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))

                    Text("Server response:", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(4.dp))
                    Text(it.responseBody.ifBlank { "(empty)" }, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))

                    Text("Summary", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))

                    val pName = it.patientName ?: "Unknown"
                    val vDate = it.visitDate ?: "Unknown"
                    val mobile = it.mobileNo ?: "Unknown"
                    val dr = it.drName ?: "Unknown"
                    val vType = it.visitType ?: "—"
                    val ageHuman = if (it.ageYears != null) "${it.ageYears} years, ${it.ageMonths} months, ${it.ageDays} days" else "Unknown"

                    Text("Patient: $pName", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Visit Date: $vDate", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Mobile: $mobile", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Age: $ageHuman", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Doctor: $dr", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("Visit Type: $vType", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))

                    Text("Raw payload:", style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    Text(it.rawPayload, style = MaterialTheme.typography.bodySmall)
                }
            }
        )
    }
}

// -------------------- Helpers --------------------

private fun parseHistoryLine(line: String, id: Int): HistoryItem {
    // Expecting: TIMESTAMP | STATUS | RESPONSE | PAYLOAD
    val parts = line.split(" | ", limit = 4)
    val timestamp = parts.getOrNull(0) ?: ""
    val status = parts.getOrNull(1) ?: ""
    val responseBody = parts.getOrNull(2) ?: ""
    val payload = parts.getOrNull(3) ?: ""

    return try {
        val json = JSONObject(payload)
        val patientName = json.optString("PatientName").ifBlank { null }
        val visitDate = json.optString("VisitDate").ifBlank { null }
        val mobileNo = json.optString("MobileNo").ifBlank { null }
        val drName = json.optString("DrName").ifBlank { null }
        val visitType = json.optString("VisitType").ifBlank { null }

        val ageY = json.optString("AgeYear", "").toIntOrNull()
        val ageM = json.optString("AgeMonth", "").toIntOrNull()
        val ageD = json.optString("AgeDay", "").toIntOrNull()

        HistoryItem(
            id = id,
            timestamp = timestamp,
            statusCodeOrError = status,
            responseBody = responseBody,
            rawPayload = payload,
            patientName = patientName,
            visitDate = visitDate,
            mobileNo = mobileNo,
            ageYears = ageY,
            ageMonths = ageM,
            ageDays = ageD,
            drName = drName,
            visitType = visitType
        )
    } catch (e: Exception) {
        HistoryItem(
            id = id,
            timestamp = timestamp,
            statusCodeOrError = status,
            responseBody = responseBody,
            rawPayload = payload
        )
    }
}

private suspend fun readHistoryFile(context: Context): List<String> {
    return withContext(Dispatchers.IO) {
        try {
            context.openFileInput("history.jsonl").bufferedReader().use { br ->
                br.readLines().filter { it.isNotBlank() }.reversed()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

private suspend fun clearHistoryFile(context: Context) {
    withContext(Dispatchers.IO) {
        try {
            context.openFileOutput("history.jsonl", Context.MODE_PRIVATE).use { it.write(ByteArray(0)) }
        } catch (e: Exception) { /* ignore */ }
    }
}