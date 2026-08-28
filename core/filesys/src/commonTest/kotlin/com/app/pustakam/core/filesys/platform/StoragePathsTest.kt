package com.app.pustakam.core.filesys.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// 🖼️ 20-Aug-2026 sync: this arithmetic decides whether a media file is found or silently orphaned,
//   and it is the same on both platforms, so it is tested once here with a fake root.
class StoragePathsTest {

    private fun paths(root: String) = object : StoragePaths {
        override fun rootPath(): String = root
    }

    private val android = paths("/data/user/0/com.app.pustakam/files")
    private val ios = paths("/var/mobile/Containers/Data/Application/ABC-123/Documents")

    @Test
    fun toRelative_stripsTheRoot() {
        assertEquals(
            "imported/note-1/photo.png",
            android.toRelative("/data/user/0/com.app.pustakam/files/imported/note-1/photo.png")
        )
        assertEquals(
            "image/note-1/1755000000000.png",
            ios.toRelative("/var/mobile/Containers/Data/Application/ABC-123/Documents/image/note-1/1755000000000.png")
        )
    }

    @Test
    fun toRelative_refusesAnythingOutsideAppStorage() {
        assertNull(android.toRelative("/sdcard/Download/photo.png"))
        assertNull(ios.toRelative("/var/mobile/Containers/Data/Application/OLD-999/Documents/a.png"))
        assertNull(android.toRelative("/data/user/0/com.app.pustakam/cache/photo.png"))
    }

    @Test
    fun toRelative_doesNotMatchTheRootAsABarePrefix() {
        // ".../filesystem" must not read as ".../files" + "ystem"
        assertNull(android.toRelative("/data/user/0/com.app.pustakam/filesystem/photo.png"))
    }

    @Test
    fun toAbsolute_isTheExactInverse() {
        val relative = "imported/note-1/photo.png"
        assertEquals(relative, android.toRelative(android.toAbsolute(relative)))
        assertEquals(relative, ios.toRelative(ios.toAbsolute(relative)))
    }

    @Test
    fun toAbsolute_survivesSlashSloppiness() {
        val expected = "/data/user/0/com.app.pustakam/files/imported/note-1/photo.png"
        assertEquals(expected, android.toAbsolute("imported/note-1/photo.png"))
        assertEquals(expected, android.toAbsolute("/imported/note-1/photo.png"))
        assertEquals(expected, paths("/data/user/0/com.app.pustakam/files/").toAbsolute("imported/note-1/photo.png"))
    }
}
