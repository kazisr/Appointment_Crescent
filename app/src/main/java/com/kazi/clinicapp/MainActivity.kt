package com.kazi.clinicapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

/**
 * MainActivity: app shell + navigation only.
 *
 * Expects the following files/composables to exist in the same package:
 *  - SendScreen(navController: NavHostController, sendPostRequest: suspend (String, (String)->Unit) -> Unit)
 *  - ScheduleScreen(navController: NavHostController)
 *  - HistoryScreen()
 *  - NetworkUtils.sendPostRequest(context, json)  (suspend)
 *  - NotificationHelper.createChannel(context)
 *  - DataStoreManager, JsonUtils, SchedulePayloadHolder, AppointmentWorker, etc.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Ask notification permission on Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { granted ->
                if (!granted) {
                    Toast.makeText(this, "Notifications permission denied — scheduled alerts may not show", Toast.LENGTH_LONG).show()
                }
            }
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Create notification channel (safe to call repeatedly)
        NotificationHelper.createChannel(this)

        setContent {
            MaterialTheme {
                AppShell()
            }
        }
    }

    // --- App shell with bottom navigation ---
    @Composable
    fun AppShell() {
        val navController = rememberNavController()
        Scaffold(
            bottomBar = {
                BottomNavBar(navController = navController)
            }
        ) { innerPadding ->
            NavHost(navController = navController, startDestination = "send", modifier = Modifier.padding(innerPadding)) {
                // SEND screen: pass a wrapper that calls NetworkUtils and returns the result via callback
                composable("send") {
                    val scope = rememberCoroutineScope()
                    SendScreen(
                        navController = navController,
                        // wrapper expected: suspend (jsonBody, callback) -> Unit
                        sendPostRequest = { jsonBody: String, callback: (String) -> Unit ->
                            // use lifecycleScope to call the suspend NetworkUtils function
                            lifecycleScope.launch {
                                try {
                                    val result = NetworkUtils.sendPostRequest(applicationContext, jsonBody)
                                    callback(result)
                                } catch (e: Exception) {
                                    callback("Error: ${e.message ?: "unknown"}")
                                }
                            }
                        }
                    )
                }

                // SCHEDULE screen: separate file
                composable("schedule") {
                    ScheduleScreen(navController)
                }

                // HISTORY screen: separate file
                composable("history") {
                    HistoryScreen()
                }
            }
        }
    }

    @Composable
    fun BottomNavBar(navController: NavHostController) {
        NavigationBar {
            val backStackEntry = navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry.value?.destination?.route

            NavigationBarItem(
                selected = currentRoute == "send",
                onClick = { navController.navigate("send") { launchSingleTop = true; restoreState = true } },
                icon = { androidx.compose.material3.Icon(Icons.Default.MedicalInformation, contentDescription = "Send") },
                label = { androidx.compose.material3.Text("Appointment Info") }
            )

//            NavigationBarItem(
//                selected = currentRoute == "schedule",
//                onClick = { navController.navigate("schedule") { launchSingleTop = true; restoreState = true } },
//                icon = { androidx.compose.material3.Icon(Icons.Default.DateRange, contentDescription = "Schedule") },
//                label = { androidx.compose.material3.Text("Schedule") }
//            )

            NavigationBarItem(
                selected = currentRoute == "history",
                onClick = { navController.navigate("history") { launchSingleTop = true; restoreState = true } },
                icon = { androidx.compose.material3.Icon(Icons.Default.History, contentDescription = "History") },
                label = { androidx.compose.material3.Text("History") }
            )
        }
    }

}