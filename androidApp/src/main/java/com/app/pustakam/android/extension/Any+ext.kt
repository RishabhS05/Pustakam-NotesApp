package com.app.pustakam.android.extension

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource


// modify type accordingly to your need
@Composable
fun Any.toImageVector() : ImageVector =  when(this) {
         is ImageVector ->   this
         is Int -> ImageVector.vectorResource(id = this)
       else -> Icons.Default.BrokenImage // place holder to protect from crashing or exception
    }
