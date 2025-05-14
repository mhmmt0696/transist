package com.transist.ui.customviews

import android.content.Context
import android.util.AttributeSet

class CustomTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyle) {

    override fun performClick(): Boolean {
        // accessibility için varsayılan davranışı çağır
        return super.performClick()
    }
}
