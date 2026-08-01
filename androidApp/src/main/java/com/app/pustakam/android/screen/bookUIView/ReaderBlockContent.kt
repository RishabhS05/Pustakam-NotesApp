package com.app.pustakam.android.screen.bookUIView

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.app.pustakam.core.filesys.reader.PageLayoutPolicy
import com.app.pustakam.core.filesys.reader.ReaderBlock
import com.app.pustakam.core.model.models.response.notes.NoteContentModel

// 📖 01-Aug-2026: block -> widget. Pure dispatch; it never decides WHAT goes on a page, only how a
//   block already placed by the shared engine draws itself.
@Composable
fun ReaderBlockContent(
    block: ReaderBlock,
    policy: PageLayoutPolicy,
    modifier: Modifier = Modifier,
    onOpenDocument: (NoteContentModel.MediaContent) -> Unit = {},
    onOpenImage: (NoteContentModel.MediaContent) -> Unit = {},
) {
    when (block) {
        is ReaderBlock.Title -> TitleBlockView(block, modifier)
        is ReaderBlock.Paragraph -> ParagraphBlockView(block, modifier)
        is ReaderBlock.ImageGrid -> ImageGridBlockView(block, policy, modifier, onOpenImage)
        is ReaderBlock.VideoGrid -> VideoGridBlockView(block, policy, modifier)
        is ReaderBlock.Audio -> AudioBlockView(block, modifier)
        is ReaderBlock.Document -> DocumentBlockView(block, modifier, onOpenDocument)
        is ReaderBlock.Link -> LinkBlockView(block, modifier)
        is ReaderBlock.Location -> LocationBlockView(block, modifier)
    }
}
