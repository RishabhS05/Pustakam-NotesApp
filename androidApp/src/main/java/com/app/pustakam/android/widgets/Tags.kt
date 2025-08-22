package com.app.pustakam.android.widgets

import android.content.res.Configuration
import android.provider.CalendarContract.Colors
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import bookmarkDefault
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.data.models.Tag
import toColor
import toHexString


@Composable
fun TagView(modifier: Modifier = Modifier, tag : Tag ) {
    val color = tag.color?.toColor()?: bookmarkDefault
    val roundShape=RoundedCornerShape(20)
    Row(modifier = modifier.
background(color.copy(alpha = .1f),shape= roundShape).
    border( shape = roundShape,
        border = BorderStroke(
            1.dp,
            color = color.copy(alpha = 0.7f)
        ),
    ).padding(vertical = 4.dp, horizontal = 8.dp),
       verticalAlignment = Alignment.CenterVertically,
        ){
        //color conversion from string to color
   Icon(imageVector = Icons.Filled.Book,
       contentDescription = "Bookmark",
       tint =  color, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.size(4.dp))
        Text(tag.label?: "Unknown",
            style = TextStyle(color = color,
                textAlign = TextAlign.Center, fontSize = 14.sp) )
    }
}
        @Preview("default")
        @Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
        @Preview("large font", fontScale = 2f)
        @Composable
        private fun TagUIPreview() {
    /** App Theme */
    MyApplicationTheme {
        /** View */
       TagView(tag = Tag(id = "111", color = bookmarkDefault.toHexString(), label = "Maths"))

    }
}