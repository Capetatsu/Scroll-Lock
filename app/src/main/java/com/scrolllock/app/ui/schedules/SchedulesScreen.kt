package com.scrolllock.app.ui.schedules

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrolllock.app.data.model.ScheduleRule
import com.scrolllock.app.data.preferences.PreferencesManager
import com.scrolllock.app.data.room.ScrollLockDatabase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchedulesScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val db = remember { ScrollLockDatabase.getInstance(context) }
    val schedulesEnabled by prefs.schedulesEnabled.collectAsState(initial = false)
    val rules by db.scheduleRuleDao().getAll().collectAsState(initial = emptyList())

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Schedules",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Rule")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Enable Schedules", fontWeight = FontWeight.Medium)
                Switch(
                    checked = schedulesEnabled,
                    onCheckedChange = { scope.launch { prefs.setSchedulesEnabled(it) } }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(rules) { rule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(rule.name ?: "Unnamed Rule", fontWeight = FontWeight.Medium)
                            Text(
                                "Days: ${formatDays(rule.daysBitmask)}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                if (rule.appliesToAllApps) "All apps" else "Selected apps",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Row {
                            Switch(
                                checked = rule.enabled,
                                onCheckedChange = { enabled ->
                                    scope.launch {
                                        db.scheduleRuleDao().upsert(rule.copy(enabled = enabled))
                                    }
                                }
                            )
                            IconButton(onClick = {
                                scope.launch { db.scheduleRuleDao().delete(rule) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddScheduleDialog(
            onDismiss = { showAddDialog = false },
            onSave = { rule ->
                scope.launch {
                    db.scheduleRuleDao().upsert(rule)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun AddScheduleDialog(onDismiss: () -> Unit, onSave: (ScheduleRule) -> Unit) {
    var name by remember { mutableStateOf("") }
    var appliesToAll by remember { mutableStateOf(true) }
    var daysBitmask by remember { mutableIntStateOf(0x7F) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Schedule Rule") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Rule Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("All Apps"); Switch(checked = appliesToAll, onCheckedChange = { appliesToAll = it })
                }
                Text("Days: All selected", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(ScheduleRule(
                    name = name.ifBlank { "Unnamed" },
                    appliesToAllApps = appliesToAll,
                    daysBitmask = daysBitmask
                ))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

fun formatDays(bitmask: Int): String {
    val days = mutableListOf<String>()
    if (bitmask and 1 != 0) days.add("Mon")
    if (bitmask and 2 != 0) days.add("Tue")
    if (bitmask and 4 != 0) days.add("Wed")
    if (bitmask and 8 != 0) days.add("Thu")
    if (bitmask and 16 != 0) days.add("Fri")
    if (bitmask and 32 != 0) days.add("Sat")
    if (bitmask and 64 != 0) days.add("Sun")
    return if (days.size == 7) "Every day" else days.joinToString(", ")
}
