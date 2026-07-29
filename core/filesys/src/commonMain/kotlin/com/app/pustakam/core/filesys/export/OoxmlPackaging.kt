package com.app.pustakam.core.filesys.export

// 🔧 20-Jul-2026: NEW (export feature) — pure-Kotlin OOXML packaging primitives so the ENTIRE
//   .docx is produced in shared code and each platform only writes the returned bytes (max DRY).
//   No java.util.zip (JVM-only) — a tiny STORE-method zip works on Android and iOS alike.

// 🔧 20-Jul-2026: standard CRC-32 (zip requires it) — table built once, poly 0xEDB88320
internal object Crc32 {
    private val table = IntArray(256) { index ->
        var c = index
        repeat(8) { c = if (c and 1 != 0) (0xEDB88320.toInt() xor (c ushr 1)) else (c ushr 1) }
        c
    }

    fun compute(data: ByteArray): Long {
        var crc = 0.inv()
        for (b in data) crc = table[(crc xor b.toInt()) and 0xFF] xor (crc ushr 8)
        return crc.inv().toLong() and 0xFFFFFFFFL
    }
}

// 🔧 20-Jul-2026: base64 DECODE only — platforms encode image bytes to base64 (trivial both sides)
//   and hand them in as strings, so no ByteArray has to cross the Kotlin/Swift boundary.
internal object Base64Codec {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun decode(input: String): ByteArray {
        val output = ArrayList<Byte>(input.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (ch in input) {
            if (ch == '=') break
            if (ch == '\n' || ch == '\r' || ch == ' ') continue
            val value = ALPHABET.indexOf(ch)
            if (value < 0) continue
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                output.add(((buffer shr bits) and 0xFF).toByte())
            }
        }
        return output.toByteArray()
    }
}

// 🔧 20-Jul-2026: minimal STORE-method (no compression) zip writer — enough for a valid .docx
//   package. Little-endian headers; tracks local-header offsets for the central directory.
internal object MiniZip {
    class Entry(val name: String, val data: ByteArray)

    fun zip(entries: List<Entry>): ByteArray {
        val out = ArrayList<Byte>()
        fun u16(v: Int) { out.add((v and 0xFF).toByte()); out.add(((v ushr 8) and 0xFF).toByte()) }
        fun u32(v: Long) {
            out.add((v and 0xFF).toByte()); out.add(((v ushr 8) and 0xFF).toByte())
            out.add(((v ushr 16) and 0xFF).toByte()); out.add(((v ushr 24) and 0xFF).toByte())
        }
        fun bytes(b: ByteArray) { for (x in b) out.add(x) }

        data class Record(val nameBytes: ByteArray, val crc: Long, val size: Int, val offset: Int)
        val records = ArrayList<Record>()

        // local file headers + data
        for (entry in entries) {
            val nameBytes = entry.name.encodeToByteArray()
            val crc = Crc32.compute(entry.data)
            val offset = out.size
            u32(0x04034b50)          // local file header signature
            u16(20)                  // version needed
            u16(0)                   // flags
            u16(0)                   // method 0 = store
            u16(0); u16(0)           // mod time / date
            u32(crc)
            u32(entry.data.size.toLong())   // compressed size
            u32(entry.data.size.toLong())   // uncompressed size
            u16(nameBytes.size)
            u16(0)                   // extra length
            bytes(nameBytes)
            bytes(entry.data)
            records.add(Record(nameBytes, crc, entry.data.size, offset))
        }

        // central directory
        val centralStart = out.size
        for (r in records) {
            u32(0x02014b50)          // central directory header signature
            u16(20); u16(20)         // version made by / needed
            u16(0); u16(0)           // flags / method
            u16(0); u16(0)           // mod time / date
            u32(r.crc)
            u32(r.size.toLong()); u32(r.size.toLong())
            u16(r.nameBytes.size)
            u16(0); u16(0)           // extra / comment length
            u16(0); u16(0)           // disk / internal attrs
            u32(0)                   // external attrs
            u32(r.offset.toLong())
            bytes(r.nameBytes)
        }
        val centralSize = out.size - centralStart

        // end of central directory
        u32(0x06054b50)
        u16(0); u16(0)
        u16(records.size); u16(records.size)
        u32(centralSize.toLong())
        u32(centralStart.toLong())
        u16(0)

        return out.toByteArray()
    }
}
