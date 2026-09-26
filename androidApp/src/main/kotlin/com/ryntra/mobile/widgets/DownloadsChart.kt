package com.ryntra.mobile.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.core.graphics.createBitmap

/**
 * Draws the downloads chart into a bitmap, since a home-screen widget cannot draw on its own.
 *
 * A week reads best as seven bars; longer ranges become an area line, where individual days
 * matter less than the shape.
 */
internal object DownloadsChart {
    /** Widgets share one bitmap budget per process; 2× density stays sharp and well inside it. */
    private const val MAX_SCALE = 2f

    fun render(
        values: List<Long>,
        widthDp: Float,
        heightDp: Float,
        density: Float,
        lineColor: Int,
        trackColor: Int,
    ): Bitmap {
        val scale = minOf(density, MAX_SCALE)
        val width = (widthDp * scale).toInt().coerceAtLeast(1)
        val height = (heightDp * scale).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(width, height)
        val canvas = Canvas(bitmap)
        val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1L
        if (values.size <= BAR_THRESHOLD) {
            drawBars(canvas, values, max, width.toFloat(), height.toFloat(), scale, lineColor, trackColor)
        } else {
            drawArea(canvas, values, max, width.toFloat(), height.toFloat(), scale, lineColor)
        }
        return bitmap
    }

    private const val BAR_THRESHOLD = 14

    private fun drawBars(
        canvas: Canvas,
        values: List<Long>,
        max: Long,
        width: Float,
        height: Float,
        scale: Float,
        color: Int,
        trackColor: Int,
    ) {
        val gap = 6f * scale
        val barWidth = (width - gap * (values.size - 1)) / values.size
        val radius = minOf(barWidth / 2, 6f * scale)
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = trackColor }
        val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        values.forEachIndexed { index, value ->
            val left = index * (barWidth + gap)
            val rect = RectF(left, 0f, left + barWidth, height)
            canvas.drawRoundRect(rect, radius, radius, track)
            // A day with downloads always shows a sliver, so it never reads as zero.
            val barHeight = if (value == 0L) 0f else maxOf(height * value / max, radius * 2)
            if (barHeight > 0f) {
                canvas.drawRoundRect(RectF(left, height - barHeight, left + barWidth, height), radius, radius, bar)
            }
        }
    }

    private fun drawArea(
        canvas: Canvas,
        values: List<Long>,
        max: Long,
        width: Float,
        height: Float,
        scale: Float,
        color: Int,
    ) {
        val stroke = 2.5f * scale
        // Leaves room for the stroke at the top instead of clipping the busiest day.
        val top = stroke
        val usable = height - top - stroke
        val step = width / (values.size - 1).coerceAtLeast(1)
        val points = values.mapIndexed { index, value -> index * step to top + usable * (1f - value.toFloat() / max) }

        val line = Path().apply {
            moveTo(points.first().first, points.first().second)
            points.drop(1).forEach { (x, y) -> lineTo(x, y) }
        }
        val fill = Path(line).apply {
            lineTo(points.last().first, height)
            lineTo(points.first().first, height)
            close()
        }
        canvas.drawPath(
            fill,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(0f, 0f, 0f, height, withAlpha(color, 0.35f), withAlpha(color, 0f), Shader.TileMode.CLAMP)
            },
        )
        canvas.drawPath(
            line,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = stroke
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
            },
        )
        val (lastX, lastY) = points.last()
        canvas.drawCircle(lastX - stroke, lastY, stroke * 1.6f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color })
    }

    private fun withAlpha(color: Int, alpha: Float): Int =
        (color and 0x00FFFFFF) or ((alpha * 255).toInt() shl 24)
}
