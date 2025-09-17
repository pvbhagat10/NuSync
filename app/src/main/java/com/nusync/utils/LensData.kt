package com.nusync.utils

data class SingleVisionLens(
    val coating: String,
    val coatingTypes: List<String>,
    val lensMaterials: List<String>,
    val sphereRange: IntRange = -30..30,
    val cylinderRange: IntRange = -6..6
)

data class KryptokLens(
    val coating: String,
    val coatingTypes: List<String>,
    val lensMaterials: List<String>,
    val sphereRange: ClosedFloatingPointRange<Double> = -6.0..6.0,
    val cylinderRange: ClosedFloatingPointRange<Double> = -4.0..4.0,
    val axisOptions: List<Int> = listOf(45, 90, 135, 180),
    val addRange: ClosedFloatingPointRange<Double> = 0.75..4.0
)

data class ProgressiveLens(
    val coating: String,
    val coatingTypes: List<String>,
    val lensMaterials: List<String>,
    val lensType: List<String>,
    val sphereRange: ClosedFloatingPointRange<Double> = -6.0..6.0,
    val cylinderRange: ClosedFloatingPointRange<Double> = -4.0..4.0,
    val axisOptions: List<Int> = (0..180 step 10).toList(),
    val addRange: ClosedFloatingPointRange<Double> = 0.75..3.5
)

val singleVisionLenses = listOf(
    SingleVisionLens(
        coating = "ARC",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    SingleVisionLens(
        coating = "Blue cut",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    SingleVisionLens(
        coating = "Hard coat",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    SingleVisionLens(
        coating = "Uncoat",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
)

val kryptokLenses = listOf(
    KryptokLens(
        coating = "ARC ",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    KryptokLens(
        coating = "Blue cut",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    KryptokLens(
        coating = "Hard Coat",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    KryptokLens(
        coating = "Uncoat",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
)

val progressiveLenses = listOf(
    ProgressiveLens(
        coating = "ARC",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensType = listOf("Right", "Left", "Both Eye"),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    ProgressiveLens(
        coating = "Blue cut",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensType = listOf("Right", "Left", "Both Eye"),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    ProgressiveLens(
        coating = "Hard Coat",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensType = listOf("Right", "Left", "Both Eye"),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    ),
    ProgressiveLens(
        coating = "Uncoat",
        coatingTypes = listOf(
            "Blue",
            "Green",
            "Mari Blu",
            "Magenta",
            "Night-Drive",
            "Dual O2",
            "Dual Eyeconic",
            "Dual Cracia"
        ),
        lensType = listOf("Right", "Left", "Both Eye"),
        lensMaterials = listOf("Polycarbonate", "Photo Chromatic", "PG Poly", "Photobrown", "None")
    )
)

fun formatMaterial(material: String): String = when (material.lowercase()) {
    "polycarbonate" -> "Poly"
    "photo chromatic", "photochromatic" -> "PG"
    "Photobrown" -> "PB"
    else -> material
}

fun formatCoating(coating: String): String = when (coating.lowercase()) {
    "hard coat" -> "HC"
    "blu ray cut", "brc", "blue cut" -> "BRC"
    "uncoat", "uc" -> "UC"
    else -> coating
}
fun formatCoatingType(type: String): String = when (type.lowercase()) {
    "anti-reflection coating", "arc" -> "ARC"
    "blue" -> "Blue"
    "green" -> "Green"
    "mari blu" -> "MariBlu"
    "magenta" -> "Magenta"
    "night-drive" -> "NightDrive"
    "dual o2" -> "D.O2"
    "dual eyeconic" -> "D.EYE"
    "dual cracia" -> "D.CRC"
    else -> type
}

fun getLensDetailString(
    type: String,
    coat: String,
    coatT: String,
    mat: String,
    sph: String,
    cyl: String,
    ax: String,
    add: String,
    spec: String
): String {
    val formattedSpec = when (spec) {
        "Right" -> "R"
        "Left" -> "L"
        "Both Eye" -> "BE"
        else -> ""
    }

    val formattedMaterial = formatMaterial(mat)
    val formattedCoating = formatCoating(coat)
    val formattedCoatingType = formatCoatingType(coatT)

    val detail = buildString {
        if (formattedSpec.isNotBlank() && type != "SingleVision") {
            append("$formattedSpec ")
        }

        when (type) {
            "SingleVision", "Kryptok" -> {
                if (type == "SingleVision" && formattedSpec.isNotBlank()) {
                    append("$formattedSpec ")
                }
                val sphereVal = sph.toDoubleOrNull() ?: 0.00
                val cylinderVal = cyl.toDoubleOrNull() ?: 0.00

                append(formatPower(sphereVal))

                if (cylinderVal != 0.0) {
                    append("/${formatPower(cylinderVal)}")
                    if (ax.isNotBlank()) {
                        append(" x $ax")
                    }
                } else {
                    if (type == "SingleVision" && sphereVal != 0.0) {
                        append(" sph")
                    }
                }

                if (type == "Kryptok") {
                    val addVal = add.toDoubleOrNull()
                    if (addVal != null && addVal != 0.0) {
                        append(" | Add +%.2f".format(addVal))
                    } else if (add.isNotBlank() && add != "0.00") {
                        append(" | Add +$add")
                    }
                }

                val parts = mutableListOf<String>()
                if (formattedMaterial.isNotBlank() && formattedMaterial != "None") parts.add(formattedMaterial)
                if (formattedCoating.isNotBlank() && formattedCoating != "-") parts.add(formattedCoating)
                if (formattedCoatingType.isNotBlank() && formattedCoatingType != "-") parts.add(formattedCoatingType)

                if (parts.isNotEmpty()) {
                    append(" ${parts.joinToString(" ")}")
                }

                if (type == "Kryptok") append(" KT")
            }

            "Progressive" -> {
                val sphereVal = sph.toDoubleOrNull() ?: 0.00
                val cylinderVal = cyl.toDoubleOrNull() ?: 0.00
                val addVal = add.toDoubleOrNull()

                val omitSphere = (sphereVal == 0.00 && (formattedSpec == "R" || formattedSpec == "L"))

                if (omitSphere) {
                    if (cylinderVal != 0.0) {
                        append(formatPower(cylinderVal))
                        if (ax.isNotBlank()) {
                            append(" x $ax")
                        }
                    }
                } else {
                    append(formatPower(sphereVal))
                    if (cylinderVal != 0.0) {
                        append("/${formatPower(cylinderVal)}")
                        if (ax.isNotBlank()) {
                            append(" x $ax")
                        }
                    }
                }

                if (addVal != null && addVal != 0.0) {
                    append(" | Add +%.2f".format(addVal))
                } else if (add.isNotBlank() && add != "0.00") {
                    append(" | Add +$add")
                }

                val parts = mutableListOf<String>()
                if (formattedCoating.isNotBlank() && formattedCoating != "-") parts.add(formattedCoating)
                if (formattedCoatingType.isNotBlank() && formattedCoatingType != "-") parts.add(formattedCoatingType)
                if (formattedMaterial.isNotBlank() && formattedMaterial != "None") parts.add(formattedMaterial)

                if (parts.isNotEmpty()) {
                    append(" ${parts.joinToString(" ")}")
                }
                append(" V2")
            }
            else -> {
                append("Unknown Lens Type: $type")
            }
        }
    }
    return detail.trim()
}

fun formatPower(value: Double): String {
    return when {
        value > 0 -> "+%.2f".format(value)
        else -> "%.2f".format(value)
    }
}
