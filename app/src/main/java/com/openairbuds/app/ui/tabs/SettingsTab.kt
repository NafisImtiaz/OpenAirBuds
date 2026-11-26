package com.openairbuds.app.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openairbuds.app.R
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.model.NoiseControlMode
import com.openairbuds.app.ui.components.KenkashiActionRow
import com.openairbuds.app.ui.components.KenkashiBadge
import com.openairbuds.app.ui.components.KenkashiDivider
import com.openairbuds.app.ui.components.KenkashiFitTestSheet
import com.openairbuds.app.ui.components.KenkashiSectionPanel
import com.openairbuds.app.ui.components.KenkashiSegmentedBar
import com.openairbuds.app.ui.components.KenkashiSettingRow
import com.openairbuds.app.ui.components.KenkashiThemeToggle
import com.openairbuds.app.ui.theme.AppThemeMode
import com.openairbuds.app.ui.theme.kenkashiSuccessColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTab(
    state: BudsState,
    currentTheme: AppThemeMode,
    onSelectTheme: (AppThemeMode) -> Unit,
    onToggleGameMode: () -> Unit,
    onToggleDual: () -> Unit,
    onStartFitTest: (Boolean) -> Unit,
    onToggleRing: () -> Unit,
    onToggleAutoAnswerCalls: (Boolean) -> Unit = {},
    onToggleFindPhone: (Boolean) -> Unit = {},
    onTogglePhoneRing: () -> Unit = {},
    onOpenSetupScreen: () -> Unit = {},
    onRelease: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = state.connectionStatus is ConnectionStatus.Connected
    var showFitTestSheet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. CONNECTED DEVICE HEADER CARD (2-Cell Tonal Split Grid)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RectangleShape),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Text(
                    text = state.deviceName ?: state.deviceProfile.displayName,
                    fontFamily = FontFamily.Serif,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                val successColor = kenkashiSuccessColor()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                if (isConnected) successColor else MaterialTheme.colorScheme.outline,
                                shape = RectangleShape
                            )
                    )
                    Text(
                        text = if (isConnected) "CONNECTED" else "DISCONNECTED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = if (isConnected) successColor else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            VerticalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxHeight()
            )

            Column(
                modifier = Modifier
                    .weight(0.95f)
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                val ancLabel = if (isConnected) {
                    when (state.noiseMode) {
                        NoiseControlMode.NOISE_CANCELLATION -> "ANC ON"
                        NoiseControlMode.TRANSPARENCY -> "TRANSPARENCY"
                        NoiseControlMode.OFF -> "ANC OFF"
                    }
                } else {
                    "OFFLINE"
                }
                val eqLabel = if (isConnected) {
                    if (state.eqPreset == EqPreset.CUSTOM) {
                        val customName = state.customEqProfiles
                            .find { it.id == state.selectedCustomEqId }
                            ?.name ?: "Custom1"
                        "EQ · ${customName.uppercase()}"
                    } else {
                        "EQ · ${state.eqPreset.displayName.uppercase()}"
                    }
                } else {
                    "NO TELEMETRY"
                }
                Text(
                    text = ancLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    color = if (isConnected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = eqLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 2. DEVICE SETTINGS SECTION
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Device",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            KenkashiSectionPanel {
                // Game Mode
                KenkashiSettingRow(
                    title = "Game Mode",
                    subtitle = "Low latency transmission for gaming",
                    checked = state.isGameModeEnabled,
                    onCheckedChange = { onToggleGameMode() },
                    enabled = isConnected
                )

                // Dual Connection (Multipoint)
                if (state.deviceProfile.capabilities.hasDualConnection) {
                    KenkashiSettingRow(
                        title = "Dual Connection",
                        subtitle = "Seamless multipoint switching between 2 devices",
                        checked = state.isDualConnectionEnabled,
                        onCheckedChange = { onToggleDual() },
                        enabled = isConnected
                    )
                }

                // Find Buds Chirp Action
                KenkashiActionRow(
                    title = "Find My Buds",
                    subtitle = if (state.isFindMyBudsRinging) "Ringing sound active • tap to stop" else "Emit high-pitch acoustic chirp to locate buds",
                    badgeText = if (state.isFindMyBudsRinging) "RINGING" else "LOCATE",
                    onClick = onToggleRing,
                    enabled = isConnected
                )

                // Auto-Answer Calls
                KenkashiSettingRow(
                    title = "Auto-Answer Calls",
                    subtitle = "Answer incoming calls when putting on earbuds",
                    checked = state.isAutoAnswerCallsEnabled,
                    onCheckedChange = { onToggleAutoAnswerCalls(it) },
                    enabled = isConnected
                )

                // Find My Phone
                KenkashiSettingRow(
                    title = "Find My Phone",
                    subtitle = "Ring phone on 4-tap gesture or test ringing",
                    checked = state.isFindPhoneEnabled,
                    onCheckedChange = { onToggleFindPhone(it) },
                    enabled = true,
                    showDivider = state.isFindPhoneEnabled
                )

                if (state.isFindPhoneEnabled) {
                    KenkashiActionRow(
                        title = "Test Phone Ringer",
                        subtitle = if (state.isPhoneRingingFromBuds) "Phone alarm active • tap to silence" else "Sound maximum volume alarm on this phone",
                        badgeText = if (state.isPhoneRingingFromBuds) "SILENCE" else "TEST",
                        onClick = onTogglePhoneRing,
                        enabled = true,
                        showDivider = false
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. APPEARANCE & THEME
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Appearance",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            KenkashiThemeToggle(
                currentTheme = currentTheme,
                onSelectTheme = onSelectTheme
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. DIAGNOSTICS SECTION
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Diagnostics",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            KenkashiSectionPanel {
                KenkashiActionRow(
                    title = "Earbud Fit Test",
                    subtitle = "Check acoustic seal & audio frequency leakage",
                    badgeText = "TEST",
                    onClick = { showFitTestSheet = true },
                    enabled = isConnected
                )

                KenkashiActionRow(
                    title = "Permissions & Setup",
                    subtitle = "Review system access, Bluetooth, and phone call permissions",
                    badgeText = "REVIEW",
                    onClick = onOpenSetupScreen,
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. ABOUT DEVICE & RFCOMM CONTROL
        Column(modifier = Modifier.fillMaxWidth()) {

            Text(
                text = "About Device",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            KenkashiSpecRow("MODEL", state.deviceName ?: state.deviceProfile.displayName)
            KenkashiSpecRow("FIRMWARE", state.firmwareVersion ?: "1.1.0.71")
            KenkashiSpecRow("HARDWARE ID", state.productId ?: "063412")
            KenkashiSpecRow("BLUETOOTH MAC", state.deviceAddress ?: "08:12:37:27:4F:C4")

            Spacer(modifier = Modifier.height(16.dp))

            // Release RFCOMM Channel Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
                    .clickable(onClick = onRelease)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "RELEASE RFCOMM CHANNEL",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    // Earbud Fit Test Bottom Sheet
    if (showFitTestSheet) {
        KenkashiFitTestSheet(
            fitTestState = state.fitTestState,
            placement = state.placement,
            currentTheme = currentTheme,
            onStartTest = { onStartFitTest(true) },
            onStopTest = { onStartFitTest(false) },
            onDismiss = {
                onStartFitTest(false)
                showFitTestSheet = false
            }
        )
    }
}

@Composable
fun KenkashiSpecRow(
    label: String,
    value: String
) {
    Column {
        KenkashiDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
