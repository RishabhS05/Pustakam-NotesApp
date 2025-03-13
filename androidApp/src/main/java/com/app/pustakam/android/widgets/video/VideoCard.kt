package com.app.pustakam.android.widgets.video

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.app.pustakam.android.MyApplicationTheme

@Composable
fun VideoCard(videoUrl :String ="", ) {
    Card(modifier = Modifier.clickable {  }
        .fillMaxWidth(.6f)
        .requiredHeight(350.dp)
        .padding(8.dp)) {
        //Preview Video

    }
}
@Preview("default")
@Preview("dark theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview("large font", fontScale = 2f)
@Composable
private fun VideoCardPreview() {
  /** App Theme */ /** View */
    MyApplicationTheme {
      /** View */ VideoCard()
    }
}