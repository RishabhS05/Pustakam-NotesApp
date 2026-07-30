package com.app.pustakam.core.filesys.model

import com.app.pustakam.core.common.util.ContentType

// 🔧 30-Jul-2026 02:10 Phase 3 — thumbnail models + the POLICY that decides their shape.
//   The policy is shared so Android and iOS cannot drift on size or quality; the actual decoding
//   stays native behind ThumbnailGenerator.

/** Everything a platform needs to produce one thumbnail. Built by [ThumbnailPolicy]. */
data class ThumbnailRequest(
    val sourceRelativePath: String,
    val destinationRelativePath: String,
    val contentType: ContentType,
    val maxDimensionPx: Int,
    /** 0..100 — JPEG quality. */
    val quality: Int,
    /** Where to grab a video frame from. Ignored for stills. */
    val videoFrameMicros: Long,
)

/** Outcome of a thumbnail attempt. Failure is normal — callers keep their existing fallback. */
sealed interface ThumbnailResult {
    data class Generated(val relativePath: String) : ThumbnailResult
    data object NotSupported : ThumbnailResult
    data class Failed(val reason: String) : ThumbnailResult
}

// 🔧 30-Jul-2026 02:10 Phase 3 — the numbers, in one place.
//   Values lifted VERBATIM from the two existing implementations, which happen to agree today:
//   androidApp FileOps.generateThumbnail (512 px, JPEG 70, frame at 1_000_000 µs) and
//   iOS FileOps.swift generateThumbnail (512 pt, JPEG 0.7, frame at 1 s). Sharing them is what
//   stops that agreement from being accidental.
object ThumbnailPolicy {

    const val MAX_DIMENSION_PX = 512
    const val JPEG_QUALITY = 70

    /** ~1 s in — far enough past the black lead-in most videos start with. */
    const val VIDEO_FRAME_MICROS = 1_000_000L

    /** Types worth generating a thumbnail for. Everything else renders from an icon. */
    fun isEligible(type: ContentType): Boolean =
        type == ContentType.IMAGE || type == ContentType.GIF || type == ContentType.VIDEO

    /**
     * Build the request for [sourceRelativePath], or null when the type has no thumbnail.
     * The destination comes from PathPolicy so thumbnails land where the app already keeps them.
     */
    fun requestFor(sourceRelativePath: String, type: ContentType): ThumbnailRequest? {
        if (!isEligible(type)) return null
        val destination = com.app.pustakam.core.filesys.path.PathPolicy
            .thumbnailPath(sourceRelativePath)
        return ThumbnailRequest(
            sourceRelativePath = sourceRelativePath,
            destinationRelativePath = destination.relativePath,
            contentType = type,
            maxDimensionPx = MAX_DIMENSION_PX,
            quality = JPEG_QUALITY,
            videoFrameMicros = VIDEO_FRAME_MICROS,
        )
    }

    /**
     * Sample size for a bounds-decode: the largest power of two that still covers [maxDimension].
     * Shared because all three Android copies (FileOps.decodeDownsampled, NoteExporter.decodeScaled,
     * String+ext.toBitmap) implemented this same loop independently.
     */
    fun sampleSizeFor(width: Int, height: Int, maxDimension: Int = MAX_DIMENSION_PX): Int {
        if (width <= 0 || height <= 0 || maxDimension <= 0) return 1
        var sample = 1
        while (width / (sample * 2) >= maxDimension || height / (sample * 2) >= maxDimension) {
            sample *= 2
        }
        return sample
    }

    /** Scale factor so the longest side becomes [maxDimension]; 1f when already small enough. */
    fun scaleFactorFor(width: Int, height: Int, maxDimension: Int = MAX_DIMENSION_PX): Float {
        val longest = maxOf(width, height)
        if (longest <= 0 || longest <= maxDimension) return 1f
        return maxDimension.toFloat() / longest
    }
}
