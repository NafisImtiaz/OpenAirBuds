package com.openairbuds.app.model

import androidx.compose.runtime.Immutable
import com.openairbuds.app.R

enum class EarbudBrand(val displayName: String) {
    REALME("realme"),
    ONEPLUS("OnePlus"),
    OPPO("OPPO"),
    GENERIC("Earbuds")
}

@Immutable
data class EqPresetOption(
    val slot: Byte,
    val displayName: String,
    val description: String = ""
)

@Immutable
data class DeviceCapabilities(
    val hasAnc: Boolean = true,
    val maxAncDb: Int = 50,
    val supportedAncLevels: List<AncLevel> = listOf(
        AncLevel.MILD,
        AncLevel.MODERATE,
        AncLevel.HIGH_50DB,
        AncLevel.SMART
    ),
    val supportedNoiseModes: List<NoiseControlMode> = listOf(
        NoiseControlMode.NOISE_CANCELLATION,
        NoiseControlMode.TRANSPARENCY,
        NoiseControlMode.OFF
    ),
    val hasInEarDetection: Boolean = true,
    val hasGameMode: Boolean = true,
    val hasDualConnection: Boolean = true,
    val hasBassWave: Boolean = true,
    val hasFitTest: Boolean = true,
    val supportedEqPresets: List<EqPresetOption> = listOf(
        EqPresetOption(0, "Balanced / Pure", "Natural acoustic balance"),
        EqPresetOption(1, "Deep Bass", "Enhanced sub-bass frequencies"),
        EqPresetOption(2, "Clear Vocals", "Accentuated human voices"),
        EqPresetOption(3, "Bass Boost+", "Maximum punchy low-end"),
        EqPresetOption(4, "Custom1", "Personalized 6-band equalizer")
    ),
    val hasCustomEq: Boolean = true,
    val hasHiResAudio: Boolean = true,
    val hiResCodec: String = "LHDC"
)

@Immutable
sealed class EarbudArtwork {
    data class BundledResource(
        val heroRes: Int,
        val caseRes: Int? = null,
        val bannerRes: Int? = null
    ) : EarbudArtwork()

    data object UniversalVector : EarbudArtwork()
}

@Immutable
data class DeviceProfile(
    val id: String,
    val displayName: String,
    val brand: EarbudBrand,
    val productIds: List<String> = emptyList(),
    val namePatterns: List<String> = emptyList(),
    val capabilities: DeviceCapabilities = DeviceCapabilities(),
    val artwork: EarbudArtwork = EarbudArtwork.UniversalVector
)
