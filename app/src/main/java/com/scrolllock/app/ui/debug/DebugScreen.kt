package com.scrolllock.app.ui.debug

import android.view.accessibility.AccessibilityNodeInfo
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
import com.scrolllock.app.detection.*
import com.scrolllock.app.detection.instagram.InstagramDetector
import com.scrolllock.app.detection.youtube.YouTubeDetector
import com.scrolllock.app.detection.tiktok.TikTokDetector
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val debugMode by prefs.debugMode.collectAsState(initial = false)

    var currentPackage by remember { mutableStateOf("N/A") }
    var eventType by remember { mutableStateOf("N/A") }
    var detectedSurface by remember { mutableStateOf("N/A") }
    var confidence by remember { mutableStateOf("0.0") }
    var matchedIds by remember { mutableStateOf(emptyList<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Debug Mode",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        if (!debugMode) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Debug mode is disabled", fontWeight = FontWeight.Medium)
                    Text(
                        "Enable debug mode in Settings to use this screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Live Detector Feed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow("Package", currentPackage)
                    InfoRow("Event Type", eventType)
                    InfoRow("Detected Surface", detectedSurface)
                    InfoRow("Confidence", confidence)

                    if (matchedIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Matched IDs:", fontWeight = FontWeight.Medium)
                        matchedIds.forEach { id ->
                            Text("  $id", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Detector Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    DetectorInfo("Instagram", listOf("com.instagram.android"))
                    DetectorInfo("YouTube", listOf("com.google.android.youtube"))
                    DetectorInfo("TikTok", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"))
                    DetectorInfo("Facebook", listOf("com.facebook.katana", "com.facebook.lite"))
                    DetectorInfo("Snapchat", listOf("com.snapchat.android"))
                    DetectorInfo("Reddit", listOf("com.reddit.frontpage"))
                    DetectorInfo("LinkedIn", listOf("com.linkedin.android"))
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("How to Use", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Enable debug mode in Settings", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Open a monitored app (Instagram, YouTube, etc.)", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Return here to see detection results", style = MaterialTheme.typography.bodyMedium)
                    Text("4. The detector will show matched resource IDs and confidence", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Text(value, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun DetectorInfo(name: String, packages: List<String>) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(name, fontWeight = FontWeight.Medium)
        packages.forEach { pkg ->
            Text("  $pkg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
