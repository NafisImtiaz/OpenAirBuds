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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.ui.components.KenkashiConnectionErrorBanner
import com.openairbuds.app.ui.components.KenkashiDivider
import com.openairbuds.app.ui.tabs.ControlsTab
import com.openairbuds.app.ui.tabs.SettingsTab
import com.openairbuds.app.ui.tabs.SoundTab
import com.openairbuds.app.ui.theme.AppThemeMode
import com.openairbuds.app.ui.theme.OpenAirBudsTheme
import com.openairbuds.app.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {

    private val viewModel: BudsViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val bluetoothGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
        if (bluetoothGranted) {
            viewModel.refreshPairedDevices()
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeManager = remember { ThemeManager(applicationContext) }
            val state by viewModel.budsState.collectAsStateWithLifecycle()
            val pairedDevices by viewModel.pairedDevices.collectAsStateWithLifecycle()

            OpenAirBudsTheme(themeMode = themeManager.currentTheme) {
                var selectedTab by rememberSaveable { mutableIntStateOf(0) }
                var showEqualizerScreen by rememberSaveable { mutableStateOf(false) }

                BackHandler(enabled = showEqualizerScreen) {
                    showEqualizerScreen = false
                }

                val view = LocalView.current
                if (!view.isInEditMode) {
                    SideEffect {
                        val window = (view.context as Activity).window
                        val insetsController = WindowCompat.getInsetsController(window, view)
                        val isDark = themeManager.currentTheme == AppThemeMode.KENKASHI_DARK || themeManager.currentTheme == AppThemeMode.NOTHING_DARK
                        insetsController.isAppearanceLightStatusBars = !isDark
                        insetsController.isAppearanceLightNavigationBars = !isDark
                    }
                }

                if (showEqualizerScreen) {
                    EqualizerScreen(
                        state = state,
                        onNavigateBack = { showEqualizerScreen = false },
                        onSetEqPreset = { viewModel.setEqPreset(it) },
                        onSetCustomEqBand = { band, gain -> viewModel.setCustomEqBand(band, gain) },
                        onResetCustomEq = { viewModel.resetCustomEq() },
                        onSelectCustomEqProfile = { viewModel.selectCustomEqProfile(it) },
                        onAddCustomEqProfile = { viewModel.addCustomEqProfile() }
                    )
                } else {
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

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            ) {
                                when (selectedTab) {
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
                                    onRelease = { viewModel.releaseForRealmeLink() }
                                )
                                }
                            }
                        }
                    }
                }
            }
        }

        checkAndRequestPermissions()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPairedDevices()
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.ANSWER_PHONE_CALLS)
            }
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.READ_PHONE_STATE)
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
    ) {
        KenkashiDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, (tabIndex, title) ->
                val isSelected = selectedTab == tabIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected(tabIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title.uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        letterSpacing = 0.5.sp,
                        textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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
