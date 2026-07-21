

package com.app.pustakam.android.screen.search

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import com.app.pustakam.android.MyApplicationTheme

@Composable
fun SearchBar(
    text: String,
    placeholder: String,
    modifier: Modifier = Modifier,
    showMicrophone: Boolean = true,
    onQueryChange: (String) -> Unit,
    isSearching: Boolean = false,
    onClick: () -> Unit,
    onMicrophoneClick: (() -> Unit)? = null
) {

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(50),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 5.dp
        )
    ) {
        TextField(
                value = text,
                onValueChange = onQueryChange,
                placeholder = { Text(text = placeholder) },
                leadingIcon = { Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search",
                    modifier = Modifier.sizeIn(52.dp,52.dp,60.dp,60.dp ).padding(12.dp)

                ) },
                trailingIcon = {
                    if (isSearching) CircularProgressIndicator(
                        strokeWidth = 2.dp, modifier = Modifier.padding(12.dp)
                    )
                   else if (showMicrophone) {
                        Icon(
                            imageVector = Icons.Outlined.Mic,
                            contentDescription = "Voice Search",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable {
                                onMicrophoneClick?.invoke()
                            }.padding(horizontal = 24.dp)
                        )
                    }
                },
                maxLines = 1,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent
                ) ,
                modifier = Modifier.fillMaxWidth(),
            )


    }
}
@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)

@Composable
private fun NotesPreview() {
    MyApplicationTheme {
        SearchBar("Hello world", "Search", onClick = {}, showMicrophone = true, onQueryChange = {})
    }
}
