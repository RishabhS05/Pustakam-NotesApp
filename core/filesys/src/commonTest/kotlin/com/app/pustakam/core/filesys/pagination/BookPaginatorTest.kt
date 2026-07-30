package com.app.pustakam.core.filesys.pagination

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 🔧 30-Jul-2026 02:10 Phase 2 — pins book pagination.
//   progressPage/totalPages are PERSISTED per MediaContent. If any expectation here changes, every
//   saved reading position on every device moves. Treat a failure as a data-migration problem, not
//   a test problem.
class BookPaginatorTest {

    // ---- the constants are part of the contract ----

    @Test
    fun chars_per_page_is_700_on_both_platforms() {
        // androidApp CHARS_PER_BOOK_PAGE = 700, iOS charsPerPage = 700. Do not "tune" this.
        assertEquals(700, BookPaginator.CHARS_PER_PAGE)
    }

    @Test
    fun text_file_cap_is_two_megabytes() {
        assertEquals(2L * 1024 * 1024, BookPaginator.MAX_TEXT_FILE_BYTES)
    }

    // ---- empty / short input ----

    @Test
    fun blank_text_produces_no_pages_at_all() {
        // An empty book, not a blank page — the reader relies on this.
        assertTrue(BookPaginator.paginate("").isEmpty())
        assertTrue(BookPaginator.paginate("     ").isEmpty())
        assertTrue(BookPaginator.paginate("\n\n").isEmpty())
    }

    @Test
    fun text_shorter_than_a_page_is_one_page() {
        val pages = BookPaginator.paginate("hello world")
        assertEquals(1, pages.size)
        assertEquals("hello world", pages[0].text)
        assertEquals(1, pages[0].pageNumber)
        assertEquals(1, pages[0].totalPages)
    }

    @Test
    fun text_exactly_one_page_long_is_not_split() {
        assertEquals(1, BookPaginator.paginate("a".repeat(700)).size)
    }

    @Test
    fun text_one_char_over_a_page_becomes_two_pages() {
        assertEquals(2, BookPaginator.paginate("a".repeat(701)).size)
    }

    // ---- word-boundary cutting ----

    @Test
    fun a_newline_past_the_halfway_mark_wins() {
        val text = "a".repeat(400) + "\n" + "b".repeat(400)
        val pages = BookPaginator.paginate(text)
        assertEquals(2, pages.size)
        assertEquals("a".repeat(400), pages[0].text)
        assertEquals("b".repeat(400), pages[1].text)
    }

    @Test
    fun a_space_past_the_halfway_mark_wins_when_there_is_no_newline() {
        val text = "a".repeat(400) + " " + "b".repeat(400)
        val pages = BookPaginator.paginate(text)
        assertEquals("a".repeat(400), pages[0].text)
        assertEquals("b".repeat(400), pages[1].text)
    }

    @Test
    fun a_break_before_the_halfway_mark_is_ignored_and_the_cut_is_hard() {
        // Space at index 10 is too early to be a good break, so we cut at exactly 700.
        val text = "a".repeat(10) + " " + "b".repeat(900)
        val pages = BookPaginator.paginate(text)
        assertEquals(700, pages[0].text.length)
    }

    @Test
    fun a_single_word_longer_than_a_page_is_hard_cut() {
        val pages = BookPaginator.paginate("x".repeat(2100))
        assertEquals(3, pages.size)
        assertEquals(700, pages[0].text.length)
        assertEquals(700, pages[1].text.length)
    }

    @Test
    fun leading_whitespace_is_trimmed_from_the_next_page() {
        val text = "a".repeat(400) + "\n" + "b".repeat(400)
        assertTrue(BookPaginator.paginate(text)[1].text.startsWith("b"))
    }

    // ---- numbering ----

    @Test
    fun page_numbers_are_one_based_and_total_is_consistent() {
        val pages = BookPaginator.paginate("y".repeat(5000))
        pages.forEachIndexed { i, p ->
            assertEquals(i + 1, p.pageNumber)
            assertEquals(pages.size, p.totalPages)
        }
    }

    @Test
    fun no_page_is_ever_empty() {
        val pages = BookPaginator.paginate(("word ".repeat(3000)))
        pages.forEach { assertTrue(it.text.isNotEmpty(), "page ${it.pageNumber} was empty") }
    }

    @Test
    fun concatenating_pages_preserves_every_non_whitespace_character() {
        val text = "The quick brown fox. ".repeat(300)
        val rejoined = BookPaginator.paginate(text).joinToString("") { it.text }
        assertEquals(text.filterNot { it.isWhitespace() }, rejoined.filterNot { it.isWhitespace() })
    }

    // ---- calculatePageBreak in isolation ----

    @Test
    fun page_break_prefers_newline_then_space_then_hard_cut() {
        assertEquals(500, BookPaginator.calculatePageBreak("a".repeat(500) + "\n" + "b".repeat(199)))
        assertEquals(500, BookPaginator.calculatePageBreak("a".repeat(500) + " " + "b".repeat(199)))
        assertEquals(700, BookPaginator.calculatePageBreak("a".repeat(700)))
    }

    @Test
    fun page_break_ignores_a_separator_at_exactly_the_halfway_mark() {
        // Rule is strictly greater than half, not >=.
        val window = "a".repeat(350) + " " + "b".repeat(349)
        assertEquals(700, BookPaginator.calculatePageBreak(window))
    }

    // ---- estimatePages ----

    @Test
    fun estimate_rounds_up_and_never_exceeds_the_real_count() {
        assertEquals(0, BookPaginator.estimatePages(0))
        assertEquals(1, BookPaginator.estimatePages(1))
        assertEquals(1, BookPaginator.estimatePages(700))
        assertEquals(2, BookPaginator.estimatePages(701))
        val text = "word ".repeat(2000)
        assertTrue(
            BookPaginator.estimatePages(text.length) <= BookPaginator.paginate(text).size,
            "estimate must be a lower bound",
        )
    }

    // ---- robustness ----

    @Test
    fun a_degenerate_page_size_still_terminates() {
        assertTrue(BookPaginator.paginate("abcdef", charsPerPage = 1).size >= 6)
        assertTrue(BookPaginator.paginate("abcdef", charsPerPage = 0).isNotEmpty())
    }
}
