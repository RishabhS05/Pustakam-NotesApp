package com.app.pustakam.core.common.util


interface Error

fun Error.displayMessage(): String = when (this) {
    is NetworkError -> getError()
    is ValidationError -> getError()
    is ErrorMessage -> message
    else -> "Something went wrong!!"
}
