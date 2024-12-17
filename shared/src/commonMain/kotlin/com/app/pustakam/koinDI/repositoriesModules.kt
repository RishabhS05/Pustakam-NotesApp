package com.app.pustakam.koinDI

import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.domain.repositories.BaseRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.dsl.module

fun  repositoriesModules()= module{
    single <BaseRepository> { BaseRepository(get()) }
}
