package com.nusync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.nusync.ui.theme.NuSyncTheme
import com.nusync.utils.formatCoating
import com.nusync.utils.formatCoatingType
import com.nusync.utils.formatMaterial

data class CommentUi(
    val firebaseKey: String,
    val detail: String, // Formatted lens details
    val parties: String,
    val commentText: String,
    val commentBy: String
)

class CommentsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                TopBar("Comments") {
                    CommentsScreen()
                }
            }
        }
    }
}

@Composable
fun CommentsScreen() {
    val context = LocalContext.current
    val db = remember { FirebaseDatabase.getInstance() }
    val groupedRef = remember { db.getReference("GroupedLensOrders") }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUid = auth.currentUser?.uid ?: "anonymous_user" // Get current user ID or default

    val commentItems = remember { mutableStateListOf<CommentUi>() }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { lensSnap ->
                    val commentNode = lensSnap.child("comment")

                    // First, check if a comment exists
                    if (!commentNode.exists()) {
                        return@mapNotNull null // Skip this item if no comment
                    }

                    val commentText =
                        commentNode.child("text").getValue(String::class.java) ?: ""

                    // NEW FILTER: Do not show if the comment text is "Rejected"
                    if (commentText == "Rejected") {
                        return@mapNotNull null // Skip this item if the comment is "Rejected"
                    }

                    // Proceed with parsing only if a comment exists AND is not "Rejected"
                    val type = lensSnap.child("type").getValue(String::class.java) ?: "Unknown"
                    val coat = lensSnap.child("coating").getValue(String::class.java) ?: "-"
                    val coatT =
                        lensSnap.child("coatingType").getValue(String::class.java) ?: "-"
                    val mat = lensSnap.child("material").getValue(String::class.java) ?: "-"
                    val sph = lensSnap.child("sphere").getValue(String::class.java) ?: "0.00"
                    val cyl = lensSnap.child("cylinder").getValue(String::class.java) ?: "0.00"
                    val ax = lensSnap.child("axis").getValue(String::class.java) ?: ""
                    val add = lensSnap.child("add").getValue(String::class.java) ?: ""
                    val spec =
                        lensSnap.child("lensSpecificType").getValue(String::class.java) ?: ""
                    val commentBy =
                        commentNode.child("updatedBy").getValue(String::class.java) ?: ""

                    val orders = lensSnap.child("orders").value as? Map<*, *>
                    val parties = orders?.keys?.joinToString(", ") ?: "Unknown"

                    // Build the detailed string for the lens based on your existing logic
                    // Ensure your formatting functions (formatMaterial, formatCoating, formatCoatingType)
                    // are accessible or defined elsewhere in your project.
                    // Using placeholders if they are not provided here.
                    val detail = buildString {
                        when (spec) {
                            "Right" -> append("R ")
                            "Left" -> append("L ")
                            "Both Eye" -> append("BE ")
                            else -> if (type != "Kryptok" && type != "Progressive") append(" ")
                        }
                        when (type) {
                            "SingleVision" -> when {
                                cyl.toDoubleOrNull() == 0.0 -> append(
                                    "%.2f sph %s %s %s".format(
                                        sph.toDouble(),
                                        formatMaterial(mat),
                                        formatCoating(coat),
                                        formatCoatingType(coatT)
                                    )
                                )

                                sph.toDoubleOrNull() == 0.0 -> append(
                                    "%.2f cyl %s %s".format(
                                        cyl.toDouble(),
                                        formatMaterial(mat),
                                        formatCoatingType(coatT)
                                    )
                                )

                                else -> append(
                                    "%.2f/%.2f %s %s %s".format(
                                        sph.toDouble(),
                                        cyl.toDouble(),
                                        formatMaterial(mat),
                                        formatCoating(coat),
                                        formatCoatingType(coatT)
                                    )
                                )
                            }

                            "Kryptok" -> when {
                                cyl.toDoubleOrNull() == 0.0 -> append(
                                    "%.2f | Add +%s %s %s KT".format(
                                        sph.toDouble(),
                                        add,
                                        formatMaterial(mat),
                                        formatCoating(coat)
                                    )
                                )

                                sph.toDoubleOrNull() == 0.0 -> append(
                                    "%.2f x %s | Add +%s %s KT".format(
                                        cyl.toDouble(), ax, add, formatCoating(coat)
                                    )
                                )

                                else -> append(
                                    "%.2f/%.2f x %s | Add +%s %s %s KT".format(
                                        sph.toDouble(),
                                        cyl.toDouble(),
                                        ax,
                                        add,
                                        formatCoating(coat),
                                        formatMaterial(mat)
                                    )
                                )
                            }

                            "Progressive" -> {
                                append(sph)
                                if (cyl != "0.00") append("/$cyl")
                                if (ax.isNotBlank()) append(" x $ax")
                                if (add.isNotBlank()) append(" | Add +$add")
                                append(
                                    " ${formatCoating(coat)} ${formatCoatingType(coatT)} ${
                                        formatMaterial(
                                            mat
                                        )
                                    }"
                                )
                            }
                        }
                    }

                    CommentUi(
                        lensSnap.key!!,
                        detail, // Use the formatted detail string
                        parties,
                        commentText,
                        commentBy
                    )
                }
                commentItems.clear()
                commentItems.addAll(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CommentsScreen", "Firebase data listen failed", error.toException())
            }
        }
        groupedRef.addValueEventListener(listener)
        onDispose { groupedRef.removeEventListener(listener) }
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (commentItems.isEmpty()) {
            Text(
                text = "No pending comments found.", // Updated message
                modifier = Modifier.padding(innerPadding).padding(16.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                items(commentItems, key = { it.firebaseKey }) { item ->
                    CommentDisplayCard(
                        commentItem = item,
                        onAccept = {
                            val intent = Intent(context, EditLensOrderDetailsActivity::class.java).apply {
                                putExtra("firebaseKey", item.firebaseKey)
                                putExtra("commentText", item.commentText) // Pass comment text to display at top
                            }
                            context.startActivity(intent)
                        },
                        onReject = {
                            handleRejectComment(db, item.firebaseKey, currentUid)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CommentDisplayCard(
    commentItem: CommentUi,
    onAccept: (CommentUi) -> Unit,
    onReject: (CommentUi) -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(commentItem.detail, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Clients: ${commentItem.parties}", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(6.dp))
            Text(
                "Comment: \"${commentItem.commentText}\" Updated by - ${commentItem.commentBy}",
                style = MaterialTheme.typography.bodySmall
            )

            // Add the buttons below the comment text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp), // Add some padding from the text above
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    WrapButtonWithBackground(
                        toDoFunction = { onAccept(commentItem) },
                        label = "Accept"
                    )
                }
                Spacer(Modifier.padding(horizontal = 8.dp)) // Space between buttons
                Box(modifier = Modifier.weight(1f)) {
                    WrapButtonWithBackground(
                        toDoFunction = { onReject(commentItem) },
                        label = "Reject"
                    )
                }
            }
        }
    }
}

// Function to handle rejecting a comment
fun handleRejectComment(db: FirebaseDatabase, firebaseKey: String, currentUid: String) {
    val commentRef = db.getReference("GroupedLensOrders").child(firebaseKey).child("comment")

    val updates = mapOf(
        "text" to "Rejected",
        "timestamp" to ServerValue.TIMESTAMP, // Use Firebase server timestamp
        "updatedBy" to currentUid
    )

    commentRef.updateChildren(updates)
        .addOnSuccessListener {
            Log.d("CommentsActivity", "Comment rejected successfully for $firebaseKey")
            // The listener in CommentsScreen will automatically re-filter and update the UI
        }
        .addOnFailureListener { e ->
            Log.e("CommentsActivity", "Failed to reject comment for $firebaseKey: ${e.message}", e)
        }
}