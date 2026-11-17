package com.openairbuds.app

import com.openairbuds.app.model.NoiseControlMode
import org.junit.Assert.assertEquals
import org.junit.Test

class AncTileLogicTest {

    private fun getNextTileMode(current: NoiseControlMode): NoiseControlMode {
        return when (current) {
            NoiseControlMode.NOISE_CANCELLATION -> NoiseControlMode.TRANSPARENCY
            NoiseControlMode.TRANSPARENCY -> NoiseControlMode.OFF
            NoiseControlMode.OFF -> NoiseControlMode.NOISE_CANCELLATION
        }
    }

    private fun getTileLabel(mode: NoiseControlMode): String {
        return when (mode) {
            NoiseControlMode.NOISE_CANCELLATION -> "Noise Cancelling"
            NoiseControlMode.TRANSPARENCY -> "Transparency"
            NoiseControlMode.OFF -> "Normal"
        }
    }

    @Test
    fun testTileModeCycleProgression() {
        var mode = NoiseControlMode.NOISE_CANCELLATION
        assertEquals("Noise Cancelling", getTileLabel(mode))

        mode = mode.next(areBothInEar = true)
        assertEquals(NoiseControlMode.TRANSPARENCY, mode)
        assertEquals("Transparency", getTileLabel(mode))

        mode = mode.next(areBothInEar = true)
        assertEquals(NoiseControlMode.OFF, mode)
        assertEquals("Normal", getTileLabel(mode))

        mode = mode.next(areBothInEar = true)
        assertEquals(NoiseControlMode.NOISE_CANCELLATION, mode)
        assertEquals("Noise Cancelling", getTileLabel(mode))
    }

    @Test
    fun testTileModeCycleRestrictedWhenSingleEarbudInEar() {
        // When only 1 earbud is worn, ANC must be skipped silently
        var mode = NoiseControlMode.OFF
        mode = mode.next(areBothInEar = false)
        assertEquals(NoiseControlMode.TRANSPARENCY, mode)

        mode = mode.next(areBothInEar = false)
        assertEquals(NoiseControlMode.OFF, mode)

        // If somehow in ANC and tapped, fall back to Transparency
        mode = NoiseControlMode.NOISE_CANCELLATION.next(areBothInEar = false)
        assertEquals(NoiseControlMode.TRANSPARENCY, mode)
    }

    @Test
    fun testExhaustiveModesCoverage() {
        for (mode in NoiseControlMode.values()) {
            val next = mode.next(areBothInEar = true)
            val label = getTileLabel(mode)
            org.junit.Assert.assertNotEquals(mode, next)
            org.junit.Assert.assertTrue(label.isNotEmpty())
        }
    }
}
