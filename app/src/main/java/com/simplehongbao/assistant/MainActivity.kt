package com.simplehongbao.assistant

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * 主界面 - 精简版红包助手设置
 * 功能：开启/关闭、类型选择、响应速度设置
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 检查无障碍服务是否已开启
        checkAccessibilityEnabled()

        // 获取控件
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val swEnable = findViewById<Switch>(R.id.swEnable)
        val swNormal = findViewById<Switch>(R.id.swNormal)
        val swShuffled = findViewById<Switch>(R.id.swShuffled)
        val tvSpeed = findViewById<TextView>(R.id.tvSpeed)
        // 加载保存的设置
        loadSettings()

        // 开启/关闭开关
        swEnable.isChecked = WxBaoAccessibilityService.isEnabled
        swEnable.setOnCheckedChangeListener { _, isChecked ->
            WxBaoAccessibilityService.isEnabled = isChecked
            if (isChecked) {
                tvStatus.text = "红包助手已启动"
                Toast.makeText(this, "已开启红包助手", Toast.LENGTH_SHORT).show()
            } else {
                tvStatus.text = "红包助手已关闭"
                Toast.makeText(this, "已关闭红包助手", Toast.LENGTH_SHORT).show()
            }
            saveSettings()
        }

        // 普通红包开关
        swNormal.isChecked = WxBaoAccessibilityService.grabNormalRedPacket
        swNormal.setOnCheckedChangeListener { _, isChecked ->
            WxBaoAccessibilityService.grabNormalRedPacket = isChecked
            saveSettings()
        }

        // 拼手气红包开关
        swShuffled.isChecked = WxBaoAccessibilityService.grabShuffledRedPacket
        swShuffled.setOnCheckedChangeListener { _, isChecked ->
            WxBaoAccessibilityService.grabShuffledRedPacket = isChecked
            saveSettings()
        }

        // 响应速度按钮
        updateSpeedText(tvSpeed)
        tvSpeed.setOnClickListener {
            when (WxBaoAccessibilityService.responseDelayMs) {
                50L -> WxBaoAccessibilityService.responseDelayMs = 100L
                100L -> WxBaoAccessibilityService.responseDelayMs = 200L
                200L -> WxBaoAccessibilityService.responseDelayMs = 300L
                300L -> WxBaoAccessibilityService.responseDelayMs = 500L
                500L -> WxBaoAccessibilityService.responseDelayMs = 1000L
                else -> WxBaoAccessibilityService.responseDelayMs = 50L
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
            Toast.makeText(
                this,
                "请在系统设置中开启红包助手的无障碍服务",
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        return enabled
    }

    private fun updateSpeedText(tv: TextView) {
        val ms = WxBaoAccessibilityService.responseDelayMs
        val label = when {
            ms <= 100 -> "极速 (100ms)"
            ms <= 300 -> "快速 (300ms)"
            ms <= 500 -> "适中 (500ms)"
            else -> "稳定 (1000ms)"
        }
        tv.text = label
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("hongbao_prefs", MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean("enabled", WxBaoAccessibilityService.isEnabled)
            putBoolean("grab_normal", WxBaoAccessibilityService.grabNormalRedPacket)
            putBoolean("grab_shuffled", WxBaoAccessibilityService.grabShuffledRedPacket)
            putLong("delay_ms", WxBaoAccessibilityService.responseDelayMs)
            apply()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("hongbao_prefs", MODE_PRIVATE)
        WxBaoAccessibilityService.isEnabled = prefs.getBoolean("enabled", false)
        WxBaoAccessibilityService.grabNormalRedPacket = prefs.getBoolean("grab_normal", true)
        WxBaoAccessibilityService.grabShuffledRedPacket = prefs.getBoolean("grab_shuffled", true)
        WxBaoAccessibilityService.responseDelayMs = prefs.getLong("delay_ms", 200L)
    }
}
