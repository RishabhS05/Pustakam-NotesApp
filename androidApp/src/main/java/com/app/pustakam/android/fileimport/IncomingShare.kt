package com.app.pustakam.android.fileimport

import android.content.Intent
import android.net.Uri
import android.os.Build
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// 🔧 17-Aug-2026: NEW FEATURE (Open With / Share) — the ONLY thing this holds is the uris another
//   app handed us, until a blank note exists to import them into. Everything after that is the
//   existing import path (FileImportManager -> ImportCoordinator).
object IncomingShare {

    private val _uris = MutableStateFlow<List<Uri>>(emptyList())
    val uris: StateFlow<List<Uri>> = _uris.asStateFlow()

    /** VIEW carries one uri in the data field; SEND / SEND_MULTIPLE carry them in EXTRA_STREAM. */
    fun urisFrom(intent: Intent?): List<Uri> = when (intent?.action) {
        Intent.ACTION_VIEW -> listOfNotNull(intent.data)
        Intent.ACTION_SEND -> listOfNotNull(intent.streamExtra())
        Intent.ACTION_SEND_MULTIPLE -> intent.streamExtras()
        else -> emptyList()
    }.filter { it.scheme == SCHEME_CONTENT || it.scheme == SCHEME_FILE }

    /** True when the intent actually carried files — a plain launch must not open an empty note. */
    fun accept(intent: Intent?): Boolean {
        val incoming = urisFrom(intent)
        if (incoming.isEmpty()) return false
        _uris.value = incoming
        return true
    }

    fun clear() {
        _uris.value = emptyList()
    }

    @Suppress("DEPRECATION")
    private fun Intent.streamExtra(): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else getParcelableExtra<Uri>(Intent.EXTRA_STREAM)

    @Suppress("DEPRECATION")
    private fun Intent.streamExtras(): List<Uri> =
        (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)).orEmpty()

    private const val SCHEME_CONTENT = "content"
    private const val SCHEME_FILE = "file"
}
