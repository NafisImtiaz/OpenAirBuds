package com.openairbuds.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.ui.components.CustomEqSheet
import com.openairbuds.app.ui.components.KenkashiDivider
import com.openairbuds.app.ui.components.KenkashiEqFrequencyVisualizer
import com.openairbuds.app.ui.theme.KenkashiMotion

private data class EqPresetDisplayItem(
    val preset: EqPreset,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

private val SOUND_EFFECTS_PRESETS = listOf(
    EqPresetDisplayItem(
        preset = EqPreset.CLEAR_VOCALS,
        title = "Clear Vocals",
        subtitle = "Enhanced vocal clarity and presence",
        icon = Icons.Default.Mic
    ),
    EqPresetDisplayItem(
        preset = EqPreset.BALANCED,
        title = "Nature Balance",
        subtitle = "Faithful neutral frequency reproduction",
        icon = Icons.Default.Tune
    ),
    EqPresetDisplayItem(
        preset = EqPreset.BASS_BOOST_PLUS,
        title = "Clear Bass",
        subtitle = "Tight sub-bass with pristine mid clarity",
        icon = Icons.Default.GraphicEq
    ),
    EqPresetDisplayItem(
        preset = EqPreset.DEEP_BASS,
        title = "Bass Boost",
        subtitle = "Amplified low-frequency acoustic slam",
        icon = Icons.AutoMirrored.Filled.VolumeUp
    )
)

/**
 * Dedicated EQ Mode Screen matching Kenkashi Acoustic Minimalist Aesthetic:
 * - Top Bar: Back button in sharp rectangular box, centered Monospace "EQ MODE" header
 * - Page Title: "Equalizer" in Serif 24.sp, "Acoustic frequency response profile" in Monospace 11.sp
 * - Sound effects section with 4 presets: Clear Vocals, Nature Balance, Clear Bass, Bass Boost
 * - Custom EQ section with Custom1 profile row and "+ Add custom EQ" action row
 * - Interactive Custom EQ Flyout Bottom Sheet with 6 minimalist studio channel faders
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    state: BudsState,
    onNavigateBack: () -> Unit,
    onSetEqPreset: (EqPreset) -> Unit,
    onSetCustomEqBand: (Int, Int) -> Unit,
    onResetCustomEq: () -> Unit,
    modifier: Modifier = Modifier,
    onSelectCustomEqProfile: (Int) -> Unit = {},
    onAddCustomEqProfile: () -> Boolean = { false }
) {
    var showCustomEqSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // 1. Top App Bar matching Kenkashi aesthetic
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                    .clickable(onClick = onNavigateBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = "EQ MODE",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        KenkashiDivider()

        // 2. Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            // Page Header Title
            Text(
                text = "Equalizer",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Acoustic frequency response profile & sound tuning",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            KenkashiDivider()
            Spacer(modifier = Modifier.height(20.dp))

            // Live Morphing Acoustic Frequency Visualizer
            val activeCurveGains = if (state.eqPreset == EqPreset.CUSTOM) {
                state.customEqGains
            } else {
                KenkashiMotion.getPresetCurveGains(state.eqPreset.displayName)
            }
            KenkashiEqFrequencyVisualizer(
                gains = activeCurveGains,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Section 1 Header: Sound effects
            Text(
                text = "SOUND EFFECTS",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 4 Preset Items
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
            ) {
                SOUND_EFFECTS_PRESETS.forEachIndexed { index, item ->
                    val isSelected = state.eqPreset == item.preset

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSetEqPreset(item.preset) }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Preset Icon in sharp 34x34dp box
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    ),
                                    RectangleShape
                                )
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontFamily = FontFamily.Serif,
                                fontSize = 17.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.subtitle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Radio Button Indicator
                        KenkashiRadioIndicator(selected = isSelected)
                    }

                    if (index < SOUND_EFFECTS_PRESETS.size - 1) {
                        KenkashiDivider()
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Section 2 Header: Custom EQ
            Text(
                text = "CUSTOM EQUALIZER",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
            ) {
                state.customEqProfiles.forEachIndexed { index, profile ->
                    val isCustomProfileSelected = state.eqPreset == EqPreset.CUSTOM && state.selectedCustomEqId == profile.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectCustomEqProfile(profile.id)
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isCustomProfileSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                    ),
                                    RectangleShape
                                )
                                .background(
                                    if (isCustomProfileSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = profile.name,
                                tint = if (isCustomProfileSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                fontFamily = FontFamily.Serif,
                                fontSize = 17.sp,
                                fontWeight = if (isCustomProfileSelected) FontWeight.SemiBold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val summaryText = profile.gains.take(6).joinToString(", ") { if (it > 0) "+$it" else "$it" }
                            Text(
                                text = "6-band curve: [$summaryText dB]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Edit button in sharp box
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                                .clickable {
                                    onSelectCustomEqProfile(profile.id)
                                    showCustomEqSheet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit ${profile.name} Curve",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Radio indicator
                        KenkashiRadioIndicator(selected = isCustomProfileSelected)
                    }

                    if (index < state.customEqProfiles.size - 1 || state.customEqProfiles.size < 3) {
                        KenkashiDivider()
                    }
                }

                // + Add custom EQ action row (shown only when fewer than 3 profiles exist)
                if (state.customEqProfiles.size < 3) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (onAddCustomEqProfile()) {
                                    showCustomEqSheet = true
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)), RectangleShape)
                                .background(MaterialTheme.colorScheme.surface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Custom EQ",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "+ ADD CUSTOM EQ",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Configure 6-band acoustic response curve",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Custom EQ Flyout Bottom Sheet matching Kenkashi aesthetic
    if (showCustomEqSheet) {
        val activeProfile = state.customEqProfiles.find { it.id == state.selectedCustomEqId }
            ?: state.customEqProfiles.firstOrNull()
        CustomEqSheet(
            customGains = activeProfile?.gains ?: state.customEqGains,
            profileTitle = activeProfile?.name ?: "Custom1",
            onGainChanged = onSetCustomEqBand,
            onReset = onResetCustomEq,
            onDismiss = { showCustomEqSheet = false }
        )
    }
}

/**
 * Minimal Kenkashi Radio Indicator (Sharp border with solid inner dot when selected)
 */
@Composable
fun KenkashiRadioIndicator(
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    Box(
        modifier = modifier
            .size(18.dp)
            .border(BorderStroke(1.dp, borderColor), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}
