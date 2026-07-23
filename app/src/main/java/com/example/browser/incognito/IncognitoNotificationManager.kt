package com.example.browser.incognito

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class IncognitoNotificationManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "incognito_session_channel"
        const val NOTIFICATION_ID = 2001
        const val ACTION_CLOSE_INCOGNITO = "com.example.ACTION_CLOSE_INCOGNITO"
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Incognito Tabs Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows an active indicator when Incognito tabs are open"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun updateNotification(incognitoCount: Int) {
        if (incognitoCount <= 0) {
            cancelNotification()
            return
        }

        val closeIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_CLOSE_INCOGNITO
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            closeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Incognito tabs open")
            .setContentText("Tap to close all $incognitoCount incognito tab${if (incognitoCount > 1) "s" else ""}")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Close all Incognito tabs",
                pendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
