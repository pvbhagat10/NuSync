package com.nusync.utils // Assuming this is your package

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth // Import FirebaseAuth
import com.google.firebase.functions.FirebaseFunctionsException // Already imported
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
        val auth = FirebaseAuth.getInstance() // Get FirebaseAuth instance

        if (auth.currentUser != null) {
            // User is signed in, proceed to call the function
            val functions = Firebase.functions
            val data = hashMapOf(
                "type" to type,
                "detail" to detail,
                "initiatorName" to initiatorName
                // Optional: You might want to include auth.currentUser.uid if your
                // cloud function needs to know *who* the admin is,
                // though callable functions automatically get the UID in the context.
                // "adminUid" to auth.currentUser!!.uid
            )

            functions
                .getHttpsCallable("sendAdminNotification") // Name of your Cloud Function
                .call(data)
                .addOnSuccessListener { httpsCallableResult ->
                    Log.d(TAG, "Admin notification function called successfully: ${httpsCallableResult.data}")
                    // Optionally show a success toast or handle success
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to send admin notification:", exception) // Use TAG consistently
                    if (exception is FirebaseFunctionsException) {
                        val code = exception.code
                        val message = exception.message
                        // val details = exception.details // details can sometimes be null
                        Log.e(
                            TAG, // Use TAG
                            "Functions error: code=$code, message=$message, details=${exception.details}"
                        )
                        if (code == FirebaseFunctionsException.Code.UNAUTHENTICATED) {
                            Log.w(
                                TAG, // Use TAG
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
            // User is not signed in.
            Log.w(TAG, "User not authenticated. Cannot send admin notification.")
            Toast.makeText(
                context,
                "You must be signed in to send an admin notification.",
                Toast.LENGTH_LONG
            ).show()
            // Depending on your app's flow, you might want to:
            // 1. Trigger a sign-in flow.
            // 2. Simply inform the user and do nothing further.
        }
    }
}
