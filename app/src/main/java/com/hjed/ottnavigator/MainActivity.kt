package com.hjed.ottnavigator

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat

class MainActivity : ComponentActivity() {

    private val playlistUrl = "https://raw.githubusercontent.com/hjacked/iliong/refs/heads/main/channels.m3u"
    private var playerKeyHandler: ((Int) -> Boolean)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.decorView.isFocusableInTouchMode = true
        window.decorView.requestFocus()
        window.decorView.setOnKeyListener { _: View, keyCode: Int, event: KeyEvent ->
            event.action == KeyEvent.ACTION_DOWN &&
                event.repeatCount == 0 &&
                playerKeyHandler?.invoke(keyCode) == true
        }

        setContent {
            StreamNavRoot(
                defaultPlaylistUrl = playlistUrl,
                onExitApp = { finishAndRemoveTask() },
                onPlayerKeyHandlerChanged = { handler -> playerKeyHandler = handler }
            )
        }
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (
            event.action == MotionEvent.ACTION_BUTTON_PRESS &&
            event.actionButton == MotionEvent.BUTTON_SECONDARY &&
            event.isFromSource(InputDevice.SOURCE_MOUSE)
        ) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }

        return super.dispatchGenericMotionEvent(event)
    }
}
