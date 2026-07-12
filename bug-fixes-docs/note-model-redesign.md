# Note Model Redesign — Backend → Shared KMM → Android/iOS

**Documentation only — nothing applied.**
Goal: one clean, extensible content model for **all formats** (text, image, video, audio, link, location, docx, pdf, gif, + future types) that flows unchanged from MongoDB → Node API → KMM shared → Android Compose / iOS SwiftUI, and is ready for the offline→online sync phase.

---

## 1. Current-State Audit (what's wrong, layer by layer)

### 1.1 Kotlin `Note` (commonMain)

| # | Problem | Consequence |
|---|---------|-------------|
| M1 | Mutable data class: `var title`, `var contents`, `var updatedAt`, `var categoryId`… | Editor mutates shared instances in place → StateFlow can't detect changes; the exact class of bugs found in refactor Piece 3 |
| M2 | Hand-written `equals` = **id only**; `hashCode` inconsistent with it (mixes title/updatedAt/contents count) | Two different versions of a note compare "equal" → StateFlow skips emissions → stale UI. Breaks Set/Map contracts |
| M3 | `contents: List<NoteContentModel>?` nullable **and** defaulted to `emptyList()` | `note.contents!` force-unwraps all over Swift/Kotlin; nullability carries no meaning |
| M4 | `updates: List<String>?` — dead field | Noise in every serialization |
| M5 | `isSynced: Boolean?` — three-state boolean, never maintained | Useless for the sync phase it exists for |

### 1.2 Kotlin `NoteContentModel` (sealed class)

| # | Problem | Consequence |
|---|---------|-------------|
| C1 | `MediaContent` is a catch-all for IMAGE/VIDEO/AUDIO/DOCX with a runtime `type` field + `title: String = "Audio"` default | Format-specific data has nowhere to live (no mimeType, size, dimensions, thumbnail); wrong defaults leak ("Audio" title on images) |
| C2 | `var text` in `TextContent` — UI mutates it directly (`textContent.text = newValue.string` in `NoteEditorView`) | Same in-place mutation disease as M1 |
| C3 | Timestamps are `String?` built by interpolation (`"${getCurrentTimestamp()}"`) | Can't compare/order/conflict-resolve; nullable for no reason |
| C4 | `position: Long` dense indexing | Reordering/insert-between requires rewriting every row's position |
| C5 | `isDeletedContent` exists in the model but **not in the DB schema** and is never set | Soft-delete (needed for sync) impossible today |
| C6 | `id`/timestamps generated in default args | Deserialization can silently regenerate ids if a field is absent — data corruption risk |
| C7 | Custom `equals`/`hashCode` on the sealed base (position/id/dates only) | Text edits compare equal → same emission bugs |
| C8 | No kotlinx polymorphic discriminator config matching the wire `type` field | Server JSON ↔ sealed class round-trip needs fragile manual mapping |

### 1.3 SQLDelight schema

| # | Problem |
|---|---------|
| S1 | `NoteContent` has **no `title` column** — yet the DAO reads `row.title` from the join (that's `Notes.title`!) and saves media titles nowhere. Media titles are silently lost + wrong title displayed |
| S2 | No `isDeleted`, no `syncStatus`, no `mimeType`/`size` — schema can't carry sync or rich media metadata |
| S3 | `position` nullable; no index on `NoteContent.noteId`; `selectWithAllContent` loads the whole DB (pagination computed but unused — known) |
| S4 | `metaData AS RichTextMetadata` exists only conceptually for text; typed adapter fires for all rows |

### 1.4 Backend (`PustakmServer/models/Note.js`) — plain JS + native Mongo driver (kept, per project rule)

| # | Problem |
|---|---------|
| B1 | `this.isSynced = true` ignores the constructor param (hardcoded) |
| B2 | `LocationContent extends Content` calls `super(this._id, ...)` — **reads `this` before `super()` runs: throws at runtime**. Also passes positional args to an object-destructuring constructor. This class is broken |
| B3 | `Content` has no `duration`, `title`, `localPath`(n/a), `mimeType`, `metadata`, `lat/long` fields → media/location data is **dropped on sync** |
| B4 | Field-name drift: Kotlin `contents` vs backend `content`; no `categoryId` on the server note |
| B5 | No validation — any shape lands in Mongo |

---

## 2. Target Design

### 2.1 Principles

1. **One wire contract.** The kotlinx-serialization JSON of the shared model IS the API contract. Mongo stores the same shape (embedded `content` array with a `type` discriminator). No per-layer translation beyond DB rows.
2. **Immutable models** (`val` everywhere). Editing = `copy()` → new instance → StateFlow/`@Published` always emit. Kills M1/C2/M2/C7 class of bugs permanently.
3. **Closed core + open payload.** The sealed hierarchy stays small (`Text`, `Media`, `Link`, `Location`); *formats* (image/video/audio/pdf/docx/gif/…) are data on `Media`, not new classes — adding a format = adding an enum entry + renderer, zero model surgery.
4. **Sync-ready from day one:** every entity carries `syncStatus` + soft delete; timestamps are comparable (`Long` epoch millis).
5. **Default equality.** Delete every hand-written `equals`/`hashCode` — data-class structural equality is exactly what StateFlow needs.

### 2.2 New shared model (commonMain) — full code

```kotlin
package com.app.pustakam.data.models.response.notes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Sync lifecycle for offline-first (replaces isSynced: Boolean?). */
@Serializable
enum class SyncStatus { LOCAL_ONLY, SYNCED, PENDING_UPDATE, PENDING_DELETE }

@Serializable
data class Note(
    @SerialName("_id") val id: String,
    val title: String = "",
    val categoryId: String? = null,
    val createdAt: Long,                       // epoch millis — comparable (fixes C3)
    val updatedAt: Long,
    val contents: List<NoteContentModel> = emptyList(),   // non-null (fixes M3)
    val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,   // fixes M5
    val isDeleted: Boolean = false             // soft delete for sync
)   // NO custom equals/hashCode (fixes M2) — data class structural equality

/** File/media payload shared by every binary format — image, video, audio, pdf, docx, gif…
 *  New format = new ContentType entry + renderer. No new model class. */
@Serializable
data class MediaAsset(
    val url: String = "",                      // remote (after upload)
    val localPath: String? = null,             // device file (before/after upload)
    val mimeType: String = "",                 // "video/mp4", "application/pdf"…
    val sizeBytes: Long = 0,
    val durationMs: Long = 0,                  // audio/video
    val width: Int = 0, val height: Int = 0,   // image/video/gif
    val thumbnailPath: String? = null,
    val checksum: String? = null               // sync integrity / dedupe
)

@Serializable
sealed class NoteContentModel {
    @SerialName("_id") abstract val id: String
    abstract val noteId: String
    abstract val position: Double              // fractional ordering (fixes C4):
                                               // insert between a and b = (a+b)/2, no rewrites
    abstract val createdAt: Long
    abstract val updatedAt: Long
    abstract val isDeleted: Boolean            // persisted soft delete (fixes C5)
    abstract val syncStatus: SyncStatus

    @Serializable @SerialName("TEXT")          // ← wire discriminator (fixes C8)
    data class Text(
        override val id: String,
        override val noteId: String,
        override val position: Double,
        override val createdAt: Long,
        override val updatedAt: Long,
        override val isDeleted: Boolean = false,
        override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
        val text: String = "",                 // val, not var (fixes C2)
        val metadata: RichTextMetadata? = null
    ) : NoteContentModel()

    @Serializable @SerialName("MEDIA")
    data class Media(
        override val id: String,
        override val noteId: String,
        override val position: Double,
        override val createdAt: Long,
        override val updatedAt: Long,
        override val isDeleted: Boolean = false,
        override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
        val mediaType: ContentType,            // IMAGE / VIDEO / AUDIO / PDF / DOCX / GIF…
        val title: String = "",                // per-item, persisted (fixes S1, C1)
        val asset: MediaAsset = MediaAsset()
    ) : NoteContentModel()

    @Serializable @SerialName("LINK")
    data class Link(
        override val id: String, override val noteId: String,
        override val position: Double,
        override val createdAt: Long, override val updatedAt: Long,
        override val isDeleted: Boolean = false,
        override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
        val url: String = "", val title: String = "", val previewImage: String? = null
    ) : NoteContentModel()

    @Serializable @SerialName("LOCATION")
    data class Location(
        override val id: String, override val noteId: String,
        override val position: Double,
        override val createdAt: Long, override val updatedAt: Long,
        override val isDeleted: Boolean = false,
        override val syncStatus: SyncStatus = SyncStatus.LOCAL_ONLY,
        val latitude: Double = 0.0, val longitude: Double = 0.0, val address: String? = null
    ) : NoteContentModel()
    // NO custom equals/hashCode anywhere (fixes C7)
}
```

Notes on the design:

- **ids/timestamps are required params, never default-generated** (fixes C6). Construction goes through the factory (below), deserialization can't regenerate anything.
- `ContentType` enum stays (TEXT/IMAGE/VIDEO/AUDIO/LINK/DOCX/LOCATION/PDF/GIF) but for `Media` it's *data*, not a class discriminator — the wire `type` is `TEXT|MEDIA|LINK|LOCATION`.
- kotlinx config (one line where Json is created): `Json { classDiscriminator = "type"; ignoreUnknownKeys = true }` — server and client agree by construction.

### 2.3 Factory replaces scattered defaults (`NoteContentObjectHelper` v2)

```kotlin
object NoteContentFactory {
    private fun now() = getCurrentTimestampMillis()

    fun text(noteId: String, position: Double, text: String = "") =
        NoteContentModel.Text(id = UniqueIdGenerator.generateUniqueId(), noteId = noteId,
            position = position, createdAt = now(), updatedAt = now(), text = text)  // uses text (fixes old bug)

    fun media(noteId: String, position: Double, mediaType: ContentType, asset: MediaAsset, title: String = "") =
        NoteContentModel.Media(id = UniqueIdGenerator.generateUniqueId(), noteId = noteId,
            position = position, createdAt = now(), updatedAt = now(),
            mediaType = mediaType, title = title, asset = asset)
    // link(...), location(...) same pattern
}
```

### 2.4 Copy helpers for Swift (important!)

Kotlin `data class copy()` **does not export usable defaults to Swift** — Swift would have to pass every argument. Ship explicit mutators in commonMain so both platforms edit immutably:

```kotlin
fun NoteContentModel.Text.withText(newText: String) =
    copy(text = newText, updatedAt = getCurrentTimestampMillis(), syncStatus = SyncStatus.PENDING_UPDATE)

fun Note.withTitle(newTitle: String) =
    copy(title = newTitle, updatedAt = getCurrentTimestampMillis(), syncStatus = SyncStatus.PENDING_UPDATE)

fun Note.withContents(newContents: List<NoteContentModel>) =
    copy(contents = newContents, updatedAt = getCurrentTimestampMillis(), syncStatus = SyncStatus.PENDING_UPDATE)

fun NoteContentModel.markDeleted(): NoteContentModel = /* per-subtype copy(isDeleted=true, syncStatus=PENDING_DELETE) */
```

Swift usage: `content.withText(newText: value)` — no `var`, no in-place mutation, StateFlow/`@Published` always fire.

### 2.5 Storage strategy (REV 2) — document-style, single table

> **Rev 2 decision.** The first draft kept two normalized tables (`Notes` + `NoteContent`) — which preserves exactly the pain being complained about: fetch = join → flatten → `groupBy(noteId)` → ~80-line row→sealed-class mapper, duplicated, drifting. Rev 2 replaces it with **document storage**: contents live as one JSON column on the note row, decoded automatically by a SQLDelight `ColumnAdapter` — the same mechanism the schema already uses for `metaData AS RichTextMetadata`.

**Why document storage is right for THIS app (usage-pattern audit):**

| Access pattern | Who | Needs |
|---|---|---|
| Load whole note w/ all contents | Editor, list cards | whole document |
| Save whole note | Editor (save-time materialization) | whole document |
| Sync note | `upsertNewNoteApi(note)` — API is note-granular | whole document |
| Media list of selected note | `NoteContentRepository` | in-memory from loaded Note |
| Delete one content | delete use case | **only per-content op** — handled in §2.5.3 |

Nothing in the app queries content rows across notes via SQL. When every consumer wants the whole document, store the document.

**Trade-offs (honest):**

| | Normalized (2 tables) | Document (1 table) — CHOSEN |
|---|---|---|
| Fetch/map complexity | join + groupBy + big mapper ×2 | `SELECT` → adapter decodes → done (~5 lines) |
| Write atomicity | multi-row transaction (currently broken — Piece 2/D) | single row = atomic by construction |
| Wire/Mongo alignment | translate rows ↔ JSON | **identical JSON** in SQLite, API, Mongo |
| Edit one content | 1-row UPDATE | rewrites contents JSON (notes are KB-scale — negligible) |
| SQL search inside contents | per-column LIKE | needs `searchText` derived column (below); FTS5 later if wanted |
| Per-content sync granularity | possible | note-granular — matches the actual API |

#### 2.5.1 Schema v2

```sql
CREATE TABLE Notes (
  id TEXT NOT NULL PRIMARY KEY,
  categoryId TEXT,
  title TEXT NOT NULL DEFAULT '',
  createdAt INTEGER NOT NULL,
  updatedAt INTEGER NOT NULL,
  isDeleted INTEGER NOT NULL DEFAULT 0,
  syncStatus TEXT NOT NULL DEFAULT 'LOCAL_ONLY',
  searchText TEXT NOT NULL DEFAULT '',                -- derived: title + all text blocks (search feature keeps working)
  contents TEXT AS List<NoteContentModel> NOT NULL    -- kotlinx JSON via ColumnAdapter
);
CREATE INDEX idx_notes_updatedAt ON Notes(updatedAt);

selectNotesPage:
SELECT * FROM Notes WHERE isDeleted = 0 ORDER BY updatedAt DESC LIMIT :limit OFFSET :offset;

searchNotes:
SELECT * FROM Notes WHERE isDeleted = 0 AND searchText LIKE '%' || :query || '%';
```

Adapter (registered where `RichTextMetadata`'s adapter already is):

```kotlin
private val json = Json { classDiscriminator = "type"; ignoreUnknownKeys = true }

val contentsAdapter = object : ColumnAdapter<List<NoteContentModel>, String> {
    override fun decode(databaseValue: String): List<NoteContentModel> = json.decodeFromString(databaseValue)
    override fun encode(value: List<NoteContentModel>): String = json.encodeToString(value)
}
```

#### 2.5.2 The DAO shrinks to almost nothing

```kotlin
fun upsertNote(note: Note) = queries.insertOrUpdateNote(
    id = note.id, categoryId = note.categoryId, title = note.title,
    createdAt = note.createdAt, updatedAt = note.updatedAt,
    isDeleted = if (note.isDeleted) 1 else 0, syncStatus = note.syncStatus.name,
    searchText = note.searchText(),            // title + joined Text blocks
    contents = note.contents)                  // adapter serializes

fun selectNotesPage(limit: Long, offset: Long): List<Note> =
    queries.selectNotesPage(limit, offset).executeAsList().map { it.toNote() }  // trivial field copy
```

The 80-line duplicated row mapper, the `groupBy`, the flattened join queries, and the broken multi-row transaction logic (Piece 2/D) **all cease to exist** — refactor Pieces 1 and 2 are absorbed rather than performed.

#### 2.5.3 Features that change internally (per "update the feature too" rule) — none break

| Feature | Today | After (same public behavior) |
|---|---|---|
| Delete one content (`DeleteNoteContentUseCase` → bridge `deleteNoteContent`) | `DELETE FROM NoteContent WHERE id=?` | repo: load note → `withContents(contents.filterNot { it.id == id })` → upsert. **Use case & bridge signatures unchanged** |
| Search screen | matches against Notes columns | same query against `searchText` (now also matches body text — strictly better) |
| Media list of selected note | filters loaded Note in memory | unchanged |
| List pagination | computed offset, never applied | actually works (`selectNotesPage`) |
| Tag CRUD | separate `Tags` table | untouched |
| Note delete cascade | FK cascade to NoteContent | nothing to cascade — contents are in the row |

#### 2.5.4 Migration v1 → v2 (code-assisted, one-time)

JSON can't be assembled in pure SQL, so the `.sqm` handles DDL and Kotlin backfills on first launch:

1. `1.sqm`: create `Notes_v2` (schema above).
2. On app start (before Koin serves the DAO): if legacy `NoteContent` table exists → read all notes via the *old* queries → map rows to new model once (timestamps `CAST` to Long, `position` to Double, media fields into `MediaAsset`) → `upsertNote` each into v2.
3. Rename legacy tables to `Notes_v1_backup` / `NoteContent_v1_backup` (⚠️ actual DROP deferred — your no-delete rule; drop ships in a later release once verified).
4. Rename `Notes_v2` → `Notes`.

### 2.6 Backend (`PustakmServer` — stays plain JS + native driver)

```js
// models/Note.js — plain objects, no broken inheritance (fixes B1/B2/B3)
export const makeNote = ({_id, userId, title = '', categoryId = null,
                          createdAt, updatedAt, contents = [], isDeleted = false}) =>
  ({_id, userId, title, categoryId, createdAt, updatedAt, contents, isDeleted})

// contents entries are passed through as-is; shape enforced by $jsonSchema below.
// Discriminated by `type`: TEXT | MEDIA | LINK | LOCATION (matches kotlinx @SerialName)
```

Native-driver validation (no mongoose, per project rule) — collection validator once at startup:

```js
db.command({ collMod: "notes", validator: { $jsonSchema: {
  required: ["_id", "userId", "title", "createdAt", "updatedAt", "contents"],
  properties: {
    createdAt: { bsonType: "long" }, updatedAt: { bsonType: "long" },
    contents: { bsonType: "array", items: {
      required: ["_id", "noteId", "type", "position", "createdAt", "updatedAt"],
      properties: { type: { enum: ["TEXT", "MEDIA", "LINK", "LOCATION"] } }
    }}
  }
}}})
```

Key alignment decisions: field is **`contents`** everywhere (kills B4 drift — server adopts the client name); timestamps are numbers; `LocationContent`/`Content` classes are replaced by the pass-through + validator (their data was being corrupted by B2 anyway). `localPath`/`thumbnailPath`/`syncStatus` are **client-only** — strip them in the route before insert (server keeps `url`, sets its own authority timestamps on write).

---

## 3. Wire format example (one JSON, three layers)

```json
{
  "_id": "n_9f2c", "title": "Trip plan", "categoryId": "t_1",
  "createdAt": 1752105600000, "updatedAt": 1752192000000,
  "isDeleted": false,
  "contents": [
    { "type": "TEXT",  "_id": "c_1", "noteId": "n_9f2c", "position": 1.0,
      "createdAt": 1752105600000, "updatedAt": 1752105600000, "text": "Pack list…" },
    { "type": "MEDIA", "_id": "c_2", "noteId": "n_9f2c", "position": 2.0,
      "createdAt": 1752105600000, "updatedAt": 1752105600000,
      "mediaType": "VIDEO", "title": "Beach",
      "asset": { "url": "https://…/v.mp4", "mimeType": "video/mp4",
                 "durationMs": 12000, "width": 1920, "height": 1080 } },
    { "type": "LOCATION", "_id": "c_3", "noteId": "n_9f2c", "position": 3.0,
      "createdAt": 1752105600000, "updatedAt": 1752105600000,
      "latitude": 15.29, "longitude": 73.91, "address": "Goa" }
  ]
}
```

Mongo stores exactly this document. kotlinx parses it into the sealed class with zero custom code. Adding PDF support later = `"mediaType": "PDF"` + a renderer. **No model, schema, or API change.**

---

## 4. Implementation in the current situation (phased, offline-first)

Ordered so each phase ships alone, aligned with the existing refactor series:

**Phase 1 — Shared model swap (KMM), biggest single step**
1. Add new model files (§2.2–2.4). Keep old `Note`/`NoteContentModel` compiling side by side for one commit if needed (new files first, then re-point imports).
2. Document-storage schema v2 + code-assisted migration (§2.5.1, §2.5.4).
3. New minimal DAO (§2.5.2) — refactor Pieces 1 **and** 2 are absorbed: the duplicated mapper and the broken multi-row transactions simply no longer exist.
4. Repoint per-content operations through whole-note updates (§2.5.3) — use-case and bridge signatures stay frozen.
5. `NoteRepository` state updates become trivial (Piece 3): immutable lists + default equality = StateFlow just works.

**Phase 2 — Client call sites**
6. Android editor/list ViewModels: replace mutation with `withX()` copies. Compose diffing improves for free (stable immutable items).
7. iOS: already positioned — the editor doc's single-source-of-truth state + save-time materialization maps 1:1 (`state.title` → `note.withTitle(...)`, contents → `withContents(...)`). Bridge/use-case signatures **unchanged** (they pass `Note` opaquely).
8. `NoteContentFactory` replaces `NoteContentObjectHelper` (Piece 5's `createText`-ignores-text bug dies here). Position values: `noteContents.count + 1` as Double; insert-between = midpoint.

**Phase 3 — Backend alignment (before Online phase)**
9. Fix `Note.js` (§2.6), add validator, rename `content` → `contents`, numeric timestamps.
10. Route mapping: strip client-only fields; server sets `updatedAt` authority on write.

**Phase 4 — Online sync (unblocked by all the above)**
11. `syncStatus` drives the sync engine: push `LOCAL_ONLY|PENDING_*`, reconcile by comparing `updatedAt` longs, purge `PENDING_DELETE` after server ack. The commented `*Api` calls in `NoteRepository` come back to life against the same model.

**Compatibility during rollout:** existing local data is preserved by the `1.sqm` backfill; existing server data (if any) needs a one-time script (`content`→`contents`, string→long timestamps, `Content`→typed entries). Old app versions can't read the new server shape — acceptable pre-release; otherwise version the endpoint (`/v2/notes`).

## 5. What this deletes/replaces (per your rule — listed for approval, nothing removed yet)

- `Note`/`NoteContentModel` custom `equals`/`hashCode` (replaced by data-class defaults)
- `updates: List<String>?`, `isSynced: Boolean?` fields (replaced by `syncStatus`)
- `NoteContentModel.MediaContent/TextContent/Link/Location` (replaced by §2.2 types)
- `NoteContentObjectHelper` (replaced by `NoteContentFactory`)
- Backend `Content`/`LocationContent` classes (replaced by pass-through + `$jsonSchema`)
- `NoteContent` SQL table + its queries + both DAO row-mappers (replaced by the `contents` JSON column — legacy tables kept as `_v1_backup` until you approve the drop)

## 6. Verification matrix

1. Round-trip: new-model JSON → Mongo → API → kotlinx → SQLDelight → back — byte-equal fields.
2. Migration: v1 DB with text/media/location rows → open app → all rows readable, positions ordered, no data loss.
3. StateFlow emission: edit title/text only → list re-emits (M2/C7 dead).
4. Media titles persist (S1 dead); every `ContentType` inserts + renders on both platforms.
5. Reorder: drag content between neighbors → only that row's `position` UPDATE.
6. Soft delete: content removed from UI, row retains `isDeleted=1` until sync purge.
7. Swift: `withText/withTitle` compile and produce new instances; no `var` mutation anywhere.
8. Backend: malformed content rejected by validator; B2 crash path gone.
