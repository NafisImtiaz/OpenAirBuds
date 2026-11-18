package com.openairbuds.app.shortcut

import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.NoiseControlMode

sealed class ShortcutResult {
    data class Success(val mode: NoiseControlMode, val message: String) : ShortcutResult()
    data class NotConnected(val message: String) : ShortcutResult()
    data class WearRestriction(val message: String) : ShortcutResult()
    data class Unsupported(val message: String) : ShortcutResult()
}

object ShortcutActionEngine {

    fun evaluate(targetMode: NoiseControlMode, state: BudsState): ShortcutResult {
        // 1. Connection check
        if (state.connectionStatus !is ConnectionStatus.Connected) {
            return ShortcutResult.NotConnected("Earbuds not connected")
        }

        // 2. Hardware capability check (e.g. models without ANC)
        val deviceName = state.deviceName?.takeIf { it.isNotBlank() } ?: state.deviceProfile.displayName
        if (!state.deviceProfile.capabilities.hasAnc && targetMode != NoiseControlMode.OFF) {
            return ShortcutResult.Unsupported("Sound modes unsupported on $deviceName")
        }

        // 3. In-ear detection check (ANC requires both earbuds to be worn)
        if (targetMode == NoiseControlMode.NOISE_CANCELLATION) {
            val wearSupported = state.deviceProfile.capabilities.hasInEarDetection
            if (wearSupported && !state.placement.areBothInEar) {
                return ShortcutResult.WearRestriction("Wear both earbuds to use Noise Cancellation")
            }
        }

        val message = when (targetMode) {
            NoiseControlMode.NOISE_CANCELLATION -> "Noise Cancelling active"
            NoiseControlMode.TRANSPARENCY -> "Transparency mode active"
            NoiseControlMode.OFF -> "Normal mode active"
        }

        return ShortcutResult.Success(targetMode, message)
    }
}
