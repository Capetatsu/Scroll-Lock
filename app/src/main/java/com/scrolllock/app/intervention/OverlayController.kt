package com.scrolllock.app.intervention

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.scrolllock.app.R

class OverlayController(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val mainHandler = Handler(Looper.getMainLooper())

    private var overlayView: LinearLayout? = null
    private var overlayVisible = false
    private var previousBounds: Rect? = null
    private var currentTitle: String = ""
    private var currentReason: String = ""
    private var dismissRunnable: Runnable? = null

    private val geometryThreshold = 8

    fun show(targetRect: Rect?, title: String = "Blocked by ScrollLock", reason: String = "Take a break!") {
        if (overlayVisible && overlayView != null) {
            if (title == currentTitle && reason == currentReason) {
                if (targetRect != null && previousBounds != null) {
                    val dx = Math.abs(targetRect.left - previousBounds!!.left)
                    val dy = Math.abs(targetRect.top - previousBounds!!.top)
                    if (dx < geometryThreshold && dy < geometryThreshold) return
                }
            }
            removeOverlay()
        }

        val layout = createOverlayLayout(title, reason, showGoBack = true)
        val params = createLayoutParams(targetRect)

        try {
            windowManager.addView(layout, params)
            overlayView = layout
            overlayVisible = true
            previousBounds = targetRect
            currentTitle = title
            currentReason = reason
        } catch (e: Exception) {
            // Overlay permission not granted
        }
    }

    fun showFullScreen(title: String = "Blocked by ScrollLock", reason: String = "Time to take a break!") {
        if (overlayVisible && overlayView != null && title == currentTitle) {
            return
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
        } catch (e: Exception) {
            // Overlay permission not granted
        }
    }

    fun showWithCountdown(title: String, reason: String, countdownSeconds: Int, onExpired: () -> Unit) {
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
            // Overlay permission not granted
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
            } catch (e: Exception) {
                // Already removed
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
}
