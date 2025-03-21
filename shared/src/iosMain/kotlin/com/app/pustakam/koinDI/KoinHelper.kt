package com.app.pustakam.koinDI

import com.app.pustakam.data.localdb.preferences.BasePreferences
import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.domain.repositories.base.BaseRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteContentRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
object KoinHelper : KoinComponent{
    fun getBaseRepository() = get<BaseRepository>()
    fun getNoteRepository() = get<NoteRepository>()
    fun getNoteContentRepository() = get<NoteContentRepository>()
    fun getPreference()= get<IAppPreferences>() as BasePreferences
}