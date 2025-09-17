package com.nusync

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.nusync.ui.theme.NuSyncTheme
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>
    private val TAG = "MainActivity"

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT)
                        .show()
                } else {
                    Toast.makeText(
                        this,
                        "Notification permission denied. You may not receive important updates.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            val token = task.result
            Log.d(TAG, "FCM Token: $token")

            val userID = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            val phoneFromIntent = intent.getStringExtra("phone") ?: ""

            enableEdgeToEdge()
            setContent {
                NuSyncTheme {
                    MainScreen(userID, phoneFromIntent, this, requestPermissionLauncher)
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainScreen(
    userID: String,
    phoneFromIntent: String,
    activity: ComponentActivity,
    requestPermissionLauncher: ActivityResultLauncher<String>
) {
    var role by remember { mutableStateOf<String?>(null) }
    var redirectTo by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    LaunchedEffect(Unit) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = android.Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(permission)
            }
        }

        val userRef = FirebaseDatabase.getInstance().reference.child("Users").child(userID)
        val flatsRef = FirebaseDatabase.getInstance().getReference("Flats")
        val buildingsRef = FirebaseDatabase.getInstance().getReference("Buildings")

        userRef.keepSynced(true)
        flatsRef.keepSynced(true)
        buildingsRef.keepSynced(true)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val fetchedRole = snapshot.child("role").getValue(String::class.java)
                    role = fetchedRole
                } else {
                    redirectTo = if (phoneFromIntent.isBlank()) "PhoneLogin" else "CreateNewUser"
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    Log.i("MainActivity", "Role: $role")
    Log.i("MainActivity", "redirectTo: $redirectTo")

    when (redirectTo) {
        "PhoneLogin" -> {
            LaunchedEffect(Unit) {
                Firebase.auth.signOut()
                context.startActivity(Intent(context, PhoneLogin::class.java))
                activity.finish()
                Toast.makeText(context, "You are logged out successfully.", Toast.LENGTH_SHORT).show()
            }
        }

        "CreateNewUser" -> {
            LaunchedEffect(Unit) {
                val intent = Intent(context, CreateNewUserActivity::class.java)
                intent.putExtra("phone", phoneFromIntent)
                context.startActivity(intent)
                activity.finish()
            }
        }

        null -> {
            if (role == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                when (role) {
                    "Admin" -> UI({ ClientOrder() }, { AdminPanel(activity) }, activity)
                    "Employee" -> UI({}, { EmployeePanel(activity) }, activity)
                    else -> Text("Unsupported role: $role")
                }
            }
        }
    }
}

@Composable
fun UI(clientOrderUI: @Composable () -> Unit, adminPanel: @Composable () -> Unit, activity: ComponentActivity) {
    TopBarForLazyColumns(stringResource(R.string.app_name)) {

        val textColor = MaterialTheme.colorScheme.onSurface

        clientOrderUI()

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Tasks",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 24.sp,
            color = textColor
        )

        OutlinedCard(
            border = BorderStroke(1.dp, Color.Gray),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                adminPanel()
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun EmployeePanel(activity: ComponentActivity) {
    val context = LocalContext.current
    Column {
        Row {
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, EditUserOrdersActivity::class.java)
                    context.startActivity(intent)
                }, "Edit")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    FirebaseAuth.getInstance().signOut()
                    context.startActivity(Intent(context, PhoneLogin::class.java))
                    activity.finish()
                    Toast.makeText(context, "You are logged out successfully.", Toast.LENGTH_SHORT).show()
                }, "Log out")
            }
        }
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, RequirementsActivity::class.java)
                    context.startActivity(intent)
                }, "Requirement")
            }
        }
    }
}


@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun AdminPanel(activity: ComponentActivity) {
    val context = LocalContext.current
    Column {
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, CompletedOrdersActivity::class.java)
                    intent.putExtra("type", "Client")
                    context.startActivity(intent)
                }, "Completed")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, PartiallyCompletedOrderActivity::class.java)
                    intent.putExtra("type", "Vendor")
                    context.startActivity(intent)
                }, "Partial")
            }
        }
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, EditUserOrdersActivity::class.java)
                    context.startActivity(intent)
                }, "Edit")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackgroundEnabled(
                    toDoFunction = {
                        downloadAllDataAsExcel(context)
                    },
                    label = "Download",
                    context = context
                )
            }
        }
        Row {
            val context = LocalContext.current
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, RequirementsActivity::class.java)
                    context.startActivity(intent)
                }, "Requirement")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    FirebaseAuth.getInstance().signOut()
                    context.startActivity(Intent(context, PhoneLogin::class.java))
                    activity.finish()
                    Toast.makeText(context, "You are logged out successfully.", Toast.LENGTH_SHORT).show()
                }, "Log out")
            }
        }
    }
}

@Composable
fun LensType(string: String) {
    val context = LocalContext.current
    Column {
        Row {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, LensOrderActivity::class.java)
                    intent.putExtra("lensType", "SingleVision")
                    context.startActivity(intent)
                }, "S. V.")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, LensOrderActivity::class.java)
                    intent.putExtra("lensType", "Kryptok")
                    context.startActivity(intent)
                }, "Kryptok")
            }
        }
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                WrapButtonWithBackground({
                    val intent = Intent(context, LensOrderActivity::class.java)
                    intent.putExtra("lensType", "Progressive")
                    context.startActivity(intent)
                }, "Progressive")
            }
            Spacer(modifier = Modifier.width(16.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {}
        }
    }
}

@Composable
fun ClientOrder() {

    val textColor = MaterialTheme.colorScheme.onSurface
    Spacer(modifier = Modifier.height(16.dp))

    Text(
        text = "Client Orders",
        style = MaterialTheme.typography.titleMedium,
        fontSize = 24.sp,
        color = textColor
    )

    OutlinedCard(
        border = BorderStroke(1.dp, Color.Gray),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            LensType("Client")
        }
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
fun downloadAllDataAsExcel(context: Context) {
    val databaseRef = FirebaseDatabase.getInstance().reference

    databaseRef.get().addOnSuccessListener { snapshot ->
        val workbook = XSSFWorkbook()

        val userMap = mutableMapOf<String, String>()
        snapshot.child("Users").children.forEach { userSnapshot ->
            val userId = userSnapshot.key
            val userName = userSnapshot.child("name").value.toString()
            if (userId != null) {
                userMap[userId] = userName
            }
        }

        val idealHeaderOrder = listOf(
            "ID", "timestamp", "client", "quantity", "price", "sphere", "cylinder", "axis",
            "coating", "coatingType", "type", "material", "vendor", "updatedBy", "fulfilledQty"
        )

        snapshot.children.forEach { node ->
            if (node.key == "Users" || !node.hasChildren()) return@forEach

            val sheet = workbook.createSheet(node.key)
            var rowIndex = 0

            val allKeysForSheet = mutableSetOf<String>()
            node.children.forEach { dataEntry ->
                allKeysForSheet.add("ID")
                (dataEntry.value as? Map<String, Any>)?.keys?.let { allKeysForSheet.addAll(it) }
            }

            if (allKeysForSheet.contains("orders")) {
                allKeysForSheet.add("client")
                allKeysForSheet.add("quantity")
            }
            allKeysForSheet.remove("orders")

            val finalSheetHeaders = mutableListOf<String>()
            idealHeaderOrder.forEach { header ->
                if (allKeysForSheet.contains(header)) {
                    finalSheetHeaders.add(header)
                }
            }
            val remainingKeys = allKeysForSheet.filter { it !in finalSheetHeaders }.sorted()
            finalSheetHeaders.addAll(remainingKeys)

            val headerToIndexMap = finalSheetHeaders.withIndex().associate { it.value to it.index }

            val headerRow = sheet.createRow(rowIndex++)
            finalSheetHeaders.forEachIndexed { index, headerText ->
                headerRow.createCell(index).setCellValue(headerText)
            }

            node.children.forEach { dataEntrySnapshot ->
                val row = sheet.createRow(rowIndex++)
                val rowData = dataEntrySnapshot.value as? Map<String, Any> ?: emptyMap()

                finalSheetHeaders.forEach { header ->
                    val columnIndex = headerToIndexMap[header] ?: return@forEach
                    val value: String = when (header) {
                        "ID" -> dataEntrySnapshot.key ?: ""
                        "timestamp" -> {
                            val ts = rowData["timestamp"] as? Long
                            if (ts != null) SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date(ts)) else ""
                        }
                        "client", "quantity" -> {
                            val ordersObject = rowData["orders"] as? Map<String, Any>
                            if (ordersObject != null && ordersObject.isNotEmpty()) {
                                val clientKey = ordersObject.keys.first()
                                val orderInfo = ordersObject[clientKey]
                                if (header == "client") {
                                    clientKey
                                } else {
                                    when (orderInfo) {
                                        is Map<*, *> -> (orderInfo["quantity"] ?: "").toString()
                                        else -> orderInfo.toString()
                                    }
                                }
                            } else ""
                        }
                        "updatedBy" -> {
                            val uid = rowData["updatedBy"] as? String
                            if (uid != null) userMap[uid] ?: uid else ""
                        }
                        else -> rowData[header]?.toString() ?: ""
                    }
                    row.createCell(columnIndex).setCellValue(value)
                }
            }
        }

        try {
            val date = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val fileName = "NuSync_$date.xlsx"

            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val fileUri = resolver.insert(collection, contentValues)

            if (fileUri != null) {
                resolver.openOutputStream(fileUri)?.use { workbook.write(it) }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(fileUri, contentValues, null, null)

                Toast.makeText(context, "Excel saved to Downloads: $fileName", Toast.LENGTH_LONG).show()

                snapshot.children.forEach { child ->
                    if (child.key != "Users") {
                        databaseRef.child(child.key!!).removeValue()
                    }
                }
                Toast.makeText(context, "Data cleared", Toast.LENGTH_SHORT).show()

            } else {
                Toast.makeText(context, "Failed to create file", Toast.LENGTH_LONG).show()
            }

            workbook.close()

        } catch (e: Exception) {
            Toast.makeText(context, "Error saving Excel: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

    }.addOnFailureListener {
        Toast.makeText(context, "Firebase fetch failed: ${it.message}", Toast.LENGTH_LONG).show()
    }
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview
@Composable
fun MainActivityPreview() {
    val activity = ComponentActivity()
    UI({ ClientOrder() }, { AdminPanel(activity) }, activity)
}

@RequiresApi(Build.VERSION_CODES.Q)
@Preview
@Composable
fun MainActivityPreview2() {
    val activity = ComponentActivity()
    UI({}, { EmployeePanel(activity)}, activity)
}