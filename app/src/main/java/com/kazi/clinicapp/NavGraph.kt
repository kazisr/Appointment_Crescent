// NavGraph.kt
package com.kazi.clinicapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

@Composable
fun NavGraph(
    // caller supplies real sendPostRequest; it's suspend so the wrapper below will launch it
    sendPostRequest: suspend (String, (String) -> Unit) -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    NavHost(navController = navController, startDestination = "send") {
        composable("send") {
            // Wrap the suspend function so SendScreen gets a non-suspending lambda it can call
            SendScreen(
                navController = navController,
                sendPostRequest = { jsonBody, callback ->
                    scope.launch {
                        // call the real suspend function provided by host
                        sendPostRequest(jsonBody) { result -> callback(result) }
                    }
                }
            )
        }

        composable("schedule") {
            ScheduleScreen(navController)
        }

        composable("history") {
            HistoryScreen()
        }
    }
}