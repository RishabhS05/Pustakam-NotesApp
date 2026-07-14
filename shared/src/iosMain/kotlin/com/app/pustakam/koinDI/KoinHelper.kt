package com.app.pustakam.koinDI

import com.app.pustakam.data.localdb.preferences.BasePreferences
import com.app.pustakam.data.localdb.preferences.IAppPreferences
import com.app.pustakam.domain.repositories.base.BaseRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteContentRepository
import com.app.pustakam.domain.repositories.noteRepository.NoteRepository
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
object KoinHelper : KoinComponent{

    // 🔧 P0/4: repo getters deprecated — iOS is use-cases-only via the bridges now.
    //   Kept (not deleted) until you approve removal; getPreference stays (AuthBridge uses it).
    @Deprecated("Use NotesBridge/AuthBridge — repositories are not exposed to iOS anymore")
    fun getBaseRepository() = get<BaseRepository>()

    @Deprecated("Use NotesBridge — repositories are not exposed to iOS anymore")
    fun getNoteRepository() = get<NoteRepository>()

    @Deprecated("Use NoteContentBridge — repositories are not exposed to iOS anymore")
    fun getNoteContentRepository() = get<NoteContentRepository>()

    fun getPreference()= get<IAppPreferences>() as BasePreferences
}