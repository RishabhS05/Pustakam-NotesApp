# Dependency Graph — `:core:filesys` Migration

**Last updated:** 30-Jul-2026 (end of Phase 2)

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

## 3. Internal graph of `:core:filesys` after Phase 2

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

  legacy shim (retired in Phase 5):
    fileimport/FileImportHelper → thin delegate over mime/ + naming/ + validation/
```

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

Topological order:
  mime, model, pagination, export, naming, validation, path, imports, fileimport   ✅ acyclic
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
only bytes. Scheduled for Phase 2.

### 5.3 Models — created as their consumer arrives

Phase 1: `CaptureDestination`, `FileMetadata`.
Phase 2: `ImportPlan`, `ImportDecision`, `TextPageChunk`, `MeasuredBlock`, `PlacedBlock`, `ExportLayout`.
Phase 3 will add `ThumbnailRequest` / `ThumbnailResult` with `ThumbnailGenerator`.
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

## 6. Platform-abstraction interfaces (Phase 3 — not yet created)

Per spec: small and single-responsibility, **no** god-object `FileStore`.

```
:core:filesys/platform/
    FileReader          read(relativePath): ByteArray?
    FileWriter          write(relativePath, bytes): Boolean
    DirectoryManager    ensure(folder), list(folder), exists(relativePath)
    FileCopier          copy(fromHandle, toRelativePath)
    FileDeleter         delete(relativePath)
    MetadataReader      read(relativePath): FileMetadata
    ThumbnailGenerator  generate(ThumbnailRequest): ThumbnailResult
    DocumentRenderer    pageCount(relativePath), renderPage(...)
```

Bound per platform via Koin, following the existing `getAndroidSpecifics()` pattern.
`MediaSaver` / `FileSharer` stay interfaces implemented in `androidApp` / `iosApp` because they need
`Activity` / top `UIViewController` and present pickers.
