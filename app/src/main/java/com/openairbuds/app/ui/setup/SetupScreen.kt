package com.openairbuds.app.ui.setup

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.openairbuds.app.ui.components.KenkashiDivider
import com.openairbuds.app.ui.theme.kenkashiSuccessColor

/**
 * Kenkashi Onboarding & Permissions Landing Screen:
 * - High-contrast minimalist aesthetic with hairline borders and sharp rectangles.
 * - Granular cards explaining the rationale for Bluetooth, Phone, and Notification permissions.
 * - Bottom action bar gate requiring Essential permissions before proceeding.
 */
@Composable
fun SetupScreen(
    isBluetoothGranted: Boolean,
    isPhoneGranted: Boolean,
    isNotificationGranted: Boolean,
    onRequestBluetoothPermission: () -> Unit,
    onRequestPhonePermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onContinue: () -> Unit,
    canDismiss: Boolean = false,
    onDismiss: () -> Unit = onContinue,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canDismiss) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Text(
                text = "SYSTEM ACCESS",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        KenkashiDivider()

        // Scrollable Body Content
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Screen Header
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Device Permissions",
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Configure hardware access so OpenAirBuds can detect, sync, and control your earbuds.",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            // 1. Bluetooth Card (Essential)
            PermissionCard(
                tag = "ESSENTIAL",
                tagColor = MaterialTheme.colorScheme.onSurface,
                title = "Nearby Devices & Bluetooth",
                description = "Required to scan, discover, pair, and send real-time noise control commands to your OpenAirBuds hardware.",
                icon = Icons.Default.Bluetooth,
                isGranted = isBluetoothGranted,
                onGrantClick = onRequestBluetoothPermission
            )

            // 2. Phone Calls Card (Optional)
            PermissionCard(
                tag = "OPTIONAL",
                tagColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = "Phone Call Management",
                description = "Used exclusively by the Auto-Answer feature to seamlessly accept incoming phone calls when placing an earbud into your ear.",
                icon = Icons.Default.Phone,
                isGranted = isPhoneGranted,
                onGrantClick = onRequestPhonePermission
            )

            // 3. Notifications Card (Optional)
            PermissionCard(
                tag = "OPTIONAL",
                tagColor = MaterialTheme.colorScheme.onSurfaceVariant,
                title = "Status & Quick Settings",
                description = "Enables background connection persistence and synchronization with the Quick Settings 1-tap ANC tile.",
                icon = Icons.Default.Notifications,
                isGranted = isNotificationGranted,
                onGrantClick = onRequestNotificationPermission
            )

            // Privacy Disclaimer Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), RectangleShape)
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "LOCAL PROCESSING GUARANTEE",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "All telemetry and commands are processed entirely on-device via Bluetooth. No personal data, audio, or call info is ever transmitted to remote servers.",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        KenkashiDivider()

        // Bottom Action Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isBluetoothGranted) {
                Text(
                    text = "* Nearby Devices & Bluetooth access is required to proceed",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        if (isBluetoothGranted) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isBluetoothGranted) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.outline
                        ),
                        RectangleShape
                    )
                    .clickable {
                        if (isBluetoothGranted) {
                            onContinue()
                        } else {
                            onRequestBluetoothPermission()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isBluetoothGranted) {
                            if (canDismiss) "DONE" else "NEXT"
                        } else {
                            "GRANT BLUETOOTH ACCESS"
                        },
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp,
                        color = if (isBluetoothGranted) MaterialTheme.colorScheme.surface
                        else MaterialTheme.colorScheme.onSurface
                    )
                    if (isBluetoothGranted && !canDismiss) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Next",
                            tint = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(
    tag: String,
    tagColor: Color,
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onGrantClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), RectangleShape)
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Tag + Granted Indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .border(BorderStroke(1.dp, tagColor.copy(alpha = 0.6f)), RectangleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tag,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = tagColor
                    )
                }

                if (isGranted) {
                    val successColor = kenkashiSuccessColor()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Granted",
                            tint = successColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "GRANTED",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = successColor
                        )
                    }
                }
            }

            // Body: Icon + Title + Description
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)), RectangleShape)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = description,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            // Action Row: Show ALLOW button when not granted
            if (!isGranted) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface), RectangleShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .clickable(onClick = onGrantClick),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ALLOW ACCESS",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
