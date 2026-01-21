package com.wifi.toolbox.services.pojie

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.wifi.toolbox.R
import com.wifi.toolbox.ui.MainActivity

object PojieNotificationHelper {

    private const val NOTIFICATION_CHANNEL_ID = "PojieServiceChannel"

    /**
     * 创建通知渠道
     * @param context 上下文
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, "密码字典破解服务", NotificationManager.IMPORTANCE_MIN
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * 构建前台服务通知
     * @param context 上下文
     * @return Notification对象
     */
    fun buildForegroundNotification(context: Context): Notification {
        val notificationIntent = Intent(context, MainActivity::class.java).apply {
            putExtra("target", "Pojie")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("密码字典破解服务").setContentText("正在运行")
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentIntent(pendingIntent)
            .setOngoing(true).setPriority(NotificationCompat.PRIORITY_MIN).build()
    }

    /**
     * 获取启动时的ASCII艺术字
     * @return 字符串
     */
    fun getAsciiArt(): String {
        return """     _     __                 _        
     | |___ / _|_ __ ___  _  _| |_ __ _ 
  _  | / __| |_| '_ ` _ \| | | | __/ _` |
 | |_| \__ \  _| | | | | | |_| | || (_| |
  \___/|___/_| |_| |_| |_|\__, |\__\__, |
                          |___/    |___/ 
==========================================
wifi密码暴力破解工具 v3 for Android
"""
    }
}