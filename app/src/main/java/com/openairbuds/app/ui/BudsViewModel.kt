package com.openairbuds.app.ui

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.openairbuds.app.bluetooth.OpoBluetoothManager
import com.openairbuds.app.data.BudsPreferences
import com.openairbuds.app.data.BudsPreferencesRepository
import com.openairbuds.app.model.AncLevel
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.model.GestureBinding
import com.openairbuds.app.model.NoiseControlMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BudsViewModel(application: Application) : AndroidViewModel(application) {

    private val bluetoothManager = OpoBluetoothManager.getInstance(application)
    private val preferencesRepository = BudsPreferencesRepository.getInstance(application)

    val budsState: StateFlow<BudsState> = bluetoothManager.budsState

    val preferences: StateFlow<BudsPreferences> = preferencesRepository.preferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = BudsPreferences()
        )

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()

    private var userInitiatedDisconnect = false

    init {
        // Continuous, race-free seamless auto-connection observer
        viewModelScope.launch {
            combine(
                preferencesRepository.preferencesFlow,
                bluetoothManager.budsState
            ) { prefs, state ->
                prefs to state
            }.collect { (prefs, state) ->
                if (shouldTriggerAutoConnect(
                        autoConnectEnabled = prefs.autoConnectEnabled,
                        userInitiatedDisconnect = userInitiatedDisconnect,
                        isA2dpConnected = state.isA2dpConnected,
                        connectionStatus = state.connectionStatus
                    )
                ) {
                    val devices = bluetoothManager.getPairedDevices().also { _pairedDevices.value = it }
                    val candidate = (if (state.deviceAddress != null) devices.firstOrNull { it.address.equals(state.deviceAddress, ignoreCase = true) } else null)
                        ?: findCandidateDevice(devices, prefs.lastConnectedMac, bluetoothManager.getConnectedA2dpDevices())
                    if (candidate != null && budsState.value.connectionStatus is ConnectionStatus.Disconnected) {
                        Log.i("BudsViewModel", "Seamless auto-connect to: ${candidate.name} [${candidate.address}]")
                        connect(candidate)
                    }
                }
            }
        }
    }

    fun refreshPairedDevices() {
        val devices = bluetoothManager.getPairedDevices()
        _pairedDevices.value = devices

        val currentStatus = budsState.value.connectionStatus
        val isBusyConnecting = currentStatus is ConnectionStatus.Connecting || currentStatus is ConnectionStatus.Handshaking
        if (isBusyConnecting) return

        val prefs = preferences.value
        if (shouldTriggerAutoConnect(
                autoConnectEnabled = prefs.autoConnectEnabled,
                userInitiatedDisconnect = userInitiatedDisconnect,
                isA2dpConnected = budsState.value.isA2dpConnected,
                connectionStatus = currentStatus
            )
        ) {
            val connectedA2dp = bluetoothManager.getConnectedA2dpDevices()
            val candidate = findCandidateDevice(devices, prefs.lastConnectedMac, connectedA2dp)
            if (candidate != null && budsState.value.connectionStatus is ConnectionStatus.Disconnected) {
                Log.i("BudsViewModel", "Refreshing and auto-connecting to: ${candidate.name} [${candidate.address}]")
                connect(candidate)
            }
        }
    }

    fun connect(device: BluetoothDevice) {
        val currentStatus = budsState.value.connectionStatus
        val currentAddress = budsState.value.deviceAddress
        if ((currentStatus is ConnectionStatus.Connecting || currentStatus is ConnectionStatus.Handshaking) &&
            currentAddress.equals(device.address, ignoreCase = true)
        ) {
            return
        }
        userInitiatedDisconnect = false
        bluetoothManager.connectToDevice(device)
    }

    fun disconnect() {
        userInitiatedDisconnect = true
        bluetoothManager.disconnect()
    }

    fun releaseForRealmeLink() {
        userInitiatedDisconnect = true
        bluetoothManager.releaseChannel()
    }

    fun setNoiseMode(mode: NoiseControlMode) {
        if (mode == NoiseControlMode.NOISE_CANCELLATION && !budsState.value.placement.areBothInEar) {
            return
        }
        val targetLevel = if (mode == NoiseControlMode.NOISE_CANCELLATION) {
            preferences.value.preferredAncLevel
        } else {
            budsState.value.ancLevel
        }
        bluetoothManager.setNoiseControlMode(mode, targetLevel)
    }

    fun setAncLevel(level: AncLevel) {
        val currentMode = budsState.value.noiseMode
        bluetoothManager.setNoiseControlMode(currentMode, level)
    }

    fun setHoldCycle(anc: Boolean, transparency: Boolean, normal: Boolean) {
        var mask = 0
        if (normal) mask = mask or com.openairbuds.app.protocol.OpoConstants.HOLD_CYCLE_OFF.toInt()
        if (transparency) mask = mask or com.openairbuds.app.protocol.OpoConstants.HOLD_CYCLE_TRANSPARENCY.toInt()
        if (anc) mask = mask or com.openairbuds.app.protocol.OpoConstants.HOLD_CYCLE_ANC.toInt()

        val count = (if (normal) 1 else 0) + (if (transparency) 1 else 0) + (if (anc) 1 else 0)
        if (count < 2) return

        bluetoothManager.setHoldCycle(mask.toByte())
    }

    fun setCustomEqBand(bandIndex: Int, gain: Int) {
        val currentGains = budsState.value.customEqGains.toMutableList()
        if (bandIndex in currentGains.indices) {
            val clamped = gain.coerceIn(-6, 6)
            if (currentGains[bandIndex] != clamped) {
                currentGains[bandIndex] = clamped
                bluetoothManager.setCustomEq(currentGains)
            }
        }
    }

    fun setCustomEq(gains: List<Int>) {
        bluetoothManager.setCustomEq(gains)
    }

    fun resetCustomEq() {
        bluetoothManager.setCustomEq(listOf(0, 0, 0, 0, 0, 0))
    }

    fun selectCustomEqProfile(profileId: Int) {
        bluetoothManager.selectCustomEqProfile(profileId)
    }

    fun addCustomEqProfile(): Boolean {
        return bluetoothManager.addCustomEqProfile()
    }

    fun setHiResAudio(enabled: Boolean) {
        bluetoothManager.setHiResAudio(enabled)
    }

    fun setVolumeEnhancer(enabled: Boolean) {
        bluetoothManager.setVolumeEnhancer(enabled)
    }

    fun setWindNoiseReduction(enabled: Boolean) {
        bluetoothManager.setWindNoiseReduction(enabled)
    }

    fun setGameMode(enabled: Boolean) {
        bluetoothManager.setGameMode(enabled)
    }

    fun setInEarDetection(enabled: Boolean) {
        bluetoothManager.setInEarDetection(enabled)
    }

    fun setEqPreset(preset: EqPreset) {
        bluetoothManager.setEqPreset(preset)
    }

    fun setBassWaveEnabled(enabled: Boolean) {
        bluetoothManager.setBassWaveToggle(enabled)
    }

    fun setBassWaveLevel(level: Int) {
        bluetoothManager.setBassWaveLevel(level)
    }

    fun findMyBuds(start: Boolean) {
        bluetoothManager.findMyBuds(start)
    }

    fun setDualConnection(enabled: Boolean) {
        bluetoothManager.setDualConnection(enabled)
    }

    fun updateGestures(bindings: List<GestureBinding>) {
        bluetoothManager.setGestures(bindings)
    }

    fun setGestureBinding(device: Byte, button: Byte, action: Byte, function: Byte) {
        val current = budsState.value.gestureBindings
        val defaultList = if (current.isNotEmpty()) current else listOf(
            GestureBinding(device = 1, button = 1, action = 1, function = 0),
            GestureBinding(device = 1, button = 1, action = 2, function = 1),
            GestureBinding(device = 1, button = 1, action = 3, function = 2),
            GestureBinding(device = 1, button = 1, action = 4, function = 5),
            GestureBinding(device = 2, button = 1, action = 1, function = 0),
            GestureBinding(device = 2, button = 1, action = 2, function = 1),
            GestureBinding(device = 2, button = 1, action = 3, function = 3),
            GestureBinding(device = 2, button = 1, action = 4, function = 5)
        )
        val updated = defaultList.map {
            if (it.device == device && it.action == action) {
                it.copy(function = function)
            } else it
        }
        updateGestures(updated)
    }

    fun startFitTest(start: Boolean) {
        bluetoothManager.startFitTest(start)
    }

    fun retryConnection() {
        var devices = _pairedDevices.value
        if (devices.isEmpty()) {
            devices = bluetoothManager.getPairedDevices()
            _pairedDevices.value = devices
        }
        val connectedA2dp = bluetoothManager.getConnectedA2dpDevices()
        val targetMac = budsState.value.deviceAddress ?: preferences.value.lastConnectedMac
        val device = findCandidateDevice(devices, targetMac, connectedA2dp) ?: devices.firstOrNull()
        if (device != null) {
            connect(device)
        }
    }

    fun setAutoConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setAutoConnectEnabled(enabled)
        }
    }

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(completed)
        }
    }

    fun setPreferredAncLevel(level: AncLevel) {
        viewModelScope.launch {
            preferencesRepository.savePreferredAncLevel(level)
            if (budsState.value.connectionStatus is ConnectionStatus.Connected &&
                budsState.value.noiseMode == NoiseControlMode.NOISE_CANCELLATION
            ) {
                setAncLevel(level)
            }
        }
    }

    fun setVoiceEnhance(enabled: Boolean) {
        bluetoothManager.setVoiceEnhance(enabled)
    }

    fun setAutoAnswerCalls(enabled: Boolean) {
        bluetoothManager.setAutoAnswerCalls(enabled)
    }

    fun setFindPhoneEnabled(enabled: Boolean) {
        bluetoothManager.setFindPhoneEnabled(enabled)
    }

    fun togglePhoneRinging() {
        bluetoothManager.togglePhoneRinging()
    }

    companion object {
        fun matchCandidateDevice(
            deviceEntries: List<Pair<String, String?>>,
            preferredMac: String?,
            connectedMacs: Set<String> = emptySet()
        ): Pair<String, String?>? {
            if (connectedMacs.isNotEmpty()) {
                if (!preferredMac.isNullOrBlank()) {
                    val connectedPref = deviceEntries.firstOrNull { entry ->
                        connectedMacs.any { cm -> cm.equals(entry.first, ignoreCase = true) } &&
                        entry.first.equals(preferredMac.trim(), ignoreCase = true)
                    }
                    if (connectedPref != null) return connectedPref
                }
                val connectedCandidate = deviceEntries.firstOrNull { entry ->
                    connectedMacs.any { cm -> cm.equals(entry.first, ignoreCase = true) } &&
                    (entry.second?.contains("realme", ignoreCase = true) == true ||
                     entry.second?.contains("Buds", ignoreCase = true) == true ||
                     entry.second?.contains("Air", ignoreCase = true) == true)
                }
                if (connectedCandidate != null) return connectedCandidate
            }

            if (!preferredMac.isNullOrBlank()) {
                val matched = deviceEntries.firstOrNull { it.first.equals(preferredMac.trim(), ignoreCase = true) }
                if (matched != null) return matched
            }
            return deviceEntries.firstOrNull {
                it.second?.contains("realme", ignoreCase = true) == true ||
                it.second?.contains("Buds", ignoreCase = true) == true
            }
        }

        fun shouldTriggerAutoConnect(
            autoConnectEnabled: Boolean,
            userInitiatedDisconnect: Boolean,
            isA2dpConnected: Boolean,
            connectionStatus: ConnectionStatus
        ): Boolean {
            return autoConnectEnabled &&
                   !userInitiatedDisconnect &&
                   isA2dpConnected &&
                   connectionStatus is ConnectionStatus.Disconnected
        }

        fun findCandidateDeviceFromEntries(
            deviceEntries: List<Pair<String, String?>>,
            preferredMac: String?,
            connectedMacs: List<String> = emptyList()
        ): Pair<String, String?>? {
            return matchCandidateDevice(deviceEntries, preferredMac, connectedMacs.toSet())
        }

        fun findCandidateDevice(
            devices: List<BluetoothDevice>,
            preferredMac: String?,
            connectedDevices: List<BluetoothDevice> = emptyList()
        ): BluetoothDevice? {
            if (connectedDevices.isNotEmpty()) {
                if (!preferredMac.isNullOrBlank()) {
                    val connectedPref = connectedDevices.firstOrNull {
                        it.address.equals(preferredMac.trim(), ignoreCase = true)
                    }
                    if (connectedPref != null) return connectedPref
                }
                val connectedCandidate = connectedDevices.firstOrNull {
                    it.name?.contains("realme", ignoreCase = true) == true ||
                    it.name?.contains("Buds", ignoreCase = true) == true ||
                    it.name?.contains("Air", ignoreCase = true) == true
                }
                if (connectedCandidate != null) return connectedCandidate
            }

            if (!preferredMac.isNullOrBlank()) {
                val matched = devices.firstOrNull { it.address.equals(preferredMac.trim(), ignoreCase = true) }
                if (matched != null) return matched
            }
            return devices.firstOrNull {
                it.name?.contains("realme", ignoreCase = true) == true ||
                it.name?.contains("Buds", ignoreCase = true) == true
            }
        }
    }
}

