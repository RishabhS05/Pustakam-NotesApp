# Pustakam — Intelligent Notebook Canvas: Feature Inventory & Roadmap

Vision: a canvas-first intelligent notebook — write, draw, record audio, capture video/images, version and share notebooks, track changes, edit media, AI chat, mindmaps, feature planning, export to MP4/JPEG/MD/PDF, share anywhere. **Offline-first, backend later.**

---

## 1. Feature Inventory — Existing vs Future (platform-wise)

Legend: ✅ done · 🟡 partial/needs work · ❌ not started

### 1.1 Shared KMM (`shared/`)

| Feature                                                 | Status | Notes                                                            | 
|---------------------------------------------------------|---|------------------------------------------------------------------|
| Notes CRUD (offline, SQLDelight)                        | ✅ | use-case layer, Koin DI, Result/Error handling                   |
| Tags CRUD                                               | ✅ | tags-state fixes documented (piece 4)                            |
| Content types: text, image, video, audio, link, location | ✅ | model refactor C1/C4/C6/C8 applied; PDF/GIF/DOCX round-trip ready |
| iOS bridge (use-cases-only, no lib)                     | ✅ | NotesBridge applied; NoteContentBridge use-case rework pending   |
| Auth use cases (login/signup)                           | 🟡 | exist; API calls commented (offline phase)                       |
| Preferences (DataStore)                                 | ✅ |                                                                  |
| Sync engine                                             | ❌ | `SyncStatus` enum added; repo API calls commented — Phase 3      |
| Note versioning / change history                        | ❌ | `updates: List<String>` reserved as version pointers             |
| DRAWING content type                                    | ❌ | prerequisite for canvas — see §4.1                               |
| File reader (.md,.pdf,.docx,.json)                      | ❌| NA                                                               |                                                                       |
| export into zip                                         |   ❌     | all the notes would be shared with in a bundle                   |

### 1.2 Android (`androidApp/`)

| Feature                            | Status | Notes                                                                     |
|------------------------------------|---|---------------------------------------------------------------------------|
| Notes list + editor (Compose)      | ✅ | staggered grid, multi-content editor                                      |
| Camera capture (image/video)       | ✅ | permissions + hardware manager                                            |
| Audio record + playback            | ✅ | media3, visualizer, PlayerViewModel                                       |
| Video playback                     | ✅ | media3                                                                    |
| Location content                   | ✅ | location service/client                                                   |
| Bluetooth manager                  | 🟡 | present; purpose to define                                                |
| Drawing canvas                     | ❌ | §4.1                                                                      |
| Rich text formatting               | 🟡 | RichTextMetadata plumbing exists, never persisted until recent fix; no UI |
| Image/video editing                | ❌ | §4.4                                                                      |
| Export (MD/JPEG/PDF/MP4)           | ❌ | §4.6                                                                      |
| Share sheet                        | ❌ | §4.7                                                                      |

### 1.3 iOS (`iosApp/`)

| Feature | Status | Notes |
|---|---|---|
| Notes list + editor (SwiftUI) | ✅ | works; clean bridge/VM migration |
| Camera capture | ✅ | |
| Audio record + playback + visualizers | ✅ | |
| Video playback | ✅ | |
| Login/Signup UI | 🟡 | offline stub |
| Theming + router | ✅ | @Observable, environment injection |
| Drawing canvas | ❌ | PencilKit — §4.1 |
| Everything else (edit/export/share/AI) | ❌ | parity with Android plan |

### 1.4 Backend (`PustakmServer/` — Node.js + native Mongo, stays JS)

| Feature | Status | Notes |
|---|---|---|
| Auth routes, users, devices | 🟡 | exists; unverified against clients |
| Notes routes | 🟡 | exists; model drift (`content` vs `contents`), broken `LocationContent` class — see note-model-redesign.md §1.4 |
| File upload | 🟡 | folder exists; media sync flow undefined |
| Versions, sharing links, AI proxy | ❌ | Phase 3–4 |

---

## 2. Strategy: Offline First → Backend

1. **Everything ships offline first.** Each feature must be fully usable with no account: local DB, local files, local export/share. This is already the codebase's direction (API calls commented in `NoteRepository`).
2. **Backend = sync + share + heavy AI, never a gatekeeper.** When Phase 3 lands, `SyncStatus` (already in the model) drives push/pull; the bridge and use-case APIs don't change — only repository internals (the payoff of the use-cases-only rule).
3. **Media files:** stored locally (`localPath`) now; upload → `url` later. `MediaContent` already carries both plus `mimeType/sizeBytes/checksum`-ready fields.

---

## 3. Scope & Recommended Features (honest cut)

**Recommended core (differentiators):** canvas/drawing, versioned notebooks with change tracking, multi-format export + share, explanation generator (§5). These make "intelligent notebook".

**Recommended defer:** full video *editing* (trim only at first — full editing is a product in itself), real-time collaboration (needs CRDT — do snapshots first), on-device AI (use cloud via backend proxy first), Bluetooth features (unclear value now).

**Scope warning:** the list (draw + record + edit + AI + mindmap + planner + export + share + versions) is 5–6 apps' worth. The phasing below is sequenced so every phase ships something usable; resist starting two phases at once — the current Notes refactor series (bridge, model, DAO) is the foundation everything sits on and must finish first.

---

## 4. How Each Future Feature Is Achieved

### 4.1 Drawing canvas (both platforms)
- New `DRAWING` ContentType + `NoteContentModel.Drawing` carrying **vector strokes as JSON** (points, width, color, tool) — platform-neutral, tiny, diffable for versioning.
- iOS: **PencilKit** (`PKCanvasView`) → serialize `PKDrawing` strokes to the shared JSON. Android: **Compose Canvas** ink layer (or Jetpack Ink when stable) → same JSON.
- Raster snapshot (PNG thumbnail) stored via existing media pipeline for list previews/export.

### 4.2 Versioning + change tracking (the "share a version of a notebook" feature)
- Local-first: `NoteVersion` table — `(versionId, noteId, snapshotJson, createdAt, label)`. Snapshot = the note's contents JSON (the document-storage direction in note-model-redesign.md makes this nearly free).
- Auto-snapshot on significant save + manual "save version" button; `updates: List<String>` holds version ids (your reserved field).
- Change tracking UI: diff two snapshots by content id — added/removed/modified list.
- Later (Phase 3): versions sync to Mongo; "share version" = server link to an immutable snapshot.

### 4.3 Mindmap + feature planner
- Both are **renderers over note structure**, not new data silos: mindmap = graph of notes/tags/links (shared KMM graph model, drawn with Compose Canvas / SwiftUI Canvas); planner = a board view over notes with a `status` tag (kanban columns).
- Keeps them offline, versioned, exportable for free.

### 4.4 Image / video editing
- Image (Phase 2): crop/rotate/annotate. iOS: Core Image + drawing overlay (reuses 4.1 canvas!). Android: Compose-based crop + overlay. **Non-destructive:** store edit operations in content metadata, render on demand.
- Video (Phase 4): trim first. Android: **media3 Transformer**. iOS: **AVFoundation** (`AVAssetExportSession`). Full editing deferred.

### 4.5 AI chat
- Phase 4, via backend proxy (`/ai/chat`) → Claude/OpenAI — keeps API keys off devices, enables usage limits.
- Chat is note-aware: context = current note's contents serialized to MD (the export pipeline §4.6 doubles as the AI context builder).

### 4.6 Export pipeline (MD / JPEG / PDF / MP4)
One shared `Exporter` use case + platform renderers:
- **MD**: pure Kotlin — walk `contents`, emit markdown (text→text, media→links/paths, location→maps URL). Free and first.
- **JPEG/PNG**: render the note view — Android `ComposeView` → bitmap; iOS `ImageRenderer`. Drawings already have raster snapshots.
- **PDF**: Android `PdfDocument`; iOS `UIGraphicsPDFRenderer` over the same rendered pages.
- **MP4** (Phase 4): slideshow = images + audio track muxed — Android media3 Transformer; iOS `AVAssetWriter`. Pairs with §5.

### 4.7 Easy share (mail/WhatsApp/anything)
- Android: `Intent.ACTION_SEND` with correct mimeType (from `MediaContent.mimeType` — already in the model). iOS: `ShareLink`/`UIActivityViewController`.
- Share = export (§4.6) + system share sheet. No per-app integrations needed — the OS routes to WhatsApp/mail/etc. by format.

---

## 5. Explanation Generator (multi-format output)

"Explain this note as audio / image / video / text" — a flagship intelligent feature.

```
Note → MD serialization (§4.6)
     → AI summarize/explain (backend proxy; script + scene list)
     → format renderers:
        • TEXT/MD  : the explanation itself                      [Phase 2 — no AI needed for v0: rule-based summary]
        • AUDIO    : TTS — iOS AVSpeechSynthesizer / Android TextToSpeech (offline!),
                     cloud TTS later for quality → saved as audio content / mp3 share
        • IMAGE    : explanation card — template render via §4.6 JPEG pipeline
        • VIDEO    : scenes (explanation images) + TTS audio → §4.6 MP4 muxer
```

Key design: the generator **reuses** export + TTS + muxing — it's an orchestration use case (`GenerateExplanationUseCase(noteId, format)`), not new infrastructure. Output lands back in the note as a content item (so it's versioned and shareable like everything else).

### 5.1 Voice narration / Tour-Guide mode (multi-language) 🎙

"Read or explain my note aloud, in my chosen language — like a tour guide."

```
NarrateNoteUseCase(noteId, languageCode, mode)
  1. SCRIPT   walk contents in position order → narration script
              (text read verbatim; media announced by title/type; location spoken as
               address — "you kept a location here: Panaji, Goa"; link titles read out)
  2. TRANSLATE (skip if target = note language)
              • Android: ML Kit on-device translation (offline, ~59 languages)
              • iOS: Apple Translation framework (iOS 17.4+/18, on-device)
              • fallback/quality: backend proxy translation (Phase 3+)
  3. SPEAK    platform TTS with the target-language voice
              • iOS: AVSpeechSynthesizer  • Android: TextToSpeech
              both OFFLINE, both ship predefined multi-language voice packs
  4. (optional) SAVE  render to audio file via §5 pipeline → audio content in the note
```

- **Predefined languages:** a curated config list (e.g. en, hi, es, fr, de, ja, ar…) filtered at runtime by which TTS voices are actually installed on the device — never offer a voice that can't speak.
- **Tour-guide UX:** play/pause/skip per content block, current block highlighted in the editor as it's spoken (walkthrough feel), speed control. Driven by TTS utterance callbacks (both platforms expose them).
- **Two modes:** `READ` (verbatim, zero AI, fully offline — ships in P2) and `EXPLAIN` (AI-generated guide script via §5, P4).
- Model impact: none required — narration is derived. Optional nice-to-have: `language: String?` on Note so auto-detect can be skipped.

---

## 6. Phased Roadmap

| Phase | Theme | Contents | Backend? |
|---|---|---|---|
| **P0** (now) | Stabilize foundation | finish refactor series (bridge, NoteContentBridge use-cases, model fixes C2, DAO transactions, list-state piece 3), both apps build green, editor/list parity | ❌ |
| **P1** | Canvas + capture complete | DRAWING type + PencilKit/Compose canvas, rich-text UI, MD + JPEG export, system share sheet | ❌ |
| **P2** | Intelligent notebook (local) | versioning + change tracking, diff UI, mindmap + planner views, image editing, PDF export, explanation v0 (rule-based + offline TTS audio), **voice narration READ mode + on-device translation (§5.1)** | ❌ |
| **P3** | Backend + sync | fix server model (redesign doc §2.6), JWT auth live, note/media sync via SyncStatus, version share links | ✅ |
| **P4** | AI + heavy media | AI chat (proxy), explanation generator with AI, MP4 export, video trim/edit | ✅ |

Rule of thumb per feature: **shared model + use case first (KMM) → Android UI → iOS UI via bridge** — the pattern the Notes CRUD migration is establishing now.

## 7. Model Fitness — is Note/NoteContentModel a perfect fit for this vision?

**Verdict: strong foundation, six gaps.** What already fits well: sealed content types + `type` discriminator (new formats = new subclass or enum entry — Drawing slots in cleanly), factory-owned ids (sync-safe), immutable edits with auto-`updatedAt`, media metadata (`mimeType/sizeBytes/width/height/thumbnailPath` — export & upload ready), `Double` positions (reordering), `updates` reserved for version pointers.

### Gaps against the roadmap (all additive — no breaking migration needed)

| # | Gap | Blocks | Fix (when its phase starts) |
|---|-----|--------|------------------------------|
| G1 | **No 2-D canvas layout.** `position: Double` is 1-D ordering; a true canvas (place anything anywhere, resize) needs a frame per content | P1 canvas vision | add optional `frame: Frame?` (`x, y, width, height, rotation, zIndex`) to `NoteContentModel` base — null = classic list layout. **Decide early: canvas-freeform or ordered-page?** This is the biggest product decision hiding in the model |
| G2 | **No DRAWING type** | P1 drawing | `@SerialName("DRAWING") data class Drawing(strokesJson, thumbnailPath, …)` — the pattern is ready |
| G3 | **No soft delete / content-level trustworthy timestamps.** `isDeletedContent` was removed; `TextContent.text` is still `var` so content `updatedAt` can't be stamped on edit (C2) | P2 versioning/diff, P3 sync | re-add `isDeleted: Boolean = false` on Note + contents when versioning starts; finish C2 with a `withText()` that stamps `updatedAt` |
| G4 | **Sync fields half-migrated.** `Note.isSynced: Boolean?` coexists with the (unused) `SyncStatus` enum; contents have no sync state; `MediaContent` lacks `checksum` for upload integrity | P3 sync | replace `isSynced` → `syncStatus: SyncStatus = LOCAL_ONLY` on Note AND contents; add `checksum: String? = null` to MediaContent |
| G5 | **Single `categoryId` = one tag per note; no note↔note relations.** Mindmap needs edges; planner wants multiple labels | P2 mindmap/planner | `tagIds: List<String> = emptyList()` (keep `categoryId` during migration) + `NoteLink` content subtype or relations table for internal note references |
| G6 | **No notebook grouping.** Vision says "share a version of a *notebook*" — today there are only flat notes | P2 versions, P3 share | decide: `Notebook` entity (id, title, noteIds, cover) **or** promote Tag to notebook role. Recommended: real `Notebook` entity — sharing/versioning semantics get much cleaner |

Minor (nice-to-have, zero urgency): note-level UX fields (`pinned`, `color`, `coverImage`), `generatedBy: String?` on contents to mark AI-generated explanations (§5), per-content `origin` for imported files.

### Evolution rule
Every gap fix is **additive with defaults** — old JSON/DB rows keep decoding (`coerceInputValues` + defaults already configured in C8). Sequence each fix into the phase that needs it (column 3); adding them all now would be speculative modeling. The one exception worth deciding **before P1 code starts** is G1 — canvas-freeform vs ordered-page changes how the editor UI is built, not just the model.

## 8. Open decisions (need your call when their phase starts)

1. **Canvas model (G1, decide before P1):** freeform 2-D canvas (`frame` per content) vs ordered page (current). Shapes the whole editor.
2. Drawing stroke format: custom JSON (recommended, cross-platform) vs PencilKit-native (iOS-lossless but Android can't read it).
2. Versioning granularity: every save vs manual + interval (recommended: manual + smart auto on big diffs).
3. AI provider + billing model for the proxy (Phase 4).
4. MP4 explanation style: slideshow (cheap, recommended) vs animated canvas replay (expensive, spectacular).
5. Narration languages (§5.1): which predefined set ships first, and whether EXPLAIN mode waits for P4 AI or gets a rule-based P2 version.
