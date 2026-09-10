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
import com.scrolllock.app.detection.DebugEventBus
import com.scrolllock.app.detection.DebugStateHolder
import com.scrolllock.app.detection.PipelineTracker
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
        private const val SCROLL_THROTTLE_MS = 60L
        private const val WINDOW_THROTTLE_MS = 200L
        private const val CONTENT_CHANGE_THROTTLE_MS = 100L
        private const val CLICK_THROTTLE_MS = 150L
        private const val OVERLAY_DISMISS_MS = 3000L
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

    private var lastScrollEventTime = 0L
    private var lastWindowEventTime = 0L
    private var lastContentChangeEventTime = 0L
    private var lastClickEventTime = 0L
    private var lastPackageName: String? = null

    private var cachedProtectionEnabled = false
    private var cachedAntiScrollEnabled = false
    private var cachedAntiReelsEnabled = false
    private var cachedBrowserBlockEnabled = false
    private var cachedCooldownEnabled = false
    private var cachedSchedulesEnabled = false

    private var cooldownActive = false
    private var cooldownExpiry = 0L
    private var cooldownSourceApp: String? = null
    private var cooldownExtraApps: Set<String> = emptySet()
    private var cooldownStart = 0L
    private var cooldownDuration = 30

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

        val prefs = PreferencesManager(applicationContext)
        val a11yEnabled = prefs.isAccessibilityServiceEnabled()
        Log.i(TAG, "Accessibility service enabled in system: $a11yEnabled")

        db = ScrollLockDatabase.getInstance(applicationContext)
        this.prefs = prefs
        overlayController = OverlayController(applicationContext)
        domainMatcher = DomainMatcher()
        browserBlocker = BrowserContentBlocker(domainMatcher!!)
        antiScrollEngine = AntiScrollEngine(db!!.swipeDao(), db!!.antiScrollDao())

        registerDetectors()

        // Update service connected state
        prefs.accessibilityServiceConnected.value = true
        prefs.accessibilityServiceEnabled.value = true

        // Initialize pipeline tracker
        PipelineTracker.getInstance(applicationContext)

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        // Start live preference flow collection (replaces 5-second polling)
        startPreferenceFlowCollection()

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

    private fun startPreferenceFlowCollection() {
        val p = prefs ?: return

        scope.launch {
            p.protectionEnabled.collect { cachedProtectionEnabled = it }
        }
        scope.launch {
            p.antiScrollEnabled.collect { cachedAntiScrollEnabled = it }
        }
        scope.launch {
            p.antiReelsEnabled.collect { cachedAntiReelsEnabled = it }
        }
        scope.launch {
            p.browserBlockEnabled.collect { cachedBrowserBlockEnabled = it }
        }
        scope.launch {
            p.schedulesEnabled.collect { cachedSchedulesEnabled = it }
        }
        scope.launch {
            p.cooldownEnabled.collect { cachedCooldownEnabled = it }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return

        PipelineTracker.getInstance(applicationContext).logEvent(
            stage = PipelineStage.EVENT_RECEIVED,
            packageName = packageName,
            eventType = event.eventType.toString()
        )

        if (!cachedProtectionEnabled) {
            mainHandler.post { overlayController?.hide() }
            return
        }

        val now = System.currentTimeMillis()

        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_SCROLLED -> {
                if (now - lastScrollEventTime < SCROLL_THROTTLE_MS) return
                lastScrollEventTime = now
                lastPackageName = packageName
                routeScrollEvent(event, packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                if (now - lastWindowEventTime < WINDOW_THROTTLE_MS) return
                lastWindowEventTime = now
                lastPackageName = packageName
                DetectorEngine.forceInvalidate()
                routeWindowEvent(event, packageName)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                if (now - lastContentChangeEventTime < CONTENT_CHANGE_THROTTLE_MS) return
                lastContentChangeEventTime = now
                lastPackageName = packageName
                DetectorEngine.forceInvalidate()
                routeContentChangeEvent(event, packageName)
            }
            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                if (now - lastClickEventTime < CLICK_THROTTLE_MS) return
                lastClickEventTime = now
                lastPackageName = packageName
                routeClickEvent(event, packageName)
            }
            AccessibilityEvent.TYPE_VIEW_FOCUSED -> {
                lastPackageName = packageName
                routeFocusEvent(event, packageName)
            }
        }
    }

    private fun routeScrollEvent(event: AccessibilityEvent, packageName: String) {
        if (cachedAntiScrollEnabled) {
            handleAntiScroll(event, packageName)
        }
        if (cachedAntiReelsEnabled) {
            val rootNode = rootInActiveWindow ?: return
            scope.launch {
                evaluateDetection(packageName, rootNode, FeatureMask.ANTI_REELS, event.eventType)
            }
        }
    }

    private fun routeWindowEvent(event: AccessibilityEvent, packageName: String) {
        if (cachedBrowserBlockEnabled && packageName in supportedBrowserPackages) {
            val rootNode = rootInActiveWindow ?: return
            handleBrowserCheck(packageName, rootNode)
            return
        }

        if (cachedAntiReelsEnabled) {
            val rootNode = rootInActiveWindow ?: return
            scope.launch {
                evaluateDetection(packageName, rootNode, FeatureMask.ANTI_REELS, event.eventType)
            }
        }
    }

    private fun routeContentChangeEvent(event: AccessibilityEvent, packageName: String) {
        if (cachedAntiReelsEnabled) {
            val rootNode = rootInActiveWindow ?: return
            scope.launch {
                evaluateDetection(packageName, rootNode, FeatureMask.ANTI_REELS, event.eventType)
            }
        }
    }

    private fun routeClickEvent(event: AccessibilityEvent, packageName: String) {
        if (cachedAntiReelsEnabled) {
            val rootNode = rootInActiveWindow ?: return
            scope.launch {
                evaluateDetection(packageName, rootNode, FeatureMask.ANTI_REELS, event.eventType)
            }
        }
    }

    private fun routeFocusEvent(event: AccessibilityEvent, packageName: String) {
        if (cachedAntiReelsEnabled) {
            val rootNode = rootInActiveWindow ?: return
            scope.launch {
                evaluateDetection(packageName, rootNode, FeatureMask.ANTI_REELS, event.eventType)
            }
        }
    }

    private fun handleAntiScroll(event: AccessibilityEvent, packageName: String) {
        val scrollY = event.scrollY
        val eventTime = event.eventTime

        scope.launch {
            val analysis = antiScrollEngine?.analyzeScrollEvent(
                packageName = packageName,
                scrollY = scrollY,
                eventTime = eventTime
            )

            val antiScrollInfo = antiScrollEngine?.getDebugInfo(packageName)
            val decisionAction = when (analysis) {
                is ScrollAnalysis.Recorded -> "RECORDED"
                is ScrollAnalysis.Blocked -> "BLOCKED_COOLDOWN"
                is ScrollAnalysis.Debounced -> "DEBOUNCED"
                is ScrollAnalysis.NoiseFiltered -> "NOISE_FILTERED"
                is ScrollAnalysis.Initial -> "INITIAL"
                else -> "UNKNOWN"
            }

            if (prefs?.isDebugModeBlocking() == true) {
                DebugStateHolder.update(DebugState(
                    packageName = packageName,
                    eventType = "SCROLL",
                    surface = DetectionSurface.UNKNOWN,
                    confidence = 0.0,
                    matchResult = DetectionResult.UNKNOWN,
                    matchedIds = emptyList(),
                    matchedContentDescriptions = emptyList(),
                    bounds = null,
                    policyDecision = decisionAction,
                    policyReason = analysis?.toString() ?: "unknown",
                    cooldownActive = cooldownActive &&
                            CooldownEngine.isSourceOrExtraApp(cooldownSourceApp, packageName, cooldownExtraApps),
                    cooldownSourceApp = cooldownSourceApp,
                    cooldownExpiry = cooldownExpiry,
                    antiScrollSessionDuration = antiScrollInfo?.sessionDuration ?: 0L,
                    antiScrollSwipeCount = antiScrollInfo?.recentWindowCount ?: 0,
                    antiScrollDirectionChanges = antiScrollInfo?.directionChanges ?: 0,
                    antiScrollBlocked = antiScrollInfo?.isBlocked ?: false,
                    antiScrollBlockedUntil = antiScrollInfo?.blockedUntil ?: 0L
                ))
            }

            when (analysis) {
                is ScrollAnalysis.Recorded -> {
                    val blockDecision = antiScrollEngine?.shouldBlock(packageName)
                    if (blockDecision is BlockDecision.Blocked) {
                        showBlockOverlay(packageName, "Anti-Scroll Active", "You've been scrolling too much!")
                        scope.launch { prefs?.incrementBlockedCount() }
                    }
                }
                is ScrollAnalysis.Blocked -> {
                    showBlockOverlay(packageName, "Anti-Scroll Active", "Cooldown in progress")
                }
                else -> {}
            }
        }
    }

    private fun handleBrowserCheck(packageName: String, rootNode: AccessibilityNodeInfo) {
        scope.launch {
            val result = browserBlocker?.checkUrl(packageName, rootNode)
            if (result?.blocked == true) {
                showBlockOverlay(packageName, "Website Blocked", "This site is not available")
                prefs?.incrementBlockedCount()
            }
        }
    }

    private suspend fun evaluateDetection(
        packageName: String,
        rootNode: AccessibilityNodeInfo,
        featureMask: Int,
        eventType: Int
    ) {
        PipelineTracker.getInstance(applicationContext).logEvent(
            stage = PipelineStage.PACKAGE_DETECTED,
            packageName = packageName
        )

        // Pass the actual event type to DetectorEngine for proper cache invalidation
        val candidates = DetectorEngine.detect(packageName, rootNode, eventType)
        val topCandidate = candidates.maxByOrNull { it.confidence } ?: return

        PipelineTracker.getInstance(applicationContext).logEvent(
            stage = PipelineStage.DETECTOR_RUNNING,
            packageName = packageName,
            surface = topCandidate.surface,
            confidence = topCandidate.confidence,
            matchResult = topCandidate.matchResult,
            matchedIds = topCandidate.signals.filter { it.type == SignalType.RESOURCE_ID }.map { it.identifier },
            matchedContentDescriptions = topCandidate.signals.filter { it.type == SignalType.CONTENT_DESCRIPTION }.map { it.identifier }
        )

        if (topCandidate.confidence < 0.70) {
            PipelineTracker.getInstance(applicationContext).logEvent(
                stage = PipelineStage.SURFACE_CLASSIFIED,
                packageName = packageName,
                surface = topCandidate.surface,
                confidence = topCandidate.confidence,
                matchResult = topCandidate.matchResult,
                errorMessage = "Confidence below threshold (0.70)"
            )
            return
        }

        val appInfo = try {
            db?.appInfoDao()?.getByPackage(packageName)
        } catch (e: Exception) { null }
        val appEnabled = appInfo?.enabled ?: false

        if (!appEnabled) {
            PipelineTracker.getInstance(applicationContext).logEvent(
                stage = PipelineStage.FEATURE_MAPPED,
                packageName = packageName,
                surface = topCandidate.surface,
                confidence = topCandidate.confidence,
                matchResult = topCandidate.matchResult,
                errorMessage = "App not enabled in ScrollLock"
            )
            return
        }

        // Map detected surface to feature mask for schedule check
        val detectedFeature = surfaceToFeatureMask(topCandidate.surface)

        PipelineTracker.getInstance(applicationContext).logEvent(
            stage = PipelineStage.FEATURE_MAPPED,
            packageName = packageName,
            surface = topCandidate.surface,
            confidence = topCandidate.confidence,
            matchResult = topCandidate.matchResult,
            extraData = mapOf("detectedFeature" to detectedFeature.toString())
        )

        val scheduleActive = if (cachedSchedulesEnabled) {
            try {
                val rules = db?.scheduleRuleDao()?.getEnabled() ?: emptyList()
                ScheduleEngine.isScheduleActive(rules, packageName, detectedFeature)
            } catch (e: Exception) { true }
        } else true

        val cooldownCurrentlyActive = cooldownActive &&
                CooldownEngine.isSourceOrExtraApp(cooldownSourceApp, packageName, cooldownExtraApps)

        // FIX: Provide default Instagram settings if JSON is missing/malformed
        val igSettings = if (packageName == "com.instagram.android") {
            try {
                val json = appInfo?.antiReelsSettingsJSON
                if (json != null) {
                    com.scrolllock.app.data.room.Converters().toInstagramSettings(json)
                } else {
                    // Default settings when JSON is missing
                    InstagramAntiReelsSettings(
                        hideReelsOnHome = true,
                        blockExplore = true,
                        blockMainFeed = false,
                        blockStories = false,
                        blockComments = false,
                        allowReelsInDMs = true,
                        redirectOnBlock = false
                    )
                }
            } catch (e: Exception) {
                // Default settings on parse error
                InstagramAntiReelsSettings(
                    hideReelsOnHome = true,
                    blockExplore = true,
                    blockMainFeed = false,
                    blockStories = false,
                    blockComments = false,
                    allowReelsInDMs = true,
                    redirectOnBlock = false
                )
            }
        } else null

        val context = PolicyEngine.buildContext(
            packageName = packageName,
            appEnabled = appEnabled,
            antiReelsEnabled = cachedAntiReelsEnabled,
            antiScrollEnabled = cachedAntiScrollEnabled,
            browserBlockEnabled = cachedBrowserBlockEnabled,
            scheduleActive = scheduleActive,
            cooldownActive = cooldownCurrentlyActive,
            blockingSessionActive = false,
            confidence = topCandidate.confidence,
            detectedFeature = detectedFeature,
            surfaceName = topCandidate.surface.name,
            instagramSettings = igSettings
        )

        val decision = PolicyEngine.evaluate(context)

        PipelineTracker.getInstance(applicationContext).logEvent(
            stage = PipelineStage.POLICY_EVALUATED,
            packageName = packageName,
            surface = topCandidate.surface,
            confidence = topCandidate.confidence,
            matchResult = topCandidate.matchResult,
            policyAction = decision.action.name,
            policyReason = decision.reason,
            extraData = mapOf(
                "scheduleActive" to scheduleActive.toString(),
                "cooldownActive" to cooldownCurrentlyActive.toString(),
                "appEnabled" to appEnabled.toString()
            )
        )

        if (prefs?.isDebugModeBlocking() == true) {
            DebugEventBus.postEvent(DetectionDebugInfo(
                packageName = packageName,
                surface = topCandidate.surface,
                confidence = topCandidate.confidence,
                matchResult = topCandidate.matchResult,
                signals = topCandidate.signals,
                reasonCodes = topCandidate.reasonCodes
            ))

            val matchedIds = topCandidate.signals
                .filter { it.type == SignalType.RESOURCE_ID }
                .map { it.identifier }
            val matchedDescs = topCandidate.signals
                .filter { it.type == SignalType.CONTENT_DESCRIPTION }
                .map { it.identifier }

            val antiScrollInfo = antiScrollEngine?.getDebugInfo(packageName)
            DebugStateHolder.update(DebugState(
                packageName = packageName,
                eventType = "DETECTION",
                surface = topCandidate.surface,
                confidence = topCandidate.confidence,
                matchResult = topCandidate.matchResult,
                matchedIds = matchedIds,
                matchedContentDescriptions = matchedDescs,
                bounds = topCandidate.bounds,
                policyDecision = decision.action.name,
                policyReason = decision.reason,
                cooldownActive = cooldownCurrentlyActive,
                cooldownSourceApp = cooldownSourceApp,
                cooldownExpiry = cooldownExpiry,
                antiScrollSessionDuration = antiScrollInfo?.sessionDuration ?: 0L,
                antiScrollSwipeCount = antiScrollInfo?.recentWindowCount ?: 0,
                antiScrollDirectionChanges = antiScrollInfo?.directionChanges ?: 0,
                antiScrollBlocked = antiScrollInfo?.isBlocked ?: false,
                antiScrollBlockedUntil = antiScrollInfo?.blockedUntil ?: 0L
            ))
        }

        if (decision.action == PolicyDecision.Action.BLOCK) {
            PipelineTracker.getInstance(applicationContext).logEvent(
                stage = PipelineStage.BLOCK_DECIDED,
                packageName = packageName,
                surface = topCandidate.surface,
                confidence = topCandidate.confidence,
                matchResult = topCandidate.matchResult,
                policyAction = decision.action.name,
                policyReason = decision.reason,
                blockAction = "SHOW_OVERLAY",
                blockReason = decision.reason
            )
        } else if (decision.action == PolicyDecision.Action.REDIRECT) {
            PipelineTracker.getInstance(applicationContext).logEvent(
                stage = PipelineStage.BLOCK_DECIDED,
                packageName = packageName,
                surface = topCandidate.surface,
                confidence = topCandidate.confidence,
                matchResult = topCandidate.matchResult,
                policyAction = decision.action.name,
                policyReason = decision.reason,
                blockAction = "REDIRECT_TO_DM",
                blockReason = decision.reason
            )
        }

        executeDecision(decision, packageName, topCandidate)
    }

    private fun surfaceToFeatureMask(surface: DetectionSurface): Int {
        return when (surface) {
            DetectionSurface.REELS,
            DetectionSurface.SHORTS,
            DetectionSurface.TIKTOK_VIDEO,
            DetectionSurface.VIDEO_FEED,
            DetectionSurface.STORIES,
            DetectionSurface.EXPLORE,
            DetectionSurface.COMMENTS,
            DetectionSurface.MAIN_FEED,
            DetectionSurface.DM -> FeatureMask.ANTI_REELS
            DetectionSurface.BROWSER_URL -> FeatureMask.BROWSER_BLOCKING
            else -> FeatureMask.ANTI_SCROLL
        }
    }

    private fun executeDecision(
        decision: PolicyDecision,
        packageName: String,
        candidate: DetectionCandidate
    ) {
        when (decision.action) {
            PolicyDecision.Action.BLOCK -> {
                val title = when (candidate.surface) {
                    DetectionSurface.REELS -> "Reels Blocked"
                    DetectionSurface.SHORTS -> "Shorts Blocked"
                    DetectionSurface.TIKTOK_VIDEO -> "TikTok Blocked"
                    DetectionSurface.STORIES -> "Stories Blocked"
                    DetectionSurface.EXPLORE -> "Explore Blocked"
                    DetectionSurface.COMMENTS -> "Comments Blocked"
                    else -> "Content Blocked"
                }

                PipelineTracker.getInstance(applicationContext).logEvent(
                    stage = PipelineStage.ENFORCEMENT_ATTEMPTED,
                    packageName = packageName,
                    surface = candidate.surface,
                    blockAction = "SHOW_OVERLAY",
                    blockReason = decision.reason
                )

                showBlockOverlay(packageName, title, decision.reason)
                scope.launch { prefs?.incrementBlockedCount() }

                PipelineTracker.getInstance(applicationContext).logEvent(
                    stage = PipelineStage.ENFORCEMENT_SUCCESS,
                    packageName = packageName,
                    surface = candidate.surface,
                    blockAction = "SHOW_OVERLAY",
                    blockReason = decision.reason
                )
            }
            PolicyDecision.Action.REDIRECT -> {
                if (decision.redirectTarget == "direct" && packageName == "com.instagram.android") {
                    PipelineTracker.getInstance(applicationContext).logEvent(
                        stage = PipelineStage.ENFORCEMENT_ATTEMPTED,
                        packageName = packageName,
                        surface = candidate.surface,
                        blockAction = "REDIRECT_TO_DM",
                        blockReason = decision.reason
                    )
                    performInstagramRedirect()
                    PipelineTracker.getInstance(applicationContext).logEvent(
                        stage = PipelineStage.ENFORCEMENT_SUCCESS,
                        packageName = packageName,
                        surface = candidate.surface,
                        blockAction = "REDIRECT_TO_DM",
                        blockReason = decision.reason
                    )
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
        val p = prefs ?: return
        if (!cachedCooldownEnabled) {
            cooldownActive = false
            return
        }

        val sourceApp = p.getCooldownSourceAppBlocking()
        val duration = p.getCooldownDurationBlocking()
        val start = p.getCooldownStartBlocking()

        cooldownSourceApp = sourceApp.ifEmpty { null }
        cooldownStart = start
        cooldownDuration = duration

        val now = System.currentTimeMillis()
        val expiry = if (start > 0) start + duration * 60_000L else 0L
        cooldownExpiry = expiry

        // Check if cooldown window is globally active (time-based only)
        cooldownActive = start > 0 && now < expiry

        // Fetch extra apps for per-event checks
        cooldownExtraApps = p.getCooldownExtraAppsBlocking()
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
        prefs?.accessibilityServiceConnected?.value = false
        overlayController?.cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Accessibility service destroyed")
        prefs?.accessibilityServiceConnected?.value = false
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