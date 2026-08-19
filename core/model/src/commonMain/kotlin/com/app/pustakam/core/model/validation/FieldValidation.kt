package com.app.pustakam.core.model.validation

import com.app.pustakam.core.model.models.request.Login
import com.app.pustakam.core.model.models.request.RegisterReq
import com.app.pustakam.core.common.extensions.isValidEmail
import com.app.pustakam.core.common.extensions.isValidName
import com.app.pustakam.core.common.extensions.isValidPassword
import com.app.pustakam.core.common.extensions.isValidPhone
import com.app.pustakam.core.common.util.ValidationError


fun isPasswordEqualsToConfirmPassword(password: String?, confirmPassword : String? ) = !confirmPassword.isNullOrEmpty() && !password.isNullOrEmpty() && password == confirmPassword
// 🔧 19-Aug-2026 — accepts either a valid email or a valid phone number as the identifier
fun checkLoginEmailPasswordValidity(req: Login): ValidationError = when {
    !(req.email.isValidEmail() || req.phone.isValidPhone()) -> ValidationError.EMAIL
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

