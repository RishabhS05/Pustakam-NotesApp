package com.app.pustakam.android.widgets


import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

import coil.compose.AsyncImage
import coil.request.ImageRequest

import com.app.pustakam.android.R


@Composable
fun IconLoad(url: String? = null,
             placeHolderDrawable: Int = R.drawable.avatar,
             modifier: Modifier = Modifier,
             onClick :  ()-> Unit ) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        placeholder = painterResource(placeHolderDrawable),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .border(shape = RoundedCornerShape(50),
            border = BorderStroke(4.dp,
            color= MaterialTheme.colorScheme.secondary))
            .clip(CircleShape)
            .size(250.dp)
    )
}

@Preview
@Composable
private fun SignUp() {
    IconLoad(placeHolderDrawable =  R.drawable.avatar){}
}