package com.app.pustakam.extensions

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun Any?.isNotnull() = this != null
fun String?.isValidEmail() = !this.isNullOrEmpty() && this.contains("@")

fun String?.isValidPhone() = this.isDigits() && this!!.length > 7

fun String?.isValidName() = !this.isNullOrEmpty() && this.length >= 3
fun String?.isDigits() = !this.isNullOrEmpty() && (this.contains(regex = Regex("^[\\d\t*#+]+$")))
fun String?.isValidPassword() = !this.isNullOrEmpty() && this.length >= 4

fun String?.isUrl()= !this.isNullOrEmpty() && (this.startsWith("http://") || this.startsWith("https://"))
fun String.toLocalFormat(timeZone: TimeZone = TimeZone.currentSystemDefault(), showTime: Boolean= true): String {
    val instant: Instant = if (this.isDigits()) Instant.fromEpochMilliseconds(this.toLong())
    else Instant.parse(this)

    val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val char = '0'
    val day = dateTime.date.dayOfMonth.toString().padStart(2, char)
    val month = dateTime.date.monthNumber.toString().padStart(2, char)
    val year = dateTime.date.year
    val hour = if (dateTime.hour % 12 == 0) 12 else dateTime.hour % 12
    val minute = dateTime.minute.toString().padStart(2, char)
    val amPm = if (dateTime.hour < 12) "am" else "pm"

    return if (showTime)"$day/$month/$year $hour:$minute $amPm" else "$day/$month/$year "
}


