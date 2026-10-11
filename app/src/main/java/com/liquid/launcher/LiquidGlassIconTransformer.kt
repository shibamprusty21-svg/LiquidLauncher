package com.liquid.launcher

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.sin

class LiquidGlassIconTransformer(private val context: Context) {

    private val targetSize = 192 // Standard density target for high-res icons

    fun transformToLiquidGlass(
        originalIcon: Drawable,
        glassDensity: Float,       // 0.0 (clear) to 1.0 (opaque)
        enableChromatic: Boolean,
        lightAngleX: Float = 0.5f,
        lightAngleY: Float = -0.5f
    ): Bitmap {
        val srcBitmap = originalIcon.toBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val output = Bitmap.createBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)

        val rect = RectF(0f, 0f, targetSize.toFloat(), targetSize.toFloat())
        val cornerRadius = targetSize * 0.23f // Standard iOS squircle radius ratio

        // 1. Draw Substrate Ambient Contact Shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(60, 0, 0, 0)
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }
        val shadowRect = RectF(rect).apply { offset(0f, 6f) }
        canvas.drawRoundRect(shadowRect, cornerRadius, cornerRadius, shadowPaint)

        // 2. Liquid Glass Base Layer (Translucent Acrylic Volume)
        val baseAlpha = ((1.0f - glassDensity) * 60 + 40).toInt()
        val glassPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, targetSize.toFloat(), targetSize.toFloat(),
                Color.argb(baseAlpha + 30, 255, 255, 255),
                Color.argb(baseAlpha, 220, 230, 245),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glassPaint)

        // 3. Render Recessed / Lensed Foreground Icon Logo
        val iconPadding = targetSize * 0.18f
        val innerRect = RectF(
            iconPadding,
            iconPadding,
            targetSize - iconPadding,
            targetSize - iconPadding
        )
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            alpha = ((glassDensity * 0.4f + 0.6f) * 255).toInt()
        }
        canvas.drawBitmap(srcBitmap, null, innerRect, iconPaint)

        // 4. Curved Refractive Bevel & Specular Highlight (Fresnel Top-Left Rim)
        val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
            shader = LinearGradient(
                rect.left, rect.top, rect.right, rect.bottom,
                intArrayOf(
                    Color.argb(230, 255, 255, 255), // Top-left specular glare
                    Color.argb(80, 255, 255, 255),
                    Color.argb(20, 255, 255, 255),
                    if (enableChromatic) Color.argb(120, 160, 220, 255) else Color.argb(60, 255, 255, 255) // Bottom-right refraction
                ),
                floatArrayOf(0.0f, 0.35f, 0.70f, 1.0f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, rimPaint)

        // 5. Dynamic Surface Glare Sheen (Motion / Gyro tracking simulation)
        val glarePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                targetSize * (0.35f + lightAngleX * 0.2f),
                targetSize * (0.25f + lightAngleY * 0.2f),
                targetSize * 0.65f,
                Color.argb(110, 255, 255, 255),
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glarePaint)

        return output
    }
}
