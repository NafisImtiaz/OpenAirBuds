package com.openairbuds.app.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openairbuds.app.data.BudsPreferencesRepository
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

    @Suppress("DEPRECATION")
    private fun configureHighRefreshRate() {
        // Enforce hardware acceleration on window
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        // Request dynamic / seamless high display refresh rate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val peakRefreshRate = display?.supportedModes?.maxOfOrNull { it.refreshRate } ?: 120f
            window.attributes = window.attributes.apply {
                preferredRefreshRate = peakRefreshRate
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6.0+ (API 23+) fallback: select high refresh rate mode if supported
            val display = windowManager.defaultDisplay
            val modes = display.supportedModes
            val mode120 = modes.firstOrNull { it.refreshRate in 118f..122f }
                ?: modes.maxByOrNull { it.refreshRate }
            if (mode120 != null && mode120.refreshRate > 60f) {
                window.attributes = window.attributes.apply {
                    preferredDisplayModeId = mode120.modeId
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureHighRefreshRate()
        refreshPermissionStates()

        setContent {
            val themeManager = remember { ThemeManager(applicationContext) }
            val state by viewModel.budsState.collectAsStateWithLifecycle()
            val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()
            val preferences by viewModel.preferences.collectAsStateWithLifecycle()

            OpenAirBudsTheme(themeMode = themeManager.currentTheme) {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var selectedControlBud by rememberSaveable { mutableIntStateOf(0) }
                var showEqualizerScreen by rememberSaveable { mutableStateOf(false) }
                var showSetupScreen by rememberSaveable { mutableStateOf(false) }
                var isOnboardingCompletedState by rememberSaveable {
                    mutableStateOf(BudsPreferencesRepository.isOnboardingCompletedSync(applicationContext, isBluetoothGrantedState))
                }

                // Gate setup requirement: uncompleted onboarding OR missing essential permissions OR explicit user navigation from Settings
                val isOnboardingCompleted = isOnboardingCompletedState || preferences.isOnboardingCompleted
                val needsSetup = shouldShowSetup(isOnboardingCompleted, isBluetoothGrantedState, showSetupScreen)
                val canDismissSetup = isBluetoothGrantedState && isOnboardingCompleted

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
                                    isOnboardingCompletedState = true
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
                                onAddCustomEqProfile = { viewModel.addCustomEqProfile() },
                                onRenameCustomEqProfile = { id, name -> viewModel.renameCustomEqProfile(id, name) }
                            )
                        }
                        AppScreen.MAIN -> {
                            val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 3 })
                            val coroutineScope = rememberCoroutineScope()
                            var beyondPageCount by remember { mutableIntStateOf(0) }
                            LaunchedEffect(Unit) {
                                withFrameNanos { }
                                beyondPageCount = 2
                            }
                            LaunchedEffect(pagerState.currentPage) {
                                selectedTab = pagerState.currentPage
                            }

                            val onConnect = remember(viewModel) { { dev: android.bluetooth.BluetoothDevice -> viewModel.connect(dev) } }
                            val onRefresh = remember(viewModel) { { viewModel.refreshPairedDevices() } }
                            val onRetry = remember(viewModel) { { viewModel.retryConnection() } }
                            val onRelease = remember(viewModel) { { viewModel.releaseForRealmeLink() } }
                            val onSetNoiseMode = remember(viewModel) { { mode: com.openairbuds.app.model.NoiseControlMode -> viewModel.setNoiseMode(mode) } }
                            val onSetAncLevel = remember(viewModel) { { lvl: com.openairbuds.app.model.AncLevel -> viewModel.setAncLevel(lvl) } }
                            val onToggleWindNoiseReduction = remember(viewModel) { { enabled: Boolean -> viewModel.setWindNoiseReduction(enabled) } }
                            val onToggleBass = remember(viewModel) { { viewModel.setBassWaveEnabled(!viewModel.budsState.value.isBassWaveEnabled) } }
                            val onSetBassWaveEnabled = remember(viewModel) { { enabled: Boolean -> viewModel.setBassWaveEnabled(enabled) } }
                            val onSetBassWaveLevel = remember(viewModel) { { lvl: Int -> viewModel.setBassWaveLevel(lvl) } }
                            val onToggleHiResAudio = remember(viewModel) { { enabled: Boolean -> viewModel.setHiResAudio(enabled) } }
                            val onToggleWear = remember(viewModel) { { enabled: Boolean -> viewModel.setInEarDetection(enabled) } }
                            val onToggleVoiceEnhance = remember(viewModel) { { enabled: Boolean -> viewModel.setVoiceEnhance(enabled) } }
                            val onOpenEqualizerScreen = remember { { showEqualizerScreen = true } }
                            val onInspectBudControls = remember(pagerState, coroutineScope) {
                                { budIdx: Int ->
                                    selectedControlBud = budIdx.coerceIn(0, 1)
                                    selectedTab = 1
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(
                                            page = 1,
                                            animationSpec = KenkashiMotion.tabTween()
                                        )
                                    }
                                    Unit
                                }
                            }
                            val onSelectEarbudTab = remember { { idx: Int -> selectedControlBud = idx } }
                            val onSetHoldCycle = remember(viewModel) { { anc: Boolean, trans: Boolean, norm: Boolean -> viewModel.setHoldCycle(anc, trans, norm) } }
                            val onUpdateGesture = remember(viewModel) { { dev: Byte, btn: Byte, act: Byte, fn: Byte -> viewModel.setGestureBinding(dev, btn, act, fn) } }
                            val onSelectTheme = remember(themeManager) { { mode: AppThemeMode -> themeManager.setTheme(mode) } }
                            val onToggleGameMode = remember(viewModel) { { viewModel.setGameMode(!viewModel.budsState.value.isGameModeEnabled) } }
                            val onToggleDual = remember(viewModel) { { viewModel.setDualConnection(!viewModel.budsState.value.isDualConnectionEnabled) } }
                            val onStartFitTest = remember(viewModel) { { start: Boolean -> viewModel.startFitTest(start) } }
                            val onToggleRing = remember(viewModel) { { viewModel.findMyBuds(!viewModel.budsState.value.isFindMyBudsRinging) } }
                            val onToggleAutoAnswerCalls = remember(viewModel) { { enabled: Boolean -> viewModel.setAutoAnswerCalls(enabled) } }
                            val onToggleFindPhone = remember(viewModel) { { enabled: Boolean -> viewModel.setFindPhoneEnabled(enabled) } }
                            val onTogglePhoneRing = remember(viewModel) { { viewModel.togglePhoneRinging() } }
                            val onOpenSetupScreen = remember { { showSetupScreen = true } }

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
                                        pagerState = pagerState,
                                        onTabSelected = { targetPage ->
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(
                                                    page = targetPage,
                                                    animationSpec = KenkashiMotion.tabTween()
                                                )
                                            }
                                        }
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
                                            onRetry = onRetry,
                                            onRelease = onRelease
                                        )
                                    }

                                    HorizontalPager(
                                        state = pagerState,
                                        beyondViewportPageCount = beyondPageCount,
                                        key = { it },
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth()
                                    ) { tab ->
                                        when (tab) {
                                            0 -> SoundTab(
                                                state = state,
                                                pairedDevices = pairedDevices,
                                                currentTheme = themeManager.currentTheme,
                                                onConnect = onConnect,
                                                onRefresh = onRefresh,
                                                onRetry = onRetry,
                                                onSetNoiseMode = onSetNoiseMode,
                                                onSetAncLevel = onSetAncLevel,
                                                onToggleWindNoiseReduction = onToggleWindNoiseReduction,
                                                onToggleBass = onToggleBass,
                                                onSetBassWaveEnabled = onSetBassWaveEnabled,
                                                onSetBassWaveLevel = onSetBassWaveLevel,
                                                onToggleHiResAudio = onToggleHiResAudio,
                                                onToggleWear = onToggleWear,
                                                onToggleVoiceEnhance = onToggleVoiceEnhance,
                                                onOpenEqualizerScreen = onOpenEqualizerScreen,
                                                onInspectBudControls = onInspectBudControls
                                            )
                                            1 -> ControlsTab(
                                                state = state,
                                                currentTheme = themeManager.currentTheme,
                                                selectedEarbudTab = selectedControlBud,
                                                onSelectEarbudTab = onSelectEarbudTab,
                                                onSetHoldCycle = onSetHoldCycle,
                                                onUpdateGesture = onUpdateGesture
                                            )
                                            2 -> SettingsTab(
                                                state = state,
                                                currentTheme = themeManager.currentTheme,
                                                onSelectTheme = onSelectTheme,
                                                onToggleGameMode = onToggleGameMode,
                                                onToggleDual = onToggleDual,
                                                onStartFitTest = onStartFitTest,
                                                onToggleRing = onToggleRing,
                                                onToggleAutoAnswerCalls = onToggleAutoAnswerCalls,
                                                onToggleFindPhone = onToggleFindPhone,
                                                onTogglePhoneRing = onTogglePhoneRing,
                                                onOpenSetupScreen = onOpenSetupScreen,
                                                onRelease = onRelease
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

    companion object {
        fun shouldShowSetup(
            isOnboardingCompleted: Boolean,
            isBluetoothGranted: Boolean,
            showSetupScreen: Boolean
        ): Boolean {
            if (showSetupScreen) return true
            if (!isBluetoothGranted) return true
            return !isOnboardingCompleted
        }
    }
}

@Composable
fun KenkashiBottomNavigationBar(
    pagerState: PagerState,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        0 to "Sound",
        1 to "Controls",
        2 to "Settings"
    )
    val haptic = LocalHapticFeedback.current
    val count = items.size
    val indicatorColor = MaterialTheme.colorScheme.primary
    val selectedTab = pagerState.targetPage

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        KenkashiDivider()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp)
                .drawBehind {
                    val tabWidth = size.width / count
                    val indicatorHeight = 2.dp.toPx()
                    val pos = KenkashiMotion.clampIndicatorPosition(
                        page = pagerState.currentPage,
                        offsetFraction = pagerState.currentPageOffsetFraction,
                        count = count
                    )
                    val left = tabWidth * pos
                    drawRect(
                        color = indicatorColor,
                        topLeft = Offset(left, size.height - indicatorHeight),
                        size = Size(tabWidth, indicatorHeight)
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, (tabIndex, title) ->
                    val isSelected = selectedTab == tabIndex
                    val animatedTextColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = KenkashiMotion.tabTween(),
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
