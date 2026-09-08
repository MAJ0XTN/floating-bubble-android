package com.example.floatingbubble

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Required to perform Select/Copy/Paste/Screenshot on another app's UI.
 * The user must enable this service in Settings -> Accessibility.
 */
class BubbleAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    companion object {
        var instance: BubbleAccessibilityService? = null
    }
}
