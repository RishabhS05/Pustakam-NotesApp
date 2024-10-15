package com.app.pustakam.util

import com.app.pustakam.data.models.request.Login
import com.app.pustakam.data.models.request.RegisterReq
import com.app.pustakam.extensions.isValidEmail
import com.app.pustakam.extensions.isValidName
import com.app.pustakam.extensions.isValidPassword


fun comparePassword(password: String?, confirmPassword : String? ) = !confirmPassword.isNullOrEmpty() && !password.isNullOrEmpty() && password == confirmPassword
fun checkLoginEmailPasswordValidity(req: Login): Pair<Boolean, ValidationError> = when {
    !req.email.isValidEmail() -> Pair(false, ValidationError.EMAIL)
        !req.password.isValidPassword() -> Pair(false, ValidationError.PASSWORD)
        else -> Pair(true, ValidationError.NONE)
    }

fun checkRegisterFieldsValidity(req : RegisterReq) : Pair<Boolean, ValidationError>
= when {
    !req.name.isValidName() -> Pair(false,ValidationError.NAME )
    !req.email.isValidEmail() -> Pair(false,ValidationError.EMAIL )
    !req.phone.isValidName() -> Pair(false,ValidationError.PHONE )
    !req.password.isValidPassword() -> Pair(false,ValidationError.PASSWORD )
    !comparePassword(req.passwordConfirm,req.password) -> Pair(false,ValidationError.PASSWORD_NOT_MATCHED )
    else -> Pair(true, ValidationError.NONE)
}