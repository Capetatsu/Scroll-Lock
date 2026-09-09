package com.scrolllock.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.scrolllock.app.R
import com.scrolllock.app.browser.BrowserContentBlocker
import com.scrolllock.app.browser.DomainMatcher
import com.scrolllock.app.data.model.FeatureMask
import com.scrolllock.app.data.preferences.PreferencesManager
import com.scrolllock.app.data.room.ScrollLockDatabase
import com.scrolllock.app.detection.*
import com.scrolllock.app.detection.instagram.InstagramDetector
import com.scrolllock.app.detection.youtube.YouTubeDetector
import com.scrolllock.app.detection.tiktok.TikTokDetector
import com.scrolllock.app.detection.facebook.FacebookDetector
import com.scrolllock.app.detection.snapchat.SnapchatDetector
import com.scrolllock.app.detection.reddit.RedditDetector
import com.scrolllock.app.detection.linkedin.LinkedInDetector
import com.scrolllock.app.detection.browser.BrowserDetector
import com.scrolllock.app.intervention.OverlayController
import com.scrolllock.app.policy.*
import kotlinx.coroutines.*

class ScrollLockAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "ScrollLockA11y"
        private const val CHANNEL_ID = "scrolllock_service"
        private const val NOTIFICATION_ID = 1
        private const val EVENT_THROTTLE_MS = 100L
        private const val COOLDOWN_CHECK_INTERVAL_MS = 5000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var db: ScrollLockDatabase? = null
    private var prefs: PreferencesManager? = null
    private var overlayController: OverlayController? = null
    private var domainMatcher: DomainMatcher? = null
    private var browserBlocker: BrowserContentBlocker? = null
    private var antiScrollEngine: AntiScrollEngine? = null

    private var lastEventTime = 0L
    private var lastPackageName: String? = null
    private var cooldownActive = false
    private var cooldownExpiry = 0L
    private var cooldownSourceApp: String? = null
    private var cooldownExtraApps: Set<String> = emptySet()

    private val supportedBrowserPackages = setOf(
        "com.android.chrome",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.sec.android.app.sbrowser",
        "com.microsoft.emmx",
        "com.brave.browser",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.duckduckgo.mobile.android"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "Accessibility service connected")

        db = ScrollLockDatabase.getInstance(applicationContext)
        prefs = PreferencesManager(applicationContext)
        overlayController = OverlayController(applicationContext)
        domainMatcher = DomainMatcher()
        browserBlocker = BrowserContentBlocker(domainMatcher!!)
        antiScrollEngine = AntiScrollEngine(db!!.swipeDao(), db!!.antiScrollDao())

        registerDetectors()

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        startCooldownCheckLoop()
    }

    private fun registerDetectors() {
        DetectorEngine.registerDetector(InstagramDetector())
        DetectorEngine.registerDetector(YouTubeDetector())
        DetectorEngine.registerDetector(TikTokDetector())
        DetectorEngine.registerDetector(FacebookDetector())
        DetectorEngine.registerDetector(SnapchatDetector())
        DetectorEngine.registerDetector(RedditDetector())
        DetectorEngine.registerDetector(LinkedInDetector())
        DetectorEngine.registerDetector(BrowserDetector())
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val now = System.currentTimeMillis()
        if (now - lastEventTime < EVENT_THROTTLE_MS) return
        lastEventTime = now

        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        try {
            routeEvent(event, packageName)
        } catch (e: Exception) {
            Log.e(TAG, "Error routing event", e)
        }
    }

    private fun routeEvent(event: AccessibilityEvent, packageName: String) {
        val isProtectionEnabled = prefs?.let {
            runBlocking { it.protectionEnabled.first() }
        } ?: false

        if (!isProtectionEnabled) {
            overlayController?.hide()
            return
        }

        lastPackageName = packageName

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> handleScrollEvent(event, packageName)
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> handleWindowChanged(event, packageName)
        }

        checkBrowserBlocking(packageName)
        checkAppBlocking(packageName)
    }

    private fun handleScrollEvent(event: AccessibilityEvent, packageName: String) {
        val isAntiScrollEnabled = prefs?.let {
            runBlocking { it.antiScrollEnabled.first() }
        } ?: false

        if (!isAntiScrollEnabled) return

        val scrollDelta = event.scrollY
        val direction = if (scrollDelta > 0) 1 else if (scrollDelta < 0) -1 else 0

        scope.launch {
            antiScrollEngine?.recordSwipe(packageName, direction)
            if (antiScrollEngine?.shouldBlock(packageName) == true) {
                showBlockOverlay(packageName, "Anti-Scroll Active", "You've been scrolling too much!")
            }
        }
    }

    private fun handleWindowChanged(event: AccessibilityEvent, packageName: String) {
        DetectorEngine.clearCache()
    }

    private fun checkBrowserBlocking(packageName: String) {
        if (packageName !in supportedBrowserPackages) return

        val isBrowserEnabled = prefs?.let {
            runBlocking { it.browserBlockEnabled.first() }
        } ?: false

        if (!isBrowserEnabled) return

        val rootNode = rootInActiveWindow ?: return
        val result = browserBlocker?.checkUrl(packageName, rootNode)

        if (result?.blocked == true) {
            showBlockOverlay(packageName, "Website Blocked", "This site is not available")
            prefs?.let { scope.launch { it.incrementBlockedCount() } }
        }
    }

    private fun checkAppBlocking(packageName: String) {
        val appInfo = runBlocking {
            db?.appInfoDao()?.getByPackage(packageName)
        } ?: return

        if (!appInfo.enabled) return

        val isAntiReelsEnabled = prefs?.let {
            runBlocking { it.antiReelsEnabled.first() }
        } ?: false

        if (!isAntiReelsEnabled) return

        val rootNode = rootInActiveWindow ?: return
        val candidates = DetectorEngine.detect(packageName, rootNode)
        val topCandidate = candidates.maxByOrNull { it.confidence } ?: return

        if (topCandidate.confidence < 0.70) return

        val featureMask = when (topCandidate.surface) {
            DetectionSurface.REELS, DetectionSurface.SHORTS, DetectionSurface.TIKTOK_VIDEO ->
                FeatureMask.ANTI_REELS
            else -> FeatureMask.ANTI_REELS
        }

        val isScheduledActive = prefs?.let {
            runBlocking { it.schedulesEnabled.first() }
        } ?: false

        if (isScheduledActive) {
            val rules = runBlocking {
                db?.scheduleRuleDao()?.getEnabled() ?: emptyList()
            }
            if (!ScheduleEngine.isScheduleActive(rules, packageName, featureMask)) return
        }

        if (cooldownActive && packageName != cooldownSourceApp && packageName !in cooldownExtraApps) {
            return
        }

        showBlockOverlay(packageName, "Content Blocked", "${topCandidate.surface.name} is not available")
        prefs?.let { scope.launch { it.incrementBlockedCount() } }
    }

    private fun showBlockOverlay(packageName: String, title: String, reason: String) {
        mainHandler.post {
            overlayController?.showFullScreen(title, reason)
        }
    }

    private fun startCooldownCheckLoop() {
        scope.launch {
            while (isActive) {
                checkCooldown()
                delay(COOLDOWN_CHECK_INTERVAL_MS)
            }
        }
    }

    private fun checkCooldown() {
        prefs?.let { preferences ->
            scope.launch {
                val enabled = preferences.cooldownEnabled.first()
                if (!enabled) {
                    cooldownActive = false
                    return@launch
                }

                val sourceApp = preferences.getCooldownApps().first().firstOrNull()
                val expiry = preferences.getCooldownExpiry().first()

                cooldownSourceApp = sourceApp
                cooldownActive = System.currentTimeMillis() < expiry
                cooldownExpiry = expiry
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.service_notification_channel),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "ScrollLock protection service"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.service_notification_title))
        .setContentText(getString(R.string.service_notification_text))
        .setSmallIcon(R.drawable.ic_notification)
        .setOngoing(true)
        .setSilent(true)
        .build()

    override fun onInterrupt() {
        Log.i(TAG, "Accessibility service interrupted")
        overlayController?.cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Accessibility service destroyed")
        scope.cancel()
        mainHandler.removeCallbacksAndMessages(null)
        overlayController?.cleanup()
        DetectorEngine.clearCache()
    }

    override fun onRebind(intent: Intent?) {
        super.onRebind(intent)
        Log.i(TAG, "Accessibility service rebound")
    }
}
