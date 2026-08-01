package com.app.pustakam.android.screen.bookUIView

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.pustakam.android.widgets.audio.AudioPlayerUIState
import com.app.pustakam.core.filesys.reader.ReaderBlock

// 📖 01-Aug-2026: AudioPlayView is used AS IS — no compact variant. Several audio blocks share a
//   page purely because their estimated heights fit, never because the widget was shrunk.
@Composable
fun AudioBlockView(block: ReaderBlock.Audio, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        AudioPlayerUIState(block.item)
    }
}
