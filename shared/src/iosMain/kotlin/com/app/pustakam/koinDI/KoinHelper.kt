package com.app.pustakam.koinDI

import com.app.pustakam.domain.repositories.BaseRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get


object KoinHelper : KoinComponent{
    fun getBaseRepository() = get<BaseRepository>()
}