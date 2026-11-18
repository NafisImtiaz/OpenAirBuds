package com.openairbuds.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.openairbuds.app.data.BudsPreferencesRepository
import com.openairbuds.app.model.AncLevel
import com.openairbuds.app.ui.BudsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class BudsPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: BudsPreferencesRepository

    @Before
    fun setup() {
        val testFile = tempFolder.newFile("test_buds.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { testFile }
        )
        repository = BudsPreferencesRepository(dataStore)
    }

    @Test
    fun testDefaultPreferences() = runTest(testDispatcher) {
        val prefs = repository.preferencesFlow.first()

        assertNull("Default lastConnectedMac should be null", prefs.lastConnectedMac)
        assertNull("Default lastConnectedName should be null", prefs.lastConnectedName)
        assertEquals("Default preferred ANC should be HIGH_50DB", AncLevel.HIGH_50DB, prefs.preferredAncLevel)
        assertTrue("Default auto-connect should be true", prefs.autoConnectEnabled)
        assertFalse("Default BassWave enabled should be false", prefs.isBassWaveEnabled)
        assertEquals("Default BassWave level should be 0", 0, prefs.bassWaveLevel)
        assertFalse("Default onboarding completed should be false", prefs.isOnboardingCompleted)
    }

    @Test
    fun testOnboardingPreference() = runTest(testDispatcher) {
        assertFalse(repository.preferencesFlow.first().isOnboardingCompleted)

        repository.setOnboardingCompleted(true)
        assertTrue(repository.preferencesFlow.first().isOnboardingCompleted)

        repository.setOnboardingCompleted(false)
        assertFalse(repository.preferencesFlow.first().isOnboardingCompleted)
    }

    @Test
    fun testSaveLastConnectedDevice() = runTest(testDispatcher) {
        repository.saveLastConnectedDevice("AA:BB:CC:DD:EE:FF", "realme Buds Air 6 Pro")

        val prefs = repository.preferencesFlow.first()
        assertEquals("AA:BB:CC:DD:EE:FF", prefs.lastConnectedMac)
        assertEquals("realme Buds Air 6 Pro", prefs.lastConnectedName)
    }

    @Test
    fun testSavePreferredAncLevel() = runTest(testDispatcher) {
        for (level in AncLevel.entries) {
            repository.savePreferredAncLevel(level)
            val prefs = repository.preferencesFlow.first()
            assertEquals("Saved ANC level should match", level, prefs.preferredAncLevel)
        }
    }

    @Test
    fun testInvalidAncLevelFallsBackToHigh50dB() = runTest(testDispatcher) {
        // Manually write an unrecognized string to the ANC key
        dataStore.edit { p ->
            p[BudsPreferencesRepository.KEY_PREFERRED_ANC_LEVEL] = "NON_EXISTENT_MODE"
        }

        val prefs = repository.preferencesFlow.first()
        assertEquals(
            "Corrupt or unknown ANC mode string must fall back to HIGH_50DB",
            AncLevel.HIGH_50DB,
            prefs.preferredAncLevel
        )
    }

    @Test
    fun testToggleAutoConnect() = runTest(testDispatcher) {
        repository.setAutoConnectEnabled(false)
        assertFalse(repository.preferencesFlow.first().autoConnectEnabled)

        repository.setAutoConnectEnabled(true)
        assertTrue(repository.preferencesFlow.first().autoConnectEnabled)
    }

    @Test
    fun testClearPreferencesResetsToDefaults() = runTest(testDispatcher) {
        repository.saveLastConnectedDevice("11:22:33:44:55:66", "My Buds")
        repository.savePreferredAncLevel(AncLevel.SMART)
        repository.setAutoConnectEnabled(false)

        repository.clear()

        val resetPrefs = repository.preferencesFlow.first()
        assertNull(resetPrefs.lastConnectedMac)
        assertNull(resetPrefs.lastConnectedName)
        assertEquals(AncLevel.HIGH_50DB, resetPrefs.preferredAncLevel)
        assertTrue(resetPrefs.autoConnectEnabled)
    }

    @Test
    fun testSaveLastConnectedDeviceNormalizesMacAndTrimsName() = runTest(testDispatcher) {
        repository.saveLastConnectedDevice("  aa:bb:cc:dd:ee:ff  ", "   realme Buds Air 6   ")

        val prefs = repository.preferencesFlow.first()
        assertEquals("AA:BB:CC:DD:EE:FF", prefs.lastConnectedMac)
        assertEquals("realme Buds Air 6", prefs.lastConnectedName)
    }

    @Test
    fun testSaveLastConnectedDeviceIgnoresBlankMac() = runTest(testDispatcher) {
        repository.saveLastConnectedDevice("AA:BB:CC:DD:EE:FF", "Initial Buds")
        repository.saveLastConnectedDevice("   ", "Should not save")

        val prefs = repository.preferencesFlow.first()
        assertEquals("AA:BB:CC:DD:EE:FF", prefs.lastConnectedMac)
        assertEquals("Initial Buds", prefs.lastConnectedName)
    }

    @Test
    fun testCandidateMatchingPrioritizesMac() {
        val devices = listOf(
            "11:22:33:44:55:66" to "realme Buds Air 5",
            "AA:BB:CC:DD:EE:FF" to "realme Buds Air 6",
            "77:88:99:00:11:22" to "Sony WH-1000XM4"
        )

        // Preferred MAC matches second device even though first device also matches "realme"
        val matched = BudsViewModel.matchCandidateDevice(devices, "aa:bb:cc:dd:ee:ff")
        assertNotNull(matched)
        assertEquals("AA:BB:CC:DD:EE:FF", matched?.first)
        assertEquals("realme Buds Air 6", matched?.second)
    }

    @Test
    fun testCandidateMatchingPrioritizesConnectedA2dpDevice() {
        val devices = listOf(
            "11:22:33:44:55:66" to "realme Buds Air 5",
            "AA:BB:CC:DD:EE:FF" to "realme Buds Air 6",
            "77:88:99:00:11:22" to "Sony WH-1000XM4"
        )

        // Even if preferredMac points to Air 5, active connectedMacs has Air 6
        val matched = BudsViewModel.matchCandidateDevice(
            devices,
            preferredMac = "11:22:33:44:55:66",
            connectedMacs = setOf("AA:BB:CC:DD:EE:FF")
        )
        assertNotNull(matched)
        assertEquals("AA:BB:CC:DD:EE:FF", matched?.first)
        assertEquals("realme Buds Air 6", matched?.second)
    }


    @Test
    fun testCandidateMatchingFallsBackToRealmeNameWhenMacNotFound() {
        val devices = listOf(
            "99:99:99:99:99:99" to "Bluetooth Keyboard",
            "11:22:33:44:55:66" to "realme Buds Air 6",
            "77:88:99:00:11:22" to "Other Earbuds"
        )

        // MAC not found in list -> should fall back to first realme device
        val matched = BudsViewModel.matchCandidateDevice(devices, "00:00:00:00:00:00")
        assertNotNull(matched)
        assertEquals("11:22:33:44:55:66", matched?.first)
    }

    @Test
    fun testCandidateMatchingFallsBackToRealmeNameWhenMacNull() {
        val devices = listOf(
            "99:99:99:99:99:99" to "Bluetooth Mouse",
            "11:22:33:44:55:66" to "realme Buds Air 6"
        )

        val matched = BudsViewModel.matchCandidateDevice(devices, null)
        assertNotNull(matched)
        assertEquals("11:22:33:44:55:66", matched?.first)
    }

    @Test
    fun testCandidateMatchingReturnsNullWhenNoDeviceMatches() {
        val devices = listOf(
            "99:99:99:99:99:99" to "Bluetooth Mouse",
            "88:88:88:88:88:88" to "Car Audio"
        )

        val matched = BudsViewModel.matchCandidateDevice(devices, null)
        assertNull(matched)
    }

    @Test
    fun testSaveHoldCycleMask() = runTest(testDispatcher) {
        val mask = 0x0A
        repository.saveHoldCycleMask(mask)
        val prefs = repository.preferencesFlow.first()
        assertEquals(0x0A, prefs.holdCycleMask)
    }

    @Test
    fun testSaveCustomEqGains() = runTest(testDispatcher) {
        val testGains = listOf(2, -3, 0, 5, -1, 4)
        repository.saveCustomEqGains(testGains)
        val prefs = repository.preferencesFlow.first()
        assertEquals(testGains, prefs.customEqGains)
    }

    @Test
    fun testCustomEqGainsClampingOnSave() = runTest(testDispatcher) {
        val rawGains = listOf(-10, 15, 0, 7, -6, 2)
        repository.saveCustomEqGains(rawGains)
        val prefs = repository.preferencesFlow.first()
        val expected = listOf(-6, 6, 0, 6, -6, 2)
        assertEquals(expected, prefs.customEqGains)
    }

    @Test
    fun testSaveHiResAudio() = runTest(testDispatcher) {
        repository.saveHiResAudio(true)
        assertTrue(repository.preferencesFlow.first().isHiResAudioEnabled)

        repository.saveHiResAudio(false)
        assertFalse(repository.preferencesFlow.first().isHiResAudioEnabled)
    }

    @Test
    fun testSaveVolumeEnhancer() = runTest(testDispatcher) {
        repository.saveVolumeEnhancer(true)
        assertTrue(repository.preferencesFlow.first().isVolumeEnhancerEnabled)

        repository.saveVolumeEnhancer(false)
        assertFalse(repository.preferencesFlow.first().isVolumeEnhancerEnabled)
    }

    @Test
    fun testSaveWindNoiseReduction() = runTest(testDispatcher) {
        repository.saveWindNoiseReduction(true)
        assertTrue(repository.preferencesFlow.first().isWindNoiseReductionEnabled)

        repository.saveWindNoiseReduction(false)
        assertFalse(repository.preferencesFlow.first().isWindNoiseReductionEnabled)
    }

    @Test
    fun testSaveBassWave() = runTest(testDispatcher) {
        repository.saveBassWave(enabled = true, level = 3)
        val prefs = repository.preferencesFlow.first()
        assertTrue(prefs.isBassWaveEnabled)
        assertEquals(3, prefs.bassWaveLevel)

        repository.saveBassWave(enabled = false, level = -2)
        val updatedPrefs = repository.preferencesFlow.first()
        assertFalse(updatedPrefs.isBassWaveEnabled)
        assertEquals(-2, updatedPrefs.bassWaveLevel)
    }

    @Test
    fun testSaveBassWaveLevelClamping() = runTest(testDispatcher) {
        repository.saveBassWave(enabled = true, level = 10)
        assertEquals(5, repository.preferencesFlow.first().bassWaveLevel)

        repository.saveBassWave(enabled = true, level = -10)
        assertEquals(-5, repository.preferencesFlow.first().bassWaveLevel)
    }

    @Test
    fun testConcurrentPreferenceUpdates() = runTest(testDispatcher) {
        val j1 = launch { repository.savePreferredAncLevel(AncLevel.SMART) }
        val j2 = launch { repository.setAutoConnectEnabled(false) }
        val j3 = launch { repository.saveLastConnectedDevice("12:34:56:78:9A:BC", "Test Buds") }

        joinAll(j1, j2, j3)

        val prefs = repository.preferencesFlow.first()
        assertEquals(AncLevel.SMART, prefs.preferredAncLevel)
        assertFalse(prefs.autoConnectEnabled)
        assertEquals("12:34:56:78:9A:BC", prefs.lastConnectedMac)
        assertEquals("Test Buds", prefs.lastConnectedName)
    }
}
