package com.openairbuds.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Kenkashi Industrial Motion Specification System
 *
 * Designed for minimalist, brutalist, tactile audio hardware interfaces.
 * Characteristics:
 * - High stiffness, zero bounce/overshoot
 * - Snappy 180ms - 260ms durations matching high-end physical studio hardware
 * - Physical tactile haptic ticks on toggles and segment selectors
 */
object KenkashiMotion {
    const val DurationFast: Int = 180
    const val DurationMedium: Int = 240
    const val DurationSlow: Int = 320

    const val EasingA: Float = 0.2f
    const val EasingB: Float = 0.0f
    const val EasingC: Float = 0.0f
    const val EasingD: Float = 1.0f

    // Mechanical deceleration curve (fast initial move, instantaneous sharp settle without bouncing)
    val MechanicalCubicBezier = CubicBezierEasing(EasingA, EasingB, EasingC, EasingD)

    fun <T> mechanicalTween(
        durationMillis: Int = DurationMedium,
        delayMillis: Int = 0
    ): TweenSpec<T> = tween(
        durationMillis = durationMillis,
        delayMillis = delayMillis,
        easing = MechanicalCubicBezier
    )

    fun <T> mechanicalSpring(
        dampingRatio: Float = 0.9f,
        stiffness: Float = Spring.StiffnessMedium
    ): SpringSpec<T> = spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness
    )

    /**
     * Map a decibel gain (-6dB .. +6dB) to a normalized 0f..1f Y-axis coordinate
     * for drawing on a frequency curve canvas.
     * Top of canvas is 0.0f (+6dB), Center is 0.5f (0dB), Bottom of canvas is 1.0f (-6dB).
     */
    fun gainToNormalizedY(gain: Float, minGain: Float = -6f, maxGain: Float = 6f): Float {
        if (maxGain <= minGain) return 0.5f
        val clamped = gain.coerceIn(minGain, maxGain)
        // Invert Y so higher gain is at the top (0.0f)
        return 1.0f - ((clamped - minGain) / (maxGain - minGain))
    }

    /**
     * Standard baseline 6-band EQ gains [62Hz, 250Hz, 1kHz, 4kHz, 8kHz, 16kHz] for presets.
     */
    fun getPresetCurveGains(presetName: String): List<Int> {
        val normalized = presetName.replace("_", " ")
        return when {
            normalized.contains("Nature Balance", ignoreCase = true) ||
            normalized.contains("Balanced", ignoreCase = true) ->
                listOf(0, 0, 0, 0, 0, 0)
            normalized.contains("Bass Boost", ignoreCase = true) ||
            normalized.contains("Deep Bass", ignoreCase = true) ->
                listOf(5, 4, 1, 0, -1, -2)
            normalized.contains("Clear Bass", ignoreCase = true) ->
                listOf(4, 3, 0, 1, 1, 0)
            normalized.contains("Clear Vocals", ignoreCase = true) ->
                listOf(-2, -1, 3, 4, 2, 0)
            else ->
                listOf(0, 0, 0, 0, 0, 0)
        }
    }
}

/**
 * Kenkashi Tactile Press Modifier
 * Applies a subtle 0.98x scale depression on press and triggers a light mechanical haptic tick.
 */
fun Modifier.kenkashiPressable(
    enabled: Boolean = true,
    hapticsEnabled: Boolean = true,
    pressedScale: Float = 0.98f,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && enabled) pressedScale else 1.0f,
        animationSpec = KenkashiMotion.mechanicalTween(durationMillis = KenkashiMotion.DurationFast),
        label = "KenkashiPressScale"
    )

    this
        .graphicsLayer {
            scaleX = animatedScale
            scaleY = animatedScale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled
        ) {
            if (hapticsEnabled) {
                runCatching {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            }
            onClick()
        }
}
