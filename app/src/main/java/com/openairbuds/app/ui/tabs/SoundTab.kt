package com.openairbuds.app.ui.tabs

import android.bluetooth.BluetoothDevice
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openairbuds.app.R
import com.openairbuds.app.model.AncLevel
import com.openairbuds.app.model.BudsBattery
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.model.NoiseControlMode
import com.openairbuds.app.ui.components.CustomEqSheet
import com.openairbuds.app.ui.components.KenkashiBadge
import com.openairbuds.app.ui.components.KenkashiDeviceSelector
import com.openairbuds.app.ui.components.KenkashiDivider
import com.openairbuds.app.ui.components.KenkashiDynamicBassSheet
import com.openairbuds.app.ui.components.KenkashiEqualizerSheet
import com.openairbuds.app.ui.components.KenkashiSegmentedBar
import com.openairbuds.app.ui.components.KenkashiSettingRow
import com.openairbuds.app.ui.components.KenkashiSwitch
import com.openairbuds.app.ui.theme.AppThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoundTab(
    state: BudsState,
    pairedDevices: List<BluetoothDevice>,
    currentTheme: AppThemeMode,
    onConnect: (BluetoothDevice) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onRelease: () -> Unit,
    onSetNoiseMode: (NoiseControlMode) -> Unit,
    onSetAncLevel: (AncLevel) -> Unit,
    onToggleWindNoiseReduction: (Boolean) -> Unit,
    onToggleBass: () -> Unit,
    onSetBassWaveEnabled: (Boolean) -> Unit,
    onSetBassWaveLevel: (Int) -> Unit,
    onToggleHiResAudio: (Boolean) -> Unit = {},
    onToggleWear: (Boolean) -> Unit = {},
    onToggleVolumeEnhancer: (Boolean) -> Unit = {},
    onToggleVoiceEnhance: (Boolean) -> Unit = {},
    onSetEqPreset: (EqPreset) -> Unit,
    onSetCustomEqBand: (Int, Int) -> Unit,
    onResetCustomEq: () -> Unit,
    onOpenEqualizerScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showBassDialog by remember { mutableStateOf(false) }

    val isConnected = state.connectionStatus is ConnectionStatus.Connected
    val isConnecting = state.connectionStatus is ConnectionStatus.Connecting ||
            state.connectionStatus is ConnectionStatus.Handshaking

    // Seamless auto-sync: automatically initiate connection when audio is connected
    LaunchedEffect(state.isA2dpConnected, isConnected, isConnecting, state.connectionStatus) {
        if (state.isA2dpConnected && !isConnected && !isConnecting && state.connectionStatus is ConnectionStatus.Disconnected) {
            onRetry()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!isConnected && state.isA2dpConnected && !isConnecting) {
            // Minimalist syncing indicator
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AUDIO CONNECTED",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Syncing controls to manage ANC and sound modes…",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // 1. HERO SECTION (Dynamic theme & design system)
        val heroArtworkRes = when (currentTheme) {
            AppThemeMode.KENKASHI_DARK -> R.drawable.ic_kenkashi_hero_dark
            AppThemeMode.NOTHING_DARK -> R.drawable.ic_earbuds_default_hero
            else -> R.drawable.ic_kenkashi_hero
        }

        KenkashiHeroSection(
            artworkRes = heroArtworkRes,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. KENKASHI HAIRLINE BATTERY GRID (LEFT 84% | RIGHT 86% | CASE 100%)
        KenkashiBatteryGrid(
            battery = state.battery,
            isConnected = isConnected,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Connection / Pairing Selector (shown when disconnected and not on A2DP)
        if (!isConnected && !state.isA2dpConnected) {
            KenkashiDeviceSelector(
                state = state,
                pairedDevices = pairedDevices,
                onConnect = onConnect,
                onRefresh = onRefresh
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // 3. SOUND CONTROL SECTION
        if (state.deviceProfile.capabilities.hasAnc) {
            KenkashiNoiseControlSection(
                currentMode = state.noiseMode,
                currentLevel = state.ancLevel,
                isVoiceEnhanceEnabled = state.isVoiceEnhanceEnabled,
                isEnabled = isConnected,
                onModeSelected = onSetNoiseMode,
                onLevelSelected = onSetAncLevel,
                onToggleVoiceEnhance = onToggleVoiceEnhance,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 4. ACOUSTIC LOGIC / AUDIO SETTINGS LIST
        KenkashiAudioSettingsList(
            state = state,
            isEnabled = isConnected,
            onToggleWindNoise = onToggleWindNoiseReduction,
            onToggleBass = onToggleBass,
            onOpenBassDialog = { showBassDialog = true },
            onToggleHiResAudio = onToggleHiResAudio,
            onToggleWear = onToggleWear,
            onOpenEqualizerScreen = onOpenEqualizerScreen,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(36.dp))
    }

    // Bass Enhancement Level Adjuster Bottom Sheet
    if (showBassDialog) {
        KenkashiDynamicBassSheet(
            isEnabled = state.isBassWaveEnabled,
            currentLevel = state.bassWaveLevel,
            onToggle = onSetBassWaveEnabled,
            onLevelChange = onSetBassWaveLevel,
            onDismiss = { showBassDialog = false }
        )
    }
}

/**
 * Line-art earbuds hero illustration with clean presentation and dynamic theme tinting.
 */
@Composable
fun KenkashiHeroSection(
    artworkRes: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = artworkRes),
            contentDescription = "Earbuds Hero",
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .fillMaxHeight(),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 2. KENKASHI HAIRLINE BATTERY GRID
 * Matching Screen 1: 3 equal cells separated by vertical hairline dividers:
 * LEFT, CASE, RIGHT with large high-contrast Serif numerals.
 */
@Composable
fun KenkashiBatteryGrid(
    battery: BudsBattery,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    val leftP = if (isConnected) battery.leftPercent else null
    val caseP = if (isConnected) battery.casePercent else null
    val rightP = if (isConnected) battery.rightPercent else null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Earbud Cell
        KenkashiBatteryCell(
            label = "LEFT",
            percent = leftP,
            isCharging = battery.isLeftCharging,
            modifier = Modifier.weight(1f)
        )

        VerticalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.height(64.dp)
        )

        // Case Cell (Middle)
        KenkashiBatteryCell(
            label = "CASE",
            percent = caseP,
            isCharging = battery.isCaseCharging,
            modifier = Modifier.weight(1f)
        )

        VerticalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.height(64.dp)
        )

        // Right Earbud Cell (Right)
        KenkashiBatteryCell(
            label = "RIGHT",
            percent = rightP,
            isCharging = battery.isRightCharging,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun KenkashiBatteryCell(
    label: String,
    percent: Int?,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (isCharging) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "⚡",
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (percent != null) "$percent%" else "--",
            fontFamily = FontFamily.Serif,
            fontSize = 26.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * 3. SOUND CONTROL & NOISE MODES SECTION
 * Exactly reproduces Screen 1:
 * "Sound Control" in Serif, followed by 3-way segmented control (TRANSPARENCY | ANC ON | OFF).
 */
@Composable
fun KenkashiNoiseControlSection(
    currentMode: NoiseControlMode,
    currentLevel: AncLevel,
    isVoiceEnhanceEnabled: Boolean = false,
    isEnabled: Boolean,
    onModeSelected: (NoiseControlMode) -> Unit,
    onLevelSelected: (AncLevel) -> Unit,
    onToggleVoiceEnhance: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        KenkashiDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Sound Control",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3-Way Segmented Control: TRANSPARENCY | ANC ON | OFF
        val modes = listOf("Transparency", "ANC On", "Off")
        val selectedIdx = when (currentMode) {
            NoiseControlMode.TRANSPARENCY -> 0
            NoiseControlMode.NOISE_CANCELLATION -> 1
            NoiseControlMode.OFF -> 2
        }

        KenkashiSegmentedBar(
            options = modes,
            selectedIndex = selectedIdx,
            onSelectIndex = { idx ->
                if (isEnabled) {
                    val targetMode = when (idx) {
                        0 -> NoiseControlMode.TRANSPARENCY
                        1 -> NoiseControlMode.NOISE_CANCELLATION
                        else -> NoiseControlMode.OFF
                    }
                    onModeSelected(targetMode)
                }
            }
        )

        // If ANC ON is selected, show sub-segmented level bar: SMART | MAX 50dB | MILD
        AnimatedVisibility(
            visible = currentMode == NoiseControlMode.NOISE_CANCELLATION,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                val levels = listOf("Smart", "Max 50dB", "Mild")
                val levelIdx = when (currentLevel) {
                    AncLevel.SMART -> 0
                    AncLevel.HIGH_50DB -> 1
                    AncLevel.MILD, AncLevel.MODERATE -> 2
                }

                KenkashiSegmentedBar(
                    options = levels,
                    selectedIndex = levelIdx,
                    onSelectIndex = { idx ->
                        if (isEnabled) {
                            val targetLevel = when (idx) {
                                0 -> AncLevel.SMART
                                1 -> AncLevel.HIGH_50DB
                                else -> AncLevel.MILD
                            }
                            onLevelSelected(targetLevel)
                        }
                    },
                    modifier = Modifier.height(40.dp)
                )
            }
        }

        // If TRANSPARENCY is selected, show Enhance Voices setting row (strictly hidden otherwise)
        AnimatedVisibility(
            visible = currentMode == NoiseControlMode.TRANSPARENCY,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                KenkashiSettingRow(
                    title = "Enhance Voices",
                    subtitle = "Amplify vocal frequencies for clear conversations",
                    checked = isVoiceEnhanceEnabled,
                    onCheckedChange = { onToggleVoiceEnhance(it) },
                    enabled = isEnabled
                )
            }
        }
    }
}

/**
 * 4. AUDIO SETTINGS LIST (Acoustic Logic rows matching Screen 2)
 */
@Composable
fun KenkashiAudioSettingsList(
    state: BudsState,
    isEnabled: Boolean,
    onToggleWindNoise: (Boolean) -> Unit,
    onToggleBass: () -> Unit,
    onOpenBassDialog: () -> Unit,
    onToggleHiResAudio: (Boolean) -> Unit,
    onToggleWear: (Boolean) -> Unit,
    onOpenEqualizerScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        KenkashiDivider()
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Acoustic Logic",
            fontFamily = FontFamily.Serif,
            fontSize = 24.sp,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Row 1: Wind Noise Reduction
        KenkashiSettingRow(
            title = "Wind Noise Reduction",
            subtitle = "Reduce outdoor wind turbulence",
            checked = state.isWindNoiseReductionEnabled,
            onCheckedChange = { if (isEnabled) onToggleWindNoise(it) },
            enabled = isEnabled,
            onClickRow = { if (isEnabled) onToggleWindNoise(!state.isWindNoiseReductionEnabled) }
        )

        // Row 2: Dynamic Bass (previously BassWave)
        val bassLevelStr = if (state.bassWaveLevel > 0) "+${state.bassWaveLevel}" else "${state.bassWaveLevel}"
        KenkashiSettingRow(
            title = "Dynamic Bass",
            subtitle = if (state.isBassWaveEnabled) "Dynamic Bass active ($bassLevelStr)" else "Deep dynamic bass boost",
            checked = state.isBassWaveEnabled,
            onCheckedChange = { if (isEnabled) onToggleBass() },
            enabled = isEnabled,
            onClickRow = { if (isEnabled) onOpenBassDialog() }
        )

        // Row 3: Hi-Res Audio (LHDC / LDAC toggle)
        if (state.deviceProfile.capabilities.hasHiResAudio) {
            KenkashiSettingRow(
                title = "Hi-Res Audio",
                subtitle = if (state.isHiResAudioEnabled) {
                    "High-definition ${state.deviceProfile.capabilities.hiResCodec} streaming active"
                } else {
                    "Ultra-high definition 24-bit audio streaming"
                },
                badgeText = state.deviceProfile.capabilities.hiResCodec,
                checked = state.isHiResAudioEnabled,
                onCheckedChange = { if (isEnabled) onToggleHiResAudio(it) },
                enabled = isEnabled,
                onClickRow = { if (isEnabled) onToggleHiResAudio(!state.isHiResAudioEnabled) }
            )
        }

        // Row 4: In-Ear Detection (moved from ControlsTab)
        if (state.deviceProfile.capabilities.hasInEarDetection) {
            KenkashiSettingRow(
                title = "In-Ear Detection",
                subtitle = "Auto-pause playback when earbuds removed",
                checked = state.isInEarDetectionEnabled,
                onCheckedChange = { if (isEnabled) onToggleWear(it) },
                enabled = isEnabled,
                onClickRow = { if (isEnabled) onToggleWear(!state.isInEarDetectionEnabled) }
            )
        }

        // Row 5: Equalizer Shortcut & Preset Badge
        val eqBadge = if (state.eqPreset == EqPreset.CUSTOM) "CUSTOM1" else state.eqPreset.displayName.uppercase()
        val eqSubtitle = if (state.eqPreset == EqPreset.CUSTOM) "Custom1 active • 6-band curve" else "Sound effects & custom tuning"
        KenkashiActionRow(
            title = "Equalizer",
            subtitle = eqSubtitle,
            badgeText = eqBadge,
            onClick = onOpenEqualizerScreen,
            enabled = isEnabled
        )
    }
}

/**
 * Standard Kenkashi Setting Row with Serif Title, Monospace Subtitle, and KenkashiSwitch
 */
@Composable
fun KenkashiSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    onClickRow: (() -> Unit)? = null
) {
    Column(modifier = modifier) {
        KenkashiDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClickRow != null) Modifier.clickable(onClick = onClickRow) else Modifier)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontFamily = FontFamily.Serif,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (badgeText != null) {
                        KenkashiBadge(text = badgeText)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            KenkashiSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}

/**
 * Standard Kenkashi Action Row with Serif Title, Monospace Subtitle, and Badge/Chevron
 */
@Composable
fun KenkashiActionRow(
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        KenkashiDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled, onClick = onClick)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                KenkashiBadge(text = badgeText)
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
