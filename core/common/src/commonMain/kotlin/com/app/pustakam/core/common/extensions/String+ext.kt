package com.app.pustakam.core.common.extensions

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

inline fun Any?.isNotnull() = this != null
fun String?.isValidEmail() = !this.isNullOrEmpty() && this.contains("@")

// 📞 28-Aug-2026 — "+91 98765-43210" and "919876543210" are the SAME number. Sign-up and sign-in
//   are two different screens and people do not type a number the same way twice, so the shape is
//   stripped before the number is validated, sent or matched. A leading + is kept: it is the only
//   part that carries meaning.
fun String?.normalizedPhone(): String {
    val raw = this?.trim().orEmpty()
    if (raw.isEmpty()) return ""
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    return if (raw.startsWith("+")) "+$digits" else digits
}

// 📞 deliberately NOT isDigits(): that one also decides whether a timestamp string is epoch millis,
//   and loosening it there would send "12 34" to toLong(). Phone shapes are this function's own.
fun String?.isValidPhone(): Boolean {
    val digits = this.normalizedPhone().filter { it.isDigit() }
    return digits.length in 7..15
}

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


