package com.openairbuds.app.model

import android.content.Context
import com.openairbuds.app.BudsApplication
import com.openairbuds.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

data class CatalogModel(
    val id: String,
    val name: String,
    val json: JSONObject
)

object ModelCatalog {

    private val initLock = Any()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var cache: List<CatalogModel>? = null

    fun init(jsonString: String) {
        val list = parseJson(jsonString)
        synchronized(initLock) {
            cache = list
        }
    }

    fun init(inputStream: InputStream) {
        val content = inputStream.bufferedReader().use { it.readText() }
        init(content)
    }

    fun init(context: Context) {
        synchronized(initLock) {
            if (cache != null) return
            runCatching {
                context.assets.open("models.json").use { input ->
                    val content = input.bufferedReader().use { it.readText() }
                    cache = parseJson(content)
                }
            }
        }
    }

    fun initAsync(context: Context) {
        val appContext = context.applicationContext
        ioScope.launch {
            init(appContext)
        }
    }

    private fun parseJson(content: String): List<CatalogModel> {
        return runCatching {
            val root = JSONObject(content)
            val whiteList: JSONArray = root.optJSONArray("whiteList") ?: JSONArray()
            val list = ArrayList<CatalogModel>(whiteList.length())
            for (i in 0 until whiteList.length()) {
                val obj = whiteList.getJSONObject(i)
                val id = obj.optString("id")
                val name = obj.optString("name")
                if (id.isNotBlank() && name.isNotBlank()) {
                    list.add(CatalogModel(id = id, name = name, json = obj))
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    fun all(): List<CatalogModel> {
        cache?.let { return it }
        runCatching {
            val app = BudsApplication.instance
            init(app)
        }
        return cache ?: emptyList()
    }

    /**
     * Folds Bluetooth LE scan / vendor colour ranges into their canonical base model ID.
     * Derived from BBK OPOv1 protocol documentation (PROTOCOL.md §6).
     */
    fun normalise(id: Int): Int = when (id) {
        in 0x100100..0x100102 -> 0x060414   // OnePlus Buds / Bullets Wireless Z2
        in 0x100200..0x100202 -> 0x060814   // OnePlus Buds Z
        in 0x108100..0x108102 -> 0x068414   // OnePlus Buds
        in 0x108200..0x108202 -> 0x068814   // OnePlus Buds Z
        else -> id
    }

    fun normalizeProductId(productId: String?): String? {
        if (productId.isNullOrBlank()) return null
        return runCatching {
            val intId = productId.trim().toInt(16)
            val normalized = normalise(intId)
            "%06X".format(normalized)
        }.getOrDefault(productId.trim().uppercase())
    }

    /**
     * Resolves an earbud model using the official 4-tier match:
     * 1. Exact Name + Product ID (strongest correlation)
     * 2. Bluetooth Name match (essential when different models share the same product ID, e.g. OPPO Enco Buds2 vs realme Buds Q2s)
     * 3. Product ID match (handles earbuds renamed by user in Bluetooth settings)
     * 4. Normalized alphanumeric name (case-insensitive, spaces/punctuation stripped, e.g. "realme Buds Air 6" vs "realme Buds Air6")
     */
    fun find(productId: String?, deviceName: String?): CatalogModel? {
        val models = all()
        if (models.isEmpty()) return null

        val normPid = normalizeProductId(productId)
        val trimmedName = deviceName?.trim()

        val byId = { m: CatalogModel -> normPid != null && m.id.equals(normPid, ignoreCase = true) }
        val byExactName = { m: CatalogModel -> trimmedName != null && m.name.equals(trimmedName, ignoreCase = true) }

        // Tier 1: Exact Name + Product ID
        if (normPid != null && trimmedName != null) {
            val tier1 = models.firstOrNull { byExactName(it) && byId(it) }
            if (tier1 != null) return tier1
        }

        // Tier 2: Exact Name
        if (trimmedName != null) {
            val tier2 = models.firstOrNull(byExactName)
            if (tier2 != null) return tier2
        }

        // Tier 3: Product ID
        if (normPid != null) {
            val tier3 = models.firstOrNull(byId)
            if (tier3 != null) return tier3
        }

        // Tier 4: Squashed alphanumeric name
        if (trimmedName != null) {
            val targetSquashed = squash(trimmedName)
            val exactSquashed = models.firstOrNull { squash(it.name) == targetSquashed }
            if (exactSquashed != null) return exactSquashed

            // Substring squashed fallback (e.g. "OnePlus Buds 3 - Paired" -> "OnePlus Buds 3")
            val substringMatch = models.firstOrNull {
                val mSquashed = squash(it.name)
                mSquashed.length >= 6 && targetSquashed.contains(mSquashed)
            }
            if (substringMatch != null) return substringMatch
        }

        return null
    }

    private fun squash(s: String): String = s.lowercase().filter { it.isLetterOrDigit() }

    /**
     * Converts a raw catalog model into an OpenAirBuds typed DeviceProfile with full dynamic capabilities.
     */
    fun toDeviceProfile(model: CatalogModel): DeviceProfile {
        val json = model.json
        val name = model.name

        val brand = when {
            name.startsWith("OnePlus", ignoreCase = true) -> EarbudBrand.ONEPLUS
            name.startsWith("OPPO", ignoreCase = true) || name.startsWith("Enco", ignoreCase = true) -> EarbudBrand.OPPO
            name.startsWith("realme", ignoreCase = true) -> EarbudBrand.REALME
            else -> EarbudBrand.GENERIC
        }

        val confirmedSet = mutableSetOf<String>()
        val confirmedArray = json.optJSONArray("confirmed")
        if (confirmedArray != null) {
            for (i in 0 until confirmedArray.length()) {
                confirmedSet.add(confirmedArray.optString(i).lowercase())
            }
        }

        // ANC capabilities
        val ancModesArray = json.optJSONArray("ancModes")
        val hasAnc = ancModesArray != null && ancModesArray.length() > 0

        val supportedAncLevels = if (!hasAnc) {
            emptyList()
        } else {
            val foundLevels = mutableSetOf<AncLevel>()
            for (i in 0 until ancModesArray!!.length()) {
                val entry = ancModesArray.optJSONObject(i) ?: continue
                if (entry.optInt("mode") == 5) {
                    val variants = entry.optJSONArray("variants")
                    if (variants != null) {
                        for (j in 0 until variants.length()) {
                            when (variants.optJSONObject(j)?.optInt("mode")) {
                                3 -> foundLevels.add(AncLevel.MILD)
                                8 -> foundLevels.add(AncLevel.MODERATE)
                                4 -> foundLevels.add(AncLevel.HIGH_50DB)
                                7 -> foundLevels.add(AncLevel.SMART)
                            }
                        }
                    }
                }
            }
            listOf(AncLevel.MILD, AncLevel.MODERATE, AncLevel.HIGH_50DB, AncLevel.SMART)
                .filter { it in foundLevels }
        }

        val maxAncDb = when {
            !hasAnc -> 0
            name.contains("Air 6", ignoreCase = true) || name.contains("Air6", ignoreCase = true) ||
            name.contains("Air 5", ignoreCase = true) || name.contains("Air5", ignoreCase = true) ||
            name.contains("Buds 3", ignoreCase = true) || name.contains("Buds Pro", ignoreCase = true) -> 50
            name.contains("T310", ignoreCase = true) -> 46
            name.contains("Air 3", ignoreCase = true) || name.contains("Air3", ignoreCase = true) -> 42
            name.contains("T300", ignoreCase = true) || name.contains("Wireless 3", ignoreCase = true) -> 30
            else -> 45
        }

        // Dual connection: explicitly disallowed for base Air 6 and Air 5
        val isBaseAir6 = model.id in listOf("063412", "060414", "060814", "068414", "068814", "063812", "063414", "063814", "103412", "103812", "100100", "100101", "100102", "100200", "100201", "100202") ||
                (squash(name).contains("air6") && !squash(name).contains("pro"))
        val isBaseAir5 = model.id in listOf("060412", "060812") ||
                (squash(name).contains("air5") && !squash(name).contains("pro"))
        val hasDualConnection = when {
            isBaseAir6 || isBaseAir5 -> false
            confirmedSet.contains("dual") -> true
            json.optInt("dual", 0) == 1 -> true
            name.contains("Pro", ignoreCase = true) -> true
            else -> false
        }

        val hasInEarDetection = when {
            name.contains("T100", ignoreCase = true) || name.contains("T110", ignoreCase = true) ||
            name.contains("T300", ignoreCase = true) || name.contains("Wireless", ignoreCase = true) -> false
            confirmedSet.contains("wear") -> true
            json.optInt("wear", 1) == 1 -> true
            else -> true
        }

        val hasGameMode = confirmedSet.contains("game") || json.has("gameSoundFx") || json.has("gameConfirm") || true

        val hasBassWave = json.optInt("bassEngine", 0) == 1 ||
                json.has("bassBands") ||
                name.contains("Air 6", ignoreCase = true) ||
                name.contains("Air6", ignoreCase = true) ||
                name.contains("Air 5", ignoreCase = true) ||
                name.contains("T310", ignoreCase = true) ||
                name.contains("T300", ignoreCase = true)

        val hasCustomEq = json.optInt("customEq", 0) == 1 ||
                name.contains("Pro", ignoreCase = true) ||
                name.contains("Air 6", ignoreCase = true) ||
                name.contains("Air6", ignoreCase = true)

        val eqKind = json.optInt("eqKind", 1)
        val supportedEqPresets = when (eqKind) {
            4 -> listOf(
                EqPresetOption(0, "Balanced", "Crisp acoustic balance"),
                EqPresetOption(1, "Bold", "Warm vibrant sound"),
                EqPresetOption(2, "Serenade", "Detailed mid-range focus"),
                EqPresetOption(3, "Bass", "Punchy energetic beats"),
                EqPresetOption(4, "Custom1", "Personalized 6-band equalizer")
            )
            2 -> listOf(
                EqPresetOption(0, "Balanced", "Studio clean profile"),
                EqPresetOption(1, "Bass Boost", "Rich low end"),
                EqPresetOption(2, "Clear Voice", "Bright vocal clarity"),
                EqPresetOption(3, "Dynaudio Classic", "Signature hi-fi tuning"),
                EqPresetOption(4, "Custom1", "Personalized 6-band equalizer")
            )
            else -> listOf(
                EqPresetOption(0, "Balanced / Pure", "Natural acoustic balance"),
                EqPresetOption(1, "Deep Bass", "Enhanced sub-bass frequencies"),
                EqPresetOption(2, "Clear Vocals", "Accentuated human voices"),
                EqPresetOption(3, "Bass Boost+", "Maximum punchy low-end"),
                EqPresetOption(4, "Custom1", "Personalized 6-band equalizer")
            )
        }

        val codecsArray = json.optJSONArray("codecs")
        val codecsList = mutableListOf<String>()
        if (codecsArray != null) {
            for (i in 0 until codecsArray.length()) {
                codecsList.add(codecsArray.optString(i).lowercase())
            }
        }
        val statusQueryStr = json.optString("statusQuery", "")
        val hasHiRes = codecsList.any { it.contains("lhdc") || it.contains("ldac") } ||
                statusQueryStr.split(" ").contains("18") ||
                confirmedSet.contains("hires") ||
                name.contains("Air 6", ignoreCase = true) ||
                name.contains("Air6", ignoreCase = true) ||
                name.contains("Air 7", ignoreCase = true) ||
                name.contains("Air7", ignoreCase = true)

        val hiResCodec = when {
            codecsList.any { it.contains("ldac") } -> "LDAC"
            codecsList.any { it.contains("lhdc") } -> "LHDC"
            name.contains("Air 6 Pro", ignoreCase = true) || name.contains("Air6 Pro", ignoreCase = true) -> "LDAC"
            name.contains("Air 5 Pro", ignoreCase = true) -> "LDAC"
            else -> "LHDC"
        }

        val artwork = EarbudArtwork.BundledResource(
            heroRes = R.drawable.ic_earbuds_default_hero,
            caseRes = R.drawable.ic_case_default_thumb,
            bannerRes = R.drawable.ic_earbuds_default_hero
        )

        return DeviceProfile(
            id = "bbk_${model.id}",
            displayName = model.name,
            brand = brand,
            productIds = listOf(model.id),
            namePatterns = listOf(model.name),
            capabilities = DeviceCapabilities(
                hasAnc = hasAnc,
                maxAncDb = maxAncDb,
                supportedAncLevels = supportedAncLevels,
                hasInEarDetection = hasInEarDetection,
                hasGameMode = hasGameMode,
                hasDualConnection = hasDualConnection,
                hasBassWave = hasBassWave,
                hasFitTest = hasAnc,
                supportedEqPresets = supportedEqPresets,
                hasCustomEq = hasCustomEq,
                hasHiResAudio = hasHiRes,
                hiResCodec = hiResCodec
            ),
            artwork = artwork
        )
    }
}
