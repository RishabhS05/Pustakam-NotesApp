# Dependency Graph — `:core:filesys` Migration

**Last updated:** 30-Jul-2026 (end of Phase 3)

---

## 1. Module graph (whole app)

```
                          ┌──────────────┐
   androidApp ───────────►│              │
                          │   :shared    │  umbrella — api() + export() only
   iosApp (import shared)►│  (iOS fwk)   │
                          └──────┬───────┘
                                 │
        ┌────────────────┬───────┴────────┬──────────────────┐
        ▼                ▼                ▼                  ▼
 :feature:notes    :feature:auth    :core:filesys      (future features)
        │                │                │
        └────────┬───────┘                │
                 ▼                        │
            :core:data                    │
                 │                        │
   ┌─────────────┼─────────────┐          │
   ▼             ▼             ▼          │
:core:network  :core:database  :core:model◄┘
   │             │             │
   └─────────────┴──────┬──────┘
                        ▼
                  :core:common
```

**Rule: edges point downward only.** Verified acyclic (see §4).

---

## 2. Where `:core:filesys` sits

`:core:filesys` depends on **exactly two** modules and nothing depends on it except `:shared`
(which re-exports it) and `:androidApp` (transitively):

```
:core:filesys
    ├── api(:core:common)   ContentType, Result, getCurrentTimestamp
    └── api(:core:model)    Note, NoteContentModel, NoteContentObjectHelper
```

It deliberately does **not** depend on `:core:database`, `:core:network` or `:core:data`. Downloading
lives in `:core:network` (see §5).

---

## 3. Internal graph of `:core:filesys` after Phase 3

```
  ┌────────────────────────────────────────────────────────────────────┐
  │ LAYER 3 — orchestration (pure policy, no IO)                       │
  │   imports/ImportCoordinator      pagination/BookPaginator          │
  │   export/ExportLayoutBuilder                                       │
  └───────────────┬────────────────────────────────────────────────────┘
                  │ uses
  ┌───────────────▼────────────────────────────────────────────────────┐
  │ LAYER 2 — policy                                                   │
  │   path/PathPolicy   validation/ImportValidator                     │
  └───────────────┬────────────────────────────────────────────────────┘
                  │ uses
  ┌───────────────▼────────────────────────────────────────────────────┐
  │ LAYER 1 — vocabulary                                               │
  │   naming/FileNameGenerator      mime/MimeCatalog                   │
  └───────────────┬────────────────────────────────────────────────────┘
                  │ uses
  ┌───────────────▼────────────────────────────────────────────────────┐
  │ LAYER 0 — models (leaf)                                            │
  │   model/CaptureDestination · FileMetadata · ImportPlan             │
  │   pagination/TextPageChunk   export/ExportBlock · ExportFormat     │
  └────────────────────────────────────────────────────────────────────┘

  ┌────────────────────────────────────────────────────────────────────┐
  │ PLATFORM SEAM (Phase 3) — interfaces in commonMain, bodies native   │
  │   platform/  FileReader · FileWriter · DirectoryManager             │
  │              FileCopier · FileDeleter · MetadataReader              │
  │              ThumbnailGenerator · DocumentRenderer · TextMeasurer   │
  │   di/        getFileSystemModule()   expect/actual Koin bindings    │
  └────────────────────────────────────────────────────────────────────┘
     androidMain → java.io · BitmapFactory · PdfRenderer · StaticLayout
     iosMain     → NSFileManager  (the three renderers land in Phase 4)

  legacy shim (retired in Phase 5, now @Deprecated):
    fileimport/FileImportHelper → thin delegate over mime/ + naming/ + validation/
```

**The seam points DOWN, never up.** `platform/` depends only on `model/` and `mime/`; no shared
policy object depends on `platform/` yet — Phase 4 orchestrators will be the first consumers.

**Internal edges, all one-directional:**

| From | To | Why |
|---|---|---|
| `naming/` | `mime/`, `export/` | `extensionFor()`; `ExportFormat.ext` |
| `path/` | `naming/`, `mime/`, `model/`, `export/` | composes a `CaptureDestination` |
| `validation/` | `mime/` | `isSupported`, `contentTypeForMime` |
| **`imports/`** | `path/`, `naming/`, `mime/`, `validation/`, `model/` | plans a destination + name |
| **`pagination/`** | — | **leaf** — depends on nothing at all |
| **`export/ExportLayoutBuilder`** | `export/ExportBlock` only | consumes measured heights |
| `fileimport/` | `mime/`, `naming/`, `validation/` | delegation only |
| **`platform/`** | `model/`, `mime/` | interface signatures reference `FileMetadata` / `ThumbnailRequest` |
| **`di/`** | `platform/` | binds the implementations |
| `model/` | — | leaf (only `ContentType` from `:core:common`) |

`BookPaginator` deliberately depends on **nothing** — it is a pure string algorithm, which is why
its output could be proven byte-identical to the shipped one across 415 documents.

---

## 4. Cycle check

```
mime         -> (leaf)
model        -> (leaf)
pagination   -> (leaf)
export       -> (leaf: blocks + layout)
naming       -> mime, export
validation   -> mime
path         -> naming, mime, model, export
imports      -> path, naming, mime, validation, model
fileimport   -> mime, naming, validation

platform     -> model, mime
di           -> platform

Topological order:
  mime, model, pagination, export, naming, validation, path, imports, fileimport,
  platform, di                                                              ✅ acyclic
```

---

## 5. Deviations from the target spec, and why

### 5.1 `ContentType` stays in `:core:common` — **not** moved to `:core:filesys/model`

The spec lists `ContentType` under MODEL. Moving it would create a hard cycle:

```
:core:model  ──uses ContentType──►  :core:filesys
:core:filesys ──uses Note/NoteContentModel──►  :core:model     ✗ CYCLE
```

Three files in `:core:model` (`Note.kt`, `Notes.kt`, `NoteContentObjectHelper.kt`) depend on
`ContentType`, and `:core:filesys` depends on `:core:model`. `ContentType` is also referenced by
18 `androidApp` files, 7 Swift files, and 4 other modules.

`ContentType` is already a **single definition with no duplicate**, so the rule the spec is
protecting ("never duplicate") is satisfied where it is. Leaving it in `:core:common` costs nothing
and avoids re-plumbing 30+ files.

**If you still want it under `:core:filesys/model`,** the prerequisite is moving `ContentType` to
`:core:model` first and having `:core:filesys/model` alias it — that is a separate, larger change.

### 5.2 Downloading

Per spec, downloading is **not** in `:core:filesys`. It belongs in `:core:network`, which already
ships Ktor with an OkHttp engine on Android and a Darwin engine on iOS. `:core:filesys` will receive
only bytes. Deferred to Phase 4 — see migration-guide §Phase 2 ¶7 for why.

### 5.3 Models — created as their consumer arrives

Phase 1: `CaptureDestination`, `FileMetadata`.
Phase 2: `ImportPlan`, `ImportDecision`, `TextPageChunk`, `MeasuredBlock`, `PlacedBlock`, `ExportLayout`.
Phase 3: `ThumbnailRequest`, `ThumbnailResult`, `ThumbnailPolicy`, `TextStyle`.
Types are created with their consumer so nothing is unused or untested.

`ExportResult` was **not** created: `NoteExporter` on both platforms already returns a file handle
(`File?` / `URL?`), which is a platform type by nature. A shared `ExportResult` would only wrap
`relativePath` + `ExportFormat`, both already available from `PathPolicy.exportPath`. Revisit in
Phase 3 if `DocumentRenderer` needs it.

### 5.4 Package named `imports/`, not `import/`

The spec says `import/`. `import` is a hard keyword in Kotlin and cannot appear as a package segment
without backticking it at every call site. The folder and package are both `imports` so the
"package matches folder" rule established in the multi-module restructure still holds.

### 5.5 `ExportLayoutBuilder`, not a renamed `ExportBuilder`

The spec calls for `ExportBuilder`. `NoteExportBuilder` already exists, already produces the ordered
block list, and is called from Swift as `NoteExportBuilder.shared.build(note:)`. Renaming it would
break the iOS call site for no functional gain, so the ordering half keeps its name and the new
layout half is `ExportLayoutBuilder`. Together they are the spec's `ExportBuilder`:

| Spec responsibility | Where it lives |
|---|---|
| Build export model | `NoteExportBuilder.build(note)` *(pre-existing)* |
| Generate ordered blocks | `NoteExportBuilder.build(note)` *(pre-existing)* |
| Generate layout model | **`ExportLayoutBuilder.layoutPaged` / `layoutContinuous`** *(new)* |
| No rendering | ✅ consumes measured heights, returns coordinates |

---

## 6. Platform-abstraction interfaces (Phase 3 — DONE)

Small and single-responsibility, **no** god-object `FileStore`. All paths are RELATIVE to the app's
private storage root; the implementation resolves it, so no caller ever sees a `Context` or an
absolute path.

```
:core:filesys/platform/
    FileReader          read(rel): ByteArray? · readText(rel, maxBytes): String?
    FileWriter          write(rel, bytes): Boolean
    DirectoryManager    ensure(folder) · exists(rel) · list(folder) · sizeOf(rel)
    FileCopier          copyIn(sourceHandle, destRel): Boolean
    FileDeleter         delete(rel): Boolean
    MetadataReader      describe(rel) · describeHandle(sourceHandle): FileMetadata?
    ThumbnailGenerator  generate(ThumbnailRequest): ThumbnailResult
    DocumentRenderer    pageCount(rel): Int
    TextMeasurer        measureHeight(text, TextStyle, width): Float
```

| Binding | Android | iOS |
|---|---|---|
| The six file-IO seams | ✅ `java.io` over `filesDir` | ✅ `NSFileManager` over Documents |
| `ThumbnailGenerator` | ✅ `BitmapFactory` / `MediaMetadataRetriever` | ⏳ Phase 4 |
| `DocumentRenderer` | ✅ `PdfRenderer` | ⏳ Phase 4 |
| `TextMeasurer` | ✅ `StaticLayout` | ⏳ Phase 4 |

The three iOS renderers are deferred on purpose: Swift already implements all three, and writing
speculative Kotlin/Native UIKit + PDFKit interop that nothing calls yet could not be exercised.
They are bound when the Swift call sites move across in Phase 4.

> **`MetadataReader.describe`, not `read`.** `read(String)` would collide with
> `FileReader.read(String)` and make it impossible for one class to implement both interfaces.
> Writing `FakeFileSystem` is what surfaced this.

Bound via `getFileSystemModule()` (expect/actual), following the existing `getDatabaseModule()`
pattern. **Not yet composed into `initKoin()`** — nothing resolves these until Phase 4.

`MediaSaver` / `FileSharer` remain interfaces to be implemented in `androidApp` / `iosApp` because
they need an `Activity` / top `UIViewController` and present pickers.
