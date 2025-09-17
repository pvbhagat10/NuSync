package com.nusync

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.nusync.ui.theme.NuSyncTheme

class CreateNewUserActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseAuth.getInstance().signOut()

        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                TopBar(heading = "Create New User") {
//                    CreateNewUserForm()
                }
            }
        }
    }
}

@Composable
fun CreateNewUserForm() {
    val context = LocalContext.current
    val phoneNumber = (context as? Activity)?.intent?.getStringExtra("phone") ?: ""
    val roleOptions = stringArrayResource(id = R.array.roles_array).toList()
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(roleOptions.firstOrNull() ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            label = "Full Name",
            textValue = name,
            onValueChange = { name = it }
        )

        DropdownTextField(
            label = "Select Role",
            selectedOption = selectedRole,
            options = roleOptions,
            onOptionSelected = { selectedRole = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        WrapButtonWithBackground(
            toDoFunction = {
                val auth = FirebaseAuth.getInstance()
                val uid = auth.currentUser?.uid

                if (uid == null) {
                    Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show()
                    return@WrapButtonWithBackground
                }

                if (name.isBlank()) {
                    Toast.makeText(context, "Please enter name", Toast.LENGTH_SHORT).show()
                    return@WrapButtonWithBackground
                }

                val userDetails = mapOf(
                    "name" to name,
                    "role" to selectedRole
                )

                FirebaseDatabase.getInstance()
                    .reference
                    .child("Users")
                    .child(uid)
                    .setValue(userDetails)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Details saved", Toast.LENGTH_SHORT).show()
                        context.startActivity(Intent(context, MainActivity::class.java))
                        (context as? Activity)?.finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(
                            context,
                            "Failed to save user: ${it.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            },
            label = "Save"
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CreateNewUserFormPreview() {
    NuSyncTheme {
        TopBarForLazyColumns("CreateNewUserForm") {
            CreateNewUserForm()
        }
    }
}