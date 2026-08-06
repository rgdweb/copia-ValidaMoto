package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

class SoundAndVibrationHelper(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        } catch (e: Exception) {
            Log.e("SoundAndVibrationHelper", "Failed to create ToneGenerator", e)
        }
    }

    fun playBeep(durationMs: Int, force: Boolean = false, isBeepSettingEnabled: Boolean = true) {
        if (!force && !isBeepSettingEnabled) return
        val generator = toneGenerator ?: try {
            ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).also { toneGenerator = it }
        } catch (e: Exception) {
            null
        }

        if (generator != null) {
            Thread {
                try {
                    generator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMs)
                } catch (e: Exception) {
                    Log.e("SoundAndVibrationHelper", "Failed to play beep tone", e)
                }
            }.start()
        }
    }

    fun vibrate(durationMs: Int, force: Boolean = false, isVibrationSettingEnabled: Boolean = true) {
        if (!force && !isVibrationSettingEnabled) return
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(
                        VibrationEffect.createOneShot(
                            durationMs.toLong(),
                            VibrationEffect.DEFAULT_AMPLITUDE
                        )
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(durationMs.toLong())
                }
            }
        } catch (e: Exception) {
            Log.e("SoundAndVibrationHelper", "Vibration failed", e)
        }
    }

    fun flashTorch(durationMs: Long = 500L) {
        try {
            if (!context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)) {
                return
            }
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                try {
                    cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } catch (e: Throwable) {
                    false
                }
            }
            if (cameraId != null) {
                Thread {
                    try {
                        cameraManager.setTorchMode(cameraId, true)
                        Thread.sleep(durationMs)
                        cameraManager.setTorchMode(cameraId, false)
                    } catch (e: Throwable) {
                        Log.e("SoundAndVibrationHelper", "Flash torch error", e)
                    }
                }.start()
            }
        } catch (e: Throwable) {
            Log.e("SoundAndVibrationHelper", "Flash torch not available", e)
        }
    }

    fun triggerEmergencyAlert(synchronous: Boolean = false) {
        val runnable = Runnable {
            try {
                val generator = toneGenerator ?: try {
                    ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100).also { toneGenerator = it }
                } catch (e: Exception) {
                    null
                }

                val vibrator = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                        vibratorManager?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                } catch (e: Exception) {
                    null
                }

                val cameraManager = try {
                    if (context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_CAMERA_FLASH)) {
                        context.getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                    } else null
                } catch (e: Throwable) {
                    null
                }

                val cameraId = try {
                    cameraManager?.cameraIdList?.firstOrNull { id ->
                        try {
                            cameraManager.getCameraCharacteristics(id).get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                        } catch (e: Throwable) {
                            false
                        }
                    }
                } catch (e: Throwable) {
                    null
                }

                for (i in 1..5) {
                    // 1. Sound (Beep)
                    try {
                        generator?.startTone(ToneGenerator.TONE_PROP_BEEP, 250)
                    } catch (e: Throwable) {
                        Log.e("SoundAndVibrationHelper", "Beep error during emergency alert", e)
                    }

                    // 2. Vibration
                    try {
                        if (vibrator?.hasVibrator() == true) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(VibrationEffect.createOneShot(250L, VibrationEffect.DEFAULT_AMPLITUDE))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(250L)
                            }
                        }
                    } catch (e: Throwable) {
                        Log.e("SoundAndVibrationHelper", "Vibration error during emergency alert", e)
                    }

                    // 3. Flash torch ON
                    try {
                        if (cameraManager != null && cameraId != null) {
                            cameraManager.setTorchMode(cameraId, true)
                        }
                    } catch (e: Throwable) {
                        Log.e("SoundAndVibrationHelper", "Flash ON error during emergency alert", e)
                    }

                    // Pulse duration
                    try {
                        Thread.sleep(250L)
                    } catch (e: InterruptedException) {
                        break
                    }

                    // Flash torch OFF
                    try {
                        if (cameraManager != null && cameraId != null) {
                            cameraManager.setTorchMode(cameraId, false)
                        }
                    } catch (e: Throwable) {
                        Log.e("SoundAndVibrationHelper", "Flash OFF error during emergency alert", e)
                    }

                    // Gap between beeps (150ms)
                    if (i < 5) {
                        try {
                            Thread.sleep(150L)
                        } catch (e: InterruptedException) {
                            break
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("SoundAndVibrationHelper", "Emergency alert failed", e)
            }
        }

        if (synchronous) {
            runnable.run()
        } else {
            Thread(runnable).start()
        }
    }
}
