package com.openairbuds.app.shortcut

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import com.openairbuds.app.bluetooth.OpoBluetoothManager
import com.openairbuds.app.model.NoiseControlMode

/**
 * Headless trampoline activity handling Android App Shortcut taps from the launcher.
 * Switches sound mode in the background with haptic and toast confirmation, then finishes immediately.
 */
class ShortcutHandlerActivity : Activity() {

    companion object {
        const val ACTION_SET_NOISE_MODE = "com.openairbuds.app.action.SET_NOISE_MODE"
        const val EXTRA_NOISE_MODE = "noise_mode"

        fun createIntent(context: Context, mode: NoiseControlMode): Intent {
            return Intent(context, ShortcutHandlerActivity::class.java).apply {
                action = ACTION_SET_NOISE_MODE
                putExtra(EXTRA_NOISE_MODE, mode.name)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        disableActivityAnimations()

        val modeStr = intent?.getStringExtra(EXTRA_NOISE_MODE)
        val targetMode = modeStr?.let {
            runCatching { NoiseControlMode.valueOf(it) }.getOrNull()
        }

        if (targetMode != null) {
            val bluetoothManager = OpoBluetoothManager.getInstance(applicationContext)
            val currentState = bluetoothManager.budsState.value

            when (val result = ShortcutActionEngine.evaluate(targetMode, currentState)) {
                is ShortcutResult.Success -> {
                    bluetoothManager.setNoiseControlMode(result.mode)
                    playHapticFeedback()
                    Toast.makeText(applicationContext, result.message, Toast.LENGTH_SHORT).show()
                }
                is ShortcutResult.NotConnected -> {
                    Toast.makeText(applicationContext, result.message, Toast.LENGTH_SHORT).show()
                }
                is ShortcutResult.WearRestriction -> {
                    Toast.makeText(applicationContext, result.message, Toast.LENGTH_SHORT).show()
                }
                is ShortcutResult.Unsupported -> {
                    Toast.makeText(applicationContext, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        finish()
        disableActivityAnimations()
    }

    private fun playHapticFeedback() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            }
        }.onFailure { e ->
            android.util.Log.w("ShortcutHandler", "Haptic feedback unavailable: ${e.message}")
        }
    }

    private fun disableActivityAnimations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
