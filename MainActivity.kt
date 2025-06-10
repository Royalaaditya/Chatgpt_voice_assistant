package com.yourname.chatgptassistant

import android.os.Bundle
import androidx.annotation.NonNull
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager

class MainActivity: FlutterActivity() {
    private val CHANNEL = "chatgpt.assistant/channel"

    override fun configureFlutterEngine(@NonNull flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "sendQuery" -> {
                    val text = call.argument<String>("text") ?: ""
                    Intent("SEND_QUERY").also {
                        it.putExtra("query", text)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(it)
                    }
                    result.success(null)
                }
                "readReply" -> {
                    // Placeholder: In basic version return empty string
                    result.success("")
                }
                "speakCloned" -> {
                    val txt = call.argument<String>("text") ?: ""
                    Intent("SPEAK_CLONED").also {
                        it.putExtra("speak_text", txt)
                        LocalBroadcastManager.getInstance(this).sendBroadcast(it)
                    }
                    result.success(null)
                }
                "stopActions" -> {
                    Intent("STOP_ALL").also {
                        LocalBroadcastManager.getInstance(this).sendBroadcast(it)
                    }
                    result.success(null)
                }
                else -> result.notImplemented()
            }
        }
    }
}
