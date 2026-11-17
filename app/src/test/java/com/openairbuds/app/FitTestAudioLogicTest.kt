package com.openairbuds.app

import com.openairbuds.app.model.BudsState
import com.openairbuds.app.protocol.OpoConstants
import com.openairbuds.app.protocol.OpoFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FitTestAudioLogicTest {

    @Test
    fun testFitTestPacketCommandsEarbudsHardwareTone() {
        val startPacket = OpoFrame.buildFitTest(start = true, sequence = 10)
        assertEquals("Sync byte must match", OpoConstants.SYNC_BYTE, startPacket[0])
        assertEquals("CmdId low byte for fit test must be 0x05", 0x05.toByte(), startPacket[4])
        assertEquals("CmdId high byte for fit test must be 0x04", 0x04.toByte(), startPacket[5])
        assertEquals("Sequence must be 10", 10.toByte(), startPacket[6])
        assertEquals("Payload byte must be 0x01 (start hardware tone)", 0x01.toByte(), startPacket[9])

        val stopPacket = OpoFrame.buildFitTest(start = false, sequence = 11)
        assertEquals("Payload byte must be 0x00 (stop hardware tone)", 0x00.toByte(), stopPacket[9])
    }

    @Test
    fun testFitTestResponseInterpretsHardwareMeasurementWithoutPhoneAudio() {
        // Inbound fit test response from earbuds firmware: 0x0204 subtype 0x04
        val raw = byteArrayOf(
            0xAA.toByte(), 0x0C, 0x00, 0x00,
            0x04, 0x02, 0xFF.toByte(), 0x05, 0x00,
            0x04, 0x01, 0x01, 0x02, 0x01
        )
        val frames = OpoFrame.drainFrames(raw.toMutableList())
        assertEquals(1, frames.size)

        val state = OpoFrame.interpret(frames[0], BudsState(fitTestResult = "Testing seal..."))
        assertNotNull(state.fitTestResult)
        assertTrue(state.fitTestResult!!.contains("Left: Good seal"))
        assertTrue(state.fitTestResult!!.contains("Right: Good seal"))
    }
}
