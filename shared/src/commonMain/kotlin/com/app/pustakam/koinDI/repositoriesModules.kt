package com.app.pustakam.koinDI

import com.app.pustakam.domain.repositories.BaseRepository
import org.koin.dsl.module

fun  repositoriesModules()= module{
    single <BaseRepository> { BaseRepository(get()) }
}
