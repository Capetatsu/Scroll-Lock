package com.scrolllock.app.ui.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrolllock.app.data.preferences.PreferencesManager
import com.scrolllock.app.detection.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferencesManager(context) }
    val debugMode by prefs.debugMode.collectAsState(initial = false)

    var debugState by remember { mutableStateOf(DebugState()) }
    var pipelineEvents by remember { mutableStateOf(PipelineEvent(stage = PipelineStage.EVENT_RECEIVED)) }
    var latestEventsMap by remember { mutableStateOf(emptyMap<PipelineStage, PipelineEvent>()) }

    LaunchedEffect(debugMode) {
        while (debugMode) {
            debugState = DebugStateHolder.get()
            pipelineEvents = PipelineTracker.getInstance(context).events.value
            latestEventsMap = PipelineTracker.getInstance(context).latestEventsMap
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
            "Detection Debug",
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
            // Accessibility Service Status Card
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = if (prefs.isAccessibilityServiceEnabled() && prefs.accessibilityServiceConnected.value) MaterialTheme.colorScheme.primaryContainer
                else if (prefs.isAccessibilityServiceEnabled()) MaterialTheme.colorScheme.secondaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Accessibility Service Status", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    StatusRow("Enabled in Android Settings", prefs.isAccessibilityServiceEnabled())
                    StatusRow("Service Connected", prefs.accessibilityServiceConnected.value)
                    StatusRow("Protection Enabled (Pref)", prefs.isProtectionEnabledBlocking())
                    StatusRow("Anti-Scroll Enabled (Pref)", prefs.isAntiScrollEnabledBlocking())
                    StatusRow("Anti-Reels Enabled (Pref)", prefs.isAntiReelsEnabledBlocking())
                    StatusRow("Browser Blocking Enabled (Pref)", prefs.isBrowserBlockEnabledBlocking())

                    if (prefs.accessibilityServiceEnabled.value && !prefs.accessibilityServiceConnected.value) {
                        Text("⚠ Service enabled but not connected - may need to restart app or re-enable in Settings",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }

                    val cooldownEnabled = prefs.isCooldownEnabledBlocking()
                    if (cooldownEnabled) {
                        StatusRow("Cooldown Active", true, color = MaterialTheme.colorScheme.error)
                        InfoRow("Cooldown Source", prefs.getCooldownSourceAppBlocking())
                        val duration = prefs.getCooldownDurationBlocking()
                        val start = prefs.getCooldownStartBlocking()
                        val expiry = if (start > 0) start + duration * 60_000L else 0L
                        if (expiry > 0) {
                            val remaining = (expiry - System.currentTimeMillis()) / 1000
                            InfoRow("Expires In", "${remaining}s")
                        }
                    } else {
                        StatusRow("Cooldown Active", false)
                    }
                }
            }

            // Pipeline Stages Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Detection Pipeline", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    PipelineStage.values().forEach { stage ->
                        val event = PipelineTracker.getInstance(context).getLatestForStage(stage)
                        PipelineStageRow(stage, event)
                    }
                }
            }

            // Latest Detection Details Card
            if (pipelineEvents.stage != PipelineStage.EVENT_RECEIVED) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Latest Detection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))

                        InfoRow("Package", pipelineEvents.packageName)
                        InfoRow("Event Type", pipelineEvents.eventType)
                        InfoRow("Surface", pipelineEvents.surface.name)
                        InfoRow("Confidence", String.format("%.2f", pipelineEvents.confidence))
                        InfoRow("Match Result", pipelineEvents.matchResult.name)
                        InfoRow("Policy Action", if (pipelineEvents.policyAction.isEmpty()) "N/A" else pipelineEvents.policyAction)
                        InfoRow("Policy Reason", if (pipelineEvents.policyReason.isEmpty()) "N/A" else pipelineEvents.policyReason)

                        if (pipelineEvents.matchedIds.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Matched Resource IDs:", fontWeight = FontWeight.Medium)
                            pipelineEvents.matchedIds.forEach { id ->
                                Text("  $id", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (pipelineEvents.matchedContentDescriptions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Matched Content Descriptions:", fontWeight = FontWeight.Medium)
                            pipelineEvents.matchedContentDescriptions.forEach { desc ->
                                Text("  $desc", style = MaterialTheme.typography.bodySmall)
                            }
                        }

                        if (pipelineEvents.nodeHierarchy.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Node Hierarchy:", fontWeight = FontWeight.Medium)
                            Text(pipelineEvents.nodeHierarchy, style = MaterialTheme.typography.bodySmall)
                        }

                        if (pipelineEvents.errorMessage != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Error: ${pipelineEvents.errorMessage}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // Block/Enforcement Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Blocking & Enforcement", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow("Block Action", if (pipelineEvents.blockAction.isEmpty()) "N/A" else pipelineEvents.blockAction)
                    InfoRow("Block Reason", if (pipelineEvents.blockReason.isEmpty()) "N/A" else pipelineEvents.blockReason)

                    // Anti-Scroll State
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Anti-Scroll", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    InfoRow("Session Duration", "${debugState.antiScrollSessionDuration / 1000}s")
                    InfoRow("Swipe Count (Window)", debugState.antiScrollSwipeCount.toString())
                    InfoRow("Direction Changes", debugState.antiScrollDirectionChanges.toString())
                    InfoRow("Blocked", if (debugState.antiScrollBlocked) "YES" else "NO")
                    if (debugState.antiScrollBlockedUntil > 0) {
                        val remaining = (debugState.antiScrollBlockedUntil - System.currentTimeMillis()) / 1000
                        InfoRow("Block Expires In", "${remaining}s")
                    }
                }
            }

            // Recent Events History
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recent Pipeline Events", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(onClick = { PipelineTracker.getInstance(context).clear() }) {
                            Text("Clear")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    val recentEvents = PipelineTracker.getInstance(context).getAllLatestEvents()
                        .sortedByDescending { it.timestamp }
                        .take(20)

                    if (recentEvents.isEmpty()) {
                        Text(
                            "No events recorded.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recentEvents.forEach { event ->
                            EventDetailRow(event)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }

            // Instructions
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Instructions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. Enable debug mode in Settings", style = MaterialTheme.typography.bodyMedium)
                    Text("2. Open a monitored app (Instagram, YouTube, TikTok, etc.)", style = MaterialTheme.typography.bodyMedium)
                    Text("3. Navigate to Reels/Shorts/video content", style = MaterialTheme.typography.bodyMedium)
                    Text("4. Return here to see live detection pipeline", style = MaterialTheme.typography.bodyMedium)
                    Text("5. Each pipeline stage shows its status and data", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Use 'Clear' to reset the pipeline history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun EventDetailRow(event: PipelineEvent) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                event.stage.name,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "${event.packageName} / ${event.surface.name} / ${String.format("%.2f", event.confidence)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            if (event.errorMessage != null) "ERROR" else if (event.policyAction.isNotEmpty()) event.policyAction else "OK",
            style = MaterialTheme.typography.bodySmall,
            color = when {
                event.errorMessage != null -> MaterialTheme.colorScheme.error
                event.policyAction.isNotEmpty() -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.primary
            }
        )
    }
}

@Composable
fun PipelineStageRow(stage: PipelineStage, event: PipelineEvent?) {
    val hasEvent = event != null
    val color = when {
        !hasEvent -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        stage == PipelineStage.ERROR -> MaterialTheme.colorScheme.error
        hasEvent && event.errorMessage != null -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .padding(horizontal = 8.dp)
            .background(color.copy(alpha = 0.1f)),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            stage.name,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (hasEvent) "●" else "○",
                color = color,
                fontSize = 12.sp
            )
            if (hasEvent) {
                Text(
                    "${event.packageName} / ${event.surface.name} / ${String.format("%.2f", event.confidence)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: Boolean, color: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (value) "YES" else "NO",
                color = if (value) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold
            )
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