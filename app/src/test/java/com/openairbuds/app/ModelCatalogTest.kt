package com.openairbuds.app

import com.openairbuds.app.model.CatalogModel
import com.openairbuds.app.model.DeviceProfileRegistry
import com.openairbuds.app.model.EarbudBrand
import com.openairbuds.app.model.ModelCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class ModelCatalogTest {

    @Before
    fun setUp() {
        val candidates = listOf(
            File("app/src/main/assets/models.json"),
            File("src/main/assets/models.json"),
            File("models.json")
        )
        val file = candidates.firstOrNull { it.exists() }
            ?: throw IllegalStateException("models.json not found in candidate paths")
        ModelCatalog.init(file.readText())
    }

    @Test
    fun testCatalogLoadsAllModels() {
        val allModels = ModelCatalog.all()
        assertEquals(143, allModels.size)
    }

    @Test
    fun test4TierIdentificationOrder() {
        // Tier 1: Exact Name + Product ID match
        val tier1 = ModelCatalog.find("063412", "realme Buds Air6")
        assertNotNull(tier1)
        assertEquals("063412", tier1!!.id)
        assertEquals("realme Buds Air6", tier1.name)

        // Tier 2: Name match wins over conflicting Product ID
        // (OPPO Enco Buds2 reports 060C12, which is also realme Buds Q2s id)
        val tier2 = ModelCatalog.find("060C12", "OPPO Enco Buds2")
        assertNotNull(tier2)
        assertEquals("OPPO Enco Buds2", tier2!!.name)

        // Tier 3: Product ID match when device was renamed by user
        val tier3 = ModelCatalog.find("063012", "My Custom Earbuds")
        assertNotNull(tier3)
        assertEquals("063012", tier3!!.id)
        assertEquals("realme Buds Air6 Pro", tier3.name)

        // Tier 4: Squashed alphanumeric name match (handling spaces and case)
        val tier4 = ModelCatalog.find(null, "realme Buds Air 6")
        assertNotNull(tier4)
        assertEquals("063412", tier4!!.id)
        assertEquals("realme Buds Air6", tier4.name)

        val tier4OnePlus = ModelCatalog.find(null, "oneplusbuds3")
        assertNotNull(tier4OnePlus)
        assertEquals("063C14", tier4OnePlus!!.id)
        assertEquals("OnePlus Buds 3", tier4OnePlus.name)
    }

    @Test
    fun testColorRangeNormalization() {
        // 0x100100..0x100102 folds into 060414 (OnePlus Bullets Wireless Z2 in QuickBuds / OnePlus Buds)
        val normalized = ModelCatalog.find("100100", null)
        assertNotNull(normalized)
        assertEquals("060414", normalized!!.id)
    }

    @Test
    fun testDynamicCapabilitiesDerivation() {
        // OnePlus Buds 3 (063C14)
        val op3 = ModelCatalog.find("063C14", null)
        assertNotNull(op3)
        val profileOp3 = ModelCatalog.toDeviceProfile(op3!!)
        assertEquals(EarbudBrand.ONEPLUS, profileOp3.brand)
        assertTrue("OnePlus Buds 3 supports ANC", profileOp3.capabilities.hasAnc)
        assertTrue("OnePlus Buds 3 has dual connection", profileOp3.capabilities.hasDualConnection)
        assertTrue("OnePlus Buds 3 has bass engine", profileOp3.capabilities.hasBassWave)

        // OnePlus Bullets Wireless Z2 (050414) - non-ANC
        val bwZ2 = ModelCatalog.find("050414", null)
        assertNotNull(bwZ2)
        val profileBwZ2 = ModelCatalog.toDeviceProfile(bwZ2!!)
        assertFalse("Bullets Wireless Z2 has no ANC", profileBwZ2.capabilities.hasAnc)

        // OPPO Enco M33 (050C10)
        val m33 = ModelCatalog.find("050C10", null)
        assertNotNull(m33)
        val profileM33 = ModelCatalog.toDeviceProfile(m33!!)
        assertEquals(EarbudBrand.OPPO, profileM33.brand)
        assertTrue("OPPO Enco M33 supports ANC", profileM33.capabilities.hasAnc)
        assertTrue("OPPO Enco M33 has bass engine", profileM33.capabilities.hasBassWave)
    }

    @Test
    fun testIntegrationWithDeviceProfileRegistry() {
        // Unlisted in old hardcoded list, but present in models.json:
        // realme Buds Wireless 6 ANC (052052)
        val bw6Anc = DeviceProfileRegistry.resolve("052052", null)
        assertEquals(EarbudBrand.REALME, bw6Anc.brand)
        assertEquals("realme Buds Wireless 6 ANC", bw6Anc.displayName)
        assertTrue(bw6Anc.capabilities.hasAnc)

        // OPPO Enco Air4 Pro (067C10)
        val encoAir4Pro = DeviceProfileRegistry.resolve(null, "OPPO Enco Air4 Pro")
        assertEquals(EarbudBrand.OPPO, encoAir4Pro.brand)
        assertEquals("OPPO Enco Air4 Pro", encoAir4Pro.displayName)
        assertTrue(encoAir4Pro.capabilities.hasAnc)

        // realme Buds Air 6 (063412) - Base Air 6: LHDC Hi-Res Audio, NO Dual Connection
        val air6 = DeviceProfileRegistry.resolve("063412", "realme Buds Air 6")
        assertEquals(EarbudBrand.REALME, air6.brand)
        assertFalse("Base Buds Air 6 must NOT have Dual Connection", air6.capabilities.hasDualConnection)
        assertTrue("Buds Air 6 supports Hi-Res Audio", air6.capabilities.hasHiResAudio)
        assertEquals("LHDC", air6.capabilities.hiResCodec)

        // realme Buds Air 6 by short name "Buds Air 6"
        val air6Short = DeviceProfileRegistry.resolve(null, "Buds Air 6")
        assertFalse("Buds Air 6 (short name) must NOT have Dual Connection", air6Short.capabilities.hasDualConnection)
        assertTrue(air6Short.capabilities.hasHiResAudio)
        assertEquals("LHDC", air6Short.capabilities.hiResCodec)

        // realme Buds Air 6 Pro (063012) - Pro Air 6: LDAC Hi-Res Audio, WITH Dual Connection
        val air6Pro = DeviceProfileRegistry.resolve("063012", "realme Buds Air6 Pro")
        assertEquals(EarbudBrand.REALME, air6Pro.brand)
        assertTrue("Buds Air 6 Pro MUST have Dual Connection", air6Pro.capabilities.hasDualConnection)
        assertTrue("Buds Air 6 Pro supports Hi-Res Audio", air6Pro.capabilities.hasHiResAudio)
        assertEquals("LDAC", air6Pro.capabilities.hiResCodec)
    }
}
