package com.openairbuds.app

import com.openairbuds.app.model.DeviceProfileRegistry
import com.openairbuds.app.model.EarbudArtwork
import com.openairbuds.app.model.EarbudBrand
import com.openairbuds.app.model.NoiseControlMode
import com.openairbuds.app.protocol.OpoConstants
import com.openairbuds.app.protocol.OpoFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceProfileRegistryTest {

    @Test
    fun testProductIdResolution() {
        val air6 = DeviceProfileRegistry.resolve("060414", null)
        assertEquals("realme Buds Air 6", air6.displayName)
        assertEquals(EarbudBrand.REALME, air6.brand)
        assertTrue(air6.capabilities.hasAnc)
        assertEquals(50, air6.capabilities.maxAncDb)
        assertFalse("Base Buds Air 6 must not have Dual Connection (multipoint)", air6.capabilities.hasDualConnection)
        assertTrue(air6.artwork is EarbudArtwork.BundledResource)

        val air6Pid3412 = DeviceProfileRegistry.resolve("063412", null)
        assertEquals("realme Buds Air 6", air6Pid3412.displayName)
        assertEquals(EarbudBrand.REALME, air6Pid3412.brand)
        assertTrue(air6Pid3412.artwork is EarbudArtwork.BundledResource)

        val air6Pid3814 = DeviceProfileRegistry.resolve("063814", null)
        assertEquals("realme Buds Air 6", air6Pid3814.displayName)
        assertTrue(air6Pid3814.artwork is EarbudArtwork.BundledResource)

        val air6Pid103412 = DeviceProfileRegistry.resolve("103412", null)
        assertEquals("realme Buds Air 6", air6Pid103412.displayName)
        assertTrue(air6Pid103412.artwork is EarbudArtwork.BundledResource)

        val air6CustomName = DeviceProfileRegistry.resolve("063412", "My Custom Earbuds")
        assertEquals("realme Buds Air 6", air6CustomName.displayName)
        assertEquals("realme_buds_air_6", air6CustomName.id)
        assertTrue(air6CustomName.artwork is EarbudArtwork.BundledResource)

        val air6Pro = DeviceProfileRegistry.resolve("060415", null)
        assertEquals("realme Buds Air 6 Pro", air6Pro.displayName)
        assertTrue(air6Pro.capabilities.hasCustomEq)
        assertTrue("Buds Air 6 Pro must have Dual Connection (multipoint)", air6Pro.capabilities.hasDualConnection)
    }

    @Test
    fun testResolveArtwork() {
        val artworkAir6 = DeviceProfileRegistry.resolveArtwork(DeviceProfileRegistry.REALME_BUDS_AIR_6)
        assertTrue(artworkAir6 is EarbudArtwork.BundledResource)

        val genericWithAir6Name = DeviceProfileRegistry.resolveArtwork(
            DeviceProfileRegistry.GENERIC_PROFILE,
            "Buds Air 6"
        )
        assertTrue(genericWithAir6Name is EarbudArtwork.BundledResource)

        val genericDefault = DeviceProfileRegistry.resolveArtwork(
            DeviceProfileRegistry.GENERIC_PROFILE,
            "Unknown Headphones"
        )
        assertTrue(genericDefault is EarbudArtwork.BundledResource)
    }


    @Test
    fun testBluetoothNamePatternResolution() {
        val t100 = DeviceProfileRegistry.resolve(null, "realme Buds T100")
        assertEquals(EarbudBrand.REALME, t100.brand)
        assertFalse("Buds T100 must not have ANC", t100.capabilities.hasAnc)
        assertFalse("Buds T100 must not have In-Ear Detection", t100.capabilities.hasInEarDetection)

        val air6NoSpace = DeviceProfileRegistry.resolve(null, "Buds Air6")
        assertEquals("realme Buds Air 6", air6NoSpace.displayName)
        assertEquals(EarbudBrand.REALME, air6NoSpace.brand)
        assertTrue(air6NoSpace.capabilities.hasAnc)

        val t300 = DeviceProfileRegistry.resolve(null, "realme Buds T300")
        assertTrue(t300.capabilities.hasAnc)
        assertEquals(30, t300.capabilities.maxAncDb)

        val oneplus = DeviceProfileRegistry.resolve(null, "OnePlus Buds Pro 2")
        assertEquals(EarbudBrand.ONEPLUS, oneplus.brand)
        assertTrue(oneplus.capabilities.hasAnc)

        val enco = DeviceProfileRegistry.resolve(null, "OPPO Enco Air 3 Pro")
        assertEquals(EarbudBrand.OPPO, enco.brand)
        assertTrue(enco.capabilities.hasAnc)
    }

    @Test
    fun testBrandFallbackForUnlistedDevices() {
        val unlistedRealme = DeviceProfileRegistry.resolve(null, "realme Buds Special Edition")
        assertEquals(EarbudBrand.REALME, unlistedRealme.brand)
        assertEquals("realme Buds Special Edition", unlistedRealme.displayName)

        val unlistedOnePlus = DeviceProfileRegistry.resolve(null, "OnePlus Nord Buds X")
        assertEquals(EarbudBrand.ONEPLUS, unlistedOnePlus.brand)
        assertEquals("OnePlus Nord Buds X", unlistedOnePlus.displayName)

        val unlistedAir6Pro = DeviceProfileRegistry.resolve(null, "Air 6 Pro Edition")
        assertEquals("realme_buds_air_6_pro", unlistedAir6Pro.id)

        val unlistedAir6 = DeviceProfileRegistry.resolve(null, "Air 6 Edition")
        assertEquals("realme_buds_air_6", unlistedAir6.id)

        val generic = DeviceProfileRegistry.resolve(null, "Custom TWS")
        assertEquals(EarbudBrand.GENERIC, generic.brand)
    }

    @Test
    fun testDynamicProbePromotesAncOnIncomingResponse() {
        val initialProfile = DeviceProfileRegistry.REALME_BUDS_T100
        assertFalse(initialProfile.capabilities.hasAnc)

        var state = com.openairbuds.app.model.BudsState(deviceProfile = initialProfile)
        assertFalse(state.deviceProfile.capabilities.hasAnc)

        // Earbud unexpectedly responds with ANC state (wire = 0x08, ANC Max)
        val ancRespFrame = OpoFrame(
            cmdId = OpoConstants.CMD_RESP_ANC,
            sequence = 0x01.toByte(),
            payload = byteArrayOf(0x01, 0x01, OpoConstants.WIRE_ANC_MAX)
        )

        state = OpoFrame.interpret(ancRespFrame, state)

        assertTrue("Dynamic probe must promote hasAnc to true when CMD_RESP_ANC is received", state.deviceProfile.capabilities.hasAnc)
        assertEquals(NoiseControlMode.NOISE_CANCELLATION, state.noiseMode)
    }
}
