package com.nusync

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.nusync.ui.theme.NuSyncTheme
import com.nusync.utils.getLensDetailString

class EditUserOrdersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("EditUserOrdersActivity", "Current UID: $currentUid")
        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                TopBar("Edit Orders") {
                    if (currentUid != null) {
                        EditUserOrdersScreen(currentUid)
                    } else {
                        Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

data class LensOrderUi(
    val firebaseKey: String,
    val detail: String, // Combined detail string like in RequirementsScreen
    val fulfilledQty: Double,
    val price: Double,
    val vendor: String,
    val updatedById: String, // Store the raw UID
    val updatedByName: String, // Store the resolved name
    val timestamp: Long,
    val orders: Map<String, Any>,
    val isPartial: Boolean,
    val groupedKey: String? = null, // Add a field to store the grouped key
    val detailMap: Map<String, Any?> // Change this to Any? to hold mixed types
)

/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserOrdersScreen(currentUid: String) {
    val context = LocalContext.current
    val db = remember { FirebaseDatabase.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val completedOrders = remember { mutableStateListOf<LensOrderUi>() }
    val partialOrders = remember { mutableStateListOf<LensOrderUi>() }
    val userNamesMap =
        remember { mutableStateOf<Map<String, String>>(emptyMap()) } // UserID to UserName mapping
    // State for the dialog box
    val showEditDialog = remember { mutableStateOf(false) }
    val editingOrder = remember { mutableStateOf<LensOrderUi?>(null) }
    Log.d(
        "EditUserOrdersScreen",
        "EditUserOrdersScreen Composable recomposed. Current UID: $currentUid"
    )
    DisposableEffect(currentUid) { // Re-trigger effect if currentUid changes
        val completedRef = db.getReference("CompletedLensOrders")
        val partialRef = db.getReference("PartiallyCompletedLensOrders")
        val usersRef = db.getReference("Users")
        Log.d(
            "EditUserOrdersScreen",
            "Setting up Firebase listeners. Current UID for filtering: $currentUid"
        )
        // Listener for User names
        val usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = mutableMapOf<String, String>()
                snapshot.children.forEach { userSnap ->
                    val userId = userSnap.key
                    val userName = userSnap.child("name").getValue(String::class.java)
                    if (userId != null && userName != null) {
                        users[userId] = userName
                        Log.d("UsersListener", "Fetched user: $userId -> $userName")
                    } else {
                        Log.w(
                            "UsersListener",
                            "Skipping user with null ID or name: $userId, $userName"
                        )
                    }
                }
                userNamesMap.value = users
                Log.d("UsersListener", "User names map updated. Size: ${userNamesMap.value.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UsersListener", "Users listen failed", error.toException())
                Toast.makeText(
                    context,
                    "Failed to load user data: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        usersRef.addValueEventListener(usersListener)

        val completedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(
                    "EditUserOrdersScreen",
                    "CompletedLensOrders: Data changed. Children count: ${snapshot.childrenCount}"
                )
                val list = snapshot.children.mapNotNull { orderSnap ->
                    Log.d(
                        "CompletedRawData",
                        "RAW CompletedLensOrders data for key '${orderSnap.key}': ${orderSnap.value}"
                    )
                    val parsedOrder =
                        parseLensOrder(orderSnap, isPartial = false, userNamesMap.value)
                    if (parsedOrder == null) {
                        Log.w(
                            "EditUserOrdersScreen",
                            "CompletedLensOrders: Failed to parse order for key: ${orderSnap.key}"
                        )
                    }
                    parsedOrder
                }
                completedOrders.clear()
                completedOrders.addAll(list)
                Log.d(
                    "EditUserOrdersScreen",
                    "CompletedLensOrders: Updated list size: ${completedOrders.size}"
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "EditUserOrdersScreen",
                    "CompletedLensOrders: Listen failed",
                    error.toException()
                )
                Toast.makeText(
                    context,
                    "Failed to load completed orders: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        val partialListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(
                    "EditUserOrdersScreen",
                    "PartiallyCompletedLensOrders: Data changed. Children count: ${snapshot.childrenCount}"
                )
                val list = snapshot.children.mapNotNull { orderSnap ->
                    Log.d(
                        "PartialRawData",
                        "RAW PartiallyCompletedLensOrders data for key '${orderSnap.key}': ${orderSnap.value}"
                    )
                    val parsedOrder =
                        parseLensOrder(orderSnap, isPartial = true, userNamesMap.value)
                    if (parsedOrder == null) {
                        Log.w(
                            "EditUserOrdersScreen",
                            "PartiallyCompletedLensOrders: Failed to parse order for key: ${orderSnap.key}"
                        )
                    }
                    parsedOrder
                }
                partialOrders.clear()
                partialOrders.addAll(list)
                Log.d(
                    "EditUserOrdersScreen",
                    "PartiallyCompletedLensOrders: Updated list size: ${partialOrders.size}"
                )
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e(
                    "EditUserOrdersScreen",
                    "PartiallyCompletedLensOrders: Listen failed",
                    error.toException()
                )
                Toast.makeText(
                    context,
                    "Failed to load partial orders: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
        completedRef.addValueEventListener(completedListener)
        partialRef.addValueEventListener(partialListener)
        onDispose {
            Log.d("EditUserOrdersScreen", "Removing Firebase listeners on dispose.")
            completedRef.removeEventListener(completedListener)
            partialRef.removeEventListener(partialListener)
            usersRef.removeEventListener(usersListener)
        }
    }
    // Combine and filter orders
    val currentUserOrders =
        remember(completedOrders, partialOrders, userNamesMap.value, currentUid) {
            val combined = (completedOrders + partialOrders)
            Log.d(
                "EditUserOrdersScreen",
                "Combining all orders. Total combined size: ${combined.size}"
            )
            // Filter by current user's ID
            val filtered = combined.filter { it.updatedById == currentUid }
            Log.d(
                "EditUserOrdersScreen",
                "Filtered orders by current UID ($currentUid). Filtered size: ${filtered.size}"
            )
            // Sort by timestamp (most recent first)
            filtered.sortedByDescending { it.timestamp }
        }
    if (currentUserOrders.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "No orders updated by you to display.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(8.dp))
            if (currentUid == "unknown") {
                Text(
                    "Your user ID is unknown. Please ensure you are logged in.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "Ensure Firebase Realtime Database rules allow read access and data exists with your user ID in 'CompletedLensOrders' or 'PartiallyCompletedLensOrders'.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Log.d(
                "EditUserOrdersScreen",
                "Displaying 'No orders' message for current user. Current UID: $currentUid"
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            items(currentUserOrders, key = { it.firebaseKey }) { order ->
                UserOrderItem(order = order) { clickedOrder ->
                    editingOrder.value = clickedOrder
                    showEditDialog.value = true
                }
            }
            Log.d(
                "EditUserOrdersScreen",
                "LazyColumn rendering ${currentUserOrders.size} items for current user."
            )
        }
    }
    // --- Edit Order Dialog ---
    if (showEditDialog.value) {
        editingOrder.value?.let { orderToEdit ->
            EditOrderDialog(
                order = orderToEdit,
                onDismiss = { showEditDialog.value = false },
                onSave = { updatedOrder ->
                    // Logic to save updated order to Firebase
                    updateOrderInFirebase(context, db, updatedOrder)
                    showEditDialog.value = false
                }
            )
        }
    }
}*/

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserOrdersScreen(currentUid: String) {
    val context = LocalContext.current
    val db = remember { FirebaseDatabase.getInstance() }

    // State is now simpler and more idiomatic for list replacement
    var allCompletedOrders by remember { mutableStateOf(emptyList<LensOrderUi>()) }
    var allPartialOrders by remember { mutableStateOf(emptyList<LensOrderUi>()) }
    // The user map is now nullable to signal when it's ready
    var userNamesMap by remember { mutableStateOf<Map<String, String>?>(null) }

    val isLoading = userNamesMap == null // Loading is simply when the user map isn't ready

    val showEditDialog = remember { mutableStateOf(false) }
    val editingOrder = remember { mutableStateOf<LensOrderUi?>(null) }

    DisposableEffect(currentUid) {
        val usersRef = db.getReference("Users")
        val completedRef = db.getReference("CompletedLensOrders")
        val partialRef = db.getReference("PartiallyCompletedLensOrders")

        // 1. LISTENER FOR USERS
        val usersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("LiveUpdateDebug", "User data updated.")
                val users = mutableMapOf<String, String>()
                snapshot.children.forEach { userSnap ->
                    val userId = userSnap.key
                    val userName = userSnap.child("name").getValue(String::class.java)
                    if (userId != null && userName != null) {
                        users[userId] = userName
                    }
                }
                userNamesMap = users // Set the map, this will trigger recomposition
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveUpdateDebug", "Failed to load users.", error.toException())
                userNamesMap = emptyMap() // Stop loading on error
            }
        }

        // 2. LISTENER FOR COMPLETED ORDERS
        val completedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("LiveUpdateDebug", "Completed orders data updated.")
                // Only parse if the user map is ready
                userNamesMap?.let { uMap ->
                    val list = snapshot.children.mapNotNull {
                        parseLensOrder(it, isPartial = false, uMap)
                    }
                    allCompletedOrders = list
                    Log.d("LiveUpdateDebug", "UI updated with ${list.size} completed orders.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveUpdateDebug", "Failed to load completed orders.", error.toException())
            }
        }

        // 3. LISTENER FOR PARTIAL ORDERS
        val partialListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("LiveUpdateDebug", "Partial orders data updated.")
                userNamesMap?.let { uMap ->
                    val list = snapshot.children.mapNotNull {
                        parseLensOrder(it, isPartial = true, uMap)
                    }
                    allPartialOrders = list
                    Log.d("LiveUpdateDebug", "UI updated with ${list.size} partial orders.")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveUpdateDebug", "Failed to load partial orders.", error.toException())
            }
        }

        // Attach all listeners independently
        usersRef.addValueEventListener(usersListener)
        completedRef.addValueEventListener(completedListener)
        partialRef.addValueEventListener(partialListener)

        // Cleanup when the screen is closed
        onDispose {
            Log.d("LiveUpdateDebug", "Removing all listeners.")
            usersRef.removeEventListener(usersListener)
            completedRef.removeEventListener(completedListener)
            partialRef.removeEventListener(partialListener)
        }
    }

    // This UI logic now correctly reacts to any state change from the listeners above
    val combinedOrders = allCompletedOrders + allPartialOrders
    val currentUserOrders = combinedOrders
        .filter { it.updatedById == currentUid }
        .sortedByDescending { it.timestamp }

    val debugMessage =
        "UID: $currentUid | Total Parsed: ${combinedOrders.size} | Your Orders: ${currentUserOrders.size}"

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (currentUserOrders.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "No orders updated by you to display.",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(16.dp))
            Text("Debug Info:", style = MaterialTheme.typography.labelMedium)
            Text(debugMessage, style = MaterialTheme.typography.bodySmall)
        }
    } else {
        LazyColumn(modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)) {
            items(currentUserOrders, key = { it.firebaseKey }) { order ->
                UserOrderItem(order = order) { clickedOrder ->
                    editingOrder.value = clickedOrder
                    showEditDialog.value = true
                }
            }
        }
    }

    if (showEditDialog.value) {
        editingOrder.value?.let { orderToEdit ->
            EditOrderDialog(
                order = orderToEdit,
                onDismiss = { showEditDialog.value = false },
                onSave = { updatedOrder ->
                    // Logic to save updated order to Firebase
                    updateOrderInFirebase(context, db, updatedOrder)
                    showEditDialog.value = false
                }
            )
        }
    }
}

@Composable
fun UserOrderItem(order: LensOrderUi, onClick: (LensOrderUi) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick(order) }, // Add clickable modifier here
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = if (order.isPartial) "PARTIAL ORDER" else "COMPLETED ORDER",
                style = MaterialTheme.typography.labelSmall,
                color = if (order.isPartial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            // Display the combined detail string
            Text(order.detail, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Vendor: ${order.vendor}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Fulfilled Qty: ${order.fulfilledQty}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Price: %.2f".format(order.price), style = MaterialTheme.typography.bodyMedium)
            // Display resolved name or UID if name not found
            Text("Updated By: ${order.updatedByName}", style = MaterialTheme.typography.bodyMedium)

            Spacer(Modifier.height(8.dp))
            Text("Orders Details:", style = MaterialTheme.typography.titleSmall)
            if (order.orders.isEmpty()) {
                Text(
                    " No specific client orders recorded.",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                order.orders.forEach { (clientKey, clientData) ->
                    if (order.isPartial) {
                        Text(
                            " Client: $clientKey, Original Quantity: $clientData",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        if (clientData is Map<*, *>) {
                            val quantity = clientData["quantity"]
                            val totalShare = clientData["totalShare"]
                            Text(
                                " Client: $clientKey, Quantity: $quantity, Share: %.2f".format(
                                    totalShare as? Double ?: 0.0
                                ), style = MaterialTheme.typography.bodySmall
                            )
                        } else if (clientData is Long || clientData is Int) {
                            Text(
                                " Client: $clientKey, Quantity: $clientData",
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                " Client: $clientKey, Unknown Data: $clientData",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrderDialog(
    order: LensOrderUi,
    onDismiss: () -> Unit,
    onSave: (LensOrderUi) -> Unit
) {
    var editedVendor by remember { mutableStateOf(order.vendor) }
    var editedPrice by remember { mutableStateOf(order.price.toString()) }
    var editedFulfilledQty by remember { mutableStateOf(order.fulfilledQty.toString()) }
    var priceError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Order (ID: ${order.firebaseKey.take(6)}...)") },
        text = {
            Column {
                OutlinedTextField(
                    value = editedVendor,
                    onValueChange = { editedVendor = it },
                    label = { Text("Vendor Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedPrice,
                    onValueChange = {
                        editedPrice = it
                        priceError = it.toDoubleOrNull() == null && it.isNotBlank()
                    },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = priceError,
                    trailingIcon = {
                        if (priceError) Icon(
                            Icons.Default.Warning,
                            "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (priceError) {
                    Text(
                        "Invalid price format. Please enter a number.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedFulfilledQty,
                    onValueChange = {
                        editedFulfilledQty = it
                        quantityError = it.toDoubleOrNull() == null && it.isNotBlank()
                    },
                    label = { Text("Fulfilled Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = quantityError,
                    enabled = order.isPartial, // Disable for completed orders
                    trailingIcon = {
                        if (quantityError) Icon(
                            Icons.Default.Warning,
                            "Error",
                            tint = MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!order.isPartial) {
                    Text(
                        "Quantity cannot be edited for completed orders.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                } else if (quantityError) {
                    Text(
                        "Invalid quantity format. Please enter an integer.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = editedPrice.toDoubleOrNull()
                    val fulfilledQty = editedFulfilledQty.toIntOrNull()
                    if (price != null && fulfilledQty != null) {
                        val updatedOrder = order.copy(
                            vendor = editedVendor,
                            price = price,
                            fulfilledQty = fulfilledQty.toDouble()
                        )
                        onSave(updatedOrder)
                    } else {
                        // Show a more specific error if needed
                        Log.e(
                            "EditOrderDialog",
                            "Validation failed: Price or Quantity is not valid."
                        )
                    }
                },
                enabled = !priceError && !quantityError && editedPrice.isNotBlank() && editedFulfilledQty.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/*@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditOrderDialog(
    order: LensOrderUi,
    onDismiss: () -> Unit,
    onSave: (LensOrderUi) -> Unit
) {
    var editedVendor by remember { mutableStateOf(order.vendor) }
    var editedPrice by remember { mutableStateOf(order.price.toString()) }
    // All quantities should be handled as Double, consistent with Firebase logic
    var editedFulfilledQty by remember { mutableStateOf(order.fulfilledQty.toDouble().toString()) }

    var priceError by remember { mutableStateOf(false) }
    var quantityError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Order") },
        text = {
            Column {
                OutlinedTextField(
                    value = editedVendor,
                    onValueChange = { editedVendor = it },
                    label = { Text("Vendor Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedPrice,
                    onValueChange = {
                        editedPrice = it
                        priceError = it.toDoubleOrNull() == null && it.isNotBlank()
                    },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = priceError,
                    modifier = Modifier.fillMaxWidth()
                )
                if (priceError) {
                    Text(
                        "Invalid price format",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = editedFulfilledQty,
                    onValueChange = {
                        // Allow for decimal input
                        if (it.matches(Regex("^\\d*\\.?\\d*$"))) {
                            editedFulfilledQty = it
                        }
                        quantityError = it.toDoubleOrNull() == null && it.isNotBlank()
                    },
                    label = { Text("Fulfilled Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    // --- CHANGE 1: This field is now always enabled ---
                    enabled = true,
                    isError = quantityError,
                    modifier = Modifier.fillMaxWidth()
                )
                // --- CHANGE 2: Removed the warning text for completed orders ---
                if (quantityError) {
                    Text(
                        "Invalid quantity format",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = editedPrice.toDoubleOrNull()
                    // --- CHANGE 3: Parse quantity as Double ---
                    val fulfilledQty: Double? = editedFulfilledQty.toDoubleOrNull()

                    if (price != null && fulfilledQty != null) {
                        // --- CHANGE 4: Copy the Double value ---
                        val updatedOrder = order.copy(
                            vendor = editedVendor,
                            price = price,
                            fulfilledQty = fulfilledQty
                        )
                        onSave(updatedOrder)
                    }
                },
                enabled = !priceError && !quantityError && editedPrice.isNotBlank() && editedFulfilledQty.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}*/

fun updateOrderInFirebase(
    context: Context,
    db: FirebaseDatabase,
    updatedOrder: LensOrderUi
) {
    val collectionRef = if (updatedOrder.isPartial) {
        db.getReference("PartiallyCompletedLensOrders")
    } else {
        db.getReference("CompletedLensOrders")
    }
    val orderRef = collectionRef.child(updatedOrder.firebaseKey)
    Log.d("FirebaseUpdate", "Attempting to update order: ${updatedOrder.firebaseKey} in ${collectionRef.key}")

    orderRef.get().addOnSuccessListener { originalSnap ->
        if (!originalSnap.exists()) {
            Log.e("FirebaseUpdate", "Original order not found,")
            Toast.makeText(context, "Error: Original order not found.", Toast.LENGTH_SHORT).show()
            return@addOnSuccessListener
        }

        val originalFulfilledQty = originalSnap.child("fulfilledQty").getValue(Long::class.java)?.toInt() ?: 0
        val originalPrice = originalSnap.child("price").getValue(Double::class.java) ?: 0.0
        val originalVendor = originalSnap.child("vendor").getValue(String::class.java) ?: ""
        val originalOrdersMap = originalSnap.child("orders").value as? Map<String, Any> ?: emptyMap()

        val updates = mutableMapOf<String, Any?>() // Changed to Any? for mixed types
        var needsUpdate = false
        var isQtyChanged = false

        if (updatedOrder.vendor != originalVendor) {
            updates["vendor"] = updatedOrder.vendor
            needsUpdate = true
        }
        if (updatedOrder.price != originalPrice) {
            updates["price"] = updatedOrder.price
            needsUpdate = true
        }
        if (updatedOrder.fulfilledQty != originalFulfilledQty.toDouble()) {
            isQtyChanged = true
        }

        if (updatedOrder.isPartial) {
            if (isQtyChanged) {
                val qtyDelta = updatedOrder.fulfilledQty - originalFulfilledQty
                Log.d("FirebaseUpdate", "Partial Qty changed. Delta: $qtyDelta. New Qty: ${updatedOrder.fulfilledQty}")

                val groupedKey = updatedOrder.groupedKey
                if (groupedKey == null) {
                    Log.e("FirebaseUpdate", "Grouped key is missing for partial order ${updatedOrder.firebaseKey}")
                    Toast.makeText(context, "Cannot update quantity: Grouped key missing.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val groupedRef = db.getReference("GroupedLensOrders").child(groupedKey)

                groupedRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val originalPartiallyAllottedQty = currentData.child("partiallyAllottedQty").getValue(Long::class.java)?.toInt() ?: 0
                        val ordersMap = currentData.child("orders").value as? Map<String, Long> ?: emptyMap()
                        val totalRequiredQty = ordersMap.values.sum().toInt()

                        val newPartiallyAllottedQty = originalPartiallyAllottedQty + qtyDelta

                        if (newPartiallyAllottedQty > totalRequiredQty) {
                            Log.w("FirebaseUpdate", "Validation failed: New fulfilled quantity (${updatedOrder.fulfilledQty}) would exceed total requirement ($totalRequiredQty) for grouped key $groupedKey.")

                            return Transaction.abort()
                        }

                        currentData.child("partiallyAllottedQty").value = newPartiallyAllottedQty

                        updates["fulfilledQty"] = updatedOrder.fulfilledQty

                        if (newPartiallyAllottedQty == totalRequiredQty.toDouble()) {
                          val completedRef = db.getReference("CompletedLensOrders").child(System.currentTimeMillis().toString())

                            val newPricePerUnit = if (newPartiallyAllottedQty > 0) updatedOrder.price / newPartiallyAllottedQty else 0.0
                            val enrichedOrders = ordersMap.mapValues { (clientName, clientQty) ->
                                val totalShare = newPricePerUnit * clientQty
                                mapOf("quantity" to clientQty, "totalShare" to totalShare)
                            }

                            // Create a new map to hold all properties for the completed order
                            val commonDataForCompleted = HashMap<String, Any?>()
                            updatedOrder.detailMap.forEach { (key, value) ->
                                commonDataForCompleted[key] = value // Add existing detail map items
                            }
                            commonDataForCompleted["price"] = updatedOrder.price
                            commonDataForCompleted["vendor"] = updatedOrder.vendor
                            commonDataForCompleted["updatedBy"] = updatedOrder.updatedById
                            commonDataForCompleted["timestamp"] = ServerValue.TIMESTAMP
                            commonDataForCompleted["fulfilledQty"] = updatedOrder.fulfilledQty
                            commonDataForCompleted["orders"] = enrichedOrders

                            completedRef.setValue(commonDataForCompleted)
                                .addOnSuccessListener {
                                    orderRef.removeValue()
                                        .addOnSuccessListener {
                                            groupedRef.removeValue()
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Order completed and moved successfully!", Toast.LENGTH_LONG).show()
                                                }
                                                .addOnFailureListener { e -> Log.e("FirebaseUpdate", "Failed to remove grouped order", e) }
                                        }
                                        .addOnFailureListener { e -> Log.e("FirebaseUpdate", "Failed to remove partial order", e) }
                                }
                                .addOnFailureListener { e -> Log.e("FirebaseUpdate", "Failed to write to CompletedLensOrders", e) }
                        } else {
                            orderRef.updateChildren(updates as Map<String, Any>)
                                .addOnSuccessListener {
                                    Log.d("FirebaseUpdate", "Partial order ${updatedOrder.firebaseKey} updated successfully.")
                                    Toast.makeText(context, "Order updated successfully!", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FirebaseUpdate", "Failed to update partial order", e)
                                    Toast.makeText(context, "Failed to update order: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                        }
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        if (error != null) {
                            Log.e("FirebaseUpdate", "Transaction failed: ${error.message}", error.toException())
                            Toast.makeText(context, "Update failed: ${error.message}", Toast.LENGTH_LONG).show()
                        } else if (!committed) {
                            Log.w("FirebaseUpdate", "Transaction aborted due to validation failure.")
                            Toast.makeText(context, "Update failed: Quantity exceeds total requirement.", Toast.LENGTH_LONG).show()
                        } else {
                            Log.d("FirebaseUpdate", "Transaction committed successfully.")
                        }
                    }
                })
            } else {
                if (needsUpdate) {
                    orderRef.updateChildren(updates as Map<String, Any>) // Cast to Map<String, Any> for updateChildren
                        .addOnSuccessListener {
                            Toast.makeText(context, "Order updated successfully!", Toast.LENGTH_SHORT).show()
                            Log.d("FirebaseUpdate", "Order ${updatedOrder.firebaseKey} updated successfully.")
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Failed to update order: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("FirebaseUpdate", "Error updating order ${updatedOrder.firebaseKey}", e)
                        }
                } else {
                    Log.d("FirebaseUpdate", "No changes detected for order ${updatedOrder.firebaseKey}.")
                }
            }
        } else {
            if (isQtyChanged) {
                // --- Business Logic for Completed Order Quantity Change ---
                Log.w("FirebaseUpdate", "Attempted to change fulfilledQty for a completed order. Operation denied.")
                Toast.makeText(context, "Quantity cannot be edited for completed orders.", Toast.LENGTH_LONG).show()
                return@addOnSuccessListener
            }

            if (updatedOrder.price != originalPrice) {
                val newPricePerUnit = if (originalFulfilledQty > 0) updatedOrder.price / originalFulfilledQty else 0.0
                val enrichedOrders = originalOrdersMap.mapValues { (clientName, clientData) ->
                    if (clientData is Map<*, *>) {
                        val quantity = clientData["quantity"] as? Long ?: 0
                        val totalShare = newPricePerUnit * quantity
                        mapOf("quantity" to quantity, "totalShare" to totalShare)
                    } else {
                        clientData // Keep original data if format is unexpected
                    }
                }
                updates["orders"] = enrichedOrders
                needsUpdate = true
            }

            if (needsUpdate) {
                orderRef.updateChildren(updates as Map<String, Any>) // Cast to Map<String, Any> for updateChildren
                    .addOnSuccessListener {
                        Toast.makeText(context, "Order updated successfully!", Toast.LENGTH_SHORT).show()
                        Log.d("FirebaseUpdate", "Completed order ${updatedOrder.firebaseKey} updated successfully.")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "Failed to update order: ${e.message}", Toast.LENGTH_LONG).show()
                        Log.e("FirebaseUpdate", "Error updating completed order ${updatedOrder.firebaseKey}", e)
                    }
            } else {
                Log.d("FirebaseUpdate", "No changes detected for order ${updatedOrder.firebaseKey}.")
            }
        }
    }.addOnFailureListener { e ->
        Log.e("FirebaseUpdate", "Failed to fetch original order data: ${e.message}", e)
        Toast.makeText(context, "Failed to retrieve order data for update.", Toast.LENGTH_SHORT).show()
    }
}

/*fun updateOrderInFirebase(
    context: Context,
    db: FirebaseDatabase,
    updatedOrder: LensOrderUi
) {
    val collectionRef = if (updatedOrder.isPartial) {
        db.getReference("PartiallyCompletedLensOrders")
    } else {
        db.getReference("CompletedLensOrders")
    }
    val orderRef = collectionRef.child(updatedOrder.firebaseKey)

    // First, get the current state of the order from Firebase
    orderRef.get().addOnSuccessListener { originalSnap ->
        if (!originalSnap.exists()) {
            Toast.makeText(context, "Error: Original order not found.", Toast.LENGTH_SHORT).show()
            Log.e("FirebaseUpdate", "Original order not found: ${updatedOrder.firebaseKey}")
            return@addOnSuccessListener
        }

        // --- PARSE ORIGINAL DATA ---
        val originalFulfilledQty =
            originalSnap.child("fulfilledQty").getValue(Double::class.java) ?: 0.0
        val originalPrice = originalSnap.child("price").getValue(Double::class.java) ?: 0.0
        val originalVendor = originalSnap.child("vendor").getValue(String::class.java) ?: ""
        val originalGroupedKey = originalSnap.child("groupedKey").getValue(String::class.java)

        val qtyDelta = updatedOrder.fulfilledQty - originalFulfilledQty

        // --- LOGIC FOR PARTIALLY COMPLETED ORDERS ---
        if (updatedOrder.isPartial) {
            if (originalGroupedKey == null) {
                Toast.makeText(
                    context,
                    "Error: GroupedKey missing for partial order.",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }

            // Run a transaction on the GroupedLensOrder to safely update its fulfilled count
            val groupedRef = db.getReference("GroupedLensOrders").child(originalGroupedKey)
            groupedRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    val currentPartiallyAllotted =
                        currentData.child("partiallyAllottedQty").getValue(Double::class.java)
                            ?: 0.0
                    val newPartiallyAllotted = currentPartiallyAllotted + qtyDelta

                    // Validation: Ensure we don't over-fulfill or go below zero
                    val totalRequired = (currentData.child("orders").value as? Map<*, *>)
                        ?.values?.sumOf { (it as Number).toDouble() } ?: 0.0

                    if (newPartiallyAllotted < 0 || newPartiallyAllotted > totalRequired + 0.001) {
                        Log.w("FirebaseUpdate", "Transaction aborted. Invalid quantity.")
                        return Transaction.abort()
                    }
                    currentData.child("partiallyAllottedQty").value = newPartiallyAllotted
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (error == null && committed) {
                        // If transaction is successful, now update the partial order itself
                        val updates = mapOf(
                            "fulfilledQty" to updatedOrder.fulfilledQty,
                            "price" to updatedOrder.price,
                            "vendor" to updatedOrder.vendor
                        )
                        orderRef.updateChildren(updates).addOnSuccessListener {
                            Toast.makeText(
                                context,
                                "Partial order updated successfully!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Update failed: ${error?.message ?: "Quantity validation failed"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            })
            return@addOnSuccessListener
        }

        // --- LOGIC FOR COMPLETED ORDERS ---
        if (qtyDelta > 0) {
            Toast.makeText(
                context,
                "Cannot increase quantity on a completed order.",
                Toast.LENGTH_LONG
            ).show()
            return@addOnSuccessListener
        }

        if (qtyDelta < 0) {
            // DECREASE SCENARIO: Re-create the GroupedLensOrder and update the CompletedLensOrder
            if (originalGroupedKey == null) {
                Toast.makeText(
                    context,
                    "Error: GroupedKey missing, cannot process return.",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }
            val originalInfoSnap = originalSnap.child("originalGroupedOrderInfo")
            val totalRequiredQty =
                originalInfoSnap.child("totalRequiredQty").getValue(Double::class.java) ?: 0.0
            val originalClientBreakdown =
                originalInfoSnap.child("originalClientBreakdown").value as? Map<String, Double>
                    ?: emptyMap()

            if (totalRequiredQty == 0.0 || originalClientBreakdown.isEmpty()) {
                Toast.makeText(
                    context,
                    "Error: Original order backup data is missing.",
                    Toast.LENGTH_LONG
                ).show()
                return@addOnSuccessListener
            }

            // 1. Prepare the re-created GroupedLensOrder data
            val newGroupedOrderData = updatedOrder.detailMap.toMutableMap()
            newGroupedOrderData["orders"] = originalClientBreakdown
            newGroupedOrderData["partiallyAllottedQty"] =
                totalRequiredQty - updatedOrder.fulfilledQty // The crucial calculation

            // 2. Prepare the updated CompletedLensOrder data
            val updatedPricePerUnit =
                if (updatedOrder.fulfilledQty > 0) updatedOrder.price / updatedOrder.fulfilledQty else 0.0
            val updatedEnrichedOrders =
                (updatedOrder.orders as Map<String, Map<String, Number>>).mapValues { (_, data) ->
                    val quantity = data["quantity"]?.toDouble() ?: 0.0
                    mapOf("quantity" to quantity, "totalShare" to updatedPricePerUnit * quantity)
                }
            val completedOrderUpdates = mapOf(
                "fulfilledQty" to updatedOrder.fulfilledQty,
                "price" to updatedOrder.price,
                "vendor" to updatedOrder.vendor,
                "orders" to updatedEnrichedOrders
            )

            // 3. Perform an atomic multi-path update
            val rootUpdates = mapOf(
                "GroupedLensOrders/$originalGroupedKey" to newGroupedOrderData,
                "CompletedLensOrders/${updatedOrder.firebaseKey}" to originalSnap.value // Start with original data
            )
            val finalUpdates = rootUpdates.toMutableMap()
            // Layer the specific updates for the completed order on top
            completedOrderUpdates.forEach { (key, value) ->
                finalUpdates["CompletedLensOrders/${updatedOrder.firebaseKey}/$key"] = value
            }

            db.reference.updateChildren(finalUpdates)
                .addOnSuccessListener {
                    Toast.makeText(
                        context,
                        "Order updated and requirement re-opened.",
                        Toast.LENGTH_LONG
                    ).show()
                }.addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }

        } else { // NO QUANTITY CHANGE SCENARIO
            val updates = mutableMapOf<String, Any>()
            if (updatedOrder.price != originalPrice) {
                updates["price"] = updatedOrder.price
                // Recalculate price distribution if price changes
                val newPricePerUnit =
                    if (originalFulfilledQty > 0) updatedOrder.price / originalFulfilledQty else 0.0
                val enrichedOrders =
                    (updatedOrder.orders as Map<String, Map<String, Number>>).mapValues { (_, data) ->
                        val quantity = data["quantity"]?.toDouble() ?: 0.0
                        mapOf("quantity" to quantity, "totalShare" to newPricePerUnit * quantity)
                    }
                updates["orders"] = enrichedOrders
            }
            if (updatedOrder.vendor != originalVendor) {
                updates["vendor"] = updatedOrder.vendor
            }

            if (updates.isNotEmpty()) {
                orderRef.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(
                        context,
                        "Order details updated successfully!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(context, "No changes detected.", Toast.LENGTH_SHORT).show()
            }
        }

    }.addOnFailureListener { e ->
        Toast.makeText(context, "Failed to retrieve order data: ${e.message}", Toast.LENGTH_LONG)
            .show()
        Log.e("FirebaseUpdate", "Failed to get original order", e)
    }
}*/

/*private fun parseLensOrder(
    orderSnap: DataSnapshot,
    isPartial: Boolean,
    userNamesMap: Map<String, String>
): LensOrderUi? {
    val key = orderSnap.key
    Log.d("ParseLensOrder", "Attempting to parse order: $key (isPartial: $isPartial)")
    if (key == null) {
        Log.w("ParseLensOrder", "Order snapshot key is null, cannot parse.")
        return null
    }
    try {
        // Raw details map - now using Any? to hold all types
        val detailMap = mutableMapOf<String, Any?>()
        detailMap["type"] = orderSnap.child("type").getValue(String::class.java)
        detailMap["coating"] = orderSnap.child("coating").getValue(String::class.java)
        detailMap["coatingType"] = orderSnap.child("coatingType").getValue(String::class.java)
        detailMap["material"] = orderSnap.child("material").getValue(String::class.java)
        detailMap["sphere"] = orderSnap.child("sphere").getValue(String::class.java)
        detailMap["cylinder"] = orderSnap.child("cylinder").getValue(String::class.java)
        detailMap["axis"] = orderSnap.child("axis").getValue(String::class.java)
        detailMap["add"] = orderSnap.child("add").getValue(String::class.java)
        detailMap["lensSpecificType"] = orderSnap.child("lensSpecificType").getValue(String::class.java)

        val fulfilledQty = orderSnap.child("fulfilledQty").getValue(Long::class.java)?.toDouble()
        val price = orderSnap.child("price").getValue(Double::class.java)
        val vendor = orderSnap.child("vendor").getValue(String::class.java)
        val updatedById = orderSnap.child("updatedBy").getValue(String::class.java)
        val timestamp = orderSnap.child("timestamp").getValue(Long::class.java)

        // Null checks for essential fields
        if (detailMap["type"] == null || detailMap["coating"] == null || detailMap["coatingType"] == null || detailMap["material"] == null ||
            detailMap["sphere"] == null || detailMap["cylinder"] == null || fulfilledQty == null || price == null ||
            vendor == null || updatedById == null || timestamp == null
        ) {
            Log.e(
                "ParseLensOrder", "Missing essential data for order $key. Data: " +
                        "Type=${detailMap["type"]}, Coating=${detailMap["coating"]}, FulfilledQty=$fulfilledQty, Price=$price, " +
                        "Vendor=$vendor, UpdatedBy=$updatedById, Timestamp=$timestamp"
            )
            return null
        }

        val updatedByName = userNamesMap[updatedById] ?: updatedById // Use UID if name not found
        if (updatedByName == updatedById) {
            Log.w(
                "ParseLensOrder",
                "User name not found for UID: $updatedById for order $key. Displaying UID."
            )
        } else {
            Log.d(
                "ParseLensOrder",
                "Resolved UID $updatedById to name '$updatedByName' for order $key."
            )
        }

        // Handling the 'orders' node
        val ordersMap = mutableMapOf<String, Any>()
        val ordersSnapshot = orderSnap.child("orders")
        if (!ordersSnapshot.exists()) {
            Log.w("ParseLensOrder", "No 'orders' node found for $key.")
        } else {
            ordersSnapshot.children.forEach { orderChild ->
                val clientKey = orderChild.key
                if (clientKey == null) {
                    Log.w("ParseLensOrder", "Null client key in orders for $key. Skipping.")
                    return@forEach
                }
                if (isPartial) {
                    val quantity = orderChild.getValue(Long::class.java)
                    if (quantity != null) {
                        ordersMap[clientKey] = quantity
                    } else {
                        Log.w(
                            "ParseLensOrder",
                            "Partial order $key: Client $clientKey has null quantity."
                        )
                    }
                } else {
                    val clientData = orderChild.value
                    if (clientData is Map<*, *>) {
                        ordersMap[clientKey] = clientData
                    } else if (clientData is Long || clientData is Int) {
                        ordersMap[clientKey] = clientData
                    } else {
                        Log.w(
                            "ParseLensOrder",
                            "Completed order $key: Client $clientKey has unexpected data type: ${clientData?.javaClass?.name}. Value: $clientData"
                        )
                    }
                }
            }
        }

        val detail = getLensDetailString(
            type = detailMap["type"] as? String ?: "Unknown",
            coat = detailMap["coating"] as? String ?: "",
            coatT = detailMap["coatingType"] as? String ?: "",
            mat = detailMap["material"] as? String ?: "",
            sph = detailMap["sphere"] as? String ?: "0.00",
            cyl = detailMap["cylinder"] as? String ?: "0.00",
            ax = detailMap["axis"] as? String ?: "",
            add = detailMap["add"] as? String ?: "",
            spec = detailMap["lensSpecificType"] as? String ?: ""
        )

        // Generate the grouped key for the detail map
        val groupedKey = getGroupedOrderKey(detailMap)

        val parsedOrder = LensOrderUi(
            firebaseKey = key,
            detail = detail, // Assign the newly generated detail string
            fulfilledQty = fulfilledQty,
            price = price,
            vendor = vendor,
            updatedById = updatedById,
            updatedByName = updatedByName,
            timestamp = timestamp,
            orders = ordersMap,
            isPartial = isPartial,
            groupedKey = groupedKey,
            detailMap = detailMap // Pass the new detailMap
        )
        Log.d(
            "ParseLensOrder",
            "Successfully parsed order: $key. Is Partial: $isPartial, FulfilledQty: $fulfilledQty, Updated By: $updatedByName ($updatedById)"
        )
        return parsedOrder
    } catch (e: Exception) {
        Log.e("ParseLensOrder", "Critical error during parsing order $key: ${e.message}", e)
        return null
    }
}*/

private fun parseLensOrder(
    orderSnap: DataSnapshot,
    isPartial: Boolean,
    userNamesMap: Map<String, String>
): LensOrderUi? {
    val key = orderSnap.key ?: return null

    Log.e("ParseLensOrder", "UNEXPECTED EXCEPTION for key ")

    try {
        val fulfilledQty = (orderSnap.child("fulfilledQty").value as? Number)?.toDouble()
        val price = (orderSnap.child("price").value as? Number)?.toDouble()
        val updatedById = orderSnap.child("updatedBy").getValue(String::class.java)
        val timestamp = (orderSnap.child("timestamp").value as? Number)?.toLong()

        if (fulfilledQty == null || price == null || updatedById == null || timestamp == null) {
            Log.e(
                "ParseLensOrder",
                "FATAL for key '$key': A core field is missing or has the wrong type."
            )
            Log.e("ParseLensOrder", "--> fulfilledQty: [${orderSnap.child("fulfilledQty").value}]")
            Log.e("ParseLensOrder", "--> price: [${orderSnap.child("price").value}]")
            Log.e("ParseLensOrder", "--> updatedById: [${orderSnap.child("updatedBy").value}]")
            Log.e("ParseLensOrder", "--> timestamp: [${orderSnap.child("timestamp").value}]")
            return null
        }

        val detailMap = mutableMapOf<String, Any?>()
        detailMap["type"] = orderSnap.child("type").getValue(String::class.java) ?: "N/A"
        detailMap["coating"] = orderSnap.child("coating").getValue(String::class.java) ?: "N/A"
        detailMap["coatingType"] =
            orderSnap.child("coatingType").getValue(String::class.java) ?: "N/A"
        detailMap["material"] = orderSnap.child("material").getValue(String::class.java) ?: "N/A"
        detailMap["sphere"] = orderSnap.child("sphere").getValue(String::class.java) ?: "0.00"
        detailMap["cylinder"] = orderSnap.child("cylinder").getValue(String::class.java) ?: "0.00"
        // These fields were causing the crash because they don't always exist.
        detailMap["axis"] = orderSnap.child("axis").getValue(String::class.java) ?: ""
        detailMap["add"] = orderSnap.child("add").getValue(String::class.java) ?: ""
        detailMap["lensSpecificType"] =
            orderSnap.child("lensSpecificType").getValue(String::class.java) ?: ""

        val vendor = orderSnap.child("vendor").getValue(String::class.java) ?: "Unknown"
        val updatedByName = userNamesMap[updatedById] ?: updatedById // Fallback to UID

        // --- Step 3: Parse the 'orders' map ---
        val ordersMap = mutableMapOf<String, Any>()
        if (orderSnap.hasChild("orders")) {
            orderSnap.child("orders").children.forEach { orderChild ->
                val clientKey = orderChild.key ?: return@forEach
                orderChild.value?.let { ordersMap[clientKey] = it }
            }
        }

        val detail = getLensDetailString(
            type = detailMap["type"] as String,
            coat = detailMap["coating"] as String,
            coatT = detailMap["coatingType"] as String,
            mat = detailMap["material"] as String,
            sph = detailMap["sphere"] as String,
            cyl = detailMap["cylinder"] as String,
            ax = detailMap["axis"] as String,
            add = detailMap["add"] as String,
            spec = detailMap["lensSpecificType"] as String
        )

        // --- Step 4: Build the final object ---
        return LensOrderUi(
            firebaseKey = key,
            detail = detail,
            fulfilledQty = fulfilledQty,
            price = price,
            vendor = vendor,
            updatedById = updatedById,
            updatedByName = updatedByName,
            timestamp = timestamp,
            orders = ordersMap,
            isPartial = isPartial,
            groupedKey = orderSnap.child("groupedKey").getValue(String::class.java),
            detailMap = detailMap
        )
    } catch (e: Exception) {
        // This catch block should now only trigger for unexpected errors.
        Log.e("ParseLensOrder", "UNEXPECTED EXCEPTION for key '$key': ${e.message}", e)
        return null
    }
}


private fun getGroupedOrderKey(details: Map<String, Any?>): String {
    val lensType = details["type"] as? String ?: ""
    val coating = details["coating"] as? String ?: ""
    val coatingType = details["coatingType"] as? String ?: ""
    val material = details["material"] as? String ?: ""
    val sphere = details["sphere"] as? String ?: ""
    val cylinder = details["cylinder"] as? String ?: ""
    val axis = details["axis"] as? String ?: ""
    val add = details["add"] as? String ?: ""
    val lensSpecificType = details["lensSpecificType"] as? String ?: "" // For Progressive

    val baseKey = buildString {
        append("$lensType-$coating-$coatingType-$material-$sphere-$cylinder")
        if (lensType != "SingleVision") {
            append("-$axis-$add")
        }
        if (lensType == "Progressive") {
            append("-$lensSpecificType")
        }
    }

    return baseKey.replace(".", "_")
}