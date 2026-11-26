package com.openairbuds.app

import com.openairbuds.app.model.BudsState
import com.openairbuds.app.protocol.OpoConstants
import com.openairbuds.app.protocol.OpoFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HoldCycleTest {

    @Test
    fun testBuildSetHoldCyclePacketFraming() {
        val mask = (OpoConstants.HOLD_CYCLE_ANC.toInt() or OpoConstants.HOLD_CYCLE_TRANSPARENCY.toInt() or OpoConstants.HOLD_CYCLE_OFF.toInt()).toByte()
        assertEquals(0x0B.toByte(), mask)

        val packet = OpoFrame.buildSetHoldCycle(mask = mask, sequence = 5)

        assertEquals(13, packet.size)
        assertEquals(OpoConstants.SYNC_BYTE, packet[0])
        assertEquals(11.toByte(), packet[1]) // total length = 7 + 4 = 11
        assertEquals(0.toByte(), packet[2])
        assertEquals(0.toByte(), packet[3])
        assertEquals(0x04.toByte(), packet[4]) // CMD_SET_ANC (0x0404) low byte
        assertEquals(0x04.toByte(), packet[5]) // CMD_SET_ANC high byte
        assertEquals(5.toByte(), packet[6])    // Sequence
        assertEquals(4.toByte(), packet[7])    // Payload len low byte (4)
        assertEquals(0.toByte(), packet[8])    // Payload len high byte
        assertEquals(OpoConstants.CMD_SUB_HOLD_CYCLE, packet[9]) // sub-id (0x02)
        assertEquals(0x01.toByte(), packet[10])
        assertEquals(0x0B.toByte(), packet[11]) // mask low byte
        assertEquals(0x00.toByte(), packet[12]) // mask high byte
    }

    @Test
    fun testHoldCycleBitmaskCalculations() {
        val anc = OpoConstants.HOLD_CYCLE_ANC.toInt()               // 0x08
        val transparency = OpoConstants.HOLD_CYCLE_TRANSPARENCY.toInt() // 0x02
        val normal = OpoConstants.HOLD_CYCLE_OFF.toInt()            // 0x01

        // All 3 modes enabled: 0x08 | 0x02 | 0x01 = 0x0B (11)
        val allThree = anc or transparency or normal
        assertEquals(0x0B, allThree)
        assertTrue((allThree and anc) != 0)
        assertTrue((allThree and transparency) != 0)
        assertTrue((allThree and normal) != 0)

        // ANC + Transparency only: 0x08 | 0x02 = 0x0A (10)
        val ancAndTrans = anc or transparency
        assertEquals(0x0A, ancAndTrans)
        assertTrue((ancAndTrans and anc) != 0)
        assertTrue((ancAndTrans and transparency) != 0)
        assertFalse((ancAndTrans and normal) != 0)

        // ANC + Normal only: 0x08 | 0x01 = 0x09 (9)
        val ancAndNormal = anc or normal
        assertEquals(0x09, ancAndNormal)
        assertTrue((ancAndNormal and anc) != 0)
        assertFalse((ancAndNormal and transparency) != 0)
        assertTrue((ancAndNormal and normal) != 0)

        // Transparency + Normal only: 0x02 | 0x01 = 0x03 (3)
        val transAndNormal = transparency or normal
        assertEquals(0x03, transAndNormal)
        assertFalse((transAndNormal and anc) != 0)
        assertTrue((transAndNormal and transparency) != 0)
        assertTrue((transAndNormal and normal) != 0)
    }

    @Test
    fun testMinimumTwoModesValidation() {
        fun validateAndComputeMask(anc: Boolean, transparency: Boolean, normal: Boolean): Int? {
            var count = 0
            var mask = 0
            if (anc) {
                mask = mask or OpoConstants.HOLD_CYCLE_ANC.toInt()
                count++
            }
            if (transparency) {
                mask = mask or OpoConstants.HOLD_CYCLE_TRANSPARENCY.toInt()
                count++
            }
            if (normal) {
                mask = mask or OpoConstants.HOLD_CYCLE_OFF.toInt()
                count++
            }
            return if (count >= 2) mask else null
        }

        // Invalid cases (< 2 modes)
        assertEquals(null, validateAndComputeMask(anc = false, transparency = false, normal = false))
        assertEquals(null, validateAndComputeMask(anc = true, transparency = false, normal = false))
        assertEquals(null, validateAndComputeMask(anc = false, transparency = true, normal = false))
        assertEquals(null, validateAndComputeMask(anc = false, transparency = false, normal = true))

        // Valid cases (>= 2 modes)
        assertEquals(0x0A, validateAndComputeMask(anc = true, transparency = true, normal = false))
        assertEquals(0x09, validateAndComputeMask(anc = true, transparency = false, normal = true))
        assertEquals(0x03, validateAndComputeMask(anc = false, transparency = true, normal = true))
        assertEquals(0x0B, validateAndComputeMask(anc = true, transparency = true, normal = true))
    }

    @Test
    fun testHoldCycleActiveReportParsing() {
        // Active report packet from earbud notifying hold cycle mask changed to 0x0A (ANC + Transparency):
        // CMD_ACTIVE_REPORT (0x0204), EVT_ANC (0x03)
        // Sub-id 0x02 (Hold cycle), sub-len 0x01, mask 0x0A
        val payload = byteArrayOf(
            0x03, // EVT_ANC
            0x02, // CMD_SUB_HOLD_CYCLE
            0x01, // subLen
            0x0A  // mask
        )
        val frame = OpoFrame(
            cmdId = OpoConstants.CMD_ACTIVE_REPORT,
            sequence = 12,
            payload = payload
        )

        val state = BudsState(holdCycleMask = 0x0B)
        val updated = OpoFrame.interpret(frame, state)
        assertEquals(0x0A, updated.holdCycleMask)
    }

    @Test
    fun testWindNoiseReductionFeatureParsing() {
        // Feature push packet for Wind Noise Reduction:
        // CMD_FEATURE_PUSH (0x0503), feature 0x1A (FEATURE_WIND_NOISE_REDUCTION), status 0x01 (Enabled)
        val payload = byteArrayOf(
            0x01, // count
            OpoConstants.FEATURE_WIND_NOISE_REDUCTION, // 0x1A
            0x01 // Enabled
        )
        val frame = OpoFrame(
            cmdId = OpoConstants.CMD_FEATURE_PUSH,
            sequence = 15,
            payload = payload
        )

        val state = BudsState(isWindNoiseReductionEnabled = false)
        val updated = OpoFrame.interpret(frame, state)
        assertTrue(updated.isWindNoiseReductionEnabled)
    }

    @Test
    fun testVolumeEnhancerFeatureParsing() {
        // Feature push packet for Volume Enhancer:
        // CMD_FEATURE_PUSH (0x0503), feature 0x1E (FEATURE_VOLUME_ENHANCER), status 0x01 (Enabled)
        val payload = byteArrayOf(
            0x01, // count
            OpoConstants.FEATURE_VOLUME_ENHANCER, // 0x1E
            0x01 // Enabled
        )
        val frame = OpoFrame(
            cmdId = OpoConstants.CMD_FEATURE_PUSH,
            sequence = 16,
            payload = payload
        )

        val state = BudsState(isVolumeEnhancerEnabled = false)
        val updated = OpoFrame.interpret(frame, state)
        assertTrue(updated.isVolumeEnhancerEnabled)
    }
}
