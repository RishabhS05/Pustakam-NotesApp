package com.app.pustakam.android.widgets.video

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.app.pustakam.android.MyApplicationTheme

@Composable
fun VideoCard() {

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