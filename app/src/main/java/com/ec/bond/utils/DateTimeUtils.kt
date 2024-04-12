package com.ec.bond.utils

import android.annotation.SuppressLint
import com.ec.bond.common.Const
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.zone.ZoneRules
import java.util.*

object DateTimeUtils {

    @SuppressLint("SimpleDateFormat")
    fun getUTCDateFormat(): SimpleDateFormat {
        return SimpleDateFormat(Const.API_DATE_TIME_FORMAT).apply { timeZone =  TimeZone.getTimeZone("UTC")}
    }

    @SuppressLint("SimpleDateFormat")
    fun parseUTCDateFormat(inputDate: String, dateFormat: String = Const.API_DATE_TIME_FORMAT): Date? {
        return try {
            SimpleDateFormat(dateFormat).apply { timeZone =  TimeZone.getTimeZone("UTC")}.parse(inputDate)
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun parseTimeZoneDateFormat(inputDate: String, dateFormat: String = Const.API_DATE_TIME_FORMAT): Date? {
        return try {
            SimpleDateFormat(dateFormat).apply { timeZone =  TimeZone.getDefault()}.parse(inputDate)
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun formatUTCDateFormat(inputDate: Date, dateFormat: String = Const.API_DATE_TIME_FORMAT): String {
        return SimpleDateFormat(dateFormat).apply { timeZone =  TimeZone.getTimeZone("UTC")}.format(inputDate)
    }

    @SuppressLint("SimpleDateFormat")
    fun formatTimeZoneDateFormat(inputDate: Date, dateFormat: String = Const.API_DATE_TIME_FORMAT): String? {
        return SimpleDateFormat(dateFormat).apply { timeZone =  TimeZone.getDefault()}.format(inputDate)
    }

    @SuppressLint("SimpleDateFormat")
    fun getTimezoneDateFormat(): SimpleDateFormat {
        return SimpleDateFormat(Const.API_DATE_TIME_FORMAT).apply { timeZone =  TimeZone.getDefault()}
    }

    fun getTimeZoneSecond(): Int {
        val zone: ZoneId = ZoneId.systemDefault()
        val rules: ZoneRules = zone.rules
        val instant: Instant = Instant.now()
        val offset: ZoneOffset = rules.getOffset(instant)
        return offset.totalSeconds
    }

    fun getExactlyDate(inputDate: Date) : Date{
        val calendar = Calendar.getInstance()
        calendar.time = inputDate
        calendar.add(Calendar.SECOND, getTimeZoneSecond())
        return calendar.time
    }
}

fun Date.getDateInTimezone(): Date {
    val calendar = Calendar.getInstance()
    calendar.time = this
    calendar.add(Calendar.SECOND, DateTimeUtils.getTimeZoneSecond())
    return calendar.time
}