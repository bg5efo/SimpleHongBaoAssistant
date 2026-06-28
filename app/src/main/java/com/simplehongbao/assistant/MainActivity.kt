package com.simplehongbao.assistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkAccessibilityEnabled()

        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val swEnable = findViewById<Switch>(R.id.swEnable)
        val swNormal = findViewById<Switch>(R.id.swNormal)
        val swShuffled = findViewById<Switch>(R.id.swShuffled)
        val tvSpeed = findViewById<TextView>(R.id.tvSpeed)

        loadSettings()

        swEnable.isChecked = WxBaoAccessibilityService.isEnabled
        swEnable.setOnCheckedChangeListener { _, isChecked ->
            WxBaoAccessibilityService.isEnabled = isChecked
            tvStatus.text = if (isChecked) "红包助手已启动" else "红包助手已关闭"
            Toast.makeText(this, if (isChecked) "已开启红包助手" else "已关闭红包助手", Toast.LENGTH_SHORT).show()
            saveSettings()
        }

        swNormal.isChecked = WxBaoAccessibilityService.grabNormalRedPacket
        swNormal.setOnCheckedChangeListener { _, isChecked ->
            WxBaoAccessibilityService.grabNormalRedPacket = isChecked
            saveSettings()
        }

        swShuffled.isChecked = WxBaoAccessibilityService.grabShuffledRedPacket
        swShuffled.setOnCheckedChangeListener { _, isChecked ->
            WxBaoAccessibilityService.grabShuffledRedPacket = isChecked
            saveSettings()
        }

        updateSpeedText(tvSpeed)
        tvSpeed.setOnClickListener {
            WxBaoAccessibilityService.responseDelayMs = when (WxBaoAccessibilityService.responseDelayMs) {
                50L -> 100L; 100L -> 200L; 200L -> 300L
                300L -> 500L; 500L -> 1000L; else -> 50L
            }
            updateSpeedText(tvSpeed)
            saveSettings()
            Toast.makeText(this, "响应速度已调整", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_accessibility_services")
            ?.contains("com.simplehongbao.assistant") ?: false
        if (!enabled) {
            Toast.makeText(this, "请在系统设置中开启红包助手的无障碍服务", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        return enabled
    }

    private fun updateSpeedText(tv: TextView) {
        tv.text = when {
            WxBaoAccessibilityService.responseDelayMs <= 100 -> "极速 (100ms)"
            WxBaoAccessibilityService.responseDelayMs <= 300 -> "快速 (300ms)"
            WxBaoAccessibilityService.responseDelayMs <= 500 -> "适中 (500ms)"
            else -> "稳定 (1000ms)"
        }
    }

    private fun saveSettings() {
        getSharedPreferences("hongbao_prefs", MODE_PRIVATE).edit().apply {
            putBoolean("enabled", WxBaoAccessibilityService.isEnabled)
            putBoolean("grab_normal", WxBaoAccessibilityService.grabNormalRedPacket)
            putBoolean("grab_shuffled", WxBaoAccessibilityService.grabShuffledRedPacket)
            putLong("delay_ms", WxBaoAccessibilityService.responseDelayMs)
            apply()
        }
    }

    private fun loadSettings() {
        getSharedPreferences("hongbao_prefs", MODE_PRIVATE).run {
            WxBaoAccessibilityService.isEnabled = getBoolean("enabled", false)
            WxBaoAccessibilityService.grabNormalRedPacket = getBoolean("grab_normal", true)
            WxBaoAccessibilityService.grabShuffledRedPacket = getBoolean("grab_shuffled", true)
            WxBaoAccessibilityService.responseDelayMs = getLong("delay_ms", 200L)
        }
    }
}
