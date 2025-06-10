package com.yourname.chatgptassistant.assist

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class ChatGPTAccessibilityService : AccessibilityService() {

    private val pkgChatGPT = "com.openai.chatgpt"
    private var pendingQuery: String? = null

    override fun onServiceConnected() {
        serviceInfo = serviceInfo.apply {
            packageNames = arrayOf(pkgChatGPT)
            flags = flags or AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
        }
        register()
    }

    private fun register() {
        val lbm = LocalBroadcastManager.getInstance(this)
        lbm.registerReceiver(receiver, android.content.IntentFilter().apply {
            addAction("SEND_QUERY")
            addAction("STOP_ALL")
            addAction("SPEAK_CLONED")
        })
    }

    private val receiver = android.content.BroadcastReceiver { _, intent ->
        when(intent.action) {
            "SEND_QUERY" -> {
                pendingQuery = intent.getStringExtra("query")
                ensureChatGPTRunning()
            }
            "STOP_ALL" -> {
                pendingQuery = null
            }
            "SPEAK_CLONED" -> {
                // Not implemented in basic version
            }
        }
    }

    private fun ensureChatGPTRunning() {
        if (rootInActiveWindow?.packageName != pkgChatGPT) {
            val launch = packageManager.getLaunchIntentForPackage(pkgChatGPT)
            launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launch)
        } else {
            injectTextAndSend()
        }
    }

    private fun injectTextAndSend() {
        val root = rootInActiveWindow ?: return
        val edit = root.findAccessibilityNodeInfosByViewId(
            "$pkgChatGPT:id/compose_text_field"
        ).firstOrNull() ?: return
        edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        val args = android.os.Bundle()
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, pendingQuery)
        edit.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)

        val sendBtn = root.findAccessibilityNodeInfosByText("Send").firstOrNull()
        sendBtn?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        pendingQuery = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Basic version: no reply reading
    }

    override fun onInterrupt() { }
}
