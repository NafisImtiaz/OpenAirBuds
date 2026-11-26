package com.openairbuds.app.model

import com.openairbuds.app.R

object DeviceProfileRegistry {

    val DEFAULT_ARTWORK = EarbudArtwork.BundledResource(
        heroRes = R.drawable.ic_kenkashi_hero,
        caseRes = R.drawable.ic_kenkashi_case,
        bannerRes = R.drawable.ic_kenkashi_hero
    )

    val REALME_BUDS_AIR_6 = DeviceProfile(
        id = "realme_buds_air_6",
        displayName = "realme Buds Air 6",
        brand = EarbudBrand.REALME,
        productIds = listOf("063412", "060414", "060814", "068414", "068814", "063812", "063414", "063814", "103412", "103812", "100100", "100101", "100102", "100200", "100201", "100202"),
        namePatterns = listOf("realme Buds Air 6", "Buds Air 6", "realme Buds Air6", "Buds Air6", "Air 6", "Air6"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 50,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = false, // Base Air 6 does not have multipoint; reserved for Air 6 Pro
            hasBassWave = true,
            hasFitTest = true,
            hasCustomEq = true,
            supportedEqPresets = listOf(
                EqPresetOption(0, "Balanced / Pure", "Natural acoustic balance"),
                EqPresetOption(1, "Deep Bass", "Enhanced sub-bass frequencies"),
                EqPresetOption(2, "Clear Vocals", "Accentuated human voices"),
                EqPresetOption(3, "Bass Boost+", "Maximum punchy low-end"),
                EqPresetOption(4, "Custom1", "Personalized 6-band equalizer")
            )
        ),
        artwork = DEFAULT_ARTWORK
    )

    val REALME_BUDS_AIR_6_PRO = DeviceProfile(
        id = "realme_buds_air_6_pro",
        displayName = "realme Buds Air 6 Pro",
        brand = EarbudBrand.REALME,
        productIds = listOf("060415", "060815", "063415", "063815", "100105", "100205"),
        namePatterns = listOf("realme Buds Air 6 Pro", "Buds Air 6 Pro", "Air 6 Pro", "realme Buds Air6 Pro", "Buds Air6 Pro"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 50,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = true,
            hasFitTest = true,
            hasCustomEq = true,
            hasHiResAudio = true,
            hiResCodec = "LDAC"
        ),
        artwork = DEFAULT_ARTWORK
    )

    val REALME_BUDS_AIR_7 = DeviceProfile(
        id = "realme_buds_air_7",
        displayName = "realme Buds Air 7",
        brand = EarbudBrand.REALME,
        productIds = listOf("063416", "063816", "100107", "100207"),
        namePatterns = listOf("realme Buds Air 7", "Buds Air 7", "realme Buds Air7", "Buds Air7", "Air 7", "Air7"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 50,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = true,
            hasFitTest = true,
            hasCustomEq = true,
            hasHiResAudio = true,
            hiResCodec = "LDAC"
        ),
        artwork = DEFAULT_ARTWORK
    )

    val REALME_BUDS_AIR_7_PRO = DeviceProfile(
        id = "realme_buds_air_7_pro",
        displayName = "realme Buds Air 7 Pro",
        brand = EarbudBrand.REALME,
        productIds = listOf("060417", "060817", "063417", "063817"),
        namePatterns = listOf("realme Buds Air 7 Pro", "Buds Air 7 Pro", "Air 7 Pro", "realme Buds Air7 Pro", "Buds Air7 Pro"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 52,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = true,
            hasFitTest = true,
            hasCustomEq = true,
            hasHiResAudio = true,
            hiResCodec = "LDAC"
        ),
        artwork = DEFAULT_ARTWORK
    )

    val REALME_BUDS_AIR_5 = DeviceProfile(
        id = "realme_buds_air_5",
        displayName = "realme Buds Air 5",
        brand = EarbudBrand.REALME,
        productIds = listOf("060412", "060812"),
        namePatterns = listOf("realme Buds Air 5", "Buds Air 5"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 50,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = false, // Base Air 5 does not have multipoint; reserved for Air 5 Pro
            hasBassWave = true,
            hasFitTest = true
        )
    )

    val REALME_BUDS_AIR_5_PRO = DeviceProfile(
        id = "realme_buds_air_5_pro",
        displayName = "realme Buds Air 5 Pro",
        brand = EarbudBrand.REALME,
        productIds = listOf("060413", "060813"),
        namePatterns = listOf("realme Buds Air 5 Pro", "Buds Air 5 Pro", "Air 5 Pro"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 50,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = true,
            hasFitTest = true,
            hasCustomEq = true
        )
    )

    val REALME_BUDS_AIR_3 = DeviceProfile(
        id = "realme_buds_air_3",
        displayName = "realme Buds Air 3",
        brand = EarbudBrand.REALME,
        namePatterns = listOf("realme Buds Air 3", "Buds Air 3"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 42,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = false,
            hasFitTest = true
        )
    )

    val REALME_BUDS_T310 = DeviceProfile(
        id = "realme_buds_t310",
        displayName = "realme Buds T310",
        brand = EarbudBrand.REALME,
        namePatterns = listOf("realme Buds T310", "Buds T310", "T310"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 46,
            hasInEarDetection = false,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = true,
            hasFitTest = false
        )
    )

    val REALME_BUDS_T300 = DeviceProfile(
        id = "realme_buds_t300",
        displayName = "realme Buds T300",
        brand = EarbudBrand.REALME,
        namePatterns = listOf("realme Buds T300", "Buds T300", "T300"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 30,
            hasInEarDetection = false,
            hasGameMode = true,
            hasDualConnection = false,
            hasBassWave = true,
            hasFitTest = false
        )
    )

    val REALME_BUDS_T100 = DeviceProfile(
        id = "realme_buds_t100",
        displayName = "realme Buds T100 / T110",
        brand = EarbudBrand.REALME,
        namePatterns = listOf("realme Buds T100", "Buds T100", "realme Buds T110", "Buds T110", "T100", "T110"),
        capabilities = DeviceCapabilities(
            hasAnc = false,
            supportedAncLevels = emptyList(),
            supportedNoiseModes = emptyList(),
            hasInEarDetection = false,
            hasGameMode = true,
            hasDualConnection = false,
            hasBassWave = false,
            hasFitTest = false,
            supportedEqPresets = listOf(
                EqPresetOption(0, "Balanced", "Clear, even audio spectrum"),
                EqPresetOption(1, "Bass Boost+", "Heavy punchy low end"),
                EqPresetOption(2, "Bright", "Accented high frequencies")
            )
        )
    )

    val REALME_BUDS_WIRELESS_3 = DeviceProfile(
        id = "realme_buds_wireless_3",
        displayName = "realme Buds Wireless 3",
        brand = EarbudBrand.REALME,
        namePatterns = listOf("realme Buds Wireless 3", "Buds Wireless 3", "Wireless 3"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 30,
            hasInEarDetection = false,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = true,
            hasFitTest = false
        )
    )

    val ONEPLUS_BUDS_PRO = DeviceProfile(
        id = "oneplus_buds_pro",
        displayName = "OnePlus Buds Pro",
        brand = EarbudBrand.ONEPLUS,
        namePatterns = listOf("OnePlus Buds Pro", "Buds Pro 2", "Buds Pro 3"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 50,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = true,
            hasFitTest = true,
            hasCustomEq = true
        )
    )

    val OPPO_ENCO_AIR_PRO = DeviceProfile(
        id = "oppo_enco_air_pro",
        displayName = "OPPO Enco Earbuds",
        brand = EarbudBrand.OPPO,
        namePatterns = listOf("OPPO Enco", "Enco Air", "Enco X", "Enco X2"),
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 49,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = true,
            hasBassWave = false,
            hasFitTest = true
        )
    )

    val DEFAULT_REALME = DeviceProfile(
        id = "default_realme",
        displayName = "realme Earbuds",
        brand = EarbudBrand.REALME,
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 50,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = false,
            hasBassWave = true,
            hasFitTest = true
        ),
        artwork = DEFAULT_ARTWORK
    )

    val GENERIC_PROFILE = DeviceProfile(
        id = "generic_earbuds",
        displayName = "Wireless Earbuds",
        brand = EarbudBrand.GENERIC,
        capabilities = DeviceCapabilities(
            hasAnc = true,
            maxAncDb = 50,
            hasInEarDetection = true,
            hasGameMode = true,
            hasDualConnection = false,
            hasBassWave = true,
            hasFitTest = true
        )
    )

    val ALL_PROFILES = listOf(
        REALME_BUDS_AIR_7_PRO,
        REALME_BUDS_AIR_7,
        REALME_BUDS_AIR_6_PRO,
        REALME_BUDS_AIR_6,
        REALME_BUDS_AIR_5_PRO,
        REALME_BUDS_AIR_5,
        REALME_BUDS_AIR_3,
        REALME_BUDS_T310,
        REALME_BUDS_T300,
        REALME_BUDS_T100,
        REALME_BUDS_WIRELESS_3,
        ONEPLUS_BUDS_PRO,
        OPPO_ENCO_AIR_PRO
    )

    /**
     * Resolves a DeviceProfile using 4-tier lookup backed by the universal 140+ BBK ModelCatalog:
     * 1. Exact hardware Product ID (0x0103 / 0x8103) in bespoke primary profiles.
     * 2. Universal ModelCatalog lookup (Name+PID, Name, PID, squashed name).
     * 3. Bluetooth friendly device name pattern matching in bespoke profiles.
     * 4. Brand heuristic fallback.
     */
    fun resolve(productId: String?, deviceName: String?): DeviceProfile {
        // Step 1: Check primary bespoke profiles by Product ID
        if (!productId.isNullOrBlank()) {
            val normalizedPid = productId.trim().uppercase()
            val matchedByPid = ALL_PROFILES.firstOrNull { profile ->
                profile.productIds.any { it.equals(normalizedPid, ignoreCase = true) }
            }
            if (matchedByPid != null) {
                return matchedByPid
            }
        }

        // Step 2: Universal 143-model ModelCatalog resolution
        val catalogModel = ModelCatalog.find(productId, deviceName)
        if (catalogModel != null) {
            val matchedBespoke = ALL_PROFILES.firstOrNull { profile ->
                profile.productIds.any { it.equals(catalogModel.id, ignoreCase = true) }
            }
            if (matchedBespoke != null) {
                return matchedBespoke
            }
            return ModelCatalog.toDeviceProfile(catalogModel)
        }

        // Step 3: Bluetooth Name Match in bespoke profiles
        if (!deviceName.isNullOrBlank()) {
            val trimmedName = deviceName.trim()
            val matchedByName = ALL_PROFILES.firstOrNull { profile ->
                profile.namePatterns.any { pattern ->
                    trimmedName.contains(pattern, ignoreCase = true)
                }
            }
            if (matchedByName != null) {
                return matchedByName
            }

            // Step 4: Brand Heuristic Fallback
            return when {
                trimmedName.contains("Air 7 Pro", ignoreCase = true) || trimmedName.contains("Air7 Pro", ignoreCase = true) ->
                    REALME_BUDS_AIR_7_PRO.copy(displayName = trimmedName)
                trimmedName.contains("Air 7", ignoreCase = true) || trimmedName.contains("Air7", ignoreCase = true) ->
                    REALME_BUDS_AIR_7.copy(displayName = trimmedName)
                trimmedName.contains("Air 6 Pro", ignoreCase = true) || trimmedName.contains("Air6 Pro", ignoreCase = true) ->
                    REALME_BUDS_AIR_6_PRO.copy(displayName = trimmedName)
                trimmedName.contains("Air 6", ignoreCase = true) || trimmedName.contains("Air6", ignoreCase = true) ->
                    REALME_BUDS_AIR_6.copy(displayName = trimmedName)
                trimmedName.contains("OnePlus", ignoreCase = true) -> ONEPLUS_BUDS_PRO.copy(displayName = trimmedName)
                trimmedName.contains("OPPO", ignoreCase = true) || trimmedName.contains("Enco", ignoreCase = true) ->
                    OPPO_ENCO_AIR_PRO.copy(displayName = trimmedName)
                trimmedName.contains("realme", ignoreCase = true) || trimmedName.contains("Buds Air", ignoreCase = true) ->
                    DEFAULT_REALME.copy(displayName = trimmedName)
                else -> GENERIC_PROFILE.copy(displayName = trimmedName)
            }
        }

        return DEFAULT_REALME
    }

    /**
     * Resolves the default universal EarbudArtwork for all earbuds.
     */
    fun resolveArtwork(profile: DeviceProfile? = null, deviceName: String? = null): EarbudArtwork {
        return DEFAULT_ARTWORK
    }
}

