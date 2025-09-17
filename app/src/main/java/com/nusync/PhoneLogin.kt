package com.nusync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nusync.ui.theme.NuSyncTheme
import java.util.concurrent.TimeUnit

class PhoneLogin : ComponentActivity() {

    private lateinit var storedVerificationId: String
    private lateinit var resendToken: PhoneAuthProvider.ForceResendingToken
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val uid = currentUser?.uid

        Log.i("uid", uid.toString())

        if (uid != null) {
            startActivity(Intent(this, MainActivity::class.java))
            Log.i("Main start","MainActivity")
            Log.i("activity", "MainActivity Main start")
            finish()
        }

        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                val context = LocalContext.current
                var phoneNumber by rememberSaveable { mutableStateOf("") }
                var otpCode by rememberSaveable { mutableStateOf("") }
                var verificationInProgress by rememberSaveable { mutableStateOf(false) }
                var message by rememberSaveable { mutableStateOf("") }
                var otpSent by rememberSaveable { mutableStateOf(false) }

                TopBar(heading = "Phone Login") {

                    Column(horizontalAlignment = Alignment.End) {

                        if (!otpSent) {
                            TextField(
                                label = "Phone Number",
                                textValue = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                textType = "phone"
                            )

                            WrapButtonWithBackground(
                                toDoFunction = {
                                    if (phoneNumber.isNotBlank()) {
                                        val options = PhoneAuthOptions.newBuilder(auth)
                                            .setPhoneNumber("+91$phoneNumber")
                                            .setTimeout(60L, TimeUnit.SECONDS)
                                            .setActivity(this@PhoneLogin)
                                            .setCallbacks(object :
                                                PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                                    auth.signInWithCredential(credential)
                                                        .addOnCompleteListener { task ->
                                                            if (task.isSuccessful) {
                                                                val intent = Intent(context, CreateNewUserActivity::class.java)
                                                                intent.putExtra("phone", phoneNumber)
                                                                context.startActivity(intent)
                                                                Log.i("activity", "CreateNewUserActivity onVerificationCompleted")
                                                                finish()
                                                            } else {
                                                                Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                }

                                                override fun onVerificationFailed(e: FirebaseException) {
                                                    Toast.makeText(context, "Verification Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }

                                                override fun onCodeSent(
                                                    verificationId: String,
                                                    token: PhoneAuthProvider.ForceResendingToken
                                                ) {
                                                    storedVerificationId = verificationId
                                                    resendToken = token
                                                    verificationInProgress = true
                                                    otpSent = true
                                                    message = "OTP Sent"
                                                }
                                            }).build()
                                        PhoneAuthProvider.verifyPhoneNumber(options)
                                    } else {
                                        Toast.makeText(context, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                label = "Get OTP"
                            )
                        }

                        if (verificationInProgress) {
                            TextField(
                                label = "Enter OTP",
                                textValue = otpCode,
                                onValueChange = { otpCode = it }
                            )

                            Row {
                                WrapButtonWithBackground(
                                    toDoFunction = {
                                        val credential = PhoneAuthProvider.getCredential(
                                            storedVerificationId,
                                            otpCode
                                        )
                                        auth.signInWithCredential(credential)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnCompleteListener
                                                    val databaseRef = FirebaseDatabase.getInstance().getReference("Users")

                                                    databaseRef.child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                                                        override fun onDataChange(snapshot: DataSnapshot) {
                                                            val intent = if (snapshot.exists()) {
                                                                Log.i("activity", "MainActivity")
                                                                Intent(context, MainActivity::class.java)
                                                                context.startActivity(intent)
                                                                (context as Activity).finish()
                                                            } else {

                                                            }
                                                            /*else {
                                                                Log.i("activity", "CreateNewUserActivity Main start")
                                                                Intent(context, CreateNewUserActivity::class.java).apply {
                                                                    putExtra("phone", phoneNumber)
                                                                }
                                                            }
                                                            context.startActivity(intent)
                                                            (context as Activity).finish()*/
                                                        }

                                                        override fun onCancelled(error: DatabaseError) {
                                                            Toast.makeText(context, "Database error: ${error.message}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    })
                                                } else {
                                                    Toast.makeText(context, "Invalid OTP", Toast.LENGTH_SHORT).show()
                                                }
                                            }

                                    },
                                    label = "Verify OTP"
                                )
                            }
                        }

                        if (message.isNotBlank()) {
                            Text(
                                text = message,
                                color = Color.Red,
                                modifier = Modifier.padding(top = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
