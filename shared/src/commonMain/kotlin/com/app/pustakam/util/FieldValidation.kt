package com.app.pustakam.util

import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isValidEmail
import com.app.pustakam.extensions.isValidName
import com.app.pustakam.extensions.isValidPassword
import com.app.pustakam.extensions.isValidPhone
import io.ktor.http.HttpHeaders.Date


fun isPasswordEqualsToConfirmPassword(password: String?, confirmPassword : String? ) = !confirmPassword.isNullOrEmpty() && !password.isNullOrEmpty() && password == confirmPassword
fun checkLoginEmailPasswordValidity(req: Login): ValidationError = when {
    !req.email.isValidEmail() ->  ValidationError.EMAIL
        !req.password.isValidPassword() -> ValidationError.PASSWORD
        else -> ValidationError.NONE
    }

fun checkRegisterFieldsValidity(req : RegisterReq) : ValidationError
= when {
    !req.name.isValidName() -> ValidationError.NAME
    !req.email.isValidEmail() -> ValidationError.EMAIL
    !req.phone.isValidPhone() ->ValidationError.PHONE
    !req.password.isValidPassword() ->ValidationError.PASSWORD
    !isPasswordEqualsToConfirmPassword(req.passwordConfirm,req.password) -> ValidationError.PASSWORD_NOT_MATCHED
    else ->  ValidationError.NONE
}

