package com.openairbuds.app.model

import androidx.compose.runtime.Immutable
import com.openairbuds.app.protocol.OpoConstants

enum class NoiseControlMode(val wire: Byte, val displayName: String) {
    NOISE_CANCELLATION(OpoConstants.WIRE_ANC_MAX, "Noise Cancelling"),
    OFF(OpoConstants.WIRE_NOISE_OFF, "Normal"),
    TRANSPARENCY(OpoConstants.WIRE_NOISE_TRANSPARENCY, "Transparency");

    fun next(areBothInEar: Boolean = true): NoiseControlMode {
        return if (!areBothInEar) {
            when (this) {
                TRANSPARENCY -> OFF
                else -> TRANSPARENCY
            }
        } else {
            when (this) {
                NOISE_CANCELLATION -> TRANSPARENCY
                TRANSPARENCY -> OFF
                OFF -> NOISE_CANCELLATION
            }
        }
    }

    companion object {
        fun fromWire(wire: Byte): NoiseControlMode {
            return when (wire) {
                OpoConstants.WIRE_NOISE_OFF -> OFF
                OpoConstants.WIRE_NOISE_TRANSPARENCY -> TRANSPARENCY
                OpoConstants.WIRE_ANC_MILD,
                OpoConstants.WIRE_ANC_MAX,
                OpoConstants.WIRE_ANC_MODERATE,
                OpoConstants.WIRE_ANC_SMART -> NOISE_CANCELLATION
                else -> OFF
            }
        }
    }
}

enum class AncLevel(val wire: Byte, val displayName: String, val shortLabel: String) {
    HIGH_50DB(OpoConstants.WIRE_ANC_MAX, "Deep (50 dB)", "Max"),
    MODERATE(OpoConstants.WIRE_ANC_MODERATE, "Moderate", "Moderate"),
    MILD(OpoConstants.WIRE_ANC_MILD, "Mild", "Mild"),
    SMART(OpoConstants.WIRE_ANC_SMART, "Smart / Dynamic", "Smart");

    companion object {
        fun fromWire(wire: Byte): AncLevel? {
            return when (wire) {
                OpoConstants.WIRE_ANC_MAX -> HIGH_50DB
                OpoConstants.WIRE_ANC_MODERATE -> MODERATE
                OpoConstants.WIRE_ANC_MILD -> MILD
                OpoConstants.WIRE_ANC_SMART -> SMART
                else -> null
            }
        }
    }
}

/**
 * 4 Realme Buds Air 6 Equalizer Presets:
 * 0 = Balanced / Pure
 * 1 = Deep Bass
 * 2 = Clear Vocals
 * 3 = Bass Boost+
 */
enum class EqPreset(val slot: Byte, val displayName: String) {
    BALANCED(0, "Nature Balance"),
    DEEP_BASS(1, "Bass Boost"),
    CLEAR_VOCALS(2, "Clear Vocals"),
    BASS_BOOST_PLUS(3, "Clear Bass"),
    CUSTOM(4, "Custom1");

    companion object {
        fun fromSlot(slot: Byte): EqPreset {
            return entries.firstOrNull { it.slot == slot } ?: BALANCED
        }
    }
}

@Immutable
data class CustomEqProfile(
    val id: Int = 1, // 1, 2, or 3
    val name: String = "Custom1",
    val gains: List<Int> = listOf(0, 0, 0, 0, 0, 0)
)

@Immutable
data class BudsBattery(
    val leftPercent: Int? = null,
    val rightPercent: Int? = null,
    val casePercent: Int? = null,
    val isLeftCharging: Boolean = false,
    val isRightCharging: Boolean = false,
    val isCaseCharging: Boolean = false
) {
    val isConnected: Boolean get() = leftPercent != null || rightPercent != null
}

@Immutable
data class BudsPlacement(
    val isLeftInEar: Boolean = true,
    val isRightInEar: Boolean = true,
    val isLeftInCase: Boolean = false,
    val isRightInCase: Boolean = false,
    val isLeftInUse: Boolean = isLeftInEar,
    val isRightInUse: Boolean = isRightInEar
) {
    val areBothInEar: Boolean get() = isLeftInEar && isRightInEar
}

@Immutable
data class BudsDevice(
    val mac: String,
    val name: String,
    val isConnected: Boolean = false
)

@Immutable
data class GestureBinding(
    val device: Byte,       // 1 = Left, 2 = Right, 4 = Both
    val button: Byte,       // Button category (1 = primary physical button, 6 = on call)
    val action: Byte,       // 1 = Single tap, 2 = Double tap, 3 = Triple tap, 4 = Touch & hold, 5 = Slide, 6 = Long hold
    val function: Byte      // Function code
) {
    val deviceName: String get() = when (device.toInt()) {
        1 -> "Left Bud"
        2 -> "Right Bud"
        else -> "Both Buds"
    }

    val gestureName: String get() {
        val act = if (action.toInt() != 0) action.toInt() else button.toInt()
        return when (act) {
            1 -> "Single Tap"
            2 -> "Double Tap"
            3 -> "Triple Tap"
            4 -> "Touch & Hold"
            5 -> "Slide"
            6 -> "Long Hold"
            else -> "Gesture #$act"
        }
    }

    val actionName: String get() = when (function.toInt()) {
        0x00 -> "None"
        0x01 -> "Play / Pause"
        0x04 -> "Voice Assistant"
        0x05 -> "Previous Track"
        0x06 -> "Next Track"
        0x08 -> "Noise Control (ANC)"
        0x0B -> "Volume Up"
        0x0C -> "Volume Down"
        0x0D -> "Switch Devices"
        0x11 -> "Game Mode"
        else -> "Action #$function"
    }
}

@Immutable
sealed interface ConnectionStatus {
    data object Disconnected : ConnectionStatus
    data object Connecting : ConnectionStatus
    data object Handshaking : ConnectionStatus
    data object Connected : ConnectionStatus
    data class Error(val message: String) : ConnectionStatus
}

enum class FitSealResult(val wire: Int, val label: String) {
    GOOD(1, "GOOD SEAL"),
    AVERAGE(0, "AVERAGE SEAL"),
    POOR(6, "POOR FIT");

    companion object {
        fun fromWire(wire: Int): FitSealResult = when (wire) {
            1 -> GOOD
            0 -> AVERAGE
            else -> POOR
        }
    }
}

@Immutable
sealed interface FitTestState {
    data object Idle : FitTestState
    data object Testing : FitTestState
    data class Completed(val left: FitSealResult?, val right: FitSealResult?) : FitTestState
    data class Error(val message: String) : FitTestState

    companion object {
        fun canStart(placement: BudsPlacement): Boolean = placement.areBothInEar
    }
}

@Immutable
data class BudsState(
    val connectionStatus: ConnectionStatus = ConnectionStatus.Disconnected,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val firmwareVersion: String? = null,
    val productId: String? = null,
    val deviceProfile: DeviceProfile = DeviceProfileRegistry.DEFAULT_REALME,
    val noiseMode: NoiseControlMode = NoiseControlMode.OFF,
    val ancLevel: AncLevel = AncLevel.HIGH_50DB,
    val battery: BudsBattery = BudsBattery(),
    val placement: BudsPlacement = BudsPlacement(),
    val eqPreset: EqPreset = EqPreset.BALANCED,
    val customEqProfiles: List<CustomEqProfile> = listOf(
        CustomEqProfile(1, "Custom1", listOf(0, 0, 0, 0, 0, 0))
    ),
    val selectedCustomEqId: Int = 1,
    val customEqGains: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    val holdCycleMask: Int = 0x0B,
    val isHiResAudioEnabled: Boolean = false,
    val isVolumeEnhancerEnabled: Boolean = false,
    val isWindNoiseReductionEnabled: Boolean = false,
    val isBassWaveEnabled: Boolean = false,
    val bassWaveLevel: Int = 0, // -5 to +5
    val isGameModeEnabled: Boolean = false,
    val isInEarDetectionEnabled: Boolean = true,
    val isDualConnectionEnabled: Boolean = false,
    val connectedDevices: List<BudsDevice> = emptyList(),
    val gestureBindings: List<GestureBinding> = emptyList(),
    val isFindMyBudsRinging: Boolean = false,
    val isVoiceEnhanceEnabled: Boolean = false,
    val isAutoAnswerCallsEnabled: Boolean = false,
    val isFindPhoneEnabled: Boolean = false,
    val isPhoneRingingFromBuds: Boolean = false,
    val fitTestState: FitTestState = FitTestState.Idle,
    val isChannelHeldByOtherApp: Boolean = false,
    val isA2dpConnected: Boolean = false
) {
    val isConnecting: Boolean get() = connectionStatus is ConnectionStatus.Connecting || connectionStatus is ConnectionStatus.Handshaking
}

