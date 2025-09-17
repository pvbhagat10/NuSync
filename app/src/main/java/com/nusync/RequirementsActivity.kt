package com.nusync

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.nusync.ui.theme.Blue2
import com.nusync.ui.theme.NuSyncTheme
import com.nusync.utils.NotificationHelper
import com.nusync.utils.formatCoating
import com.nusync.utils.formatCoatingType
import com.nusync.utils.formatMaterial
import com.nusync.utils.getLensDetailString
import kotlin.collections.addAll
import kotlin.collections.getValue
import kotlin.text.clear
import kotlin.text.get

data class RequirementUi(
    val firebaseKey: String,
    val detail: String,
    val partyNames: String,
    val totalQty: Double, // Changed from Int to Double
    val comment: String = "",
    val updatedBy: String = ""
)

class RequirementsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                TopBar(heading = "Inventory") {
                    RequirementsScreen()
                }
            }
        }
    }
}


@Composable
fun RequirementsScreen() {
    val context = LocalContext.current
    val db = remember { FirebaseDatabase.getInstance() }
    val groupedRef = remember { db.getReference("GroupedLensOrders") }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUid = auth.currentUser?.uid ?: "unknown"
    var userName by remember { mutableStateOf("Unknown User") }
    val requirements = remember { mutableStateListOf<RequirementUi>() }

    var activeReq by remember { mutableStateOf<RequirementUi?>(null) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var userRole by remember { mutableStateOf("User") } // Default role

    LaunchedEffect(currentUid) {
        if (currentUid != "unknown") {
            db.getReference("Users").child(currentUid).get()
                .addOnSuccessListener { snapshot ->
                    userRole = snapshot.child("role").getValue(String::class.java) ?: "User"
                    userName = snapshot.child("name").getValue(String::class.java)
                        ?: "Unknown User"
                    Log.d(
                        "RequirementsScreen",
                        "User role fetched: $userRole, Name: $userName for UID: $currentUid"
                    )
                }
                .addOnFailureListener {
                    Log.e("RequirementsScreen", "Failed to fetch user role/name", it)
                    userRole = "User"
                    userName = "Unknown User"
                }
        }
    }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { lensSnap ->
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
                    val commentNode = lensSnap.child("comment")
                    val commentText =
                        commentNode.child("text").getValue(String::class.java) ?: ""
                    val commentBy =
                        commentNode.child("updatedBy").getValue(String::class.java) ?: ""
                    val orders = lensSnap.child("orders").value as? Map<*, *>
                    val parties = orders?.keys?.joinToString(", ") ?: "Unknown"

                    val totalNoOfLenseRequiredInOrder =
                        orders?.values?.filterIsInstance<Number>()?.sumOf { it.toDouble() } ?: 0.0

                    val partiallyAllotedLenses =
                        (lensSnap.child("partiallyAllottedQty").value as? Number)?.toDouble() ?: 0.0
                    val currentDisplayQty =
                        totalNoOfLenseRequiredInOrder - partiallyAllotedLenses

                    if (currentDisplayQty <= 0.0) {
                        return@mapNotNull null
                    }

                    Log.i("getLensDetailString supply", "getLensDetailString($type, $coat, $coatT, $mat, $sph, $cyl, $ax, $add, $spec)")
                    val detail = getLensDetailString(type, coat, coatT, mat, sph, cyl, ax, add, spec)
                    Log.i("getLensDetailString", detail)


                    Log.d(
                        "RequirementsScreen",
                        "FirebaseKey: ${lensSnap.key}, Original Total: $totalNoOfLenseRequiredInOrder, Allotted: $partiallyAllotedLenses, Display Qty: $currentDisplayQty"
                    )

                    RequirementUi(
                        lensSnap.key!!,
                        "%.2f P $detail".format(currentDisplayQty), // Format for display
                        parties,
                        currentDisplayQty, // Pass the calculated remaining quantity to RequirementUi
                        commentText,
                        commentBy
                    )
                }
                requirements.clear()
                requirements.addAll(list)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DB", "listen failed", error.toException())
            }
        }
        groupedRef.addValueEventListener(listener)
        onDispose { groupedRef.removeEventListener(listener) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        items(requirements, key = { it.firebaseKey }) { req ->
            InventoryItem(
                detail = req.detail,
                partyNames = req.partyNames,
                comment = req.comment,
                updatedBy = req.updatedBy,
                userRole = userRole,
                onClick = { activeReq = req; showActionDialog = true },
                onDeleteClick = {
                    activeReq = req
                    showDeleteConfirmationDialog = true
                },
                onEditClick = {
                    val intent = Intent(context, EditLensOrderDetailsActivity::class.java).apply {
                        putExtra("firebaseKey", req.firebaseKey)
                        putExtra("commentText", req.comment)
                    }
                    context.startActivity(intent)
                }
            )
        }
    }

    if (showActionDialog && activeReq != null) {
        ActionDialog(
            req = activeReq!!,
            maxQty = activeReq!!.totalQty,
            onDismiss = { showActionDialog = false },
            onFulfil = { price, qty, vendor ->
                handleFulfil(context, db, currentUid, activeReq!!, price, qty, vendor, userName)
                showActionDialog = false
            },
            onComment = { comment ->
                handleComment(context, db, currentUid, activeReq!!.firebaseKey, comment, userName, activeReq!!.detail)
                showActionDialog = false
            }
        )
    }

    if (showDeleteConfirmationDialog && activeReq != null) {
        DeleteConfirmationDialog(
            req = activeReq!!,
            onDismiss = { showDeleteConfirmationDialog = false },
            onConfirmDelete = { firebaseKey ->
                handleDelete(context, db, firebaseKey, userName, activeReq!!.detail) { success ->
                    if (success) {
                        Toast.makeText(context, "Item deleted successfully", Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, "Error: Could not delete item", Toast.LENGTH_SHORT)
                            .show()
                    }
                    showDeleteConfirmationDialog = false
                }
            }
        )
    }
}

@Composable
fun InventoryItem(
    detail: String,
    partyNames: String,
    comment: String,
    updatedBy: String,
    userRole: String,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                if (userRole == "Admin") {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Item",
                                tint = Blue2
                            )
                        }
                        IconButton(onClick = onDeleteClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Item",
                                tint = Color.DarkGray
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text("Clients: $partyNames", style = MaterialTheme.typography.bodySmall)
            if (comment.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Comment: \"$comment\" Updated by - $updatedBy",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun ActionDialog(
    req: RequirementUi,
    maxQty: Double,
    onDismiss: () -> Unit,
    onFulfil: (price: Double, qty: Double, vendor: String) -> Unit,
    onComment: (String) -> Unit
) {
    val context = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var price by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("") }
    var comment by remember { mutableStateOf("") }
    var vendor by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        text = {
            Column {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { tab = 0 }) {
                        Text("Fulfil")
                    }
                    TextButton(onClick = { tab = 1 }) {
                        Text("Comment")
                    }
                }
                Spacer(Modifier.height(12.dp))

                // --- Fulfil Section ---
                if (tab == 0) {
                    TextField(
                        label = "Vendor Name",
                        textValue = vendor,
                        onValueChange = { vendor = it }
                    )
                    TextField(
                        label = "Price (₹)",
                        textValue = price,
                        onValueChange = { price = it },
                        textType = stringResource(id = R.string.number) // Assuming R.string.number exists
                    )
                    TextField(
                        label = "Quantity",
                        textValue = qty,
                        onValueChange = { qty = it },
                        textType = stringResource(id = R.string.number)
                    )

                    WrapButtonWithBackground(
                        toDoFunction = {
                            val enteredQty = qty.toDoubleOrNull() ?: 0.0
                            if (enteredQty <= 0.0) {
                                Toast.makeText(
                                    context,
                                    "Quantity must be greater than 0",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@WrapButtonWithBackground
                            }
                            if (enteredQty > maxQty) {
                                Toast.makeText(
                                    context,
                                    "Entered quantity %.2f exceeds remaining stock (%.2f)".format(
                                        enteredQty,
                                        maxQty
                                    ),
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@WrapButtonWithBackground
                            }
                            onFulfil(price.toDoubleOrNull() ?: 0.0, enteredQty, vendor)
                        },
                        label = "Save"
                    )

                } else {
                    TextField(
                        label = "Comment",
                        textValue = comment,
                        onValueChange = { comment = it }
                    )
                    Spacer(Modifier.height(16.dp))
                    WrapButtonWithBackground(
                        toDoFunction = {
                            if (comment.isBlank()) {
                                Toast.makeText(
                                    context,
                                    "Comment cannot be empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@WrapButtonWithBackground
                            }
                            onComment(comment)
                        },
                        label = "Submit"
                    )
                }
            }
        },
        dismissButton = {}
    )

    // Quantity cap enforcement in UI for responsiveness
    LaunchedEffect(qty, tab) {
        if (tab == 0 && qty.isNotBlank()) {
            val entered = qty.toDoubleOrNull() ?: 0.0
            if (entered > maxQty) qty = "%.2f".format(maxQty)
        }
    }
}

@Composable
fun DeleteConfirmationDialog(
    req: RequirementUi,
    onDismiss: () -> Unit,
    onConfirmDelete: (firebaseKey: String) -> Unit // Callback when delete is confirmed
) {
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Confirm Deletion") },
        text = {
            Column {
                Text("Do you want to delete this item?")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = req.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (!isLoading) {
                        isLoading = true // Show loading indicator
                        onConfirmDelete(req.firebaseKey)
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.height(24.dp))
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

private fun handleFulfil(
    context: Context,
    db: FirebaseDatabase,
    userId: String,
    req: RequirementUi,
    price: Double,
    qty: Double,
    vendor: String,
    initiatorName: String
) {
    if (qty <= 0.0) {
        Toast.makeText(context, "Quantity must be positive.", Toast.LENGTH_SHORT).show()
        return
    }

    val groupedRef = db.getReference("GroupedLensOrders").child(req.firebaseKey)
    val partiallyCompletedRef = db.getReference("PartiallyCompletedLensOrders")
    val completedRef = db.getReference("CompletedLensOrders")
    val timestampKey = System.currentTimeMillis().toString()

    groupedRef.get().addOnSuccessListener { snap ->
        val originalOrdersMap = snap.child("orders").value as? Map<String, Number> ?: emptyMap()
        val originalTotalQuantityInOrder = originalOrdersMap.values.sumOf { it.toDouble() }

        val currentPartiallyAllottedQty =
            (snap.child("partiallyAllottedQty").value as? Number)?.toDouble() ?: 0.0

        val actualRemainingInDB = originalTotalQuantityInOrder - currentPartiallyAllottedQty
        // Add a small epsilon for floating point comparison
        if (qty > actualRemainingInDB + 0.001) {
            Toast.makeText(
                context,
                "Entered quantity exceeds actual remaining stock in database. This shouldn't happen if UI is correct.",
                Toast.LENGTH_LONG
            ).show()
            Log.e(
                "handleFulfil",
                "Validation error: Tried to fulfill $qty but only $actualRemainingInDB remaining in DB for ${req.firebaseKey}"
            )
            return@addOnSuccessListener
        }

        val detailMap = mapOf(
            "type" to snap.child("type").getValue(String::class.java),
            "coating" to snap.child("coating").getValue(String::class.java),
            "coatingType" to snap.child("coatingType").getValue(String::class.java),
            "material" to snap.child("material").getValue(String::class.java),
            "sphere" to snap.child("sphere").getValue(String::class.java),
            "cylinder" to snap.child("cylinder").getValue(String::class.java),
            "axis" to snap.child("axis").getValue(String::class.java),
            "add" to snap.child("add").getValue(String::class.java),
            "lensSpecificType" to snap.child("lensSpecificType").getValue(String::class.java)
        )

        // Common data for both partial and full fulfillments
        val commonData = mutableMapOf<String, Any>(
            "price" to price,
            "vendor" to vendor,
            "updatedBy" to userId,
            "timestamp" to ServerValue.TIMESTAMP,
            "fulfilledQty" to qty // This is the quantity being fulfilled in *this* transaction
        ) + detailMap

        val newPartiallyAllottedQty = currentPartiallyAllottedQty + qty

        // Check for full grouped order completion (using a small epsilon for floating point comparison)
        if (newPartiallyAllottedQty >= originalTotalQuantityInOrder - 0.001) {
            // SCENARIO 1: Full grouped order completion
            Log.d("handleFulfil", "Full completion detected for ${req.firebaseKey}")

            // Calculate price per unit for THIS specific fulfillment transaction
            val pricePerUnitForThisFulfillment = if (qty > 0) price / qty else 0.0

            // Enriched orders for CompletedLensOrders, based on the *current transaction's* quantities
            // This distributes the 'qty' (the amount being fulfilled NOW) proportionally
            // among the clients based on their original order quantities.
            val enrichedOrders = originalOrdersMap.mapValues { (clientName, clientOriginalQtyNum) ->
                val clientOriginalQty = clientOriginalQtyNum.toDouble()
                // Calculate the proportion of this client's original quantity relative to the total original quantity
                val clientProportion = if (originalTotalQuantityInOrder > 0) clientOriginalQty / originalTotalQuantityInOrder else 0.0
                // Calculate the quantity for this client in *this specific fulfillment*
                val quantityInThisFulfillment = clientProportion * qty
                val totalShareForThisClient = pricePerUnitForThisFulfillment * quantityInThisFulfillment
                mapOf("quantity" to quantityInThisFulfillment, "totalShare" to totalShareForThisClient)
            }

            val dataToSaveToCompleted = commonData.toMutableMap().apply {
                put("orders", enrichedOrders)
                // fulfilledQty is already set to 'qty' in commonData, which is correct for this transaction
            }

            completedRef.child(timestampKey).setValue(dataToSaveToCompleted)
                .addOnSuccessListener {
                    Log.d("DB", "Full order saved to CompletedLensOrders for key: $timestampKey")
                    // Remove from GroupedLensOrders ONLY after successful save to CompletedLensOrders
                    groupedRef.removeValue()
                        .addOnSuccessListener {
                            Log.d("DB", "Grouped order removed for key: ${req.firebaseKey}")
                            Log.d(
                                "handleFulfil",
                                "Order ${req.firebaseKey} fully completed and moved to CompletedLensOrders."
                            )
                            NotificationHelper.sendAdminNotification(
                                context,
                                "FULFILLED",
                                req.detail,
                                initiatorName
                            )
                        }
                        .addOnFailureListener { Log.e("DB", "Failed to remove grouped order", it) }
                }
                .addOnFailureListener { Log.e("DB", "Failed to save full order to CompletedLensOrders", it) }

        } else {
            // SCENARIO 2 & 3: Partial fulfillment of grouped order
            Log.d("handleFulfil", "Partial fulfillment detected for ${req.firebaseKey}. New allotted: $newPartiallyAllottedQty")

            // Always reduce quantity (update partiallyAllottedQty in GroupedLensOrders)
            val updates = mapOf<String, Any>(
                "partiallyAllottedQty" to newPartiallyAllottedQty
            )
            groupedRef.updateChildren(updates)
                .addOnFailureListener {
                    Log.e(
                        "DB",
                        "Failed to update partiallyAllottedQty in GroupedLensOrders",
                        it
                    )
                }

            /*
            // CODE COMMENTED OUT: The original logic treated partial fulfillments for
            // single-client orders differently from multi-client orders.
            // The new requirement is to treat ALL partial fulfillments the same way:
            // log them to 'PartiallyCompletedLensOrders'. The original code is kept here
            // for future reference.

            val isSingleClient = originalOrdersMap.size == 1

            if (isSingleClient) {
                // SCENARIO 2: Partial fulfillment for a single client grouped order
                Log.d("handleFulfil", "Partial fulfillment for single client. Logging to CompletedLensOrders.")

                val clientName = originalOrdersMap.keys.first()
                val pricePerUnitForThisFulfillment = if (qty > 0) price / qty else 0.0

                // Create enriched orders for *this specific partial fulfillment*
                val enrichedOrdersForPartial = mapOf(
                    clientName to mapOf(
                        "quantity" to qty, // The quantity fulfilled for this client in *this* transaction
                        "totalShare" to pricePerUnitForThisFulfillment * qty
                    )
                )

                val dataToSaveToCompletedForPartial = commonData.toMutableMap().apply {
                    put("orders", enrichedOrdersForPartial)
                }

                completedRef.child(timestampKey).setValue(dataToSaveToCompletedForPartial)
                    .addOnSuccessListener {
                        Log.d("DB", "Partial fulfillment for single client saved to CompletedLensOrders for key: $timestampKey")
                        NotificationHelper.sendAdminNotification(
                            context,
                            "FULFILLED",
                            req.detail,
                            initiatorName
                        )
                    }
                    .addOnFailureListener {
                        Log.e("DB", "Failed to save partial fulfillment for single client to CompletedLensOrders", it)
                    }
            } else {
                // SCENARIO 3: Partial fulfillment for multiple clients grouped order
                Log.d("handleFulfil", "Partial fulfillment for multiple clients. Logging to PartiallyCompletedLensOrders.")

                val originalOrdersMapDouble = originalOrdersMap.mapValues { (_, qtyNum) -> qtyNum.toDouble() }

                val dataToSaveToPartiallyCompleted = commonData.toMutableMap().apply {
                    put("orders", originalOrdersMapDouble)
                }

                partiallyCompletedRef.child(timestampKey).setValue(dataToSaveToPartiallyCompleted)
                    .addOnSuccessListener {
                        Log.d("DB", "Partial fulfillment for multiple clients saved to PartiallyCompletedLensOrders for key: $timestampKey")
                        NotificationHelper.sendAdminNotification(
                            context,
                            "FULFILLED",
                            req.detail,
                            initiatorName
                        )
                    }
                    .addOnFailureListener {
                        Log.e("DB", "Failed to save partial fulfillment for multiple clients to PartiallyCompletedLensOrders", it)
                    }
            }
            */

            // NEW UNIFIED LOGIC FOR ALL PARTIAL FULFILLMENTS:
            // Any partial fulfillment, regardless of client count, is logged to PartiallyCompletedLensOrders.
            Log.d("handleFulfil", "Partial fulfillment. Logging to PartiallyCompletedLensOrders.")

            // For PartiallyCompletedLensOrders, we store the original client breakdown
            // and the fulfilledQty reflects the amount fulfilled in *this transaction*.
            val originalOrdersMapDouble = originalOrdersMap.mapValues { (_, qtyNum) -> qtyNum.toDouble() }

            val dataToSaveToPartiallyCompleted = commonData.toMutableMap().apply {
                put("orders", originalOrdersMapDouble)
                // fulfilledQty is already set to 'qty' in commonData, which is correct for this transaction
            }

            partiallyCompletedRef.child(timestampKey).setValue(dataToSaveToPartiallyCompleted)
                .addOnSuccessListener {
                    Log.d("DB", "Partial fulfillment saved to PartiallyCompletedLensOrders for key: $timestampKey")
                    NotificationHelper.sendAdminNotification(
                        context,
                        "PARTIALLY_FULFILLED", // Using a more specific notification type
                        req.detail,
                        initiatorName
                    )
                }
                .addOnFailureListener {
                    Log.e("DB", "Failed to save partial fulfillment to PartiallyCompletedLensOrders", it)
                }
        }
    }.addOnFailureListener {
        Log.e("DB", "Failed to read original grouped data for fulfillment", it)
        Toast.makeText(context, "Failed to process fulfillment: ${it.message}", Toast.LENGTH_SHORT)
            .show()
    }
}

//OLD CODE
/*private fun handleFulfil(
    context: Context,
    db: FirebaseDatabase,
    userId: String,
    req: RequirementUi,
    price: Double,
    qty: Double,
    vendor: String,
    initiatorName: String
) {
    if (qty <= 0.0) {
        Toast.makeText(context, "Quantity must be positive.", Toast.LENGTH_SHORT).show()
        return
    }

    val groupedRef = db.getReference("GroupedLensOrders").child(req.firebaseKey)
    val partiallyCompletedRef = db.getReference("PartiallyCompletedLensOrders")
    val completedRef = db.getReference("CompletedLensOrders")
    val timestampKey = System.currentTimeMillis().toString()

    groupedRef.get().addOnSuccessListener { snap ->
        val originalOrdersMap = snap.child("orders").value as? Map<String, Number> ?: emptyMap()
        val originalTotalQuantityInOrder = originalOrdersMap.values.sumOf { it.toDouble() }

        val currentPartiallyAllottedQty =
            (snap.child("partiallyAllottedQty").value as? Number)?.toDouble() ?: 0.0

        val actualRemainingInDB = originalTotalQuantityInOrder - currentPartiallyAllottedQty
        // Add a small epsilon for floating point comparison
        if (qty > actualRemainingInDB + 0.001) {
            Toast.makeText(
                context,
                "Entered quantity exceeds actual remaining stock in database. This shouldn't happen if UI is correct.",
                Toast.LENGTH_LONG
            ).show()
            Log.e(
                "handleFulfil",
                "Validation error: Tried to fulfill $qty but only $actualRemainingInDB remaining in DB for ${req.firebaseKey}"
            )
            return@addOnSuccessListener
        }

        val detailMap = mapOf(
            "type" to snap.child("type").getValue(String::class.java),
            "coating" to snap.child("coating").getValue(String::class.java),
            "coatingType" to snap.child("coatingType").getValue(String::class.java),
            "material" to snap.child("material").getValue(String::class.java),
            "sphere" to snap.child("sphere").getValue(String::class.java),
            "cylinder" to snap.child("cylinder").getValue(String::class.java),
            "axis" to snap.child("axis").getValue(String::class.java),
            "add" to snap.child("add").getValue(String::class.java),
            "lensSpecificType" to snap.child("lensSpecificType").getValue(String::class.java)
        )

        // Common data for both partial and full fulfillments
        val commonData = mutableMapOf<String, Any>(
            "price" to price,
            "vendor" to vendor,
            "updatedBy" to userId,
            "timestamp" to ServerValue.TIMESTAMP,
            "fulfilledQty" to qty // This is the quantity being fulfilled in *this* transaction
        ) + detailMap

        val newPartiallyAllottedQty = currentPartiallyAllottedQty + qty

        // Check for full grouped order completion (using a small epsilon for floating point comparison)
        if (newPartiallyAllottedQty >= originalTotalQuantityInOrder - 0.001) {
            // SCENARIO 1: Full grouped order completion
            Log.d("handleFulfil", "Full completion detected for ${req.firebaseKey}")

            // Calculate price per unit for THIS specific fulfillment transaction
            val pricePerUnitForThisFulfillment = if (qty > 0) price / qty else 0.0

            // Enriched orders for CompletedLensOrders, based on the *current transaction's* quantities
            // This distributes the 'qty' (the amount being fulfilled NOW) proportionally
            // among the clients based on their original order quantities.
            val enrichedOrders = originalOrdersMap.mapValues { (clientName, clientOriginalQtyNum) ->
                val clientOriginalQty = clientOriginalQtyNum.toDouble()
                // Calculate the proportion of this client's original quantity relative to the total original quantity
                val clientProportion = if (originalTotalQuantityInOrder > 0) clientOriginalQty / originalTotalQuantityInOrder else 0.0
                // Calculate the quantity for this client in *this specific fulfillment*
                val quantityInThisFulfillment = clientProportion * qty
                val totalShareForThisClient = pricePerUnitForThisFulfillment * quantityInThisFulfillment
                mapOf("quantity" to quantityInThisFulfillment, "totalShare" to totalShareForThisClient)
            }

            val dataToSaveToCompleted = commonData.toMutableMap().apply {
                put("orders", enrichedOrders)
                // fulfilledQty is already set to 'qty' in commonData, which is correct for this transaction
            }

            completedRef.child(timestampKey).setValue(dataToSaveToCompleted)
                .addOnSuccessListener {
                    Log.d("DB", "Full order saved to CompletedLensOrders for key: $timestampKey")
                    // Remove from GroupedLensOrders ONLY after successful save to CompletedLensOrders
                    groupedRef.removeValue()
                        .addOnSuccessListener {
                            Log.d("DB", "Grouped order removed for key: ${req.firebaseKey}")
                            Log.d(
                                "handleFulfil",
                                "Order ${req.firebaseKey} fully completed and moved to CompletedLensOrders."
                            )
                            NotificationHelper.sendAdminNotification(
                                context,
                                "FULFILLED",
                                req.detail,
                                initiatorName
                            )
                        }
                        .addOnFailureListener { Log.e("DB", "Failed to remove grouped order", it) }
                }
                .addOnFailureListener { Log.e("DB", "Failed to save full order to CompletedLensOrders", it) }

        } else {
            // SCENARIO 2 & 3: Partial fulfillment of grouped order
            Log.d("handleFulfil", "Partial fulfillment detected for ${req.firebaseKey}. New allotted: $newPartiallyAllottedQty")

            // Always reduce quantity (update partiallyAllottedQty in GroupedLensOrders)
            val updates = mapOf<String, Any>(
                "partiallyAllottedQty" to newPartiallyAllottedQty
            )
            groupedRef.updateChildren(updates)
                .addOnFailureListener {
                    Log.e(
                        "DB",
                        "Failed to update partiallyAllottedQty in GroupedLensOrders",
                        it
                    )
                }

            // Determine if single client or multiple clients for logging the transaction
            val isSingleClient = originalOrdersMap.size == 1

            if (isSingleClient) {
                // SCENARIO 2: Partial fulfillment for a single client grouped order
                Log.d("handleFulfil", "Partial fulfillment for single client. Logging to CompletedLensOrders.")

                val clientName = originalOrdersMap.keys.first()
                val pricePerUnitForThisFulfillment = if (qty > 0) price / qty else 0.0

                // Create enriched orders for *this specific partial fulfillment*
                val enrichedOrdersForPartial = mapOf(
                    clientName to mapOf(
                        "quantity" to qty, // The quantity fulfilled for this client in *this* transaction
                        "totalShare" to pricePerUnitForThisFulfillment * qty
                    )
                )

                val dataToSaveToCompletedForPartial = commonData.toMutableMap().apply {
                    put("orders", enrichedOrdersForPartial)
                    // fulfilledQty is already set to 'qty' in commonData, which is correct for this transaction
                }

                completedRef.child(timestampKey).setValue(dataToSaveToCompletedForPartial)
                    .addOnSuccessListener {
                        Log.d("DB", "Partial fulfillment for single client saved to CompletedLensOrders for key: $timestampKey")
                        NotificationHelper.sendAdminNotification(
                            context,
                            "FULFILLED", // Or "PARTIALLY_FULFILLED" if you define a new type
                            req.detail,
                            initiatorName
                        )
                    }
                    .addOnFailureListener {
                        Log.e("DB", "Failed to save partial fulfillment for single client to CompletedLensOrders", it)
                    }
            } else {
                // SCENARIO 3: Partial fulfillment for multiple clients grouped order
                Log.d("handleFulfil", "Partial fulfillment for multiple clients. Logging to PartiallyCompletedLensOrders.")

                // For PartiallyCompletedLensOrders, we store the original client breakdown
                // and the fulfilledQty reflects the amount fulfilled in *this transaction*.
                val originalOrdersMapDouble = originalOrdersMap.mapValues { (_, qtyNum) -> qtyNum.toDouble() }

                val dataToSaveToPartiallyCompleted = commonData.toMutableMap().apply {
                    put("orders", originalOrdersMapDouble)
                    // fulfilledQty is already set to 'qty' in commonData, which is correct for this transaction
                }

                partiallyCompletedRef.child(timestampKey).setValue(dataToSaveToPartiallyCompleted)
                    .addOnSuccessListener {
                        Log.d("DB", "Partial fulfillment for multiple clients saved to PartiallyCompletedLensOrders for key: $timestampKey")
                        NotificationHelper.sendAdminNotification(
                            context,
                            "FULFILLED", // Or "PARTIALLY_FULFILLED" if you define a new type
                            req.detail,
                            initiatorName
                        )
                    }
                    .addOnFailureListener {
                        Log.e("DB", "Failed to save partial fulfillment for multiple clients to PartiallyCompletedLensOrders", it)
                    }
            }
        }
    }.addOnFailureListener {
        Log.e("DB", "Failed to read original grouped data for fulfillment", it)
        Toast.makeText(context, "Failed to process fulfillment: ${it.message}", Toast.LENGTH_SHORT)
            .show()
    }
}*/

/*private fun handleFulfil(
    context: Context,
    db: FirebaseDatabase,
    userId: String,
    req: RequirementUi,
    price: Double,
    qty: Double,
    vendor: String,
    initiatorName: String
) {
    if (qty <= 0.0) {
        Toast.makeText(context, "Quantity must be positive.", Toast.LENGTH_SHORT).show()
        return
    }

    val groupedRef = db.getReference("GroupedLensOrders").child(req.firebaseKey)
    val partiallyCompletedRef = db.getReference("PartiallyCompletedLensOrders")
    val completedRef = db.getReference("CompletedLensOrders")
    val timestampKey = System.currentTimeMillis().toString()

    groupedRef.get().addOnSuccessListener { snap ->
        val originalOrdersMap = snap.child("orders").value as? Map<String, Number> ?: emptyMap()
        val originalTotalQuantityInOrder = originalOrdersMap.values.sumOf { it.toDouble() }

        val currentPartiallyAllottedQty =
            (snap.child("partiallyAllottedQty").value as? Number)?.toDouble() ?: 0.0

        val actualRemainingInDB = originalTotalQuantityInOrder - currentPartiallyAllottedQty
        // Add a small epsilon for floating point comparison
        if (qty > actualRemainingInDB + 0.001) {
            Toast.makeText(
                context,
                "Entered quantity exceeds actual remaining stock in database.",
                Toast.LENGTH_LONG
            ).show()
            Log.e(
                "handleFulfil",
                "Validation error: Tried to fulfill $qty but only $actualRemainingInDB remaining in DB for ${req.firebaseKey}"
            )
            return@addOnSuccessListener
        }

        val detailMap = mapOf(
            "type" to snap.child("type").getValue(String::class.java),
            "coating" to snap.child("coating").getValue(String::class.java),
            "coatingType" to snap.child("coatingType").getValue(String::class.java),
            "material" to snap.child("material").getValue(String::class.java),
            "sphere" to snap.child("sphere").getValue(String::class.java),
            "cylinder" to snap.child("cylinder").getValue(String::class.java),
            "axis" to snap.child("axis").getValue(String::class.java),
            "add" to snap.child("add").getValue(String::class.java),
            "lensSpecificType" to snap.child("lensSpecificType").getValue(String::class.java)
        )

        // Common data for both partial and full fulfillments
        val commonData = mutableMapOf<String, Any>(
            "price" to price,
            "vendor" to vendor,
            "updatedBy" to userId,
            "timestamp" to ServerValue.TIMESTAMP,
            "fulfilledQty" to qty
        ) + detailMap

        val newPartiallyAllottedQty = currentPartiallyAllottedQty + qty

        // Check for full grouped order completion
        if (newPartiallyAllottedQty >= originalTotalQuantityInOrder - 0.001) {
            // SCENARIO 1: Full grouped order completion
            Log.d("handleFulfil", "Full completion detected for ${req.firebaseKey}")

            val enrichedOrders = originalOrdersMap.mapValues { (_, clientOriginalQtyNum) ->
                val clientOriginalQty = clientOriginalQtyNum.toDouble()
                mapOf("quantity" to clientOriginalQty, "totalShare" to (price / originalTotalQuantityInOrder) * clientOriginalQty)
            }

            // <-- THE CRITICAL ADDITION 1/2: Create the backup map -->
            val originalGroupedOrderInfo = mapOf(
                "totalRequiredQty" to originalTotalQuantityInOrder,
                "originalClientBreakdown" to originalOrdersMap
            )

            val dataToSaveToCompleted = commonData.toMutableMap().apply {
                put("orders", enrichedOrders)
                // Use the total original quantity as the fulfilled quantity for the final record
                put("fulfilledQty", originalTotalQuantityInOrder)
                // <-- THE CRITICAL ADDITION 2/2: Add the backup map to the data -->
                put("originalGroupedOrderInfo", originalGroupedOrderInfo)
                // Add the groupedKey for future reference when editing
                put("groupedKey", req.firebaseKey)
            }

            completedRef.child(timestampKey).setValue(dataToSaveToCompleted)
                .addOnSuccessListener {
                    Log.d("DB", "Full order saved to CompletedLensOrders for key: $timestampKey")
                    groupedRef.removeValue()
                        .addOnSuccessListener {
                            Log.d("DB", "Grouped order removed for key: ${req.firebaseKey}")
                            NotificationHelper.sendAdminNotification(
                                context,
                                "FULFILLED",
                                req.detail,
                                initiatorName
                            )
                        }
                        .addOnFailureListener { Log.e("DB", "Failed to remove grouped order", it) }
                }
                .addOnFailureListener { Log.e("DB", "Failed to save full order to CompletedLensOrders", it) }

        } else {
            // SCENARIO 2 & 3: Partial fulfillment of grouped order (Your existing logic)
            Log.d("handleFulfil", "Partial fulfillment detected for ${req.firebaseKey}. New allotted: $newPartiallyAllottedQty")

            val updates = mapOf<String, Any>(
                "partiallyAllottedQty" to newPartiallyAllottedQty
            )
            groupedRef.updateChildren(updates)
                .addOnFailureListener {
                    Log.e("DB", "Failed to update partiallyAllottedQty", it)
                }

            val isSingleClient = originalOrdersMap.size == 1

            if (isSingleClient) {
                // Partial fulfillment for a single client grouped order
                Log.d("handleFulfil", "Partial fulfillment for single client. Logging to CompletedLensOrders.")

                val clientName = originalOrdersMap.keys.first()
                val enrichedOrdersForPartial = mapOf(
                    clientName to mapOf(
                        "quantity" to qty,
                        "totalShare" to price
                    )
                )

                val dataToSaveToCompletedForPartial = commonData.toMutableMap().apply {
                    put("orders", enrichedOrdersForPartial)
                    // Add the groupedKey for future reference
                    put("groupedKey", req.firebaseKey)
                }

                completedRef.child(timestampKey).setValue(dataToSaveToCompletedForPartial)
                    .addOnSuccessListener {
                        Log.d("DB", "Partial fulfillment for single client saved to CompletedLensOrders")
                        NotificationHelper.sendAdminNotification(context, "FULFILLED", req.detail, initiatorName)
                    }
                    .addOnFailureListener {
                        Log.e("DB", "Failed to save partial fulfillment for single client", it)
                    }
            } else {
                // Partial fulfillment for multiple clients grouped order
                Log.d("handleFulfil", "Partial fulfillment for multiple clients. Logging to PartiallyCompletedLensOrders.")

                val originalOrdersMapDouble = originalOrdersMap.mapValues { (_, qtyNum) -> qtyNum.toDouble() }
                val dataToSaveToPartiallyCompleted = commonData.toMutableMap().apply {
                    put("orders", originalOrdersMapDouble)
                    // Add the groupedKey for future reference
                    put("groupedKey", req.firebaseKey)
                }

                partiallyCompletedRef.child(timestampKey).setValue(dataToSaveToPartiallyCompleted)
                    .addOnSuccessListener {
                        Log.d("DB", "Partial fulfillment for multiple clients saved to PartiallyCompletedLensOrders")
                        NotificationHelper.sendAdminNotification(context, "FULFILLED", req.detail, initiatorName)
                    }
                    .addOnFailureListener {
                        Log.e("DB", "Failed to save partial fulfillment for multiple clients", it)
                    }
            }
        }
    }.addOnFailureListener {
        Log.e("DB", "Failed to read original grouped data for fulfillment", it)
        Toast.makeText(context, "Failed to process fulfillment: ${it.message}", Toast.LENGTH_SHORT)
            .show()
    }
}*/

private fun handleComment(
    context: Context,
    db: FirebaseDatabase,
    userId: String,
    key: String,
    comment: String,
    initiatorName: String,
    itemDetail: String
) {
    val userRef = db.getReference("Users").child(userId)

    userRef.get().addOnSuccessListener { snapshot ->
        val userName = snapshot.child("name").getValue(String::class.java) ?: "Unknown User"

        val data = mapOf(
            "text" to comment,
            "updatedBy" to userName,
            "timestamp" to ServerValue.TIMESTAMP
        )

        db.getReference("GroupedLensOrders")
            .child(key)
            .child("comment")
            .setValue(data)
            .addOnSuccessListener {
                Log.d("handleComment", "Comment updated successfully for $key by $initiatorName")
                NotificationHelper.sendAdminNotification(
                    context,
                    "COMMENTED",
                    itemDetail,
                    initiatorName
                )
            }
            .addOnFailureListener {
                Log.e("DB", "Comment write failed for $key", it)
            }

    }.addOnFailureListener {
        Log.e("DB", "Failed to get user name for comment: ${it.message}", it)
    }
}

// New function to handle deletion
private fun handleDelete(
    context: Context,
    db: FirebaseDatabase,
    firebaseKey: String,
    initiatorName: String,
    itemDetail: String,
    onComplete: (Boolean) -> Unit
) {
    db.getReference("GroupedLensOrders").child(firebaseKey).removeValue()
        .addOnSuccessListener {
            Log.d("handleDelete", "Item $firebaseKey deleted successfully from GroupedLensOrders")
            NotificationHelper.sendAdminNotification(context, "DELETED", itemDetail, initiatorName)
            onComplete(true)
        }
        .addOnFailureListener { e ->
            Log.e("handleDelete", "Failed to delete item $firebaseKey", e)
            onComplete(false)
        }
}