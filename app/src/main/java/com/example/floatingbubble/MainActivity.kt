package com.example.floatingbubble

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.appcompat.app.AppCompatActivity
import com.example.floatingbubble.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnOverlay.setOnClickListener { requestOverlay() }
        binding.btnAccessibility.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        binding.btnToggle.setOnClickListener { toggleBubble() }
        binding.btnTestRoot.setOnClickListener { testRoot() }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val overlay = Settings.canDrawOverlays(this)
        binding.tvOverlayStatus.text =
            if (overlay) "دسترسی Overlay: فعال ✅" else "دسترسی Overlay: غیرفعال ❌"

        val acc = isAccessibilityEnabled()
        binding.tvAccStatus.text =
            if (acc) "سرویس دسترسی: فعال ✅" else "سرویس دسترسی: غیرفعال ❌"
    }

    private fun isAccessibilityEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.installedAccessibilityServiceList.any {
            it.id.contains("com.example.floatingbubble/.BubbleAccessibilityService")
        }
    }

    private fun requestOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }
    }

    private fun toggleBubble() {
        if (FloatingBubbleService.isRunning) {
            startService(
                Intent(this, FloatingBubbleService::class.java)
                    .setAction(FloatingBubbleService.ACTION_STOP)
            )
            binding.btnToggle.text = "شروع حباب شناور"
        } else {
            if (!Settings.canDrawOverlays(this)) {
                requestOverlay()
                return
            }
            val intent = Intent(this, FloatingBubbleService::class.java)
                .setAction(FloatingBubbleService.ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            binding.btnToggle.text = "توقف حباب شناور"
        }
    }

    private fun testRoot() {
        Thread {
            val ok = RootHelper.checkRoot()
            runOnUiThread {
                binding.tvRootStatus.text =
                    if (ok) "وضعیت روت: دسترسی دارید ✅" else "وضعیت روت: روت در دسترس نیست ❌"
            }
        }.start()
    }
}
