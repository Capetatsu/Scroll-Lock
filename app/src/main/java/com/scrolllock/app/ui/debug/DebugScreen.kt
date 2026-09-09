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
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val debugMode by prefs.debugMode.collectAsState(initial = false)

    var latestEvent by remember { mutableStateOf<DetectionDebugInfo?>(null) }
    var eventCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(debugMode) {
        while (debugMode) {
            val event = DebugEventBus.getLatestEvent()
            if (event != latestEvent) {
                latestEvent = event
                eventCount = DebugEventBus.getRecentEvents().size
            }
            delay(200)
        }
    }

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

                    if (latestEvent != null) {
                        val event = latestEvent!!
                        InfoRow("Package", event.packageName)
                        InfoRow("Surface", event.surface.name)
                        InfoRow("Confidence", String.format("%.2f", event.confidence))
                        InfoRow("Result", event.matchResult.name)
                        InfoRow("Events Cached", eventCount.toString())

                        if (event.reasonCodes.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Reason Codes:", fontWeight = FontWeight.Medium)
                            event.reasonCodes.forEach { code ->
                                Text("  $code", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (event.signals.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Detection Signals:", fontWeight = FontWeight.Medium)
                            event.signals.forEach { signal ->
                                SignalRow(signal)
                            }
                        }
                    } else {
                        Text(
                            "No detection events yet. Navigate to a monitored app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Recent Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    val recentEvents = DebugEventBus.getRecentEvents().take(10)
                    if (recentEvents.isEmpty()) {
                        Text(
                            "No events recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recentEvents.forEach { event ->
                            EventSummaryRow(event)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
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
fun SignalRow(signal: DetectionSignal) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            signal.description,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "conf=${String.format("%.2f", signal.confidence)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            if (signal.isSelected) {
                Text(
                    "selected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            if (signal.isVisible) {
                Text(
                    "visible",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
fun EventSummaryRow(event: DetectionDebugInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            event.surface.name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            String.format("%.2f", event.confidence),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(0.5f)
        )
        Text(
            event.matchResult.name,
            style = MaterialTheme.typography.bodySmall,
            color = when (event.matchResult) {
                DetectionResult.MATCH -> MaterialTheme.colorScheme.tertiary
                DetectionResult.NO_MATCH -> MaterialTheme.colorScheme.error
                DetectionResult.UNKNOWN -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.weight(0.7f)
        )
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
