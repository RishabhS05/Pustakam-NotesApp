package com.app.pustakam.core.data.di

import com.app.pustakam.core.user.UserSession
import org.koin.core.module.Module
import org.koin.dsl.module

fun coreDataModule(): Module = module {
    single { UserSession(get()) }
}
