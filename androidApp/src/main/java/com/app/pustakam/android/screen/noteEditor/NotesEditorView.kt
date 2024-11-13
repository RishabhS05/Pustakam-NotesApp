package com.app.pustakam.android.screen.noteEditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.app.pustakam.android.screen.OnLifecycleEvent
import com.app.pustakam.android.widgets.LoadingUI
import com.app.pustakam.android.widgets.SecondaryTextButton
import com.app.pustakam.android.widgets.SnackBarUi
import com.app.pustakam.extensions.isNotnull


@Composable
fun NotesEditorView(
    id : String? = "",
    onBack:()->Unit = {},

){
   val noteEditorViewModel  : NoteEditorViewModel = viewModel()
    NotesEditor(id= id, onBack=onBack, noteEditorViewModel= noteEditorViewModel)
}

@Composable
fun NotesEditor(
    id : String? = "",
    onBack:()->Unit = {},
    noteEditorViewModel  : NoteEditorViewModel = viewModel()
) {

    // Note content state
    val textState = remember { mutableStateOf("") }
    val textTitleState = remember { mutableStateOf("") }
    val isRuledEnabledState =  remember { mutableStateOf(false) }
  val appState =  noteEditorViewModel.noteUIState.collectAsState().value.apply {
      when{
          isLoading -> LoadingUI()
          error.isNotnull() -> SnackBarUi(error = error!!) {
              noteEditorViewModel.clearError()
          }
      }
      when(noteStatus){
          NoteStatus.onBackPress -> {
              noteEditorViewModel.createOrUpdate( id= id,
                  title =  textTitleState.value,
                  body = textState.value,
                  note = note)
          }
          NoteStatus.onSaveCompletedExit -> {
              onBack()
          }
          NoteStatus.OnEditingMode ->{}
          NoteStatus.onSaveCompleted -> {}
          else -> {}
      }
  }
    OnLifecycleEvent { owner, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                if (!id.isNullOrEmpty()) noteEditorViewModel.readFromDataBase(id)
            }
            Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                noteEditorViewModel.changeNoteStatus(NoteStatus.onBackPress)
            }
            Lifecycle.Event.ON_DESTROY -> {
                //sink with server
            }
            else -> {}
        }
    }
    LaunchedEffect(appState.note, appState.isSetupValues) {
        if(appState.note.isNotnull() && appState.isSetupValues) {
        textTitleState.value = appState.note!!.title.toString()
        textState.value = appState.note.description.toString()
        appState.isSetupValues =false
    }
    }
    val paddingLeft = if(isRuledEnabledState.value) 100.dp else 12.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(4.dp)
    ) {
        if (isRuledEnabledState.value) RuledPage()
        Column {
            Row {
                TextField(
                    value = textTitleState.value, placeholder = {
                        Text(
                            "Title : Keep your thoughts alive.", modifier = Modifier.padding(start = paddingLeft),
                            style = TextStyle(
                                color = Color.DarkGray,
                                fontSize = 18.sp,
                            )
                        )
                    }, colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                    ), onValueChange = {
                        textTitleState.value = it
                    }, textStyle = TextStyle(
                        color = Color.Black, fontSize = 24.sp
                    ), modifier = Modifier.weight(1f)
                        .padding(top = 2.dp)
                )
                SecondaryTextButton(
                    label = "Done", modifier = Modifier.padding(top = 8.dp).height(40.dp).weight(.5f)
                ){
                    noteEditorViewModel.createOrUpdate( id= id,
                        title =  textTitleState.value,
                        body = textState.value,
                        note = appState.note)
                }
            }


            TextField(
                value = textState.value, onValueChange = {
                    textState.value = it
                },
                placeholder = {
                    Text(
                        "Hi, whats in your mind take a quick note, before it get lost.",
                        modifier = Modifier.padding(start = paddingLeft),
                        style = TextStyle(
                            color = Color.Gray, fontSize = 14.sp, lineHeight = 32.sp
                        )
                    )
                }, textStyle = TextStyle(
                    color = Color.Black, fontSize = 18.sp, lineHeight = 32.sp
                ), colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                ), modifier = Modifier.fillMaxSize() // Padding to simulate left margin
            )

        }
    }
}

@Composable
fun RuledPage() {
    val lineColor = Color.LightGray
    val marginColor = Color.Red
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineSpacing = 35.dp.toPx()
        val startX = 80.dp.toPx()

        var y = lineSpacing + 80
        while (y < size.height) {
            drawLine(
                color = lineColor, start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1.dp.toPx()
            )
            y += lineSpacing
        }

        drawLine(
            color = marginColor, start = androidx.compose.ui.geometry.Offset(startX, 0f),
            end = androidx.compose.ui.geometry.Offset(startX, size.height), strokeWidth = 2.dp.toPx()
        )
        drawLine(
            color = marginColor, start = androidx.compose.ui.geometry.Offset(startX + 20f, 0f),
            end = androidx.compose.ui.geometry.Offset(startX + 20f, size.height), strokeWidth = 2.dp.toPx()
        )
    }
}


@Preview
@Composable
private fun NoteEditorPreview() {
    NotesEditorView()
}