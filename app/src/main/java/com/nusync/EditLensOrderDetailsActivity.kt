package com.nusync

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.nusync.ui.theme.NuSyncTheme
import com.nusync.utils.KryptokLens
import com.nusync.utils.NotificationHelper
import com.nusync.utils.ProgressiveLens
import com.nusync.utils.SingleVisionLens
import com.nusync.utils.kryptokLenses
import com.nusync.utils.progressiveLenses
import com.nusync.utils.singleVisionLenses

class EditLensOrderDetailsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get firebaseKey and commentText from the Intent
        val firebaseKey = intent.getStringExtra("firebaseKey")
        val commentText = intent.getStringExtra("commentText")

        setContent {
            NuSyncTheme {
                TopBar("Edit Lens Details") { // Assuming TopBar is available
                    EditLensDetailsScreen(
                        firebaseKey = firebaseKey,
                        initialCommentText = commentText
                    )
                }
            }
        }
    }
}

@Composable
fun EditLensDetailsScreen(firebaseKey: String?, initialCommentText: String?) {
    val context = LocalContext.current
    val db = remember { FirebaseDatabase.getInstance() }
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUid = auth.currentUser?.uid ?: "anonymous_user"

    var originalFirebaseData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedLensType by remember { mutableStateOf("") }
    var selectedCoating by remember { mutableStateOf("") }
    var customCoating by remember { mutableStateOf("") }
    var selectedCoatingType by remember { mutableStateOf("") }
    var customCoatingType by remember { mutableStateOf("") }
    var selectedMaterial by remember { mutableStateOf("") }
    var customMaterial by remember { mutableStateOf("") }

    var sphere by remember { mutableStateOf("0.00") }
    var cylinder by remember { mutableStateOf("0.00") }
    var axis by remember { mutableStateOf("0") }
    var add by remember { mutableStateOf("0.75") }
    var lensSpecificType by remember { mutableStateOf("") }

    // Track if the original item had a comment for navigation (Suggestion 1)
    var hadOriginalComment by remember { mutableStateOf(false) }
    var currentUserName by remember { mutableStateOf("Unknown User") } // To pass to notification

    // Fetch user name for notifications
    LaunchedEffect(currentUid) {
        if (currentUid != "anonymous_user") {
            db.getReference("Users").child(currentUid).child("name").get()
                .addOnSuccessListener { snapshot ->
                    currentUserName = snapshot.getValue(String::class.java) ?: "Unknown User"
                }
                .addOnFailureListener {
                    Log.e("EditLensDetailsScreen", "Failed to fetch user name for notification", it)
                }
        }
    }

    DisposableEffect(firebaseKey) {
        if (firebaseKey == null) {
            Toast.makeText(context, "No lens order selected for editing.", Toast.LENGTH_SHORT).show()
            isLoading = false
            return@DisposableEffect onDispose {}
        }

        val lensRef = db.getReference("GroupedLensOrders").child(firebaseKey)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as? Map<String, Any>
                if (data != null) {
                    originalFirebaseData = data
                    selectedLensType = data["type"] as? String ?: "Unknown"
                    selectedCoating = data["coating"] as? String ?: ""
                    selectedCoatingType = data["coatingType"] as? String ?: ""
                    selectedMaterial = data["material"] as? String ?: ""
                    sphere = data["sphere"] as? String ?: "0.00"
                    cylinder = data["cylinder"] as? String ?: "0.00"
                    axis = data["axis"] as? String ?: "0"
                    add = data["add"] as? String ?: "0.75"
                    lensSpecificType = data["lensSpecificType"] as? String ?: ""

                    // Determine if the original item had a comment (Suggestion 1)
                    hadOriginalComment = snapshot.child("comment").exists()

                    // ... (Your existing logic for checking and setting custom values) ...
                    val currentLensOptions = when (selectedLensType) {
                        "SingleVision" -> singleVisionLenses
                        "Kryptok" -> kryptokLenses
                        "Progressive" -> progressiveLenses
                        else -> emptyList()
                    }

                    if (currentLensOptions.isNotEmpty()) {
                        val allCoatings = currentLensOptions.map {
                            when (it) {
                                is SingleVisionLens -> it.coating
                                is KryptokLens -> it.coating
                                is ProgressiveLens -> it.coating
                                else -> ""
                            }
                        }.toSet()
                        if (selectedCoating.isNotBlank() && selectedCoating !in allCoatings) {
                            customCoating = selectedCoating
                            selectedCoating = "Other"
                        }

                        val allCoatingTypes = currentLensOptions.flatMap {
                            when (it) {
                                is SingleVisionLens -> it.coatingTypes
                                is KryptokLens -> it.coatingTypes
                                is ProgressiveLens -> it.coatingTypes
                                else -> emptyList()
                            }
                        }.toSet()
                        if (selectedCoatingType.isNotBlank() && selectedCoatingType !in allCoatingTypes) {
                            customCoatingType = selectedCoatingType
                            selectedCoatingType = "Other"
                        }

                        val allMaterials = currentLensOptions.flatMap {
                            when (it) {
                                is SingleVisionLens -> it.lensMaterials
                                is KryptokLens -> it.lensMaterials
                                is ProgressiveLens -> it.lensMaterials
                                else -> emptyList()
                            }
                        }.toSet()
                        if (selectedMaterial.isNotBlank() && selectedMaterial !in allMaterials) {
                            customMaterial = selectedMaterial
                            selectedMaterial = "Other"
                        }
                    }

                    Log.d("EditScreen", "Data loaded: $data")
                } else {
                    Toast.makeText(context, "Lens order not found.", Toast.LENGTH_SHORT).show()
                }
                isLoading = false
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("EditScreen", "Firebase data load failed", error.toException())
                isLoading = false
            }
        }
        lensRef.addListenerForSingleValueEvent(listener)

        onDispose {}
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (firebaseKey == null || originalFirebaseData == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Error: Could not load lens order details.")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    initialCommentText?.let {
                        Text(
                            text = "Original Comment: \"$it\"",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    val lensOptions = when (selectedLensType) {
                        "SingleVision" -> singleVisionLenses
                        "Kryptok" -> kryptokLenses
                        "Progressive" -> progressiveLenses
                        else -> emptyList()
                    }

                    val currentLensModel = lensOptions.firstOrNull()

                    // Using the new TextField for display purposes as requested previously
                    TextField(
                        label = "Lens Type",
                        textValue = selectedLensType, // Use textValue
                        onValueChange = {}, // No-op as it's not editable
                        enabled = false // Disable editing
                    )

                    currentLensModel?.let { lens ->
                        when (selectedLensType) {
                            "SingleVision" -> {
                                val svLens = lens as SingleVisionLens
                                val sphereOptions = generateSphereOptions(svLens.sphereRange)
                                val cylinderOptions = generateCylinderOptions(svLens.cylinderRange.first.toDouble()..svLens.cylinderRange.last.toDouble())

                                val coatingOptions = (singleVisionLenses.map { it.coating }.toSet() + "Other").toList()
                                DropdownTextField("Coating", selectedCoating, coatingOptions) { selectedCoating = it }
                                if (selectedCoating == "Other") {
                                    TextField("Enter custom coating", customCoating, { customCoating = it })
                                }

                                val coatingTypeOptions = (svLens.coatingTypes.toSet() + "Other").toList()
                                DropdownTextField("Coating Type", selectedCoatingType, coatingTypeOptions) { selectedCoatingType = it }
                                if (selectedCoatingType == "Other") {
                                    TextField("Enter custom coating type", customCoatingType, { customCoatingType = it })
                                }

                                val materialOptions = (svLens.lensMaterials.toSet() + "Other").toList()
                                DropdownTextField("Lens Material", selectedMaterial, materialOptions) { selectedMaterial = it }
                                if (selectedMaterial == "Other") {
                                    TextField("Enter custom material", customMaterial, { customMaterial = it })
                                }

                                // Replaced DropdownTextField with SphereCylinderSelector
                                SphereCylinderSelector("Sphere", sphere, sphereOptions) { sphere = it }
                                SphereCylinderSelector("Cylinder", cylinder, cylinderOptions) { cylinder = it }
                            }

                            "Kryptok" -> {
                                val ktLens = lens as KryptokLens
                                // Adjusted sphereRange to match generateSphereOptions (IntRange)
                                val sphereOptions = generateSphereOptions(ktLens.sphereRange.start.toInt()..ktLens.sphereRange.endInclusive.toInt())
                                val cylinderOptions = generateCylinderOptions(ktLens.cylinderRange)
                                val axisOptions = ktLens.axisOptions.map { it.toString() }
                                val addOptions = generateAddOptions(ktLens.addRange)

                                val coatingOptions = (kryptokLenses.map { it.coating }.toSet() + "Other").toList()
                                DropdownTextField("Coating", selectedCoating, coatingOptions) { selectedCoating = it }
                                if (selectedCoating == "Other") {
                                    TextField("Enter custom coating", customCoating, { customCoating = it })
                                }

                                val coatingTypeOptions = (ktLens.coatingTypes.toSet() + "Other").toList()
                                DropdownTextField("Coating Type", selectedCoatingType, coatingTypeOptions) { selectedCoatingType = it }
                                if (selectedCoatingType == "Other") {
                                    TextField("Enter custom coating type", customCoatingType, { customCoatingType = it })
                                }

                                val materialOptions = (ktLens.lensMaterials.toSet() + "Other").toList()
                                DropdownTextField("Lens Material", selectedMaterial, materialOptions) { selectedMaterial = it }
                                if (selectedMaterial == "Other") {
                                    TextField("Enter custom material", customMaterial, { customMaterial = it })
                                }

                                DropdownTextField("Axis", axis, axisOptions) { axis = it }
                                DropdownTextField("Add", add, addOptions) { add = it }
                                // Replaced DropdownTextField with SphereCylinderSelector
                                SphereCylinderSelector("Sphere", sphere, sphereOptions) { sphere = it }
                                SphereCylinderSelector("Cylinder", cylinder, cylinderOptions) { cylinder = it }
                            }

                            "Progressive" -> {
                                val pgLens = lens as ProgressiveLens
                                // Adjusted sphereRange to match generateSphereOptions (IntRange)
                                val sphereOptions = generateSphereOptions(pgLens.sphereRange.start.toInt()..pgLens.sphereRange.endInclusive.toInt())
                                val cylinderOptions = generateCylinderOptions(pgLens.cylinderRange)
                                val axisOptions = pgLens.axisOptions.map { it.toString() }
                                val addOptions = generateAddOptions(pgLens.addRange)

                                val coatingOptions = (progressiveLenses.map { it.coating }.toSet() + "Other").toList()
                                DropdownTextField("Coating", selectedCoating, coatingOptions) { selectedCoating = it }
                                if (selectedCoating == "Other") {
                                    TextField("Enter custom coating", customCoating, { customCoating = it })
                                }

                                val coatingTypeOptions = (pgLens.coatingTypes.toSet() + "Other").toList()
                                DropdownTextField("Coating Type", selectedCoatingType, coatingTypeOptions) { selectedCoatingType = it }
                                if (selectedCoatingType == "Other") {
                                    TextField("Enter custom coating type", customCoatingType, { customCoatingType = it })
                                }

                                val materialOptions = (pgLens.lensMaterials.toSet() + "Other").toList()
                                DropdownTextField("Lens Material", selectedMaterial, materialOptions) { selectedMaterial = it }
                                if (selectedMaterial == "Other") {
                                    TextField("Enter custom material", customMaterial, { customMaterial = it })
                                }

                                DropdownTextField("Lens Specific Type", lensSpecificType, pgLens.lensType) { lensSpecificType = it }
                                DropdownTextField("Axis", axis, axisOptions) { axis = it }
                                DropdownTextField("Add", add, addOptions) { add = it }
                                // Replaced DropdownTextField with SphereCylinderSelector
                                SphereCylinderSelector("Sphere", sphere, sphereOptions) { sphere = it }
                                SphereCylinderSelector("Cylinder", cylinder, cylinderOptions) { cylinder = it }
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    WrapButtonWithBackground(toDoFunction = {

                        val currentUser = auth.currentUser
                        if (currentUser == null) {
                            Log.e("EditScreen", "FirebaseAuth.getInstance().currentUser is NULL right before sending notification.")
                            Toast.makeText(context, "Authentication required for this action.", Toast.LENGTH_LONG).show()
                            return@WrapButtonWithBackground
                        } else {
                            Log.d("EditScreen", "FirebaseAuth.getInstance().currentUser is NOT NULL. UID: ${currentUser.uid}, Email: ${currentUser.email}")
                        }

                        val finalCoating = if (selectedCoating == "Other") customCoating else selectedCoating
                        val finalCoatingType = if (selectedCoatingType == "Other") customCoatingType else selectedCoatingType
                        val finalMaterial = if (selectedMaterial == "Other") customMaterial else selectedMaterial

                        val ordersMapRaw = originalFirebaseData?.get("orders") as? Map<String, Number> ?: emptyMap()
                        val ordersMap = ordersMapRaw.mapValues { it.value.toDouble() }

                        val partiallyAllottedQty = (originalFirebaseData?.get("partiallyAllottedQty") as? Number)?.toDouble() ?: 0.0

                        val newLensKey = buildString {
                            append("$selectedLensType-$finalCoating-$finalCoatingType")
                            if (finalMaterial.isNotBlank()) {
                                append("-$finalMaterial")
                            }
                            append("-$sphere-$cylinder")
                            if (selectedLensType != "SingleVision") append("-$axis-$add")
                            if (selectedLensType == "Progressive") append("-$lensSpecificType")
                        }.replace(".", "_").replace(" ", "") // Replace dots and spaces for valid Firebase keys


                        val newGroupedData = mutableMapOf<String, Any>(
                            "type" to selectedLensType,
                            "coating" to finalCoating,
                            "coatingType" to finalCoatingType,
                            "material" to finalMaterial,
                            "sphere" to sphere,
                            "cylinder" to cylinder,
                            "orders" to ordersMap,
                            "partiallyAllottedQty" to partiallyAllottedQty
                        )

                        if (selectedLensType != "SingleVision") {
                            newGroupedData["axis"] = axis
                            newGroupedData["add"] = add
                        }

                        if (selectedLensType == "Progressive") {
                            newGroupedData["lensSpecificType"] = lensSpecificType
                        }

                        // Get the current comment data if it exists, to carry it over
                        val existingComment = originalFirebaseData?.get("comment") as? Map<String, Any>

                        if (existingComment != null) {
                            newGroupedData["comment"] = existingComment
                        }

                        db.getReference("GroupedLensOrders").child(newLensKey).setValue(newGroupedData)
                            .addOnSuccessListener {
                                Log.d("EditScreen", "New lens order saved at $newLensKey")

                                NotificationHelper.sendAdminNotification(
                                    context,
                                    "UPDATED",
                                    newLensKey, // You might want to format this into a more readable detail for the notification
                                    currentUserName
                                )

                                firebaseKey.let { oldKey ->
                                    if (oldKey != newLensKey) { // Only delete if the key actually changed
                                        db.getReference("GroupedLensOrders").child(oldKey).removeValue()
                                            .addOnSuccessListener {
                                                Log.d("EditScreen", "Old lens order deleted: $oldKey")
                                                Toast.makeText(context, "Item updated successfully!", Toast.LENGTH_SHORT).show() // Suggestion 2
                                                (context as? Activity)?.finish() // Suggestion 1: Go back to previous activity
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("EditScreen", "Failed to delete old lens order $oldKey: ${e.message}", e)
                                                Toast.makeText(context, "Updated lens order, but failed to delete old one.", Toast.LENGTH_LONG).show() // Suggestion 2
                                                (context as? Activity)?.finish() // Suggestion 1: Still go back even if old delete fails
                                            }
                                    } else {
                                        Toast.makeText(context, "Item updated successfully! (Key did not change)", Toast.LENGTH_SHORT).show()
                                        (context as? Activity)?.finish()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("EditScreen", "Failed to save new lens order: ${e.message}", e)
                                Toast.makeText(context, "Failed to save updated lens order: ${e.message}", Toast.LENGTH_SHORT).show() // Suggestion 2
                            }
                    }, label = "Save Changes")
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SphereCylinderSelector2(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    var lazyRowWidthPx by remember { mutableStateOf(0) } // State to hold the measured width of the LazyRow

    // Define item dimensions here so they are accessible throughout the Composable
    val itemWidthDp = 70.dp
    val itemSpacingDp = 8.dp

    LaunchedEffect(selectedValue, options, lazyRowWidthPx) {
        // Ensure options are not empty and LazyRow has been measured
        if (options.isNotEmpty() && lazyRowWidthPx > 0) {
            val targetIndex = options.indexOf(selectedValue)

            if (targetIndex != -1) {
                // Measure item dimensions in pixels
                val itemWidthPx = with(density) { itemWidthDp.toPx() }
                val itemSpacingPx = with(density) { itemSpacingDp.toPx() }

                // Calculate the start position (in pixels) of the target item if the row were at scroll 0
                val targetItemStartPositionPx = targetIndex * (itemWidthPx + itemSpacingPx)

                // Calculate the desired scroll position to center the item
                // This is (target item's center) - (half of LazyRow's width)
                val desiredScrollPosition = targetItemStartPositionPx + (itemWidthPx / 2) - (lazyRowWidthPx / 2)

                // The `scrollToItem` offset is how many pixels to scroll *from the start of the item*
                // to make its start visible. We need to tell it to scroll to the beginning of the item
                // such that the item's center is at the center of the LazyRow.
                listState.scrollToItem(targetIndex, desiredScrollPosition.toInt())
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .onGloballyPositioned { coordinates ->
                    // Capture the actual width of the LazyRow when it's laid out
                    lazyRowWidthPx = coordinates.size.width
                },
            horizontalArrangement = Arrangement.spacedBy(itemSpacingDp), // Use the defined constant
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(options) { option ->
                val isSelected = option == selectedValue
                Card(
                    modifier = Modifier
                        .width(itemWidthDp) // Use the defined constant
                        .height(48.dp)
                        .clickable { onValueSelected(option) },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = option,
                            style = if (isSelected) MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.primary)
                            else MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}