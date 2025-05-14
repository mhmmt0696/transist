package com.transist.ui.customviews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.text.style.ReplacementSpan
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

class DrawableSpan(
    private val context: Context,
    @DrawableRes private val drawableRes: Int
) : ReplacementSpan() {

    private var drawable: Drawable? = null

    private fun getDrawable(): Drawable {
        if (drawable == null) {
            drawable = ContextCompat.getDrawable(context, drawableRes)?.apply {
                setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            }
        }
        return drawable!!
    }

    override fun getSize(
        paint: Paint,
        text: CharSequence?,
        start: Int,
        end: Int,
        fm: Paint.FontMetricsInt?
    ): Int {
        val d = getDrawable()
        if (fm != null) {
            val fontHeight = paint.fontMetricsInt.descent - paint.fontMetricsInt.ascent
            val drHeight = d.bounds.height()
            val centerY = (fontHeight - drHeight) / 2
            fm.ascent = -drHeight + centerY
            fm.descent = centerY
            fm.top = fm.ascent
            fm.bottom = fm.descent
        }
        return d.bounds.width()
    }

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val d = getDrawable()
        val transY = bottom - d.bounds.bottom
        canvas.save()
        canvas.translate(x, transY.toFloat())
        d.draw(canvas)
        canvas.restore()
    }
}
