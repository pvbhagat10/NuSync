package com.nusync

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nusync.ui.theme.NuSyncTheme
import com.nusync.utils.getLensDetailString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CompletedClientOrder(
    val quantity: Double,
    val totalShare: Double
)

data class CompletedOrderUi(
    val firebaseKey: String,
    val detail: String,
    val clients: Map<String, CompletedClientOrder>,
    val vendor: String,
    val updatedById: String,
    var updatedByName: String = "",
    val timestamp: Long
) {
    val totalQuantity: Double
        get() = clients.values.sumOf { it.quantity }

    val totalPrice: Double
        get() = clients.values.sumOf { it.totalShare }

    val formattedClients: String
        get() = clients.entries.joinToString(", ") { (clientName, clientOrder) ->
            "$clientName (Qty: %.2f, Share: ₹%.2f)".format(clientOrder.quantity, clientOrder.totalShare)
        }
}

class CompletedOrdersActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                TopBar("Completed Orders") { CompletedOrdersScreen() }
            }
        }
    }
}

@Composable
fun CompletedOrdersScreen() {
    val db = remember { FirebaseDatabase.getInstance() }
    val ordersRef = remember { db.getReference("CompletedLensOrders") }
    val usersRef = remember { db.getReference("Users") }

    val orders = remember { mutableStateListOf<CompletedOrderUi>() }
    var searchText by remember { mutableStateOf("") }

    val userNameCache = remember { mutableStateMapOf<String, String>() }

    DisposableEffect(Unit) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val fetchedOrders = snapshot.children.mapNotNull { snap ->
                    val key = snap.key ?: return@mapNotNull null
                    val vendor = snap.child("vendor").getValue(String::class.java) ?: "-"
                    val updatedBy = snap.child("updatedBy").getValue(String::class.java) ?: "-"
                    val timestamp = snap.child("timestamp").getValue(Long::class.java) ?: 0L

                    val rawOrdersMap = snap.child("orders").value as? Map<String, Any> ?: emptyMap()
                    val clientsMap = rawOrdersMap.mapNotNull { (clientName, clientData) ->
                        if (clientData is Map<*, *>) {
                            val quantity = (clientData["quantity"] as? Number)?.toDouble() ?: 0.0
                            val totalShare = (clientData["totalShare"] as? Number)?.toDouble() ?: 0.0
                            clientName to CompletedClientOrder(quantity, totalShare)
                        } else {
                            null
                        }
                    }.toMap()

                    val type = snap.child("type").getValue(String::class.java) ?: "Unknown"
                    val sph = snap.child("sphere").getValue(String::class.java) ?: "0.00"
                    val cyl = snap.child("cylinder").getValue(String::class.java) ?: "0.00"
                    val ax = snap.child("axis").getValue(String::class.java) ?: ""
                    val add = snap.child("add").getValue(String::class.java) ?: ""
                    val coating = snap.child("coating").getValue(String::class.java) ?: ""
                    val coatingType = snap.child("coatingType").getValue(String::class.java) ?: ""
                    val material = snap.child("material").getValue(String::class.java) ?: ""
                    val spec = snap.child("lensSpecificType").getValue(String::class.java) ?: ""

                    val detail = getLensDetailString(type, coating, coatingType, material, sph, cyl, ax, add, spec)

                    CompletedOrderUi(
                        firebaseKey = key,
                        detail = detail,
                        clients = clientsMap,
                        vendor = vendor,
                        updatedById = updatedBy,
                        timestamp = timestamp
                    )
                }

                Log.i("fetchedOrders", fetchedOrders.toString())
                orders.clear()
                orders.addAll(fetchedOrders.sortedByDescending { it.timestamp })

                val uniqueUserIds = orders.map { it.updatedById }.toSet()
                for (uid in uniqueUserIds) {
                    if (uid !in userNameCache) {
                        usersRef.child(uid).get().addOnSuccessListener { userSnap ->
                            val name = userSnap.child("name").getValue(String::class.java) ?: "Unknown"
                            userNameCache[uid] = name
                            val ordersToUpdate = orders.filter { it.updatedById == uid }
                            val tempOrders = orders.toMutableList()
                            ordersToUpdate.forEach { order ->
                                val index = tempOrders.indexOf(order)
                                if (index != -1) {
                                    tempOrders[index] = order.copy(updatedByName = name)
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
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("DB", "Orders fetch failed", error.toException())
            }
        }

        ordersRef.addValueEventListener(listener)
        onDispose { ordersRef.removeEventListener(listener) }
    }

    Column(Modifier.fillMaxSize()) {
        TextField(
            label = "Search client name",
            textValue = searchText,
            onValueChange = { searchText = it }
        )

        val filteredOrders = orders.filter { order ->
            order.clients.keys.any { clientName ->
                clientName.contains(searchText, ignoreCase = true)
            }
        }.onEach {
            it.updatedByName = userNameCache[it.updatedById] ?: "Loading..."
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp)
        ) {
            items(filteredOrders, key = { it.firebaseKey }) { order ->
                CompletedOrderItem(order)
            }
        }
    }
}

@Composable
fun CompletedOrderItem(order: CompletedOrderUi) {
    val dateTime = remember(order.timestamp) {
        val formatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        formatter.format(Date(order.timestamp))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(order.detail, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text("Clients: ${order.formattedClients}", style = MaterialTheme.typography.bodySmall)
            Text("Total Qty: %.2f, Total Price: ₹%.2f".format(order.totalQuantity, order.totalPrice), style = MaterialTheme.typography.bodySmall)
            Text("Vendor: ${order.vendor}", style = MaterialTheme.typography.bodySmall)
            Text("Updated by: ${order.updatedByName}", style = MaterialTheme.typography.bodySmall)
            Text("On: $dateTime", style = MaterialTheme.typography.labelSmall)
        }
    }
}
