package com.lexiflow.wordbook.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Space
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.lexiflow.wordbook.R
import kotlin.math.roundToInt

class UiKit(val context: Context) {
    val forest = color(R.color.forest)
    val ink = color(R.color.ink)
    val muted = color(R.color.muted)
    val paper = color(R.color.paper)
    val mint = color(R.color.mint)
    val coral = color(R.color.coral)

    fun vertical(block: (LinearLayout.() -> Unit)? = null) = LinearLayout(context).apply {
        orientation = LinearLayout.VERTICAL
        block?.invoke(this)
    }

    fun horizontal(block: LinearLayout.() -> Unit) = LinearLayout(context).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        block()
    }

    fun text(value: String, size: Int, color: Int = ink, bold: Boolean = false, gravity: Int = Gravity.START) =
        TextView(context).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            this.gravity = gravity
            includeFontPadding = false
            if (bold) setTypeface(typeface, Typeface.BOLD)
            setLineSpacing(dp(2).toFloat(), 1f)
        }

    fun card(color: Int = paper, block: LinearLayout.() -> Unit) = MaterialCardView(context).apply {
        radius = dp(22).toFloat()
        cardElevation = dp(1).toFloat()
        strokeWidth = dp(1)
        strokeColor = if (color == paper) this@UiKit.color(R.color.sand) else color
        setCardBackgroundColor(color)
        addView(vertical(block))
    }

    fun primaryButton(label: String, action: (View) -> Unit) = MaterialButton(context).apply {
        text = label
        textSize = 15f
        cornerRadius = dp(15)
        setTextColor(Color.WHITE)
        backgroundTintList = ColorStateList.valueOf(forest)
        setOnClickListener(action)
    }

    fun softButton(label: String, tint: Int = mint, textColor: Int = ink, action: (View) -> Unit) =
        MaterialButton(context).apply {
            text = label
            textSize = 14f
            cornerRadius = dp(15)
            setTextColor(textColor)
            backgroundTintList = ColorStateList.valueOf(tint)
            setOnClickListener(action)
        }

    fun progress(maximum: Int, value: Int, tint: Int = forest) =
        ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = maximum
            progress = value.coerceIn(0, maximum)
            progressTintList = ColorStateList.valueOf(tint)
            progressBackgroundTintList = ColorStateList.valueOf(mint)
        }

    fun LinearLayout.gap(value: Int) =
        addView(Space(context), LinearLayout.LayoutParams(1, dp(value)))

    fun str(id: Int) = context.getString(id)
    fun str(id: Int, vararg args: Any) = context.getString(id, *args)

    fun LinearLayout.addDivider() {
        addView(View(context).apply {
            layoutParams = LinearLayout.LayoutParams(-1, dp(1))
            setBackgroundColor(muted and 0x3fffffff)
        })
    }

    fun dp(value: Int) = (value * context.resources.displayMetrics.density).roundToInt()
    private fun color(id: Int) = ContextCompat.getColor(context, id)
}
