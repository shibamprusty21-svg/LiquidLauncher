package com.liquid.launcher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.GridView
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.InputStreamReader

data class AppModel(
    val label: String,
    val packageName: String,
    val iconBitmap: android.graphics.Bitmap
)

class MainActivity : Activity(), SensorEventListener {

    private lateinit var settingsRepo: LiquidGlassSettingsRepository
    private lateinit var iconTransformer: LiquidGlassIconTransformer
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var lightX: Float = 0.0f
    private var lightY: Float = 0.0f

    private val appsList = ArrayList<AppModel>()
    private lateinit var gridView: GridView
    private lateinit var adapter: AppAdapter
    private var runtimeShader: RuntimeShader? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsRepo = LiquidGlassSettingsRepository(this)
        iconTransformer = LiquidGlassIconTransformer(this)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        gridView = findViewById(R.id.apps_grid)
        adapter = AppAdapter(this, appsList)
        gridView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.fab_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        initShader()
        loadInstalledApps()
    }

    private fun initShader() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val shaderCode = assets.open("shaders/liquid_glass.agsl")
                    .bufferedReader().use(InputStreamReader::readText)
                runtimeShader = RuntimeShader(shaderCode)
            } catch (e: Exception) {
                runtimeShader = null
            }
        }
    }

    private fun loadInstalledApps() {
        appsList.clear()
        val config = settingsRepo.getConfig()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveList: List<ResolveInfo> = pm.queryIntentActivities(intent, 0)

        for (resolve in resolveList) {
            if (resolve.activityInfo.packageName == packageName) continue

            val label = resolve.loadLabel(pm).toString()
            val rawIcon = resolve.loadIcon(pm)
            val finalIcon = if (config.convertAllAppIcons) {
                iconTransformer.transformToLiquidGlass(
                    originalIcon = rawIcon,
                    glassDensity = config.density,
                    enableChromatic = config.chromaticAberration,
                    lightAngleX = lightX,
                    lightAngleY = lightY
                )
            } else {
                androidx.core.graphics.drawable.toBitmap(rawIcon, 192, 192)
            }

            appsList.add(AppModel(label, resolve.activityInfo.packageName, finalIcon))
        }
        adapter.notifyDataSetChanged()
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
        loadInstalledApps()
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val config = settingsRepo.getConfig()
        if (!config.gyroLightingEnabled) return

        // Normalize accelerometer tilt into light direction (-1.0 to 1.0)
        lightX = (event.values[0] / 9.8f).coerceIn(-1.0f, 1.0f)
        lightY = (event.values[1] / 9.8f).coerceIn(-1.0f, 1.0f)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null) {
            runtimeShader?.setFloatUniform("uLightDir", lightX, lightY)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    inner class AppAdapter(private val context: Context, private val list: List<AppModel>) :
        BaseAdapter() {
        override fun getCount(): Int = list.size
        override fun getItem(position: Int): Any = list[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: LayoutInflater.from(context)
                .inflate(R.layout.item_app_icon, parent, false)
            val app = list[position]

            val iconView = view.findViewById<ImageView>(R.id.img_icon)
            val labelView = view.findViewById<TextView>(R.id.txt_label)

            iconView.setImageBitmap(app.iconBitmap)
            labelView.text = app.label

            // Apply AGSL RenderEffect if running on Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && runtimeShader != null) {
                val config = settingsRepo.getConfig()
                runtimeShader?.setFloatUniform("uResolution", 192f, 192f)
                runtimeShader?.setFloatUniform("uGlassDensity", config.density)
                runtimeShader?.setFloatUniform("uRefractionIndex", 0.6f)
                runtimeShader?.setFloatUniform("uChromaticAberr", if (config.chromaticAberration) 1.0f else 0.0f)
                runtimeShader?.setFloatUniform("uCornerRadius", 44f)

                val shaderEffect = RenderEffect.createRuntimeShaderEffect(runtimeShader!!, "contentShader")
                iconView.setRenderEffect(shaderEffect)
            }

            view.setOnClickListener {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(app.packageName)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                }
            }
            return view
        }
    }
}
