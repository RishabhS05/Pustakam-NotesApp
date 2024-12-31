package com.app.pustakam.android.widgets.fabWidget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.R
import orange50
import orange80

@Composable
fun OverLayEditorButtons(
    modifier: Modifier = Modifier,
    offset: Animatable<Float, AnimationVector1D> = remember { Animatable(initialValue = 0f) },
    onAddTextField : ()-> Unit ={},
    onRecordVideo: () -> Unit = {},
    onAddImage: () -> Unit = {},
    onRecordMic: () -> Unit = {},
    onArrowButton: ()-> Unit ={}
) {
    var showArrow = remember { mutableStateOf(false) }
    val cardColors = CardDefaults.cardColors(
        contentColor = orange80,
        containerColor = colorScheme.secondary
    )
    val iconModifier = Modifier.padding(6.dp)
    val icon = if (showArrow.value) Icons.AutoMirrored.Filled.KeyboardArrowLeft
    else Icons.AutoMirrored.Filled.KeyboardArrowRight
    val cardElevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    if (showArrow.value)
        Card(
            modifier = modifier,
            shape = CardDefaults.elevatedShape,
            elevation = CardDefaults.cardElevation(),
            colors = CardDefaults.cardColors(containerColor = orange50.copy(alpha = 0.4f))
        ) {
            Icon(icon,
                contentDescription = "",
                modifier = Modifier
                    .padding(start = 4.dp, top = 4.dp)
                    .clickable {
                        onArrowButton()
                        showArrow.value = !showArrow.value
                    })
            Column(
                modifier = Modifier
                    .padding(2.dp)
                    .offset(x = offset.value.dp)
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Card(
                    onClick = {
                        onArrowButton()
                        onRecordMic()
                              },
                    colors = cardColors,
                    elevation = cardElevation
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_mic),
                        contentDescription = "Mic",
                        modifier = iconModifier
                    )
                }
                Card(
                    onClick = {
                        onArrowButton()
                        onAddImage()
                             },
                    colors = cardColors,
                    elevation = cardElevation
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_add_photo),
                        contentDescription = "Add Photo",
                        modifier = iconModifier
                    )
                }
                Card(
                    onClick = {
                        onArrowButton()
                        onRecordVideo()
                              },
                    colors = cardColors,
                    elevation = cardElevation
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_video_file),
                        contentDescription = "Save a  Video",
                        modifier = iconModifier
                    )
                }
                Card(
                    onClick = {
                        onArrowButton()
                      onAddTextField()
                    },
                    colors = cardColors,
                    elevation = CardDefaults.elevatedCardElevation()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.text_fields),
                        contentDescription = "Add new text note",
                        modifier = iconModifier
                    )
                }
            }
        }
    else
        Icon(icon, contentDescription = "",
            tint = colorScheme.secondary,
            modifier = modifier
                .padding(8.dp)
                .clickable {
                    onArrowButton()
                    showArrow.value = !showArrow.value
                })

}

@Preview
@Composable
private fun OverLayEditorButtons_Prev() {
    MyApplicationTheme { OverLayEditorButtons() }
}