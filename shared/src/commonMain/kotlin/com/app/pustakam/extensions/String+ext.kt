package com.app.pustakam.extensions
fun Any?.isNotnull() = this!= null
fun String?.isValidEmail() = !this.isNullOrEmpty() && this.contains("@")

fun String?.isValidPhone() = this.isDigits() && this!!.length > 7

fun String?.isValidName() = !this.isNullOrEmpty()  && this.length >= 3
fun String?.isDigits() =!this.isNullOrEmpty()
       && (this.contains(regex = Regex("^[\\d\t*#+]+$")))
fun String?.isValidPassword() = !this.isNullOrEmpty() && this.length >= 4