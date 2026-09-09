package com.scrolllock.app.ui.antiscroll

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrolllock.app.data.model.CustomAntiScrollMode
import com.scrolllock.app.data.preferences.PreferencesManager
import com.scrolllock.app.data.room.ScrollLockDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AntiScrollScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val db = remember { ScrollLockDatabase.getInstance(context) }
    val antiScrollEnabled by prefs.antiScrollEnabled.collectAsState(initial = false)
    val modes by db.antiScrollDao().getAll().collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Anti-Scroll",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Anti-Scroll", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = antiScrollEnabled,
                        onCheckedChange = { scope.launch { prefs.setAntiScrollEnabled(it) } }
                    )
                }
                Text(
                    "Detects excessive scrolling patterns and blocks the app when thresholds are exceeded.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            "Default Thresholds",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        var frequency by remember { mutableStateOf("1") }
        var window by remember { mutableStateOf("20") }
        var duration by remember { mutableStateOf("180") }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = frequency,
                    onValueChange = { frequency = it },
                    label = { Text("Swipe Frequency (swipes per window)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = window,
                    onValueChange = { window = it },
                    label = { Text("Check Window (seconds)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Duration Threshold (seconds)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        scope.launch {
                            val mode = CustomAntiScrollMode(
                                packageName = "__default__",
                                swipesFrequency = frequency.toIntOrNull() ?: 1,
                                checkWindow = window.toIntOrNull() ?: 20,
                                durationThreshold = duration.toIntOrNull() ?: 180
                            )
                            db.antiScrollDao().upsert(mode)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Defaults")
                }
            }
        }

        if (modes.isNotEmpty()) {
            Text(
                "Per-App Settings",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            modes.filter { it.packageName != "__default__" }.forEach { mode ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(mode.packageName, fontWeight = FontWeight.Medium)
                        Text("Frequency: ${mode.swipesFrequency}, Window: ${mode.checkWindow}s, Duration: ${mode.durationThreshold}s")
                    }
                }
            }
        }
    }
}
