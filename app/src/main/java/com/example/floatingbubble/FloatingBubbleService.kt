package com.example.floatingbubble

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var bubbleView: View? = null
    private var menuContainer: FrameLayout? = null
    private var isMenuOpen = false
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var downTime = 0L
    private var downX = 0f
    private var downY = 0f
    private var initialX = 0
    private var initialY = 0
    private var moved = false

    companion object {
        const val ACTION_START = "com.example.floatingbubble.START"
        const val ACTION_STOP = "com.example.floatingbubble.STOP"
        var isRunning = false
    }

    private val actions = listOf(
        BubbleAction.SELECT_ALL,
        BubbleAction.COPY,
        BubbleAction.PASTE,
        BubbleAction.ENTER,
        BubbleAction.ALT,
        BubbleAction.TAB,
        BubbleAction.SHIFT,
        BubbleAction.SCREENSHOT
    )

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            else -> {
                if (!isRunning) {
                    startForegroundNotification()
                    showBubble()
                    isRunning = true
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        closeMenu()
        bubbleView?.let { windowManager.removeView(it) }
        bubbleView = null
        isRunning = false
    }

    private fun windowType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            WindowManager.LayoutParams.TYPE_PHONE

    private fun dp(px: Int): Int =
        (px * resources.displayMetrics.density).toInt()

    private fun startForegroundNotification() {
        val channelId = "bubble_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "حباب شناور", NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = Notification.Builder(this, channelId)
            .setContentTitle("حباب شناور فعال است")
            .setContentText("برای عملیات از حباب استفاده کنید")
            .setSmallIcon(R.drawable.ic_bubble)
            .build()
        startForeground(1, notification)
    }

    private fun showBubble() {
        val view = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null)
        val params = WindowManager.LayoutParams(
            dp(56), dp(56),
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200
        view.tag = params
        view.findViewById<View>(R.id.bubble).setOnTouchListener(BubbleTouchListener())
        windowManager.addView(view, params)
        bubbleView = view
    }

    private fun openMenu(centerX: Int, centerY: Int) {
        if (isMenuOpen) return
        isMenuOpen = true

        val container = FrameLayout(this)
        container.setBackgroundColor(Color.parseColor("#33000000"))
        container.setOnClickListener { closeMenu() }

        val radius = dp(110)
        val n = actions.size
        actions.forEachIndexed { i, action ->
            val angle = Math.toRadians(-90.0 + i * 360.0 / n)
            val x = centerX + (radius * Math.cos(angle)).toInt()
            val y = centerY + (radius * Math.sin(angle)).toInt()
            val btn = makeMenuItem(action)
            val lp = FrameLayout.LayoutParams(dp(56), dp(56))
            lp.leftMargin = x - dp(28)
            lp.topMargin = y - dp(28)
            container.addView(btn, lp)
        }

        menuContainer = container
        val mlp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            windowType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        windowManager.addView(container, mlp)
    }

    private fun makeMenuItem(action: BubbleAction): TextView {
        val btn = TextView(this)
        btn.text = "${action.emoji}\n${action.label}"
        btn.textSize = 12f
        btn.setTextColor(Color.WHITE)
        btn.gravity = android.view.Gravity.CENTER
        btn.setBackgroundResource(R.drawable.menu_item_bg)
        btn.setOnClickListener {
            performAction(action)
        }
        return btn
    }

    private fun closeMenu() {
        if (!isMenuOpen) return
        menuContainer?.let { windowManager.removeView(it) }
        menuContainer = null
        isMenuOpen = false
    }

    private fun performAction(action: BubbleAction) {
        when (action) {
            BubbleAction.SELECT_ALL -> sendCombo("KEYCODE_CTRL_LEFT", "KEYCODE_A")
            BubbleAction.COPY -> sendCombo("KEYCODE_CTRL_LEFT", "KEYCODE_C")
            BubbleAction.PASTE -> sendCombo("KEYCODE_CTRL_LEFT", "KEYCODE_V")
            BubbleAction.ENTER -> sendKey("KEYCODE_ENTER")
            BubbleAction.ALT -> sendKey("KEYCODE_ALT_LEFT")
            BubbleAction.TAB -> sendKey("KEYCODE_TAB")
            BubbleAction.SHIFT -> sendKey("KEYCODE_SHIFT_LEFT")
            BubbleAction.SCREENSHOT -> takeScreenshot()
        }
        closeMenu()
    }

    /** Ctrl/Cmd style combos: prefer root, fall back to accessibility for copy/paste. */
    private fun sendCombo(vararg keys: String) {
        if (RootHelper.hasRoot) {
            RootHelper.sendKey(*keys)
            return
        }
        when {
            keys.contains("KEYCODE_A") ->
                toast("انتخاب همه نیاز به روت دارد")
            keys.contains("KEYCODE_C") -> accAction(GLOBAL_ACTION_COPY)
            keys.contains("KEYCODE_V") -> accAction(GLOBAL_ACTION_PASTE)
        }
    }

    private fun sendKey(key: String) {
        if (RootHelper.hasRoot) {
            RootHelper.sendKey(key)
        } else {
            toast("دکمه $key فقط با روت کار می‌کند")
        }
    }

    private fun accAction(global: Int) {
        val acc = BubbleAccessibilityService.instance
        if (acc != null) {
            acc.performGlobalAction(global)
        } else {
            toast("سرویس دسترسی فعال نیست")
        }
    }

    private fun takeScreenshot() {
        val acc = BubbleAccessibilityService.instance
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && acc != null) {
            acc.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else if (RootHelper.hasRoot) {
            RootHelper.runAsRoot("input keyevent KEYCODE_SYSRQ")
        } else {
            toast("اسکرین‌شات نیاز به روت یا اندروید ۹+ دارد")
        }
    }

    private fun toast(msg: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
    }

    private inner class BubbleTouchListener : View.OnTouchListener {
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            val params = bubbleView?.tag as? WindowManager.LayoutParams ?: return false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    initialX = params.x
                    initialY = params.y
                    moved = false
                    downTime = System.currentTimeMillis()
                    longPressRunnable = Runnable {
                        openMenu(params.x + dp(28), params.y + dp(28))
                    }
                    longPressHandler.postDelayed(longPressRunnable!!, 400)
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downX
                    val dy = event.rawY - downY
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        moved = true
                        longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                        params.x = initialX + dx.toInt()
                        params.y = initialY + dy.toInt()
                        windowManager.updateViewLayout(bubbleView, params)
                    }
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                    if (isMenuOpen) return true
                    if (!moved && System.currentTimeMillis() - downTime < 400) {
                        openMenu(params.x + dp(28), params.y + dp(28))
                    }
                    return true
                }
            }
            return false
        }
    }
}

// GLOBAL_ACTION constants live in AccessibilityService; re-declare locally for convenience.
private const val GLOBAL_ACTION_COPY = android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_COPY
private const val GLOBAL_ACTION_PASTE = android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_PASTE
