package com.scrolllock.app.ui.apps

import android.content.pm.PackageManager
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.scrolllock.app.data.model.AppInfo
import com.scrolllock.app.data.preferences.PreferencesManager
import com.scrolllock.app.data.room.ScrollLockDatabase
import kotlinx.coroutines.launch

data class AppEntry(
    val packageName: String,
    val name: String,
    val enabled: Boolean = false,
    val antiReels: Boolean = false,
    val antiScroll: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { ScrollLockDatabase.getInstance(context) }
    val apps by db.appInfoDao().getAll().collectAsState(initial = emptyList())

    val socialPackages = listOf(
        "com.instagram.android" to "Instagram",
        "com.google.android.youtube" to "YouTube",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok (Global)",
        "com.facebook.katana" to "Facebook",
        "com.facebook.lite" to "Facebook Lite",
        "com.twitter.android" to "X (Twitter)",
        "com.snapchat.android" to "Snapchat",
        "com.reddit.frontpage" to "Reddit",
        "com.linkedin.android" to "LinkedIn"
    )

    val browserPackages = listOf(
        "com.android.chrome" to "Chrome",
        "org.mozilla.firefox" to "Firefox",
        "com.sec.android.app.sbrowser" to "Samsung Browser",
        "com.microsoft.emmx" to "Edge",
        "com.brave.browser" to "Brave",
        "com.opera.browser" to "Opera",
        "com.duckduckgo.mobile.android" to "DuckDuckGo"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Apps",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            item {
                Text(
                    "Social Media",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(socialPackages) { (pkg, name) ->
                val appInfo = apps.find { it.packageName == pkg }
                AppItem(
                    name = name,
                    packageName = pkg,
                    enabled = appInfo?.enabled ?: false,
                    antiReels = appInfo?.antiReels?.let { it > 0 } ?: false,
                    antiScroll = appInfo?.antiScroll ?: false,
                    onToggleEnabled = { enabled ->
                        scope.launch {
                            val current = db.appInfoDao().getByPackage(pkg)
                            db.appInfoDao().upsert(
                                (current ?: AppInfo(packageName = pkg)).copy(enabled = enabled)
                            )
                        }
                    },
                    onToggleAntiReels = { antiReels ->
                        scope.launch {
                            val current = db.appInfoDao().getByPackage(pkg)
                            db.appInfoDao().upsert(
                                (current ?: AppInfo(packageName = pkg)).copy(
                                    antiReels = if (antiReels) 1 else 0
                                )
                            )
                        }
                    }
                )
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "Browsers",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(browserPackages) { (pkg, name) ->
                val appInfo = apps.find { it.packageName == pkg }
                AppItem(
                    name = name,
                    packageName = pkg,
                    enabled = appInfo?.enabled ?: false,
                    onToggleEnabled = { enabled ->
                        scope.launch {
                            val current = db.appInfoDao().getByPackage(pkg)
                            db.appInfoDao().upsert(
                                (current ?: AppInfo(packageName = pkg)).copy(enabled = enabled)
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun AppItem(
    name: String,
    packageName: String,
    enabled: Boolean,
    antiReels: Boolean = false,
    antiScroll: Boolean = false,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleAntiReels: ((Boolean) -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Medium)
                Text(
                    packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onToggleAntiReels != null) {
                    FilterChip(
                        selected = antiReels,
                        onClick = { onToggleAntiReels(!antiReels) },
                        label = { Text("Reels") }
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled
                )
            }
        }
    }
}
