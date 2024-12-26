package com.app.pustakam.android.permission

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PermissionViewModel : ViewModel() {
private  val _showDialog  = MutableStateFlow(false)
    val showDialog = _showDialog.asStateFlow()
    private val _launchAppSettings = MutableStateFlow(false)
    val launchAppSettings = _launchAppSettings.asStateFlow()
    fun updateShowDisplay(value  : Boolean){
        _showDialog.update { value }
    }
    fun updateLaunchSettings(value  : Boolean){
        _launchAppSettings.update { value   }
    }
}