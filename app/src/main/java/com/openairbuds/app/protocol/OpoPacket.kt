package com.openairbuds.app.protocol

import com.openairbuds.app.model.AncLevel
import com.openairbuds.app.model.BudsBattery
import com.openairbuds.app.model.BudsDevice
import com.openairbuds.app.model.BudsPlacement
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.DeviceProfileRegistry
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.model.GestureBinding
import com.openairbuds.app.model.NoiseControlMode

data class OpoFrame(
    val cmdId: Short,
    val sequence: Byte,
    val payload: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as OpoFrame
        if (cmdId != other.cmdId) return false
        if (sequence != other.sequence) return false
        if (!payload.contentEquals(other.payload)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = cmdId.toInt()
        result = 31 * result + sequence.toInt()
        result = 31 * result + payload.contentHashCode()
        return result
    }

    /**
     * Serializes this frame into real OPOv1 binary wire format:
     * aa <TotalLen LEB128> 00 00 <cmdId_lo> <cmdId_hi> <seq> <payLen_lo> <payLen_hi> <payload...>
     * TotalLen counts every byte after itself (slice 00 00, cmd 2B, seq 1B, payLen 2B, payload NB = 7 + payloadLen).
     */
    fun toByteArray(): ByteArray {
        val payloadLen = payload.size
        val totalLen = 7 + payloadLen

        // Encode LEB128 for totalLen
        val lenBytes = mutableListOf<Byte>()
        var tempLen = totalLen
        do {
            var b = tempLen and 0x7F
            tempLen = tempLen ushr 7
            if (tempLen != 0) b = b or 0x80
            lenBytes.add(b.toByte())
        } while (tempLen != 0)

        val out = ByteArray(1 + lenBytes.size + totalLen)
        var idx = 0
        out[idx++] = OpoConstants.SYNC_BYTE
        for (b in lenBytes) {
            out[idx++] = b
        }
        out[idx++] = 0x00 // slice 00
        out[idx++] = 0x00 // 00
        out[idx++] = (cmdId.toInt() and 0xFF).toByte()
        out[idx++] = ((cmdId.toInt() ushr 8) and 0xFF).toByte()
        out[idx++] = sequence
        out[idx++] = (payloadLen and 0xFF).toByte()
        out[idx++] = ((payloadLen ushr 8) and 0xFF).toByte()
        System.arraycopy(payload, 0, out, idx, payloadLen)

        return out
    }

    companion object {
        // --- Packet Builders ---

        /** Handshake / Hello: 0x0100 */
        fun buildHandshake(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_HANDSHAKE, sequence, ByteArray(0)).toByteArray()

        fun buildHello(sequence: Byte): ByteArray = buildHandshake(sequence)

        /** Product ID Query: 0x0103 */
        fun buildProductIdQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_PRODUCT_ID, sequence, ByteArray(0)).toByteArray()

        /** Broadcast Codes Query: 0x0200 */
        fun buildBroadcastCodesQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_BROADCAST, sequence, ByteArray(0)).toByteArray()

        /**
         * Subscribe notifications: 0x0205
         * Default subscribed codes: 01 (Battery), 02 (Wear), 03 (ANC), 04 (Fit Test)
         */
        fun buildSubscribe(
            sequence: Byte,
            codes: ByteArray = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        ): ByteArray {
            val payload = ByteArray(1 + codes.size)
            payload[0] = codes.size.toByte()
            System.arraycopy(codes, 0, payload, 1, codes.size)
            return OpoFrame(OpoConstants.CMD_REGISTER_NOTIFY, sequence, payload).toByteArray()
        }

        /**
         * Clock Sync Reply: 0x8500
         * Echoes sequence with payload: 00 <EpochSeconds u32 LE>
         */
        fun buildClockSyncReply(
            sequence: Byte,
            epochSeconds: Int = (System.currentTimeMillis() / 1000).toInt()
        ): ByteArray {
            val payload = byteArrayOf(
                0x00,
                (epochSeconds and 0xFF).toByte(),
                ((epochSeconds ushr 8) and 0xFF).toByte(),
                ((epochSeconds ushr 16) and 0xFF).toByte(),
                ((epochSeconds ushr 24) and 0xFF).toByte()
            )
            return OpoFrame(OpoConstants.CMD_CLOCK_SYNC_RESP, sequence, payload).toByteArray()
        }

        /**
         * Set Noise Mode / ANC depth: 0x0404 01 01 <val>
         */
        fun buildSetNoiseMode(
            mode: NoiseControlMode,
            level: AncLevel = AncLevel.HIGH_50DB,
            sequence: Byte
        ): ByteArray {
            val wireValue = if (mode == NoiseControlMode.NOISE_CANCELLATION) {
                level.wire
            } else {
                mode.wire
            }
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_ANC,
                sequence = sequence,
                payload = byteArrayOf(0x01, 0x01, wireValue)
            ).toByteArray()
        }

        /**
         * Set Hold Cycle mask: 0x0404 02 01 0B 00
         */
        fun buildHoldCycle(sequence: Byte, mask: Short = 0x000B.toShort()): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_ANC,
                sequence = sequence,
                payload = byteArrayOf(
                    OpoConstants.CMD_SUB_HOLD_CYCLE,
                    0x01,
                    (mask.toInt() and 0xFF).toByte(),
                    ((mask.toInt() ushr 8) and 0xFF).toByte()
                )
            ).toByteArray()
        }

        fun buildSetHoldCycle(mask: Byte, sequence: Byte): ByteArray =
            buildHoldCycle(sequence, (mask.toInt() and 0xFF).toShort())

        /**
         * Game Mode: 0x0403 06 <01|00>
         */
        fun buildSetGameMode(enabled: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_FEATURE,
                sequence = sequence,
                payload = byteArrayOf(OpoConstants.FEATURE_GAME_MODE, if (enabled) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * Wind Noise Reduction: 0x0403 0C <01|00>
         */
        fun buildSetWindNoiseReduction(enabled: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_FEATURE,
                sequence = sequence,
                payload = byteArrayOf(OpoConstants.FEATURE_WIND_NOISE_REDUCTION, if (enabled) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * Volume Enhancer: 0x0403 1E <01|00>
         */
        fun buildSetVolumeEnhancer(enabled: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_FEATURE,
                sequence = sequence,
                payload = byteArrayOf(OpoConstants.FEATURE_VOLUME_ENHANCER, if (enabled) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * Hi-Res Audio (LHDC / LDAC): 0x0403 18 <01|00>
         */
        fun buildSetHiResAudio(enabled: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_FEATURE,
                sequence = sequence,
                payload = byteArrayOf(OpoConstants.FEATURE_HI_RES_AUDIO, if (enabled) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * In-Ear Wear Detection: 0x0403 04 <01|00>
         */
        fun buildSetInEarDetection(enabled: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_FEATURE,
                sequence = sequence,
                payload = byteArrayOf(OpoConstants.FEATURE_IN_EAR_DETECTION, if (enabled) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * Enhance Voices in Transparency Mode: 0x0403 09 <01|00>
         */
        fun buildSetVoiceEnhance(enabled: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_FEATURE,
                sequence = sequence,
                payload = byteArrayOf(OpoConstants.FEATURE_ENHANCE_VOICE, if (enabled) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * Select Built-in EQ preset: 0x0406 <slot>
         */
        fun buildSetEqPreset(preset: EqPreset, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_EQ,
                sequence = sequence,
                payload = byteArrayOf(preset.slot)
            ).toByteArray()
        }

        /**
         * Custom 6-Band Graphic Equalizer (Realme Link protocol: opcode 0x0418)
         * Bands: 62Hz, 250Hz, 1kHz, 4kHz, 8kHz, 16kHz (-6dB to +6dB)
         * Payload (31 bytes with "Custom1"):
         *   [0] action: 0x02 (MODIFY) or 0x01 (ADD)
         *   [1] minValue: -6 (byte)
         *   [2] maxValue: +6 (byte)
         *   [3] eqId: 4 (byte, Custom slot 4 on earbuds)
         *   [4] nameLength: name.length
         *   [5..] UTF-8 name bytes
         *   [N] frequencyCount: 6 (byte)
         *   Interleaved for each frequency:
         *     2 bytes Little-Endian freq: (freq & 0xFF), ((freq ushr 8) & 0xFF)
         *     1 byte signed dB gain: gain (-6..6)
         */
        fun buildSetCustomEq(
            gains: List<Int>,
            sequence: Byte,
            action: Byte = 0x02,
            eqId: Byte = 0x04,
            name: String = "Custom1"
        ): ByteArray {
            val frequencies = intArrayOf(62, 250, 1000, 4000, 8000, 16000)
            val clampedGains = gains.take(6).map { it.coerceIn(-6, 6) }
            val finalGains = clampedGains + List(maxOf(0, 6 - clampedGains.size)) { 0 }
            val cleanName = name.ifEmpty { "Custom1" }
            val nameBytes = cleanName.toByteArray(Charsets.UTF_8)

            val payload = ByteArray(1 + 1 + 1 + 1 + 1 + nameBytes.size + 1 + (frequencies.size * 3))
            var idx = 0
            payload[idx++] = action
            payload[idx++] = (-6).toByte()
            payload[idx++] = 6.toByte()
            payload[idx++] = eqId
            payload[idx++] = nameBytes.size.toByte()
            for (b in nameBytes) {
                payload[idx++] = b
            }
            payload[idx++] = frequencies.size.toByte()
            for (i in frequencies.indices) {
                val f = frequencies[i]
                payload[idx++] = (f and 0xFF).toByte()
                payload[idx++] = ((f ushr 8) and 0xFF).toByte()
                payload[idx++] = finalGains[i].toByte()
            }

            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_CUSTOM_EQ,
                sequence = sequence,
                payload = payload
            ).toByteArray()
        }

        /**
         * BassWave toggle: 0x0403 1D <01|00>
         */
        fun buildSetBassWaveToggle(enabled: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_FEATURE,
                sequence = sequence,
                payload = byteArrayOf(OpoConstants.FEATURE_BASSWAVE, if (enabled) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * BassWave level: 0x041B FB 05 <level> (clamped -5 to +5)
         */
        fun buildSetBassWaveLevel(level: Int, sequence: Byte): ByteArray {
            val clamped = level.coerceIn(-5, 5)
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_BASSWAVE_LEVEL,
                sequence = sequence,
                payload = byteArrayOf(0xFB.toByte(), 0x05, clamped.toByte())
            ).toByteArray()
        }

        /**
         * Find My Buds: 0x0400 01/00
         */
        fun buildFindMyBuds(ring: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_FIND_BUDS,
                sequence = sequence,
                payload = byteArrayOf(if (ring) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * Gestures table write: 0x0401 <count> [dev, btn, act, fn]...
         */
        fun buildSetGestures(bindings: List<GestureBinding>, sequence: Byte): ByteArray {
            val payload = ByteArray(1 + bindings.size * 4)
            payload[0] = bindings.size.toByte()
            var idx = 1
            for (b in bindings) {
                payload[idx++] = b.device
                payload[idx++] = b.button
                payload[idx++] = b.action
                payload[idx++] = b.function
            }
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_KEY_FUNCTION,
                sequence = sequence,
                payload = payload
            ).toByteArray()
        }

        /**
         * Dual Connection toggle: 0x0403 11 <01|00>
         */
        fun buildSetDualConnection(enabled: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_SET_FEATURE,
                sequence = sequence,
                payload = byteArrayOf(OpoConstants.FEATURE_DUAL_CONNECTION, if (enabled) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * Fit Test: 0x0405 <01|00>
         */
        fun buildFitTest(start: Boolean, sequence: Byte): ByteArray {
            return OpoFrame(
                cmdId = OpoConstants.CMD_FIT_TEST,
                sequence = sequence,
                payload = byteArrayOf(if (start) 0x01 else 0x00)
            ).toByteArray()
        }

        /**
         * Firmware query: 0x0105
         */
        fun buildFirmwareQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_FIRMWARE, sequence, ByteArray(0)).toByteArray()

        /**
         * Status queries for initialization
         */
        fun buildAncQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_ANC, sequence, byteArrayOf(0x01, 0x01)).toByteArray()

        fun buildHoldCycleQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_ANC, sequence, byteArrayOf(0x02, 0x01)).toByteArray()

        fun buildBatteryQuery(sequence: Byte = 0xF0.toByte()): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_BATTERY, sequence, ByteArray(0)).toByteArray()

        fun buildWearQuery(sequence: Byte = 0xF2.toByte()): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_WEARING, sequence, ByteArray(0)).toByteArray()

        fun buildEqQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_EQ, sequence, ByteArray(0)).toByteArray()

        fun buildBassWaveLevelQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_BASSWAVE, sequence, ByteArray(0)).toByteArray()

        fun buildGesturesQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_KEY_FUNCTION, sequence, ByteArray(0)).toByteArray()

        fun buildDualDevicesQuery(sequence: Byte): ByteArray =
            OpoFrame(OpoConstants.CMD_QUERY_DEVICES, sequence, ByteArray(0)).toByteArray()

        fun buildFeaturesQuery(
            sequence: Byte,
            features: ByteArray = byteArrayOf(
                OpoConstants.FEATURE_GAME_MODE,
                OpoConstants.FEATURE_IN_EAR_DETECTION,
                OpoConstants.FEATURE_DUAL_CONNECTION,
                OpoConstants.FEATURE_HI_RES_AUDIO,
                OpoConstants.FEATURE_BASSWAVE,
                OpoConstants.FEATURE_WIND_NOISE_REDUCTION,
                OpoConstants.FEATURE_VOLUME_ENHANCER
            )
        ): ByteArray {
            val payload = ByteArray(1 + features.size)
            payload[0] = features.size.toByte()
            System.arraycopy(features, 0, payload, 1, features.size)
            return OpoFrame(OpoConstants.CMD_QUERY_FEATURES, sequence, payload).toByteArray()
        }

        /**
         * Pulls complete frames out of an accumulating buffer.
         * Handles arbitrary chunks, fragmented reads, and LEB128 multi-byte length.
         */
        fun drainFrames(buffer: MutableList<Byte>): List<OpoFrame> {
            val frames = mutableListOf<OpoFrame>()

            while (true) {
                // Drop any bytes before sync
                val start = buffer.indexOf(OpoConstants.SYNC_BYTE)
                if (start == -1) {
                    buffer.clear()
                    break
                }
                if (start > 0) {
                    buffer.subList(0, start).clear()
                }

                if (buffer.size < 2) break

                // Decode LEB128 total length
                var len = 0
                var shift = 0
                var offset = 1
                var completeLen = false
                while (offset < buffer.size) {
                    val b = buffer[offset].toInt() and 0xFF
                    len = len or ((b and 0x7F) shl shift)
                    offset++
                    if ((b and 0x80) == 0) {
                        completeLen = true
                        break
                    }
                    shift += 7
                    if (shift >= 28) {
                        break
                    }
                }
                if (!completeLen) {
                    if (shift >= 28) {
                        buffer.removeAt(0)
                        continue
                    }
                    break // Wait for full LEB128 length bytes
                }

                if (len < 7 || len > 2048) {
                    // Frame length is invalid/corrupt
                    buffer.removeAt(0)
                    continue
                }

                val total = offset + len
                if (buffer.size < total) break // Wait for remaining frame data

                // Unpack header: offset+0: slice, offset+1: 00, offset+2..3: cmdId (LE), offset+4: seq, offset+5..6: payLen (LE)
                val cmdLo = buffer[offset + 2].toInt() and 0xFF
                val cmdHi = buffer[offset + 3].toInt() and 0xFF
                val cmdId = ((cmdHi shl 8) or cmdLo).toShort()
                val seq = buffer[offset + 4]
                val payLenLo = buffer[offset + 5].toInt() and 0xFF
                val payLenHi = buffer[offset + 6].toInt() and 0xFF
                val payLen = (payLenHi shl 8) or payLenLo

                if (offset + 7 + payLen == total) {
                    val payload = ByteArray(payLen)
                    for (i in 0 until payLen) {
                        payload[i] = buffer[offset + 7 + i]
                    }
                    frames.add(OpoFrame(cmdId, seq, payload))
                    buffer.subList(0, total).clear()
                } else {
                    buffer.removeAt(0)
                }
            }

            return frames
        }

        /**
         * Interprets an incoming frame and returns the updated BudsState.
         */
        fun interpret(frame: OpoFrame, currentState: BudsState): BudsState {
            val payload = frame.payload
            var updatedState = currentState

            when (frame.cmdId) {
                // 1. Unsolicited Push 0x0204
                OpoConstants.CMD_ACTIVE_REPORT -> {
                    if (payload.isEmpty()) return currentState
                    val evtType = payload[0]

                    when (evtType) {
                        OpoConstants.EVT_BATTERY -> {
                            if (payload.size >= 2) {
                                val count = payload[1].toInt() and 0xFF
                                var left = currentState.battery.leftPercent
                                var right = currentState.battery.rightPercent
                                var case = currentState.battery.casePercent
                                var leftChg = currentState.battery.isLeftCharging
                                var rightChg = currentState.battery.isRightCharging
                                var caseChg = currentState.battery.isCaseCharging

                                var idx = 2
                                for (i in 0 until count) {
                                    if (idx + 1 >= payload.size) break
                                    val slot = payload[idx]
                                    val raw = payload[idx + 1].toInt() and 0xFF
                                    val level = raw and 0x7F
                                    val charging = (raw and 0x80) != 0
                                    val validLevel = if (level == 0) null else level
                                    val actualCharging = if (validLevel == null) false else charging

                                    when (slot) {
                                        OpoConstants.SLOT_LEFT -> {
                                            left = validLevel
                                            leftChg = actualCharging
                                        }
                                        OpoConstants.SLOT_RIGHT -> {
                                            right = validLevel
                                            rightChg = actualCharging
                                        }
                                        OpoConstants.SLOT_CASE -> {
                                            case = validLevel
                                            caseChg = actualCharging
                                        }
                                    }
                                    idx += 2
                                }

                                updatedState = updatedState.copy(
                                    battery = currentState.battery.copy(
                                        leftPercent = left,
                                        rightPercent = right,
                                        casePercent = case,
                                        isLeftCharging = leftChg,
                                        isRightCharging = rightChg,
                                        isCaseCharging = caseChg
                                    )
                                )
                            }
                        }

                        OpoConstants.EVT_WEAR -> {
                            if (payload.size >= 2) {
                                val count = payload[1].toInt() and 0xFF
                                var leftInEar = currentState.placement.isLeftInEar
                                var rightInEar = currentState.placement.isRightInEar
                                var leftInCase = currentState.placement.isLeftInCase
                                var rightInCase = currentState.placement.isRightInCase

                                var idx = 2
                                for (i in 0 until count) {
                                    if (idx + 1 >= payload.size) break
                                    val slot = payload[idx]
                                    val value = payload[idx + 1]
                                    val inEar = (value == OpoConstants.PLACEMENT_IN_EAR || value == OpoConstants.PLACEMENT_IN_USE)
                                    val inCase = (value == OpoConstants.PLACEMENT_IN_CASE)
                                    when (slot) {
                                        OpoConstants.SLOT_LEFT -> {
                                            leftInEar = inEar
                                            leftInCase = inCase
                                        }
                                        OpoConstants.SLOT_RIGHT -> {
                                            rightInEar = inEar
                                            rightInCase = inCase
                                        }
                                    }
                                    idx += 2
                                }

                                updatedState = updatedState.copy(
                                    placement = BudsPlacement(
                                        isLeftInEar = leftInEar,
                                        isRightInEar = rightInEar,
                                        isLeftInCase = leftInCase,
                                        isRightInCase = rightInCase
                                    )
                                )
                            }
                        }

                        OpoConstants.EVT_ANC -> {
                            // Format: 03 01 01 <val> [val_hi]
                            if (payload.size >= 4 && payload[1] == 0x01.toByte() && payload[2] == 0x01.toByte()) {
                                val wireVal = payload[3]
                                val mode = NoiseControlMode.fromWire(wireVal)
                                val level = AncLevel.fromWire(wireVal) ?: currentState.ancLevel
                                updatedState = updatedState.copy(
                                    noiseMode = mode,
                                    ancLevel = level
                                )
                            } else if (payload.size >= 4 && payload[1] == OpoConstants.CMD_SUB_HOLD_CYCLE && payload[2] == 0x01.toByte()) {
                                val mask = payload[3].toInt() and 0xFF
                                updatedState = updatedState.copy(holdCycleMask = mask)
                            }
                        }

                        OpoConstants.EVT_FIT_TEST -> {
                            var left: com.openairbuds.app.model.FitSealResult? = null
                            var right: com.openairbuds.app.model.FitSealResult? = null
                            var hasAnyResult = false
                            var idx = 1
                            while (idx + 1 < payload.size) {
                                val dev = payload[idx].toInt() and 0xFF
                                val res = payload[idx + 1].toInt() and 0xFF
                                val seal = com.openairbuds.app.model.FitSealResult.fromWire(res)
                                when (dev) {
                                    1 -> {
                                        left = seal
                                        hasAnyResult = true
                                    }
                                    2 -> {
                                        right = seal
                                        hasAnyResult = true
                                    }
                                }
                                idx += 2
                            }
                            if (hasAnyResult) {
                                updatedState = updatedState.copy(
                                    fitTestState = com.openairbuds.app.model.FitTestState.Completed(left, right)
                                )
                            }
                        }

                        OpoConstants.EVT_GAME_MODE -> {
                            if (payload.size >= 2) {
                                val state = (payload.last().toInt() and 0xFF) == 1
                                updatedState = updatedState.copy(isGameModeEnabled = state)
                            }
                        }

                        OpoConstants.EVT_DUAL_DEVICES -> {
                            val devices = parseDeviceList(payload, offset = 1)
                            updatedState = updatedState.copy(connectedDevices = devices)
                        }
                    }
                }

                // 2. Feature Push 0x0503
                OpoConstants.CMD_FEATURE_PUSH -> {
                    if (payload.isNotEmpty()) {
                        val count = payload[0].toInt() and 0xFF
                        var idx = 1
                        var newGameMode = currentState.isGameModeEnabled
                        var newWearDetect = currentState.isInEarDetectionEnabled
                        var newDualConn = currentState.isDualConnectionEnabled
                        var newHiResAudio = currentState.isHiResAudioEnabled
                        var newBassWave = currentState.isBassWaveEnabled
                        var newWindNoise = currentState.isWindNoiseReductionEnabled
                        var newVolumeEnhancer = currentState.isVolumeEnhancerEnabled

                        for (i in 0 until count) {
                            if (idx + 1 >= payload.size) break
                            val fid = payload[idx]
                            val fval = (payload[idx + 1].toInt() and 0xFF) == 1
                            when (fid) {
                                OpoConstants.FEATURE_GAME_MODE -> newGameMode = fval
                                OpoConstants.FEATURE_IN_EAR_DETECTION -> newWearDetect = fval
                                OpoConstants.FEATURE_DUAL_CONNECTION -> newDualConn = fval
                                OpoConstants.FEATURE_HI_RES_AUDIO -> newHiResAudio = fval
                                OpoConstants.FEATURE_BASSWAVE -> newBassWave = fval
                                OpoConstants.FEATURE_WIND_NOISE_REDUCTION -> newWindNoise = fval
                                OpoConstants.FEATURE_VOLUME_ENHANCER -> newVolumeEnhancer = fval
                            }
                            idx += 2
                        }

                        updatedState = updatedState.copy(
                            isGameModeEnabled = newGameMode,
                            isInEarDetectionEnabled = newWearDetect,
                            isDualConnectionEnabled = newDualConn,
                            isHiResAudioEnabled = newHiResAudio,
                            isBassWaveEnabled = newBassWave,
                            isWindNoiseReductionEnabled = newWindNoise,
                            isVolumeEnhancerEnabled = newVolumeEnhancer
                        )
                    }
                }

                // 3. EQ Changed 0x0504
                OpoConstants.CMD_EQ_CHANGED -> {
                    if (payload.isNotEmpty()) {
                        updatedState = updatedState.copy(eqPreset = EqPreset.fromSlot(payload[0]))
                    }
                }

                // 4. Query Responses 0x81xx
                OpoConstants.CMD_RESP_PRODUCT_ID -> {
                    val rawId = when {
                        payload.size >= 4 && payload[0] == 0.toByte() -> {
                            (payload[1].toInt() and 0xFF) or
                                ((payload[2].toInt() and 0xFF) shl 8) or
                                ((payload[3].toInt() and 0xFF) shl 16)
                        }
                        payload.size >= 3 -> {
                            (payload[0].toInt() and 0xFF) or
                                ((payload[1].toInt() and 0xFF) shl 8) or
                                ((payload[2].toInt() and 0xFF) shl 16)
                        }
                        else -> null
                    }
                    if (rawId != null) {
                        val folded = when (rawId) {
                            in 0x100100..0x100102 -> 0x060414
                            in 0x100200..0x100202 -> 0x060814
                            in 0x108100..0x108102 -> 0x068414
                            in 0x108200..0x108202 -> 0x068814
                            in 0x103100..0x103102, 0x103412 -> 0x063412
                            in 0x103200..0x103202, 0x103812 -> 0x063812
                            else -> rawId
                        }
                        val pidStr = String.format(java.util.Locale.US, "%06X", folded)
                        val resolvedProfile = DeviceProfileRegistry.resolve(pidStr, updatedState.deviceName)
                        updatedState = updatedState.copy(
                            productId = pidStr,
                            deviceProfile = resolvedProfile
                        )
                    }
                }

                OpoConstants.CMD_RESP_FIRMWARE -> {
                    if (payload.size >= 3 && payload[0] == 0.toByte()) {
                        try {
                            val str = String(payload, 2, payload.size - 2, Charsets.UTF_8).trimEnd('\u0000', ' ')
                            val parts = str.split(",")
                            val versions = mutableListOf<String>()
                            var i = 0
                            while (i + 2 < parts.size) {
                                val verType = parts[i + 1]
                                val ver = parts[i + 2]
                                if (verType == "2" && ver != "0" && ver.isNotBlank()) {
                                    versions.add(ver)
                                }
                                i += 3
                            }
                            val verStr = if (versions.any { it.contains(".") }) {
                                versions.firstOrNull { it != "0" && it.isNotBlank() } ?: str
                            } else if (versions.isNotEmpty()) {
                                versions.joinToString(".")
                            } else {
                                str
                            }
                            updatedState = updatedState.copy(firmwareVersion = verStr)
                        } catch (_: Exception) {}
                    }
                }

                OpoConstants.CMD_RESP_BATTERY -> {
                    if (payload.size >= 2 && payload[0] == 0.toByte()) {
                        val count = payload[1].toInt() and 0xFF
                        var left = currentState.battery.leftPercent
                        var right = currentState.battery.rightPercent
                        var case = currentState.battery.casePercent
                        var leftChg = currentState.battery.isLeftCharging
                        var rightChg = currentState.battery.isRightCharging
                        var caseChg = currentState.battery.isCaseCharging

                        var idx = 2
                        for (j in 0 until count) {
                            if (idx + 1 >= payload.size) break
                            val slot = payload[idx]
                            val raw = payload[idx + 1].toInt() and 0xFF
                            val level = raw and 0x7F
                            val charging = (raw and 0x80) != 0
                            val validLevel = if (level == 0) null else level
                            val actualCharging = if (validLevel == null) false else charging

                            when (slot) {
                                OpoConstants.SLOT_LEFT -> {
                                    left = validLevel
                                    leftChg = actualCharging
                                }
                                OpoConstants.SLOT_RIGHT -> {
                                    right = validLevel
                                    rightChg = actualCharging
                                }
                                OpoConstants.SLOT_CASE -> {
                                    case = validLevel
                                    caseChg = actualCharging
                                }
                            }
                            idx += 2
                        }

                        updatedState = updatedState.copy(
                            battery = currentState.battery.copy(
                                leftPercent = left,
                                rightPercent = right,
                                casePercent = case,
                                isLeftCharging = leftChg,
                                isRightCharging = rightChg,
                                isCaseCharging = caseChg
                            )
                        )
                    }
                }

                OpoConstants.CMD_RESP_KEY_FUNCTION -> {
                    if (payload.size >= 2 && payload[0] == 0.toByte()) {
                        val count = payload[1].toInt() and 0xFF
                        val list = mutableListOf<GestureBinding>()
                        var o = 2
                        for (j in 0 until count) {
                            if (o + 3 >= payload.size) break
                            list.add(GestureBinding(payload[o], payload[o + 1], payload[o + 2], payload[o + 3]))
                            o += 4
                        }
                        updatedState = updatedState.copy(gestureBindings = list)
                    }
                }

                OpoConstants.CMD_RESP_WEARING -> {
                    if (payload.size >= 2 && payload[0] == 0.toByte()) {
                        val count = payload[1].toInt() and 0xFF
                        var leftInEar = currentState.placement.isLeftInEar
                        var rightInEar = currentState.placement.isRightInEar
                        var leftInCase = currentState.placement.isLeftInCase
                        var rightInCase = currentState.placement.isRightInCase
                        var idx = 2
                        for (j in 0 until count) {
                            if (idx + 1 >= payload.size) break
                            val slot = payload[idx]
                            val state = payload[idx + 1]
                            val inEar = (state == OpoConstants.PLACEMENT_IN_EAR || state == OpoConstants.PLACEMENT_IN_USE)
                            val inCase = (state == OpoConstants.PLACEMENT_IN_CASE)
                            when (slot) {
                                OpoConstants.SLOT_LEFT -> {
                                    leftInEar = inEar
                                    leftInCase = inCase
                                }
                                OpoConstants.SLOT_RIGHT -> {
                                    rightInEar = inEar
                                    rightInCase = inCase
                                }
                            }
                            idx += 2
                        }
                        updatedState = updatedState.copy(
                            placement = BudsPlacement(
                                isLeftInEar = leftInEar,
                                isRightInEar = rightInEar,
                                isLeftInCase = leftInCase,
                                isRightInCase = rightInCase
                            )
                        )
                    }
                }

                OpoConstants.CMD_RESP_ANC -> {
                    val wireVal = if (payload.size >= 3 && payload[0] == 0x01.toByte() && payload[1] == 0x01.toByte()) {
                        payload[2]
                    } else if (payload.size >= 4 && payload[0] == 0.toByte() && payload[1] == 0x01.toByte() && payload[2] == 0x01.toByte()) {
                        payload[3]
                    } else null

                    if (wireVal != null) {
                        val mode = NoiseControlMode.fromWire(wireVal)
                        val level = AncLevel.fromWire(wireVal) ?: currentState.ancLevel
                        val currentProfile = updatedState.deviceProfile
                        val updatedProfile = if (!currentProfile.capabilities.hasAnc) {
                            currentProfile.copy(capabilities = currentProfile.capabilities.copy(hasAnc = true))
                        } else currentProfile
                        updatedState = updatedState.copy(
                            noiseMode = mode,
                            ancLevel = level,
                            deviceProfile = updatedProfile
                        )
                    }

                    val holdCycleVal = if (payload.size >= 3 && payload[0] == OpoConstants.CMD_SUB_HOLD_CYCLE && payload[1] == 0x01.toByte()) {
                        payload[2].toInt() and 0xFF
                    } else if (payload.size >= 4 && payload[0] == 0.toByte() && payload[1] == OpoConstants.CMD_SUB_HOLD_CYCLE && payload[2] == 0x01.toByte()) {
                        payload[3].toInt() and 0xFF
                    } else null

                    if (holdCycleVal != null) {
                        updatedState = updatedState.copy(holdCycleMask = holdCycleVal)
                    }
                }

                OpoConstants.CMD_RESP_FEATURES -> {
                    if (payload.size >= 2 && payload[0] == 0.toByte()) {
                        val count = payload[1].toInt() and 0xFF
                        var o = 2
                        var newGameMode = currentState.isGameModeEnabled
                        var newWearDetect = currentState.isInEarDetectionEnabled
                        var newDualConn = currentState.isDualConnectionEnabled
                        var newHiResAudio = currentState.isHiResAudioEnabled
                        var newBassWave = currentState.isBassWaveEnabled
                        var newWindNoise = currentState.isWindNoiseReductionEnabled
                        var newVolumeEnhancer = currentState.isVolumeEnhancerEnabled
                        for (j in 0 until count) {
                            if (o + 1 >= payload.size) break
                            val fid = payload[o]
                            val fval = (payload[o + 1].toInt() and 0xFF) == 1
                            when (fid) {
                                OpoConstants.FEATURE_GAME_MODE -> newGameMode = fval
                                OpoConstants.FEATURE_IN_EAR_DETECTION -> newWearDetect = fval
                                OpoConstants.FEATURE_DUAL_CONNECTION -> newDualConn = fval
                                OpoConstants.FEATURE_HI_RES_AUDIO -> newHiResAudio = fval
                                OpoConstants.FEATURE_BASSWAVE -> newBassWave = fval
                                OpoConstants.FEATURE_WIND_NOISE_REDUCTION -> newWindNoise = fval
                                OpoConstants.FEATURE_VOLUME_ENHANCER -> newVolumeEnhancer = fval
                            }
                            o += 2
                        }
                        updatedState = updatedState.copy(
                            isGameModeEnabled = newGameMode,
                            isInEarDetectionEnabled = newWearDetect,
                            isDualConnectionEnabled = newDualConn,
                            isHiResAudioEnabled = newHiResAudio,
                            isBassWaveEnabled = newBassWave,
                            isWindNoiseReductionEnabled = newWindNoise,
                            isVolumeEnhancerEnabled = newVolumeEnhancer
                        )
                    }
                }

                OpoConstants.CMD_RESP_EQ -> {
                    if (payload.isNotEmpty()) {
                        val slot = if (payload.size >= 2 && payload[0] == 0.toByte()) payload[1] else payload[0]
                        updatedState = updatedState.copy(eqPreset = EqPreset.fromSlot(slot))
                    }
                }

                OpoConstants.CMD_RESP_DEVICES -> {
                    if (payload.isNotEmpty()) {
                        val offset = if (payload[0] == 0.toByte() && payload.size >= 2) 1 else 0
                        val devices = parseDeviceList(payload, offset = offset)
                        updatedState = updatedState.copy(connectedDevices = devices)
                    }
                }

                OpoConstants.CMD_RESP_BASSWAVE -> {
                    val lvl = if (payload.size >= 4 && payload[0] == 0.toByte()) {
                        payload[3].toInt().coerceIn(-5, 5)
                    } else if (payload.size >= 3 && payload[0] == 0xFB.toByte()) {
                        payload[2].toInt().coerceIn(-5, 5)
                    } else null
                    if (lvl != null) {
                        val currentProfile = updatedState.deviceProfile
                        val updatedProfile = if (!currentProfile.capabilities.hasBassWave) {
                            currentProfile.copy(capabilities = currentProfile.capabilities.copy(hasBassWave = true))
                        } else currentProfile
                        updatedState = updatedState.copy(
                            bassWaveLevel = lvl,
                            deviceProfile = updatedProfile
                        )
                    }
                }
            }

            return updatedState
        }

        private fun parseDeviceList(payload: ByteArray, offset: Int): List<BudsDevice> {
            val list = mutableListOf<BudsDevice>()
            if (offset >= payload.size) return list
            val count = payload[offset].toInt() and 0xFF
            var idx = offset + 1
            for (i in 0 until count) {
                if (idx + 6 > payload.size) break
                // MAC 6 bytes reversed on wire
                val macStr = buildString(17) {
                    for (bIdx in 5 downTo 0) {
                        val b = payload[idx + bIdx].toInt() and 0xFF
                        val hi = (b ushr 4) and 0x0F
                        val lo = b and 0x0F
                        append((if (hi < 10) '0' + hi else 'A' + (hi - 10)))
                        append((if (lo < 10) '0' + lo else 'A' + (lo - 10)))
                        if (bIdx > 0) append(':')
                    }
                }
                idx += 6

                var isConnected = false
                if (idx < payload.size) {
                    isConnected = (payload[idx].toInt() and 0xFF) == 1
                    idx++
                }
                if (idx < payload.size) {
                    // flags
                    idx++
                }
                var name = "Device ${i + 1}"
                if (idx < payload.size) {
                    val nameLen = payload[idx].toInt() and 0xFF
                    idx++
                    if (nameLen > 0 && idx + nameLen <= payload.size) {
                        name = String(payload, idx, nameLen, Charsets.UTF_8).trimEnd('\u0000', ' ')
                        idx += nameLen
                    } else if (nameLen > 0) {
                        break
                    }
                }
                list.add(BudsDevice(mac = macStr, name = name, isConnected = isConnected))
            }
            return list
        }
    }
}
