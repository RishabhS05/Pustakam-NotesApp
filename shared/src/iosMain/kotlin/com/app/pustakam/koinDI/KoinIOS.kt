package com.app.pustakam.koinDI

import org.koin.core.KoinApplication
import org.koin.core.context.startKoin

object KoinIOS {
    fun initialize(): KoinApplication = startKoin{}
}