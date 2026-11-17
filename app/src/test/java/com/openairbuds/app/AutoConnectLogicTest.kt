package com.openairbuds.app

import com.openairbuds.app.model.ConnectionStatus
import com.openairbuds.app.ui.BudsViewModel
import org.junit.Assert.*
import org.junit.Test

class AutoConnectLogicTest {

    @Test
    fun testFindCandidateDevicePreferredConnectedMatch() {
        val mac1 = "11:22:33:44:55:66"
        val mac2 = "AA:BB:CC:DD:EE:FF"
        val entries = listOf(
            mac1 to "realme Buds Air 6",
            mac2 to "Car Audio"
        )
        val connectedMacs = listOf(mac1)

        val candidate = BudsViewModel.findCandidateDeviceFromEntries(
            deviceEntries = entries,
            preferredMac = mac1,
            connectedMacs = connectedMacs
        )

        assertNotNull(candidate)
        assertEquals(mac1, candidate?.first)
    }

    @Test
    fun testFindCandidateDeviceFiltersOutNonBudsAudio() {
        val macCar = "99:88:77:66:55:44"
        val entries = listOf(
            macCar to "Car Bluetooth Audio"
        )
        val connectedMacs = listOf(macCar)

        val candidate = BudsViewModel.findCandidateDeviceFromEntries(
            deviceEntries = entries,
            preferredMac = null,
            connectedMacs = connectedMacs
        )

        assertNull("Should not auto-connect to non-earbud car audio without explicit preference", candidate)
    }

    @Test
    fun testAutoConnectConditionEvaluator() {
        // Condition: autoConnectEnabled=true, userInitiatedDisconnect=false, isA2dpConnected=true, status=Disconnected
        assertTrue(
            BudsViewModel.shouldTriggerAutoConnect(
                autoConnectEnabled = true,
                userInitiatedDisconnect = false,
                isA2dpConnected = true,
                connectionStatus = ConnectionStatus.Disconnected
            )
        )

        // If user manually disconnected, should not auto-connect
        assertFalse(
            BudsViewModel.shouldTriggerAutoConnect(
                autoConnectEnabled = true,
                userInitiatedDisconnect = true,
                isA2dpConnected = true,
                connectionStatus = ConnectionStatus.Disconnected
            )
        )

        // If autoConnect is disabled in preferences, should not auto-connect
        assertFalse(
            BudsViewModel.shouldTriggerAutoConnect(
                autoConnectEnabled = false,
                userInitiatedDisconnect = false,
                isA2dpConnected = true,
                connectionStatus = ConnectionStatus.Disconnected
            )
        )

        // If already connecting or connected, should not trigger auto-connect
        assertFalse(
            BudsViewModel.shouldTriggerAutoConnect(
                autoConnectEnabled = true,
                userInitiatedDisconnect = false,
                isA2dpConnected = true,
                connectionStatus = ConnectionStatus.Connecting
            )
        )
        assertFalse(
            BudsViewModel.shouldTriggerAutoConnect(
                autoConnectEnabled = true,
                userInitiatedDisconnect = false,
                isA2dpConnected = true,
                connectionStatus = ConnectionStatus.Connected
            )
        )
    }
}
