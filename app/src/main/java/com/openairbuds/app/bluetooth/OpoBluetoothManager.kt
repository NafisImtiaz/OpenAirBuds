package com.openairbuds.app.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import android.util.Log
import com.openairbuds.app.data.BudsPreferences
import com.openairbuds.app.data.BudsPreferencesRepository
import com.openairbuds.app.model.AncLevel
import com.openairbuds.app.model.BudsBattery
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.CustomEqProfile
import com.openairbuds.app.model.DeviceProfileRegistry
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.model.FitTestState
import com.openairbuds.app.model.GestureBinding
import com.openairbuds.app.model.NoiseControlMode
import com.openairbuds.app.protocol.OpoConstants
import com.openairbuds.app.protocol.OpoFrame
import com.openairbuds.app.service.AncQuickSettingsTileService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.Collections
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

class OpoBluetoothManager(
    private val context: Context,
    private val preferencesRepository: BudsPreferencesRepository = BudsPreferencesRepository.getInstance(context)
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val sendMutex = Mutex()
    private val sequenceCounter = AtomicInteger(1)

    private var a2dpProxy: BluetoothA2dp? = null
    private var headsetProxy: BluetoothHeadset? = null
    private var currentPreferences = BudsPreferences()
    private var isPhoneRinging = false
    private var ringtonePlayer: Ringtone? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.A2DP -> {
                    a2dpProxy = proxy as? BluetoothA2dp
                    checkAndUpdateA2dpStatus()
                }
                BluetoothProfile.HEADSET -> {
                    headsetProxy = proxy as? BluetoothHeadset
                    checkAndUpdateA2dpStatus()
                }
            }
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.A2DP -> a2dpProxy = null
                BluetoothProfile.HEADSET -> headsetProxy = null
            }
            checkAndUpdateA2dpStatus()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action ?: return
            when (action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    @Suppress("DEPRECATION")
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        Log.d(TAG, "Bluetooth ACL connected: ${device.name} [${device.address}]")
                        checkAndUpdateA2dpStatus(device)
                    }
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    @Suppress("DEPRECATION")
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (device != null) {
                        Log.d(TAG, "Bluetooth ACL disconnected: ${device.name} [${device.address}]")
                        checkAndUpdateA2dpStatus()
                    }
                }
                BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                    checkAndUpdateA2dpStatus()
                }
            }
        }
    }

    init {
        scope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                currentPreferences = prefs
                _budsState.update { current ->
                    val prefAnc = if (current.connectionStatus !is ConnectionStatus.Connected ||
                        current.noiseMode != NoiseControlMode.NOISE_CANCELLATION
                    ) {
                        prefs.preferredAncLevel
                    } else {
                        current.ancLevel
                    }
                    current.copy(
                        ancLevel = prefAnc,
                        holdCycleMask = prefs.holdCycleMask,
                        customEqProfiles = prefs.customEqProfiles,
                        selectedCustomEqId = prefs.selectedCustomEqId,
                        customEqGains = prefs.customEqGains,
                        isHiResAudioEnabled = prefs.isHiResAudioEnabled,
                        isVolumeEnhancerEnabled = prefs.isVolumeEnhancerEnabled,
                        isWindNoiseReductionEnabled = prefs.isWindNoiseReductionEnabled,
                        isBassWaveEnabled = prefs.isBassWaveEnabled,
                        bassWaveLevel = prefs.bassWaveLevel,
                        isVoiceEnhanceEnabled = prefs.isVoiceEnhanceEnabled,
                        isAutoAnswerCallsEnabled = prefs.isAutoAnswerCallsEnabled,
                        isFindPhoneEnabled = prefs.isFindPhoneEnabled
                    )
                }
            }
        }

        initTelephonyListener()

        try {
            val adapter = getBluetoothAdapter()
            adapter?.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
            adapter?.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to bind bluetooth profile proxies: ${e.message}")
        }

        try {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            }
            context.registerReceiver(bluetoothReceiver, filter)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to register bluetooth receiver: ${e.message}")
        }
    }

    private var socket: BluetoothSocket? = null
    private var pendingSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    private var readerJob: Job? = null
    private var connectJob: Job? = null

    private val rxBuffer = Collections.synchronizedList(mutableListOf<Byte>())

    private val _budsState = MutableStateFlow(BudsState())
    val budsState: StateFlow<BudsState> = _budsState.asStateFlow()

    fun isDeviceConnected(device: BluetoothDevice): Boolean {
        if (a2dpProxy?.getConnectedDevices()?.any { it.address.equals(device.address, ignoreCase = true) } == true) return true
        if (headsetProxy?.getConnectedDevices()?.any { it.address.equals(device.address, ignoreCase = true) } == true) return true
        return try {
            val method = device.javaClass.getMethod("isConnected")
            method.invoke(device) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    fun isDeviceConnected(address: String): Boolean {
        val adapter = getBluetoothAdapter() ?: return false
        val device = try { adapter.getRemoteDevice(address) } catch (_: Exception) { null } ?: return false
        return isDeviceConnected(device)
    }

    fun getConnectedA2dpDevices(): List<BluetoothDevice> {
        val result = mutableListOf<BluetoothDevice>()
        try {
            a2dpProxy?.connectedDevices?.let { result.addAll(it) }
            headsetProxy?.connectedDevices?.let { result.addAll(it) }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing permission to query connected profile devices: ${e.message}")
        }
        if (result.isEmpty()) {
            val bonded = getPairedDevices()
            for (dev in bonded) {
                if (isDeviceConnected(dev)) {
                    result.add(dev)
                }
            }
        }
        return result.distinctBy { it.address }
    }

    fun checkAndUpdateA2dpStatus(connectedDevice: BluetoothDevice? = null) {
        val connectedDevices = getConnectedA2dpDevices()
        val currentAddr = _budsState.value.deviceAddress
        val activeDevice = connectedDevice ?: if (currentAddr != null) {
            connectedDevices.firstOrNull { it.address.equals(currentAddr, ignoreCase = true) }
        } else {
            connectedDevices.firstOrNull { dev ->
                dev.name?.contains("realme", ignoreCase = true) == true ||
                dev.name?.contains("Buds", ignoreCase = true) == true ||
                dev.name?.contains("Air", ignoreCase = true) == true
            } ?: connectedDevices.firstOrNull()
        }

        val isCurrentConnected = activeDevice != null || if (currentAddr != null) {
            connectedDevices.any { it.address.equals(currentAddr, ignoreCase = true) }
        } else {
            connectedDevices.isNotEmpty()
        }

        _budsState.update { current ->
            if (activeDevice != null && (current.deviceAddress == null || current.connectionStatus is ConnectionStatus.Disconnected)) {
                val resolvedProfile = DeviceProfileRegistry.resolve(current.productId, activeDevice.name)
                current.copy(
                    isA2dpConnected = isCurrentConnected,
                    deviceAddress = activeDevice.address,
                    deviceName = activeDevice.name ?: resolvedProfile.displayName,
                    deviceProfile = resolvedProfile
                )
            } else {
                current.copy(isA2dpConnected = isCurrentConnected)
            }
        }

        if (isCurrentConnected && activeDevice != null) {
            autoConnectIfEligible(activeDevice)
        }
    }

    private fun autoConnectIfEligible(device: BluetoothDevice) {
        if (!currentPreferences.autoConnectEnabled) return

        val currentStatus = _budsState.value.connectionStatus
        if (currentStatus is ConnectionStatus.Connecting ||
            currentStatus is ConnectionStatus.Handshaking ||
            currentStatus is ConnectionStatus.Connected
        ) {
            return
        }

        val name = device.name ?: ""
        val isCompatible = name.contains("realme", ignoreCase = true) ||
                           name.contains("Buds", ignoreCase = true) ||
                           name.contains("Air", ignoreCase = true) ||
                           name.contains("OnePlus", ignoreCase = true) ||
                           name.contains("OPPO", ignoreCase = true) ||
                           name.contains("Enco", ignoreCase = true) ||
                           device.address.equals(currentPreferences.lastConnectedMac, ignoreCase = true) ||
                           device.address.equals(_budsState.value.deviceAddress, ignoreCase = true)

        if (isCompatible) {
            Log.i(TAG, "Seamlessly auto-connecting to Bluetooth-connected device: $name [${device.address}]")
            connectToDevice(device)
        }
    }

    private fun nextSequence(): Byte {
        val next = sequenceCounter.getAndIncrement() and 0x7F
        return if (next == 0) 1 else next.toByte()
    }

    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        val adapter = getBluetoothAdapter() ?: return emptyList()
        return try {
            adapter.bondedDevices?.toList() ?: emptyList()
        } catch (e: Exception) {
            Log.w(TAG, "Unable to read bondedDevices: ${e.message}")
            emptyList()
        }
    }

    private fun getBluetoothAdapter(): BluetoothAdapter? {
        val bm = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
        return bm?.adapter ?: @Suppress("DEPRECATION") BluetoothAdapter.getDefaultAdapter()
    }

    private var pendingTargetMode: NoiseControlMode? = null

    fun setPendingTargetMode(mode: NoiseControlMode?) {
        pendingTargetMode = mode
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(device: BluetoothDevice, targetModeOnConnect: NoiseControlMode? = null) {
        val currentStatus = _budsState.value.connectionStatus
        val currentAddress = _budsState.value.deviceAddress
        if (currentStatus is ConnectionStatus.Connecting || currentStatus is ConnectionStatus.Handshaking) {
            if (currentAddress == null || currentAddress.equals(device.address, ignoreCase = true)) {
                Log.d(TAG, "Already connecting to ${device.address}, ignoring duplicate connect call")
                return
            }
        }

        val audioConnected = isDeviceConnected(device)
        disconnect(keepA2dp = audioConnected)
        pendingTargetMode = targetModeOnConnect

        val adapter = getBluetoothAdapter()
        if (adapter == null || !adapter.isEnabled) {
            _budsState.update {
                it.copy(
                    connectionStatus = ConnectionStatus.Error("Bluetooth is turned off. Please turn on Bluetooth in settings."),
                    isA2dpConnected = audioConnected
                )
            }
            return
        }

        val initialProfile = DeviceProfileRegistry.resolve(null, device.name)
        _budsState.update {
            it.copy(
                connectionStatus = ConnectionStatus.Connecting,
                deviceName = device.name ?: initialProfile.displayName,
                deviceAddress = device.address,
                deviceProfile = initialProfile,
                isChannelHeldByOtherApp = false,
                isA2dpConnected = audioConnected
            )
        }


        connectJob = scope.launch {
            try {
                Log.d(TAG, "Opening RFCOMM channel to ${device.name} [${device.address}]")
                try {
                    adapter.cancelDiscovery()
                } catch (_: Exception) {}

                var connectedSocket: BluetoothSocket? = null
                var lastConnectError: String? = null
                var isConflictDetected = false
                val uuidsToTry = listOf(
                    OpoConstants.UUID_OPPO_INTERACTION,
                    OpoConstants.UUID_OPPO_SECONDARY,
                    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard Serial Port Profile (SPP)
                )

                for (uuid in uuidsToTry) {
                    var s: BluetoothSocket? = null
                    // Try insecure first: peripheral devices like earbuds reliably accept insecure RFCOMM
                    try {
                        Log.d(TAG, "Attempting insecure socket on $uuid")
                        s = device.createInsecureRfcommSocketToServiceRecord(uuid)
                        pendingSocket = s
                        s.connect()
                        pendingSocket = null
                        connectedSocket = s
                        break
                    } catch (e: Exception) {
                        lastConnectError = e.message
                        if (e.message?.contains("EBUSY", ignoreCase = true) == true ||
                            (e.message?.contains("busy", ignoreCase = true) == true && e.message?.contains("read failed", ignoreCase = true) != true)) {
                            isConflictDetected = true
                        }
                        pendingSocket = null
                        runCatching { s?.close() }
                        Log.w(TAG, "Insecure socket failed on $uuid: ${e.message}")
                        try {
                            Log.d(TAG, "Attempting secure socket on $uuid")
                            s = device.createRfcommSocketToServiceRecord(uuid)
                            pendingSocket = s
                            s.connect()
                            pendingSocket = null
                            connectedSocket = s
                            break
                        } catch (e2: Exception) {
                            lastConnectError = e2.message
                            if (e2.message?.contains("EBUSY", ignoreCase = true) == true ||
                                (e2.message?.contains("busy", ignoreCase = true) == true && e2.message?.contains("read failed", ignoreCase = true) != true)) {
                                isConflictDetected = true
                            }
                            pendingSocket = null
                            runCatching { s?.close() }
                            Log.w(TAG, "Secure socket failed on $uuid: ${e2.message}")
                        }
                    }
                }

                if (connectedSocket == null) {
                    for (port in listOf(15, 1, 2, 4)) {
                        var s: BluetoothSocket? = null
                        try {
                            Log.d(TAG, "Attempting reflection socket on channel $port")
                            val method = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
                            s = method.invoke(device, port) as BluetoothSocket
                            pendingSocket = s
                            s.connect()
                            pendingSocket = null
                            connectedSocket = s
                            break
                        } catch (e: Exception) {
                            lastConnectError = e.message
                            if (e.message?.contains("EBUSY", ignoreCase = true) == true ||
                                (e.message?.contains("busy", ignoreCase = true) == true && e.message?.contains("read failed", ignoreCase = true) != true)) {
                                isConflictDetected = true
                            }
                            pendingSocket = null
                            runCatching { s?.close() }
                            Log.w(TAG, "Reflection socket on channel $port failed: ${e.message}")
                        }
                    }
                }

                if (!isActive) {
                    connectedSocket?.let { runCatching { it.close() } }
                    return@launch
                }

                if (connectedSocket == null) {
                    val isConflict = isConflictDetected ||
                                     lastConnectError?.contains("EBUSY", ignoreCase = true) == true ||
                                     lastConnectError?.contains("resource busy", ignoreCase = true) == true ||
                                     (lastConnectError?.contains("busy", ignoreCase = true) == true &&
                                      lastConnectError.contains("read failed", ignoreCase = true) != true)
                    val errorMsg = if (isConflict) {
                        "Channel busy (realme Link or another service may be holding the connection)."
                    } else if (audioConnected) {
                        "Earbuds connected for audio, but RFCOMM control channel could not be opened."
                    } else {
                        "Could not connect to earbud control channel (RFCOMM)."
                    }
                    _budsState.update {
                        it.copy(
                            connectionStatus = ConnectionStatus.Error(errorMsg),
                            isChannelHeldByOtherApp = isConflict,
                            isA2dpConnected = audioConnected
                        )
                    }
                    return@launch
                }

                socket = connectedSocket
                inputStream = connectedSocket.inputStream
                outputStream = connectedSocket.outputStream

                _budsState.update { it.copy(connectionStatus = ConnectionStatus.Handshaking) }

                // Start inbound packet reader loop
                startReaderLoop()

                // Execute Full Connection Initialization Sequence
                Log.i(TAG, "Executing full connection initialization sequence...")

                // 1. Handshake (0x0100)
                Log.d(TAG, "Init 1/13: Handshake 0x0100")
                sendRaw(OpoFrame.buildHandshake(nextSequence()))
                delay(70)

                // 2. Product ID query (0x0103)
                Log.d(TAG, "Init 2/13: Product ID 0x0103")
                sendRaw(OpoFrame.buildProductIdQuery(nextSequence()))
                delay(70)

                // 3. Broadcast codes query (0x0200)
                Log.d(TAG, "Init 3/13: Broadcast codes 0x0200")
                sendRaw(OpoFrame.buildBroadcastCodesQuery(nextSequence()))
                delay(70)

                // 4. Subscribe notifications (0x0205 04 01 02 03 04)
                Log.d(TAG, "Init 4/13: Subscribe 0x0205")
                sendRaw(OpoFrame.buildSubscribe(nextSequence(), byteArrayOf(0x01, 0x02, 0x03, 0x04)))
                delay(70)

                // 5. Initial status polls:
                // ANC mode (0x010C 01 01)
                Log.d(TAG, "Init 5/13: Query ANC mode 0x010C 01 01")
                sendRaw(OpoFrame.buildAncQuery(nextSequence()))
                delay(50)

                // Hold cycle (0x010C 02 01)
                Log.d(TAG, "Init 6/13: Query Hold Cycle 0x010C 02 01")
                sendRaw(OpoFrame.buildHoldCycleQuery(nextSequence()))
                delay(50)

                // Battery (0x0106 with Seq F0)
                Log.d(TAG, "Init 7/13: Query Battery 0x0106 Seq F0")
                sendRaw(OpoFrame.buildBatteryQuery(0xF0.toByte()))
                delay(50)

                // Wearing status (0x0109 with Seq F2)
                Log.d(TAG, "Init 8/13: Query Wear 0x0109 Seq F2")
                sendRaw(OpoFrame.buildWearQuery(0xF2.toByte()))
                delay(50)

                // Current EQ preset (0x010F)
                Log.d(TAG, "Init 9/13: Query EQ 0x010F")
                sendRaw(OpoFrame.buildEqQuery(nextSequence()))
                delay(50)

                // BassWave level query (0x0124)
                Log.d(TAG, "Init 10/13: Query BassWave Level 0x0124")
                sendRaw(OpoFrame.buildBassWaveLevelQuery(nextSequence()))
                delay(50)

                // Feature switches (0x010D)
                Log.d(TAG, "Init 11/13: Query Feature Switches 0x010D")
                sendRaw(OpoFrame.buildFeaturesQuery(nextSequence()))
                delay(50)

                // Gestures table (0x0108)
                Log.d(TAG, "Init 12/13: Query Gestures 0x0108")
                sendRaw(OpoFrame.buildGesturesQuery(nextSequence()))
                delay(50)

                // Dual connection devices list (0x0112) - only query if supported
                if (_budsState.value.deviceProfile.capabilities.hasDualConnection) {
                    Log.d(TAG, "Init 13/13: Query Dual Devices 0x0112")
                    sendRaw(OpoFrame.buildDualDevicesQuery(nextSequence()))
                    delay(50)
                }
                Log.d(TAG, "Init: Query Firmware 0x0105")
                sendRaw(OpoFrame.buildFirmwareQuery(nextSequence()))
                delay(50)

                if (isActive) {
                    _budsState.update { it.copy(connectionStatus = ConnectionStatus.Connected) }
                    Log.i(TAG, "OPOv1 channel connected and ready!")
                    val pendingMode = pendingTargetMode
                    pendingTargetMode = null
                    if (pendingMode != null) {
                        setNoiseControlMode(pendingMode)
                    }

                    // Synchronize saved preferences to hardware
                    if (currentPreferences.holdCycleMask != 0x0B) {
                        sendRaw(OpoFrame.buildSetHoldCycle(currentPreferences.holdCycleMask.toByte(), nextSequence()))
                    }
                    if (currentPreferences.isHiResAudioEnabled) {
                        sendRaw(OpoFrame.buildSetHiResAudio(true, nextSequence()))
                    }
                    if (currentPreferences.isVolumeEnhancerEnabled) {
                        sendRaw(OpoFrame.buildSetVolumeEnhancer(true, nextSequence()))
                    }
                    if (currentPreferences.isWindNoiseReductionEnabled) {
                        sendRaw(OpoFrame.buildSetWindNoiseReduction(true, nextSequence()))
                    }
                    if (currentPreferences.isVoiceEnhanceEnabled && _budsState.value.noiseMode == NoiseControlMode.TRANSPARENCY) {
                        sendRaw(OpoFrame.buildSetVoiceEnhance(true, nextSequence()))
                    }
                    val activeCustomProfile = currentPreferences.customEqProfiles.find { it.id == currentPreferences.selectedCustomEqId }
                        ?: currentPreferences.customEqProfiles.firstOrNull()
                    val activeGains = activeCustomProfile?.gains ?: currentPreferences.customEqGains
                    val activeName = activeCustomProfile?.name ?: "Custom1"
                    if (activeGains.any { it != 0 } || _budsState.value.eqPreset == EqPreset.CUSTOM) {
                        sendRaw(OpoFrame.buildSetCustomEq(activeGains, nextSequence(), action = 0x02, name = activeName))
                        if (_budsState.value.eqPreset == EqPreset.CUSTOM) {
                            sendRaw(OpoFrame.buildSetEqPreset(EqPreset.CUSTOM, nextSequence()))
                        }
                    }
                    if (currentPreferences.isBassWaveEnabled) {
                        sendRaw(OpoFrame.buildSetBassWaveToggle(true, nextSequence()))
                        sendRaw(OpoFrame.buildSetBassWaveLevel(currentPreferences.bassWaveLevel, nextSequence()))
                    }
                    _budsState.update {
                        it.copy(
                            holdCycleMask = currentPreferences.holdCycleMask,
                            isHiResAudioEnabled = currentPreferences.isHiResAudioEnabled,
                            isVolumeEnhancerEnabled = currentPreferences.isVolumeEnhancerEnabled,
                            isWindNoiseReductionEnabled = currentPreferences.isWindNoiseReductionEnabled,
                            customEqProfiles = currentPreferences.customEqProfiles,
                            selectedCustomEqId = currentPreferences.selectedCustomEqId,
                            customEqGains = activeGains,
                            isBassWaveEnabled = currentPreferences.isBassWaveEnabled,
                            bassWaveLevel = currentPreferences.bassWaveLevel
                        )
                    }

                    scope.launch {
                        preferencesRepository.saveLastConnectedDevice(
                            mac = device.address,
                            name = device.name ?: "realme Buds Air 6"
                        )
                    }
                }

            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(TAG, "Connection attempt terminated", e)
                val isBusy = e.message?.contains("EBUSY", ignoreCase = true) == true ||
                             (e.message?.contains("busy", ignoreCase = true) == true &&
                              e.message?.contains("read failed", ignoreCase = true) != true)
                val audioStillConnected = isDeviceConnected(device)
                _budsState.update {
                    it.copy(
                        connectionStatus = ConnectionStatus.Error(
                            if (isBusy) "Channel busy (realme Link is holding the connection). Force-close realme Link or tap Retry."
                            else if (audioStillConnected) "Earbuds connected for audio, but controls could not sync. Tap Retry."
                            else e.localizedMessage ?: "Connection failed"
                        ),
                        isChannelHeldByOtherApp = isBusy,
                        isA2dpConnected = audioStillConnected
                    )
                }
                teardown()
            }
        }
    }

    fun setNoiseControlMode(mode: NoiseControlMode, level: AncLevel = _budsState.value.ancLevel) {
        scope.launch {
            try {
                Log.d(TAG, "Setting noise mode: $mode ($level)")
                val packet = OpoFrame.buildSetNoiseMode(mode, level, nextSequence())
                sendRaw(packet)
                _budsState.update {
                    it.copy(
                        noiseMode = mode,
                        ancLevel = if (mode == NoiseControlMode.NOISE_CANCELLATION) level else it.ancLevel
                    )
                }
                AncQuickSettingsTileService.requestTileUpdate(context)
                if (mode == NoiseControlMode.NOISE_CANCELLATION) {
                    preferencesRepository.savePreferredAncLevel(level)
                } else if (mode == NoiseControlMode.TRANSPARENCY && _budsState.value.isVoiceEnhanceEnabled) {
                    val voicePacket = OpoFrame.buildSetVoiceEnhance(true, nextSequence())
                    sendRaw(voicePacket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send noise mode packet", e)
            }
        }
    }

    private var mediaPlayer: android.media.MediaPlayer? = null

    fun playAcousticTone(toneResId: Int, loop: Boolean = false) {
        stopAcousticTone()
        try {
            mediaPlayer = android.media.MediaPlayer.create(context, toneResId)?.apply {
                isLooping = loop
                start()
                setOnCompletionListener {
                    it.release()
                    if (mediaPlayer == it) {
                        mediaPlayer = null
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play acoustic tone", e)
        }
    }

    fun stopAcousticTone() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
        } catch (_: Exception) {}
        mediaPlayer = null
    }

    fun setHoldCycle(mask: Byte) {
        val maskInt = mask.toInt() and 0xFF
        _budsState.update { it.copy(holdCycleMask = maskInt) }
        scope.launch {
            try {
                preferencesRepository.saveHoldCycleMask(maskInt)
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Setting hold cycle mask: 0x${mask.toString(16)}")
                    val packet = OpoFrame.buildSetHoldCycle(mask, nextSequence())
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set hold cycle mask", e)
            }
        }
    }

    fun setCustomEq(gains: List<Int>) {
        val clamped = gains.take(6).map { it.coerceIn(-6, 6) }
        val finalGains = clamped + List(maxOf(0, 6 - clamped.size)) { 0 }
        val currentProfiles = _budsState.value.customEqProfiles.toMutableList()
        val currentSelectedId = _budsState.value.selectedCustomEqId
        val existingIndex = currentProfiles.indexOfFirst { it.id == currentSelectedId }
        val activeProfile = if (existingIndex >= 0) {
            val updated = currentProfiles[existingIndex].copy(gains = finalGains)
            currentProfiles[existingIndex] = updated
            updated
        } else {
            val newProfile = CustomEqProfile(currentSelectedId, "Custom$currentSelectedId", finalGains)
            currentProfiles.add(newProfile)
            newProfile
        }

        _budsState.update {
            it.copy(
                customEqProfiles = currentProfiles,
                customEqGains = finalGains,
                eqPreset = EqPreset.CUSTOM
            )
        }
        scope.launch {
            try {
                preferencesRepository.saveCustomEqProfiles(currentProfiles, currentSelectedId)
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Setting custom EQ: $finalGains for ${activeProfile.name}")
                    val packet = OpoFrame.buildSetCustomEq(
                        finalGains,
                        nextSequence(),
                        action = 0x02,
                        name = activeProfile.name
                    )
                    sendRaw(packet)
                    val presetPacket = OpoFrame.buildSetEqPreset(EqPreset.CUSTOM, nextSequence())
                    sendRaw(presetPacket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send custom EQ packet", e)
            }
        }
    }

    fun selectCustomEqProfile(profileId: Int) {
        val currentProfiles = _budsState.value.customEqProfiles
        val targetProfile = currentProfiles.find { it.id == profileId } ?: return
        _budsState.update {
            it.copy(
                selectedCustomEqId = profileId,
                customEqGains = targetProfile.gains,
                eqPreset = EqPreset.CUSTOM
            )
        }
        scope.launch {
            try {
                preferencesRepository.saveCustomEqProfiles(currentProfiles, profileId)
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Activating custom EQ profile ${targetProfile.name}: ${targetProfile.gains}")
                    val packet = OpoFrame.buildSetCustomEq(
                        targetProfile.gains,
                        nextSequence(),
                        action = 0x02,
                        name = targetProfile.name
                    )
                    sendRaw(packet)
                    val presetPacket = OpoFrame.buildSetEqPreset(EqPreset.CUSTOM, nextSequence())
                    sendRaw(presetPacket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to switch custom EQ profile", e)
            }
        }
    }

    fun addCustomEqProfile(): Boolean {
        val currentProfiles = _budsState.value.customEqProfiles.toMutableList()
        if (currentProfiles.size >= 3) {
            return false
        }
        val existingIds = currentProfiles.map { it.id }.toSet()
        val nextId = (1..3).firstOrNull { it !in existingIds } ?: return false
        val newProfile = CustomEqProfile(id = nextId, name = "Custom$nextId", gains = listOf(0, 0, 0, 0, 0, 0))
        currentProfiles.add(newProfile)

        _budsState.update {
            it.copy(
                customEqProfiles = currentProfiles,
                selectedCustomEqId = nextId,
                customEqGains = newProfile.gains,
                eqPreset = EqPreset.CUSTOM
            )
        }
        scope.launch {
            try {
                preferencesRepository.saveCustomEqProfiles(currentProfiles, nextId)
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Added and activated custom EQ profile ${newProfile.name}")
                    val packet = OpoFrame.buildSetCustomEq(
                        newProfile.gains,
                        nextSequence(),
                        action = 0x02,
                        name = newProfile.name
                    )
                    sendRaw(packet)
                    val presetPacket = OpoFrame.buildSetEqPreset(EqPreset.CUSTOM, nextSequence())
                    sendRaw(presetPacket)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to add custom EQ profile", e)
            }
        }
        return true
    }

    fun renameCustomEqProfile(profileId: Int, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        val currentProfiles = _budsState.value.customEqProfiles.map { profile ->
            if (profile.id == profileId) profile.copy(name = trimmed) else profile
        }
        _budsState.update { it.copy(customEqProfiles = currentProfiles) }
        val activeSelectedId = _budsState.value.selectedCustomEqId
        scope.launch {
            try {
                preferencesRepository.saveCustomEqProfiles(currentProfiles, activeSelectedId)
                val target = currentProfiles.find { it.id == profileId }
                if (target != null && socket?.isConnected == true) {
                    val packet = OpoFrame.buildSetCustomEq(
                        target.gains,
                        nextSequence(),
                        action = 0x02,
                        name = target.name
                    )
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to rename custom EQ profile", e)
            }
        }
    }

    fun setWindNoiseReduction(enabled: Boolean) {
        _budsState.update { it.copy(isWindNoiseReductionEnabled = enabled) }
        scope.launch {
            try {
                preferencesRepository.saveWindNoiseReduction(enabled)
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Setting wind noise reduction: $enabled")
                    val packet = OpoFrame.buildSetWindNoiseReduction(enabled, nextSequence())
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send wind noise reduction packet", e)
            }
        }
    }

    fun setHiResAudio(enabled: Boolean) {
        _budsState.update { it.copy(isHiResAudioEnabled = enabled) }
        scope.launch {
            try {
                preferencesRepository.saveHiResAudio(enabled)
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Setting Hi-Res Audio: $enabled")
                    val packet = OpoFrame.buildSetHiResAudio(enabled, nextSequence())
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send Hi-Res Audio packet", e)
            }
        }
    }

    fun setGameMode(enabled: Boolean) {
        scope.launch {
            try {
                Log.d(TAG, "Setting game mode: $enabled")
                val packet = OpoFrame.buildSetGameMode(enabled, nextSequence())
                sendRaw(packet)
                _budsState.update { it.copy(isGameModeEnabled = enabled) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send game mode packet", e)
            }
        }
    }

    fun setInEarDetection(enabled: Boolean) {
        scope.launch {
            try {
                Log.d(TAG, "Setting in-ear detection: $enabled")
                val packet = OpoFrame.buildSetInEarDetection(enabled, nextSequence())
                sendRaw(packet)
                _budsState.update { it.copy(isInEarDetectionEnabled = enabled) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send in-ear detection packet", e)
            }
        }
    }

    fun setEqPreset(preset: EqPreset) {
        if (preset == EqPreset.CUSTOM) {
            selectCustomEqProfile(_budsState.value.selectedCustomEqId)
            return
        }
        scope.launch {
            try {
                Log.d(TAG, "Setting EQ preset: $preset")
                val packet = OpoFrame.buildSetEqPreset(preset, nextSequence())
                sendRaw(packet)
                _budsState.update { it.copy(eqPreset = preset) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send EQ preset packet", e)
            }
        }
    }

    fun setBassWaveEnabled(enabled: Boolean) {
        _budsState.update { it.copy(isBassWaveEnabled = enabled) }
        scope.launch {
            try {
                preferencesRepository.saveBassWave(enabled, _budsState.value.bassWaveLevel)
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Setting BassWave toggle: $enabled")
                    val packet = OpoFrame.buildSetBassWaveToggle(enabled, nextSequence())
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send BassWave toggle packet", e)
            }
        }
    }

    fun setBassWaveToggle(enabled: Boolean) {
        setBassWaveEnabled(enabled)
    }

    fun setBassWaveLevel(level: Int) {
        val clamped = level.coerceIn(-5, 5)
        _budsState.update { it.copy(bassWaveLevel = clamped) }
        scope.launch {
            try {
                preferencesRepository.saveBassWave(_budsState.value.isBassWaveEnabled, clamped)
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Setting BassWave level: $clamped")
                    val packet = OpoFrame.buildSetBassWaveLevel(clamped, nextSequence())
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send BassWave level packet", e)
            }
        }
    }

    fun findMyBuds(start: Boolean) {
        _budsState.update { it.copy(isFindMyBudsRinging = start) }
        if (start) {
            playAcousticTone(com.openairbuds.app.R.raw.find_buds_chirp, loop = true)
        } else {
            stopAcousticTone()
        }
        scope.launch {
            try {
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Find My Buds: start=$start")
                    val packet = OpoFrame.buildFindMyBuds(start, nextSequence())
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send Find My Buds packet", e)
            }
        }
    }

    fun setDualConnection(enabled: Boolean) {
        scope.launch {
            try {
                Log.d(TAG, "Setting Dual Connection: $enabled")
                val packet = OpoFrame.buildSetDualConnection(enabled, nextSequence())
                sendRaw(packet)
                _budsState.update { it.copy(isDualConnectionEnabled = enabled) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send Dual Connection packet", e)
            }
        }
    }

    fun setGestures(bindings: List<GestureBinding>) {
        _budsState.update { it.copy(gestureBindings = bindings) }
        scope.launch {
            try {
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Writing Gestures table (${bindings.size} bindings)")
                    val packet = OpoFrame.buildSetGestures(bindings, nextSequence())
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send Gestures packet", e)
            }
        }
    }

    fun startFitTest(start: Boolean) {
        if (start) {
            _budsState.update { it.copy(fitTestState = FitTestState.Testing) }
        } else {
            _budsState.update { it.copy(fitTestState = FitTestState.Idle) }
        }
        scope.launch {
            try {
                if (socket?.isConnected == true) {
                    Log.d(TAG, "Fit Test: start=$start")
                    val packet = OpoFrame.buildFitTest(start, nextSequence())
                    sendRaw(packet)
                } else if (start) {
                    _budsState.update { it.copy(fitTestState = FitTestState.Error("Earbuds not connected")) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send Fit Test packet", e)
                if (start) {
                    _budsState.update { it.copy(fitTestState = FitTestState.Error("Failed to start test")) }
                }
            }
        }
    }

    fun setVoiceEnhance(enabled: Boolean) {
        _budsState.update { it.copy(isVoiceEnhanceEnabled = enabled) }
        scope.launch {
            try {
                preferencesRepository.saveVoiceEnhance(enabled)
                if (socket?.isConnected == true && _budsState.value.noiseMode == NoiseControlMode.TRANSPARENCY) {
                    Log.d(TAG, "Setting voice enhance: $enabled")
                    val packet = OpoFrame.buildSetVoiceEnhance(enabled, nextSequence())
                    sendRaw(packet)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send voice enhance packet", e)
            }
        }
    }

    fun setAutoAnswerCalls(enabled: Boolean) {
        _budsState.update { it.copy(isAutoAnswerCallsEnabled = enabled) }
        scope.launch {
            try {
                preferencesRepository.saveAutoAnswerCalls(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save auto-answer calls preference", e)
            }
        }
    }

    fun setFindPhoneEnabled(enabled: Boolean) {
        _budsState.update { it.copy(isFindPhoneEnabled = enabled) }
        scope.launch {
            try {
                preferencesRepository.saveFindPhone(enabled)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save find phone preference", e)
            }
        }
    }

    fun togglePhoneRinging() {
        val shouldRing = !_budsState.value.isPhoneRingingFromBuds
        ringPhone(shouldRing)
    }

    fun ringPhone(start: Boolean) {
        _budsState.update { it.copy(isPhoneRingingFromBuds = start) }
        try {
            if (start) {
                if (ringtonePlayer == null) {
                    val alertUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ringtonePlayer = alertUri?.let { RingtoneManager.getRingtone(context, it) }
                    ringtonePlayer?.audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                }
                ringtonePlayer?.play()
            } else {
                ringtonePlayer?.stop()
                ringtonePlayer = null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing phone ringtone", e)
        }
    }

    @SuppressLint("MissingPermission")
    private fun initTelephonyListener() {
        try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                telephonyManager.registerTelephonyCallback(
                    context.mainExecutor,
                    object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                        override fun onCallStateChanged(state: Int) {
                            handleCallStateChanged(state)
                        }
                    }
                )
            } else {
                @Suppress("DEPRECATION")
                telephonyManager.listen(
                    object : PhoneStateListener() {
                        @Deprecated("Deprecated in Java")
                        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                            handleCallStateChanged(state)
                        }
                    },
                    PhoneStateListener.LISTEN_CALL_STATE
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not register telephony listener: ${e.message}")
        }
    }

    private fun handleCallStateChanged(state: Int) {
        isPhoneRinging = (state == TelephonyManager.CALL_STATE_RINGING)
        Log.d(TAG, "Phone call state changed: $state (isPhoneRinging=$isPhoneRinging)")
    }

    @SuppressLint("MissingPermission")
    private fun checkAndTriggerAutoAnswer() {
        if (!isPhoneRinging || !_budsState.value.isAutoAnswerCallsEnabled) return
        try {
            Log.i(TAG, "Auto-answering incoming call via TelecomManager...")
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            telecomManager?.acceptRingingCall()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-answer ringing call", e)
        }
    }

    private suspend fun sendRaw(bytes: ByteArray) {
        sendMutex.withLock {
            withContext(Dispatchers.IO) {
                val out = outputStream ?: throw IllegalStateException("RFCOMM channel not open")
                out.write(bytes)
                out.flush()
                Log.d(TAG, "TX: ${bytes.joinToString(" ") { String.format("%02x", it) }}")
            }
        }
    }

    private fun startReaderLoop() {
        readerJob?.cancel()
        synchronized(rxBuffer) {
            rxBuffer.clear()
        }

        readerJob = scope.launch(Dispatchers.IO) {
            val stream = inputStream ?: return@launch
            val chunk = ByteArray(512)

            while (isActive) {
                try {
                    val bytesRead = stream.read(chunk)
                    if (bytesRead > 0) {
                        val frames = synchronized(rxBuffer) {
                            for (i in 0 until bytesRead) {
                                rxBuffer.add(chunk[i])
                            }
                            OpoFrame.drainFrames(rxBuffer)
                        }
                        for (frame in frames) {
                            // 1. Automatically respond to incoming 0x0500 clock sync requests
                            if (frame.cmdId == OpoConstants.CMD_CLOCK_SYNC_REQ) {
                                Log.i(TAG, "Responding to clock sync request from earbuds (Seq=${frame.sequence})")
                                scope.launch {
                                    try {
                                        val syncReply = OpoFrame.buildClockSyncReply(frame.sequence)
                                        sendRaw(syncReply)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to reply to clock sync", e)
                                    }
                                }
                            }

                            // 2. Dispatch event to update state
                            _budsState.update { current ->
                                val next = OpoFrame.interpret(frame, current)
                                if (next.noiseMode != current.noiseMode) {
                                    AncQuickSettingsTileService.requestTileUpdate(context)
                                }
                                if (next.isAutoAnswerCallsEnabled) {
                                    val wasInEar = current.placement.isLeftInEar || current.placement.isRightInEar
                                    val nowInEar = next.placement.isLeftInEar || next.placement.isRightInEar
                                    if (!wasInEar && nowInEar) {
                                        checkAndTriggerAutoAnswer()
                                    }
                                }
                                next
                            }
                        }
                    } else if (bytesRead == -1) {
                        Log.w(TAG, "End of RFCOMM stream")
                        break
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) break
                    Log.e(TAG, "RFCOMM socket read error: ${e.message}")
                    break
                }
            }

            if (isActive) {
                val currentDev = _budsState.value.deviceAddress
                val a2dpActive = currentDev?.let { isDeviceConnected(it) } ?: false
                disconnect(keepA2dp = a2dpActive)
            }
        }
    }

    /**
     * Closes the RFCOMM socket so realme Link can access the channel when needed.
     */
    fun releaseChannel() {
        disconnect(keepA2dp = _budsState.value.deviceAddress?.let { isDeviceConnected(it) } ?: false)
    }

    private fun teardown() {
        connectJob?.cancel()
        connectJob = null
        readerJob?.cancel()
        readerJob = null
        runCatching { pendingSocket?.close() }
        pendingSocket = null
        runCatching { inputStream?.close() }
        runCatching { outputStream?.close() }
        runCatching { socket?.close() }
        inputStream = null
        outputStream = null
        socket = null
        stopAcousticTone()
        synchronized(rxBuffer) {
            rxBuffer.clear()
        }
    }

    fun disconnect(keepA2dp: Boolean = false) {
        teardown()

        val isAudio = keepA2dp || (_budsState.value.deviceAddress?.let { isDeviceConnected(it) } ?: false)
        _budsState.update {
            it.copy(
                connectionStatus = ConnectionStatus.Disconnected,
                battery = BudsBattery(),
                firmwareVersion = null,
                productId = null,
                isFindMyBudsRinging = false,
                fitTestState = FitTestState.Idle,
                isA2dpConnected = isAudio
            )
        }
    }

    companion object {
        private const val TAG = "OpoBluetoothManager"

        @Volatile
        private var instance: OpoBluetoothManager? = null

        fun getInstance(context: Context): OpoBluetoothManager {
            return instance ?: synchronized(this) {
                instance ?: OpoBluetoothManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
