package com.app.pustakam.android.screen.notes.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.screen.navigation.Screen
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.data.models.response.notes.Note
import com.app.pustakam.extensions.isNotnull

@Composable
fun NotesView() {
    val notesViewModel : NotesViewModel = viewModel()

     notesViewModel.notesUIState.collectAsState().value.apply{
         OnLifecycleEvent { _ , event ->
             when(event){
                 Lifecycle.Event.ON_START -> if(notes.isEmpty()) notesViewModel.getNotes()
                 else -> {}
             }
         }
         when {
             error.isNotnull() -> SnackBarUi(error = error!!) {
                 notesViewModel.clearError()
             }
             notes.isNotEmpty() ->
                 Column(modifier = Modifier
                     .fillMaxSize()
                     .background(color = Color.LightGray), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                     notes.forEach{ note ->
                         Text(text = "${note.title}", style = TextStyle(color = Color.Black, fontSize = 30.sp))
                     }
                 }
             isLoading -> LoadingUI()
         }
     }

}