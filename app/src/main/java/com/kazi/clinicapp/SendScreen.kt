package com.kazi.clinicapp

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendScreen(
    navController: NavController,
    sendPostRequest: suspend (String, (String) -> Unit) -> Unit = { _json, cb ->
        // default no-op implementation so callers that don't provide the function still compile.
        // Runs on caller coroutine; just return a short message via callback.
        cb("sendPostRequest not provided")
    }
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ---------------- STATE ----------------
    var visitDate by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var patientName by rememberSaveable { mutableStateOf("Rownak Jahan Sharna") }
    var mobileNo by rememberSaveable { mutableStateOf("01959521675") }
    var dob by rememberSaveable { mutableStateOf("1999-01-28") }

    val age = computeFullAge(dob)
    var ageYears by rememberSaveable { mutableStateOf(age.first) }
    var ageMonths by rememberSaveable { mutableStateOf(age.second) }
    var ageDays by rememberSaveable { mutableStateOf(age.third) }

    var sex by rememberSaveable { mutableStateOf("Female") }
    var visitType by rememberSaveable { mutableStateOf("") }
    var logOutput by rememberSaveable { mutableStateOf("") }

    // ---------------- DATE PICKERS ----------------
    val cal = Calendar.getInstance()

    val visitDatePicker = DatePickerDialog(
        context,
        { _, y, m, d -> visitDate = "%04d-%02d-%02d".format(y, m + 1, d) },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    )

    val dobPicker = DatePickerDialog(
        context,
        { _, y, m, d ->
            dob = "%04d-%02d-%02d".format(y, m + 1, d)
            val age2 = computeFullAge(dob)
            ageYears = age2.first
            ageMonths = age2.second
            ageDays = age2.third
        },
        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
    )

    // ---------------- PAYLOAD JSON ----------------
    val previewJson by remember(
        visitDate, patientName, mobileNo, dob,
        ageYears, ageMonths, ageDays, sex, visitType
    ) {
        mutableStateOf(
            JsonUtils.buildJson(
                visitDate = visitDate,
                drCode = "0164",
                drName = "Prof.Dr. Jobaida Sultana , MBBS (DMC), FCPS (Gynae), MS (Gynae)",
                patientName = patientName,
                mobileNo = mobileNo,
                dob = dob,
                ageYears = ageYears,
                ageMonths = ageMonths,
                ageDays = ageDays,
                sex = sex,
                visitType = visitType
            )
        )
    }

    // ---------------- UI ----------------

    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(16.dp)
    ) {
        Text("Appointment Crescent", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(16.dp))

        // ---------- CARD ----------
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                // VISIT DATE
                OutlinedTextField(
                    value = visitDate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Visit Date") },
                    trailingIcon = {
                        IconButton(onClick = { visitDatePicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // PATIENT NAME
                OutlinedTextField(
                    value = patientName,
                    onValueChange = { patientName = it },
                    label = { Text("Patient Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // MOBILE
                OutlinedTextField(
                    value = mobileNo,
                    onValueChange = { mobileNo = it },
                    label = { Text("Mobile No") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                // DOB
                OutlinedTextField(
                    value = dob,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("DOB") },
                    trailingIcon = {
                        IconButton(onClick = { dobPicker.show() }) {
                            Icon(Icons.Default.DateRange, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text("Age: ${ageYears}y ${ageMonths}m ${ageDays}d")

                Spacer(Modifier.height(12.dp))

                // SEX DROPDOWN
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = sex,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        label = { Text("Sex") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        listOf("Female", "Male", "Other").forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    sex = option
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Visit type
                OutlinedTextField(
                    value = visitType,
                    onValueChange = { visitType = it },
                    label = { Text("Visit Type (Optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

                // PAYLOAD PREVIEW
                Text("Payload Preview", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 220.dp),
                    elevation = CardDefaults.cardElevation(3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(previewJson)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---------- BUTTONS ----------
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    // SAVE BUTTON
                    Button(
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
                        Text("Save")
                    }

                    // SEND NOW BUTTON
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val result = NetworkUtils.sendPostRequest(context, previewJson)
                                logOutput = result
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Send Now")
                    }

                    // SCHEDULE THIS BUTTON
                    Button(
                        onClick = {
                            SchedulePayloadHolder.latestPayload = previewJson
                            SchedulePayloadHolder.latestVisitDate = visitDate

                            navController.navigate("schedule") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Schedule")
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Text("Server Response", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Text(
                text = if (logOutput.isBlank()) "No response yet" else logOutput,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
