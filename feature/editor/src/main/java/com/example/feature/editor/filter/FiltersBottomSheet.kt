package com.example.feature.editor.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.ui.theme.AppSpacing

/**
 * Filter presets for 1-tap professional grading looks.
 */
enum class FilterPreset(val displayName: String) {
    NONE("Original"),
    CINEMATIC("Cinematic"),
    VINTAGE("Vintage"),
    WARM("Warm Sunset"),
    COOL("Cyber Cool"),
    MOODY("Moody Dark"),
    FRESH("Fresh Nature"),
    BW("B & W Classic"),
    VIVID("Vivid Pop"),
    FILM("Film 35mm"),
    RETRO("Retro 90s"),
    TEAL_ORANGE("Teal & Orange"),
    NOIR("Film Noir"),
    PASTEL("Pastel Dreams"),
    EMERALD("Emerald"),
    SUNSET("Sunset Glow");

    fun toFilterSettings(): FilterSettings {
        return when (this) {
            NONE -> FilterSettings(selectedPreset = NONE)
            CINEMATIC -> FilterSettings(
                contrast = 1.25f,
                saturation = 0.85f,
                temperature = 15f,
                tint = -10f,
                vignette = 30f,
                selectedPreset = CINEMATIC
            )
            VINTAGE -> FilterSettings(
                contrast = 0.95f,
                saturation = 0.70f,
                temperature = 30f,
                fade = 25f,
                grain = 20f,
                selectedPreset = VINTAGE
            )
            WARM -> FilterSettings(
                temperature = 45f,
                tint = 12f,
                saturation = 1.15f,
                highlights = -10f,
                selectedPreset = WARM
            )
            COOL -> FilterSettings(
                temperature = -40f,
                tint = 20f,
                contrast = 1.3f,
                saturation = 1.20f,
                selectedPreset = COOL
            )
            MOODY -> FilterSettings(
                contrast = 1.35f,
                brightness = -15f,
                saturation = 0.75f,
                shadows = -20f,
                selectedPreset = MOODY
            )
            FRESH -> FilterSettings(
                saturation = 1.25f,
                temperature = -10f,
                tint = -15f,
                contrast = 1.1f,
                selectedPreset = FRESH
            )
            BW -> FilterSettings(
                saturation = 0f,
                contrast = 1.35f,
                brightness = 5f,
                selectedPreset = BW
            )
            VIVID -> FilterSettings(
                saturation = 1.45f,
                contrast = 1.20f,
                vibrance = 30f,
                selectedPreset = VIVID
            )
            FILM -> FilterSettings(
                contrast = 1.15f,
                saturation = 0.90f,
                temperature = 15f,
                fade = 15f,
                vignette = 20f,
                grain = 25f,
                selectedPreset = FILM
            )
            RETRO -> FilterSettings(
                contrast = 1.10f,
                saturation = 1.10f,
                temperature = 20f,
                fade = 20f,
                selectedPreset = RETRO
            )
            TEAL_ORANGE -> FilterSettings(
                contrast = 1.25f,
                shadowHue = 190f,
                shadowSat = 35f,
                highlightHue = 35f,
                highlightSat = 35f,
                selectedPreset = TEAL_ORANGE
            )
            NOIR -> FilterSettings(
                saturation = 0f,
                contrast = 1.60f,
                blacks = -15f,
                whites = 15f,
                vignette = 40f,
                selectedPreset = NOIR
            )
            PASTEL -> FilterSettings(
                saturation = 0.80f,
                brightness = 10f,
                contrast = 0.95f,
                fade = 15f,
                selectedPreset = PASTEL
            )
            EMERALD -> FilterSettings(
                greenBalance = 25f,
                temperature = -15f,
                saturation = 1.20f,
                contrast = 1.15f,
                selectedPreset = EMERALD
            )
            SUNSET -> FilterSettings(
                temperature = 40f,
                tint = 25f,
                highlights = 15f,
                saturation = 1.25f,
                selectedPreset = SUNSET
            )
        }
    }
}

/**
 * Filter and color adjustment values for real-time video editing preview.
 */
@Immutable
data class FilterSettings(
    // 1. Basic Adjustments
    val brightness: Float = 0f,      // Range: -100f to +100f
    val contrast: Float = 1f,        // Range: 0.5f to 2.0f
    val exposure: Float = 0f,        // Range: -100f to +100f
    val highlights: Float = 0f,      // Range: -100f to +100f
    val shadows: Float = 0f,         // Range: -100f to +100f
    val whites: Float = 0f,          // Range: -100f to +100f
    val blacks: Float = 0f,          // Range: -100f to +100f
    val saturation: Float = 1f,      // Range: 0.0f to 2.0f
    val vibrance: Float = 0f,        // Range: -100f to +100f
    val temperature: Float = 0f,     // Range: -100f to +100f
    val tint: Float = 0f,            // Range: -100f to +100f

    // 2. Color & HSL
    val hue: Float = 0f,             // Range: -180f to +180f
    val redBalance: Float = 0f,      // Range: -100f to +100f
    val greenBalance: Float = 0f,    // Range: -100f to +100f
    val blueBalance: Float = 0f,     // Range: -100f to +100f

    // 3. Tone & Light
    val gamma: Float = 1f,           // Range: 0.5f to 2.0f
    val midtones: Float = 0f,        // Range: -100f to +100f
    val dynamicRange: Float = 0f,    // Range: -100f to +100f
    val whitePoint: Float = 100f,    // Range: 50f to 100f
    val blackPoint: Float = 0f,      // Range: 0f to 50f

    // 4. Detail
    val sharpness: Float = 0f,       // Range: 0f to 100f
    val clarity: Float = 0f,         // Range: 0f to 100f
    val texture: Float = 0f,         // Range: 0f to 100f
    val structure: Float = 0f,       // Range: 0f to 100f
    val dehaze: Float = 0f,          // Range: 0f to 100f
    val noiseReduction: Float = 0f,  // Range: 0f to 100f

    // 5. Effects
    val vignette: Float = 0f,        // Range: 0f to 100f
    val fade: Float = 0f,            // Range: 0f to 100f
    val grain: Float = 0f,           // Range: 0f to 100f
    val blur: Float = 0f,            // Range: 0f to 25f
    val glow: Float = 0f,            // Range: 0f to 100f
    val chromaticAberration: Float = 0f, // Range: 0f to 100f

    // 6. Color Grading (3-Way: Shadows, Midtones, Highlights)
    val shadowHue: Float = 0f,       // Range: 0f to 360f
    val shadowSat: Float = 0f,       // Range: 0f to 100f
    val midtoneHue: Float = 0f,      // Range: 0f to 360f
    val midtoneSat: Float = 0f,      // Range: 0f to 100f
    val highlightHue: Float = 0f,    // Range: 0f to 360f
    val highlightSat: Float = 0f,    // Range: 0f to 100f
    val opacity: Float = 100f,       // Range: 0f to 100f

    // 7. Preset
    val selectedPreset: FilterPreset = FilterPreset.NONE
) {
    val isDefault: Boolean
        get() = brightness == 0f &&
                contrast == 1f &&
                exposure == 0f &&
                highlights == 0f &&
                shadows == 0f &&
                whites == 0f &&
                blacks == 0f &&
                saturation == 1f &&
                vibrance == 0f &&
                temperature == 0f &&
                tint == 0f &&
                hue == 0f &&
                redBalance == 0f &&
                greenBalance == 0f &&
                blueBalance == 0f &&
                gamma == 1f &&
                midtones == 0f &&
                dynamicRange == 0f &&
                whitePoint == 100f &&
                blackPoint == 0f &&
                sharpness == 0f &&
                clarity == 0f &&
                texture == 0f &&
                structure == 0f &&
                dehaze == 0f &&
                noiseReduction == 0f &&
                vignette == 0f &&
                fade == 0f &&
                grain == 0f &&
                blur == 0f &&
                glow == 0f &&
                chromaticAberration == 0f &&
                shadowSat == 0f &&
                midtoneSat == 0f &&
                highlightSat == 0f &&
                opacity == 100f &&
                selectedPreset == FilterPreset.NONE
}

/**
 * Bottom sheet for controlling Color Filters and Professional Adjustments.
 * Features tabs for Presets, Basic, Color & HSL, Tone & Light, Detail, Effects, and Grading.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FiltersBottomSheet(
    filterSettings: FilterSettings,
    onFilterChange: (FilterSettings) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Presets", "Basic", "Color & HSL", "Tone & Light", "Detail", "Effects", "Grading")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F111A),
        contentColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF2C3448)) },
        modifier = modifier.testTag("filters_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.xs)
        ) {
            // Header: Title | Reset | Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filters & Adjustments",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = onReset,
                        modifier = Modifier.testTag("reset_filters_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", color = Color.White)
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("filters_sheet_close_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tabs Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) Color.White else Color(0xFF1E2230))
                            .clickable { selectedTab = index }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = title,
                            color = if (isSelected) Color(0xFF0A0D14) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Content (Scrollable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // Presets Tab (16 Presets)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            FilterPreset.entries.forEach { preset ->
                                val isSelected = filterSettings.selectedPreset == preset
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clickable {
                                            onFilterChange(preset.toFilterSettings())
                                        }
                                        .width(76.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (isSelected) Color(0xFF2C3448) else Color(0xFF1E2230))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) Color.White else Color(0xFF2C3448),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = preset.displayName.take(2).uppercase(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = preset.displayName,
                                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    1 -> {
                        // Basic Tab: Brightness, Contrast, Exposure, Highlights, Shadows, Whites, Blacks, Saturation, Vibrance, Temperature, Tint
                        FilterSliderItem(
                            label = "Brightness",
                            value = filterSettings.brightness,
                            displayValue = "${filterSettings.brightness.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(brightness = it)) }
                        )

                        FilterSliderItem(
                            label = "Contrast",
                            value = filterSettings.contrast,
                            displayValue = String.format("%.2fx", filterSettings.contrast),
                            valueRange = 0.5f..2.0f,
                            onValueChange = { onFilterChange(filterSettings.copy(contrast = it)) }
                        )

                        FilterSliderItem(
                            label = "Exposure",
                            value = filterSettings.exposure,
                            displayValue = "${filterSettings.exposure.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(exposure = it)) }
                        )

                        FilterSliderItem(
                            label = "Highlights",
                            value = filterSettings.highlights,
                            displayValue = "${filterSettings.highlights.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(highlights = it)) }
                        )

                        FilterSliderItem(
                            label = "Shadows",
                            value = filterSettings.shadows,
                            displayValue = "${filterSettings.shadows.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(shadows = it)) }
                        )

                        FilterSliderItem(
                            label = "Whites",
                            value = filterSettings.whites,
                            displayValue = "${filterSettings.whites.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(whites = it)) }
                        )

                        FilterSliderItem(
                            label = "Blacks",
                            value = filterSettings.blacks,
                            displayValue = "${filterSettings.blacks.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(blacks = it)) }
                        )

                        FilterSliderItem(
                            label = "Saturation",
                            value = filterSettings.saturation,
                            displayValue = String.format("%.2fx", filterSettings.saturation),
                            valueRange = 0.0f..2.0f,
                            onValueChange = { onFilterChange(filterSettings.copy(saturation = it)) }
                        )

                        FilterSliderItem(
                            label = "Vibrance",
                            value = filterSettings.vibrance,
                            displayValue = "${filterSettings.vibrance.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(vibrance = it)) }
                        )

                        FilterSliderItem(
                            label = "Temperature (Cool / Warm)",
                            value = filterSettings.temperature,
                            displayValue = "${filterSettings.temperature.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(temperature = it)) }
                        )

                        FilterSliderItem(
                            label = "Tint (Green / Magenta)",
                            value = filterSettings.tint,
                            displayValue = "${filterSettings.tint.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(tint = it)) }
                        )
                    }

                    2 -> {
                        // Color & HSL Tab
                        FilterSliderItem(
                            label = "Hue Shift",
                            value = filterSettings.hue,
                            displayValue = "${filterSettings.hue.toInt()}°",
                            valueRange = -180f..180f,
                            onValueChange = { onFilterChange(filterSettings.copy(hue = it)) }
                        )

                        FilterSliderItem(
                            label = "Red Balance",
                            value = filterSettings.redBalance,
                            displayValue = "${filterSettings.redBalance.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(redBalance = it)) }
                        )

                        FilterSliderItem(
                            label = "Green Balance",
                            value = filterSettings.greenBalance,
                            displayValue = "${filterSettings.greenBalance.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(greenBalance = it)) }
                        )

                        FilterSliderItem(
                            label = "Blue Balance",
                            value = filterSettings.blueBalance,
                            displayValue = "${filterSettings.blueBalance.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(blueBalance = it)) }
                        )
                    }

                    3 -> {
                        // Tone & Light Tab: Gamma, Midtones, Dynamic Range, White Point, Black Point
                        FilterSliderItem(
                            label = "Gamma",
                            value = filterSettings.gamma,
                            displayValue = String.format("%.2fx", filterSettings.gamma),
                            valueRange = 0.5f..2.0f,
                            onValueChange = { onFilterChange(filterSettings.copy(gamma = it)) }
                        )

                        FilterSliderItem(
                            label = "Midtones",
                            value = filterSettings.midtones,
                            displayValue = "${filterSettings.midtones.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(midtones = it)) }
                        )

                        FilterSliderItem(
                            label = "Dynamic Range",
                            value = filterSettings.dynamicRange,
                            displayValue = "${filterSettings.dynamicRange.toInt()}",
                            valueRange = -100f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(dynamicRange = it)) }
                        )

                        FilterSliderItem(
                            label = "White Point",
                            value = filterSettings.whitePoint,
                            displayValue = "${filterSettings.whitePoint.toInt()}%",
                            valueRange = 50f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(whitePoint = it)) }
                        )

                        FilterSliderItem(
                            label = "Black Point",
                            value = filterSettings.blackPoint,
                            displayValue = "${filterSettings.blackPoint.toInt()}%",
                            valueRange = 0f..50f,
                            onValueChange = { onFilterChange(filterSettings.copy(blackPoint = it)) }
                        )
                    }

                    4 -> {
                        // Detail Tab: Sharpness, Clarity, Texture, Structure, Dehaze, Noise Reduction
                        FilterSliderItem(
                            label = "Sharpness",
                            value = filterSettings.sharpness,
                            displayValue = "${filterSettings.sharpness.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(sharpness = it)) }
                        )

                        FilterSliderItem(
                            label = "Clarity",
                            value = filterSettings.clarity,
                            displayValue = "${filterSettings.clarity.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(clarity = it)) }
                        )

                        FilterSliderItem(
                            label = "Texture",
                            value = filterSettings.texture,
                            displayValue = "${filterSettings.texture.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(texture = it)) }
                        )

                        FilterSliderItem(
                            label = "Structure",
                            value = filterSettings.structure,
                            displayValue = "${filterSettings.structure.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(structure = it)) }
                        )

                        FilterSliderItem(
                            label = "Dehaze",
                            value = filterSettings.dehaze,
                            displayValue = "${filterSettings.dehaze.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(dehaze = it)) }
                        )

                        FilterSliderItem(
                            label = "Noise Reduction",
                            value = filterSettings.noiseReduction,
                            displayValue = "${filterSettings.noiseReduction.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(noiseReduction = it)) }
                        )
                    }

                    5 -> {
                        // Effects Tab: Vignette, Fade, Film Grain, Blur, Glow, Chromatic Aberration
                        FilterSliderItem(
                            label = "Vignette",
                            value = filterSettings.vignette,
                            displayValue = "${filterSettings.vignette.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(vignette = it)) }
                        )

                        FilterSliderItem(
                            label = "Fade",
                            value = filterSettings.fade,
                            displayValue = "${filterSettings.fade.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(fade = it)) }
                        )

                        FilterSliderItem(
                            label = "Film Grain",
                            value = filterSettings.grain,
                            displayValue = "${filterSettings.grain.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(grain = it)) }
                        )

                        FilterSliderItem(
                            label = "Blur",
                            value = filterSettings.blur,
                            displayValue = "${filterSettings.blur.toInt()}",
                            valueRange = 0f..25f,
                            onValueChange = { onFilterChange(filterSettings.copy(blur = it)) }
                        )

                        FilterSliderItem(
                            label = "Bloom & Glow",
                            value = filterSettings.glow,
                            displayValue = "${filterSettings.glow.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(glow = it)) }
                        )

                        FilterSliderItem(
                            label = "Chromatic Aberration",
                            value = filterSettings.chromaticAberration,
                            displayValue = "${filterSettings.chromaticAberration.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(chromaticAberration = it)) }
                        )
                    }

                    6 -> {
                        // Grading Tab: 3-Way Color Wheels / Sliders
                        Text(
                            "Shadows Color Tint",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00D2FF)
                        )
                        FilterSliderItem(
                            label = "Shadows Hue",
                            value = filterSettings.shadowHue,
                            displayValue = "${filterSettings.shadowHue.toInt()}°",
                            valueRange = 0f..360f,
                            onValueChange = { onFilterChange(filterSettings.copy(shadowHue = it)) }
                        )
                        FilterSliderItem(
                            label = "Shadows Saturation",
                            value = filterSettings.shadowSat,
                            displayValue = "${filterSettings.shadowSat.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(shadowSat = it)) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Midtones Color Tint",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00D2FF)
                        )
                        FilterSliderItem(
                            label = "Midtones Hue",
                            value = filterSettings.midtoneHue,
                            displayValue = "${filterSettings.midtoneHue.toInt()}°",
                            valueRange = 0f..360f,
                            onValueChange = { onFilterChange(filterSettings.copy(midtoneHue = it)) }
                        )
                        FilterSliderItem(
                            label = "Midtones Saturation",
                            value = filterSettings.midtoneSat,
                            displayValue = "${filterSettings.midtoneSat.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(midtoneSat = it)) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Highlights Color Tint",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF00D2FF)
                        )
                        FilterSliderItem(
                            label = "Highlights Hue",
                            value = filterSettings.highlightHue,
                            displayValue = "${filterSettings.highlightHue.toInt()}°",
                            valueRange = 0f..360f,
                            onValueChange = { onFilterChange(filterSettings.copy(highlightHue = it)) }
                        )
                        FilterSliderItem(
                            label = "Highlights Saturation",
                            value = filterSettings.highlightSat,
                            displayValue = "${filterSettings.highlightSat.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(highlightSat = it)) }
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        FilterSliderItem(
                            label = "Overall Opacity",
                            value = filterSettings.opacity,
                            displayValue = "${filterSettings.opacity.toInt()}%",
                            valueRange = 0f..100f,
                            onValueChange = { onFilterChange(filterSettings.copy(opacity = it)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Reusable parameter slider with dark theme styling.
 */
@Composable
private fun FilterSliderItem(
    label: String,
    value: Float,
    displayValue: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = displayValue,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.White,
                inactiveTrackColor = Color(0xFF2C3448)
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
