package com.app.pustakam.koinDI

import com.app.pustakam.domain.repositories.BaseRepository
import com.app.pustakam.domain.repositories.noteContentRepo.NoteContentRepository
import org.koin.dsl.module

fun  repositoriesModules()= module{
    single <BaseRepository> { BaseRepository(get()) }
    single <NoteContentRepository>{ NoteContentRepository()  }
}
