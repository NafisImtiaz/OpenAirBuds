package com.openairbuds.app

import com.openairbuds.app.model.BudsPlacement
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.model.DeviceCapabilities
import com.openairbuds.app.model.DeviceProfile
import com.openairbuds.app.model.DeviceProfileRegistry
import com.openairbuds.app.model.EarbudBrand
import com.openairbuds.app.model.NoiseControlMode
import com.openairbuds.app.shortcut.ShortcutActionEngine
import com.openairbuds.app.shortcut.ShortcutResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShortcutActionEngineTest {

    @Test
    fun testModeChangeSucceedsWhenConnected() {
        val connectedState = BudsState(
            connectionStatus = ConnectionStatus.Connected,
            deviceProfile = DeviceProfileRegistry.REALME_BUDS_AIR_6,
            placement = BudsPlacement(isLeftInEar = true, isRightInEar = true)
        )

        val result = ShortcutActionEngine.evaluate(NoiseControlMode.NOISE_CANCELLATION, connectedState)
        assertTrue(result is ShortcutResult.Success)
        assertEquals(NoiseControlMode.NOISE_CANCELLATION, (result as ShortcutResult.Success).mode)
        assertEquals("Noise Cancelling active", result.message)

        val transResult = ShortcutActionEngine.evaluate(NoiseControlMode.TRANSPARENCY, connectedState)
        assertTrue(transResult is ShortcutResult.Success)
        assertEquals(NoiseControlMode.TRANSPARENCY, (transResult as ShortcutResult.Success).mode)

        val offResult = ShortcutActionEngine.evaluate(NoiseControlMode.OFF, connectedState)
        assertTrue(offResult is ShortcutResult.Success)
        assertEquals(NoiseControlMode.OFF, (offResult as ShortcutResult.Success).mode)
    }

    @Test
    fun testBlockedWhenDisconnected() {
        val disconnectedState = BudsState(
            connectionStatus = ConnectionStatus.Disconnected
        )

        val result = ShortcutActionEngine.evaluate(NoiseControlMode.NOISE_CANCELLATION, disconnectedState)
        assertTrue(result is ShortcutResult.NotConnected)
        assertEquals("Earbuds not connected", (result as ShortcutResult.NotConnected).message)
    }

    @Test
    fun testAncBlockedWhenSingleEarbudInEar() {
        val singleBudState = BudsState(
            connectionStatus = ConnectionStatus.Connected,
            deviceProfile = DeviceProfileRegistry.REALME_BUDS_AIR_6,
            placement = BudsPlacement(isLeftInEar = true, isRightInEar = false)
        )

        val result = ShortcutActionEngine.evaluate(NoiseControlMode.NOISE_CANCELLATION, singleBudState)
        assertTrue(result is ShortcutResult.WearRestriction)
        assertEquals("Wear both earbuds to use Noise Cancellation", (result as ShortcutResult.WearRestriction).message)

        // Transparency and Normal must still be permitted with single earbud
        val transResult = ShortcutActionEngine.evaluate(NoiseControlMode.TRANSPARENCY, singleBudState)
        assertTrue(transResult is ShortcutResult.Success)

        val offResult = ShortcutActionEngine.evaluate(NoiseControlMode.OFF, singleBudState)
        assertTrue(offResult is ShortcutResult.Success)
    }

    @Test
    fun testBlockedWhenEarbudDoesNotSupportAnc() {
        val nonAncProfile = DeviceProfile(
            id = "non_anc",
            displayName = "realme Buds T100",
            brand = EarbudBrand.REALME,
            capabilities = DeviceCapabilities(hasAnc = false)
        )
        val nonAncState = BudsState(
            connectionStatus = ConnectionStatus.Connected,
            deviceProfile = nonAncProfile,
            deviceName = "realme Buds T100"
        )

        val result = ShortcutActionEngine.evaluate(NoiseControlMode.NOISE_CANCELLATION, nonAncState)
        assertTrue(result is ShortcutResult.Unsupported)
        assertEquals("Sound modes unsupported on realme Buds T100", (result as ShortcutResult.Unsupported).message)

        val transResult = ShortcutActionEngine.evaluate(NoiseControlMode.TRANSPARENCY, nonAncState)
        assertTrue(transResult is ShortcutResult.Unsupported)
        assertEquals("Sound modes unsupported on realme Buds T100", (transResult as ShortcutResult.Unsupported).message)

        val offResult = ShortcutActionEngine.evaluate(NoiseControlMode.OFF, nonAncState)
        assertTrue(offResult is ShortcutResult.Success)
    }
}
