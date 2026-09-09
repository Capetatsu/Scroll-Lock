package com.scrolllock.app.ui.cooldown

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrolllock.app.data.preferences.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CooldownScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val cooldownEnabled by prefs.cooldownEnabled.collectAsState(initial = false)

    var duration by remember { mutableStateOf("30") }
    var sourceApp by remember { mutableStateOf("com.instagram.android") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Cross-App Cooldown",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Cooldown", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = cooldownEnabled,
                        onCheckedChange = { scope.launch { prefs.setCooldownEnabled(it) } }
                    )
                }
                Text(
                    "When a source app is blocked, other configured apps also trigger intervention during the cooldown period.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            "Configuration",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = duration,
                    onValueChange = { duration = it },
                    label = { Text("Cooldown Duration (minutes)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Source App", fontWeight = FontWeight.Medium)

                val socialApps = listOf(
                    "com.instagram.android" to "Instagram",
                    "com.google.android.youtube" to "YouTube",
                    "com.zhiliaoapp.musically" to "TikTok",
                    "com.facebook.katana" to "Facebook",
                    "com.twitter.android" to "X",
                    "com.snapchat.android" to "Snapchat",
                    "com.reddit.frontpage" to "Reddit"
                )

                socialApps.forEach { (pkg, name) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name)
                        RadioButton(
                            selected = sourceApp == pkg,
                            onClick = { sourceApp = pkg }
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            prefs.setCooldownDuration(duration.toIntOrNull() ?: 30)
                            prefs.setCooldownSourceApp(sourceApp)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Cooldown Settings")
                }
            }
        }
    }
}
