package com.app.pustakam.android.widgets.FAB

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import com.app.pustakam.android.MyApplicationTheme

@Composable
fun AddNewNoteFAB(modifier: Modifier = Modifier,onClick : ()-> Unit={}) {
    ExtendedFloatingActionButton(modifier = modifier,
        shape = CircleShape,
        containerColor = colorScheme.secondary,
        onClick = { onClick() },
        icon = { Icon(Icons.Filled.Edit, tint = colorScheme.primary,
            contentDescription = "Add A Quick note.") },
        text = { Text(text = "Quick note.", style = TextStyle(color = colorScheme.primary, )) },
    )
}

@Preview
@Composable
private fun Preview_AddFAB() {
    MyApplicationTheme {
        AddNewNoteFAB()
    }

}