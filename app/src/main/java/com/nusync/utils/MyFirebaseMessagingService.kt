package com.nusync.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.nusync.MainActivity
import com.nusync.R

//class MyFirebaseMessagingService : FirebaseMessagingService() {
//
//    private val TAG = "MyFirebaseMsgService"
//
//    /**
//     * Called when message is received.
//     *
//     * @param remoteMessage Object representing the message received from Firebase Cloud Messaging.
//     */
//    override fun onMessageReceived(remoteMessage: RemoteMessage) {
//        Log.d(TAG, "From: ${remoteMessage.from}")
//
//        // Check if message contains a data payload.
//        remoteMessage.data.isNotEmpty().let {
//            Log.d(TAG, "Message data payload: " + remoteMessage.data)
//
//            // You can process data messages here.
//            // If you only send data messages, you'd manually build the notification here.
//            // For example:
//            // val title = remoteMessage.data["title"] ?: "New Order"
//            // val body = remoteMessage.data["body"] ?: "A new lens order has been placed."
//            // sendNotification(title, body)
//        }
//
//        // Check if message contains a notification payload.
//        // FCM automatically displays notification payload messages when app is in background.
//        // For foreground apps, onMessageReceived is called and you should display it.
//        remoteMessage.notification?.let {
//            Log.d(TAG, "Message Notification Body: ${it.body}")
//            sendNotification(it.title, it.body)
//        }
//    }
//
//    /**
//     * Called when a new token for the default Firebase project is generated.
//     *
//     * @param token The new token.
//     */
//    override fun onNewToken(token: String) {
//        Log.d(TAG, "Refreshed token: $token")
//
//        // If you need to send messages to specific application instances
//        // (not just topics), you would send this token to your app server
//        // and store it in your database associated with the user.
//        // For topic-based notifications ("new_orders" to all users), this
//        // is less critical unless you want to track device tokens.
//    }
//
//    /**
//     * Create and show a simple notification containing the received FCM message.
//     *
//     * @param messageTitle FCM message title.
//     * @param messageBody FCM message body.
//     */
//    private fun sendNotification(messageTitle: String?, messageBody: String?) {
//        val intent = Intent(this, MainActivity::class.java) // Target your main activity
//        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
//        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
//            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE) // FLAG_IMMUTABLE is required for Android 12+
//
//        val channelId = "lens_order_channel" // Unique ID for your notification channel
//        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//
//        // Corrected NotificationCompat.Builder usage
//        val notificationBuilder = NotificationCompat.Builder(this, channelId)
//            .setSmallIcon(R.drawable.ic) // Use your notification icon
//            .setContentTitle(messageTitle ?: "New Lens Order") // Corrected method call
//            .setContentText(messageBody ?: "A new order has been placed.") // Corrected method call
//            .setAutoCancel(true) // Automatically dismiss the notification when tapped
//            .setSound(defaultSoundUri)
//            .setContentIntent(pendingIntent)
//
//        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        // Create the NotificationChannel for Android 8.0 (API level 26) and above
//        val channel = NotificationChannel(channelId,
//            "Lens Order Notifications", // User-visible name for the channel
//            NotificationManager.IMPORTANCE_DEFAULT) // Importance level
//        notificationManager.createNotificationChannel(channel)
//
//        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
//    }
//}

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFirebaseMsgService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // ... (your existing onMessageReceived logic)
        Log.d(TAG, "From: ${remoteMessage.from}")

        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            // You can decide what to do with data messages.
            // If it's a notification-like data message:
            val title = remoteMessage.data["title"] ?: "NuSync Update"
            val body = remoteMessage.data["body"] ?: "You have a new update."
            sendNotification(title, body)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Send this token to your app server AND store it in Firebase DB
        // when the user is logged in.
        sendRegistrationToServer(token) // This will now store it in Realtime DB
    }

    private fun sendRegistrationToServer(token: String?) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null && token != null) {
            val userId = currentUser.uid
            // Store the token under the user's node in Realtime Database
            FirebaseDatabase.getInstance().getReference("Users")
                .child(userId)
                .child("fcmToken") // New field to store the token
                .setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM Token stored for user $userId successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to store FCM Token for user $userId: ${e.message}", e)
                }
        } else {
            Log.d(TAG, "User not logged in or token is null, cannot store FCM token.")
        }
    }

    private fun sendNotification(messageTitle: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = getString(R.string.app_name)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_ic_notification)
            .setContentTitle(messageTitle ?: "NuSync Notification")
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Default Channel",
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID of notification */, notificationBuilder.build())
    }
}