package com.app.pustakam.feature.auth.di

import com.app.pustakam.feature.auth.domain.repository.AuthRepository
import com.app.pustakam.feature.auth.domain.repository.IAuthRepository
import com.app.pustakam.feature.auth.domain.usecase.AppUserCase
import com.app.pustakam.feature.auth.domain.usecase.DeleteUserUseCase
import com.app.pustakam.feature.auth.domain.usecase.LoginUseCase
import com.app.pustakam.feature.auth.domain.usecase.LogoutUseCase
import com.app.pustakam.feature.auth.domain.usecase.ReadUserUseCase
import com.app.pustakam.feature.auth.domain.usecase.SignUseCase
import com.app.pustakam.feature.auth.domain.usecase.UpdateUserUseCase
import org.koin.core.module.Module
import org.koin.dsl.module

// 🔧 30-Jul-2026 02:10 — lifted VERBATIM out of :shared/koin/Koin.kt (the auth half of `useCases`)
fun authModule(): Module = module {
    single<IAuthRepository> { AuthRepository() }

    factory<LogoutUseCase> { LogoutUseCase() }
    factory<SignUseCase> { SignUseCase() }
    factory<LoginUseCase> { LoginUseCase() }
    factory<AppUserCase> { AppUserCase() }
    factory<DeleteUserUseCase> { DeleteUserUseCase() }
    factory<UpdateUserUseCase> { UpdateUserUseCase() }
    factory<ReadUserUseCase> { ReadUserUseCase() }
}
