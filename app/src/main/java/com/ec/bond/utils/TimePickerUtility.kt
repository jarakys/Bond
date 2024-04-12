package com.ec.bond.utils

import android.app.TimePickerDialog
import android.content.Context
import android.content.DialogInterface
import com.ec.bond.R
import java.util.*

object TimePickerUtility {
    fun showTimerPicker(
            pContext: Context?,
            pTimePickerInterface: TimePickerInterface,
            pDate: Date? = null,
            pSetMinimumDate: Boolean = false
    ) {
        if (pContext == null) {
            return
        }
        val calender = Calendar.getInstance()
        var mHourOfDay = calender.get(Calendar.HOUR_OF_DAY)
        var mMinute = calender.get(Calendar.MINUTE)

        if (pDate != null) {
            calender.time = pDate
            mHourOfDay = calender.get(Calendar.HOUR_OF_DAY)
            mMinute = calender.get(Calendar.MINUTE)
        }
        // Launch Time Picker Dialog
        val timePickerDialog = TimePickerDialog(
                pContext,
                { _, hourOfDay, minute ->
                    pTimePickerInterface.onTimeGet(hourOfDay, minute)
                },
                mHourOfDay,
                mMinute,
                false
        )
        if (pSetMinimumDate) {

        }
        timePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, pContext.getString(R.string.cancel)) { _, _ -> pTimePickerInterface.onTimePickerCancelListener() }
        timePickerDialog.setCancelable(false)
        timePickerDialog.show()
    }

    interface TimePickerInterface {
        fun onTimeGet(pHourOfDay: Int, pMinute: Int)
        fun onTimePickerCancelListener()
    }
}
