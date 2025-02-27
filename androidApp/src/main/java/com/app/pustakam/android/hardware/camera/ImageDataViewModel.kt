package com.app.pustakam.android.hardware.camera

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

import org.koin.core.component.KoinComponent

class ImageDataViewModel: ViewModel() , KoinComponent {
private val _bitmaps =  MutableStateFlow<List<Bitmap>>(emptyList())
    val bitmaps = _bitmaps.asStateFlow()
    fun onTakePhoto(bitmap: Bitmap){
        _bitmaps.value+= bitmap
    }
}