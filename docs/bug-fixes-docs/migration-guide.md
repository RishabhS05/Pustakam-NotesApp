# Migration Guide — `:core:filesys`

**Last updated:** 30-Jul-2026 · **Status: Phases 1–2 complete, Phases 3–5 pending**

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

## PHASE 3 — Platform interfaces ⏳

`FileReader`, `FileWriter`, `DirectoryManager`, `FileCopier`, `FileDeleter`, `MetadataReader`,
`ThumbnailGenerator`, `DocumentRenderer`, plus `TextMeasurer` (needed before `ExportLayoutBuilder`
can be wired). One responsibility each; bound via Koin. `ThumbnailPolicy` (512 px, JPEG q70, 1 s
video frame) lands here as pure constants alongside `ThumbnailGenerator`.

## PHASE 4 — Replace duplicated Android + iOS logic ⏳

Swift call sites move onto the shared components, and the Phase 2 components that were built but not
wired get connected on **both** platforms at once:

- `ImportCoordinator` → `FileImportManager` (Android) + `FileImportService` (iOS)
- `BookPaginator` → iOS `BookReaderView.paginate` (Android already delegates)
- `ExportLayoutBuilder` → both `NoteExporter`s, once `TextMeasurer` exists
- Downloading → `:core:network`

**Two decisions must be made before starting:**

1. **Folder casing.** iOS writes `IMAGE/<noteId>/`, Android writes `image/<noteId>/`. Needs a
   one-time on-device migration or a read-both/write-lowercase policy.
2. **The "invalid link" rule.** Reject only explicit non-https schemes, or anything not literally
   starting with `https` (which would stop `example.com/x.pdf` from working)?

## PHASE 5 — Delete duplicates ⏳

Only after both platforms are verified identical. Produces a deletion list for explicit approval —
nothing is deleted without it.
