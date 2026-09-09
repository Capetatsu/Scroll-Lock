package com.scrolllock.app.ui.browser

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrolllock.app.browser.DomainMatcher
import com.scrolllock.app.data.preferences.PreferencesManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val browserEnabled by prefs.browserBlockEnabled.collectAsState(initial = false)
    val domainMatcher = remember { DomainMatcher() }

    var newDomain by remember { mutableStateOf("") }
    var customDomains by remember { mutableStateOf(domainMatcher.getCustomDomains()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Browser Blocking",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Browser Blocking", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = browserEnabled,
                        onCheckedChange = { scope.launch { prefs.setBrowserBlockEnabled(it) } }
                    )
                }
                Text(
                    "Blocks access to adult and custom-blocked websites in supported browsers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            "Supported Browsers",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val browsers = listOf(
            "Chrome", "Firefox", "Samsung Browser",
            "Edge", "Brave", "Opera", "DuckDuckGo"
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                browsers.forEach { browser ->
                    Text("  $browser", modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        Text(
            "Custom Blocked Domains",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newDomain,
                        onValueChange = { newDomain = it },
                        label = { Text("Add domain") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (newDomain.isNotBlank()) {
                                domainMatcher.addCustomDomain(newDomain)
                                customDomains = domainMatcher.getCustomDomains()
                                newDomain = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                customDomains.forEach { domain ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(domain)
                        IconButton(onClick = {
                            domainMatcher.removeCustomDomain(domain)
                            customDomains = domainMatcher.getCustomDomains()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Adult Domain Protection", fontWeight = FontWeight.Medium)
                Text(
                    "Built-in list of adult domains is always active when browser blocking is enabled.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
