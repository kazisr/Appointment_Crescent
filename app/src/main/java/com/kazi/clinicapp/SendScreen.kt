package com.kazi.clinicapp

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    navController: NavHostController,
    sendPostRequest: suspend (String, (String) -> Unit) -> Unit,
    onScheduleRequest: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    // --- form state (survive rotation/navigation)
    var visitDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var patientName by rememberSaveable { mutableStateOf("Rownak Jahan Sharna") }
    var mobileNo by rememberSaveable { mutableStateOf("01959521675") }
    var dob by rememberSaveable { mutableStateOf("1999-01-28") }
    var sex by rememberSaveable { mutableStateOf("Female") }
    var visitType by rememberSaveable { mutableStateOf("") }

    var ageTriple by rememberSaveable { mutableStateOf(computeFullAge(dob)) }

    // result / status
    var resultText by rememberSaveable { mutableStateOf("") }
    var sending by rememberSaveable { mutableStateOf(false) }
    var showPayload by rememberSaveable { mutableStateOf(false) }

    // Date pickers
    val cal = Calendar.getInstance()
    val visitPicker = DatePickerDialog(
        context,
        { _, y, m, d -> visitDate = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )
    val dobPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            dob = "%04d-%02d-%02d".format(y, m + 1, d)
            ageTriple = computeFullAge(dob)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    )

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        // Header
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Appointment Crescent", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Quickly create and send appointments", style = MaterialTheme.typography.bodyMedium)
            }
            Icon(imageVector = Icons.Default.Timer, contentDescription = "app icon", modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Card: Form
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Appointment Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                Spacer(modifier = Modifier.height(12.dp))

                // Visit date + time (only date here)
                OutlinedTextField(
                    value = visitDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Visit Date") },
                    trailingIcon = {
                        IconButton(onClick = { visitPicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "pick visit date")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mobileNo,
                    onValueChange = { mobileNo = it },
                    label = { Text("Mobile No") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("DOB") },
                    trailingIcon = {
                        IconButton(onClick = { dobPicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = "pick dob")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Age: ${ageTriple.first}y ${ageTriple.second}m ${ageTriple.third}d", style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(8.dp))

                // Sex dropdown (simple)
                var sexExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = sexExpanded, onExpandedChange = { sexExpanded = !sexExpanded }) {
                    OutlinedTextField(
                        value = sex,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Sex") },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) }
                    )
                    ExposedDropdownMenu(expanded = sexExpanded, onDismissRequest = { sexExpanded = false }) {
                        listOf("Female", "Male", "Other").forEach { opt ->
                            DropdownMenuItem(text = { Text(opt) }, onClick = { sex = opt; sexExpanded = false })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Visit type hidden by request — keep code but collapsed (not shown)
                /* Uncomment to show:
                OutlinedTextField(
                    value = visitType,
                    onValueChange = { visitType = it },
                    label = { Text("Visit Type (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                */

                Spacer(modifier = Modifier.height(12.dp))

                // small actions row: Save defaults + Show payload toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            coroutineScope.launch {
                                DataStoreManager.savePatientName(context, patientName)
                                DataStoreManager.saveMobile(context, mobileNo)
                                DataStoreManager.saveDob(context, dob)
                                DataStoreManager.saveSex(context, sex)
                                DataStoreManager.saveVisitType(context, visitType)
                                DataStoreManager.saveVisitDate(context, visitDate)
                                Toast.makeText(context, "Defaults saved", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "save")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Defaults")
                    }

                    TextButton(onClick = { showPayload = !showPayload }) {
                        Text(if (showPayload) "Hide payload" else "Show payload")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // optionally show payload in compact card
                if (showPayload) {
                    val previewJson = remember(visitDate, patientName, mobileNo, dob, sex, visitType) {
                        JsonUtils.buildJson(
                            visitDate = visitDate,
                            drCode = "0164",
                            drName = "Prof.Dr. Jobaida Sultana , MBBS (DMC), FCPS (Gynae), MS (Gynae)",
                            patientName = patientName,
                            mobileNo = mobileNo,
                            dob = dob,
                            ageYears = ageTriple.first,
                            ageMonths = ageTriple.second,
                            ageDays = ageTriple.third,
                            sex = sex,
                            visitType = visitType
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(2.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Payload preview", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(previewJson, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        } // end form card

        Spacer(modifier = Modifier.height(16.dp))

        // Action area: big primary send + schedule + clear
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // SEND NOW primary
            Button(
                onClick = {
                    // build payload
                    val payload = JsonUtils.buildJson(
                        visitDate = visitDate,
                        drCode = "0164",
                        drName = "Prof.Dr. Jobaida Sultana , MBBS (DMC), FCPS (Gynae), MS (Gynae)",
                        patientName = patientName,
                        mobileNo = mobileNo,
                        dob = dob,
                        ageYears = ageTriple.first,
                        ageMonths = ageTriple.second,
                        ageDays = ageTriple.third,
                        sex = sex,
                        visitType = visitType
                    )
                    coroutineScope.launch {
                        sending = true
                        // explicit lambda type to avoid inference issues
                        sendPostRequest(payload) { result: String ->
                            resultText = result
                            sending = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !sending
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sending...")
                } else {
                    Icon(Icons.Default.Send, contentDescription = "send")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SEND NOW")
                }
            }

            // Schedule (less prominent) — navigates or calls onScheduleRequest
            OutlinedButton(
                    onClick = {
                        SchedulePayloadHolder.latestPayload = JsonUtils.buildJson(
                            visitDate = visitDate,
                            drCode = "0164",
                            drName = "Prof.Dr. Jobaida Sultana , MBBS (DMC), FCPS (Gynae), MS (Gynae)",
                            patientName = patientName,
                            mobileNo = mobileNo,
                            dob = dob,
                            ageYears = ageTriple.first,
                            ageMonths = ageTriple.second,
                            ageDays = ageTriple.third,
                            sex = sex,
                            visitType = visitType
                        )
                        SchedulePayloadHolder.latestVisitDate = visitDate
                        navController.navigate("schedule") {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    modifier = Modifier.width(150.dp)
                ) {
                Icon(Icons.Default.Schedule, contentDescription = "schedule")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule")
                }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Result card (was "Server Response") — shows latest result, copy & clear actions
        Text("Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(8.dp))
        Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(3.dp)) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (resultText.isBlank()) {
                    Text("No result yet", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(resultText, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = {
                        if (resultText.isNotBlank()) {
                            clipboard.setText(AnnotatedString(resultText))
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "copy")
                    }
                    TextButton(onClick = { resultText = "" }) {
                        Text("Clear")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}