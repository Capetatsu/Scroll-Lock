package com.scrolllock.app.intervention

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.scrolllock.app.R

class OverlayController(private val context: Context) {
    companion object {
        private const val TAG = "OverlayController"
        private const val GEOMETRY_THRESHOLD = 8
        private const val DUPLICATE_SUPPRESS_MS = 2000L
    }

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var overlayView: LinearLayout? = null

    @Volatile
    private var overlayVisible = false

    private var previousBounds: Rect? = null
    private var currentTitle: String = ""
    private var currentReason: String = ""
    private var dismissRunnable: Runnable? = null
    private var lastShowTime = 0L
    private var lastShowPackage: String? = null

    fun show(targetRect: Rect?, title: String = "Blocked by ScrollLock", reason: String = "Take a break!") {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { show(targetRect, title, reason) }
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastShowTime < DUPLICATE_SUPPRESS_MS && title == currentTitle) {
            if (targetRect != null && previousBounds != null) {
                val dx = kotlin.math.abs(targetRect.left - previousBounds!!.left)
                val dy = kotlin.math.abs(targetRect.top - previousBounds!!.top)
                if (dx < GEOMETRY_THRESHOLD && dy < GEOMETRY_THRESHOLD) return
            } else if (targetRect == null && previousBounds == null) {
                return
            }
        }

        removeOverlay()

        val layout = createOverlayLayout(title, reason, showGoBack = true)
        val params = createLayoutParams(targetRect)

        try {
            windowManager.addView(layout, params)
            overlayView = layout
            overlayVisible = true
            previousBounds = targetRect?.let { Rect(it) }
            currentTitle = title
            currentReason = reason
            lastShowTime = now
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add overlay: ${e.message}")
        }
    }

    fun showFullScreen(title: String = "Blocked by ScrollLock", reason: String = "Time to take a break!") {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showFullScreen(title, reason) }
            return
        }

        val now = System.currentTimeMillis()
        if (overlayVisible && overlayView != null && title == currentTitle) {
            if (now - lastShowTime < DUPLICATE_SUPPRESS_MS) return
        }

        removeOverlay()

        val layout = createOverlayLayout(title, reason, showGoBack = true)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        try {
            windowManager.addView(layout, params)
            overlayView = layout
            overlayVisible = true
            previousBounds = null
            currentTitle = title
            currentReason = reason
            lastShowTime = now
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add fullscreen overlay: ${e.message}")
        }
    }

    fun showWithCountdown(title: String, reason: String, countdownSeconds: Int, onExpired: () -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post { showWithCountdown(title, reason, countdownSeconds, onExpired) }
            return
        }

        removeOverlay()

        val countdownText = TextView(context).apply {
            setTextColor(0xFFFFFFFF.toInt())
            textSize = 48f
            gravity = Gravity.CENTER
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xE61A1C18.toInt())

            addView(TextView(context).apply {
                text = title
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            })

            addView(TextView(context).apply {
                text = reason
                setTextColor(0xB3FFFFFF.toInt())
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            })

            addView(countdownText)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START

        try {
            windowManager.addView(layout, params)
            overlayView = layout
            overlayVisible = true
            previousBounds = null
            currentTitle = title
            currentReason = reason
            lastShowTime = System.currentTimeMillis()

            var remaining = countdownSeconds
            val countdownRunnable = object : Runnable {
                override fun run() {
                    if (remaining > 0) {
                        countdownText.text = "${remaining}s"
                        remaining--
                        mainHandler.postDelayed(this, 1000L)
                    } else {
                        hide()
                        onExpired()
                    }
                }
            }
            dismissRunnable = countdownRunnable
            mainHandler.post(countdownRunnable)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to add countdown overlay: ${e.message}")
        }
    }

    fun hide() {
        dismissRunnable?.let { mainHandler.removeCallbacks(it) }
        dismissRunnable = null
        removeOverlay()
    }

    private fun removeOverlay() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: IllegalArgumentException) {
                // View was already removed
            } catch (e: Exception) {
                Log.w(TAG, "Error removing overlay: ${e.message}")
            }
        }
        overlayView = null
        overlayVisible = false
        previousBounds = null
    }

    private fun createOverlayLayout(title: String, reason: String, showGoBack: Boolean): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(48, 48, 48, 48)
            setBackgroundColor(0xE61A1C18.toInt())

            addView(TextView(context).apply {
                text = title
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 24f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 24)
            })

            addView(TextView(context).apply {
                text = reason
                setTextColor(0xB3FFFFFF.toInt())
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, 48)
            })

            if (showGoBack) {
                addView(Button(context).apply {
                    text = "Go Back"
                    setOnClickListener {
                        hide()
                        val intent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
                            addCategory(android.content.Intent.CATEGORY_HOME)
                            flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    }
                })
            }
        }
    }

    private fun createLayoutParams(targetRect: Rect?): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    fun cleanup() {
        hide()
    }

    fun isOverlayVisible(): Boolean = overlayVisible

    fun getCurrentTitle(): String = currentTitle
}
