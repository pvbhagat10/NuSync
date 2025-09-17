package com.nusync

import android.os.Bundle
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nusync.ui.theme.NuSyncTheme
import kotlin.math.abs

class StocksActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                TopBar("Stocks") {
                    InventoryStocksScreen()
                }
            }
        }
    }
}

@Composable
fun InventoryStocksScreen() {
    val inventory = remember { mutableStateListOf<Pair<String, Int>>() }
    val databaseRef = FirebaseDatabase.getInstance().getReference("lensInventory")
    databaseRef.keepSynced(true)

    LaunchedEffect(true) {
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                inventory.clear()
                for (itemSnapshot in snapshot.children) {
                    val detail = itemSnapshot.key ?: continue
                    val quantity = itemSnapshot.child("quantity").getValue(Int::class.java) ?: 0
                    if (quantity < 0) {
                        inventory.add(detail to abs(quantity))
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error if needed
            }
        })
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(inventory) { (detail, quantity) ->
            InventoryStocksItem(detail = detail, quantity = quantity)
        }
    }
}

@Composable
fun InventoryStocksItem(detail: String, quantity: Int) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            fun clean(value: String): String = value.replace("_", ".")
            fun clean2(value: String): String = clean(value).replace(";", " | ")

            val inventoryDetail = clean2(detail)
            Text(text = inventoryDetail, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Quantity: $quantity", style = MaterialTheme.typography.bodySmall)
        }
    }
}