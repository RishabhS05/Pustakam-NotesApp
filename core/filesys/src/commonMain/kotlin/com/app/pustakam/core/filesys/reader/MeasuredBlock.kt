package com.app.pustakam.core.filesys.reader


/** Space a block occupies on the sheet. */
data class BlockSize(val width: Float, val height: Float) {
    companion object {
        val Zero = BlockSize(0f, 0f)
    }
}

/** A block plus everything the composer needs to place it. */
data class MeasuredBlock(
    val block: ReaderBlock,
    val size: BlockSize,
    /** Whether the block may continue on the next page. Media and audio never may. */
    val breakable: Boolean,
) {
    val height: Float get() = size.height
    val width: Float get() = size.width
}

/**
 * Stage 3 accumulator — the CONTAINER that reports what a page currently occupies, so the next
 * block can be tested against the remaining room before it is placed.
 */
data class PageBudget(
    val policy: PageLayoutPolicy,
    val occupied: BlockSize = BlockSize.Zero,
    val blockCount: Int = 0,
) {
    val remainingHeight: Float get() = policy.usableHeight - occupied.height

    val isEmpty: Boolean get() = blockCount == 0

    /** Gap that would precede the next block — none when the page is still empty. */
    val nextGap: Float get() = if (isEmpty) 0f else policy.blockGap

    /** Does [measured] still fit on this page? */
    fun fits(measured: MeasuredBlock): Boolean =
        measured.height + nextGap <= remainingHeight

    /** Height available to a block placed next, gap already deducted. */
    fun roomFor(): Float = (remainingHeight - nextGap).coerceAtLeast(0f)

    /** Place [measured] and return the new occupied size. */
    fun place(measured: MeasuredBlock): PageBudget = copy(
        occupied = BlockSize(
            width = maxOf(occupied.width, measured.width),
            height = occupied.height + nextGap + measured.height,
        ),
        blockCount = blockCount + 1,
    )

    /** Swift-facing factory — Kotlin default arguments are not exposed to Swift. */
    companion object {
        fun of(policy: PageLayoutPolicy): PageBudget = PageBudget(policy)
    }
}
