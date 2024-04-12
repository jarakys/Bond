package com.ec.bond.extensions

import android.widget.CompoundButton

fun CompoundButton.setCustomChecked(value: Boolean, listener: CompoundButton.OnCheckedChangeListener) {
    setOnCheckedChangeListener(null)
    isChecked = value
    setOnCheckedChangeListener(listener)
}