package com.ec.bond.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.widget.EditText
import com.simplemobiletools.commons.extensions.adjustAlpha
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.helpers.MEDIUM_ALPHA


@SuppressLint("AppCompatCustomView")
class MyEditText : EditText {
    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun setColors(textColor: Int, accentColor: Int, backgroundColor: Int) {
        background?.mutate()?.applyColorFilter(accentColor)

        // requires android:textCursorDrawable="@null" in xml to color the cursor too
        setTextColor(textColor)
        setHintTextColor(textColor.adjustAlpha(MEDIUM_ALPHA))
        setLinkTextColor(accentColor)
    }
}