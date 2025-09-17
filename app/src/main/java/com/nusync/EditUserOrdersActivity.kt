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
    val detail: String,
    val fulfilledQty: Double,
    val price: Double,
    val vendor: String,
    val updatedById: String,
    val updatedByName: String,
    val timestamp: Long,
    val orders: Map<String, Any>,
    val isPartial: Boolean,
    val groupedKey: String? = null,
    val detailMap: Map<String, Any?>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditUserOrdersScreen(currentUid: String) {
    val context = LocalContext.current
    val db = remember { FirebaseDatabase.getInstance() }

    var allCompletedOrders by remember { mutableStateOf(emptyList<LensOrderUi>()) }
    var allPartialOrders by remember { mutableStateOf(emptyList<LensOrderUi>()) }
    var userNamesMap by remember { mutableStateOf<Map<String, String>?>(null) }

    val isLoading = userNamesMap == null

    val showEditDialog = remember { mutableStateOf(false) }
    val editingOrder = remember { mutableStateOf<LensOrderUi?>(null) }

    DisposableEffect(currentUid) {
        val usersRef = db.getReference("Users")
        val completedRef = db.getReference("CompletedLensOrders")
        val partialRef = db.getReference("PartiallyCompletedLensOrders")

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
                userNamesMap = users
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("LiveUpdateDebug", "Failed to load users.", error.toException())
                userNamesMap = emptyMap()
            }
        }

        val completedListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("LiveUpdateDebug", "Completed orders data updated.")
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

        usersRef.addValueEventListener(usersListener)
        completedRef.addValueEventListener(completedListener)
        partialRef.addValueEventListener(partialListener)

        onDispose {
            Log.d("LiveUpdateDebug", "Removing all listeners.")
            usersRef.removeEventListener(usersListener)
            completedRef.removeEventListener(completedListener)
            partialRef.removeEventListener(partialListener)
        }
    }

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
            .clickable { onClick(order) },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(
                text = if (order.isPartial) "PARTIAL ORDER" else "COMPLETED ORDER",
                style = MaterialTheme.typography.labelSmall,
                color = if (order.isPartial) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )
            Text(order.detail, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Vendor: ${order.vendor}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Fulfilled Qty: ${order.fulfilledQty}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Price: %.2f".format(order.price), style = MaterialTheme.typography.bodyMedium)
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
                    enabled = order.isPartial,
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

        val updates = mutableMapOf<String, Any?>()
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

                            val commonDataForCompleted = HashMap<String, Any?>()
                            updatedOrder.detailMap.forEach { (key, value) ->
                                commonDataForCompleted[key] = value
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
                    orderRef.updateChildren(updates as Map<String, Any>)
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
                        clientData
                    }
                }
                updates["orders"] = enrichedOrders
                needsUpdate = true
            }

            if (needsUpdate) {
                orderRef.updateChildren(updates as Map<String, Any>)
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
        detailMap["axis"] = orderSnap.child("axis").getValue(String::class.java) ?: ""
        detailMap["add"] = orderSnap.child("add").getValue(String::class.java) ?: ""
        detailMap["lensSpecificType"] =
            orderSnap.child("lensSpecificType").getValue(String::class.java) ?: ""

        val vendor = orderSnap.child("vendor").getValue(String::class.java) ?: "Unknown"
        val updatedByName = userNamesMap[updatedById] ?: updatedById

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
    val lensSpecificType = details["lensSpecificType"] as? String ?: ""

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