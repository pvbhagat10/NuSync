package com.nusync.utils
//
//import android.util.Log
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.google.firebase.database.DataSnapshot
//import com.google.firebase.database.DatabaseError
//import com.google.firebase.database.DatabaseReference
//import com.google.firebase.database.FirebaseDatabase
//import com.google.firebase.database.ServerValue
//import com.google.firebase.database.ValueEventListener
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.tasks.await
//
//sealed class OrderStatus {
//    object Loading : OrderStatus()
//    data class Success(val groupedOrders: Map<String, GroupedOrder>,
//                       val partiallyCompletedOrders: Map<String, PartiallyCompletedOrder>,
//                       val completedOrders: Map<String, CompletedOrder>) : OrderStatus()
//    data class Error(val message: String) : OrderStatus()
//}
//
//class EditUserOrdersViewModel : ViewModel() {
//
//    private val db = FirebaseDatabase.getInstance()
//    private val groupedRef = db.getReference("GroupedLensOrders")
//    private val partialRef = db.getReference("PartiallyCompletedLensOrders")
//    private val completedRef = db.getReference("CompletedLensOrders")
//    private val userUpdatesRef = db.getReference("UserUpdates")
//
//    private val _orderStatus = MutableStateFlow<OrderStatus>(OrderStatus.Loading)
//    val orderStatus: StateFlow<OrderStatus> = _orderStatus
//
//    init {
//        loadAllOrders()
//    }
//
//    private fun loadAllOrders() {
//        viewModelScope.launch {
//            try {
//                val groupedSnapshot = groupedRef.get().await()
//                val partialSnapshot = partialRef.get().await()
//                val completedSnapshot = completedRef.get().await()
//
//                val groupedOrders = groupedSnapshot.children.associate { it.key!! to it.getValue(GroupedOrder::class.java)!! }
//                val partiallyCompletedOrders = partialSnapshot.children.associate { it.key!! to it.getValue(PartiallyCompletedOrder::class.java)!! }
//                val completedOrders = completedSnapshot.children.associate { it.key!! to it.getValue(CompletedOrder::class.java)!! }
//
//                _orderStatus.value = OrderStatus.Success(groupedOrders, partiallyCompletedOrders, completedOrders)
//            } catch (e: Exception) {
//                _orderStatus.value = OrderStatus.Error("Failed to load orders: ${e.message}")
//            }
//        }
//    }
//
//    /**
//     * Updates an order and handles the business logic for different scenarios.
//     *
//     * @param firebaseKey The key of the order in the database (e.g., timestamp key or grouped key).
//     * @param newQty The new quantity to be updated.
//     * @param newPrice The new price for the new quantity.
//     * @param newVendor The new vendor.
//     * @param originalNodeType The original node where the data came from ("GroupedLensOrders", "CompletedLensOrders", or "PartiallyCompletedLensOrders").
//     */
//    fun updateOrder(
//        firebaseKey: String,
//        newQty: Int,
//        newPrice: Double,
//        newVendor: String,
//        originalNodeType: String,
//        userId: String
//    ) {
//        viewModelScope.launch {
//            try {
//                when (originalNodeType) {
//                    "GroupedLensOrders" -> updateGroupedOrder(firebaseKey, newQty, newPrice, newVendor, userId)
//                    "CompletedLensOrders" -> updateCompletedOrder(firebaseKey, newQty, newPrice, newVendor, userId)
//                    "PartiallyCompletedLensOrders" -> updatePartiallyCompletedOrder(firebaseKey, newQty, newPrice, newVendor, userId)
//                    else -> throw IllegalArgumentException("Invalid node type: $originalNodeType")
//                }
//            } catch (e: Exception) {
//                // Handle the error (e.g., show a Toast or update UI state)
//                Log.e("EditUserOrdersViewModel", "Update failed: ${e.message}", e)
//                // You might want to update a state flow for error messages
//            }
//        }
//    }
//
//    private suspend fun updateGroupedOrder(
//        firebaseKey: String,
//        newQty: Int,
//        newPrice: Double,
//        newVendor: String,
//        userId: String
//    ) {
//        val groupedRefNode = groupedRef.child(firebaseKey)
//        val snapshot = groupedRefNode.get().await()
//        val originalOrdersMap = snapshot.child("orders").value as? Map<String, Long> ?: emptyMap()
//        val clientKeys = originalOrdersMap.keys.toList()
//        val isSingleClient = clientKeys.size == 1
//        val clientKey = clientKeys.firstOrNull()
//
//        val totalOriginalQty = originalOrdersMap.values.sum().toInt()
//
//        if (newQty > totalOriginalQty) {
//            // Handle error: new quantity exceeds original total quantity
//            return
//        }
//
//        val detailMap = mapOf(
//            "type"              to snapshot.child("type").getValue(String::class.java),
//            "coating"           to snapshot.child("coating").getValue(String::class.java),
//            "coatingType"       to snapshot.child("coatingType").getValue(String::class.java),
//            "material"          to snapshot.child("material").getValue(String::class.java),
//            "sphere"            to snapshot.child("sphere").getValue(String::class.java),
//            "cylinder"          to snapshot.child("cylinder").getValue(String::class.java),
//            "axis"              to snapshot.child("axis").getValue(String::class.java),
//            "add"               to snapshot.child("add").getValue(String::class.java),
//            "lensSpecificType"  to snapshot.child("lensSpecificType").getValue(String::class.java)
//        )
//
//        val pricePerUnit = if (newQty > 0) newPrice / newQty else 0.0
//
//        if (newQty == totalOriginalQty) {
//            // Case 1: Full fulfillment of the entire grouped order (even if updated)
//            val enrichedOrders = originalOrdersMap.mapValues { (clientName, clientQty) ->
//                val totalShare = pricePerUnit * clientQty
//                mapOf("quantity" to clientQty, "totalShare" to totalShare)
//            }
//
//            val dataWithOrders = detailMap.toMutableMap().apply {
//                put("price", newPrice.toString())
//                put("vendor", newVendor)
//                put("updatedBy", userId)
//                put("timestamp", ServerValue.TIMESTAMP.toString())
//                put("fulfilledQty", newQty.toString())
//                put("orders", enrichedOrders.toString())
//            }
//
//            // Write to CompletedLensOrders
//            val timestampKey = System.currentTimeMillis().toString()
//            completedRef.child(timestampKey).setValue(dataWithOrders).await()
//
//            // Remove from GroupedLensOrders
//            groupedRefNode.removeValue().await()
//
//            // Update UserUpdates
//            userUpdatesRef.child(userId).child(timestampKey).setValue(dataWithOrders).await()
//
//        } else if (isSingleClient) {
//            // Case 2: Partial fulfillment for a single client (still in Grouped)
//            val originalQty = originalOrdersMap[clientKey]?.toInt() ?: 0
//            val remainingQty = originalQty - newQty
//            val singleClientShare = pricePerUnit * newQty
//
//            val singleClientOrder = clientKey?.let {
//                mapOf(it to mapOf("quantity" to newQty.toLong(), "totalShare" to singleClientShare))
//            } ?: emptyMap()
//
//            val dataForSingleClient = detailMap.toMutableMap().apply {
//                put("price", newPrice.toString())
//                put("vendor", newVendor)
//                put("updatedBy", userId)
//                put("timestamp", ServerValue.TIMESTAMP.toString())
//                put("fulfilledQty", newQty.toString())
//                put("orders", singleClientOrder.toString())
//            }
//
//            // Write to CompletedLensOrders for the fulfilled portion
//            val timestampKey = System.currentTimeMillis().toString()
//            completedRef.child(timestampKey).setValue(dataForSingleClient).await()
//
//            // Update remaining quantity in GroupedLensOrders
//            if (clientKey != null) {
//                groupedRefNode.child("orders").child(clientKey).setValue(remainingQty).await()
//            }
//
//            // Update UserUpdates
//            userUpdatesRef.child(userId).child(timestampKey).setValue(dataForSingleClient).await()
//
//        } else {
//            // Case 3: Partial fulfillment for multi-client (still in Grouped)
//            val partialData = detailMap.toMutableMap().apply {
//                put("price", newPrice.toString())
//                put("vendor", newVendor)
//                put("updatedBy", userId)
//                put("timestamp", ServerValue.TIMESTAMP.toString())
//                put("fulfilledQty", newQty.toString())
//                put("orders", originalOrdersMap.toString()) // Keep original requirements
//            }
//            // Write to PartiallyCompletedLensOrders
//            val timestampKey = System.currentTimeMillis().toString()
//            partialRef.child(timestampKey).setValue(partialData).await()
//
//            // Do NOT remove or update GroupedLensOrders
//
//            // Update UserUpdates
//            userUpdatesRef.child(userId).child(timestampKey).setValue(partialData).await()
//        }
//        // After a successful update, reload all orders to refresh the UI
//        loadAllOrders()
//    }
//
//
//    private suspend fun updateCompletedOrder(
//        firebaseKey: String,
//        newQty: Int,
//        newPrice: Double,
//        newVendor: String,
//        userId: String
//    ) {
//        val completedRefNode = completedRef.child(firebaseKey)
//        val snapshot = completedRefNode.get().await()
//        val originalOrdersMap = snapshot.child("orders").value as? Map<String, Map<String, Any>> ?: emptyMap()
//
//        val totalOriginalQty = originalOrdersMap.values.sumOf {
//            (it["quantity"] as? Long)?.toInt() ?: 0
//        }
//
//        if (newQty > totalOriginalQty) {
//            // Handle error: new quantity exceeds original total quantity
//            return
//        }
//
//        val pricePerUnit = if (newQty > 0) newPrice / newQty else 0.0
//
//        val newOrdersMap = mutableMapOf<String, Any>()
//
//        // Calculate new shares based on the new total quantity and price
//        originalOrdersMap.forEach { (clientName, orderDetails) ->
//            val originalQty = (orderDetails["quantity"] as? Long) ?: 0
//            val newShare = pricePerUnit * originalQty
//            newOrdersMap[clientName] = mapOf("quantity" to originalQty, "totalShare" to newShare)
//        }
//
//        val updatedData = mapOf(
//            "fulfilledQty" to newQty,
//            "price" to newPrice,
//            "vendor" to newVendor,
//            "updatedBy" to userId,
//            "orders" to newOrdersMap,
//            "timestamp" to ServerValue.TIMESTAMP
//        )
//
//        // Use updateChildren to update only the modified fields
//        completedRefNode.updateChildren(updatedData).await()
//
//        // Update UserUpdates node
//        userUpdatesRef.child(userId).child(firebaseKey).updateChildren(updatedData).await()
//
//        loadAllOrders()
//    }
//
//    private suspend fun updatePartiallyCompletedOrder(
//        firebaseKey: String,
//        newQty: Int,
//        newPrice: Double,
//        newVendor: String,
//        userId: String
//    ) {
//        val partialRefNode = partialRef.child(firebaseKey)
//        val snapshot = partialRefNode.get().await()
//        val originalOrdersMap = snapshot.child("orders").value as? Map<String, Long> ?: emptyMap()
//
//        val totalOriginalQty = originalOrdersMap.values.sum().toInt()
//
//        if (newQty > totalOriginalQty) {
//            // Handle error: new quantity exceeds original total quantity
//            return
//        }
//
//        val updatedData = mapOf(
//            "fulfilledQty" to newQty,
//            "price" to newPrice,
//            "vendor" to newVendor,
//            "updatedBy" to userId,
//            "timestamp" to ServerValue.TIMESTAMP
//        )
//
//        partialRefNode.updateChildren(updatedData).await()
//        userUpdatesRef.child(userId).child(firebaseKey).updateChildren(updatedData).await()
//
//        loadAllOrders()
//    }
//}
//
//data class CompletedOrder(
//    val coating: String? = null,
//    val coatingType: String? = null,
//    val cylinder: String? = null,
//    val fulfilledQty: Long? = null,
//    val material: String? = null,
//    val orders: Map<String, Any>? = null, // Can be Map<String, Long> or Map<String, Map<String, Any>>
//    val price: Double? = null,
//    val sphere: String? = null,
//    val timestamp: Long? = null,
//    val type: String? = null,
//    val updatedBy: String? = null,
//    val vendor: String? = null,
//    val add: String? = null,
//    val axis: String? = null,
//    val lensSpecificType: String? = null
//)
//
//data class PartiallyCompletedOrder(
//    val coating: String? = null,
//    val coatingType: String? = null,
//    val cylinder: String? = null,
//    val fulfilledQty: Long? = null,
//    val material: String? = null,
//    val orders: Map<String, Long>? = null,
//    val price: Double? = null,
//    val sphere: String? = null,
//    val timestamp: Long? = null,
//    val type: String? = null,
//    val updatedBy: String? = null,
//    val vendor: String? = null,
//    val add: String? = null,
//    val axis: String? = null,
//    val lensSpecificType: String? = null
//)
//
//data class GroupedOrder(
//    val coating: String? = null,
//    val coatingType: String? = null,
//    val cylinder: String? = null,
//    val material: String? = null,
//    val orders: Map<String, Long>? = null,
//    val sphere: String? = null,
//    val type: String? = null
//)
//
//data class UserUpdate(
//    // The structure varies, so we use a generic map for now
//    val data: Map<String, Any>? = null
//)
//
//data class ClientShare(
//    val quantity: Long? = null,
//    val totalShare: Double? = null
//)
//
//// This is a UI-specific data class to hold the requirement details
//data class RequirementDetails(
//    val firebaseKey: String,
//    val orderType: String,
//    val quantity: Int,
//    val price: Double,
//    val vendor: String,
//    val clientName: String? = null // For single client orders
//)