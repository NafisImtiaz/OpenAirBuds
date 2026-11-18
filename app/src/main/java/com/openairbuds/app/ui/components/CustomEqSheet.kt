package com.openairbuds.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

data class EqBandInfo(
    val freq: String,
    val unit: String,
    val label: String = "$freq $unit"
)

val EQ_BANDS = listOf(
    EqBandInfo("62", "Hz", "62 Hz"),
    EqBandInfo("250", "Hz", "250 Hz"),
    EqBandInfo("1k", "Hz", "1 kHz"),
    EqBandInfo("4k", "Hz", "4 kHz"),
    EqBandInfo("8k", "Hz", "8 kHz"),
    EqBandInfo("16k", "Hz", "16 kHz")
)

/**
 * KENKASHI ACOUSTIC CUSTOM 6-BAND EQUALIZER BOTTOM SHEET
 *
 * Strict Kenkashi Minimalist Architecture:
 * - Typography: Serif headers, Monospace telemetry/frequencies/dB readouts/buttons
 * - Geometry: Pure RectangleShape, 1.dp hairline borders & dividers
 * - Sliders: 6 vertical studio channel faders with discrete -6 dB..+6 dB range
 * - Visuals: Monochrome Paper Light / Charcoal Dark, high-contrast, zero AI slop
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomEqSheet(
    customGains: List<Int>,
    onGainChanged: (bandIndex: Int, gain: Int) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    profileTitle: String = "Custom1",
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val gains = remember(customGains) {
        if (customGains.size == 6) customGains else listOf(0, 0, 0, 0, 0, 0)
    }

    var customTitle by remember(profileTitle) { mutableStateOf(profileTitle) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        var tempName by remember { mutableStateOf(customTitle) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RectangleShape,
            title = {
                Text(
                    text = "RENAME PRESET",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    shape = RectangleShape,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            },
            confirmButton = {
                Box(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RectangleShape)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                        .clickable {
                            if (tempName.isNotBlank()) {
                                customTitle = tempName.trim()
                            }
                            showRenameDialog = false
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "SAVE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            },
            dismissButton = {
                Box(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                        .clickable { showRenameDialog = false }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "CANCEL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        shape = RectangleShape,
        scrimColor = Color.Black.copy(alpha = 0.5f),
        dragHandle = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(16.dp))
                KenkashiDivider()
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 14.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar: Title + Rename, Reset, Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = customTitle,
                            fontFamily = FontFamily.Serif,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { showRenameDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Rename Preset",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "6-band parametric acoustic frequency tuning",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Reset Button
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                            .clickable(onClick = onReset)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "RESET",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Close Button
                    Box(
                        modifier = Modifier
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            KenkashiDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Live Spline Response Curve
            KenkashiEqFrequencyVisualizer(
                gains = gains,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 6-Band Minimalist Graphic Equalizer Console
            KenkashiGraphicEqualizer(
                gains = gains,
                onGainChanged = onGainChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Low freq & High freq indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LOW FREQ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "HIGH FREQ",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. DONE Action Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .background(MaterialTheme.colorScheme.primary, RectangleShape)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DONE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

/**
 * Kenkashi 6-Band Graphic Equalizer Console
 * Features 6 vertical channel strips in a sharp bordered console with 1.dp hairline dividers,
 * monospace dB readouts, 0 dB center ticks, and sharp rectangular fader caps.
 */
@Composable
fun KenkashiGraphicEqualizer(
    gains: List<Int>,
    onGainChanged: (bandIndex: Int, gain: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EQ_BANDS.forEachIndexed { index, band ->
                val currentGain = gains.getOrElse(index) { 0 }

                KenkashiFaderChannel(
                    band = band,
                    gain = currentGain,
                    onGainChange = { newGain -> onGainChanged(index, newGain) },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                if (index < EQ_BANDS.size - 1) {
                    VerticalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxHeight()
                    )
                }
            }
        }
    }
}

/**
 * Single Channel Strip for Kenkashi Equalizer:
 * - Top: Gain value readout in Monospace (+3 dB, 0 dB, -2 dB)
 * - Center: Vertical fader track with 0 dB reference line and sharp rectangular thumb
 * - Bottom: Frequency label ("62", "250", "1k", ...) + "Hz"
 */
@Composable
fun KenkashiFaderChannel(
    band: EqBandInfo,
    gain: Int,
    onGainChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val faderTrackHeightDp = 130.dp
    val thumbHeightDp = 14.dp
    val thumbWidthDp = 28.dp

    Column(
        modifier = modifier
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // 1. Gain Readout
        val gainStr = when {
            gain > 0 -> "+$gain"
            else -> "$gain"
        }
        val isZero = gain == 0
        Text(
            text = "$gainStr dB",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = if (!isZero) FontWeight.Bold else FontWeight.Normal,
            color = if (!isZero) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        // 2. Vertical Slider / Fader Track
        BoxWithConstraints(
            modifier = Modifier
                .width(thumbWidthDp)
                .height(faderTrackHeightDp),
            contentAlignment = Alignment.TopCenter
        ) {
            val totalHeightPx = with(density) { maxHeight.toPx() }
            val thumbHeightPx = with(density) { thumbHeightDp.toPx() }
            val maxTravelPx = totalHeightPx - thumbHeightPx

            // Map gain (-6..+6) to Y offset (0 at +6dB, maxTravel at -6dB)
            val fraction = ((6 - gain) / 12f).coerceIn(0f, 1f)
            val thumbOffsetY = with(density) { (fraction * maxTravelPx).toDp() }

            // Drag gesture handler on vertical track
            val gestureModifier = Modifier
                .matchParentSize()
                .pointerInput(totalHeightPx, thumbHeightPx) {
                    val updateGainFromY = { y: Float ->
                        val clampedY = (y - thumbHeightPx / 2f).coerceIn(0f, maxTravelPx)
                        val norm = (clampedY / maxTravelPx).coerceIn(0f, 1f)
                        val newGain = (6 - (norm * 12f)).roundToInt().coerceIn(-6, 6)
                        if (newGain != gain) {
                            onGainChange(newGain)
                        }
                    }

                    detectTapGestures { offset ->
                        updateGainFromY(offset.y)
                    }
                }
                .pointerInput(totalHeightPx, thumbHeightPx) {
                    val updateGainFromY = { y: Float ->
                        val clampedY = (y - thumbHeightPx / 2f).coerceIn(0f, maxTravelPx)
                        val norm = (clampedY / maxTravelPx).coerceIn(0f, 1f)
                        val newGain = (6 - (norm * 12f)).roundToInt().coerceIn(-6, 6)
                        if (newGain != gain) {
                            onGainChange(newGain)
                        }
                    }

                    detectDragGestures { change, _ ->
                        change.consume()
                        updateGainFromY(change.position.y)
                    }
                }

            Box(modifier = gestureModifier) {
                // Background Track Line (vertical 2.dp hairline)
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.6f), RectangleShape)
                )

                // 0 dB Baseline Reference Tick (center horizontal line)
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(1.dp)
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                )

                // Rectangular Fader Thumb
                Box(
                    modifier = Modifier
                        .offset(y = thumbOffsetY)
                        .size(width = thumbWidthDp, height = thumbHeightDp)
                        .background(MaterialTheme.colorScheme.primary, RectangleShape)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Center horizontal grip slot on the fader cap
                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f), RectangleShape)
                    )
                }
            }
        }

        // 3. Frequency & Unit Label
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Text(
                text = band.freq,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = band.unit,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
