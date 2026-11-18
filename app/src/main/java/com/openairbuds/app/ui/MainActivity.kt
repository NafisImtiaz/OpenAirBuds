package com.openairbuds.app.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.ui.components.KenkashiConnectionErrorBanner
import com.openairbuds.app.ui.components.KenkashiDivider
import com.openairbuds.app.ui.setup.SetupScreen
import com.openairbuds.app.ui.tabs.ControlsTab
import com.openairbuds.app.ui.tabs.SettingsTab
import com.openairbuds.app.ui.tabs.SoundTab
import com.openairbuds.app.ui.theme.AppThemeMode
import com.openairbuds.app.ui.theme.KenkashiMotion
import com.openairbuds.app.ui.theme.OpenAirBudsTheme
import com.openairbuds.app.ui.theme.ThemeManager

private enum class AppScreen {
    SETUP,
    EQUALIZER,
    MAIN
}

class MainActivity : ComponentActivity() {

    private val viewModel: BudsViewModel by viewModels()

    private var isBluetoothGrantedState by mutableStateOf(false)
    private var isPhoneGrantedState by mutableStateOf(false)
    private var isNotificationGrantedState by mutableStateOf(false)

    private val bluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissionStates()
        if (checkBluetoothGranted()) {
            viewModel.refreshPairedDevices()
        }
    }

    private val phoneLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        refreshPermissionStates()
    }

    private val notificationLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        refreshPermissionStates()
    }

    private fun requestBluetoothPermission() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        bluetoothLauncher.launch(perms)
    }

    private fun requestPhonePermission() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            arrayOf(Manifest.permission.ANSWER_PHONE_CALLS, Manifest.permission.READ_PHONE_STATE)
        } else {
            arrayOf(Manifest.permission.READ_PHONE_STATE)
        }
        phoneLauncher.launch(perms)
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            isNotificationGrantedState = true
        }
    }

    private fun refreshPermissionStates() {
        isBluetoothGrantedState = checkBluetoothGranted()
        isPhoneGrantedState = checkPhoneGranted()
        isNotificationGrantedState = checkNotificationGranted()
    }

    private fun checkBluetoothGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkPhoneGranted(): Boolean {
        val readPhone = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        val answerCalls = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) == PackageManager.PERMISSION_GRANTED
        } else true
        return readPhone && answerCalls
    }

    private fun checkNotificationGranted(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else true

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPermissionStates()

        setContent {
            val themeManager = remember { ThemeManager(applicationContext) }
            val state by viewModel.budsState.collectAsStateWithLifecycle()
            val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            OpenAirBudsTheme(themeMode = themeManager.currentTheme) {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var showEqualizerScreen by rememberSaveable { mutableStateOf(false) }
                var showSetupScreen by rememberSaveable { mutableStateOf(false) }

                val needsSetup = !preferences.isOnboardingCompleted || !isBluetoothGrantedState || showSetupScreen
                val canDismissSetup = preferences.isOnboardingCompleted && isBluetoothGrantedState

                BackHandler(enabled = showEqualizerScreen || (needsSetup && canDismissSetup)) {
                    if (showEqualizerScreen) {
                        showEqualizerScreen = false
                    } else if (showSetupScreen) {
                        showSetupScreen = false
                    }
                }

                val systemInDark = isSystemInDarkTheme()
                val isDark = themeManager.currentTheme.isDark(systemInDark)
                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        insetsController.isAppearanceLightStatusBars = !isDark
                        insetsController.isAppearanceLightNavigationBars = !isDark
                    }
                }

                val currentScreen = when {
                    needsSetup -> AppScreen.SETUP
                    showEqualizerScreen -> AppScreen.EQUALIZER
                    else -> AppScreen.MAIN
                }

                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        if (targetState != AppScreen.MAIN && initialState == AppScreen.MAIN) {
                            (slideInHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { it } +
                                    fadeIn(animationSpec = KenkashiMotion.mechanicalTween()))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { -it / 3 } +
                                            fadeOut(animationSpec = KenkashiMotion.mechanicalTween())
                                )
                        } else if (targetState == AppScreen.MAIN && initialState != AppScreen.MAIN) {
                            (slideInHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { -it / 3 } +
                                    fadeIn(animationSpec = KenkashiMotion.mechanicalTween()))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { it } +
                                            fadeOut(animationSpec = KenkashiMotion.mechanicalTween())
                                )
                        } else {
                            (slideInHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { it } +
                                    fadeIn(animationSpec = KenkashiMotion.mechanicalTween()))
                                .togetherWith(
                                    slideOutHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { -it } +
                                            fadeOut(animationSpec = KenkashiMotion.mechanicalTween())
                                )
                        }
                    },
                    label = "ScreenPushPop",
                    modifier = Modifier.fillMaxSize()
                ) { screen ->
                    when (screen) {
                        AppScreen.SETUP -> {
                            SetupScreen(
                                isBluetoothGranted = isBluetoothGrantedState,
                                isPhoneGranted = isPhoneGrantedState,
                                isNotificationGranted = isNotificationGrantedState,
                                onRequestBluetoothPermission = { requestBluetoothPermission() },
                                onRequestPhonePermission = { requestPhonePermission() },
                                onRequestNotificationPermission = { requestNotificationPermission() },
                                onContinue = {
                                    viewModel.setOnboardingCompleted(true)
                                    showSetupScreen = false
                                },
                                canDismiss = canDismissSetup,
                                onDismiss = {
                                    showSetupScreen = false
                                }
                            )
                        }
                        AppScreen.EQUALIZER -> {
                            EqualizerScreen(
                                state = state,
                                onNavigateBack = { showEqualizerScreen = false },
                                onSetEqPreset = { viewModel.setEqPreset(it) },
                                onSetCustomEqBand = { band, gain -> viewModel.setCustomEqBand(band, gain) },
                                onResetCustomEq = { viewModel.resetCustomEq() },
                                onSelectCustomEqProfile = { viewModel.selectCustomEqProfile(it) },
                                onAddCustomEqProfile = { viewModel.addCustomEqProfile() }
                            )
                        }
                        AppScreen.MAIN -> {
                            Scaffold(
                                topBar = {
                                    val headerTitle = (state.deviceName ?: state.deviceProfile.displayName).uppercase()
                                    val outlineColor = MaterialTheme.colorScheme.outline
                                    CenterAlignedTopAppBar(
                                        title = {
                                            Text(
                                                text = headerTitle,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                letterSpacing = 2.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                            containerColor = MaterialTheme.colorScheme.background
                                        ),
                                        modifier = Modifier.drawBehind {
                                            drawLine(
                                                color = outlineColor,
                                                start = Offset(0f, size.height),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                        }
                                    )
                                },
                                bottomBar = {
                                    KenkashiBottomNavigationBar(
                                        selectedTab = selectedTab,
                                        onTabSelected = { selectedTab = it }
                                    )
                                },
                                containerColor = MaterialTheme.colorScheme.background,
                                contentWindowInsets = WindowInsets(0, 0, 0, 0)
                            ) { innerPadding ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    val errorMsg = (state.connectionStatus as? ConnectionStatus.Error)?.message
                                    val isConflict = state.isChannelHeldByOtherApp || errorMsg?.contains("realme Link", ignoreCase = true) == true
                                    val hasConnectionError = state.connectionStatus is ConnectionStatus.Error || isConflict

                                    AnimatedVisibility(
                                        visible = hasConnectionError,
                                        enter = slideInVertically(initialOffsetY = { -it }) + expandVertically(),
                                        exit = slideOutVertically(targetOffsetY = { -it }) + shrinkVertically()
                                    ) {
                                        KenkashiConnectionErrorBanner(
                                            isConflict = isConflict,
                                            errorMessage = errorMsg,
                                            onRetry = { viewModel.retryConnection() },
                                            onRelease = { viewModel.releaseForRealmeLink() }
                                        )
                                    }

                                    AnimatedContent(
                                        targetState = selectedTab,
                                        transitionSpec = {
                                            if (targetState > initialState) {
                                                (slideInHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { it } +
                                                        fadeIn(animationSpec = KenkashiMotion.mechanicalTween()))
                                                    .togetherWith(
                                                        slideOutHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { -it } +
                                                                fadeOut(animationSpec = KenkashiMotion.mechanicalTween())
                                                    )
                                            } else {
                                                (slideInHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { -it } +
                                                        fadeIn(animationSpec = KenkashiMotion.mechanicalTween()))
                                                    .togetherWith(
                                                        slideOutHorizontally(animationSpec = KenkashiMotion.mechanicalTween()) { it } +
                                                                fadeOut(animationSpec = KenkashiMotion.mechanicalTween())
                                                    )
                                            }
                                        },
                                        label = "TabPagingSlide",
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) { tab ->
                                        when (tab) {
                                        0 -> SoundTab(
                                            state = state,
                                            pairedDevices = pairedDevices,
                                            currentTheme = themeManager.currentTheme,
                                            onConnect = { viewModel.connect(it) },
                                            onRefresh = { viewModel.refreshPairedDevices() },
                                            onRetry = { viewModel.retryConnection() },
                                            onRelease = { viewModel.releaseForRealmeLink() },
                                            onSetNoiseMode = { viewModel.setNoiseMode(it) },
                                            onSetAncLevel = { viewModel.setAncLevel(it) },
                                            onToggleWindNoiseReduction = { viewModel.setWindNoiseReduction(it) },
                                            onToggleBass = { viewModel.setBassWaveEnabled(!state.isBassWaveEnabled) },
                                            onSetBassWaveEnabled = { viewModel.setBassWaveEnabled(it) },
                                            onSetBassWaveLevel = { viewModel.setBassWaveLevel(it) },
                                            onToggleHiResAudio = { viewModel.setHiResAudio(it) },
                                            onToggleWear = { viewModel.setInEarDetection(it) },
                                            onToggleVoiceEnhance = { viewModel.setVoiceEnhance(it) },
                                            onSetEqPreset = { viewModel.setEqPreset(it) },
                                            onSetCustomEqBand = { band, gain -> viewModel.setCustomEqBand(band, gain) },
                                            onResetCustomEq = { viewModel.resetCustomEq() },
                                            onOpenEqualizerScreen = { showEqualizerScreen = true }
                                        )
                                        1 -> ControlsTab(
                                            state = state,
                                            currentTheme = themeManager.currentTheme,
                                            onSetHoldCycle = { anc, trans, norm -> viewModel.setHoldCycle(anc, trans, norm) },
                                            onUpdateGesture = { dev, btn, act, fn -> viewModel.setGestureBinding(dev, btn, act, fn) }
                                        )
                                        2 -> SettingsTab(
                                            state = state,
                                            currentTheme = themeManager.currentTheme,
                                            onSelectTheme = { themeManager.setTheme(it) },
                                            onToggleGameMode = { viewModel.setGameMode(!state.isGameModeEnabled) },
                                            onToggleDual = { viewModel.setDualConnection(!state.isDualConnectionEnabled) },
                                            onStartFitTest = { viewModel.startFitTest(it) },
                                            onToggleRing = { viewModel.findMyBuds(!state.isFindMyBudsRinging) },
                                            onToggleAutoAnswerCalls = { viewModel.setAutoAnswerCalls(it) },
                                            onToggleFindPhone = { viewModel.setFindPhoneEnabled(it) },
                                            onTogglePhoneRing = { viewModel.togglePhoneRinging() },
                                            onOpenSetupScreen = { showSetupScreen = true },
                                            onRelease = { viewModel.releaseForRealmeLink() }
                                        )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStates()
        if (checkBluetoothGranted()) {
            viewModel.refreshPairedDevices()
        }
    }
}

@Composable
fun KenkashiBottomNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        0 to "Sound",
        1 to "Controls",
        2 to "Settings"
    )
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        KenkashiDivider()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
        ) {
            val count = items.size
            val tabWidth = maxWidth / count
            val targetX = tabWidth * selectedTab.coerceIn(0, count - 1)
            val animatedX by animateDpAsState(
                targetValue = targetX,
                animationSpec = KenkashiMotion.mechanicalTween(KenkashiMotion.DurationFast),
                label = "BottomNavIndicatorSlide"
            )

            // Sliding hairline indicator at the bottom edge of the nav bar
            Box(
                modifier = Modifier
                    .offset { IntOffset(x = animatedX.roundToPx(), y = 0) }
                    .width(tabWidth)
                    .height(2.dp)
                    .align(Alignment.BottomStart)
                    .background(MaterialTheme.colorScheme.primary, RectangleShape)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, (tabIndex, title) ->
                    val isSelected = selectedTab == tabIndex
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = KenkashiMotion.mechanicalTween(KenkashiMotion.DurationFast),
                        label = "BottomNavTextColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable {
                                if (!isSelected) {
                                    runCatching {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                                onTabSelected(tabIndex)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = title.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 0.5.sp,
                            color = animatedTextColor,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }

                    if (index < items.size - 1) {
                        VerticalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}
