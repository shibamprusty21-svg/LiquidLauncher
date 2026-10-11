package com.liquid.launcher

import android.content.Context
import android.content.SharedPreferences

data class LiquidGlassConfig(
    val density: Float,              // 0.0f (Clear Glass) to 1.0f (Opaque)
    val chromaticAberration: Boolean,
    val gyroLightingEnabled: Boolean,
    val convertAllAppIcons: Boolean,
    val blurRadius: Float
)

class LiquidGlassSettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("liquid_glass_prefs", Context.MODE_PRIVATE)

    fun getConfig(): LiquidGlassConfig {
        return LiquidGlassConfig(
            density = prefs.getFloat(KEY_DENSITY, 0.25f),
            chromaticAberration = prefs.getBoolean(KEY_CHROMATIC, true),
            gyroLightingEnabled = prefs.getBoolean(KEY_GYRO, true),
            convertAllAppIcons = prefs.getBoolean(KEY_CONVERT_ICONS, true),
            blurRadius = prefs.getFloat(KEY_BLUR, 25f)
        )
    }

    fun saveConfig(config: LiquidGlassConfig) {
        prefs.edit().apply {
            putFloat(KEY_DENSITY, config.density)
            putBoolean(KEY_CHROMATIC, config.chromaticAberration)
            putBoolean(KEY_GYRO, config.gyroLightingEnabled)
            putBoolean(KEY_CONVERT_ICONS, config.convertAllAppIcons)
            putFloat(KEY_BLUR, config.blurRadius)
            apply()
        }
    }

    companion object {
        const val KEY_DENSITY = "key_density"
        const val KEY_CHROMATIC = "key_chromatic"
        const val KEY_GYRO = "key_gyro"
        const val KEY_CONVERT_ICONS = "key_convert_icons"
        const val KEY_BLUR = "key_blur"
    }
}
