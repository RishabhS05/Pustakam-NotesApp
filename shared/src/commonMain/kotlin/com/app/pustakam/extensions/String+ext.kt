package com.app.pustakam.extensions
fun Any?.isNotnull() = this!= null
fun String?.isValidEmail() = !this.isNullOrEmpty() && this.contains("@")

fun String?.isValidPhone() = !this.isNullOrEmpty() &&this.length > 7

fun String?.isValidName() =  this.isDigits() && this!!.length >= 3
fun String?.isDigits() =!this.isNullOrEmpty() && this.contains(regex = Regex("^[0-9*#+]$"))
fun String?.isValidPassword() = !this.isNullOrEmpty() && this.length >= 4