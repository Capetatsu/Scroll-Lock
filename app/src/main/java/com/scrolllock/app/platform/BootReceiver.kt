package com.scrolllock.app.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i("ScrollLock", "Boot/package replacement detected, ensuring service is running")
            ensureAccessibilityServiceEnabled(context)
        }
    }

    private fun ensureAccessibilityServiceEnabled(context: Context) {
        val intent = Intent(context, com.scrolllock.app.accessibility.ScrollLockAccessibilityService::class.java)
        try {
            context.startForegroundService(intent)
        } catch (e: Exception) {
            Log.e("ScrollLock", "Failed to start service on boot", e)
        }
    }
}
