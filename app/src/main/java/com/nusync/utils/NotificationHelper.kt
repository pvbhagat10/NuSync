package com.nusync.utils

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase

object NotificationHelper {

    private const val TAG = "NotificationHelper"

    fun sendAdminNotification(
        context: Context,
        type: String,
        detail: String,
        initiatorName: String
    ) {
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser != null) {
            val functions = Firebase.functions
            val data = hashMapOf(
                "type" to type,
                "detail" to detail,
                "initiatorName" to initiatorName
            )

            functions
                .getHttpsCallable("sendAdminNotification")
                .call(data)
                .addOnSuccessListener { httpsCallableResult ->
                    Log.d(TAG, "Admin notification function called successfully: ${httpsCallableResult.data}")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to send admin notification:", exception)
                    if (exception is FirebaseFunctionsException) {
                        val code = exception.code
                        val message = exception.message
                        Log.e(
                            TAG,
                            "Functions error: code=$code, message=$message, details=${exception.details}"
                        )
                        if (code == FirebaseFunctionsException.Code.UNAUTHENTICATED) {
                            Log.w(
                                TAG,
                                "Cloud Function explicitly returned UNAUTHENTICATED error. User token likely missing or invalid."
                            )
                            Toast.makeText(
                                context,
                                "Authentication required to send admin notification.",
                                Toast.LENGTH_LONG
                            ).show()
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to send admin notification: $message",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Failed to send admin notification: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } else {
            Log.w(TAG, "User not authenticated. Cannot send admin notification.")
            Toast.makeText(
                context,
                "You must be signed in to send an admin notification.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
