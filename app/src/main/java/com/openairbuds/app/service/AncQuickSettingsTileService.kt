package com.openairbuds.app.service

import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.openairbuds.app.R
import com.openairbuds.app.bluetooth.OpoBluetoothManager
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.NoiseControlMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AncQuickSettingsTileService : TileService() {

    companion object {
        private const val TAG = "AncTileService"

        /**
         * Requests the system to refresh the Quick Settings tile state.
         * Safe to call from any background coroutine or lifecycle event.
         */
        fun requestTileUpdate(context: Context) {
            try {
                requestListeningState(
                    context,
                    ComponentName(context, AncQuickSettingsTileService::class.java)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to request tile listening state: ${e.message}")
            }
        }
    }

    private var serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var listeningJob: Job? = null
    private lateinit var bluetoothManager: OpoBluetoothManager

    override fun onCreate() {
        super.onCreate()
        serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        bluetoothManager = OpoBluetoothManager.getInstance(this)
    }

    override fun onStartListening() {
        super.onStartListening()
        listeningJob?.cancel()

        val currentState = bluetoothManager.budsState.value

        // Auto-connect only if disconnected and active audio connection is present
        if (currentState.connectionStatus is ConnectionStatus.Disconnected && currentState.isA2dpConnected) {
            val connectedA2dp = bluetoothManager.getConnectedA2dpDevices()
            val candidate = (currentState.deviceAddress?.let { addr ->
                connectedA2dp.firstOrNull { it.address.equals(addr, ignoreCase = true) }
            }) ?: connectedA2dp.firstOrNull { dev ->
                dev.name?.contains("realme", ignoreCase = true) == true ||
                dev.name?.contains("Buds", ignoreCase = true) == true ||
                dev.name?.contains("Air", ignoreCase = true) == true
            }
            if (candidate != null) {
                bluetoothManager.connectToDevice(candidate)
            }
        }

        // Render current state immediately
        updateTile(
            bluetoothManager.budsState.value.noiseMode,
            bluetoothManager.budsState.value.connectionStatus
        )

        // React to live state emissions
        listeningJob = bluetoothManager.budsState
            .onEach { state ->
                updateTile(state.noiseMode, state.connectionStatus)
            }
            .launchIn(serviceScope)
    }

    override fun onStopListening() {
        super.onStopListening()
        listeningJob?.cancel()
        listeningJob = null
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onClick() {
        super.onClick()
        val currentState = bluetoothManager.budsState.value

        // If the connected earbud does not support ANC (e.g. realme Buds T100), toggle Game Mode
        if (currentState.connectionStatus is ConnectionStatus.Connected && !currentState.deviceProfile.capabilities.hasAnc) {
            val nextGame = !currentState.isGameModeEnabled
            bluetoothManager.setGameMode(nextGame)
            qsTile?.let { tile ->
                tile.state = if (nextGame) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
                tile.label = if (nextGame) "Game Mode On" else "Game Mode Off"
                tile.updateTile()
            }
            return
        }

        // Cycle through modes: Noise Cancelling -> Transparency -> Off (Normal) -> Noise Cancelling
        // When only 1 earbud is in-ear, strictly cycle between Off and Transparency
        val nextMode = currentState.noiseMode.next(currentState.placement.areBothInEar)

        if (currentState.connectionStatus !is ConnectionStatus.Connected) {
            // Attempt auto-connect to paired device if available and apply selected mode
            val paired = bluetoothManager.getPairedDevices()
            val realmeDevice = paired.firstOrNull {
                it.name?.contains("realme", ignoreCase = true) == true ||
                it.name?.contains("Buds", ignoreCase = true) == true
            } ?: paired.firstOrNull()

            if (realmeDevice != null) {
                if (currentState.connectionStatus is ConnectionStatus.Connecting) {
                    bluetoothManager.setPendingTargetMode(nextMode)
                } else {
                    bluetoothManager.connectToDevice(realmeDevice, targetModeOnConnect = nextMode)
                }

                qsTile?.let { tile ->
                    tile.state = if (nextMode == NoiseControlMode.OFF) Tile.STATE_INACTIVE else Tile.STATE_ACTIVE
                    tile.label = when (nextMode) {
                        NoiseControlMode.NOISE_CANCELLATION -> getString(R.string.tile_anc_on)
                        NoiseControlMode.TRANSPARENCY -> getString(R.string.tile_anc_transparency)
                        NoiseControlMode.OFF -> getString(R.string.tile_anc_off)
                    }
                    tile.icon = Icon.createWithResource(this, when (nextMode) {
                        NoiseControlMode.NOISE_CANCELLATION -> R.drawable.ic_tile_anc
                        NoiseControlMode.TRANSPARENCY -> R.drawable.ic_tile_transparency
                        NoiseControlMode.OFF -> R.drawable.ic_tile_off
                    })
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        tile.subtitle = "Connecting…"
                    }
                    tile.contentDescription = "Connecting and switching to ${tile.label}…"
                    tile.updateTile()
                }
            }
            return
        }

        // Immediate optimistic UI update for instant visual feedback on tap
        updateTile(nextMode, ConnectionStatus.Connected)

        bluetoothManager.setNoiseControlMode(nextMode)
    }

    private fun updateTile(mode: NoiseControlMode, status: ConnectionStatus) {
        val tile = qsTile ?: return

        if (status !is ConnectionStatus.Connected) {
            tile.state = Tile.STATE_INACTIVE
            tile.label = getString(R.string.tile_anc_label)
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_off)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val isBusy = status is ConnectionStatus.Connecting || status is ConnectionStatus.Handshaking
                tile.subtitle = if (isBusy) "Connecting…" else if (bluetoothManager.budsState.value.isA2dpConnected) "Audio Connected • Tap to sync" else "Disconnected • Tap to connect"
            }
            tile.contentDescription = "Noise Control, disconnected. Tap to connect."
            tile.updateTile()
            return
        }

        val current = bluetoothManager.budsState.value
        val deviceName = current.deviceName?.takeIf { it.isNotBlank() } ?: current.deviceProfile.displayName

        if (!current.deviceProfile.capabilities.hasAnc) {
            tile.state = if (current.isGameModeEnabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            tile.label = if (current.isGameModeEnabled) "Game Mode On" else "Game Mode Off"
            tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_off)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                tile.subtitle = deviceName
            }
            tile.contentDescription = "Game Mode on $deviceName"
            tile.updateTile()
            return
        }

        when (mode) {
            NoiseControlMode.NOISE_CANCELLATION -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_anc_on) // "Noise Cancelling"
                tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_anc)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = deviceName
                }
                tile.contentDescription = "Noise Cancelling active on $deviceName"
            }
            NoiseControlMode.TRANSPARENCY -> {
                tile.state = Tile.STATE_ACTIVE
                tile.label = getString(R.string.tile_anc_transparency) // "Transparency"
                tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_transparency)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = deviceName
                }
                tile.contentDescription = "Transparency mode active on $deviceName"
            }
            NoiseControlMode.OFF -> {
                tile.state = Tile.STATE_INACTIVE
                tile.label = getString(R.string.tile_anc_off) // "Normal"
                tile.icon = Icon.createWithResource(this, R.drawable.ic_tile_off)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    tile.subtitle = deviceName
                }
                tile.contentDescription = "Normal mode on $deviceName"
            }
        }
        tile.updateTile()
    }
}
