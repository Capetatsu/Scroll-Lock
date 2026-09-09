package com.scrolllock.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
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
import com.scrolllock.app.data.model.InstagramAntiReelsSettings
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
        private const val SCROLL_THROTTLE_MS = 100L
        private const val WINDOW_THROTTLE_MS = 300L
        private const val COOLDOWN_CHECK_INTERVAL_MS = 5000L
        private const val OVERLAY_DISMISS_MS = 5000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private var db: ScrollLockDatabase? = null
    private var prefs: PreferencesManager? = null
    private var overlayController: OverlayController? = null
    private var domainMatcher: DomainMatcher? = null
    private var browserBlocker: BrowserContentBlocker? = null
    private var antiScrollEngine: AntiScrollEngine? = null

    private var lastScrollEventTime = 0L
    private var lastWindowEventTime = 0L
    private var lastPackageName: String? = null

    private var cooldownActive = false
    private var cooldownExpiry = 0L
    private var cooldownSourceApp: String? = null
    private var cooldownExtraApps: Set<String> = emptySet()

    private var overlayShownTime = 0L
    private var lastOverlayPackage: String? = null

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
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        val preferences = prefs ?: return
        if (!preferences.isProtectionEnabledBlocking()) {
            overlayController?.hide()
            return
        }

        val now = System.currentTimeMillis()

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (now - lastScrollEventTime < SCROLL_THROTTLE_MS) return
                lastScrollEventTime = now
                lastPackageName = packageName
                handleScrollEvent(event, packageName, preferences)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (now - lastWindowEventTime < WINDOW_THROTTLE_MS) return
                lastWindowEventTime = now
                lastPackageName = packageName
                DetectorEngine.forceInvalidate()
                handleWindowChanged(event, packageName, preferences)
            }
        }
    }

    private fun handleScrollEvent(
        event: AccessibilityEvent,
        packageName: String,
        preferences: PreferencesManager
    ) {
        val isAntiScrollEnabled = preferences.isAntiScrollEnabledBlocking()
        val isAntiReelsEnabled = preferences.isAntiReelsEnabledBlocking()

        if (!isAntiScrollEnabled && !isAntiReelsEnabled) return

        val scrollDeltaY = event.scrollY
        val fromScrollY = event.fromIndex
        val toScrollY = event.toIndex
        val eventTime = event.eventTime

        if (isAntiScrollEnabled) {
            scope.launch {
                val result = antiScrollEngine?.recordSwipe(
                    packageName, scrollDeltaY, eventTime, fromScrollY, toScrollY
                )
                if (result == SwipeResult.RECORDED) {
                    if (antiScrollEngine?.shouldBlock(packageName) == true) {
                        showBlockOverlay(packageName, "Anti-Scroll Active", "You've been scrolling too much!")
                        preferences.incrementBlockedCount()
                    }
                }
            }
        }

        if (isAntiReelsEnabled) {
            val rootNode = rootInActiveWindow ?: return
            evaluateDetection(packageName, rootNode, preferences, FeatureMask.ANTI_SCROLL)
        }
    }

    private fun handleWindowChanged(
        event: AccessibilityEvent,
        packageName: String,
        preferences: PreferencesManager
    ) {
        val isAntiReelsEnabled = preferences.isAntiReelsEnabledBlocking()
        val isBrowserEnabled = preferences.isBrowserBlockEnabledBlocking()

        if (isBrowserEnabled && packageName in supportedBrowserPackages) {
            val rootNode = rootInActiveWindow ?: return
            val result = browserBlocker?.checkUrl(packageName, rootNode)
            if (result?.blocked == true) {
                showBlockOverlay(packageName, "Website Blocked", "This site is not available")
                scope.launch { preferences.incrementBlockedCount() }
            }
            return
        }

        if (isAntiReelsEnabled) {
            val rootNode = rootInActiveWindow ?: return
            evaluateDetection(packageName, rootNode, preferences, FeatureMask.ANTI_REELS)
        }
    }

    private fun evaluateDetection(
        packageName: String,
        rootNode: AccessibilityNodeInfo,
        preferences: PreferencesManager,
        featureMask: Int
    ) {
        scope.launch {
            val candidates = DetectorEngine.detect(packageName, rootNode, 0)
            val topCandidate = candidates.maxByOrNull { it.confidence } ?: return@launch

            if (topCandidate.confidence < 0.70) return@launch

            val appInfo = db?.appInfoDao()?.getByPackage(packageName)
            val appEnabled = appInfo?.enabled ?: false

            if (!appEnabled) return@launch

            val isScheduledActive = preferences.isSchedulesEnabledBlocking()
            val scheduleActive = if (isScheduledActive) {
                val rules = db?.scheduleRuleDao()?.getEnabled() ?: emptyList()
                ScheduleEngine.isScheduleActive(rules, packageName, featureMask)
            } else true

            val cooldownCurrentlyActive = cooldownActive &&
                    (packageName == cooldownSourceApp || packageName in cooldownExtraApps)

            val igSettings = if (packageName == "com.instagram.android") {
                try {
                    val json = appInfo?.antiReelsSettingsJSON
                    if (json != null) {
                        com.scrolllock.app.data.room.Converters().toInstagramSettings(json)
                    } else null
                } catch (e: Exception) { null }
            } else null

            val context = PolicyEngine.buildContext(
                packageName = packageName,
                appEnabled = appEnabled,
                antiReelsEnabled = preferences.isAntiReelsEnabledBlocking(),
                antiScrollEnabled = preferences.isAntiScrollEnabledBlocking(),
                browserBlockEnabled = preferences.isBrowserBlockEnabledBlocking(),
                scheduleActive = scheduleActive,
                cooldownActive = cooldownCurrentlyActive,
                blockingSessionActive = false,
                confidence = topCandidate.confidence,
                detectedFeature = featureMask,
                surfaceName = topCandidate.surface.name,
                instagramSettings = igSettings
            )

            val decision = PolicyEngine.evaluate(context)

            when (decision.action) {
                PolicyDecision.Action.BLOCK -> {
                    val title = when {
                        topCandidate.surface == DetectionSurface.REELS -> "Reels Blocked"
                        topCandidate.surface == DetectionSurface.SHORTS -> "Shorts Blocked"
                        topCandidate.surface == DetectionSurface.TIKTOK_VIDEO -> "TikTok Blocked"
                        else -> "Content Blocked"
                    }
                    showBlockOverlay(packageName, title, decision.reason)
                    scope.launch { preferences.incrementBlockedCount() }
                }
                PolicyDecision.Action.REDIRECT -> {
                    if (decision.redirectTarget == "direct" && packageName == "com.instagram.android") {
                        performInstagramRedirect()
                    }
                }
                PolicyDecision.Action.ALLOW -> {
                    if (lastOverlayPackage == packageName) {
                        mainHandler.post { overlayController?.hide() }
                    }
                }
                PolicyDecision.Action.HIDE -> {
                    mainHandler.post { overlayController?.hide() }
                }
            }
        }
    }

    private fun performInstagramRedirect() {
        val rootNode = rootInActiveWindow ?: return
        val directTab = NodeUtils.findNodeById(rootNode, "com.instagram.android:id/direct_tab")
        if (directTab != null) {
            directTab.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
    }

    private fun showBlockOverlay(packageName: String, title: String, reason: String) {
        val now = System.currentTimeMillis()
        if (lastOverlayPackage == packageName && (now - overlayShownTime) < OVERLAY_DISMISS_MS) {
            return
        }
        overlayShownTime = now
        lastOverlayPackage = packageName
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
        val preferences = prefs ?: return
        val enabled = preferences.isCooldownEnabledBlocking()
        if (!enabled) {
            cooldownActive = false
            return
        }

        val sourceApp = preferences.getCooldownSourceAppBlocking()
        val duration = preferences.getCooldownDurationBlocking()
        val start = preferences.getCooldownStartBlocking()

        cooldownSourceApp = sourceApp.ifEmpty { null }
        cooldownActive = CooldownEngine.isCooldownActive(sourceApp, start, duration, "", emptySet())
        cooldownExpiry = if (start > 0) start + duration * 60_000L else 0L
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
