package com.app.pustakam.core.data.di

import com.app.pustakam.core.common.events.DomainEventBus
import com.app.pustakam.core.user.UserSession
import org.koin.core.module.Module
import org.koin.dsl.module

fun coreDataModule(): Module = module {
    single { UserSession(get()) }
    single { DomainEventBus() }
}
