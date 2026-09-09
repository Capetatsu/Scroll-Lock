package com.scrolllock.app.ui.debug

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
    var matchedText by remember { mutableStateOf(emptyList<String>()) }
    var targetBounds by remember { mutableStateOf("N/A") }
    var activeFeature by remember { mutableStateOf("N/A") }
    var cooldownState by remember { mutableStateOf("Inactive") }

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
                    InfoRow("Target Bounds", targetBounds)
                    InfoRow("Active Feature", activeFeature)
                    InfoRow("Cooldown", cooldownState)

                    if (matchedIds.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Matched IDs:", fontWeight = FontWeight.Medium)
                        matchedIds.forEach { id ->
                            Text("  $id", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    if (matchedText.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Matched Text:", fontWeight = FontWeight.Medium)
                        matchedText.forEach { text ->
                            Text("  $text", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Supported Detectors", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    DetectorInfo("Instagram", listOf("com.instagram.android"), "Reels, Stories, Explore, Feed, DM, Comments")
                    DetectorInfo("YouTube", listOf("com.google.android.youtube"), "Shorts detection")
                    DetectorInfo("TikTok", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"), "Video feed")
                    DetectorInfo("Facebook", listOf("com.facebook.katana", "com.facebook.lite"), "Reels, Stories")
                    DetectorInfo("Snapchat", listOf("com.snapchat.android"), "Spotlight, Stories")
                    DetectorInfo("Reddit", listOf("com.reddit.frontpage"), "Video feed")
                    DetectorInfo("LinkedIn", listOf("com.linkedin.android"), "Video feed")
                    DetectorInfo("Browsers", listOf("Chrome", "Firefox", "Edge", "Brave", "Opera", "Samsung", "DuckDuckGo"), "URL blocking")
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Enable debug mode in Settings", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Open a monitored app (Instagram, YouTube, TikTok, etc.)", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Navigate to Reels/Shorts/video content", style = MaterialTheme.typography.bodyMedium)
                    Text("4. Return here to see detection results", style = MaterialTheme.typography.bodyMedium)
                    Text("5. The detector will show matched resource IDs and confidence scores", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Note: Detection relies on accessibility tree fingerprints that may change with app updates.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Text(value, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
    }
}

@Composable
fun DetectorInfo(name: String, packages: List<String>, capabilities: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(name, fontWeight = FontWeight.Medium)
        packages.forEach { pkg ->
            Text("  $pkg", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("  Capabilities: $capabilities", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
    }
}
