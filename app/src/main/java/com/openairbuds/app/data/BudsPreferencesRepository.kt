package com.openairbuds.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.openairbuds.app.model.AncLevel
import com.openairbuds.app.model.CustomEqProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

data class BudsPreferences(
    val lastConnectedMac: String? = null,
    val lastConnectedName: String? = null,
    val preferredAncLevel: AncLevel = AncLevel.HIGH_50DB,
    val autoConnectEnabled: Boolean = true,
    val holdCycleMask: Int = 0x0B,
    val customEqProfiles: List<CustomEqProfile> = listOf(CustomEqProfile(1, "Custom1", listOf(0, 0, 0, 0, 0, 0))),
    val selectedCustomEqId: Int = 1,
    val customEqGains: List<Int> = listOf(0, 0, 0, 0, 0, 0),
    val isHiResAudioEnabled: Boolean = false,
    val isVolumeEnhancerEnabled: Boolean = false,
    val isWindNoiseReductionEnabled: Boolean = false,
    val isBassWaveEnabled: Boolean = false,
    val bassWaveLevel: Int = 0,
    val isVoiceEnhanceEnabled: Boolean = false,
    val isAutoAnswerCallsEnabled: Boolean = false,
    val isFindPhoneEnabled: Boolean = false,
    val isOnboardingCompleted: Boolean = false
)

class BudsPreferencesRepository(
    private val dataStore: DataStore<Preferences>,
    private val context: Context? = null
) {

    val preferencesFlow: Flow<BudsPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val mac = prefs[KEY_LAST_CONNECTED_MAC]
            val name = prefs[KEY_LAST_CONNECTED_NAME]
            val ancLevelStr = prefs[KEY_PREFERRED_ANC_LEVEL]
            val preferredAnc = ancLevelStr?.let {
                runCatching { AncLevel.valueOf(it) }.getOrNull()
            } ?: AncLevel.HIGH_50DB
            val autoConnect = prefs[KEY_AUTO_CONNECT_ENABLED] ?: true
            val holdCycle = prefs[KEY_HOLD_CYCLE_MASK] ?: 0x0B
            val customEqStr = prefs[KEY_CUSTOM_EQ_GAINS]
            val legacyCustomEq = customEqStr?.split(",")?.mapNotNull { it.trim().toIntOrNull() }?.takeIf { it.size == 6 }
                ?: listOf(0, 0, 0, 0, 0, 0)

            val profilesStr = prefs[KEY_CUSTOM_EQ_PROFILES]
            val parsedProfiles = profilesStr?.split(";")?.mapNotNull { item ->
                val parts = item.split("#")
                if (parts.size == 3) {
                    val id = parts[0].toIntOrNull() ?: 1
                    val name = parts[1].ifEmpty { "Custom$id" }
                    val gains = parts[2].split(",").mapNotNull { it.trim().toIntOrNull() }.takeIf { it.size == 6 }
                        ?: listOf(0, 0, 0, 0, 0, 0)
                    CustomEqProfile(id, name, gains)
                } else null
            }?.takeIf { it.isNotEmpty() } ?: listOf(CustomEqProfile(1, "Custom1", legacyCustomEq))

            val selectedId = prefs[KEY_SELECTED_CUSTOM_EQ_ID] ?: 1
            val activeGains = parsedProfiles.find { it.id == selectedId }?.gains
                ?: parsedProfiles.firstOrNull()?.gains
                ?: legacyCustomEq

            val hiResAudio = prefs[KEY_HI_RES_AUDIO] ?: false
            val volumeEnhancer = prefs[KEY_VOLUME_ENHANCER] ?: false
            val windNoise = prefs[KEY_WIND_NOISE_REDUCTION] ?: false
            val bassWaveEnabled = prefs[KEY_BASS_WAVE_ENABLED] ?: false
            val bassWaveLevel = prefs[KEY_BASS_WAVE_LEVEL] ?: 0
            val voiceEnhance = prefs[KEY_VOICE_ENHANCE] ?: false
            val autoAnswerCalls = prefs[KEY_AUTO_ANSWER_CALLS] ?: false
            val findPhone = prefs[KEY_FIND_PHONE] ?: false
            val onboardingCompleted = prefs[KEY_ONBOARDING_COMPLETED] ?: false

            BudsPreferences(
                lastConnectedMac = mac,
                lastConnectedName = name,
                preferredAncLevel = preferredAnc,
                autoConnectEnabled = autoConnect,
                holdCycleMask = holdCycle,
                customEqProfiles = parsedProfiles,
                selectedCustomEqId = selectedId,
                customEqGains = activeGains,
                isHiResAudioEnabled = hiResAudio,
                isVolumeEnhancerEnabled = volumeEnhancer,
                isWindNoiseReductionEnabled = windNoise,
                isBassWaveEnabled = bassWaveEnabled,
                bassWaveLevel = bassWaveLevel,
                isVoiceEnhanceEnabled = voiceEnhance,
                isAutoAnswerCallsEnabled = autoAnswerCalls,
                isFindPhoneEnabled = findPhone,
                isOnboardingCompleted = onboardingCompleted
            )
        }

    suspend fun saveLastConnectedDevice(mac: String, name: String) {
        val trimmedMac = mac.trim().uppercase()
        val trimmedName = name.trim()
        if (trimmedMac.isNotEmpty()) {
            dataStore.edit { prefs ->
                prefs[KEY_LAST_CONNECTED_MAC] = trimmedMac
                prefs[KEY_LAST_CONNECTED_NAME] = trimmedName.ifEmpty { "realme Buds Air 6" }
            }
        }
    }

    suspend fun savePreferredAncLevel(level: AncLevel) {
        dataStore.edit { prefs ->
            prefs[KEY_PREFERRED_ANC_LEVEL] = level.name
        }
    }

    suspend fun setAutoConnectEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_CONNECT_ENABLED] = enabled
        }
    }

    suspend fun saveHoldCycleMask(mask: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_HOLD_CYCLE_MASK] = mask
        }
    }

    suspend fun saveCustomEqProfiles(profiles: List<CustomEqProfile>, selectedId: Int) {
        val serialized = profiles.take(3).joinToString(";") { p ->
            val clamped = p.gains.take(6).map { it.coerceIn(-6, 6) }
            val finalGains = clamped + List(maxOf(0, 6 - clamped.size)) { 0 }
            "${p.id}#${p.name.replace('#', '_').replace(';', '_')}#${finalGains.joinToString(",")}"
        }
        val activeGains = profiles.find { it.id == selectedId }?.gains
            ?: profiles.firstOrNull()?.gains
            ?: listOf(0, 0, 0, 0, 0, 0)
        val clampedActive = activeGains.take(6).map { it.coerceIn(-6, 6) }
        val finalActiveGains = clampedActive + List(maxOf(0, 6 - clampedActive.size)) { 0 }
        dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_EQ_PROFILES] = serialized
            prefs[KEY_SELECTED_CUSTOM_EQ_ID] = selectedId
            prefs[KEY_CUSTOM_EQ_GAINS] = finalActiveGains.joinToString(",")
        }
    }

    suspend fun saveCustomEqGains(gains: List<Int>) {
        val clamped = gains.take(6).map { it.coerceIn(-6, 6) }
        val finalGains = clamped + List(maxOf(0, 6 - clamped.size)) { 0 }
        dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_EQ_GAINS] = finalGains.joinToString(",")
            val currentProfilesStr = prefs[KEY_CUSTOM_EQ_PROFILES]
            val selectedId = prefs[KEY_SELECTED_CUSTOM_EQ_ID] ?: 1
            val profiles = currentProfilesStr?.split(";")?.mapNotNull { item ->
                val parts = item.split("#")
                if (parts.size == 3) {
                    val id = parts[0].toIntOrNull() ?: 1
                    val name = parts[1].ifEmpty { "Custom$id" }
                    val g = parts[2].split(",").mapNotNull { it.trim().toIntOrNull() }.takeIf { it.size == 6 }
                        ?: listOf(0, 0, 0, 0, 0, 0)
                    CustomEqProfile(id, name, g)
                } else null
            }?.toMutableList() ?: mutableListOf(CustomEqProfile(1, "Custom1", finalGains))

            val existingIndex = profiles.indexOfFirst { it.id == selectedId }
            if (existingIndex >= 0) {
                profiles[existingIndex] = profiles[existingIndex].copy(gains = finalGains)
            } else {
                profiles.add(CustomEqProfile(selectedId, "Custom$selectedId", finalGains))
            }
            prefs[KEY_CUSTOM_EQ_PROFILES] = profiles.take(3).joinToString(";") { p ->
                "${p.id}#${p.name.replace('#', '_').replace(';', '_')}#${p.gains.joinToString(",")}"
            }
        }
    }

    suspend fun saveHiResAudio(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_HI_RES_AUDIO] = enabled
        }
    }

    suspend fun saveVolumeEnhancer(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_VOLUME_ENHANCER] = enabled
        }
    }

    suspend fun saveWindNoiseReduction(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_WIND_NOISE_REDUCTION] = enabled
        }
    }

    suspend fun saveBassWave(enabled: Boolean, level: Int) {
        val clampedLevel = level.coerceIn(-5, 5)
        dataStore.edit { prefs ->
            prefs[KEY_BASS_WAVE_ENABLED] = enabled
            prefs[KEY_BASS_WAVE_LEVEL] = clampedLevel
        }
    }

    suspend fun saveVoiceEnhance(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_VOICE_ENHANCE] = enabled
        }
    }

    suspend fun saveAutoAnswerCalls(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_ANSWER_CALLS] = enabled
        }
    }

    suspend fun saveFindPhone(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_FIND_PHONE] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context?.let { setOnboardingCompletedSync(it, completed) }
        dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun clear() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    companion object {
        private const val FAST_PREFS_NAME = "buds_fast_prefs"
        private const val PREF_ONBOARDING_COMPLETED = "onboarding_completed"

        fun isOnboardingCompletedSync(context: Context, isBluetoothGranted: Boolean = false): Boolean {
            val prefs = context.getSharedPreferences(FAST_PREFS_NAME, Context.MODE_PRIVATE)
            if (prefs.contains(PREF_ONBOARDING_COMPLETED)) {
                return prefs.getBoolean(PREF_ONBOARDING_COMPLETED, false)
            }
            if (isBluetoothGranted) {
                prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETED, true).apply()
                return true
            }
            return false
        }

        fun setOnboardingCompletedSync(context: Context, completed: Boolean) {
            val prefs = context.getSharedPreferences(FAST_PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putBoolean(PREF_ONBOARDING_COMPLETED, completed).apply()
        }
        val KEY_LAST_CONNECTED_MAC = stringPreferencesKey("last_connected_mac")
        val KEY_LAST_CONNECTED_NAME = stringPreferencesKey("last_connected_name")
        val KEY_PREFERRED_ANC_LEVEL = stringPreferencesKey("preferred_anc_level")
        val KEY_AUTO_CONNECT_ENABLED = booleanPreferencesKey("auto_connect_enabled")
        val KEY_HOLD_CYCLE_MASK = intPreferencesKey("hold_cycle_mask")
        val KEY_CUSTOM_EQ_GAINS = stringPreferencesKey("custom_eq_gains")
        val KEY_CUSTOM_EQ_PROFILES = stringPreferencesKey("custom_eq_profiles")
        val KEY_SELECTED_CUSTOM_EQ_ID = intPreferencesKey("selected_custom_eq_id")
        val KEY_HI_RES_AUDIO = booleanPreferencesKey("hi_res_audio")
        val KEY_VOLUME_ENHANCER = booleanPreferencesKey("volume_enhancer")
        val KEY_WIND_NOISE_REDUCTION = booleanPreferencesKey("wind_noise_reduction")
        val KEY_BASS_WAVE_ENABLED = booleanPreferencesKey("bass_wave_enabled")
        val KEY_BASS_WAVE_LEVEL = intPreferencesKey("bass_wave_level")
        val KEY_VOICE_ENHANCE = booleanPreferencesKey("voice_enhance")
        val KEY_AUTO_ANSWER_CALLS = booleanPreferencesKey("auto_answer_calls")
        val KEY_FIND_PHONE = booleanPreferencesKey("find_phone")
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")

        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "buds_preferences")

        @Volatile
        private var instance: BudsPreferencesRepository? = null

        fun getInstance(context: Context): BudsPreferencesRepository {
            return instance ?: synchronized(this) {
                instance ?: BudsPreferencesRepository(context.applicationContext.dataStore, context.applicationContext).also { instance = it }
            }
        }
    }
}
