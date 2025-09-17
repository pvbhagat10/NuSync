package com.nusync

import android.content.Intent
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.nusync.ui.theme.Blue2
import com.nusync.ui.theme.NuSyncTheme
import com.nusync.ui.theme.White
import com.nusync.utils.KryptokLens
import com.nusync.utils.ProgressiveLens
import com.nusync.utils.SingleVisionLens
import com.nusync.utils.kryptokLenses
import com.nusync.utils.progressiveLenses
import com.nusync.utils.singleVisionLenses

class LensOrderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lensType = intent.getStringExtra("lensType") ?: ""

        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                TopBar(lensType) {
                    LensFormScreen(lensType)
                }
            }
        }
    }
}

@Composable
fun LensFormScreen(lensType: String) {
    val context = LocalContext.current
    var selectedCoating by remember { mutableStateOf("") }
    var customCoating by remember { mutableStateOf("") }

    var selectedCoatingType by remember { mutableStateOf("") }
    var customCoatingType by remember { mutableStateOf("") }

    var selectedMaterial by remember { mutableStateOf("") }
    var customMaterial by remember { mutableStateOf("") }

    var selectedLensType by remember { mutableStateOf("") }

    var sphere by remember { mutableStateOf("0.00") }
    var cylinder by remember { mutableStateOf("0.00") }
    var axis by remember { mutableStateOf("0") }
    var add by remember { mutableStateOf("0.75") }
    var quantity by remember { mutableStateOf("") }
    var partyName by remember { mutableStateOf("") }

    val lensOptions = when (lensType) {
        "SingleVision" -> singleVisionLenses
        "Kryptok" -> kryptokLenses
        "Progressive" -> progressiveLenses
        else -> emptyList()
    }
    val selectedLens = lensOptions.firstOrNull()

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            selectedLens?.let {
                when (lensType) {
                    "SingleVision" -> {
                        val lens = it as SingleVisionLens
                        val sphereOptions = generateSphereOptions(lens.sphereRange)
                        val cylinderOptions = generateCylinderOptions(lens.cylinderRange.first.toDouble()..lens.cylinderRange.last.toDouble())

                        val coatingOptions = (lensOptions.map { (it as SingleVisionLens).coating }.toSet() + "Other").toList()
                        DropdownTextField("Coating", selectedCoating, coatingOptions) { selectedCoating = it }
                        if (selectedCoating == "Other") {
                            TextField("Enter custom coating", customCoating, { customCoating = it })
                        }

                        val coatingTypeOptions = (lens.coatingTypes.toSet() + "Other").toList()
                        DropdownTextField("Coating Type", selectedCoatingType, coatingTypeOptions) { selectedCoatingType = it }
                        if (selectedCoatingType == "Other") {
                            TextField("Enter custom coating type", customCoatingType, { customCoatingType = it })
                        }

                        val materialOptions = (lens.lensMaterials.toSet() + "Other").toList()
                        DropdownTextField("Lens Material", selectedMaterial, materialOptions) { selectedMaterial = it }
                        if (selectedMaterial == "Other") {
                            TextField("Enter custom material", customMaterial, { customMaterial = it })
                        }

                        SphereCylinderSelector("Sphere", sphere, sphereOptions) { sphere = it }
                        SphereCylinderSelector("Cylinder", cylinder, cylinderOptions) { cylinder = it }
                    }

                    "Kryptok" -> {
                        val lens = it as KryptokLens
                        val sphereOptions = generateSphereOptions(lens.sphereRange.start.toInt()..lens.sphereRange.endInclusive.toInt())
                        val cylinderOptions = generateCylinderOptions(lens.cylinderRange)
                        val axisOptions = lens.axisOptions.map { it.toString() }
                        val addOptions = generateAddOptions(lens.addRange)

                        val coatingOptions = (lensOptions.map { (it as KryptokLens).coating }.toSet() + "Other").toList()
                        DropdownTextField("Coating", selectedCoating, coatingOptions) { selectedCoating = it }
                        if (selectedCoating == "Other") {
                            TextField("Enter custom coating", customCoating, { customCoating = it })
                        }

                        val coatingTypeOptions = (lens.coatingTypes.toSet() + "Other").toList()
                        DropdownTextField("Coating Type", selectedCoatingType, coatingTypeOptions) { selectedCoatingType = it }
                        if (selectedCoatingType == "Other") {
                            TextField("Enter custom coating type", customCoatingType, { customCoatingType = it })
                        }

                        val materialOptions = (lens.lensMaterials.toSet() + "Other").toList()
                        DropdownTextField("Lens Material", selectedMaterial, materialOptions) { selectedMaterial = it }
                        if (selectedMaterial == "Other") {
                            TextField("Enter custom material", customMaterial, { customMaterial = it })
                        }

                        DropdownTextField("Axis", axis, axisOptions) { axis = it }
                        DropdownTextField("Add", add, addOptions) { add = it }
                        SphereCylinderSelector("Sphere", sphere, sphereOptions) { sphere = it }
                        SphereCylinderSelector("Cylinder", cylinder, cylinderOptions) { cylinder = it }
                    }

                    "Progressive" -> {
                        val lens = it as ProgressiveLens
                        val sphereOptions = generateSphereOptions(lens.sphereRange.start.toInt()..lens.sphereRange.endInclusive.toInt())
                        val cylinderOptions = generateCylinderOptions(lens.cylinderRange)
                        val axisOptions = lens.axisOptions.map { it.toString() }
                        val addOptions = generateAddOptions(lens.addRange)

                        val coatingOptions = (lensOptions.map { (it as ProgressiveLens).coating }.toSet() + "Other").toList()
                        DropdownTextField("Coating", selectedCoating, coatingOptions) { selectedCoating = it }
                        if (selectedCoating == "Other") {
                            TextField("Enter custom coating", customCoating, { customCoating = it })
                        }

                        val coatingTypeOptions = (lens.coatingTypes.toSet() + "Other").toList()
                        DropdownTextField("Coating Type", selectedCoatingType, coatingTypeOptions) { selectedCoatingType = it }
                        if (selectedCoatingType == "Other") {
                            TextField("Enter custom coating type", customCoatingType, { customCoatingType = it })
                        }

                        val materialOptions = (lens.lensMaterials.toSet() + "Other").toList()
                        DropdownTextField("Lens Material", selectedMaterial, materialOptions) { selectedMaterial = it }
                        if (selectedMaterial == "Other") {
                            TextField("Enter custom material", customMaterial, { customMaterial = it })
                        }

                        DropdownTextField("Lens Type", selectedLensType, lens.lensType) { selectedLensType = it }
                        DropdownTextField("Axis", axis, axisOptions) { axis = it }
                        DropdownTextField("Add", add, addOptions) { add = it }
                        SphereCylinderSelector("Sphere", sphere, sphereOptions) { sphere = it }
                        SphereCylinderSelector("Cylinder", cylinder, cylinderOptions) { cylinder = it }
                    }
                }

                TextField("Client Name", partyName, { partyName = it })

                OutlinedTextField(
                    value = quantity,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*$"))) {
                            quantity = newValue
                        }
                    },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                WrapButtonWithBackground(toDoFunction = {
                    val finalCoating = if (selectedCoating == "Other") customCoating else selectedCoating
                    val finalCoatingType = if (selectedCoatingType == "Other") customCoatingType else selectedCoatingType

                    val finalMaterial = when (selectedMaterial) {
                        "Other" -> customMaterial
                        "None" -> ""
                        else -> selectedMaterial
                    }

                    val quantityDouble: Double? = quantity.toDoubleOrNull()

                    val database = FirebaseDatabase.getInstance().reference
                    val auth = FirebaseAuth.getInstance()
                    val userId = auth.currentUser?.uid

                    if (userId == null) {
                        Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
                        return@WrapButtonWithBackground
                    }

                    val lensKey = buildString {
                        append("$lensType-$finalCoating-$finalCoatingType")
                        if (finalMaterial.isNotBlank()) {
                            append("-$finalMaterial")
                        }
                        append("-$sphere-$cylinder")
                        if (lensType != "SingleVision") append("-$axis-$add")
                        if (lensType == "Progressive") append("-$selectedLensType")
                    }.replace(".", "_").replace(" ", "")

                    val groupedRef = database.child("GroupedLensOrders").child(lensKey)

                    groupedRef.get().addOnSuccessListener { snapshot ->
                        val existingData = snapshot.value as? Map<String, Any>

                        val ordersMap = mutableMapOf<String, Double>()
                        var existingPartiallyAllottedQty = 0.0

                        if (existingData != null) {
                            if (existingData["orders"] is Map<*, *>) {
                                val currentOrders = existingData["orders"] as Map<*, *>
                                for ((party, qty) in currentOrders) {
                                    if (party is String && qty is Number) {
                                        ordersMap[party] = qty.toDouble()
                                    }
                                }
                            }
                            existingPartiallyAllottedQty = (existingData["partiallyAllottedQty"] as? Number)?.toDouble() ?: 0.0
                        }

                        val existingQtyForParty = ordersMap[partyName] ?: 0.0
                        ordersMap[partyName] = existingQtyForParty + (quantityDouble?.toDouble() ?:0.00)

                        val groupedData = mutableMapOf<String, Any>(
                            "type" to lensType,
                            "coating" to finalCoating,
                            "coatingType" to finalCoatingType,
                            "sphere" to sphere,
                            "cylinder" to cylinder,
                            "orders" to ordersMap,
                            "partiallyAllottedQty" to existingPartiallyAllottedQty
                        )

                        if (finalMaterial.isNotBlank()) {
                            groupedData["material"] = finalMaterial
                        }

                        if (lensType != "SingleVision") {
                            groupedData["axis"] = axis
                            groupedData["add"] = add
                        }

                        if (lensType == "Progressive") {
                            groupedData["lensSpecificType"] = selectedLensType
                        }

                        groupedRef.setValue(groupedData).addOnSuccessListener {
                            Toast.makeText(context, "Lens order saved successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(context, MainActivity::class.java)
                            context.startActivity(intent)
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to save lens order: ${it.message}", Toast.LENGTH_SHORT).show()
                            Log.e("LensFormScreen", "Failed to save lens order", it)
                        }
                    }.addOnFailureListener {
                        Toast.makeText(context, "Error reading existing lens data: ${it.message}", Toast.LENGTH_SHORT).show()
                        Log.e("LensFormScreen", "Error reading existing lens data", it)
                    }

                }, label = "Save")

            }
        }
    }
}

fun generateSphereOptions(range: IntRange): List<String> {
    val options = mutableListOf<String>()

    for (value in range.filter { it in -30..-20 && it % 1 == 0 }) {
        options.add("%.2f".format(value.toFloat()))
    }

    var v = -19.5
    while (v <= -10.5) {
        if (v >= range.first && v <= range.last) {
            options.add("%.2f".format(v))
        }
        v += 0.5
    }

    v = -10.0
    while (v <= 10.0) {
        if (v >= range.first && v <= range.last) {
            options.add("%.2f".format(v))
        }
        v += 0.25
    }

    v = 10.5
    while (v <= 19.5) {
        if (v >= range.first && v <= range.last) {
            options.add("%.2f".format(v))
        }
        v += 0.5
    }

    for (value in range.filter { it in 20..30 && it % 1 == 0 }) {
        options.add("%.2f".format(value.toFloat()))
    }

    return options.distinct().sortedBy { it.toDouble() }
}

fun generateCylinderOptions(range: ClosedFloatingPointRange<Double>): List<String> {
    val options = mutableListOf<Double>()
    var value = range.start
    while (value <= range.endInclusive) {
        options.add(value)
        value = (value * 100 + 25).toInt() / 100.0
    }
    return options.map { String.format("%.2f", it) }
}

fun generateAddOptions(range: ClosedFloatingPointRange<Double>): List<String> {
    val values = mutableListOf<Double>()
    var value = range.start
    while (value <= range.endInclusive) {
        values.add(value)
        value = (value * 100 + 25).toInt() / 100.0
    }
    return values.map { String.format("%.2f", it) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SphereCylinderSelector(
    label: String,
    selectedValue: String,
    options: List<String>,
    onValueSelected: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val density = LocalDensity.current

    val initialIndex = options.indexOf(selectedValue)

    LaunchedEffect(options) {
        val zeroIndex = options.indexOf("0.00")
        if (zeroIndex != -1) {
            val offsetPx = with(density) {
                val itemWidthPx = 70.dp.toPx()
                val spacingPx = 8.dp.toPx()
                val totalItemSpacePx = itemWidthPx + spacingPx
                val screenWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
                (screenWidthPx / 2 - totalItemSpacePx / 2).toInt()
            }
            listState.scrollToItem(zeroIndex, -offsetPx)
        } else if (options.isNotEmpty()) {
            if (initialIndex != -1) {
                listState.scrollToItem(initialIndex)
            } else {
                listState.scrollToItem(0)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 4.dp))
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items(options) { option ->
                val isSelected = option == selectedValue
                Card(
                    modifier = Modifier
                        .width(70.dp)
                        .height(48.dp)
                        .clickable { onValueSelected(option) },
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Blue2 else MaterialTheme.colorScheme.surfaceVariant
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
                            color = if (isSelected) White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}