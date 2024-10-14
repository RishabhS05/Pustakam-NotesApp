package com.app.pustakam.android.screen

import androidx.lifecycle.ViewModel
import com.app.pustakam.domain.repositories.BaseRepository

open class BaseViewModel : ViewModel() {
    val baseRepository : BaseRepository = BaseRepository()
}