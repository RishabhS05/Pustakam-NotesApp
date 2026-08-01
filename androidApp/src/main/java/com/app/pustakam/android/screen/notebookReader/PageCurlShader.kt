package com.app.pustakam.android.screen.notebookReader

// 📖 23-Jul-2026: FIX (page-flip realism) — the old flip rotated the leaf as a rigid flat rectangle,
//   which reads as a spinning card, not paper. This replaces the geometry with a true cylindrical
//   curl (AGSL, API 33+): the page wraps around a cylinder whose axis slides across the sheet, so the
//   leading edge curves, the back face shows through, and the curl casts a soft shadow — matching the
//   feel of iOS UIPageViewController's native .pageCurl (BookReaderView.swift).
import android.os.Build

// 📖 23-Jul-2026 — devices below API 33 have no RuntimeShader; they keep the (retuned) rigid flip.
val supportsShaderCurl: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

/**
 * 📖 23-Jul-2026 — cylindrical page-curl in AGSL.
 *
 * The sheet is wrapped around a cylinder of radius [radius] whose axis is a vertical line at
 * `progress` across the page. For each pixel we work out which part of the sheet lands there:
 *  - ahead of the cylinder  → flat, untouched page (front face)
 *  - on the cylinder        → the curling band; lit by the cylinder normal
 *  - past the cylinder      → the back of the leaf, mirrored, or the page underneath
 *
 * Uniforms are set from [PageCurlEffect]. Kept as a string constant so the shader compiles once.
 */
@Suppress("unused")
const val PAGE_CURL_AGSL = """
uniform shader front;      // the page being turned (this layer's own content)
uniform float2 size;       // page size in px
uniform float progress;    // 0 = flat, 1 = fully turned
uniform float radius;      // curl cylinder radius in px

const float PI = 3.14159265;

// soft paper tint on the reverse of a leaf — you see the sheet through the paper, dimmed,
// never a perfect mirror of the front
half4 backFace(float2 p) {
    half4 c = front.eval(p);
    return half4(c.rgb * 0.88, c.a);
}

half4 main(float2 coord) {
    // the curl axis sweeps from the right edge to just past the left edge
    float origin = size.x - progress * (size.x + radius * PI);
    float dist = coord.x - origin;   // distance from the curl axis, along the page

    // --- flat, not yet reached by the curl: plain front face ---
    if (dist <= 0.0) {
        return front.eval(coord);
    }

    // --- inside the curling band: the sheet is bent around the cylinder ---
    float arc = radius * PI;   // arc length of the half-cylinder (front → back)
    if (dist < arc) {
        float theta = dist / radius;              // angle around the cylinder
        // where this pixel's paper actually comes from, measured along the flat sheet
        float src = origin + radius * sin(theta);
        // cylinder shading: the normal turns away from the viewer as theta grows
        float shade = cos(theta) * 0.5 + 0.5;

        if (theta <= PI * 0.5) {
            // front half of the curl — still showing the readable face, foreshortened
            half4 c = front.eval(float2(src, coord.y));
            // darken as the paper tilts away, plus a bright edge where it catches the light
            float lit = mix(0.55, 1.0, shade);
            return half4(c.rgb * lit, c.a);
        } else {
            // back half — we now see the reverse of the leaf, mirrored around the axis
            float mirrored = origin + radius * sin(PI - theta);
            half4 c = backFace(float2(mirrored, coord.y));
            float lit = mix(0.45, 0.95, shade + 0.5);
            return half4(c.rgb * lit, c.a);
        }
    }

    // --- past the curl: the leaf has lifted away entirely ---
    // Transparent here, so the page composited underneath this layer shows through. The curl's
    // cast shadow is painted as translucent black that darkens whatever lies below.
    float shadow = 1.0 - clamp((dist - arc) / (radius * 1.6), 0.0, 1.0);
    return half4(0.0, 0.0, 0.0, shadow * 0.45);
}
"""

// 📖 23-Jul-2026 — curl tightness. Smaller radius = tighter roll; this is tuned to read like a
//   paperback leaf at phone widths rather than a poster tube.
const val PAGE_CURL_RADIUS_FRACTION = 0.14f

// 🐛 23-Jul-2026: a shared RuntimeShader singleton used to live here. It was wrong — uniforms are
//   per-instance state, so overlapping flips overwrote each other's `progress` and left pages frozen
//   mid-curl. Each leaf now remembers its own instance (see ShaderCurlLeaf in BookPagerView.kt);
//   compiling once per leaf is cheap next to the correctness bug.
