package com.ec.bond.utils
import java.text.SimpleDateFormat
import java.util.*

enum class DateStyle {
    short, medium, long, full, default
}

enum class TimeStyle {
    short, medium, long
}


/**
 * Date string from date.
 *
 *      Date().dateString(ofStyle: .short) -> "1/12/17"
 *      Date().dateString(ofStyle: .medium) -> "Jan 12, 2017"
 *      Date().dateString(ofStyle: .long) -> "January 12, 2017"
 *      Date().dateString(ofStyle: .full) -> "Thursday, January 12, 2017"
 *      Date().dateString(ofStyle: .default) -> "2017-12-01 20:32:10"
 *
 *  @param DateStyle
 *
 *  @return date string
 */
fun Date.dateString(style: DateStyle = DateStyle.medium) : String {
    return when (style) {
        DateStyle.default -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(this)
        DateStyle.short -> SimpleDateFormat("dd-MM-yy", Locale.getDefault()).format(this)
        DateStyle.medium -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(this)
        DateStyle.long -> SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(this)
        DateStyle.full -> SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault()).format(this)
    }
}

fun Date.timeString(style: TimeStyle = TimeStyle.medium) : String {
    return when (style) {
        TimeStyle.short -> SimpleDateFormat("hh:mm a", Locale.getDefault()).format(this)
        TimeStyle.medium -> SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(this)
        TimeStyle.long -> SimpleDateFormat("hh:mm:ss", Locale.getDefault()).format(this)
    }
}




/**
 * Pattern: yyyy-MM-dd HH:mm:ss
 */
fun Date.formatToServerDateTimeDefaults(): String{
    val sdf= SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(this)
}

fun Date.formatToTruncatedDateTime(): String{
    val sdf= SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault())
    return sdf.format(this)
}

/**
 * Pattern: yyyy-MM-dd
 */
fun Date.formatToServerDateDefaults(): String{
    val sdf= SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(this)
}

/**
 * Pattern: HH:mm:ss
 */
fun Date.formatToServerTimeDefaults(): String{
    val sdf= SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(this)
}

/**
 * Pattern: dd/MM/yyyy HH:mm:ss
 */
fun Date.formatToViewDateTimeDefaults(): String{
    val sdf= SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(this)
}

/**
 * Pattern: dd/MM/yyyy
 */
fun Date.formatToViewDateDefaults(): String{
    val sdf= SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    return sdf.format(this)
}

/**
 * Pattern: HH:mm:ss
 */
fun Date.formatToViewTimeDefaults(): String{
    val sdf= SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return sdf.format(this)
}

/**
 * Add field date to current date
 */
fun Date.add(field: Int, amount: Int): Date {
    Calendar.getInstance().apply {
        time = this@add
        add(field, amount)
        return time
    }
}

fun Date.addYears(years: Int): Date{
    return add(Calendar.YEAR, years)
}
fun Date.addMonths(months: Int): Date {
    return add(Calendar.MONTH, months)
}
fun Date.addDays(days: Int): Date{
    return add(Calendar.DAY_OF_MONTH, days)
}
fun Date.addHours(hours: Int): Date{
    return add(Calendar.HOUR_OF_DAY, hours)
}
fun Date.addMinutes(minutes: Int): Date{
    return add(Calendar.MINUTE, minutes)
}
fun Date.addSeconds(seconds: Int): Date{
    return add(Calendar.SECOND, seconds)
}
