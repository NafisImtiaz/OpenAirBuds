package com.openairbuds.app.ui.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
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
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.GestureBinding
import com.openairbuds.app.protocol.OpoConstants
import com.openairbuds.app.ui.components.KenkashiBadge
import com.openairbuds.app.ui.components.KenkashiDivider
import com.openairbuds.app.ui.components.KenkashiSectionPanel
import com.openairbuds.app.ui.components.KenkashiSegmentedBar
import com.openairbuds.app.ui.components.KenkashiSwitch
import com.openairbuds.app.ui.theme.AppThemeMode
import com.openairbuds.app.ui.theme.KenkashiMotion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ControlsTab(
    state: BudsState,
    currentTheme: AppThemeMode = AppThemeMode.KENKASHI_PAPER,
    onSetHoldCycle: (anc: Boolean, transparency: Boolean, normal: Boolean) -> Unit,
    onUpdateGesture: (device: Byte, button: Byte, action: Byte, function: Byte) -> Unit,
    modifier: Modifier = Modifier,
    selectedEarbudTab: Int? = null,
    onSelectEarbudTab: ((Int) -> Unit)? = null
) {
    var internalEarbudTab by remember { mutableIntStateOf(0) } // 0 = Left, 1 = Right
    val activeEarbudTab = selectedEarbudTab ?: internalEarbudTab
    val setEarbudTab: (Int) -> Unit = { idx ->
        internalEarbudTab = idx
        onSelectEarbudTab?.invoke(idx)
    }
    var selectedGestureForEdit by remember { mutableStateOf<GestureBinding?>(null) }
    var validationWarning by remember { mutableStateOf<String?>(null) }

    // Active cycle flags from hardware mask
    val mask = state.holdCycleMask
    val hasAnc = (mask and OpoConstants.HOLD_CYCLE_ANC.toInt()) != 0
    val hasTransparency = (mask and OpoConstants.HOLD_CYCLE_TRANSPARENCY.toInt()) != 0
    val hasNormal = (mask and OpoConstants.HOLD_CYCLE_OFF.toInt()) != 0

    // Available gesture mappings
    val availableFunctions = listOf(
        OpoConstants.GESTURE_FN_PLAY_PAUSE to "Play / Pause",
        OpoConstants.GESTURE_FN_NEXT_TRACK to "Next Track",
        OpoConstants.GESTURE_FN_PREVIOUS_TRACK to "Previous Track",
        OpoConstants.GESTURE_FN_CYCLE_ANC to "Noise Control (ANC)",
        OpoConstants.GESTURE_FN_VOLUME_UP to "Volume Up",
        OpoConstants.GESTURE_FN_VOLUME_DOWN to "Volume Down",
        OpoConstants.GESTURE_FN_VOICE_ASSISTANT to "Voice Assistant",
        OpoConstants.GESTURE_FN_GAME_MODE to "Game Mode",
        OpoConstants.GESTURE_FN_SWITCH_DEVICES to "Switch Devices",
        OpoConstants.GESTURE_FN_NONE to "None"
    )

    val currentBindings = state.gestureBindings
    val activeBindings = remember(currentBindings) {
        if (currentBindings.isNotEmpty()) currentBindings else listOf(
            GestureBinding(device = 1, button = 1, action = 1, function = OpoConstants.GESTURE_FN_PLAY_PAUSE),
            GestureBinding(device = 1, button = 1, action = 2, function = OpoConstants.GESTURE_FN_NEXT_TRACK),
            GestureBinding(device = 1, button = 1, action = 3, function = OpoConstants.GESTURE_FN_PREVIOUS_TRACK),
            GestureBinding(device = 1, button = 1, action = 4, function = OpoConstants.GESTURE_FN_CYCLE_ANC),
            GestureBinding(device = 2, button = 1, action = 1, function = OpoConstants.GESTURE_FN_PLAY_PAUSE),
            GestureBinding(device = 2, button = 1, action = 2, function = OpoConstants.GESTURE_FN_NEXT_TRACK),
            GestureBinding(device = 2, button = 1, action = 3, function = OpoConstants.GESTURE_FN_PREVIOUS_TRACK),
            GestureBinding(device = 2, button = 1, action = 4, function = OpoConstants.GESTURE_FN_CYCLE_ANC)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // 1. LEFT / RIGHT EARBUD SEGMENTED SELECTOR
        KenkashiSegmentedBar(
            options = listOf("Left Earbud", "Right Earbud"),
            selectedIndex = activeEarbudTab,
            onSelectIndex = setEarbudTab
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. SINGLE EARBUD LINE-ART SHOWCASE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .background(MaterialTheme.colorScheme.surfaceContainer, RectangleShape)
                .clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = currentTheme.isDark(systemDark)

            AnimatedContent(
                targetState = activeEarbudTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { width -> width / 2 } +
                                fadeIn(animationSpec = KenkashiMotion.mechanicalTween()))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { width -> -width / 2 } +
                                        fadeOut(animationSpec = KenkashiMotion.mechanicalTween())
                            )
                    } else {
                        (slideInHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { width -> -width / 2 } +
                                fadeIn(animationSpec = KenkashiMotion.mechanicalTween()))
                            .togetherWith(
                                slideOutHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { width -> width / 2 } +
                                        fadeOut(animationSpec = KenkashiMotion.mechanicalTween())
                            )
                    }
                },
                label = "EarbudShowcaseSlide",
                modifier = Modifier.fillMaxSize()
            ) { targetTab ->
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val drawableRes = when {
                        targetTab == 0 && isDarkTheme -> R.drawable.ic_kenkashi_bud_left_dark
                        targetTab == 0 -> R.drawable.ic_kenkashi_bud_left
                        isDarkTheme -> R.drawable.ic_kenkashi_bud_right_dark
                        else -> R.drawable.ic_kenkashi_bud_right
                    }

                    Image(
                        painter = painterResource(id = drawableRes),
                        contentDescription = if (targetTab == 0) "Left Earbud" else "Right Earbud",
                        modifier = Modifier
                            .size(130.dp)
                            .padding(8.dp),
                        contentScale = ContentScale.Fit
                    )

                    // Small watermark label in the corner
                    Text(
                        text = if (targetTab == 0) "L" else "R",
                        fontFamily = FontFamily.Serif,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 3. GESTURE ACTIONS LIST
        val devCode = if (activeEarbudTab == 0) 1.toByte() else 2.toByte()
        val currentDevBindings = activeBindings.filter { it.device == devCode }

        val singleTap = currentDevBindings.firstOrNull { it.action == 1.toByte() }
            ?: GestureBinding(device = devCode, button = 1, action = 1, function = OpoConstants.GESTURE_FN_PLAY_PAUSE)
        val doubleTap = currentDevBindings.firstOrNull { it.action == 2.toByte() }
            ?: GestureBinding(device = devCode, button = 1, action = 2, function = OpoConstants.GESTURE_FN_NEXT_TRACK)
        val tripleTap = currentDevBindings.firstOrNull { it.action == 3.toByte() }
            ?: GestureBinding(device = devCode, button = 1, action = 3, function = OpoConstants.GESTURE_FN_PREVIOUS_TRACK)
        val touchAndHold = currentDevBindings.firstOrNull { it.action == 4.toByte() }
            ?: GestureBinding(device = devCode, button = 1, action = 4, function = OpoConstants.GESTURE_FN_CYCLE_ANC)

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Touch Controls",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(14.dp))

            KenkashiSectionPanel {
                // Row 1: Single Tap
                KenkashiGestureRow(
                    gestureName = "Single Tap",
                    actionName = singleTap.actionName,
                    onClick = { selectedGestureForEdit = singleTap }
                )

                // Row 2: Double Tap
                KenkashiGestureRow(
                    gestureName = "Double Tap",
                    actionName = doubleTap.actionName,
                    onClick = { selectedGestureForEdit = doubleTap }
                )

                // Row 3: Triple Tap
                KenkashiGestureRow(
                    gestureName = "Triple Tap",
                    actionName = tripleTap.actionName,
                    onClick = { selectedGestureForEdit = tripleTap }
                )

                // Row 4: Touch & Hold
                KenkashiGestureRow(
                    gestureName = "Touch & Hold",
                    actionName = touchAndHold.actionName,
                    onClick = { selectedGestureForEdit = touchAndHold },
                    showDivider = false
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. ANC CYCLE MODE SECTION
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "ANC Cycle Mode",
                fontFamily = FontFamily.Serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Select modes included when holding earbud",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainer, RectangleShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ANC toggle block
                KenkashiCycleBlock(
                    title = "ANC",
                    isActive = hasAnc,
                    onClick = {
                        val activeCount = (if (hasAnc) 1 else 0) + (if (hasTransparency) 1 else 0) + (if (hasNormal) 1 else 0)
                        if (hasAnc && activeCount <= 2) {
                            validationWarning = "At least 2 modes must be selected in cycle"
                        } else {
                            validationWarning = null
                            onSetHoldCycle(!hasAnc, hasTransparency, hasNormal)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // Transparency toggle block
                KenkashiCycleBlock(
                    title = "TRANSPARENCY",
                    isActive = hasTransparency,
                    onClick = {
                        val activeCount = (if (hasAnc) 1 else 0) + (if (hasTransparency) 1 else 0) + (if (hasNormal) 1 else 0)
                        if (hasTransparency && activeCount <= 2) {
                            validationWarning = "At least 2 modes must be selected in cycle"
                        } else {
                            validationWarning = null
                            onSetHoldCycle(hasAnc, !hasTransparency, hasNormal)
                        }
                    },
                    modifier = Modifier.weight(1.3f)
                )

                // Normal toggle block
                KenkashiCycleBlock(
                    title = "OFF",
                    isActive = hasNormal,
                    onClick = {
                        val activeCount = (if (hasAnc) 1 else 0) + (if (hasTransparency) 1 else 0) + (if (hasNormal) 1 else 0)
                        if (hasNormal && activeCount <= 2) {
                            validationWarning = "At least 2 modes must be selected in cycle"
                        } else {
                            validationWarning = null
                            onSetHoldCycle(hasAnc, hasTransparency, !hasNormal)
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            AnimatedVisibility(visible = validationWarning != null) {
                Text(
                    text = validationWarning ?: "",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(36.dp))
    }

    // Modal Bottom Sheet for Gesture Action Selection (Kenkashi Minimalist Style)
    val gestureToEdit = selectedGestureForEdit
    if (gestureToEdit != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val target = gestureToEdit
        val actionName = when (target.action) {
            1.toByte() -> "Single Tap"
            2.toByte() -> "Double Tap"
            3.toByte() -> "Triple Tap"
            4.toByte() -> "Touch & Hold"
            else -> "Gesture"
        }
        val earbudSide = if (target.device == 1.toByte()) "Left Earbud" else "Right Earbud"

        ModalBottomSheet(
            onDismissRequest = { selectedGestureForEdit = null },
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
                    text = "$earbudSide • $actionName",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assign Action",
                    fontFamily = FontFamily.Serif,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Normal,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))
                KenkashiDivider()

                availableFunctions.forEach { (funcCode, funcLabel) ->
                    val isSelected = target.function == funcCode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUpdateGesture(target.device, target.button, target.action, funcCode)
                                selectedGestureForEdit = null
                            }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = funcLabel,
                            fontFamily = FontFamily.Serif,
                            fontSize = 16.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isSelected) {
                            KenkashiBadge(text = "ACTIVE")
                        }
                    }
                    KenkashiDivider()
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun KenkashiGestureRow(
    gestureName: String,
    actionName: String,
    onClick: () -> Unit,
    showDivider: Boolean = true
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = gestureName,
                fontFamily = FontFamily.Serif,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = actionName.uppercase(),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showDivider) {
            KenkashiDivider()
        }
    }
}

@Composable
fun KenkashiCycleBlock(
    title: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.5.sp,
            color = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
