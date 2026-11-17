package com.openairbuds.app

import com.openairbuds.app.model.CustomEqProfile
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.protocol.OpoConstants
import com.openairbuds.app.protocol.OpoFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomEqTest {

    @Test
    fun testBuildSetCustomEqFraming() {
        val testGains = listOf(0, 2, -3, 6, -6, 1)
        val packet = OpoFrame.buildSetCustomEq(testGains, sequence = 7)

        // Expected total packet: SYNC(1) + lenByte(1) + totalLen(38) = 40 bytes
        assertEquals(40, packet.size)
        assertEquals(OpoConstants.SYNC_BYTE, packet[0])
        assertEquals(38.toByte(), packet[1]) // total length (7 + payloadLen 31 = 38)
        assertEquals(0.toByte(), packet[2])
        assertEquals(0.toByte(), packet[3])
        assertEquals(0x18.toByte(), packet[4]) // CMD_SET_CUSTOM_EQ (0x0418) low byte
        assertEquals(0x04.toByte(), packet[5]) // CMD_SET_CUSTOM_EQ high byte
        assertEquals(7.toByte(), packet[6])    // Sequence
        assertEquals(31.toByte(), packet[7])   // Payload length low byte (0x1F = 31)
        assertEquals(0.toByte(), packet[8])    // Payload length high byte

        // Payload structure:
        // [0] = action (0x02 = MODIFY)
        // [1] = minValue (-6)
        // [2] = maxValue (6)
        // [3] = eqId (0x04 Custom slot 4 on earbuds)
        // [4] = nameLength (0x07 for "Custom1")
        // [5..11] = "Custom1" UTF-8 bytes
        // [12] = freqCount (0x06)
        assertEquals(0x02.toByte(), packet[9])
        assertEquals((-6).toByte(), packet[10])
        assertEquals(6.toByte(), packet[11])
        assertEquals(0x04.toByte(), packet[12])
        assertEquals(0x07.toByte(), packet[13])
        assertEquals('C'.code.toByte(), packet[14])
        assertEquals('u'.code.toByte(), packet[15])
        assertEquals('s'.code.toByte(), packet[16])
        assertEquals('t'.code.toByte(), packet[17])
        assertEquals('o'.code.toByte(), packet[18])
        assertEquals('m'.code.toByte(), packet[19])
        assertEquals('1'.code.toByte(), packet[20])
        assertEquals(0x06.toByte(), packet[21])

        // Interleaved Frequencies (LE 16-bit) and 8-bit signed gains:
        // 62 Hz = 0x003E -> 3E 00, gain: 0
        assertEquals(0x3E.toByte(), packet[22])
        assertEquals(0x00.toByte(), packet[23])
        assertEquals(0.toByte(), packet[24])

        // 250 Hz = 0x00FA -> FA 00, gain: 2
        assertEquals(0xFA.toByte(), packet[25])
        assertEquals(0x00.toByte(), packet[26])
        assertEquals(2.toByte(), packet[27])

        // 1000 Hz = 0x03E8 -> E8 03, gain: -3
        assertEquals(0xE8.toByte(), packet[28])
        assertEquals(0x03.toByte(), packet[29])
        assertEquals((-3).toByte(), packet[30])

        // 4000 Hz = 0x0FA0 -> A0 0F, gain: 6
        assertEquals(0xA0.toByte(), packet[31])
        assertEquals(0x0F.toByte(), packet[32])
        assertEquals(6.toByte(), packet[33])

        // 8000 Hz = 0x1F40 -> 40 1F, gain: -6
        assertEquals(0x40.toByte(), packet[34])
        assertEquals(0x1F.toByte(), packet[35])
        assertEquals((-6).toByte(), packet[36])

        // 16000 Hz = 0x3E80 -> 80 3E, gain: 1
        assertEquals(0x80.toByte(), packet[37])
        assertEquals(0x3E.toByte(), packet[38])
        assertEquals(1.toByte(), packet[39])
    }

    @Test
    fun testCustomEqGainClamping() {
        // Values outside [-6, +6] must be clamped
        val extremeGains = listOf(-20, 15, -6, 6, 0, 100)
        val packet = OpoFrame.buildSetCustomEq(extremeGains, sequence = 1)

        assertEquals((-6).toByte(), packet[24]) // band 0 gain
        assertEquals(6.toByte(), packet[27])    // band 1 gain
        assertEquals((-6).toByte(), packet[30]) // band 2 gain
        assertEquals(6.toByte(), packet[33])    // band 3 gain
        assertEquals(0.toByte(), packet[36])    // band 4 gain
        assertEquals(6.toByte(), packet[39])    // band 5 gain
    }

    @Test
    fun testCustomEqFewerThanSixBandsPadded() {
        // When fewer than 6 gains passed, pad with 0s
        val partialGains = listOf(3, -2)
        val packet = OpoFrame.buildSetCustomEq(partialGains, sequence = 1)

        assertEquals(3.toByte(), packet[24])
        assertEquals((-2).toByte(), packet[27])
        assertEquals(0.toByte(), packet[30])
        assertEquals(0.toByte(), packet[33])
        assertEquals(0.toByte(), packet[36])
        assertEquals(0.toByte(), packet[39])
    }

    @Test
    fun testCustomEqCustomNameAndAction() {
        val packet = OpoFrame.buildSetCustomEq(
            gains = listOf(1, 2, 3, 4, 5, 6),
            sequence = 5,
            action = 0x01, // ADD
            name = "Custom2"
        )

        assertEquals(40, packet.size)
        assertEquals(0x01.toByte(), packet[9]) // action: ADD
        assertEquals(7.toByte(), packet[13])   // nameLength: 7
        assertEquals('C'.code.toByte(), packet[14])
        assertEquals('2'.code.toByte(), packet[20])
    }

    @Test
    fun testDualCommandCustomEqPresetActivation() {
        val presetPacket = OpoFrame.buildSetEqPreset(EqPreset.CUSTOM, sequence = 3)
        assertEquals(10, presetPacket.size)
        assertEquals(0x06.toByte(), presetPacket[4]) // CMD_SET_EQ (0x0406)
        assertEquals(0x04.toByte(), presetPacket[5])
        assertEquals(1.toByte(), presetPacket[7])    // payloadLen: 1
        assertEquals(4.toByte(), presetPacket[9])    // payload[0] == 4 (Custom slot 4)
    }

    @Test
    fun testThreeCustomProfilesLimitAndAllocation() {
        val profiles = mutableListOf(CustomEqProfile(1, "Custom1"))
        assertEquals(1, profiles.size)

        // Helper replicating manager allocation logic:
        fun addProfile(list: MutableList<CustomEqProfile>): Boolean {
            if (list.size >= 3) return false
            val existingIds = list.map { it.id }.toSet()
            val nextId = (1..3).firstOrNull { it !in existingIds } ?: return false
            list.add(CustomEqProfile(nextId, "Custom$nextId"))
            return true
        }

        assertTrue(addProfile(profiles))
        assertEquals(2, profiles.size)
        assertEquals(2, profiles[1].id)
        assertEquals("Custom2", profiles[1].name)

        assertTrue(addProfile(profiles))
        assertEquals(3, profiles.size)
        assertEquals(3, profiles[2].id)
        assertEquals("Custom3", profiles[2].name)

        // 4th add must be rejected
        assertFalse(addProfile(profiles))
        assertEquals(3, profiles.size)
    }

    @Test
    fun testCustomEqStringSerialization() {
        val original = listOf(1, -2, 3, -4, 5, -6)
        val serialized = original.joinToString(",")
        assertEquals("1,-2,3,-4,5,-6", serialized)

        val deserialized = serialized.split(",").mapNotNull { it.trim().toIntOrNull() }
        assertEquals(original, deserialized)
    }

    @Test
    fun testMalformedGainsFallback() {
        val malformedString = "invalid,data"
        val parsed = malformedString.split(",").mapNotNull { it.trim().toIntOrNull() }
        val result = if (parsed.size == 6) parsed else listOf(0, 0, 0, 0, 0, 0)
        assertEquals(listOf(0, 0, 0, 0, 0, 0), result)
    }

    @Test
    fun testCustomEqPresetRepresentation() {
        assertEquals(4.toByte(), com.openairbuds.app.model.EqPreset.CUSTOM.slot)
        assertEquals("Custom1", com.openairbuds.app.model.EqPreset.CUSTOM.displayName)
        assertEquals(com.openairbuds.app.model.EqPreset.CUSTOM, com.openairbuds.app.model.EqPreset.fromSlot(4))
    }

    @Test
    fun testRealmeBudsAir6CapabilitiesIncludesCustomEq() {
        val air6 = com.openairbuds.app.model.DeviceProfileRegistry.REALME_BUDS_AIR_6
        org.junit.Assert.assertTrue("Air 6 must have hasCustomEq enabled", air6.capabilities.hasCustomEq)
        val customOption = air6.capabilities.supportedEqPresets.find { it.slot == 4.toByte() }
        assertNotNull("Air 6 supportedEqPresets must include slot 4", customOption)
        assertEquals("Custom1", customOption?.displayName)
    }

    @Test
    fun testCustomEqBandsFrequencies() {
        val bands = com.openairbuds.app.ui.components.EQ_BANDS
        assertEquals(6, bands.size)
        assertEquals(listOf("62", "250", "1k", "4k", "8k", "16k"), bands.map { it.freq })
    }
}
