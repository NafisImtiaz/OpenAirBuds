package com.openairbuds.app.ui.components

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.openairbuds.app.R
import com.openairbuds.app.model.BudsPlacement
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.model.EqPresetOption
import com.openairbuds.app.ui.theme.AppThemeMode
import com.openairbuds.app.ui.theme.KenkashiBlack
import com.openairbuds.app.ui.theme.KenkashiHairline
import com.openairbuds.app.ui.theme.KenkashiPaperBg
import com.openairbuds.app.ui.theme.KenkashiTextPrimary
import com.openairbuds.app.ui.theme.KenkashiTextSecondary
import com.openairbuds.app.ui.theme.KenkashiWhite
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * 1. KENKASHI MINIMALIST RECTANGULAR SWITCH
 * Sharp rectangular box with 1px border and a sliding square thumb.
 */
@Composable
fun KenkashiSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outline
) {
    val interactionSource = remember { MutableInteractionSource() }
    val trackWidth = 44.dp
    val trackHeight = 22.dp
    val thumbSize = 14.dp
    val padding = 3.dp

    val maxOffset = trackWidth - thumbSize - (padding * 2)
    val thumbOffsetFraction by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        label = "KenkashiSwitchThumb"
    )
    val thumbOffset = maxOffset * thumbOffsetFraction

    Box(
        modifier = modifier
            .size(width = trackWidth, height = trackHeight)
            .border(
                BorderStroke(
                    1.dp,
                    if (enabled) borderColor else borderColor.copy(alpha = 0.4f)
                ),
                RectangleShape
            )
            .background(Color.Transparent, RectangleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled
            ) {
                onCheckedChange?.invoke(!checked)
            }
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .then(
                    if (checked) {
                        Modifier.background(if (enabled) activeColor else activeColor.copy(alpha = 0.4f), RectangleShape)
                    } else {
                        Modifier.border(BorderStroke(1.dp, if (enabled) inactiveColor else inactiveColor.copy(alpha = 0.4f)), RectangleShape)
                    }
                )
        )
    }
}

/**
 * 2. KENKASHI SEGMENTED BAR
 * Hairline grid with N equal segments:
 * Selected item is solid filled with high contrast inverted text.
 */
@Composable
fun KenkashiSegmentedBar(
    options: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    activeBg: Color = MaterialTheme.colorScheme.primary,
    activeText: Color = MaterialTheme.colorScheme.onPrimary,
    inactiveText: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .border(BorderStroke(1.dp, borderColor), RectangleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, title ->
            val isSelected = selectedIndex == index
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(if (isSelected) activeBg else Color.Transparent)
                    .clickable { onSelectIndex(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title.uppercase(),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = if (isSelected) activeText else inactiveText,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }

            if (index < options.size - 1) {
                VerticalDivider(
                    thickness = 1.dp,
                    color = borderColor,
                    modifier = Modifier.fillMaxHeight()
                )
            }
        }
    }
}

/**
 * 3. KENKASHI BADGE (e.g., [LDAC], [50dB], [AAC])
 */
@Composable
fun KenkashiBadge(
    text: String,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Box(
        modifier = modifier
            .border(BorderStroke(1.dp, borderColor), RectangleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * 4. KENKASHI HAIRLINE DIVIDER
 */
@Composable
fun KenkashiDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.outline
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = color
    )
}

/**
 * 5. KENKASHI SETTING ROW WITH SWITCH
 */
@Composable
fun KenkashiSettingRow(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Normal,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (badge != null) {
                        KenkashiBadge(text = badge)
                    }
                }
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            KenkashiSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
        KenkashiDivider()
    }
}

/**
 * 6. KENKASHI ACTION ROW
 */
@Composable
fun KenkashiActionRow(
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                Text(
                    text = title,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (badgeText != null) {
                KenkashiBadge(text = badgeText)
            }
        }
        KenkashiDivider()
    }
}

/**
 * 7. KENKASHI FLAT HORIZONTAL THEME TOGGLE
 * Ultra-minimalist 2-option selector:
 * [ PAPER LIGHT | CHARCOAL DARK ]
 */
@Composable
fun KenkashiThemeToggle(
    isDark: Boolean,
    onSelectTheme: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    activeBg: Color = MaterialTheme.colorScheme.primary,
    activeText: Color = MaterialTheme.colorScheme.onPrimary,
    inactiveText: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp)
            .border(BorderStroke(1.dp, borderColor), RectangleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Option 1: Paper Light
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (!isDark) activeBg else Color.Transparent)
                .clickable { onSelectTheme(false) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFFF9F8F5), RectangleShape)
                        .border(BorderStroke(1.dp, if (!isDark) Color(0xFF111111) else borderColor), RectangleShape)
                )
                Text(
                    text = "PAPER LIGHT",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (!isDark) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = if (!isDark) activeText else inactiveText,
                    maxLines = 1
                )
            }
        }

        VerticalDivider(
            thickness = 1.dp,
            color = borderColor,
            modifier = Modifier.fillMaxHeight()
        )

        // Option 2: Charcoal Dark
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(if (isDark) activeBg else Color.Transparent)
                .clickable { onSelectTheme(true) },
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF121212), RectangleShape)
                        .border(BorderStroke(1.dp, if (isDark) Color(0xFFF9F8F5) else borderColor), RectangleShape)
                )
                Text(
                    text = "CHARCOAL DARK",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (isDark) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 1.sp,
                    color = if (isDark) activeText else inactiveText,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * 8. KENKASHI SLIDE-DOWN CONNECTION ERROR BANNER
 * Full horizontal width minimal alert pinned directly beneath the top bar.
 */
@Composable
fun KenkashiConnectionErrorBanner(
    isConflict: Boolean,
    errorMessage: String? = null,
    onRetry: () -> Unit,
    onRelease: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val containerBg = if (isDark) Color(0xFF2E1416) else Color(0xFFFEE2E2)
    val borderColor = if (isDark) Color(0xFF7F1D1D) else Color(0xFFFCA5A5)
    val titleColor = if (isDark) Color(0xFFFCA5A5) else Color(0xFF991B1B)
    val subtitleColor = if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)

    val title = if (isConflict) "BLUETOOTH CHANNEL CONFLICT" else "CONNECTION ERROR"
    val subtitle = when {
        isConflict -> "Channel held by another app • Tap to resolve"
        !errorMessage.isNullOrBlank() && errorMessage.contains("realme Link", ignoreCase = true) ->
            "Channel held by realme Link • Tap to release"
        else -> "Could not connect to earbuds • Tap to retry"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(containerBg)
            .border(BorderStroke(1.dp, borderColor), RectangleShape)
            .clickable {
                if (isConflict && onRelease != null) onRelease() else onRetry()
            }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = titleColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = subtitleColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 9. KENKASHI MINIMALIST SLIDER
 * Hairline 2.dp track with a sharp 14x14dp square thumb and zero rounding.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KenkashiSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        onValueChangeFinished = onValueChangeFinished,
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        modifier = modifier,
        thumb = {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RectangleShape)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(2.dp),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.outline,
                    disabledActiveTrackColor = MaterialTheme.colorScheme.outline,
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                thumbTrackGapSize = 0.dp
            )
        }
    )
}

/**
 * 10. KENKASHI DYNAMIC BASS BOTTOM SHEET
 * Full-width edge-to-edge sharp bottom sheet with hairline dividers and monospace gain indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KenkashiDynamicBassSheet(
    isEnabled: Boolean,
    currentLevel: Int,
    onToggle: (Boolean) -> Unit,
    onLevelChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempLevel by remember(currentLevel) { mutableFloatStateOf(currentLevel.toFloat()) }

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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Dynamic Bass",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Low-frequency algorithmic enhancement",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            KenkashiDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Dynamic Bass",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                KenkashiSwitch(
                    checked = isEnabled,
                    onCheckedChange = onToggle
                )
            }

            if (isEnabled) {
                Spacer(modifier = Modifier.height(20.dp))
                val rounded = tempLevel.roundToInt()
                val formattedGain = if (rounded > 0) "+$rounded dB" else "$rounded dB"

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "GAIN LEVEL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formattedGain,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                KenkashiSlider(
                    value = tempLevel,
                    onValueChange = { tempLevel = it },
                    onValueChangeFinished = { onLevelChange(tempLevel.roundToInt()) },
                    valueRange = -5f..5f,
                    steps = 9
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

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
 * Backward compatibility alias for KenkashiDynamicBassSheet.
 */
@Composable
fun KenkashiDynamicBassDialog(
    isEnabled: Boolean,
    currentLevel: Int,
    onToggle: (Boolean) -> Unit,
    onLevelChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    KenkashiDynamicBassSheet(
        isEnabled = isEnabled,
        currentLevel = currentLevel,
        onToggle = onToggle,
        onLevelChange = onLevelChange,
        onDismiss = onDismiss
    )
}

/**
 * 11. KENKASHI EQUALIZER BOTTOM SHEET
 * Full-width edge-to-edge sharp bottom sheet with hairline dividers and solid active selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KenkashiEqualizerSheet(
    currentPreset: EqPreset,
    options: List<EqPresetOption> = emptyList(),
    onSelectPreset: (EqPreset) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val presetsToDisplay = remember(options) {
        val baseList = if (options.isNotEmpty()) {
            options
        } else {
            EqPreset.entries.map { EqPresetOption(it.slot, it.displayName) }
        }
        if (baseList.none { it.slot == EqPreset.CUSTOM.slot }) {
            baseList + EqPresetOption(EqPreset.CUSTOM.slot, EqPreset.CUSTOM.displayName, "Custom equalizer profile")
        } else {
            baseList
        }
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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Equalizer",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Acoustic frequency response profile",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            KenkashiDivider()
            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
            ) {
                presetsToDisplay.forEachIndexed { index, opt ->
                    val isSelected = currentPreset.slot == opt.slot
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable {
                                val matched = EqPreset.fromSlot(opt.slot)
                                onSelectPreset(matched)
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = opt.displayName.uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                letterSpacing = 1.sp,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }

                    if (index < presetsToDisplay.size - 1) {
                        KenkashiDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "CLOSE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Backward compatibility alias for KenkashiEqualizerSheet.
 */
@Composable
fun KenkashiEqualizerDialog(
    currentPreset: EqPreset,
    options: List<EqPresetOption> = emptyList(),
    onSelectPreset: (EqPreset) -> Unit,
    onDismiss: () -> Unit
) {
    KenkashiEqualizerSheet(
        currentPreset = currentPreset,
        options = options,
        onSelectPreset = onSelectPreset,
        onDismiss = onDismiss
    )
}

/**
 * 12. KENKASHI DEVICE SELECTOR
 * Inline flat rectangular boxed container with 1.dp hairline borders.
 * Replaces the legacy 24.dp rounded card and green accents.
 */
@Composable
fun KenkashiDeviceSelector(
    state: BudsState,
    pairedDevices: List<BluetoothDevice>,
    onConnect: (BluetoothDevice) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnecting = state.isConnecting

    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
            .padding(18.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Paired Earbuds",
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (state.isA2dpConnected) "Audio connected • Control offline" else "Select device to establish link",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                        .clickable(onClick = onRefresh)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "RESCAN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            KenkashiDivider()
            Spacer(modifier = Modifier.height(12.dp))

            if (isConnecting) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (state.connectionStatus is ConnectionStatus.Handshaking) "SYNCING CONTROLS..." else "OPENING BLUETOOTH CHANNEL...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (pairedDevices.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                ) {
                    pairedDevices.forEachIndexed { index, dev ->
                        val devName = runCatching { dev.name }.getOrNull() ?: dev.address
                        val isAudioActive = state.isA2dpConnected && dev.address.equals(state.deviceAddress, ignoreCase = true)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = devName,
                                    fontFamily = FontFamily.Serif,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = dev.address,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (isAudioActive) {
                                        KenkashiBadge(text = "AUDIO ACTIVE")
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary, RectangleShape)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                                    .clickable { onConnect(dev) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CONNECT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }

                        if (index < pairedDevices.size - 1) {
                            KenkashiDivider()
                        }
                    }
                }
            } else {
                Text(
                    text = "No paired Bluetooth audio devices found.",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
    }
}

/**
 * 13. KENKASHI EARBUD FIT TEST SHEET
 * Full-width edge-to-edge sharp bottom sheet with 2D line-art illustration and monospace telemetry.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KenkashiFitTestSheet(
    fitTestResult: String?,
    placement: BudsPlacement,
    currentTheme: AppThemeMode,
    onStartTest: () -> Unit,
    onStopTest: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hasStarted by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }

    LaunchedEffect(hasStarted, fitTestResult) {
        if (hasStarted && fitTestResult == "Testing seal...") {
            delay(15000L)
            if (fitTestResult == "Testing seal...") {
                timedOut = true
                onStopTest()
            }
        }
        if (fitTestResult != null && fitTestResult != "Testing seal...") {
            hasStarted = false
            timedOut = false
        }
    }

    val isDark = currentTheme == AppThemeMode.KENKASHI_DARK
    val leftBudRes = if (isDark) R.drawable.ic_kenkashi_bud_left_dark else R.drawable.ic_kenkashi_bud_left
    val rightBudRes = if (isDark) R.drawable.ic_kenkashi_bud_right_dark else R.drawable.ic_kenkashi_bud_right

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
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Earbud Fit Test",
                        fontFamily = FontFamily.Serif,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Acoustic seal & audio frequency leakage inspection",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
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

            Spacer(modifier = Modifier.height(16.dp))

            // 2D Line-Art Earbuds Showcase (Left & Right only, case removed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = leftBudRes),
                        contentDescription = "Left Earbud",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.fillMaxHeight()
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = rightBudRes),
                        contentDescription = "Right Earbud",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            val isFailed = fitTestResult?.startsWith("Failed") == true

            when {
                // 1. IN PROGRESS
                hasStarted && fitTestResult == "Testing seal..." && !timedOut && !isFailed -> {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "TESTING ACOUSTIC SEAL...",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Remain quiet and keep earbuds inserted firmly in ears.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                            .clickable {
                                hasStarted = false
                                onStopTest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CANCEL TEST",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 2. TIMED OUT / FAILED
                timedOut || isFailed -> {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (timedOut) "TEST TIMED OUT" else (fitTestResult ?: "FAILED TO COMPLETE TEST").uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Ensure both earbuds are worn firmly in ears and Bluetooth connection is active.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                                .clickable(onClick = onDismiss),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "CLOSE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .background(MaterialTheme.colorScheme.primary, RectangleShape)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                                .clickable {
                                    timedOut = false
                                    hasStarted = true
                                    onStartTest()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "RETRY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }

                // 3. RESULTS RECEIVED
                fitTestResult != null && fitTestResult != "Testing seal..." && !isFailed -> {
                    val hasLeft = fitTestResult.contains("Left", ignoreCase = true)
                    val hasRight = fitTestResult.contains("Right", ignoreCase = true)
                    val isLeftGood = fitTestResult.contains("Left: Good", ignoreCase = true)
                    val isRightGood = fitTestResult.contains("Right: Good", ignoreCase = true)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Left Cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "LEFT EARBUD",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (!hasLeft) "NOT DETECTED" else if (isLeftGood) "GOOD SEAL" else "POOR FIT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isLeftGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        // Right Cell
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                                .padding(14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "RIGHT EARBUD",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    letterSpacing = 1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (!hasRight) "NOT DETECTED" else if (isRightGood) "GOOD SEAL" else "POOR FIT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp,
                                    color = if (isRightGood) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.primary, RectangleShape)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                            .clickable {
                                timedOut = false
                                hasStarted = true
                                onStartTest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TEST AGAIN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                // 4. INITIAL STATE
                else -> {
                    Text(
                        text = "Play an acoustic diagnostic chirp to verify that your silicone ear tips create a proper acoustic seal.",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(MaterialTheme.colorScheme.primary, RectangleShape)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                            .clickable {
                                timedOut = false
                                hasStarted = true
                                onStartTest()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "START TEST",
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
    }
}
