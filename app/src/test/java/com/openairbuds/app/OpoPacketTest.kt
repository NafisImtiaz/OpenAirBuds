package com.openairbuds.app

import com.openairbuds.app.model.AncLevel
import com.openairbuds.app.model.BudsState
import com.openairbuds.app.model.EqPreset
import com.openairbuds.app.model.GestureBinding
import com.openairbuds.app.model.NoiseControlMode
import com.openairbuds.app.protocol.OpoConstants
import com.openairbuds.app.protocol.OpoFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpoPacketTest {

    private fun hexToBytes(hex: String): ByteArray {
        val clean = hex.replace(" ", "")
        val result = ByteArray(clean.length / 2)
        for (i in result.indices) {
            val byteStr = clean.substring(i * 2, i * 2 + 2)
            result[i] = byteStr.toInt(16).toByte()
        }
        return result
    }

    @Test
    fun testHelloPacketFraming() {
        val helloBytes = OpoFrame.buildHello(sequence = 1)
        // Expected: aa 07 00 00 00 01 01 00 00
        assertEquals(9, helloBytes.size)
        assertEquals(OpoConstants.SYNC_BYTE, helloBytes[0])
        assertEquals(7.toByte(), helloBytes[1])
        assertEquals(0.toByte(), helloBytes[2])
        assertEquals(0.toByte(), helloBytes[3])
        assertEquals(0x00.toByte(), helloBytes[4]) // cmdId low byte (0x0100)
        assertEquals(0x01.toByte(), helloBytes[5]) // cmdId high byte
        assertEquals(1.toByte(), helloBytes[6])    // seq
        assertEquals(0.toByte(), helloBytes[7])    // payLen low
        assertEquals(0.toByte(), helloBytes[8])    // payLen high
    }

    @Test
    fun testProductIdFraming() {
        val bytes = OpoFrame.buildProductIdQuery(sequence = 2)
        // Expected: aa 07 00 00 03 01 02 00 00
        assertEquals(9, bytes.size)
        assertEquals(0x03.toByte(), bytes[4]) // cmd 0x0103
        assertEquals(0x01.toByte(), bytes[5])
        assertEquals(2.toByte(), bytes[6])
    }

    @Test
    fun testBroadcastCodesQueryFraming() {
        val bytes = OpoFrame.buildBroadcastCodesQuery(sequence = 3)
        // Expected: aa 07 00 00 00 02 03 00 00
        assertEquals(9, bytes.size)
        assertEquals(0x00.toByte(), bytes[4]) // cmd 0x0200
        assertEquals(0x02.toByte(), bytes[5])
    }

    @Test
    fun testSubscribeFraming() {
        val bytes = OpoFrame.buildSubscribe(sequence = 4, byteArrayOf(0x01, 0x02, 0x03, 0x04))
        // Expected: aa 0c 00 00 05 02 04 05 00 04 01 02 03 04
        assertEquals(14, bytes.size)
        assertEquals(12.toByte(), bytes[1])   // totalLen = 7 + 5 = 12
        assertEquals(0x05.toByte(), bytes[4]) // cmd 0x0205
        assertEquals(0x02.toByte(), bytes[5])
        assertEquals(4.toByte(), bytes[6])    // seq
        assertEquals(5.toByte(), bytes[7])    // payLen = 5
        assertEquals(4.toByte(), bytes[9])    // count = 4
        assertEquals(1.toByte(), bytes[10])   // 01 Battery
        assertEquals(2.toByte(), bytes[11])   // 02 Wear
        assertEquals(3.toByte(), bytes[12])   // 03 ANC
        assertEquals(4.toByte(), bytes[13])   // 04 Fit Test
    }

    @Test
    fun testClockSyncReplyEncoding() {
        val epochSeconds = 1700000000 // 0x6553F100
        val bytes = OpoFrame.buildClockSyncReply(sequence = 5, epochSeconds = epochSeconds)
        assertEquals(14, bytes.size)
        assertEquals(0x00.toByte(), bytes[4]) // cmd 0x8500
        assertEquals(0x85.toByte(), bytes[5])
        assertEquals(5.toByte(), bytes[6])    // seq
        assertEquals(5.toByte(), bytes[7])    // payLen = 5
        assertEquals(0.toByte(), bytes[9])    // status 00
        assertEquals(((epochSeconds) and 0xFF).toByte(), bytes[10]) // LE epoch bytes
        assertEquals(((epochSeconds ushr 8) and 0xFF).toByte(), bytes[11])
        assertEquals(((epochSeconds ushr 16) and 0xFF).toByte(), bytes[12])
        assertEquals(((epochSeconds ushr 24) and 0xFF).toByte(), bytes[13])
    }

    @Test
    fun testSetNoiseModePacketEncoding() {
        // ANC Max (50dB) -> wire 0x08
        val ancBytes = OpoFrame.buildSetNoiseMode(
            mode = NoiseControlMode.NOISE_CANCELLATION,
            level = AncLevel.HIGH_50DB,
            sequence = 3
        )
        // Expected: aa 0a 00 00 04 04 03 03 00 01 01 08
        assertEquals(12, ancBytes.size)
        assertEquals(OpoConstants.SYNC_BYTE, ancBytes[0])
        assertEquals(10.toByte(), ancBytes[1]) // totalLen = 10
        assertEquals(0x04.toByte(), ancBytes[4]) // cmd 0x0404 lo
        assertEquals(0x04.toByte(), ancBytes[5]) // cmd 0x0404 hi
        assertEquals(0x03.toByte(), ancBytes[6]) // seq
        assertEquals(0x03.toByte(), ancBytes[7]) // payloadLen lo
        assertEquals(0x00.toByte(), ancBytes[8]) // payloadLen hi
        assertEquals(0x01.toByte(), ancBytes[9])
        assertEquals(0x01.toByte(), ancBytes[10])
        assertEquals(0x08.toByte(), ancBytes[11]) // wire value for Max ANC
    }

    @Test
    fun testSetOffAndTransparencyEncoding() {
        val offBytes = OpoFrame.buildSetNoiseMode(NoiseControlMode.OFF, sequence = 4)
        assertEquals(0x01.toByte(), offBytes[11]) // wire value for Off

        val transBytes = OpoFrame.buildSetNoiseMode(NoiseControlMode.TRANSPARENCY, sequence = 5)
        assertEquals(0x02.toByte(), transBytes[11]) // wire value for Transparency
    }

    @Test
    fun testHoldCycleEncoding() {
        val bytes = OpoFrame.buildHoldCycle(sequence = 6, mask = 0x000B.toShort())
        // Expected: aa 0b 00 00 04 04 06 04 00 02 01 0B 00
        assertEquals(13, bytes.size)
        assertEquals(0x04.toByte(), bytes[4]) // cmd 0x0404
        assertEquals(0x04.toByte(), bytes[5])
        assertEquals(6.toByte(), bytes[6])    // seq
        assertEquals(0x02.toByte(), bytes[9])
        assertEquals(0x01.toByte(), bytes[10])
        assertEquals(0x0B.toByte(), bytes[11])
        assertEquals(0x00.toByte(), bytes[12])
    }

    @Test
    fun testGameModeEncoding() {
        val onBytes = OpoFrame.buildSetGameMode(enabled = true, sequence = 7)
        // Expected: aa 09 00 00 03 04 07 02 00 06 01
        assertEquals(11, onBytes.size)
        assertEquals(0x03.toByte(), onBytes[4]) // cmd 0x0403
        assertEquals(0x04.toByte(), onBytes[5])
        assertEquals(0x06.toByte(), onBytes[9]) // FEATURE_GAME_MODE
        assertEquals(0x01.toByte(), onBytes[10])

        val offBytes = OpoFrame.buildSetGameMode(enabled = false, sequence = 8)
        assertEquals(0x00.toByte(), offBytes[10])
    }

    @Test
    fun testInEarDetectionEncoding() {
        val onBytes = OpoFrame.buildSetInEarDetection(enabled = true, sequence = 9)
        assertEquals(0x03.toByte(), onBytes[4]) // cmd 0x0403
        assertEquals(0x04.toByte(), onBytes[5])
        assertEquals(0x04.toByte(), onBytes[9]) // FEATURE_IN_EAR_DETECTION
        assertEquals(0x01.toByte(), onBytes[10])
    }

    @Test
    fun testEqPresetEncoding() {
        // Test all 4 realme presets
        val balanced = OpoFrame.buildSetEqPreset(EqPreset.BALANCED, sequence = 10)
        assertEquals(0x06.toByte(), balanced[4]) // cmd 0x0406
        assertEquals(0x04.toByte(), balanced[5])
        assertEquals(0.toByte(), balanced[9])    // slot 0

        val deepBass = OpoFrame.buildSetEqPreset(EqPreset.DEEP_BASS, sequence = 11)
        assertEquals(1.toByte(), deepBass[9])    // slot 1

        val clearVocals = OpoFrame.buildSetEqPreset(EqPreset.CLEAR_VOCALS, sequence = 12)
        assertEquals(2.toByte(), clearVocals[9]) // slot 2

        val bassBoost = OpoFrame.buildSetEqPreset(EqPreset.BASS_BOOST_PLUS, sequence = 13)
        assertEquals(3.toByte(), bassBoost[9])   // slot 3
    }

    @Test
    fun testBassWaveEncoding() {
        val toggleBytes = OpoFrame.buildSetBassWaveToggle(enabled = true, sequence = 14)
        assertEquals(0x03.toByte(), toggleBytes[4]) // cmd 0x0403
        assertEquals(0x04.toByte(), toggleBytes[5])
        assertEquals(0x1D.toByte(), toggleBytes[9]) // FEATURE_BASSWAVE
        assertEquals(0x01.toByte(), toggleBytes[10])

        // Level +3: FB 05 03
        val levelBytes = OpoFrame.buildSetBassWaveLevel(level = 3, sequence = 15)
        assertEquals(0x1B.toByte(), levelBytes[4]) // cmd 0x041B
        assertEquals(0x04.toByte(), levelBytes[5])
        assertEquals(0xFB.toByte(), levelBytes[9])
        assertEquals(0x05.toByte(), levelBytes[10])
        assertEquals(0x03.toByte(), levelBytes[11])
    }

    @Test
    fun testHiResAudioEncoding() {
        val enableBytes = OpoFrame.buildSetHiResAudio(enabled = true, sequence = 19)
        assertEquals(0x03.toByte(), enableBytes[4]) // cmd 0x0403
        assertEquals(0x04.toByte(), enableBytes[5])
        assertEquals(OpoConstants.FEATURE_HI_RES_AUDIO, enableBytes[9]) // 0x18
        assertEquals(0x01.toByte(), enableBytes[10])

        val disableBytes = OpoFrame.buildSetHiResAudio(enabled = false, sequence = 20)
        assertEquals(0x03.toByte(), disableBytes[4])
        assertEquals(0x04.toByte(), disableBytes[5])
        assertEquals(OpoConstants.FEATURE_HI_RES_AUDIO, disableBytes[9])
        assertEquals(0x00.toByte(), disableBytes[10])
    }

    @Test
    fun testFeaturesQueryContainsHiResAudio() {
        val queryBytes = OpoFrame.buildFeaturesQuery(sequence = 21)
        assertEquals(0x0D.toByte(), queryBytes[4]) // cmd 0x010D
        assertEquals(0x01.toByte(), queryBytes[5])
        val count = queryBytes[9].toInt() and 0xFF
        val features = queryBytes.sliceArray(10 until 10 + count)
        assertTrue("Features query must contain FEATURE_HI_RES_AUDIO (0x18)", features.contains(OpoConstants.FEATURE_HI_RES_AUDIO))
    }

    @Test
    fun testFindMyBudsEncoding() {
        val startBytes = OpoFrame.buildFindMyBuds(ring = true, sequence = 16)
        assertEquals(0x00.toByte(), startBytes[4]) // cmd 0x0400
        assertEquals(0x04.toByte(), startBytes[5])
        assertEquals(0x01.toByte(), startBytes[9])

        val stopBytes = OpoFrame.buildFindMyBuds(ring = false, sequence = 17)
        assertEquals(0x00.toByte(), stopBytes[9])
    }

    @Test
    fun testGesturesEncoding() {
        val bindings = listOf(
            GestureBinding(device = 1, button = 1, action = 1, function = 1),
            GestureBinding(device = 2, button = 2, action = 1, function = 2)
        )
        val bytes = OpoFrame.buildSetGestures(bindings, sequence = 18)
        assertEquals(0x01.toByte(), bytes[4]) // cmd 0x0401
        assertEquals(0x04.toByte(), bytes[5])
        assertEquals(2.toByte(), bytes[9])    // count = 2
        assertEquals(1.toByte(), bytes[10])   // dev = 1
        assertEquals(1.toByte(), bytes[11])   // btn = 1
        assertEquals(1.toByte(), bytes[12])   // act = 1
        assertEquals(1.toByte(), bytes[13])   // fn = 1
        assertEquals(2.toByte(), bytes[14])   // dev = 2
        assertEquals(2.toByte(), bytes[15])   // btn = 2
    }

    @Test
    fun testDualConnectionEncoding() {
        val bytes = OpoFrame.buildSetDualConnection(enabled = true, sequence = 19)
        assertEquals(0x03.toByte(), bytes[4]) // cmd 0x0403
        assertEquals(0x04.toByte(), bytes[5])
        assertEquals(0x11.toByte(), bytes[9]) // FEATURE_DUAL_CONNECTION
        assertEquals(0x01.toByte(), bytes[10])
    }

    @Test
    fun testFitTestEncoding() {
        val bytes = OpoFrame.buildFitTest(start = true, sequence = 20)
        assertEquals(0x05.toByte(), bytes[4]) // cmd 0x0405
        assertEquals(0x04.toByte(), bytes[5])
        assertEquals(0x01.toByte(), bytes[9])
    }

    @Test
    fun testFirmwareQueryEncoding() {
        val bytes = OpoFrame.buildFirmwareQuery(sequence = 21)
        assertEquals(0x05.toByte(), bytes[4]) // cmd 0x0105
        assertEquals(0x01.toByte(), bytes[5])
    }

    @Test
    fun testRealHardwareBatteryFrameDecoding() {
        // Captured from realme hardware: Both buds 100%, Case 80% (non-charging)
        // aa 0f 00 00 04 02 0c 08 00 01 03 01 64 02 64 03 50
        val raw = hexToBytes("aa 0f 00 00 04 02 0c 08 00 01 03 01 64 02 64 03 50")
        val buffer = raw.toMutableList()
        val frames = OpoFrame.drainFrames(buffer)

        assertEquals(1, frames.size)
        assertTrue(buffer.isEmpty())

        val frame = frames[0]
        assertEquals(OpoConstants.CMD_ACTIVE_REPORT, frame.cmdId)

        val updatedState = OpoFrame.interpret(frame, BudsState())
        assertEquals(100, updatedState.battery.leftPercent)
        assertEquals(100, updatedState.battery.rightPercent)
        assertEquals(80, updatedState.battery.casePercent)
        assertFalse(updatedState.battery.isLeftCharging)
        assertFalse(updatedState.battery.isRightCharging)
        assertFalse(updatedState.battery.isCaseCharging)
        assertTrue(updatedState.battery.isConnected)
    }

    @Test
    fun testBatteryChargingBitDecoding() {
        // Left bud: 100% charging (raw = 0xE4, which is 0x80 or 100)
        // Right bud: 90% not charging (raw = 0x5A)
        // Case: 75% charging (raw = 0xCB, which is 0x80 or 75)
        // aa 0f 00 00 04 02 0c 08 00 01 03 01 E4 02 5A 03 CB
        val raw = hexToBytes("aa 0f 00 00 04 02 0c 08 00 01 03 01 E4 02 5A 03 CB")
        val buffer = raw.toMutableList()
        val frames = OpoFrame.drainFrames(buffer)
        assertEquals(1, frames.size)

        val state = OpoFrame.interpret(frames[0], BudsState())
        assertEquals(100, state.battery.leftPercent)
        assertTrue(state.battery.isLeftCharging)

        assertEquals(90, state.battery.rightPercent)
        assertFalse(state.battery.isRightCharging)

        assertEquals(75, state.battery.casePercent)
        assertTrue(state.battery.isCaseCharging)
    }

    @Test
    fun testRealHardwareNoiseModeFrameDecoding() {
        // Captured from realme hardware: Max ANC report (0x08)
        // aa 0b 00 00 04 02 9b 04 00 03 01 01 08
        val raw = hexToBytes("aa 0b 00 00 04 02 9b 04 00 03 01 01 08")
        val buffer = raw.toMutableList()
        val frames = OpoFrame.drainFrames(buffer)

        assertEquals(1, frames.size)
        val frame = frames[0]
        val updatedState = OpoFrame.interpret(frame, BudsState())
        assertEquals(NoiseControlMode.NOISE_CANCELLATION, updatedState.noiseMode)
        assertEquals(AncLevel.HIGH_50DB, updatedState.ancLevel)
    }

    @Test
    fun testInboundPushWearEventDecoding() {
        // 0x0204 with type 0x02: Left bud in ear (0x03), Right bud in case (0x00)
        // aa 0d 00 00 04 02 ff 06 00 02 02 01 03 02 00
        val raw = hexToBytes("aa 0d 00 00 04 02 ff 06 00 02 02 01 03 02 00")
        val frames = OpoFrame.drainFrames(raw.toMutableList())
        assertEquals(1, frames.size)

        val state = OpoFrame.interpret(frames[0], BudsState())
        assertEquals(true, state.placement.isLeftInUse)
        assertEquals(false, state.placement.isRightInUse)
    }

    @Test
    fun testInboundPushGameModeAndFitTest() {
        // Game Mode Push: 0x0204 subtype 0x05, state 01
        val gmRaw = hexToBytes("aa 09 00 00 04 02 ff 02 00 05 01")
        val gmFrames = OpoFrame.drainFrames(gmRaw.toMutableList())
        val gmState = OpoFrame.interpret(gmFrames[0], BudsState())
        assertTrue(gmState.isGameModeEnabled)

        // Fit Test Push: 0x0204 subtype 0x04, dev 1 res 01 (Good seal per QuickBuds)
        val ftRaw = hexToBytes("aa 0a 00 00 04 02 ff 03 00 04 01 01")
        val ftFrames = OpoFrame.drainFrames(ftRaw.toMutableList())
        val ftState = OpoFrame.interpret(ftFrames[0], BudsState())
        assertTrue(ftState.fitTestState is com.openairbuds.app.model.FitTestState.Completed)
        val completed = ftState.fitTestState as com.openairbuds.app.model.FitTestState.Completed
        assertEquals(com.openairbuds.app.model.FitSealResult.GOOD, completed.left)
    }

    @Test
    fun testFeaturePush0503() {
        // 0x0503: 4 pairs -> GameMode=1, InEar=0, Dual=1, BassWave=1
        // aa 10 00 00 03 05 ff 09 00 04 06 01 04 00 11 01 1D 01
        val raw = hexToBytes("aa 10 00 00 03 05 ff 09 00 04 06 01 04 00 11 01 1D 01")
        val frames = OpoFrame.drainFrames(raw.toMutableList())
        assertEquals(1, frames.size)

        val state = OpoFrame.interpret(frames[0], BudsState())
        assertTrue(state.isGameModeEnabled)
        assertFalse(state.isInEarDetectionEnabled)
        assertTrue(state.isDualConnectionEnabled)
        assertTrue(state.isBassWaveEnabled)
    }

    @Test
    fun testHiResAudioFeaturePush0503() {
        // 0x0503: 1 pair -> HiResAudio=1 (0x18 0x01)
        // aa 0a 00 00 03 05 ff 03 00 01 18 01
        val rawEnable = hexToBytes("aa 0a 00 00 03 05 ff 03 00 01 18 01")
        val framesEnable = OpoFrame.drainFrames(rawEnable.toMutableList())
        val stateEnabled = OpoFrame.interpret(framesEnable[0], BudsState(isHiResAudioEnabled = false))
        assertTrue(stateEnabled.isHiResAudioEnabled)

        // 0x0503: 1 pair -> HiResAudio=0 (0x18 0x00)
        val rawDisable = hexToBytes("aa 0a 00 00 03 05 ff 03 00 01 18 00")
        val framesDisable = OpoFrame.drainFrames(rawDisable.toMutableList())
        val stateDisabled = OpoFrame.interpret(framesDisable[0], BudsState(isHiResAudioEnabled = true))
        assertFalse(stateDisabled.isHiResAudioEnabled)
    }

    @Test
    fun testHiResAudioResponseFeatures810D() {
        // 0x810D: status 0x00, count 1, feature 0x18 (Hi-Res Audio), value 0x01 (Enabled)
        val respEnable = OpoFrame(
            cmdId = OpoConstants.CMD_RESP_FEATURES,
            sequence = 1,
            payload = byteArrayOf(0x00, 0x01, OpoConstants.FEATURE_HI_RES_AUDIO, 0x01)
        )
        val stateEnabled = OpoFrame.interpret(respEnable, BudsState(isHiResAudioEnabled = false))
        assertTrue("810D response should enable Hi-Res Audio", stateEnabled.isHiResAudioEnabled)

        // 0x810D: status 0x00, count 1, feature 0x18 (Hi-Res Audio), value 0x00 (Disabled)
        val respDisable = OpoFrame(
            cmdId = OpoConstants.CMD_RESP_FEATURES,
            sequence = 2,
            payload = byteArrayOf(0x00, 0x01, OpoConstants.FEATURE_HI_RES_AUDIO, 0x00)
        )
        val stateDisabled = OpoFrame.interpret(respDisable, BudsState(isHiResAudioEnabled = true))
        assertFalse("810D response should disable Hi-Res Audio", stateDisabled.isHiResAudioEnabled)
    }

    @Test
    fun testEqChangedPush0504() {
        // 0x0504: slot 2 (Clear Vocals)
        val raw = hexToBytes("aa 08 00 00 04 05 ff 01 00 02")
        val frames = OpoFrame.drainFrames(raw.toMutableList())
        val state = OpoFrame.interpret(frames[0], BudsState())
        assertEquals(EqPreset.CLEAR_VOCALS, state.eqPreset)
    }

    @Test
    fun testResponseFirmwareAndProductId() {
        // Product ID 0x8103: 00 12 34 06 -> 063412
        val prodRaw = hexToBytes("aa 0b 00 00 03 81 02 04 00 00 12 34 06")
        val prodFrames = OpoFrame.drainFrames(prodRaw.toMutableList())
        val prodState = OpoFrame.interpret(prodFrames[0], BudsState())
        assertEquals("063412", prodState.productId)
        assertEquals("realme Buds Air 6", prodState.deviceProfile.displayName)
        assertTrue(prodState.deviceProfile.artwork is com.openairbuds.app.model.EarbudArtwork.BundledResource)


        // Firmware 0x8105: 00 03 1,2,105,2,2,105,3,2,101 -> 105.105.101
        val fwStr = "1,2,105,2,2,105,3,2,101"
        val fwPayload = byteArrayOf(0x00, 0x03) + fwStr.toByteArray(Charsets.UTF_8)
        val fwFrame = OpoFrame(OpoConstants.CMD_RESP_FIRMWARE, sequence = 1, payload = fwPayload)
        val fwBytes = fwFrame.toByteArray()
        val fwFrames = OpoFrame.drainFrames(fwBytes.toMutableList())
        val fwState = OpoFrame.interpret(fwFrames[0], BudsState())
        assertEquals("105.105.101", fwState.firmwareVersion)
    }

    @Test
    fun testGestureBindingNamesAndActions() {
        val binding1 = GestureBinding(device = 1, button = 1, action = 1, function = OpoConstants.GESTURE_FN_PLAY_PAUSE)
        assertEquals("Left Bud", binding1.deviceName)
        assertEquals("Single Tap", binding1.gestureName)
        assertEquals("Play / Pause", binding1.actionName)

        val binding2 = GestureBinding(device = 2, button = 1, action = 2, function = OpoConstants.GESTURE_FN_NEXT_TRACK)
        assertEquals("Right Bud", binding2.deviceName)
        assertEquals("Double Tap", binding2.gestureName)
        assertEquals("Next Track", binding2.actionName)

        val binding3 = GestureBinding(device = 1, button = 1, action = 3, function = OpoConstants.GESTURE_FN_CYCLE_ANC)
        assertEquals("Triple Tap", binding3.gestureName)
        assertEquals("Noise Control (ANC)", binding3.actionName)

        val binding4 = GestureBinding(device = 2, button = 1, action = 4, function = OpoConstants.GESTURE_FN_GAME_MODE)
        assertEquals("Touch & Hold", binding4.gestureName)
        assertEquals("Game Mode", binding4.actionName)

        val bindingNone = GestureBinding(device = 1, button = 1, action = 2, function = OpoConstants.GESTURE_FN_NONE)
        assertEquals("None", bindingNone.actionName)

        val bindingPrev = GestureBinding(device = 1, button = 1, action = 3, function = OpoConstants.GESTURE_FN_PREVIOUS_TRACK)
        assertEquals("Previous Track", bindingPrev.actionName)

        val bindingVA = GestureBinding(device = 1, button = 1, action = 4, function = OpoConstants.GESTURE_FN_VOICE_ASSISTANT)
        assertEquals("Voice Assistant", bindingVA.actionName)

        val bindingVolUp = GestureBinding(device = 1, button = 1, action = 2, function = OpoConstants.GESTURE_FN_VOLUME_UP)
        assertEquals("Volume Up", bindingVolUp.actionName)

        val bindingVolDown = GestureBinding(device = 1, button = 1, action = 2, function = OpoConstants.GESTURE_FN_VOLUME_DOWN)
        assertEquals("Volume Down", bindingVolDown.actionName)
    }

    @Test
    fun testMultiBudFitTestDecoding() {
        // Fit Test Push: 0x0204 subtype 0x04, dev 1 res 01 (Good), dev 2 res 06 (Poor) per QuickBuds
        val raw = hexToBytes("aa 0c 00 00 04 02 ff 05 00 04 01 01 02 06")
        val frames = OpoFrame.drainFrames(raw.toMutableList())
        assertEquals(1, frames.size)

        val state = OpoFrame.interpret(frames[0], BudsState())
        assertTrue(state.fitTestState is com.openairbuds.app.model.FitTestState.Completed)
        val completed = state.fitTestState as com.openairbuds.app.model.FitTestState.Completed
        assertEquals(com.openairbuds.app.model.FitSealResult.GOOD, completed.left)
        assertEquals(com.openairbuds.app.model.FitSealResult.POOR, completed.right)
    }

    @Test
    fun testFitTestAverageSeal() {
        // Fit Test Push: 0x0204 subtype 0x04, dev 1 res 00 (Average seal) per QuickBuds
        val raw = hexToBytes("aa 0a 00 00 04 02 ff 03 00 04 01 00")
        val frames = OpoFrame.drainFrames(raw.toMutableList())
        assertEquals(1, frames.size)

        val state = OpoFrame.interpret(frames[0], BudsState())
        assertTrue(state.fitTestState is com.openairbuds.app.model.FitTestState.Completed)
        val completed = state.fitTestState as com.openairbuds.app.model.FitTestState.Completed
        assertEquals(com.openairbuds.app.model.FitSealResult.AVERAGE, completed.left)
    }

    @Test
    fun testFitTestFraming() {
        val startBytes = OpoFrame.buildFitTest(start = true, sequence = 5)
        // Expected: aa 08 00 00 05 04 05 01 00 01
        assertEquals(10, startBytes.size)
        assertEquals(0x05.toByte(), startBytes[4]) // cmdId low byte (0x0405)
        assertEquals(0x04.toByte(), startBytes[5]) // cmdId high byte
        assertEquals(5.toByte(), startBytes[6])    // seq
        assertEquals(1.toByte(), startBytes[7])    // payLen low
        assertEquals(0.toByte(), startBytes[8])    // payLen high
        assertEquals(1.toByte(), startBytes[9])    // start = 1

        val stopBytes = OpoFrame.buildFitTest(start = false, sequence = 6)
        assertEquals(10, stopBytes.size)
        assertEquals(6.toByte(), stopBytes[6])
        assertEquals(0.toByte(), stopBytes[9])     // stop = 0
    }

    @Test
    fun testCorruptLeb128DoesNotLoop() {
        // Corrupt stream where 5 consecutive bytes have bit 0x80 set
        val corrupt = mutableListOf<Byte>(
            OpoConstants.SYNC_BYTE,
            0x81.toByte(), 0x82.toByte(), 0x83.toByte(), 0x84.toByte(), 0x85.toByte(),
            OpoConstants.SYNC_BYTE, 0x07.toByte(), 0x00, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00
        )
        val frames = OpoFrame.drainFrames(corrupt)
        // Should recover and parse the valid Hello frame after the corrupt sync
        assertEquals(1, frames.size)
        assertEquals(OpoConstants.CMD_HANDSHAKE, frames[0].cmdId)
    }

    @Test
    fun testBassWaveResponseWithoutStatusByte() {
        val raw = byteArrayOf(0xFB.toByte(), 0x05, 0x03)
        val frame = OpoFrame(OpoConstants.CMD_RESP_BASSWAVE, sequence = 1, payload = raw)
        val state = OpoFrame.interpret(frame, BudsState())
        assertEquals(3, state.bassWaveLevel)
    }

    @Test
    fun testBatteryLevelZeroNullsCharging() {
        // Case closed / unreadable reports level 0 with bit 0x80 set (raw = 0x80)
        // Should produce level = null and isCharging = false (not stuck charging with null percent)
        val raw = hexToBytes("aa 0d 00 00 04 02 0c 06 00 01 02 01 64 03 80")
        val frames = OpoFrame.drainFrames(raw.toMutableList())
        val state = OpoFrame.interpret(frames[0], BudsState())

        assertEquals(100, state.battery.leftPercent)
        assertFalse(state.battery.isLeftCharging)
        assertEquals(null, state.battery.casePercent)
        assertFalse(state.battery.isCaseCharging)
    }

    @Test
    fun testProductIdColorFolding() {
        // Colorway range 0x100101 should fold to 060414
        val raw = hexToBytes("aa 0b 00 00 03 81 02 04 00 00 01 01 10")
        val frames = OpoFrame.drainFrames(raw.toMutableList())
        val state = OpoFrame.interpret(frames[0], BudsState())
        assertEquals("060414", state.productId)
    }

    @Test
    fun testCorruptExcessiveLengthDoesNotBlockBuffer() {
        // Corrupt stream with 0xAA followed by huge LEB128 length (0x7FFF = 16383)
        val buffer = mutableListOf<Byte>(
            OpoConstants.SYNC_BYTE,
            0xFF.toByte(), 0x7F.toByte(), 0x00, 0x00,
            // Valid hello frame follows
            OpoConstants.SYNC_BYTE, 0x07.toByte(), 0x00, 0x00, 0x00, 0x01, 0x01, 0x00, 0x00
        )
        val frames = OpoFrame.drainFrames(buffer)
        assertEquals(1, frames.size)
        assertEquals(OpoConstants.CMD_HANDSHAKE, frames[0].cmdId)
    }

    @Test
    fun testFirmwareResponseWithTrailingNullBytes() {
        val fwStr = "1,2,105,2,2,105,3,2,101\u0000\u0000"
        val fwPayload = byteArrayOf(0x00, 0x03) + fwStr.toByteArray(Charsets.UTF_8)
        val fwFrame = OpoFrame(OpoConstants.CMD_RESP_FIRMWARE, sequence = 1, payload = fwPayload)
        val state = OpoFrame.interpret(fwFrame, BudsState())
        assertEquals("105.105.101", state.firmwareVersion)
        assertFalse(state.firmwareVersion!!.contains("\u0000"))
    }

    @Test
    fun testThreeByteProductIdResponse() {
        // Product ID payload with 3 bytes (no leading 0 status byte)
        val payload = byteArrayOf(0x14, 0x04, 0x06) // 0x060414
        val frame = OpoFrame(OpoConstants.CMD_RESP_PRODUCT_ID, sequence = 1, payload = payload)
        val state = OpoFrame.interpret(frame, BudsState())
        assertEquals("060414", state.productId)
        assertEquals("realme Buds Air 6", state.deviceProfile.displayName)
        assertTrue(state.deviceProfile.artwork is com.openairbuds.app.model.EarbudArtwork.BundledResource)
    }

    @Test
    fun testProductId063412Response() {
        // Wire payload for 063412 with leading status byte 0
        // Little endian: 0x12, 0x34, 0x06
        val payload = byteArrayOf(0x00, 0x12, 0x34, 0x06)
        val frame = OpoFrame(OpoConstants.CMD_RESP_PRODUCT_ID, sequence = 1, payload = payload)
        val state = OpoFrame.interpret(frame, BudsState())
        assertEquals("063412", state.productId)
        assertEquals("realme Buds Air 6", state.deviceProfile.displayName)
        assertTrue(state.deviceProfile.artwork is com.openairbuds.app.model.EarbudArtwork.BundledResource)
    }

    @Test
    fun testFoldedProductId103412Response() {
        // Wire payload for 0x103412 (little endian: 0x12, 0x34, 0x10)
        val payload = byteArrayOf(0x00, 0x12, 0x34, 0x10)
        val frame = OpoFrame(OpoConstants.CMD_RESP_PRODUCT_ID, sequence = 1, payload = payload)
        val state = OpoFrame.interpret(frame, BudsState())
        assertEquals("063412", state.productId)
        assertEquals("realme Buds Air 6", state.deviceProfile.displayName)
        assertTrue(state.deviceProfile.artwork is com.openairbuds.app.model.EarbudArtwork.BundledResource)
    }

    @Test
    fun testAllAncModesWireDecoding() {
        assertEquals(NoiseControlMode.OFF, NoiseControlMode.fromWire(OpoConstants.WIRE_NOISE_OFF))
        assertEquals(NoiseControlMode.TRANSPARENCY, NoiseControlMode.fromWire(OpoConstants.WIRE_NOISE_TRANSPARENCY))
        assertEquals(NoiseControlMode.NOISE_CANCELLATION, NoiseControlMode.fromWire(OpoConstants.WIRE_ANC_MAX))
        assertEquals(NoiseControlMode.NOISE_CANCELLATION, NoiseControlMode.fromWire(OpoConstants.WIRE_ANC_MODERATE))
        assertEquals(NoiseControlMode.NOISE_CANCELLATION, NoiseControlMode.fromWire(OpoConstants.WIRE_ANC_MILD))
        assertEquals(NoiseControlMode.NOISE_CANCELLATION, NoiseControlMode.fromWire(OpoConstants.WIRE_ANC_SMART))

        assertEquals(AncLevel.HIGH_50DB, AncLevel.fromWire(OpoConstants.WIRE_ANC_MAX))
        assertEquals(AncLevel.MODERATE, AncLevel.fromWire(OpoConstants.WIRE_ANC_MODERATE))
        assertEquals(AncLevel.MILD, AncLevel.fromWire(OpoConstants.WIRE_ANC_MILD))
        assertEquals(AncLevel.SMART, AncLevel.fromWire(OpoConstants.WIRE_ANC_SMART))
        assertEquals(null, AncLevel.fromWire(OpoConstants.WIRE_NOISE_OFF))
    }

    @Test
    fun testDrainFramesMultipleFramesInSingleChunk() {
        val hello = OpoFrame.buildHello(sequence = 1)
        val battQuery = OpoFrame.buildBatteryQuery(sequence = 0xF0.toByte())
        val combined = (hello + battQuery).toMutableList()

        val frames = OpoFrame.drainFrames(combined)
        assertEquals(2, frames.size)
        assertEquals(OpoConstants.CMD_HANDSHAKE, frames[0].cmdId)
        assertEquals(OpoConstants.CMD_QUERY_BATTERY, frames[1].cmdId)
        assertTrue(combined.isEmpty())
    }

    @Test
    fun testRealmeBudsAir6RealHardwareFirmwareString() {
        val rawFw = "1,1,111,1,2,1.1.0.71,1,3,1.1.0.71,2,1,111,2,2,1.1.0.71,2,3,1.1.0.71,3,1,111,3,2,0,3,3,0"
        val payload = byteArrayOf(0x00, 0x09) + rawFw.toByteArray(Charsets.UTF_8)
        val frame = OpoFrame(OpoConstants.CMD_RESP_FIRMWARE, sequence = 1, payload = payload)
        val state = OpoFrame.interpret(frame, BudsState())
        assertEquals("1.1.0.71", state.firmwareVersion)
    }

    @Test
    fun testSingleEarbudInEarPlacement() {
        // Event payload: EVT_WEAR (0x02), count 2, slot 1 (left) in-ear (0x01), slot 2 (right) out-of-case (0x04)
        val payload = byteArrayOf(
            OpoConstants.EVT_WEAR, 0x02,
            OpoConstants.SLOT_LEFT, OpoConstants.PLACEMENT_IN_EAR,
            OpoConstants.SLOT_RIGHT, OpoConstants.PLACEMENT_OUT_OF_CASE
        )
        val frame = OpoFrame(OpoConstants.CMD_ACTIVE_REPORT, sequence = 1, payload = payload)
        val state = OpoFrame.interpret(frame, BudsState())

        assertTrue(state.placement.isLeftInEar)
        assertFalse(state.placement.isRightInEar)
        assertFalse(state.placement.areBothInEar)
    }

    @Test
    fun testBothEarbudsInEarPlacement() {
        // Event payload: EVT_WEAR (0x02), count 2, left in-ear (0x01), right in-ear (0x01)
        val payload = byteArrayOf(
            OpoConstants.EVT_WEAR, 0x02,
            OpoConstants.SLOT_LEFT, OpoConstants.PLACEMENT_IN_EAR,
            OpoConstants.SLOT_RIGHT, OpoConstants.PLACEMENT_IN_EAR
        )
        val frame = OpoFrame(OpoConstants.CMD_ACTIVE_REPORT, sequence = 1, payload = payload)
        val state = OpoFrame.interpret(frame, BudsState())

        assertTrue(state.placement.isLeftInEar)
        assertTrue(state.placement.isRightInEar)
        assertTrue(state.placement.areBothInEar)
    }

    @Test
    fun testBuildSetVoiceEnhanceFraming() {
        val onBytes = OpoFrame.buildSetVoiceEnhance(enabled = true, sequence = 10)
        assertEquals(11, onBytes.size)
        assertEquals(0x03.toByte(), onBytes[4]) // CMD_SET_FEATURE 0x0403
        assertEquals(0x04.toByte(), onBytes[5])
        assertEquals(10.toByte(), onBytes[6])   // sequence
        assertEquals(2.toByte(), onBytes[7])    // payLen
        assertEquals(0x09.toByte(), onBytes[9]) // FEATURE_ENHANCE_VOICE = 0x09
        assertEquals(0x01.toByte(), onBytes[10])

        val offBytes = OpoFrame.buildSetVoiceEnhance(enabled = false, sequence = 11)
        assertEquals(0x00.toByte(), offBytes[10])
    }

    @Test
    fun testSwitchDevicesGestureBindingActionName() {
        val binding = GestureBinding(device = 1, button = 1, action = 3, function = OpoConstants.GESTURE_FN_SWITCH_DEVICES)
        assertEquals("Triple Tap", binding.gestureName)
        assertEquals("Switch Devices", binding.actionName)
    }

    @Test
    fun testNewFeatureStateDefaults() {
        val state = BudsState()
        assertFalse(state.isVoiceEnhanceEnabled)
        assertFalse(state.isAutoAnswerCallsEnabled)
        assertFalse(state.isFindPhoneEnabled)
        assertFalse(state.isPhoneRingingFromBuds)
    }
}
