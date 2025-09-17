package com.nusync

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import com.nusync.ui.theme.NuSyncTheme
import com.nusync.utils.formatCoating
import com.nusync.utils.formatCoatingType
import com.nusync.utils.formatMaterial
import com.nusync.utils.getLensDetailString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PartiallyCompletedOrderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                TopBar(heading = "Partial Orders") {
                    PartiallyCompletedOrdersScreen()
                }
            }
        }
    }
}

data class PartialOrderUi(
    val firebaseKey: String,
    val detail: String,
    val clients: String,
    val fulfilledQty: Double,
    val price: Double,
    val vendor: String,
    val updatedById: String,
    var updatedByName: String = "",
    val timestamp: Long,
    val groupedOrderKey: String? = null,
    val type: String,
    val coating: String,
    val coatingType: String,
    val material: String,
    val sphere: String,
    val cylinder: String,
    val axis: String,
    val add: String,
    val lensSpec: String
)

data class ClientAssignment(
    val clientName: String,
    var assignedQty: MutableState<String> = mutableStateOf(""),
    val requiredQty: Double? = null
)


@Composable
fun PartiallyCompletedOrdersScreen() {
    val context = LocalContext.current
    val db = remember { FirebaseDatabase.getInstance() }
    val partialOrdersRef = remember { db.getReference("PartiallyCompletedLensOrders") }
    val usersRef = remember { db.getReference("Users") }
    val groupedOrdersRef = remember { db.getReference("GroupedLensOrders") }
    val completedOrdersRef = remember { db.getReference("CompletedLensOrders") }

    val orders = remember { mutableStateListOf<PartialOrderUi>() }
    val userNameCache = remember { mutableStateMapOf<String, String>() }
    val requiredQuantitiesCache = remember { mutableStateMapOf<String, Map<String, Double>>() }

    var activeOrder by remember { mutableStateOf<PartialOrderUi?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedOrders = snapshot.children.mapNotNull { snap ->
                    val key = snap.key ?: return@mapNotNull null
                    val price = snap.child("price").getValue(Double::class.java) ?: 0.0
                    val vendor = snap.child("vendor").getValue(String::class.java) ?: "-"
                    val updatedBy = snap.child("updatedBy").getValue(String::class.java) ?: "-"
                    val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: 0L
                    val fulfilledQty =
                        snap.child("fulfilledQty").getValue(Double::class.java) ?: 0.0

                    val ordersNode = snap.child("orders")
                    val clients = ordersNode.children.joinToString(", ") { it.key ?: "" }

                    val type = snap.child("type").getValue(String::class.java) ?: "Unknown"
                    val coating = snap.child("coating").getValue(String::class.java) ?: "-"
                    val coatingType = snap.child("coatingType").getValue(String::class.java) ?: "-"
                    val material = snap.child("material").getValue(String::class.java) ?: "-"
                    val sphere = snap.child("sphere").getValue(String::class.java) ?: "0.00"
                    val cylinder = snap.child("cylinder").getValue(String::class.java) ?: "0.00"
                    val axis = snap.child("axis").getValue(String::class.java) ?: ""
                    val add = snap.child("add").getValue(String::class.java) ?: ""
                    val lensSpec = snap.child("lensSpecificType").getValue(String::class.java) ?: ""

                    val detail = getLensDetailString(
                        type,
                        coating,
                        coatingType,
                        material,
                        sphere,
                        cylinder,
                        axis,
                        add,
                        lensSpec
                    )

                    val groupedOrderKey = when (type) {
                        "SingleVision" -> "SingleVision-${coating}-${coatingType}-${material}-${
                            sphere.replace(
                                ".",
                                "_"
                            )
                        }-${cylinder.replace(".", "_")}"

                        "Kryptok" -> "Kryptok-${coating}-${coatingType}-${material}-${
                            sphere.replace(
                                ".",
                                "_"
                            )
                        }-${cylinder.replace(".", "_")}-${axis}-${add.replace(".", "_")}"

                        "Progressive" -> "Progressive-${coating}-${coatingType}-${material}-${
                            sphere.replace(
                                ".",
                                "_"
                            )
                        }-${cylinder.replace(".", "_")}-${axis}-${add.replace(".", "_")}"

                        else -> null
                    }


                    PartialOrderUi(
                        firebaseKey = key,
                        detail = detail,
                        clients = clients,
                        fulfilledQty = fulfilledQty,
                        price = price,
                        vendor = vendor,
                        updatedById = updatedBy,
                        timestamp = timestamp,
                        groupedOrderKey = groupedOrderKey,
                        type = type,
                        coating = coating,
                        coatingType = coatingType,
                        material = material,
                        sphere = sphere,
                        cylinder = cylinder,
                        axis = axis,
                        add = add,
                        lensSpec = lensSpec
                    )
                }

                orders.clear()
                orders.addAll(fetchedOrders.sortedByDescending { it.timestamp })

                val uniqueUids = orders.map { it.updatedById }.toSet()
                uniqueUids.forEach { uid ->
                    if (uid !in userNameCache) {
                        usersRef.child(uid).get().addOnSuccessListener { userSnap ->
                            val name =
                                userSnap.child("name").getValue(String::class.java) ?: "Unknown"
                            userNameCache[uid] = name
                            val ordersToUpdate = orders.filter { it.updatedById == uid }
                            val tempOrders = orders.toMutableList()
                            ordersToUpdate.forEach { order ->
                                val index = tempOrders.indexOf(order)
                                if (index != -1) {
                                    tempOrders[index] =
                                        order.copy(updatedByName = name)
                                }
                            }
                            orders.clear()
                            orders.addAll(tempOrders)
                        }.addOnFailureListener {
                            Log.e("DB", "Failed to fetch user name for $uid", it)
                            userNameCache[uid] = "Error"
                        }
                    }
                }

                orders.forEach { order ->
                    order.groupedOrderKey?.let { groupedKey ->
                        if (groupedKey !in requiredQuantitiesCache) {
                            groupedOrdersRef.child(groupedKey).child("orders").get()
                                .addOnSuccessListener { groupedSnap ->
                                    val quantities = groupedSnap.children.associate {
                                        it.key.toString() to (it.getValue(Double::class.java)
                                            ?: 0.0)
                                    }
                                    requiredQuantitiesCache[groupedKey] = quantities
                                }.addOnFailureListener {
                                    Log.e(
                                        "DB",
                                        "Failed to fetch grouped order quantities for $groupedKey",
                                        it
                                    )
                                }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DB", "Partial fetch failed", error.toException())
            }
        }

        partialOrdersRef.addValueEventListener(listener)
        onDispose { partialOrdersRef.removeEventListener(listener) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(orders, key = { it.firebaseKey }) { order ->
            order.updatedByName = userNameCache[order.updatedById] ?: "Loading..."
            PartialOrderItem(order) {
                activeOrder = order
                showDialog = true
            }
        }
    }

    if (showDialog && activeOrder != null) {
        val requiredQuantities = activeOrder!!.groupedOrderKey?.let {
            requiredQuantitiesCache[it]
        } ?: emptyMap()

        ClientAssignmentDialog(
            order = activeOrder!!,
            requiredQuantities = requiredQuantities,
            onDismiss = { showDialog = false },
            onSubmit = { assignments ->
                val totalAssigned =
                    assignments.sumOf { it.assignedQty.value.toDoubleOrNull() ?: 0.0 }
                if (totalAssigned != activeOrder!!.fulfilledQty) {
                    Toast.makeText(
                        context,
                        "Total assigned quantity must equal fulfilled quantity (${activeOrder!!.fulfilledQty})",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val assignedQuantities = assignments.associate {
                        it.clientName to (it.assignedQty.value.toDoubleOrNull() ?: 0.0)
                    }

                    val orderToSave = activeOrder!!
                    val timestampKey = System.currentTimeMillis().toString()
                    val pricePerUnit =
                        if (orderToSave.fulfilledQty > 0) orderToSave.price / orderToSave.fulfilledQty else 0.0

                    val enrichedOrders = assignedQuantities.filter { it.value > 0 }
                        .mapValues { (clientName, assignedQty) ->
                            val totalShare = pricePerUnit * assignedQty
                            mapOf("quantity" to assignedQty, "totalShare" to totalShare)
                        }

                    val allocationData = mutableMapOf<String, Any>(
                        "type" to orderToSave.type,
                        "coating" to orderToSave.coating,
                        "coatingType" to orderToSave.coatingType,
                        "material" to orderToSave.material,
                        "sphere" to orderToSave.sphere,
                        "cylinder" to orderToSave.cylinder,
                        "axis" to orderToSave.axis,
                        "add" to orderToSave.add,
                        "lensSpecificType" to orderToSave.lensSpec,
                        "price" to orderToSave.price,
                        "vendor" to orderToSave.vendor,
                        "timestamp" to ServerValue.TIMESTAMP,
                        "fulfilledQty" to orderToSave.fulfilledQty,
                        "orders" to enrichedOrders
                    )

                    val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                    val currentUid = auth.currentUser?.uid ?: "unknown"
                    allocationData["updatedBy"] = currentUid


                    completedOrdersRef.child(timestampKey).setValue(allocationData)
                        .addOnSuccessListener {
                            Log.d("DB", "Allocation saved successfully for key: $timestampKey")
                            Toast.makeText(context, "Allocation saved!", Toast.LENGTH_SHORT).show()

                            if (orderToSave.groupedOrderKey != null) {
                                assignedQuantities.forEach { (clientName, assignedQty) ->
                                    val currentQtyRef =
                                        groupedOrdersRef.child(orderToSave.groupedOrderKey!!)
                                            .child("orders").child(clientName)

                                    currentQtyRef.runTransaction(object : Transaction.Handler {
                                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                            val currentQty =
                                                mutableData.getValue(Double::class.java) ?: 0.0
                                            val newQty = currentQty - assignedQty

                                            if (newQty < 0) {
                                                Log.w(
                                                    "DB",
                                                    "Attempted to deduct more than available. Aborting transaction for $clientName."
                                                )
                                                return Transaction.abort()
                                            }

                                            mutableData.value = newQty
                                            return Transaction.success(mutableData)
                                        }

                                        override fun onComplete(
                                            databaseError: DatabaseError?,
                                            committed: Boolean,
                                            currentData: DataSnapshot?
                                        ) {
                                            if (databaseError != null) {
                                                Log.e(
                                                    "DB",
                                                    "Transaction failed for $clientName",
                                                    databaseError.toException()
                                                )
                                            }
                                        }
                                    })
                                }
                            }

                            partialOrdersRef.child(orderToSave.firebaseKey).removeValue()
                                .addOnSuccessListener {
                                    Log.d(
                                        "DB",
                                        "Partial record removed for key: ${orderToSave.firebaseKey}"
                                    )
                                }
                                .addOnFailureListener {
                                    Log.e(
                                        "DB",
                                        "Failed to remove partial record",
                                        it
                                    )
                                }
                        }
                        .addOnFailureListener {
                            Log.e("DB", "Failed to save allocation", it)
                            Toast.makeText(
                                context,
                                "Failed to save allocation.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                    showDialog = false
                }
            }
        )
    }
}

@Composable
fun PartialOrderItem(order: PartialOrderUi, onClick: () -> Unit) {
    val dateTime = remember(order.timestamp) {
        val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        formatter.format(Date(order.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "${order.fulfilledQty} P ${order.detail}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Clients: ${order.clients}", style = MaterialTheme.typography.bodySmall)
            Text(
                "Price: ₹${order.price}, Vendor: ${order.vendor}",
                style = MaterialTheme.typography.bodySmall
            )
            Text("Updated by: ${order.updatedByName}", style = MaterialTheme.typography.bodySmall)
            Text("On: $dateTime", style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun ClientAssignmentDialog(
    order: PartialOrderUi,
    requiredQuantities: Map<String, Double>,
    onDismiss: () -> Unit,
    onSubmit: (List<ClientAssignment>) -> Unit
) {
    val assignments = remember(order.clients, requiredQuantities) {
        order.clients.split(",").map { client ->
            val clientName = client.trim()
            ClientAssignment(
                clientName = clientName,
                assignedQty = mutableStateOf(""),
                requiredQty = requiredQuantities[clientName]
            )
        }.toMutableStateList()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = { onSubmit(assignments) }) {
                Text("Submit")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        text = {
            Column {
                assignments.forEach { assignment ->
                    val labelText = if (assignment.requiredQty != null) {
                        "Qty for ${assignment.clientName} (Required: %.2f)".format(assignment.requiredQty)
                    } else {
                        "Qty for ${assignment.clientName}"
                    }
                    OutlinedTextField(
                        value = assignment.assignedQty.value,
                        onValueChange = { assignment.assignedQty.value = it },
                        label = { Text(labelText) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    )
}