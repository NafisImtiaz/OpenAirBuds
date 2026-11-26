package com.openairbuds.app

import com.openairbuds.app.ui.theme.KenkashiMotion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class KenkashiMotionTest {

    @Test
    fun testMotionTokenDurationsAndConstants() {
        assertEquals(200, KenkashiMotion.DurationFast)
        assertEquals(280, KenkashiMotion.DurationMedium)
        assertEquals(360, KenkashiMotion.DurationSlow)

        // Easing curve control points (silky fluid mechanical curve for 120 FPS persistence)
        assertEquals(0.05f, KenkashiMotion.EasingA, 0.001f)
        assertEquals(0.7f, KenkashiMotion.EasingB, 0.001f)
        assertEquals(0.1f, KenkashiMotion.EasingC, 0.001f)
        assertEquals(1.0f, KenkashiMotion.EasingD, 0.001f)

        // Transform test
        assertEquals(0.0f, KenkashiMotion.MechanicalCubicBezier.transform(0.0f), 0.001f)
        assertEquals(1.0f, KenkashiMotion.MechanicalCubicBezier.transform(1.0f), 0.001f)
    }

    @Test
    fun testMechanicalTweenAndSpringSpecs() {
        // Default mechanical tween spec
        val defaultTween = KenkashiMotion.mechanicalTween<Float>()
        assertEquals(KenkashiMotion.DurationMedium, defaultTween.durationMillis)
        assertEquals(0, defaultTween.delay)
        assertEquals(KenkashiMotion.MechanicalCubicBezier, defaultTween.easing)

        // Custom duration and delay
        val customTween = KenkashiMotion.mechanicalTween<Float>(
            durationMillis = KenkashiMotion.DurationFast,
            delayMillis = 50
        )
        assertEquals(200, customTween.durationMillis)
        assertEquals(50, customTween.delay)

        // Spring spec defaults
        val springSpec = KenkashiMotion.mechanicalSpring<Float>()
        assertEquals(0.9f, springSpec.dampingRatio, 0.001f)
        assertEquals(androidx.compose.animation.core.Spring.StiffnessMedium, springSpec.stiffness, 0.001f)
    }

    @Test
    fun testMechanicalCubicBezierIntermediateProgression() {
        val bezier = KenkashiMotion.MechanicalCubicBezier
        val q1 = bezier.transform(0.25f)
        val mid = bezier.transform(0.5f)
        val q3 = bezier.transform(0.75f)

        // Strictly monotonic and within [0, 1]
        assertTrue(q1 in 0.0f..1.0f)
        assertTrue(mid in 0.0f..1.0f)
        assertTrue(q3 in 0.0f..1.0f)
        assertTrue(q1 <= mid)
        assertTrue(mid <= q3)
    }

    @Test
    fun testEqGainToNormalizedYCoordinate() {
        // Default arguments (-6dB to +6dB)
        assertEquals(1.0f, KenkashiMotion.gainToNormalizedY(-6f), 0.001f)
        assertEquals(0.5f, KenkashiMotion.gainToNormalizedY(0f), 0.001f)
        assertEquals(0.0f, KenkashiMotion.gainToNormalizedY(6f), 0.001f)

        // Linear interpolation midpoints
        assertEquals(0.25f, KenkashiMotion.gainToNormalizedY(3f), 0.001f)
        assertEquals(0.75f, KenkashiMotion.gainToNormalizedY(-3f), 0.001f)

        // Clamping checks
        val overMax = KenkashiMotion.gainToNormalizedY(10f)
        val underMin = KenkashiMotion.gainToNormalizedY(-10f)
        assertEquals(0.0f, overMax, 0.001f)
        assertEquals(1.0f, underMin, 0.001f)

        // Custom range (-12dB to +12dB)
        assertEquals(0.5f, KenkashiMotion.gainToNormalizedY(0f, minGain = -12f, maxGain = 12f), 0.001f)
        assertEquals(0.0f, KenkashiMotion.gainToNormalizedY(12f, minGain = -12f, maxGain = 12f), 0.001f)
        assertEquals(1.0f, KenkashiMotion.gainToNormalizedY(-12f, minGain = -12f, maxGain = 12f), 0.001f)

        // Degenerate zero / negative range guard check
        assertEquals(0.5f, KenkashiMotion.gainToNormalizedY(0f, minGain = 5f, maxGain = 5f), 0.001f)
        assertEquals(0.5f, KenkashiMotion.gainToNormalizedY(0f, minGain = 10f, maxGain = 2f), 0.001f)
    }

    @Test
    fun testEqPresetExactValuesAndFallbacks() {
        val flat = listOf(0, 0, 0, 0, 0, 0)

        // Exact 6-band values
        assertEquals(flat, KenkashiMotion.getPresetCurveGains("Nature Balance"))
        assertEquals(flat, KenkashiMotion.getPresetCurveGains("Balanced"))
        assertEquals(listOf(5, 4, 1, 0, -1, -2), KenkashiMotion.getPresetCurveGains("Bass Boost"))
        assertEquals(listOf(5, 4, 1, 0, -1, -2), KenkashiMotion.getPresetCurveGains("Deep Bass"))
        assertEquals(listOf(4, 3, 0, 1, 1, 0), KenkashiMotion.getPresetCurveGains("Clear Bass"))
        assertEquals(listOf(-2, -1, 3, 4, 2, 0), KenkashiMotion.getPresetCurveGains("Clear Vocals"))

        // Case insensitivity & partial matches
        assertEquals(listOf(5, 4, 1, 0, -1, -2), KenkashiMotion.getPresetCurveGains("bass boost"))
        assertEquals(listOf(4, 3, 0, 1, 1, 0), KenkashiMotion.getPresetCurveGains("PRESET_CLEAR_BASS"))

        // Fallback for unknown / custom presets
        assertEquals(flat, KenkashiMotion.getPresetCurveGains("Custom"))
        assertEquals(flat, KenkashiMotion.getPresetCurveGains("Rock"))
        assertEquals(flat, KenkashiMotion.getPresetCurveGains(""))
    }

    @Test
    fun testClampIndicatorPosition() {
        assertEquals(0.0f, KenkashiMotion.clampIndicatorPosition(0, 0.0f, 3), 0.001f)
        assertEquals(0.5f, KenkashiMotion.clampIndicatorPosition(0, 0.5f, 3), 0.001f)
        assertEquals(1.75f, KenkashiMotion.clampIndicatorPosition(2, -0.25f, 3), 0.001f)
        // Overscroll clamping
        assertEquals(0.0f, KenkashiMotion.clampIndicatorPosition(0, -0.4f, 3), 0.001f)
        assertEquals(2.0f, KenkashiMotion.clampIndicatorPosition(2, 0.4f, 3), 0.001f)
        // Degenerate count guard
        assertEquals(0.0f, KenkashiMotion.clampIndicatorPosition(1, 0.5f, 1), 0.001f)
        assertEquals(0.0f, KenkashiMotion.clampIndicatorPosition(0, 0.0f, 0), 0.001f)
    }

    @Test
    fun testTabTweenConstantAndSpec() {
        assertEquals(160, KenkashiMotion.DurationTab)
        val tweenSpec = KenkashiMotion.tabTween<Float>()
        assertEquals(160, tweenSpec.durationMillis)
        assertEquals(0, tweenSpec.delay)
    }
}
