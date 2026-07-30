# Centralizing File Handling into `:core:filesys`

**Date:** 30-Jul-2026
**Platforms:** Frontend/shared (F) · iOS (I) · Android (A)
**Seam decision:** `expect`/`actual` declared inside `:core:filesys`
**Constraint:** zero feature regression · no UI change on either platform · `import shared` keeps working

---

## 0. Summary

There are **two parallel file subsystems** — one in `androidApp`, one in `iosApp` — implementing the
same **12 responsibilities** across **28 files**. Only 4 of those responsibilities are shared today
(`:core:filesys`), and the duplication has **already drifted into four live bugs** (§2.3).

| | Android | iOS | Shared today |
|---|---:|---:|---:|
| Files touching the filesystem | 17 | 11 | 4 |
| Est. shareable logic (lines) | ~900 | ~850 | 469 |

Every content type the app handles is covered below: **image, GIF, video, audio, PDF, DOCX, TXT, MD,
EPUB, plus TEXT/LINK/LOCATION blocks and the OTHER fallback** — across **create, capture, import,
download, read, paginate, thumbnail, export, save-out, share and delete**.

Separately, **link-download is broken on Android** — four defects in
`FileImportManager.importFromUrl` (§3).

This document is analysis + plan only. **No code was changed.**

> **Revision note:** the first pass of this doc covered only the four obvious file modules
> (`FileOps`, `NoteExporter`, `FileImportManager`, `ExportShare` and their Swift twins). A full sweep
> for filesystem access found **20 more call sites** — capture, audio recording, book-reader document
> reading, and image decoding. §1 and §2 below are the complete set.

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

**Android — `androidApp` (17 files touch the filesystem)**

| File | Lines | File responsibilities |
|---|---:|---|
| `fileUtils/FileOps.kt` | 228 | `createFileWithFolders`, `deleteFile`, `generateThumbnail`, `decodeDownsampled`, `scaleDown`, `saveBitmapToFile` ×2, `mimeTypeFor`, `suggestedFileName`, `saveMediaToGallery`, `writeMediaToUri` |
| `export/NoteExporter.kt` | 172 | `export`, `exportDocx`, `buildItems`, `textItem`, `exportPdf`, `exportImage`, `drawItem`, `decodeScaled` |
| `fileimport/FileImportManager.kt` | 131 | `ImportResult`, `destinationDir`, `destinationFile`, `importUris`, `importFromUrl`, `dispositionFileName` |
| `export/ExportShare.kt` | 51 | `shareExportedFile`, `shareMediaFile` (FileProvider + `ACTION_SEND`) |
| **`noteContentProvider/NoteContentProvider.kt`** | 47 | **capture destination policy** — folder `<type>/<noteId>`, name `<timestamp><ext>`, for all 10 media types |
| **`screen/bookReader/BookPageFactory.kt`** | 109 | **reads PDF (page count), TXT/MD (bytes, capped)**, `paginate()` word-boundary rule |
| **`screen/bookReader/BookReaderScreen.kt`** | 435 | `PdfRenderer` page→bitmap rendering |
| **`hardware/audio/recorder/AudioRecorder.kt`** | 52 | writes AUDIO via `MediaRecorder` → `FileOutputStream(outputFile).fd` |
| **`hardware/audio/recorder/AudioViewModel.kt`** | 98 | recording-file lifecycle |
| **`hardware/camera/CameraInAction.kt`** | 82 | writes VIDEO + photo capture to a provided `File` |
| **`hardware/camera/ImageDataViewModel.kt`** | 187 | `saveImage`, `saveRecordedVideo`, `clearRecording`, `clearPaths` |
| **`screen/noteEditor/NoteEditorViewModel.kt`** | 538 | deletes files on block removal, drives thumbnails + import |
| **`screen/noteEditor/NotesEditorView.kt`** | 856 | SAF launcher, save-to-device overlay |
| **`widgets/fabWidget/OverLayEditorButtons.kt`** | 175 | triggers file creation per content type |
| **`extension/String+ext.kt`** | 25 | `toBitmap()` — **third** copy of `inSampleSize` downsampling |
| `screen/bookReader/…`, `widgets/document/…` | — | existence checks / `getMediaUrl()` fallbacks (UI) |

**iOS — `iosApp` (11 files touch the filesystem)**

| File | Lines | File responsibilities |
|---|---:|---|
| `file/FileOps.swift` | 235 | `createFilepath`, `createFolder`, `getDocumentsDirectory`, `getATempFilePath`, `saveData`, `moveData`, `saveImageFile`, `saveVideoFile`, `deleteFile`, `saveMediaToDevice`, `presentSaveOptions`, `saveMediaToGallery`, `copyFile`, `generateThumbnail` |
| `export/NoteExporter.swift` | 180 | `export`, `exportDocx`, `buildItems`, `textItem`, `exportPdf`, `exportImage`, `draw`, `loadImage`, `share` |
| `file/FileImportService.swift` | 136 | `LinkImportResult`, `destinationFolder`, `uniqueFileName`, `importPicked`, `importFromLink`, `MultiFilePicker` |
| `hardware/stroage/StroagePermission.swift` | 60 | `presentDocumentExporter`, `topMostViewController` |
| **`view/screens/notes/noteEditor/NoteEditorViewModel.swift`** | 373 | **capture destination policy** — `saveImageFile` / `copyFile` into `<type>/<noteId>/<timestamp><ext>` |
| **`hardware/microPhone/AudioRecoderHandler.swift`** | 84 | records AUDIO to `temporaryDirectory/audio.m4a` |
| **`view/screens/bookReader/BookReaderView.swift`** | 1039 | `PDFDocument` reads, page rendering, text pages |
| **`media/MediaManager.swift`** | 261 | existence checks before playback |
| **`view/widgets/imageview/ImagePreviewView.swift`** | 34 | `UIImage(contentsOfFile:)` |
| **`view/screens/notes/noteList/NoteBookView.swift`** | 144 | `UIImage(contentsOfFile:)` |
| **`view/screens/notes/noteEditor/NoteEditorView.swift`** | 377 | existence checks |

`LocalFilePathResolver` (`expect`/`actual`, currently in `:core:common`) is already shared and should
move to `:core:filesys` in Phase 3 — it is file-path logic, not general utility.

**Not present anywhere: upload.** No multipart/upload path exists on either platform. Media stays
local and `getMediaUrl()` falls back to a remote `url` that nothing ever populates. Flagged as a gap,
out of scope for this refactor.

---

## PART B — DUPLICATION MAP

### 2.1 The twelve responsibilities

| # | Responsibility | Types | Android | iOS | Verdict |
|---|---|---|---|---|---|
| 1 | **Capture destination policy** `<type>/<noteId>/<ts><ext>` | img, gif, vid, aud, pdf, docx, txt, md, epub, other | `NoteContentProvider.addContent` | `NoteEditorViewModel.handleMedia` | **Pure → share** (drifted, §2.3b) |
| 2 | Import destination policy `imported/<noteId>/` | all | `FileImportManager.destinationDir` | `FileImportService.destinationFolder` | **Pure → share** |
| 3 | Export/thumbnail folders `exports/`, `thumbnails/` | — | `NoteExporter`, `FileOps` | `NoteExporter`, `FileOps` | **Pure → share** |
| 4 | Unique/safe filename | all | `destinationFile` | `uniqueFileName` | **Pure → share** |
| 5 | ext/mime → `ContentType`; `ContentType` → mime | all | *(shared)* + **dup `mimeTypeFor`** | *(shared)* | **Delete the duplicate** (§2.3a) |
| 6 | Create/delete/copy/exists/size | all | `createFileWithFolders`, `deleteFile` | `createFilepath`, `createFolder`, `copyFile`, `deleteFile` | **`expect` FileStore** |
| 7 | **Write a capture to disk** | img, vid, aud | `AudioRecorder`, `CameraInAction`, `ImageDataViewModel.saveImage` | `AudioRecoderHandler`, `saveImageFile`, `copyFile` | **Policy shared, encoder native** (§2.3c) |
| 8 | Copy a picked file in | all | `importUris` (SAF cursor) | `importPicked` (`copyItem`) | **Orchestration shared, IO platform** |
| 9 | Download from a link | all | `importFromUrl` (`HttpURLConnection`) | `importFromLink` (`URLSession`) | **Share via Ktor — §3** |
| 10 | **Read a document for the reader** | pdf, txt, md, epub | `BookPageFactory` + `BookReaderScreen` | `BookReaderView` | **`paginate()` pure → share; render native** (§2.3d) |
| 11 | Thumbnail + image downsample | img, gif, vid | `generateThumbnail`, `decodeDownsampled`, `decodeScaled`, `toBitmap` (**3 copies**) | `generateThumbnail`, `loadImage` | **Constants shared, decode native** |
| 12 | Export note → PDF/PNG/DOCX | note → file | `NoteExporter` (`PdfDocument`/`Canvas`) | `NoteExporter` (`UIGraphics…`) | **Layout shared, raster native** |
| 13 | Save/share out to the OS | all | `saveMediaToGallery`, `writeMediaToUri`, `ExportShare` | `saveMediaToGallery`, `presentDocumentExporter`, `share` | **Stays native, common interface** |

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

### 2.3 Drift this has ALREADY caused — four live bugs

`ContentType` has 13 entries. There are two MIME maps:

| `ContentType` | shared `FileImportHelper.mimeFor` | Android `FileOps.mimeTypeFor` |
|---|---|---|
| IMAGE, GIF, VIDEO, AUDIO, PDF, DOCX | ✅ same | ✅ same |
| **TXT** | `text/plain` | ❌ `application/octet-stream` |
| **MD** | `text/markdown` | ❌ `application/octet-stream` |
| **EPUB** | `application/epub+zip` | ❌ `application/octet-stream` |

#### (a) Wrong MIME for TXT / MD / EPUB — Android

`FileOps.mimeTypeFor` was written before TXT/MD/EPUB existed and never updated. It is what feeds
`MediaStore.MediaColumns.MIME_TYPE` and the SAF picker, so on Android today:

> **Saving an imported `.txt`, `.md` or `.epub` to the device writes it as
> `application/octet-stream`** — the file lands without a usable type, so the system picker offers no
> app to open it. iOS is unaffected. This is a pure consequence of the duplicate map.

#### (b) Capture folders differ in case between platforms

Responsibility #1 is implemented twice and the two copies disagree on one character:

| | Folder written |
|---|---|
| Android `NoteContentProvider` | `contentType.name.lowercase()` → `image/<noteId>/` |
| iOS `NoteEditorViewModel` | `type.name` → `IMAGE/<noteId>/` |

Same intent, different result. Harmless while each platform only reads its own files, but it means
the on-disk layout is **not** portable — any future export/backup/sync that assumes one convention
breaks on the other platform.

#### (c) iOS records AAC and labels it `.mp3`

`AudioRecoderHandler` records with `kAudioFormatMPEG4AAC` to `temporaryDirectory/audio.m4a`, and
`NoteEditorViewModel` then copies it to `<type>/<noteId>/<ts>.mp3` because
`ContentType.AUDIO.getExt()` is `".mp3"`. **The container is M4A/AAC; the extension says MP3.** In-app
playback tolerates it, but "save to device" hands the user a mislabelled file, and
`MimeCatalog` will report `audio/mpeg` for it.

Two further problems in the same file: the temp name is the **fixed** `audio.m4a` (two concurrent
recordings collide), and a recording left in `temporaryDirectory` can be evicted by iOS before it is
copied.

#### (d) Book pagination is a duplicated rule

`BookPageFactory.paginate()` implements a word-boundary page cut: prefer the last `\n`, else the last
space, but only if past the halfway mark, else hard-cut at `CHARS_PER_BOOK_PAGE`. This is **pure
logic** and `BookReaderView.swift` reimplements it. If the two ever disagree, the same book paginates
differently per platform — and saved reading progress (`progressPage` / `totalPages`, persisted per
`MediaContent`) **points at a different place on each device**.

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
│   ├── path/       FilePaths.kt          ← pure    ALL folder policy + safe/unique naming
│   │               CaptureDestination.kt ← pure    <type>/<noteId>/<ts><ext>  (fixes 2.3b)
│   ├── mime/       MimeCatalog.kt        ← pure    the ONE mime map (absorbs mimeTypeFor)
│   ├── naming/     FileNaming.kt         ← pure    suggestedFileName + fileNameFromUrl
│   ├── store/      FileStore.kt          ← expect  read/write/copy/delete/exists/size
│   │               LocalFilePathResolver ← expect  (moved from :core:common)
│   ├── download/   FileDownloader.kt     ← pure    Ktor — no platform code at all
│   ├── thumbnail/  Thumbnailer.kt        ← expect  generate(path, type) -> String?
│   │               ImageScaling.kt       ← pure    sample-size math (kills the 3 copies)
│   ├── document/   BookPaginator.kt      ← pure    paginate() word-boundary rule (fixes 2.3d)
│   │               DocumentReader.kt     ← expect  pdf page count, txt/md bytes (capped)
│   ├── capture/    CaptureSink.kt        ← iface   audio/video/photo encoders stay native (2.3c)
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

### Phase 1 — Pure logic, no behaviour change · ~1 day · risk: low

1. `MimeCatalog` in commonMain = the shared `mimeFor` **plus** the 3 missing entries.
2. Point Android `FileOps.mimeTypeFor` at it (keep the function as a one-line delegate so no call
   site changes) — **fixes §2.3a.**
3. `CaptureDestination.folderFor(type, noteId)` + `.fileNameFor(type, timestamp)` in commonMain;
   both `NoteContentProvider` and iOS `NoteEditorViewModel` call it — **fixes §2.3b.**
   *Pick lowercase and ship a one-time migration that renames existing `IMAGE/` → `image/` on iOS,
   or accept both on read. Decide before merging — this touches paths already on user devices.*
4. Move `suggestedFileName` → `FileNaming`, add the iOS binding it never had.
5. Move `destinationFile`/`uniqueFileName` → `FilePaths.uniqueName()`.
6. `ImageScaling.sampleSizeFor(w, h, target)` in commonMain; the three Android copies
   (`decodeDownsampled`, `decodeScaled`, `toBitmap`) and iOS's `loadImage` call it.

**Verify:** save a `.txt` and an `.epub` to device on Android — correct type in the picker. Capture an
image on both platforms — same folder, same filename shape. Existing notes still resolve their media.

### Phase 1b — Audio format honesty · ~0.5 day · risk: low

Fixes §2.3c, independent of everything else. Either record MP3 on iOS, or add
`ContentType.M4A`/let `CaptureDestination` take the encoder's real extension. Also give the temp
recording a unique name and move it out of `temporaryDirectory` before the OS can evict it.

**Verify:** record audio on iOS → play back in-app → save to device → the file opens in a normal
player. Existing `.mp3`-named recordings still play (do not break old rows).

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

### Phase 4b — `BookPaginator` + `DocumentReader` · ~1 day · risk: **high**

Fixes §2.3d. `paginate()` and `CHARS_PER_BOOK_PAGE` move to commonMain verbatim; `DocumentReader`
(`expect`) covers "how many pages in this PDF" and "give me the first N bytes of this txt/md".
`PdfRenderer` / `PDFDocument` page→bitmap rendering **stays native** — it is UI-bound and heavily
cached on both sides.

> **High risk because reading progress is already persisted.** `progressPage`/`totalPages` live on
> `MediaContent` rows. If the shared paginator produces a different page count than the platform
> implementation it replaces, **every saved book position shifts.** Before merging, dump page counts
> for a few real books on both platforms and diff them against the shared implementation.

**Verify:** open a TXT, an MD, a PDF and an EPUB → page counts unchanged from before the phase →
scroll → back → reopen → progress lands on the same page. Test a file larger than
`MAX_TEXT_FILE_BYTES`.

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

| Phase | Fixes | Days | Risk | Removes |
|---|---|---:|---|---:|
| 1 Pure logic (mime, capture path, naming, scaling) | §2.3a, §2.3b | 1.0 | Low | ~150 |
| 1b Audio format honesty | §2.3c | 0.5 | Low | ~20 |
| 2 `FileDownloader` (Ktor, common) | §3.1–3.4 | 1.0 | Medium | ~120 |
| 3 `FileStore` + `FilePaths` + capture sinks | — | 1.0 | Low | ~180 |
| 4 `Thumbnailer` | — | 0.5 | Low | ~90 |
| 4b `BookPaginator` + `DocumentReader` | §2.3d | 1.0 | **High** | ~120 |
| 5 `ExportLayout` + `PageRenderer` | — | 1.5 | **High** | ~350 |
| 6 Cleanup | — | 0.5 | Low | — |
| **Total** | | **7.0** | | **~1030** |

Add 1–2 days buffer. Phases 4b and 5 are the ones that historically overrun, and both touch data
already on user devices (reading positions, exported files).

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

**Capture (writes a new file)**

1. Cold start → no Koin crash
2. Take a photo → appears in editor → thumbnail renders
3. Record video → appears → plays back → thumbnail renders
4. Record audio → appears → plays back → **file extension matches the real container (§2.3c)**
5. Add TEXT / LINK / LOCATION blocks → no file created, block saves

**Import (copies a file in)**

6. Device multi-pick, one of each: `.png .gif .mp4 .mp3 .pdf .docx .txt .md .epub` + one unknown type
   → all 10 appear with the right icon and type
7. Link import: a real PDF URL → file appears
8. Link import: a plain web page → **"No file found at this link."**
9. Link import: bad host → failure message **and** a log line naming the real cause
10. Link import: 2-hop redirect, and an extensionless `application/octet-stream` URL

**Read**

11. Book reader: open a PDF, a TXT, an MD, an EPUB → **page counts unchanged from before the phase**
12. Scroll → back → reopen → progress lands on the same page (both platforms, same book)

**Export / save out / share**

13. Export note → PDF, PNG, DOCX → open all three, both platforms (6 files)
14. Export a multi-page note, a note with a missing image, a note with no images
15. Save to device: image + video → Gallery/Photos
16. Save to device: audio, PDF, DOCX, GIF, **`.txt`, `.md`, `.epub` → correct type in the picker (§2.3a)**
17. Share a note export → share sheet lists sensible apps

**Delete / upgrade**

18. Delete a note content block → the file is gone from disk
19. **Install over the previous build (do not uninstall) → old notes, imported files, captured media
    and reading positions all still resolve**

Items 11, 16 and 19 are the ones that catch this refactor going wrong. Do not skip them.
Item 19 matters most after Phase 1 (capture paths change) and Phase 4b (page counts change).

---

## Appendix — Order of operations

Phases **1, 1b and 2** are worth doing regardless of whether the rest proceeds — together ~2.5 days,
they fix four shipped bugs plus the reported Android download failure, and they delete more code than
they add. None of them changes an architecture boundary.

Phases **3, 4 and 5** are optimization of a working system. Schedule them, don't rush them.

Phase **4b** is the one to think hardest about: it is the highest-value deduplication in the app
(pagination correctness is currently guaranteed only by two people writing the same algorithm twice)
but it is also the only phase that can silently move every user's saved reading position. Do it alone,
on its own branch, with page-count diffs captured before and after.
