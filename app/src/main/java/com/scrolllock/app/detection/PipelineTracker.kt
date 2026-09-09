package com.scrolllock.app.detection

import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.concurrent.ConcurrentHashMap

data class PipelineEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val stage: PipelineStage,
    val packageName: String = "",
    val eventType: String = "",
    val surface: DetectionSurface = DetectionSurface.UNKNOWN,
    val confidence: Double = 0.0,
    val matchResult: DetectionResult = DetectionResult.UNKNOWN,
    val policyAction: String = "",
    val policyReason: String = "",
    val blockAction: String = "",
    val blockReason: String = "",
    val matchedIds: List<String> = emptyList(),
    val matchedContentDescriptions: List<String> = emptyList(),
    val nodeHierarchy: String = "",
    val errorMessage: String? = null,
    val extraData: Map<String, String> = emptyMap()
)

enum class PipelineStage {
    EVENT_RECEIVED,
    PACKAGE_DETECTED,
    DETECTOR_RUNNING,
    SURFACE_CLASSIFIED,
    FEATURE_MAPPED,
    POLICY_EVALUATED,
    BLOCK_DECIDED,
    ENFORCEMENT_ATTEMPTED,
    ENFORCEMENT_SUCCESS,
    ENFORCEMENT_FAILED,
    ERROR
}

class PipelineTracker private constructor(private val context: Context) : LifecycleObserver {

    companion object {
        @Volatile
        private var INSTANCE: PipelineTracker? = null
        private val lock = Any()

        fun getInstance(context: Context): PipelineTracker {
            synchronized(lock) {
                if (INSTANCE == null) {
                    INSTANCE = PipelineTracker(context.applicationContext)
                }
                return INSTANCE!!
            }
        }
    }

    private val _events = MutableStateFlow<PipelineEvent>(PipelineEvent(stage = PipelineStage.EVENT_RECEIVED))
    val events: MutableStateFlow<PipelineEvent> = _events

    private val _latestEvents = ConcurrentHashMap<PipelineStage, PipelineEvent>()
    fun getLatestForStage(stage: PipelineStage): PipelineEvent? = _latestEvents[stage]
    val latestEventsMap: Map<PipelineStage, PipelineEvent>
        get() = _latestEvents.toMap()

    fun addEvent(event: PipelineEvent) {
        _events.value = event
        _latestEvents[event.stage] = event
    }

    fun getAllLatestEvents(): List<PipelineEvent> = _latestEvents.values.toList()

    fun clear() {
        _events.value = PipelineEvent(stage = PipelineStage.EVENT_RECEIVED)
        _latestEvents.clear()
    }

    // Lifecycle methods
    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        clear()
    }
}

// Extension function for easy logging from anywhere in the pipeline
fun PipelineTracker.logEvent(
    stage: PipelineStage,
    packageName: String = "",
    eventType: String = "",
    surface: DetectionSurface = DetectionSurface.UNKNOWN,
    confidence: Double = 0.0,
    matchResult: DetectionResult = DetectionResult.UNKNOWN,
    policyAction: String = "",
    policyReason: String = "",
    blockAction: String = "",
    blockReason: String = "",
    matchedIds: List<String> = emptyList(),
    matchedContentDescriptions: List<String> = emptyList(),
    nodeHierarchy: String = "",
    errorMessage: String? = null,
    extraData: Map<String, String> = emptyMap()
) {
    addEvent(PipelineEvent(
        stage = stage,
        packageName = packageName,
        eventType = eventType,
        surface = surface,
        confidence = confidence,
        matchResult = matchResult,
        policyAction = policyAction,
        policyReason = policyReason,
        blockAction = blockAction,
        blockReason = blockReason,
        matchedIds = matchedIds,
        matchedContentDescriptions = matchedContentDescriptions,
        nodeHierarchy = nodeHierarchy,
        errorMessage = errorMessage,
        extraData = extraData
    ))
}