package com.scrolllock.app.ui.antireels

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
fun AntiReelsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { PreferencesManager(context) }
    val antiReelsEnabled by prefs.antiReelsEnabled.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Anti-Reels",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Enable Anti-Reels", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = antiReelsEnabled,
                        onCheckedChange = { scope.launch { prefs.setAntiReelsEnabled(it) } }
                    )
                }
                Text(
                    "Detects and blocks short-form video content (Reels, Shorts, TikTok) across supported apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            "Supported Platforms",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        val platforms = listOf(
            "Instagram Reels" to "Blocks Reels, Explore, and optionally feed/stories",
            "YouTube Shorts" to "Blocks Shorts player and shelf",
            "TikTok" to "Blocks main video feed",
            "Facebook Reels" to "Blocks video/reels tab (experimental)",
            "Snapchat Spotlight" to "Blocks Spotlight feed (experimental)",
            "Reddit Video" to "Blocks video player (experimental)"
        )

        platforms.forEach { (name, desc) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(name, fontWeight = FontWeight.Medium)
                    Text(
                        desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Text(
            "Instagram-Specific Controls",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var hideReels by remember { mutableStateOf(true) }
                var blockExplore by remember { mutableStateOf(true) }
                var blockFeed by remember { mutableStateOf(false) }
                var blockStories by remember { mutableStateOf(false) }
                var allowReelsInDM by remember { mutableStateOf(true) }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Hide Reels on Home"); Switch(checked = hideReels, onCheckedChange = { hideReels = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Block Explore"); Switch(checked = blockExplore, onCheckedChange = { blockExplore = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Block Main Feed"); Switch(checked = blockFeed, onCheckedChange = { blockFeed = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Block Stories"); Switch(checked = blockStories, onCheckedChange = { blockStories = it })
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Allow Reels in DMs"); Switch(checked = allowReelsInDM, onCheckedChange = { allowReelsInDM = it })
                }
            }
        }
    }
}
