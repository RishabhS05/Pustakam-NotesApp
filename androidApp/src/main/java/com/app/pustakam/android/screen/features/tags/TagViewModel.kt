package com.app.pustakam.android.screen.features.tags

import androidx.lifecycle.viewModelScope
import com.app.pustakam.android.screen.BOOKS
import com.app.pustakam.android.screen.DialogEnum
import com.app.pustakam.android.screen.TagState
import com.app.pustakam.android.screen.TaskCode
import com.app.pustakam.android.screen.base.BaseViewModel
import com.app.pustakam.domain.repositories.usecases.CreateTagUseCase
import com.app.pustakam.domain.repositories.usecases.GetTagCase
import com.app.pustakam.domain.repositories.usecases.UpdateTagUseCase
import com.app.pustakam.android.screen.notes.list.TagIntent
import com.app.pustakam.data.models.BaseResponse
import com.app.pustakam.data.models.Tag
import com.app.pustakam.util.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class TagViewModel : BaseViewModel() {
    private val getTagsCase = GetTagCase()
    private val createTagUseCase = CreateTagUseCase()
    private val updateTagUseCase = UpdateTagUseCase()
    private val _tagsUiState = MutableStateFlow(TagState())
    val tagsUiState = _tagsUiState.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5000L),
        _tagsUiState.value
    )
init {
    getTags()
}
    fun getTags(){
        viewModelScope.launch {
            println("setTags() called") // Debug log
            makeAWish(BOOKS.GET_BOOKS) {
                getTagsCase.invoke()
            }
        }
    }
    fun onTagIntent(intent : TagIntent){
        when(intent){
            is TagIntent.OnTagClick -> {
            }
            is TagIntent.OnCreateTag -> {
                createTag(intent.tag)
            }
            is TagIntent.ShowOrHideUIAlerts -> {
                _tagsUiState.update {
                    it.copy(dialog = intent.dialog)
                }
            }
            else-> {}
        }
    }
    override fun onSuccess(
        taskCode: TaskCode,
        result: Result.Success<BaseResponse<*>>
    ) {
when (taskCode){
    BOOKS.GET_BOOKS -> {
        val tags  = result.data.data as? ArrayList<Tag> ?: arrayListOf()
        _tagsUiState.update {
            it.copy(tags = tags)
        }
    }
    BOOKS.ADD_BOOK -> {
        val tag= result.data.data as? Tag ?: return
        _tagsUiState.update {
            it.copy(tags = ArrayList<Tag>(it.tags+tag), dialog = DialogEnum.NONE)
        }
    }
} }


    fun createTag(tag: Tag) {
        if (tag.label.isNullOrEmpty())  return
        viewModelScope.launch {
            makeAWish(taskCode = BOOKS.ADD_BOOK) {
                createTagUseCase.invoke(tag)
            }
        }
    }
    fun updateTag(tag: Tag) {
        viewModelScope.launch {
            updateTagUseCase.invoke(tag)
        }
        _tagsUiState.update {
            it.copy(tags = it.tags.apply {
                val index = indexOfFirst { it.id == tag.id }
                if (index != -1) set(index, tag)
            })
        }
    }
    override suspend fun logoutUserForcefully() {}

    override fun clearError() {
    }
}