package com.app.pustakam.android.widgets.image

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme
import com.app.pustakam.android.widgets.LoadImage

@Composable
fun ImageCard(modifier: Modifier = Modifier,
              imageUrl : String = "", onClick: ()-> Unit) {
    Card(modifier = Modifier.fillMaxWidth(0.6f).requiredHeight(350.dp).clickable { onClick() }.padding(8.dp)) {
        Box(modifier= Modifier.fillMaxSize()){
            LoadImage( url =  imageUrl, modifier=modifier.matchParentSize())
        }
    }
}
@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
private fun ImageCardPreview() {
    /** App Theme */ /** View */
    MyApplicationTheme {
        /** View */ /** View */ ImageCard(){}
    }
}