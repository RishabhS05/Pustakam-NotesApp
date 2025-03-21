package com.app.pustakam.koinDI

import com.app.pustakam.domain.repositories.base.BaseRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteContentRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteRepository
import org.koin.dsl.module

fun  repositoriesModules()= module{
    single <BaseRepository> { BaseRepository(get()) }
    single <NoteContentRepository>{ NoteContentRepository()  }
    single <NoteRepository>{ NoteRepository(get())  }
}
