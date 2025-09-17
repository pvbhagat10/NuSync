package com.nusync

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.*
import com.nusync.ui.theme.NuSyncTheme

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val type = intent.getStringExtra("type") ?: "Client"

        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                OrderHistoryScreen(
                    heading = if (type == "Client") "Client Orders" else "Vendor Orders",
                    type = type
                )
            }
        }
    }
}

data class LensOrder2(
    val details: String = "",
    val partyName: String = "",
    val price: String = "",
    val quantity: Int = 0,
    val time: String = "",
    val updatedBy: String = ""
)

@Composable
fun OrderHistoryScreen(heading: String, type: String) {
    val context = LocalContext.current
    val orders = remember { mutableStateListOf<LensOrder2>() }

    val dbPath = if (type == "Client") "ClientLensOrdersHistory" else "VendorLensOrdersHistory"

    LaunchedEffect(type) {
        val database = FirebaseDatabase.getInstance().getReference(dbPath)
        database.keepSynced(true)
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                orders.clear()
                for (child in snapshot.children) {
                    val order = child.getValue(LensOrder2::class.java)
                    order?.let { orders.add(it) }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    TopBar3(heading = heading) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                OrderCard(order, type)
            }
        }
    }
}

@Composable
fun OrderCard(order: LensOrder2, type: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        fun clean(value: String): String = value.replace("_", ".")
        fun clean2(value: String): String = clean(value).replace(";", " | ")

        val inventoryDetail = clean2(order.details)
        Column(modifier = Modifier.padding(12.dp)) {
            Text("$type: ${order.partyName}", fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Details: $inventoryDetail")
            Text("Quantity: ${order.quantity}")
            Text("Price: ₹${order.price}")
            Text("Updated By: ${order.updatedBy}")
            Text("Time: ${order.time}")
        }
    }
}