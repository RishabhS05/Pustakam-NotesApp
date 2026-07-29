# Centralizing File Handling into `:core:filesys`

**Date:** 30-Jul-2026
**Platforms:** Frontend/shared (F) · iOS (I) · Android (A)
**Seam decision:** `expect`/`actual` declared inside `:core:filesys`
**Constraint:** zero feature regression · no UI change on either platform · `import shared` keeps working

---

## 0. Summary

There are **two parallel file subsystems** — one in `androidApp`, one in `iosApp` — implementing the same
seven responsibilities. Only ~4 of those responsibilities are shared today (`:core:filesys`), and the
duplication has **already drifted into a live bug** (§2.3).

| | Android | iOS | Shared today |
|---|---:|---:|---:|
| File-logic lines | 582 | 551 | 469 |

Separately, **link-download is broken on Android** and the cause is not in the shared code — it is
four defects in `FileImportManager.importFromUrl`, listed with fixes in §3.

This document is analysis + plan only. **No code was changed.**

---

## PART A — INVENTORY

### 1. Where file code lives right now

**Shared — `:core:filesys` (469 lines, already correct)**

| File | Lines | Responsibility |
|---|---:|---|
| `export/DocxExporter.kt` | 166 | Builds a complete `.docx` as `ByteArray` |
| `export/OoxmlPackaging.kt` | 112 | Pure-Kotlin zip + CRC32 + base64 decode |
| `export/NoteExportBuilder.kt` | 97 | `Note` → ordered `List<ExportBlock>`; `ExportFormat` |
| `fileimport/FileImportHelper.kt` | 94 | ext/mime → `ContentType`, URL filename, `MediaContent` factory |

**Android — `androidApp` (582 lines, should mostly move)**

| File | Lines | Contents |
|---|---:|---|
| `fileUtils/FileOps.kt` | 228 | `createFileWithFolders`, `deleteFile`, `generateThumbnail`, `decodeDownsampled`, `scaleDown`, `saveBitmapToFile` ×2, `mimeTypeFor`, `suggestedFileName`, `saveMediaToGallery`, `writeMediaToUri` |
| `export/NoteExporter.kt` | 172 | `export`, `exportDocx`, `buildItems`, `textItem`, `exportPdf`, `exportImage`, `drawItem`, `decodeScaled` |
| `fileimport/FileImportManager.kt` | 131 | `ImportResult`, `destinationDir`, `destinationFile`, `importUris`, `importFromUrl`, `dispositionFileName` |
| `export/ExportShare.kt` | 51 | `shareExportedFile`, `shareMediaFile` (FileProvider + `ACTION_SEND`) |

**iOS — `iosApp` (551 lines, should mostly move)**

| File | Lines | Contents |
|---|---:|---|
| `file/FileOps.swift` | 235 | `createFilepath`, `createFolder`, `getDocumentsDirectory`, `getATempFilePath`, `saveData`, `moveData`, `saveImageFile`, `saveVideoFile`, `deleteFile`, `saveMediaToDevice`, `presentSaveOptions`, `saveMediaToGallery`, `copyFile`, `generateThumbnail` |
| `export/NoteExporter.swift` | 180 | `export`, `exportDocx`, `buildItems`, `textItem`, `exportPdf`, `exportImage`, `draw`, `loadImage`, `share` |
| `file/FileImportService.swift` | 136 | `LinkImportResult`, `destinationFolder`, `uniqueFileName`, `importPicked`, `importFromLink`, `MultiFilePicker` |
| `hardware/stroage/StroagePermission.swift` | 60 | `presentDocumentExporter`, `topMostViewController` |

---

## PART B — DUPLICATION MAP

### 2.1 The seven responsibilities

| # | Responsibility | Android | iOS | Verdict |
|---|---|---|---|---|
| 1 | Path/folder policy (`imported/<noteId>/`, `exports/`, `thumbnails/`) | `FileOps.createFileWithFolders`, `FileImportManager.destinationDir` | `FileOps.createFolder`, `FileImportService.destinationFolder` | **Policy shared, IO platform** |
| 2 | Unique/safe filename | `destinationFile` | `uniqueFileName` | **Pure logic → share** |
| 3 | ext/mime → `ContentType` | *(uses shared)* + **duplicate `mimeTypeFor`** | *(uses shared)* | **Already shared — delete the duplicate** |
| 4 | Copy a picked file in | `importUris` (SAF cursor) | `importPicked` (`copyItem`) | **Orchestration shared, IO platform** |
| 5 | Download from a link | `importFromUrl` (`HttpURLConnection`) | `importFromLink` (`URLSession`) | **Share via Ktor — see §3** |
| 6 | Export note → PDF/PNG/DOCX | `NoteExporter` (`PdfDocument`/`Canvas`) | `NoteExporter` (`UIGraphics…`) | **Layout shared, rasterizing platform** |
| 7 | Save/share out to the OS | `saveMediaToGallery`, `writeMediaToUri`, `ExportShare` | `saveMediaToGallery`, `presentDocumentExporter` | **Stays native** (MediaStore vs Photos) |

### 2.2 `NoteExporter` — a line-for-line mirror

Both files are the *same program* written twice. Identical constants, identical decomposition:

| | Android | iOS |
|---|---|---|
| Page size | `PDF_WIDTH=595`, `PDF_HEIGHT=842` | `pdfSize = 595×842` |
| Long-image width | `IMAGE_WIDTH=1080` | `imageWidth = 1080` |
| Margin / gap | `MARGIN=40f`, `BLOCK_GAP=14f` | `margin = 40`, `blockGap = 14` |
| Measured element | `sealed class Item{TextItem,ImageItem}` | `enum Item{text,image}` |
| Functions | `buildItems`, `textItem`, `exportPdf`, `exportImage`, `drawItem`, `decodeScaled` | `buildItems`, `textItem`, `exportPdf`, `exportImage`, `draw`, `loadImage` |

Every one of those constants and the whole pagination rule ("start a new page when the item won't
fit") is business logic that must agree on both platforms — and today nothing enforces that. Change
`BLOCK_GAP` on Android and iOS silently diverges.

### 2.3 Drift this has ALREADY caused — a live bug

`ContentType` has 13 entries. There are two MIME maps:

| `ContentType` | shared `FileImportHelper.mimeFor` | Android `FileOps.mimeTypeFor` |
|---|---|---|
| IMAGE, GIF, VIDEO, AUDIO, PDF, DOCX | ✅ same | ✅ same |
| **TXT** | `text/plain` | ❌ `application/octet-stream` |
| **MD** | `text/markdown` | ❌ `application/octet-stream` |
| **EPUB** | `application/epub+zip` | ❌ `application/octet-stream` |

`FileOps.mimeTypeFor` was written before TXT/MD/EPUB existed and never updated. It is what feeds
`MediaStore.MediaColumns.MIME_TYPE` and the SAF picker, so on Android today:

> **Saving an imported `.txt`, `.md` or `.epub` to the device writes it as
> `application/octet-stream`** — the file lands without a usable type, so the system picker offers no
> app to open it. iOS is unaffected. This is a pure consequence of the duplicate map.

### 2.4 Smaller divergences worth recording

| Behaviour | Android | iOS |
|---|---|---|
| Saved-file naming | `suggestedFileName()` — title → source name → timestamp | **no equivalent**; uses the raw path's last component |
| Thumbnail size/quality | 512px, JPEG q70 | 512px, JPEG q0.7 — *agree today, by hand* |
| Video thumbnail frame | `getFrameAtTime(1_000_000)`, fallback 0 | `copyCGImage(at: 1s)`, fallback `.zero` — *agree by hand* |
| Import dedupe prefix | `System.currentTimeMillis()` | `Int(Date().timeIntervalSince1970 * 1000)` |
| Redirects on download | manual loop, max 5 | `URLSession` automatic |

### 2.5 Not duplication — leave alone

`DocumentFileCard.kt` (108) ↔ `DocumentFileCardView.swift` (105) and `InlineBookFileWidget.kt` (191)
↔ `InlineBookFileView.swift` (168) are **UI**. Per the multi-module plan §2.2 the UI stays native per
platform. They are listed only so the inventory is complete.

---

## PART C — THE ANDROID LINK-DOWNLOAD DEFECTS

`FileImportManager.importFromUrl` (`androidApp/fileimport/FileImportManager.kt:79`). Four defects,
ranked by how likely each is to be what you are seeing. The wiring is fine — `ImportFilesSheet` →
`NoteEditorViewModel.importFromLink` → `Dispatchers.IO` is all correct, and `INTERNET` is granted.

### 3.1 No `User-Agent` — most likely cause

`HttpURLConnection` sends `Dalvik/2.1.0 (Linux; U; Android …)` when you don't set one. Cloudflare,
Google Drive, Dropbox and most CDNs answer that with **403 or an HTML interstitial**, which
`isDownloadableFile()` then correctly rejects → the user sees *"No file found at this link."*
`URLSession` sends a normal `CFNetwork` UA, which is why the identical link works on iOS.

```kotlin
// 🔧 30-Jul-2026 the default Dalvik UA gets 403/HTML from most CDNs; iOS's URLSession UA does not
connection.setRequestProperty("User-Agent",
    "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122 Mobile Safari/537.36")
connection.setRequestProperty("Accept", "*/*")
```

### 3.2 Relative redirects resolve against the wrong base

```kotlin
connection = (URL(URL(urlText), location).openConnection() as HttpURLConnection)
```

`urlText` is always the **original** URL. On a two-hop chain where hop 2 is relative
(`Location: /files/x.pdf`), this resolves against the original host instead of the host hop 1 landed
on → 404 → `NoFileFound`. Fix: track the current URL.

```kotlin
// 🔧 30-Jul-2026 resolve Location against the CURRENT url, not the original, or multi-hop relative redirects hit the wrong host
var currentUrl = URL(urlText)
// … in the redirect branch:
currentUrl = URL(currentUrl, location)
connection = currentUrl.openConnection() as HttpURLConnection
```

### 3.3 Filename fallback loses the extension

iOS uses `response.suggestedFilename`, which `URLSession` synthesizes from `Content-Disposition`
**and** the MIME type — so it always has an extension. Android only checks `Content-Disposition`,
then falls back to `FileImportHelper.fileNameFromUrl()`, which returns `download-<timestamp>` with
**no extension** when the URL path has no dot.

Shared `isDownloadableFile()` then evaluates
`fileName.contains('.') || contentTypeForMime(m) != null` → for a URL like
`https://host/download?id=123` served as `application/octet-stream`, **Android returns
`NoFileFound` while iOS succeeds.**

Fix in shared code so both platforms gain it — give `fileNameFromUrl` an optional mime and append
the extension `FileImportHelper` already knows:

```kotlin
// 🔧 30-Jul-2026 append the mime's extension so extensionless download URLs are still recognised (iOS parity)
fun fileNameFromUrl(url: String, mime: String? = null): String {
    val cleaned = url.substringBefore('?').substringBefore('#').trimEnd('/')
    val segment = cleaned.substringAfterLast('/')
    if (segment.isNotBlank() && segment.contains('.')) return segment
    val ext = contentTypeForMime(mime)?.getExt().orEmpty()
    return "download-${getCurrentTimestamp()}$ext"
}
```

### 3.4 The real error is swallowed

```kotlin
} catch (e: Exception) {
    e.printStackTrace()
    ImportResult.Failed("Couldn't download from this link. Check the URL and your connection.")
}
```

Every failure — DNS, TLS, cleartext-blocked, timeout — collapses into one message with no log line,
so the field cause is unknowable. Add `log_d("FileImport", "…$urlText → ${e.message}")`.

### 3.5 Confirmed NOT the cause

- `INTERNET` permission — present (`AndroidManifest.xml:20`).
- Main-thread violation — `importFromLink` runs on `Dispatchers.IO`.
- UI wiring — `onImportFromLink` reaches the ViewModel correctly.
- **Cleartext `http://`** — blocked on Android (`network_security_config.xml` declares only a
  `domain-config` for 4 LAN IPs, so the implicit `base-config` for `targetSdk 35` denies cleartext).
  But iOS has **no ATS exception in `Info.plist` either**, so plain `http` links fail on *both*
  platforms. This is existing parity, not the Android-only bug. Worth a deliberate decision later.

---

## PART D — TARGET ARCHITECTURE

### 4.1 Package layout inside `:core:filesys`

```
core/filesys/src/
├── commonMain/kotlin/com/app/pustakam/core/filesys/
│   ├── path/       FilePaths.kt          ← pure    folder policy + safe/unique naming
│   ├── mime/       MimeCatalog.kt        ← pure    the ONE mime map (absorbs mimeTypeFor)
│   ├── naming/     FileNaming.kt         ← pure    suggestedFileName + fileNameFromUrl
│   ├── store/      FileStore.kt          ← expect  read/write/copy/delete/exists/size
│   ├── download/   FileDownloader.kt     ← pure    Ktor — no platform code at all
│   ├── thumbnail/  Thumbnailer.kt        ← expect  generate(path, type) -> String?
│   ├── export/     (existing)
│   │               ExportLayout.kt       ← pure    measurement + pagination
│   │               TextMeasurer.kt       ← iface   platform text metrics
│   │               PageRenderer.kt       ← expect  rasterize layout -> PDF/PNG bytes
│   ├── fileimport/ FileImportHelper.kt (existing)
│   │               FileImporter.kt       ← pure    common orchestration
│   └── save/       MediaSaver.kt         ← iface   implemented in androidApp / iosApp
│                   FileSharer.kt         ← iface   implemented in androidApp / iosApp
├── androidMain/…   java.io / MediaStore / BitmapFactory / PdfDocument / Canvas bodies
└── iosMain/…       FileManager / Photos / UIGraphicsPDFRenderer / AVAssetImageGenerator bodies
```

`:core:filesys` already sits at the right place in the graph — it depends on `:core:common` and
`:core:model` only, and `:shared` already `api()`s + `export()`s it, so **every symbol added here is
automatically visible to Swift and to `androidApp` with no build-file change.**

### 4.2 The one rule that makes this safe

> Pure logic (policy, measurement, ordering, naming, mime) goes in `commonMain`.
> Anything that touches a platform framework goes in an `actual` **in the same module**.

Concretely, for export: `ExportLayout` in commonMain answers *"what goes where on which page"* and
returns a `List<PlacedItem>`; `PageRenderer.actual` only answers *"draw this text at this point"*.
That is the difference between 350 duplicated lines and ~60 per platform.

### 4.3 Download: no `expect` needed at all

This is the biggest single win. `:core:network` already ships Ktor with an OkHttp engine on Android
and a Darwin engine on iOS. A Ktor `HttpClient` gives redirect following, timeouts, headers and
streaming **in common code**, which deletes both `importFromUrl` (Android) and `importFromLink`
(iOS) and makes all four defects in §3 structurally impossible.

```kotlin
// commonMain — replaces HttpURLConnection AND URLSession
class FileDownloader(private val client: HttpClient) {
    suspend fun download(rawUrl: String): DownloadOutcome { /* one implementation, both platforms */ }
}
```

`ImportResult` (Kotlin) then replaces Swift's hand-written `LinkImportResult`, so the two enums can
no longer drift.

### 4.4 Where an interface beats `expect`/`actual`

`expect`/`actual` is the default seam, but it is the wrong tool in two situations, so those components
use a **`commonMain` interface + platform implementation bound in Koin** instead:

- **The implementation needs a construction-time dependency the common caller can't supply**
  (`Context`, `Activity`).
- **The implementation genuinely belongs to the app layer**, because it presents UI (pickers, share
  sheets) — that code should not live in `:core:filesys` at all.

| Component | Seam | Why |
|---|---|---|
| `FilePaths`, `MimeCatalog`, `FileNaming`, `ExportLayout`, `FileImporter` | *(none — pure `commonMain`)* | No platform API touched |
| `FileDownloader` | *(none — `commonMain` + Ktor)* | Engines already differ per platform inside `:core:network` |
| `FileStore` | **`expect class`** | Mirrors the project's existing `SqlDelightDriverFactory` idiom: `actual class FileStore(val context: Context)` on Android, no-arg on iOS, constructed by a platform Koin module (`expect fun getFileSystemModule(): Module`) |
| `Thumbnailer` | **`expect object`** | Pure `path in → path out`, no context needed |
| `PageRenderer` | **`expect object`** | Takes an already-measured layout, returns bytes |
| `TextMeasurer` | **interface** | `StaticLayout` vs `UIFont`; injected into `ExportLayout` so the layout stays unit-testable with a fake measurer |
| `MediaSaver` | **interface** | Needs an `Activity` / top `UIViewController` and shows pickers → implemented in `androidApp` / `iosApp`, registered in Koin (exactly like `getAndroidSpecifics()` does for `ExoPlayer` and `IAudioRecorder` today) |
| `FileSharer` | **interface** | Same reason — `FileProvider` + `ACTION_SEND` vs `UIActivityViewController` |

The `MediaSaver` / `FileSharer` interfaces are the important consequence of your note: they let the
**call sites** be common (`mediaSaver.save(media)` from a shared use case) while the bodies stay in
the app modules where `Context` and UIKit already live. Nothing in `:core:filesys` learns about
`Activity`, and Part F's "stays native" list stays native — it just gains a shared front door.

---

## PART E — PHASED MIGRATION

**Rule, same as the multi-module plan: every phase ends with a green build on both platforms plus the
§7 smoke test. If a phase can't get there, revert that phase only.** Each phase is one commit.

### Phase 1 — Pure logic, no behaviour change · ~0.5 day · risk: low

1. `MimeCatalog` in commonMain = the shared `mimeFor` **plus** the 3 missing entries.
2. Point Android `FileOps.mimeTypeFor` at it (keep the function as a one-line delegate so no call
   site changes) — **this alone fixes §2.3.**
3. Move `suggestedFileName` → `FileNaming`, add the iOS binding it never had.
4. Move `destinationFile`/`uniqueFileName` → `FilePaths.uniqueName()`.

**Verify:** save a `.txt` and an `.epub` to device on Android — correct type in the picker. Save an
image on both platforms — filename now identical.

### Phase 2 — `FileDownloader` in common (Ktor) · ~1 day · risk: medium

Replaces `importFromUrl` + `importFromLink`. Fixes §3.1–3.4 by construction.
Keep the old Android path behind the new one for one commit so you can A/B a failing link.

**Verify:** the link that fails today; a `Content-Disposition` link; a 2-hop redirect; an
extensionless `application/octet-stream` link; an HTML page (must still say *"No file found"*); a
404; airplane mode.

### Phase 3 — `FileStore` + `FilePaths` expect/actual · ~1 day · risk: low

`read/write/copy/delete/exists/size/mkdirs` + the `imported/<noteId>/`, `exports/`, `thumbnails/`
policy. Android body wraps `java.io`; iOS body wraps `FileManager`. `importUris`/`importPicked`
collapse into one common `FileImporter` whose only platform part is *"give me bytes for this picked
handle"* (SAF cursor vs `copyItem`).

**Verify:** multi-file import on both; delete a note content block → file gone from disk.

### Phase 4 — `Thumbnailer` expect/actual · ~0.5 day · risk: low

512px / q70 and the 1-second video frame become **shared constants**; only the decode is platform.

**Verify:** list cards + video placeholders still show thumbnails; import a video → thumbnail appears.

### Phase 5 — `ExportLayout` shared + `PageRenderer` expect/actual · ~1.5 days · risk: **high**

The valuable one and the one to do last. Constants, `buildItems`, pagination and block ordering move
to commonMain; each platform keeps only text measurement and rasterizing.

> Text **measurement** cannot be shared (`StaticLayout` vs `UIFont`), so `ExportLayout` takes a
> `TextMeasurer` **interface** (§4.4) rather than an `expect` — that keeps the pagination rule
> unit-testable in `commonTest` with a fake measurer, which is the only way to prove Android and iOS
> paginate identically. Height values will still differ by a pixel or two between platforms; that is
> expected and does not affect correctness.

**Verify:** export the same note to PDF, PNG and DOCX on both platforms and open all six files.
Multi-page note; note with a missing image; note with no images.

### Phase 6 — Delete the dead native code · ~0.5 day

Only after Phases 1–5 are green. **Nothing is deleted without your explicit approval** — the phase
produces a list for you to confirm.

| Phase | Days | Risk | Removes |
|---|---:|---|---:|
| 1 Pure logic | 0.5 | Low | ~60 |
| 2 Downloader | 1.0 | Medium | ~120 |
| 3 FileStore | 1.0 | Low | ~180 |
| 4 Thumbnailer | 0.5 | Low | ~90 |
| 5 Export layout | 1.5 | **High** | ~350 |
| 6 Cleanup | 0.5 | Low | — |
| **Total** | **5.0** | | **~800 of 1133** |

---

## PART F — WHAT STAYS NATIVE, DELIBERATELY

Do **not** try to share these. They are thin, they are genuinely OS-specific, and wrapping them buys
nothing but risk:

| Stays native | Why |
|---|---|
| `saveMediaToGallery` | `MediaStore` + `IS_PENDING` vs `PHPhotoLibrary` + add-only auth — no common shape |
| `writeMediaToUri` / `presentDocumentExporter` | SAF `ACTION_CREATE_DOCUMENT` vs `UIDocumentPickerViewController` |
| `ExportShare` / `NoteExporter.share` | `FileProvider` + `ACTION_SEND` vs `UIActivityViewController` |
| `presentSaveOptions`, `topMostViewController` | UIKit presentation |
| `MultiFilePicker`, SAF launcher | UI |
| `DocumentFileCard*`, `InlineBookFile*` | UI — per multi-module plan §2.2 |

The `MediaSaver` / `FileSharer` **interfaces** (§4.4) exist only to give these a common call site
(`mediaSaver.save(media)`), so shared code stops branching on platform. The bodies stay exactly where
they are — `androidApp` registers its implementation in `getAndroidSpecifics()`, iOS registers its own,
and `:core:filesys` never sees `Context` or UIKit.

---

## PART G — REGRESSION CHECKLIST

Run in full at the end of **every** phase, both platforms.

```
Android:  ./gradlew clean :androidApp:assembleDebug
iOS:      Xcode → Product → Clean Build Folder → Build
```

1. Cold start → no Koin crash
2. Import 3 files of mixed type from device → all 3 appear in the editor, correct icons
3. Import from link: a real PDF URL → file appears
4. Import from link: a plain web page → **"No file found at this link."**
5. Import from link: bad host → the failure message, and a log line naming the real cause
6. Add image / audio / video → thumbnail renders → playback works
7. Export note → PDF, then PNG, then DOCX → open all three
8. Save media to device: image → Gallery/Photos; **`.txt` and `.epub` → correct type in the picker**
9. Share a note export → share sheet lists sensible apps
10. Open book reader → scroll → back → reopen → progress restored
11. Delete a note content block → the file is gone from disk
12. **Install over the previous build (do not uninstall) → old notes and imported files still present**

Items 8 and 12 are the ones that catch this refactor going wrong. Do not skip them.

---

## Appendix — Order of operations

Phase 1 is worth doing on its own regardless of whether the rest proceeds: it is ~60 lines and it
fixes a shipped bug. Phase 2 is worth doing next because it fixes the reported Android download
failure *and* deletes more code than it adds. Phases 3–5 are optimization of a working system —
schedule them, don't rush them.
