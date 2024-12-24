package com.app.pustakam.android.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.app.pustakam.android.R

@Composable
fun CircleIconLoad(url: String? = null,
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
            .border(
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    4.dp,
                    color = MaterialTheme.colorScheme.secondary
                )
            )
            .clip(CircleShape)
            .size(200.dp)
            .clickable { onClick() }
    )
}


@Composable
fun LoadImage( url: String,
               placeHolderDrawable: Int = R.drawable.ic_add_photo,
               modifier: Modifier = Modifier,
               ) {
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            .crossfade(true)
            .build(),
        placeholder = painterResource(placeHolderDrawable),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxSize()
    )
}

@Preview
@Composable
private fun LoadImagePrev() {
    LoadImage(placeHolderDrawable = R.drawable.avatar, url = "https://picsum.photos/seed/picsum/200/300", modifier = Modifier)
}
@Preview
@Composable
private fun SignUp() {
    CircleIconLoad(placeHolderDrawable =  R.drawable.avatar){}
}