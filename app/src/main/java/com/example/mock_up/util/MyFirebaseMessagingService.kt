package com.example.mock_up.util

import ApiClient
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues.TAG
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.*
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {
    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM Token", "Token: $token")
                //sendRegistrationToServer(token)
            } else {
                Log.w("FCM Token", "Fetching FCM registration token failed", task.exception)
            }
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        //sendRegistrationToServer(token)
    }

//    private fun sendRegistrationToServer(token: String) {
//        // Implement your own logic to submit token to server
//        val apiClient = ApiClient()
//        val userId = "" // 使用你的学生ID
//        apiClient.POST(
//            "https://149.248.20.141:80/submit_push_token",
//            "user_id=$userId&token=$token"
//        )
//    }
override fun onMessageReceived(remoteMessage: RemoteMessage) {
    // Handle FCM messages here
    Log.d(TAG, "From: ${remoteMessage.from}")
    // Check if the message contains data payload
    remoteMessage.data.isNotEmpty().let {
        Log.d(TAG, "Message data payload: ${remoteMessage.data}")
        // Handle data payload
    }
    // Check if the message contains a notification payload
    remoteMessage.notification?.let {
        Log.d(TAG, "Message Notification Body: ${it.body}")
        sendNotification("${it.title}", "${it.body}")
    }
}
    private fun sendNotification(title: String, message: String) {
        val channelId = "MyNotification"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSmallIcon(com.example.mock_up.R.drawable.notification)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Channel human-readable title",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}