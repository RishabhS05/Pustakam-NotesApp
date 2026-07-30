# Testing Guide — `:core:filesys`

**Last updated:** 30-Jul-2026 (Phase 3 complete · Phase 4 Android)

---

## 1. How to run

```bash
# shared unit tests (JVM, fast — this is the main suite)
./gradlew :core:filesys:allTests

# just the common tests on the Android target
./gradlew :core:filesys:allTests

# iOS simulator target (proves the same tests pass on Kotlin/Native)
./gradlew :core:filesys:iosSimulatorArm64Test

# full build both platforms
./gradlew clean :androidApp:assembleDebug
# Xcode → Product → Clean Build Folder → Build
```

`commonTest` already had `implementation(libs.kotlin.test)` in `core/filesys/build.gradle.kts`, so
no build-file change was needed.

---

## 2. Why these tests exist

Every component migrated into `commonMain` is a **pure function over the app's file conventions**.
Those conventions describe files that already exist on users' devices, so the tests are not
"coverage" — they are a **contract that an upgrade will not orphan data**.

A failing test in `PathPolicyTest` means a shipped build would stop finding existing files.

---

## 3. Phase 1 — test inventory (64 tests)

### `MimeCatalogTest` — 14 tests

| Area | Cases |
|---|---|
| Canonical mime | every mapped `ContentType` |
| **Regression guard** | TXT / MD / EPUB are *not* `octet-stream` (the shipped bug) |
| Fallback | `OTHER`, `TEXT`, `LINK`, `LOCATION` → `octet-stream` |
| Totality | every one of the 13 types yields a non-blank mime |
| Ordering | `image/gif` matched before `image/`; `text/markdown` before `text/` |
| Normalisation | `"TEXT/PLAIN; charset=utf-8"` → `TXT` |
| **Invalid input** | `null`, `""`, `"   "`, unknown mime → `null` |
| Extensions | case- and dot-insensitive; unknown → `null` |
| Precedence | filename beats a mislabelled mime (`report.pdf` + `octet-stream` → PDF) |
| **Edge** | dotfiles (`.gitignore`), multi-dot names (`my.report.v2.pdf`) |

### `FileNameGeneratorTest` — 24 tests

| Area | Cases |
|---|---|
| Capture | `<ts><ext>`; extensionless type → bare timestamp |
| Sanitize | `/` and `\` → `_`; ordinary names untouched; blank → `"file"` |
| **Large filenames** | 500-char base capped at 200, **extension preserved**; no-extension variant |
| **Edge** | leading dot is part of the name, not an extension |
| **Duplicate names** | free name; first collision → `<ts>_`; second → `<ts>_1_`; sanitize-before-probe |
| Export names | allow-list `[A-Za-z0-9-_]`; blank title → `note`; 1000-char title capped |
| **Non-ASCII** | `Ünïcodé` → `_n_cod_`, `日本語だ` → `____` — pins the old regex exactly |
| Save names | source-with-extension wins; title + type extension; timestamped fallback |
| Thumbnails | always `<base>_thumb.jpg` |
| URL names | last segment; query/fragment stripped; **mime-derived extension** when the URL has none; no extension when the mime is unknown too |

### `PathPolicyTest` — 13 tests

| Area | Cases |
|---|---|
| Capture | `image/note-42/<ts>.png`; all 10 media types map to the right folder |
| **Casing guard** | no folder ever contains an uppercase letter (blocks the iOS `IMAGE/` variant) |
| Import | `imported/<noteId>/<name>`; name sanitized |
| Thumbnail | flat `thumbnails/` |
| Export | flat `exports/`; correct extension per `ExportFormat` |
| **Frozen roots** | `imported`, `exports`, `thumbnails` pinned as literals |
| Join | single separator; empty folder handled |
| `isManaged` | accepts our roots and capture folders; **rejects `../../etc/passwd`, `Downloads/`, and uppercase `IMAGE/`** |

### `ImportValidatorTest` — 13 tests

| Area | Cases |
|---|---|
| **"No file found" contract** | HTML / XHTML rejected, with and without charset |
| Acceptance | extension alone is enough; recognised mime alone is enough; neither → rejected |
| **Invalid input** | blank name, zero size, negative size |
| Attach-anything | unknown extension still imports as `OTHER` |
| `isBook` | PDF/TXT/MD only — **EPUB excluded**, matching `BookPageFactory.pagesFor` |
| `isMedia` / `isImage` / `isGalleryEligible` | exhaustive over all 13 types |
| Invariant | no type is both a book and media |
| **Exhaustiveness alarm** | asserts `ContentType.entries.size == 13` so a new type forces a decision here |

---

## 3b. Phase 2 — test inventory (54 tests)

### `BookPaginatorTest` — 19 tests

> **A failure here is a data-migration problem, not a test problem.** `progressPage`/`totalPages`
> are persisted per `MediaContent`; changing any expectation moves every saved reading position.

| Area | Cases |
|---|---|
| **Frozen constants** | 700 chars/page (both platforms), 2 MB text cap |
| Empty input | `""`, spaces, newlines → **no pages at all**, not a blank page |
| Boundaries | shorter than a page; exactly 700; 701 → 2 pages |
| Word-boundary cuts | newline past halfway wins; space past halfway wins; break before halfway ignored → hard cut |
| **Single word longer than a page** | hard-cut into 700-char pages |
| Whitespace | leading `\n`/space trimmed from the next page |
| Numbering | 1-based, `totalPages` consistent on every page, no empty page |
| **Content preservation** | pages rejoined == original ignoring whitespace |
| `calculatePageBreak` | newline → space → hard cut; separator at *exactly* half is ignored (rule is `>`, not `>=`) |
| `estimatePages` | rounds up; proven to be a lower bound on the real count |
| **Robustness** | `charsPerPage` of 1 and 0 still terminate |

### `ImportCoordinatorTest` — 17 tests

| Area | Cases |
|---|---|
| Happy path | lands under `imported/<noteId>/`; type, mime, size, display name all resolved |
| Naming | display name stays human-readable even when the file is uniqued |
| Type resolution | extension when the server is silent; extension beats a mislabelled `octet-stream`; unknown → `OTHER` |
| Source URL | carried for link imports, empty for device picks |
| **Invalid input** | blank name, whitespace name, zero size, negative size |
| **Not a file** | HTML page; extensionless `octet-stream` |
| **Duplicate names** | free name; second collision → counter; sanitize-before-probe |
| **Batch** | distinct names; **two identical names in one batch cannot collide**; good files kept and bad ones reported; empty batch; on-disk collisions respected |

### `ExportLayoutBuilderTest` — 18 tests

| Area | Cases |
|---|---|
| **Frozen geometry** | 595×842, 1080 wide, margin 40, gap 14 |
| Content width | page minus both margins |
| Paged layout | fits on one page; first block at the top margin; offset = height + gap |
| Page breaks | block crossing the bottom margin starts a new page and restarts at the top margin |
| **Oversized blocks** | taller than a page stays on page 0 (the `y > MARGIN` guard); oversized *after* content gets a fresh page |
| Empty note | still one page |
| **Block ordering** | order preserved exactly; every block placed once; mixed kinds keep relative order; pages assigned monotonically |
| Continuous layout | single page; height floored at 1080; grows with content |
| Helper | `blocksOnPage` returns only that page, in draw order |

---

## 3c. Phase 3 — test inventory (28 tests)

### `PlatformSeamsTest` — 28 tests, all driven through `FakeFileSystem`

| Area | Cases |
|---|---|
| `FileReader` | missing file → null (never throws); `readText` truncates at the cap; cap larger than the file is ignored |
| `FileWriter` / `DirectoryManager` | write creates file + registers folder; unknown folder lists empty; `sizeOf` absent → 0; **`list` returns direct children only** |
| `FileCopier` | unknown handle fails cleanly; bytes land at the destination |
| `FileDeleter` | **deleting something already gone still succeeds** (both platforms rely on this) |
| `MetadataReader` | `describe` resolves name/extension/type/size; missing file and handle → null |
| **Signature-collision guard** | one class implements all six file-IO seams — compiling the test *is* the assertion |
| `ThumbnailPolicy` | 512 px / q70 / 1 s frame pinned; only IMAGE/GIF/VIDEO eligible; `requestFor` targets `thumbnails/`; ineligible → null |
| `ThumbnailGenerator` | ineligible type reported as `NotSupported`, not a failure |
| **Sample-size maths** | largest power of two covering the target; **degenerate bounds (0, negative, zero max) never divide by zero**; scale factor never upscales |
| `DocumentRenderer` | unknown document → 0 pages |
| `TextMeasurer` | style sizes pinned (44/30/24/28, TITLE bold); empty text and zero width → 0; height grows with length |

**`FakeFileSystem`, `FakeThumbnailGenerator`, `FakeDocumentRenderer` and `FakeTextMeasurer` are the
real deliverable here.** From Phase 4 on, any shared component that touches files can be tested with
no platform, no disk and no Robolectric.

---

## 4. Equivalence verification (how "no behaviour change" was proved)

Gradle cannot run in the analysis sandbox (no Android SDK, JDK 11 only), so Phase 1 was verified by
**re-implementing the old and new rules side by side and diffing them exhaustively**:

| Rule | Inputs covered | Result |
|---|---|---|
| `mimeTypeFor` | all 13 `ContentType` | 3 intentional deltas (TXT/MD/EPUB) |
| capture path | 13 types × 3 note ids | **identical** |
| unique filename | 6 collision scenarios | 2 intentional deltas (overwrite bug, blank name) |
| export name | 10 title shapes incl. Unicode, emoji, 300 chars | **identical** after the ASCII fix |
| save name | 3 titles × 3 sources × 3 types | **identical** |
| thumbnail name | 4 path shapes | **identical** |

This check is what caught the `isLetterOrDigit()` Unicode regression before it shipped. **Re-run an
equivalent diff for every future phase** — it is the cheapest defence available without a device.

**Phase 2:**

| Rule | Inputs covered | Result |
|---|---|---|
| `paginate` | 415 documents — empty, whitespace-only, exactly 700, 701, no-spaces, newline-heavy, 15 hand-built shapes + 400 randomised up to 6000 chars | **0 mismatches** |
| PDF `layoutPaged` | 312 layouts — exact-fit, oversized block, oversized-after-content, empty, 12 hand-built + 300 randomised | **0 mismatches** |

The pagination diff is the single most important verification in this migration. Re-run it any time
`BookPaginator` is touched, before the change reaches a device.

**Phase 3:**

| Rule | Inputs covered | Result |
|---|---|---|
| Import destination + naming (old `FileImportManager` vs `ImportCoordinator`) | 9 scenarios: single, batch, duplicate names ×2 and ×3, on-disk collision, path separators, blank name, 400-char name | **0 mismatches** |

Writing `FakeFileSystem` also caught a design defect before it compiled: `FileReader.read(String)`
and `MetadataReader.read(String)` had identical signatures, so **no class could have implemented
both**. `MetadataReader.read` was renamed to `describe`.

---

## 5. Manual verification checklist — Phase 1

Both platforms, after `./gradlew clean :androidApp:assembleDebug` and a clean Xcode build.

**Must be unchanged (Phase 1 touched no iOS code, so iOS is the control):**

1. Cold start → no Koin crash
2. Capture photo / video / audio → file appears in the editor, thumbnail renders
3. Import from device, one of each: `.png .gif .mp4 .mp3 .pdf .docx .txt .md .epub` + one unknown →
   all appear with the correct icon and type
4. Import the **same filename twice** → second lands as `<ts>_<name>`, first is still intact
5. Import from link: real PDF → appears · web page → **"No file found at this link."**
6. Export note → PDF, PNG, DOCX → all three open
7. Export a note whose title contains spaces, `/`, and accented characters → filename shape unchanged
8. Book reader: open PDF / TXT / MD → page counts unchanged
9. Share a note export → share sheet appears
10. Delete a note content block → file gone from disk

**Must be FIXED on Android (delta #1):**

11. Save an imported `.txt` to device → picker offers a text app, not "unknown file"
12. Same for `.md` and `.epub`

**Must survive upgrade:**

13. **Install over the previous build (do not uninstall)** → old notes, captured media, imported
    files, thumbnails, exports and reading positions all still resolve

Item 13 is the one that catches a path-policy mistake. Do not skip it.

---

## 5b. Manual verification checklist — Phase 2

Phase 2 changed exactly one runtime path: Android book pagination. iOS is untouched and acts as the
control. `ImportCoordinator` and `ExportLayoutBuilder` are not wired yet, so they cannot affect
anything on device.

**The one that matters — reading positions:**

1. **Before installing:** open 2–3 books you are part-way through (a TXT, an MD, a long note) and
   write down the page number and total for each
2. Install the new build **over** the old one (do not uninstall)
3. Reopen each book → **same page number, same total** → progress landed where you left it

**Pagination behaviour, Android:**

4. Open a long TXT → page through to the end → page numbers run 1..N with no blank page
5. Open an MD file → same
6. Open a very short text note in the reader → exactly one page
7. Open an empty/whitespace-only text block → **no page is produced** (not a blank page)
8. Open a text file over 2 MB → still opens, truncated as before
9. Open a PDF → page count unchanged (PDFs never went through the paginator)

**Unaffected, confirm no collateral damage:**

10. Inline book widget in the editor → same pages as the full-screen reader
11. Import, export, capture, save-to-device → all unchanged from Phase 1
12. iOS: open the same books → page numbers match Android

Item 3 is the pass/fail for this phase. If a position moved, revert before investigating.

---

## 5c. Manual verification checklist — Phase 3

Phase 3 changed one runtime path: **Android file import**. The platform interfaces are built and
bound but nothing resolves them yet, and iOS is untouched.

**Import — the pass/fail for this phase:**

1. Multi-pick 5 files of mixed type → all 5 appear, correct icons, correct order
2. **Multi-pick two files with the SAME name in one go** → both land, second is `<ts>_<name>`,
   neither overwrites the other
3. Import a file whose name already exists in the note → new one gets the timestamp prefix, the
   existing file is untouched
4. Import a file with `/` in its display name → lands with `_` instead
5. Import from link: real PDF → appears · web page → **"No file found at this link."** · bad host →
   the failure message
6. **Link with no extension serving a known type** (e.g. `…/download?id=1` returning a PDF) → now
   imports with `.pdf` where it previously said "No file found" *(intended delta #1)*
7. Reopen the note after each import → files still resolve, thumbnails render

**Unaffected, confirm no collateral damage:**

8. Capture photo / video / audio → unchanged
9. Book reader page counts and saved positions → unchanged from Phase 2
10. Export → PDF, PNG, DOCX → unchanged
11. Save to device → unchanged
12. iOS: import, export, reader → all unchanged (no Swift touched)
13. **Install over the previous build** → old imported files still resolve

Item 2 is the one that exercises the new batch-reservation logic. Item 13 catches a storage-root
mistake in `AndroidFileSystem`.

---

## 5d. Regression check — deleted content must stay deleted

The bug this fixes was invisible inside a session; it only appeared after a relaunch.

1. Open a note with a **document** block (`.pdf`, `.txt`, `.md`, `.epub`)
2. Delete the block → it disappears from the editor
3. **Force-close the app, reopen, open the same note → the container must NOT be back**
4. Repeat for an image, a video and an audio block
5. Delete a block, then relaunch **without** saving the note → still gone
6. Confirm the file is gone from disk too (re-import the same name → no `<ts>_` prefix appears,
   proving nothing is squatting the old name)
7. A block whose file was already missing (orphaned by the old bug) → deleting it now clears it

Steps 3 and 7 are the pass/fail. Step 7 is how pre-existing orphaned containers get cleaned up.

---

## 6. Test requirements for future phases

| Phase | Additional required tests |
|---|---|
| ~~2 — ImportCoordinator~~ | ✅ done — 17 tests |
| ~~2 — ExportBuilder~~ | ✅ done — 18 tests (`ExportLayoutBuilderTest`) |
| ~~2 — BookPaginator~~ | ✅ done — 19 tests + 415-document equivalence diff |
| ~~3 — platform interfaces~~ | ✅ done — 28 tests via `FakeFileSystem` |
| ~~3 — `TextMeasurer`~~ | ✅ done — `FakeTextMeasurer` exists; wiring it to `ExportLayoutBuilder` is Phase 4 |
| ~~3 — `ThumbnailPolicy`~~ | ✅ done — constants pinned |
| 4 — iOS renderers | `ThumbnailGenerator`, `DocumentRenderer`, `TextMeasurer` bound on iOS |
| 4 — FileDownloader (`:core:network`) | redirect chains, `Content-Disposition`, HTML rejection, 404, timeout, non-https rejection |
| 4 — parity | same input → same output on Android and iOS, asserted on both targets |
| 4 — folder casing | migration from `IMAGE/` to `image/` finds pre-existing iOS files |

**Rule for every phase: a component is not "migrated" until it has `commonTest` coverage of its
happy path, its edge cases, and its invalid inputs.**
