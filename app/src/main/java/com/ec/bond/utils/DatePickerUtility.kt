package com.ec.bond.utils

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Context
import android.content.DialogInterface
import com.ec.bond.R
import java.util.*

object DatePickerUtility {
    @SuppressLint("SetTextI18n")
    fun showDatePicker(
            pContext: Context?,
            pDatePickerInterface: DatePickerInterface,
            pDate: Date? = null,
            pSetMinimumDate: Boolean = false
    ) {
        if (pContext == null) {
            return
        }
        val calender = Calendar.getInstance()
        val calendarNew = Calendar.getInstance()
        var year = calender.get(Calendar.YEAR)
        var month = calender.get(Calendar.MONTH)
        var day = calender.get(Calendar.DAY_OF_MONTH)

        calendarNew.set(Calendar.DAY_OF_MONTH, day)
        if (pDate != null) {
            calender.time = pDate
            year = calender.get(Calendar.YEAR)
            month = calender.get(Calendar.MONTH)
            day = calender.get(Calendar.DAY_OF_MONTH)
        }
        var selectDate = false
        val datePickerDialog = DatePickerDialog(pContext, { _, yearGet, monthOfYear, dayOfMonth ->
            var monthValue = monthOfYear
            monthValue++
            selectDate = true
            pDatePickerInterface.onDateGet(dayOfMonth, yearGet, monthValue)
        },
                year,
                month,
                day
        )
        if (pSetMinimumDate) {
            datePickerDialog.datePicker.minDate = calendarNew.timeInMillis
        }
        datePickerDialog.setButton(DialogInterface.BUTTON_NEGATIVE, pContext.getString(R.string.cancel)) { _, _ -> pDatePickerInterface.onDatePickerCancelListener() }
        datePickerDialog.setOnDismissListener {
            if (selectDate) {
                return@setOnDismissListener
            }
            pDatePickerInterface.onDatePickerCancelListener()
        }
        datePickerDialog.show()
    }

    interface DatePickerInterface {
        fun onDateGet(pDayOfMonth: Int, pYear: Int, pMonth: Int)
        fun onDatePickerCancelListener()
    }
}