package com.liquid.launcher

import android.app.Activity
import android.os.Bundle
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView

class SettingsActivity : Activity() {

    private lateinit var repo: LiquidGlassSettingsRepository
    private lateinit var iconTransformer: LiquidGlassIconTransformer
    private lateinit var previewIcon: ImageView
    private lateinit var densitySlider: SeekBar
    private lateinit var chromaticSwitch: Switch
    private lateinit var gyroSwitch: Switch
    private lateinit var convertAllSwitch: Switch
    private lateinit var densityValueText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        repo = LiquidGlassSettingsRepository(this)
        iconTransformer = LiquidGlassIconTransformer(this)

        previewIcon = findViewById(R.id.img_preview)
        densitySlider = findViewById(R.id.seek_density)
        chromaticSwitch = findViewById(R.id.switch_chromatic)
        gyroSwitch = findViewById(R.id.switch_gyro)
        convertAllSwitch = findViewById(R.id.switch_convert_all)
        densityValueText = findViewById(R.id.txt_density_val)

        val config = repo.getConfig()

        densitySlider.progress = (config.density * 100).toInt()
        chromaticSwitch.isChecked = config.chromaticAberration
        gyroSwitch.isChecked = config.gyroLightingEnabled
        convertAllSwitch.isChecked = config.convertAllAppIcons
        updateDensityLabel(config.density)
        updatePreview()

        densitySlider.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val density = progress / 100f
                updateDensityLabel(density)
                saveCurrent()
                updatePreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        chromaticSwitch.setOnCheckedChangeListener { _, _ -> saveCurrent(); updatePreview() }
        gyroSwitch.setOnCheckedChangeListener { _, _ -> saveCurrent() }
        convertAllSwitch.setOnCheckedChangeListener { _, _ -> saveCurrent(); updatePreview() }
    }

    private fun updateDensityLabel(density: Float) {
        val label = when {
            density < 0.25f -> "Ultra Glass (High Refraction)"
            density < 0.70f -> "Liquid Frosted (Balanced)"
            else -> "Opaque Solid"
        }
        densityValueText.text = "$label (${(density * 100).toInt()}%)"
    }

    private fun updatePreview() {
        val sampleDrawable = packageManager.getApplicationIcon(packageName)
        val bitmap = iconTransformer.transformToLiquidGlass(
            originalIcon = sampleDrawable,
            glassDensity = densitySlider.progress / 100f,
            enableChromatic = chromaticSwitch.isChecked
        )
        previewIcon.setImageBitmap(bitmap)
    }

    private fun saveCurrent() {
        val updated = LiquidGlassConfig(
            density = densitySlider.progress / 100f,
            chromaticAberration = chromaticSwitch.isChecked,
            gyroLightingEnabled = gyroSwitch.isChecked,
            convertAllAppIcons = convertAllSwitch.isChecked,
            blurRadius = 25f
        )
        repo.saveConfig(updated)
    }
}
