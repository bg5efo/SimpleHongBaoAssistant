package com.simplehongbao.assistant

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Application 类
 * 初始化通知栏服务（让应用在前台运行，防止被系统杀死）
 */
class HongBaoApplication : Application() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "hongbao_service"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()

        // 创建通知栏渠道 (Android 8.0+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "红包助手服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "红包助手无障碍服务运行中"
                setShowBadge(false)
            }

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
}
