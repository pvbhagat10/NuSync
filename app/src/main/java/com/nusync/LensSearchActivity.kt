package com.nusync

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nusync.ui.theme.NuSyncTheme
import com.nusync.utils.kryptokLenses
import com.nusync.utils.progressiveLenses
import com.nusync.utils.singleVisionLenses

class LensSearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val lensType = intent.getStringExtra("lensType") ?: ""
        val workType = intent.getStringExtra("workType") ?: ""

        enableEdgeToEdge()
        setContent {
            NuSyncTheme {
                var searchText by remember { mutableStateOf("") }

                val filteredLenses by remember(searchText, lensType) {
                    mutableStateOf(
                        when (lensType) {
                            "SingleVisionLens" -> generateFilteredSingleVisionCombinations(searchText)
                            "KryptokLens" -> generateFilteredKryptokCombinations(searchText)
                            "ProgressiveLens" -> generateFilteredProgressiveCombinations(searchText)
                            else -> emptyList()
                        }
                    )
                }

                TopBar(heading = "Lens Search: $lensType") {
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text("Search Lens") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredLenses) { lens ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Text(
                                    text = lens,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun generateFilteredSingleVisionCombinations(searchText: String): List<String> {
        val keywords = parseKeywords(searchText)
        if (keywords.isEmpty()) return emptyList()

        val results = mutableListOf<String>()
        for (lens in singleVisionLenses) {
            val sphereSteps = getSphereSteps(lens.sphereRange)
            val cylinderSteps = getCylinderSteps(lens.cylinderRange)

            loop@ for (coatingType in lens.coatingTypes)
                for (material in lens.lensMaterials)
                    for (sphere in sphereSteps)
                        for (cylinder in cylinderSteps) {
                            val entry =
                                "${lens.coating} | $coatingType | $material | Sphere: $sphere | Cylinder: $cylinder"
                            val entryLower = entry.lowercase()
                            if (keywords.all { entryLower.contains(it) }) {
                                results.add(entry)
                                if (results.size > 1000) break@loop
                            }
                        }
        }
        return results
    }

    private fun generateFilteredKryptokCombinations(searchText: String): List<String> {
        val keywords = parseKeywords(searchText)
        if (keywords.isEmpty()) return emptyList()

        val results = mutableListOf<String>()
        for (lens in kryptokLenses) {
            val sphereSteps = getSphereSteps(lens.sphereRange)
            val cylinderSteps = getCylinderSteps(lens.cylinderRange)
            val addSteps = generateFloatRange(lens.addRange.start, lens.addRange.endInclusive, 0.25)

            loop@ for (coatingType in lens.coatingTypes)
                for (material in lens.lensMaterials)
                    for (sphere in sphereSteps)
                        for (cylinder in cylinderSteps)
                            for (axis in lens.axisOptions)
                                for (add in addSteps) {
                                    val entry =
                                        "${lens.coating} | $coatingType | $material | Sphere: $sphere | Cylinder: $cylinder | Axis: $axis | Add: $add"
                                    val entryLower = entry.lowercase()
                                    if (keywords.all { entryLower.contains(it) }) {
                                        results.add(entry)
                                        if (results.size > 1000) break@loop
                                    }
                                }
        }
        return results
    }

    private fun generateFilteredProgressiveCombinations(searchText: String): List<String> {
        val keywords = parseKeywords(searchText)
        if (keywords.isEmpty()) return emptyList()

        val results = mutableListOf<String>()
        for (lens in progressiveLenses) {
            val sphereSteps = getSphereSteps(lens.sphereRange)
            val cylinderSteps = getCylinderSteps(lens.cylinderRange)
            val addSteps = generateFloatRange(lens.addRange.start, lens.addRange.endInclusive, 0.25)

            loop@ for (coatingType in lens.coatingTypes)
                for (material in lens.lensMaterials)
                    for (type in lens.lensType)
                        for (sphere in sphereSteps)
                            for (cylinder in cylinderSteps)
                                for (axis in lens.axisOptions)
                                    for (add in addSteps) {
                                        val entry =
                                            "${lens.coating} | $coatingType | $material | Type: $type | Sphere: $sphere | Cylinder: $cylinder | Axis: $axis | Add: $add"
                                        val entryLower = entry.lowercase()
                                        if (keywords.all { entryLower.contains(it) }) {
                                            results.add(entry)
                                            if (results.size > 1000) break@loop
                                        }
                                    }
        }
        return results
    }

    private fun parseKeywords(input: String): List<String> {
        return input.lowercase()
            .split(" ", ",", "|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun generateFloatRange(start: Double, end: Double, step: Double): List<Double> {
        val list = mutableListOf<Double>()
        var current = start
        while (current <= end) {
            list.add(String.format("%.2f", current).toDouble())
            current += step
        }
        return list
    }

    private fun getSphereSteps(range: IntRange): List<Double> {
        return when {
            range.last <= 10 -> generateFloatRange(range.first.toDouble(), range.last.toDouble(), 0.25)
            range.last <= 20 -> generateFloatRange(range.first.toDouble(), range.last.toDouble(), 0.5)
            else -> generateFloatRange(range.first.toDouble(), range.last.toDouble(), 1.0)
        }
    }

    private fun getCylinderSteps(range: IntRange): List<Double> {
        return generateFloatRange(range.first.toDouble(), range.last.toDouble(), 0.25)
    }

    private fun getSphereSteps(range: ClosedFloatingPointRange<Double>): List<Double> {
        return when {
            range.endInclusive <= 10 -> generateFloatRange(range.start, range.endInclusive, 0.25)
            range.endInclusive <= 20 -> generateFloatRange(range.start, range.endInclusive, 0.5)
            else -> generateFloatRange(range.start, range.endInclusive, 1.0)
        }
    }

    private fun getCylinderSteps(range: ClosedFloatingPointRange<Double>): List<Double> {
        return generateFloatRange(range.start, range.endInclusive, 0.25)
    }
}