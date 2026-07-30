# Migration Guide — `:core:filesys`

**Last updated:** 30-Jul-2026 · **Status: Phases 1–3 complete · Phase 4 in progress (Android done, iOS blocked) · Phase 5 pending**

Companion documents: `dependency-graph.md`, `testing-guide.md`,
`30jul2026-FIA-centralize-file-system-in-core-filesys.md` (the original audit).

---

## PHASE 1 — MIME · Naming · PathPolicy · Validation ✅

### 1. Architecture

```
BEFORE                                    AFTER
──────                                    ─────
FileImportHelper                          MimeCatalog        (the ONE mime + extension table)
  ├─ extensionMap        ─────────────►   FileNameGenerator  (the ONE naming authority)
  ├─ mimeFor                              PathPolicy         (the ONE folder authority)
  ├─ contentTypeForMime                   ImportValidator    (the ONE acceptance authority)
  ├─ fileNameFromUrl                              ▲
  └─ isDownloadableFile                           │
                                                  │ delegate (API unchanged)
androidApp/FileOps.mimeTypeFor  ── DUP ──►        │
androidApp/FileOps.suggestedFileName ─────────────┤
androidApp/NoteContentProvider (inline path) ─────┤
androidApp/FileImportManager (inline path+name) ──┤
androidApp/NoteExporter (inline path+name) ───────┘
```

Every rule now has exactly one implementation. The old entry points survive as one-line delegates so
no caller — Kotlin or Swift — had to change.

### 2. Dependency graph

See `dependency-graph.md` §3. Summary: `mime/` is the leaf; `naming/` → `mime/`; `path/` → `naming/`;
`validation/` → `mime/`. Acyclic.

### 3. Files created (11)

| File | Purpose |
|---|---|
| `core/filesys/…/model/CaptureDestination.kt` | folder + fileName, no file handle |
| `core/filesys/…/model/FileMetadata.kt` | what we know without opening a file |
| `core/filesys/…/mime/MimeCatalog.kt` | `mimeFor`, `contentTypeForMime`, `extensionFor`, `isSupported`, `contentTypeFor`, `normalize` |
| `core/filesys/…/naming/FileNameGenerator.kt` | `generate`, `generateUnique`, `sanitize`, `suggestExportName`, `suggestSaveName`, `thumbnailName`, `fromUrl` |
| `core/filesys/…/path/PathPolicy.kt` | `capturePath`, `importPath`, `thumbnailPath`, `exportPath`, `relativePath`, `folderForContentType`, `isManaged` |
| `core/filesys/…/validation/ImportValidator.kt` | `canImport`, `canExport`, `isSupported`, `isBook`, `isMedia`, `isImage`, `isGalleryEligible`, `isDownloadable` |
| `core/filesys/src/commonTest/…/MimeCatalogTest.kt` | 14 tests |
| `core/filesys/src/commonTest/…/FileNameGeneratorTest.kt` | 24 tests |
| `core/filesys/src/commonTest/…/PathPolicyTest.kt` | 13 tests |
| `core/filesys/src/commonTest/…/ImportValidatorTest.kt` | 13 tests |
| `docs/bug-fixes-docs/{dependency-graph,migration-guide,testing-guide}.md` | this set |

### 4. Files modified (5) — **no UI files**

| File | Change | Why |
|---|---|---|
| `core/filesys/…/fileimport/FileImportHelper.kt` | all logic → delegates | it held the good mime table; now it holds none |
| `androidApp/…/fileUtils/FileOps.kt` | `mimeTypeFor` → `MimeCatalog`; `suggestedFileName` → `FileNameGenerator`; thumbnail path → `PathPolicy` | this file held the **stale duplicate** mime table |
| `androidApp/…/noteContentProvider/NoteContentProvider.kt` | capture folder/name → `PathPolicy.capturePath` | inline `"${type.name.lowercase()}/$noteId"` was a second copy of the rule |
| `androidApp/…/fileimport/FileImportManager.kt` | dest folder → `PathPolicy`; unique name → `FileNameGenerator` | inline copy of the naming rule |
| `androidApp/…/export/NoteExporter.kt` | export folder/name → `PathPolicy.exportPath` | inline copy of the naming rule |

### 5. Files removed

**None.** Phase 1 removes no file and renames no public API. Deletion is Phase 5, after both
platforms are verified identical.

### 6. Why each change was made

- **One mime table.** Two existed; the Android copy predated TXT/MD/EPUB. Any future `ContentType`
  had to be added in two places, and the second was already forgotten once.
- **One naming authority.** Filename rules were spread over five call sites in two languages. They
  are pure string functions with zero platform coupling — the clearest possible win.
- **One folder authority.** Folder strings were inline literals. They describe files that already
  exist on user devices, so they need tests, and tests need one owner.
- **One acceptance authority.** `isDownloadableFile` drives the user-visible "No file found at this
  link" message. It is a business rule, not an IO concern.
- **Clock injected, not read.** Every function takes `timestamp: Long`. Without this none of the
  naming rules are testable.

### 7. Behaviour deltas — all four intentional

| # | Delta | Old | New | Rationale |
|---|---|---|---|---|
| 1 | `mimeTypeFor(TXT/MD/EPUB)` | `application/octet-stream` | `text/plain`, `text/markdown`, `application/epub+zip` | **Bug fix.** Saved `.txt/.md/.epub` had no usable type in MediaStore or the SAF picker. |
| 2 | Import name collision when both `a.pdf` **and** `<ts>_a.pdf` exist | returned `<ts>_a.pdf` → **overwrote** | returns `<ts>_1_a.pdf` | **Bug fix.** Old path silently destroyed a file. |
| 3 | Import of a blank filename | `""` (invalid) | `"file"` | **Hardening.** Old value could not be written. |
| 4 | Name longer than 200 chars | passed through, write fails | base truncated, extension kept | **Hardening.** Only engages where the write already failed. |

Everything else was proven byte-identical by exhaustive old-vs-new simulation across all 13
`ContentType` values, 6 collision scenarios, 10 title shapes and 4 path shapes.

> One regression was caught *by that check during development*: `Char.isLetterOrDigit()` keeps
> accented characters, whereas the old `Regex("[^A-Za-z0-9-_]")` replaced them. Export names for
> non-ASCII titles would have changed. `suggestExportName` now uses an explicit ASCII allow-list and
> `export_name_replaces_non_ascii_letters_like_the_old_regex_did` pins it.

### 8. Risks

| Risk | Severity | Mitigation |
|---|---|---|
| A path constant drifts and orphans files already on devices | **High** | `PathPolicyTest` pins every root and folder rule; upgrade-in-place is checklist item 12 |
| Delta #1 changes what MediaStore records for new saves | Low | Only affects the type recorded for TXT/MD/EPUB; existing rows untouched |
| `:core:filesys` gains a platform dependency by accident | Medium | Verified: zero non-`com.app.pustakam` imports in the new packages; re-check each phase |
| iOS still runs its own copies of these rules | Medium | **By design in Phase 1.** iOS is untouched, so it cannot regress. Alignment is Phase 4. |

### 9. Rollback

Phase 1 is one commit and self-contained.

```bash
git revert <phase-1-sha>      # full rollback, no data migration needed
```

Partial rollback — to keep the shared code but restore old behaviour, revert only the delegate body:

```kotlin
// androidApp/…/FileOps.kt
fun mimeTypeFor(type: ContentType): String = when (type) { /* old inline table */ }
```

No database change, no file moves, no path changes ⇒ **nothing to migrate on rollback.**

### 10. Verification checklist

See `testing-guide.md` §Phase 1.

---

## PHASE 2 — ImportCoordinator · ExportLayoutBuilder · BookPaginator ✅

### 1. Architecture

```
LAYER 3  orchestration (new in Phase 2)
   ImportCoordinator ──► plans WHERE + WHAT NAME, performs no IO
   BookPaginator     ──► splits text into pages, leaf, depends on nothing
   ExportLayoutBuilder ► assigns blocks to pages given measured heights
              │
LAYER 2  policy    PathPolicy · ImportValidator          (Phase 1)
LAYER 1  vocabulary FileNameGenerator · MimeCatalog       (Phase 1)
LAYER 0  models    CaptureDestination · ImportPlan · TextPageChunk · ExportLayout
```

Each Layer-3 component takes its side effects as parameters — `exists: (String) -> Boolean` for the
coordinator, pre-measured heights for the layout builder — which is what keeps them pure.

### 2. Dependency graph

See `dependency-graph.md` §3–4. `pagination/` is a leaf with zero dependencies; `imports/` sits above
`path/`, `naming/`, `mime/` and `validation/`. Still acyclic.

### 3. Files created (8)

| File | Purpose |
|---|---|
| `…/pagination/BookPaginator.kt` | `paginate`, `calculatePageBreak`, `estimatePages`, `CHARS_PER_PAGE`, `MAX_TEXT_FILE_BYTES` |
| `…/pagination/TextPageChunk.kt` | one paginated page, platform-agnostic |
| `…/imports/ImportCoordinator.kt` | `planImport`, `planImports`, `ImportRequest`, `ImportBatch` |
| `…/model/ImportPlan.kt` | `ImportPlan`, `ImportDecision` (Accepted / NotAFile / Rejected) |
| `…/export/ExportLayoutBuilder.kt` | `layoutPaged`, `layoutContinuous`, `contentWidth`, `blocksOnPage` + `MeasuredBlock`, `PlacedBlock`, `ExportLayout` |
| `commonTest/…/BookPaginatorTest.kt` | 19 tests |
| `commonTest/…/ImportCoordinatorTest.kt` | 17 tests |
| `commonTest/…/ExportLayoutBuilderTest.kt` | 18 tests |

### 4. Files modified (1) — **no UI files**

| File | Change | Why |
|---|---|---|
| `androidApp/…/bookReader/BookPageFactory.kt` | `paginate()` now delegates to `BookPaginator`; `CHARS_PER_BOOK_PAGE` and `MAX_TEXT_FILE_BYTES` alias the shared constants | it held the second copy of the pagination rule; the names are kept so no call site changes |

### 5. Files removed

**None.**

### 6. Why each change was made

- **`BookPaginator` was wired immediately** (unlike `ImportCoordinator`, see §7) because leaving the
  algorithm duplicated in `BookPageFactory` would directly violate "never duplicate pagination
  logic", and because a verbatim port could be *proven* identical before shipping.
- **`ImportCoordinator` returns a plan, never copies.** Splitting "decide" from "do" is what makes
  duplicate resolution, type inference and rejection testable without a filesystem.
- **`planImports` reserves names within a batch.** The old per-file path resolved collisions against
  disk only; nothing is on disk yet when a multi-pick is planned, so two picked files with the same
  name could both target the same destination.
- **`ExportLayoutBuilder` takes heights rather than measuring.** Text measurement is genuinely
  platform (`StaticLayout` vs `UIFont`), but *pagination* is not. Splitting them moves the rule that
  must agree across platforms into shared code while leaving the part that cannot be shared alone.

### 7. Deliberately NOT done in Phase 2

| Item | Why deferred |
|---|---|
| **Wiring `ImportCoordinator` into `FileImportManager`** | You fixed and verified the Android link download immediately before this phase. Re-plumbing that path now would discard a verification that cost real device time. The coordinator is built and tested; wiring both platforms together is Phase 4, which is where the spec puts platform replacement anyway. |
| **Moving downloading to `:core:network`** | Same reason. The spec places downloading outside `:core:filesys`, and that still holds — but the move should happen when iOS moves too, not on top of a just-verified Android fix. |
| **Wiring `ExportLayoutBuilder` into `NoteExporter`** | Needs the `TextMeasurer` interface, which is Phase 3. |
| **The "invalid link" rule** | Parked at your request pending the bare-domain decision (`example.com/x.pdf` — upgrade to https, or reject?). |

### 8. Behaviour deltas

**None.** Both changes were proven byte-identical before wiring:

| Rule | Method | Coverage | Result |
|---|---|---|---|
| Pagination | old vs new algorithm, diffed | 415 documents — empty, whitespace, exact-page, off-by-one, no-spaces, newline-heavy, 400 randomised | **0 mismatches** |
| PDF layout | old `exportPdf` loop vs `layoutPaged` | 312 layouts incl. oversized blocks, exact-fit, empty, 300 randomised | **0 mismatches** |

`ImportCoordinator` and `ExportLayoutBuilder` are not yet called by anything, so they cannot change
behaviour at all this phase.

### 9. Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Pagination drift moves every saved reading position | **High** | 415-document equivalence diff (0 mismatches) + `BookPaginatorTest` pins 700 chars/page and every cut rule. Manual check: reopen a book and confirm the page you left on. |
| `BookPageFactory` constants are now aliases, so a future edit to `BookPaginator` silently changes the reader | Medium | Both constants asserted by name in `BookPaginatorTest`; the test comment says treat a failure as a data migration |
| `ImportCoordinator` diverges from the live `FileImportManager` before Phase 4 wires it | Low | Same underlying `FileNameGenerator`/`PathPolicy`; `ImportCoordinatorTest` pins the same collision strategy the live path uses |
| iOS still runs its own paginator | Medium | By design — iOS untouched, so it cannot regress. Both use 700 chars/page today, so Phase 4 alignment is a no-op for page counts. |

### 10. Rollback

One commit, self-contained, no data migration:

```bash
git revert <phase-2-sha>
```

Partial: to keep the shared code but restore the local paginator, drop the delegate body in
`BookPageFactory.paginate` back to its inline loop — the old code is in the Phase 2 diff.

### 11. Verification checklist

See `testing-guide.md` §Phase 2. The one that matters: **open a book you were part-way through and
confirm it reopens on the same page.**

---

## PHASE 3 — Platform interfaces ✅

### 1. Architecture

```
        shared policy (Phases 1–2)          ImportCoordinator · BookPaginator
                    │                       PathPolicy · MimeCatalog · …
                    │ depends on
                    ▼
        platform/  ── 9 small interfaces ──►  androidMain: java.io, BitmapFactory,
        (commonMain, no platform types)       PdfRenderer, StaticLayout
                    │                         iosMain: NSFileManager
                    │ bound by
                    ▼
        di/getFileSystemModule()   expect/actual, Koin
```

Every path crossing the seam is **relative** to the app's private storage root. The implementation
resolves the root, so no shared code ever sees a `Context`, an `NSURL`, or an absolute path.

### 2. Dependency graph

See `dependency-graph.md` §3 and §6. `platform/` depends only on `model/` and `mime/`; nothing in
the shared policy layers depends on `platform/` yet — Phase 4 orchestrators are its first consumers.
Still acyclic.

### 3. Files created (8)

| File | Purpose |
|---|---|
| `…/platform/FileSystem.kt` | `FileReader`, `FileWriter`, `DirectoryManager`, `FileCopier`, `FileDeleter`, `MetadataReader` |
| `…/platform/Rendering.kt` | `ThumbnailGenerator`, `DocumentRenderer`, `TextMeasurer`, `TextStyle` |
| `…/model/Thumbnail.kt` | `ThumbnailRequest`, `ThumbnailResult`, `ThumbnailPolicy` (512 px / q70 / 1 s + sample-size maths) |
| `…/di/FileSystemModule.kt` | `expect fun getFileSystemModule(): Module` |
| `androidMain/…/platform/AndroidFileSystem.kt` | six `java.io` bindings over `filesDir` |
| `androidMain/…/platform/AndroidRendering.kt` | `BitmapFactory` / `MediaMetadataRetriever` / `PdfRenderer` / `StaticLayout` |
| `androidMain/…/di/FileSystemModule.android.kt` | all nine bound |
| `iosMain/…/platform/IosFileSystem.kt` + `di/FileSystemModule.ios.kt` | six `NSFileManager` bindings |
| `commonTest/…/platform/FakeFileSystem.kt` | in-memory fakes for every seam |
| `commonTest/…/platform/PlatformSeamsTest.kt` | 28 tests |

### 4. Files modified (5) — **no UI files**

| File | Change |
|---|---|
| `androidApp/…/fileimport/FileImportManager.kt` | **`ImportCoordinator` wired in.** Planning (validate → name → resolve duplicate → destination) moved out; this object now performs IO only. Download mechanics untouched. |
| `core/filesys/build.gradle.kts` | `api(libs.koin)`, androidMain `implementation(libs.koin.android)` |
| `core/filesys/…/fileimport/FileImportHelper.kt` | `@Deprecated` |
| `androidApp/…/fileUtils/FileOps.kt` | `mimeTypeFor`, `suggestedFileName` `@Deprecated` |
| `androidApp/…/bookReader/BookPageFactory.kt` | both constant aliases `@Deprecated` |

### 5. Files removed

**None.** Nothing is deleted before Phase 5.

### 6. Deprecations added

| Symbol | Replacement | Still called by |
|---|---|---|
| `FileImportHelper` (whole object) | `MimeCatalog` / `FileNameGenerator` / `ImportValidator` | 5 Swift call sites, `BookReaderScreen.kt`, `ExportShare.kt` |
| `FileOps.mimeTypeFor` | `MimeCatalog.mimeFor` | `BoxScope+ext.kt`, `NotesEditorView.kt` |
| `FileOps.suggestedFileName` | `FileNameGenerator.suggestSaveName` | `BoxScope+ext.kt`, `NotesEditorView.kt` |
| `CHARS_PER_BOOK_PAGE` | `BookPaginator.CHARS_PER_PAGE` | — |
| `MAX_TEXT_FILE_BYTES` | `BookPaginator.MAX_TEXT_FILE_BYTES` | `BookPageFactory.kt` itself |

All remaining callers are UI or Swift, i.e. exactly the Phase 4 work list. The project has no
`allWarningsAsErrors`, so these stay warnings and cannot break the build.

### 7. Why each change was made

- **Nine small interfaces, not one `FileStore`.** Each has one reason to change; a consumer that
  only reads bytes depends on `FileReader` alone, not on delete or thumbnailing.
- **Relative paths only.** The single most valuable property of the seam: shared code cannot
  accidentally build a platform path, and the root policy stays in one place per platform.
- **`ImportCoordinator` wired now** (you asked): the Android import path no longer contains any
  naming, validation or duplicate-resolution logic — only IO.
- **Fakes are a deliverable, not scaffolding.** From Phase 4 on, shared components that touch files
  are testable with no platform, no disk and no Robolectric.

### 8. Behaviour deltas

| # | Delta | Old | New | Rationale |
|---|---|---|---|---|
| 1 | Link import of an extensionless URL serving a known type | `NoFileFound` | downloads with the mime's extension | `FileNameGenerator.fromUrl` is now mime-aware (FIA doc §3.3). Only turns a failure into a success. |

Import destinations and naming were proven unchanged across 9 scenarios (single, batch, duplicate
names ×2 and ×3, on-disk collision, path separators, blank name, 400-char name) — **0 mismatches**.

### 9. Risks

| Risk | Severity | Mitigation |
|---|---|---|
| Rewired import path breaks device picks | Medium | Equivalence diff on destinations; download mechanics untouched; manual multi-pick is checklist item 3 |
| `AndroidFileSystem` resolves a different root than the inline code | **High** | Both use `context.filesDir`; `PathPolicy` roots unchanged; upgrade-in-place is the check |
| iOS root mismatch (Documents vs elsewhere) | Medium | `IosFileSystem` uses `NSSearchPathForDirectoriesInDomains(NSDocumentDirectory…)`, exactly what `FileOps.swift getDocumentsDirectory()` uses — **but nothing calls it until Phase 4**, so it cannot regress iOS now |
| Kotlin/Native interop in `IosFileSystem` is unverified | Medium | Not registered for anything that runs; first exercised in Phase 4 with a device build |

### 10. Rollback

```bash
git revert <phase-3-sha>
```

Self-contained, no data migration. To keep the seam but restore the old import path, revert only
`FileImportManager.kt` — the previous version is in the Phase 3 diff.

### 11. Verification checklist

See `testing-guide.md` §Phase 3. The one that matters: **multi-pick several files including two with
the same name, and confirm both land intact.**

---

## PHASE 4 — Replace duplicated Android + iOS logic 🟡 Android done, iOS blocked

### Done

| Change | File | Why |
|---|---|---|
| Platform seams composed into the composition root | `shared/…/koin/Koin.kt` | `getFileSystemModule()` added to `initKoin()`. All `single`, all lazy — nothing is constructed until something injects it. |
| Share intent off the deprecated delegate | `androidApp/…/export/ExportShare.kt` | `FileImportHelper.mimeFor` → `MimeCatalog.mimeFor` |
| Save-to-device naming | `androidApp/…/fileUtils/FileOps.kt` + the two UI call sites | now `FileNameGenerator.suggestedFileNameFromMedia`, a thin wrapper over `suggestSaveName` — no duplicated rule |

**Every Android call site of the deprecated helpers is now gone.** What remains is Swift only.

> **Note on `suggestedFileNameFromMedia`:** it reads the clock internally
> (`getCurrentTimestamp()`), unlike the rest of `FileNameGenerator`, which takes `timestamp` as a
> parameter so every rule stays unit-testable. It is a convenience wrapper over the pure
> `suggestSaveName`, so the *rule* is still testable — but prefer the pure form in new code, and do
> not add clock reads to the other functions.

### Blocked — needs a decision

| Item | Blocker |
|---|---|
| iOS onto `ImportCoordinator` (`FileImportService.swift`, 5 call sites) | how far into Swift may be edited |
| iOS onto `BookPaginator` (`BookReaderView.swift`) | the paginator is embedded in a SwiftUI view file |
| iOS onto `CaptureDestination` (`NoteEditorViewModel.swift`) | Swift ViewModel |
| Binding `ThumbnailGenerator` / `DocumentRenderer` / `TextMeasurer` on iOS | follows the Swift move |
| Wiring `ExportLayoutBuilder` into both `NoteExporter`s | needs `TextMeasurer` bound on both |
| Downloading → `:core:network` | should move with iOS, not on top of a verified Android fix |

**Decided:** folder casing → **read both, write lowercase**. New captures go to `image/`; the
resolver checks lowercase first, then falls back to the legacy uppercase folder. No migration step,
no risk to existing media, self-heals over time.
**Still open:** the `https` / "invalid link" rule.

---

### BUG FIX (shipped with this phase) — deleted content came back on relaunch

**Symptom.** Deleting a content block removed it from the editor, but after closing and reopening
the app the container was back — most visible on document files, which render a container even when
their file is missing.

**Root cause.** `NoteEditorViewModel.removeContent` did:

```kotlin
viewModelScope.launch { deleteNoteContentUseCase.invoke(value) }   // never collected
```

`BaseUseCase.getBaseApiCall()` returns a **cold** `Flow`. Invoking it only builds the flow;
without a terminal operator `deleteNoteContentFromDb()` is never reached. So:

- the in-memory list dropped the block → the editor looked correct
- the DB row survived → the container returned on next launch
- the file HAD been deleted (that call was synchronous) → the returning container pointed at nothing

The DAO and the `.sq` `deleteNoteContentById` were correct all along.

**Fix.** Collect the flow, and only delete the files once the row is actually gone:

```kotlin
viewModelScope.launch(Dispatchers.IO) {
    var rowDeleted = false
    deleteNoteContentUseCase.invoke(value).collect { result ->
        when (result) {
            is Result.Success -> rowDeleted = true
            is Result.Error -> log_d("NoteEditor", "content delete failed: ${result.error}")
            else -> Unit
        }
    }
    if (rowDeleted && find?.isMediaFile() == true) { /* delete file + thumbnail */ }
}
```

Deliberately **not** routed through `makeAWish(NOTES_CODES.DELETE)`: that code maps to
`NoteStatus.exit`, which would close the whole editor when a single block is removed.

Two secondary improvements fall out: file deletion now runs off the main thread, and a failed DB
delete no longer leaves a surviving row pointing at a deleted file.

**Existing data.** Rows orphaned by this bug before the fix are still in the database. Deleting
those blocks again will now work correctly and clear them.

**Risk:** low — one call site, no schema change, no path change.
**Rollback:** revert the `removeContent` hunk.

---

## PHASE 5 — Delete duplicates ⏳

Only after both platforms are verified identical. Produces a deletion list for explicit approval —
nothing is deleted without it.
