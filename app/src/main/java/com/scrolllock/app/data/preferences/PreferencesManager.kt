package com.scrolllock.app.data.preferences

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scrolllock_settings")

class PreferencesManager(private val context: Context) {

    companion object {
        val PROTECTION_ENABLED = booleanPreferencesKey("protection_enabled")
        val ANTI_SCROLL_ENABLED = booleanPreferencesKey("anti_scroll_enabled")
        val ANTI_REELS_ENABLED = booleanPreferencesKey("anti_reels_enabled")
        val BROWSER_BLOCK_ENABLED = booleanPreferencesKey("browser_block_enabled")
        val SCHEDULES_ENABLED = booleanPreferencesKey("schedules_enabled")
        val COOLDOWN_ENABLED = booleanPreferencesKey("cooldown_enabled")
        val COOLDOWN_SOURCE_APP = stringPreferencesKey("cooldown_source_app")
        val COOLDOWN_EXTRA_APPS = stringPreferencesKey("cooldown_extra_apps")
        val COOLDOWN_DURATION_MINUTES = intPreferencesKey("cooldown_duration_minutes")
        val COOLDOWN_START = longPreferencesKey("cooldown_start")
        val DEVICE_ADMIN_ENABLED = booleanPreferencesKey("device_admin_enabled")
        val LAST_BOOT_TIME = longPreferencesKey("last_boot_time")
        val TODAY_BLOCKED_COUNT = intPreferencesKey("today_blocked_count")
        val TODAY_BLOCKED_DATE = stringPreferencesKey("today_blocked_date")
        val TODAY_PROTECTED_SECONDS = longPreferencesKey("today_protected_seconds")
        val CUSTOM_DOMAINS = stringPreferencesKey("custom_domains")
        val DEBUG_MODE = booleanPreferencesKey("debug_mode")
    }

    val protectionEnabled: Flow<Boolean> = context.dataStore.data.map { it[PROTECTION_ENABLED] ?: false }
    val antiScrollEnabled: Flow<Boolean> = context.dataStore.data.map { it[ANTI_SCROLL_ENABLED] ?: false }
    val antiReelsEnabled: Flow<Boolean> = context.dataStore.data.map { it[ANTI_REELS_ENABLED] ?: false }
    val browserBlockEnabled: Flow<Boolean> = context.dataStore.data.map { it[BROWSER_BLOCK_ENABLED] ?: false }
    val schedulesEnabled: Flow<Boolean> = context.dataStore.data.map { it[SCHEDULES_ENABLED] ?: false }
    val cooldownEnabled: Flow<Boolean> = context.dataStore.data.map { it[COOLDOWN_ENABLED] ?: false }
    val debugMode: Flow<Boolean> = context.dataStore.data.map { it[DEBUG_MODE] ?: false }

    fun isProtectionEnabledBlocking(): Boolean = runBlocking { protectionEnabled.first() }
    fun isAntiScrollEnabledBlocking(): Boolean = runBlocking { antiScrollEnabled.first() }
    fun isAntiReelsEnabledBlocking(): Boolean = runBlocking { antiReelsEnabled.first() }
    fun isBrowserBlockEnabledBlocking(): Boolean = runBlocking { browserBlockEnabled.first() }
    fun isSchedulesEnabledBlocking(): Boolean = runBlocking { schedulesEnabled.first() }
    fun isCooldownEnabledBlocking(): Boolean = runBlocking { cooldownEnabled.first() }

    suspend fun setProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PROTECTION_ENABLED] = enabled }
    }

    suspend fun setAntiScrollEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ANTI_SCROLL_ENABLED] = enabled }
    }

    suspend fun setAntiReelsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[ANTI_REELS_ENABLED] = enabled }
    }

    suspend fun setBrowserBlockEnabled(enabled: Boolean) {
        context.dataStore.edit { it[BROWSER_BLOCK_ENABLED] = enabled }
    }

    suspend fun setSchedulesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SCHEDULES_ENABLED] = enabled }
    }

    suspend fun setCooldownEnabled(enabled: Boolean) {
        context.dataStore.edit { it[COOLDOWN_ENABLED] = enabled }
    }

    suspend fun setCooldownSourceApp(packageName: String) {
        context.dataStore.edit { it[COOLDOWN_SOURCE_APP] = packageName }
    }

    suspend fun setCooldownExtraApps(apps: Set<String>) {
        context.dataStore.edit { it[COOLDOWN_EXTRA_APPS] = apps.joinToString(",") }
    }

    suspend fun setCooldownDuration(minutes: Int) {
        context.dataStore.edit { it[COOLDOWN_DURATION_MINUTES] = minutes }
    }

    suspend fun setCooldownStart(timestamp: Long) {
        context.dataStore.edit { it[COOLDOWN_START] = timestamp }
    }

    suspend fun setDeviceAdminEnabled(enabled: Boolean) {
        context.dataStore.edit { it[DEVICE_ADMIN_ENABLED] = enabled }
    }

    suspend fun incrementBlockedCount() {
        context.dataStore.edit { prefs ->
            val today = java.time.LocalDate.now().toString()
            if (prefs[TODAY_BLOCKED_DATE] != today) {
                prefs[TODAY_BLOCKED_COUNT] = 1
                prefs[TODAY_BLOCKED_DATE] = today
            } else {
                prefs[TODAY_BLOCKED_COUNT] = (prefs[TODAY_BLOCKED_COUNT] ?: 0) + 1
            }
        }
    }

    suspend fun addProtectedTime(seconds: Long) {
        context.dataStore.edit { prefs ->
            val today = java.time.LocalDate.now().toString()
            if (prefs[TODAY_BLOCKED_DATE] != today) {
                prefs[TODAY_PROTECTED_SECONDS] = seconds
                prefs[TODAY_BLOCKED_DATE] = today
            } else {
                prefs[TODAY_PROTECTED_SECONDS] = (prefs[TODAY_PROTECTED_SECONDS] ?: 0) + seconds
            }
        }
    }

    fun getBlockedCount(): Flow<Int> = context.dataStore.data.map { prefs ->
        val today = java.time.LocalDate.now().toString()
        if (prefs[TODAY_BLOCKED_DATE] == today) prefs[TODAY_BLOCKED_COUNT] ?: 0 else 0
    }

    fun getProtectedSeconds(): Flow<Long> = context.dataStore.data.map { prefs ->
        val today = java.time.LocalDate.now().toString()
        if (prefs[TODAY_BLOCKED_DATE] == today) prefs[TODAY_PROTECTED_SECONDS] ?: 0 else 0
    }

    fun getCooldownApps(): Flow<Set<String>> = context.dataStore.data.map { prefs ->
        val extra = prefs[COOLDOWN_EXTRA_APPS] ?: ""
        val source = prefs[COOLDOWN_SOURCE_APP] ?: ""
        val apps = mutableSetOf<String>()
        if (source.isNotEmpty()) apps.add(source)
        if (extra.isNotEmpty()) apps.addAll(extra.split(","))
        apps
    }

    fun getCooldownExpiry(): Flow<Long> = context.dataStore.data.map { prefs ->
        val start = prefs[COOLDOWN_START] ?: 0L
        val duration = (prefs[COOLDOWN_DURATION_MINUTES] ?: 30) * 60_000L
        start + duration
    }

    fun getCooldownSourceAppBlocking(): String = runBlocking {
        context.dataStore.data.first()[COOLDOWN_SOURCE_APP] ?: ""
    }

    fun getCooldownDurationBlocking(): Int = runBlocking {
        context.dataStore.data.first()[COOLDOWN_DURATION_MINUTES] ?: 30
    }

    fun getCooldownStartBlocking(): Long = runBlocking {
        context.dataStore.data.first()[COOLDOWN_START] ?: 0L
    }

    fun getCooldownExtraAppsBlocking(): Set<String> = runBlocking {
        val extra = context.dataStore.data.first()[COOLDOWN_EXTRA_APPS] ?: ""
        extra.split(",").filter { it.isNotEmpty() }.toSet()
    }

    fun setDebugMode(enabled: Boolean) {
        runBlocking {
            context.dataStore.edit { it[DEBUG_MODE] = enabled }
        }
    }

    fun isDebugModeBlocking(): Boolean = runBlocking { debugMode.first() }

    fun isAccessibilityServiceEnabled(): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val targetService = "com.scrolllock.app/com.scrolllock.app.accessibility.ScrollLockAccessibilityService"
        return enabledServices.split(":").any { it == targetService }
    }

    val accessibilityServiceEnabled: MutableStateFlow<Boolean> = MutableStateFlow(isAccessibilityServiceEnabled())

    fun refreshAccessibilityServiceState() {
        accessibilityServiceEnabled.value = isAccessibilityServiceEnabled()
    }
}
