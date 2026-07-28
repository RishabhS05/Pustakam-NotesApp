# 15-Jul-2026 · Note & NoteContent Scaling — Can the current model handle 1000–2000 pages?

**Question asked:** If a note grows very large — 1000 to 2000 pages, or thousands of data nodes
(many text blocks, media, locations, links) — can the current `Note` / `NoteContentModel` design
fetch and manage that much data? And are we associating the note data points correctly?

---

## 1. Short answer

- **The database design: YES, it is ready.** Contents are stored the right way — as separate,
  properly associated rows (data points), not as one big blob. This part can hold millions of rows.
- **The fetching / updating strategy: NO, not yet.** Everything today is *all-or-nothing*: opening
  a note loads **every** content row into memory at once, and every save **rewrites every row**.
  This is fine for tens or a few hundred blocks. At thousands of blocks (1000–2000 pages) the app
  will slow down, stutter, and eventually run out of comfortable memory — not because of the model,
  but because of *how much of it we load and rewrite at a time*.

So: the foundation (schema + associations) is correct; the loading strategy must evolve from
"load the whole note" to "load the note in pages/windows".

---

## 2. How it works today (the facts, with file references)

### Storage — normalized and associated (GOOD)
- `Notes` table = note header only (id, title, category, timestamps).
- `NoteContent` table = **one row per data point**, linked with
  `noteId REFERENCES Notes(id) ON DELETE CASCADE` — so yes, every content block is a real,
  associated data point, and deleting a note automatically deletes its contents.
  (`shared/src/commonMain/sqldelight/com/app/pustakam/database/NotesDatabase.sq`)
- Ordering uses `position REAL` (fractional ordering) — inserting a block between two others
  does not require renumbering everything. This is exactly the right primitive for big documents.
- Media rows only store **pointers** (localPath / url / thumbnailPath) plus metadata — the heavy
  bytes stay on disk, never in the model. Also correct.

### Fetching — eager and unbounded (THE PROBLEM)
- `selectById` joins the note with **ALL** of its content rows and maps them into one
  `ArrayList<NoteContentModel>` in a single shot (`NotesDao.selectNoteById`). No LIMIT.
- The notes LIST screen is worse: `selectAllNotesFromDb()` runs `selectWithAllContent` which joins
  **every note × every content row in the whole database**, then groups in memory. The `limit` and
  `page` parameters are computed but **ignored** — the `LIMIT ? OFFSET ?` in the .sq file is
  commented out (`NotesDatabase.sq:138`, `NotesDao.kt:49-52`).
- The join also duplicates the note's header columns onto every content row (row explosion),
  so 5,000 blocks = 5,000 copies of the note header travelling through the mapper.

### Editing — whole-note copies (THE SECOND PROBLEM)
- The editor holds all blocks in one `SnapshotStateList` (`NoteContentUiState.contents`) and the
  `LazyColumn` calls `sortedBy { position }` **on every recomposition** — an O(n·log n) sort per
  UI frame (`NotesEditorView.kt`, contentList).
- Every content update rebuilds the whole immutable `Note` via `withContents(...)` — one keystroke
  copies a list of N items.
- Every save (`createOrUpdateNote`) re-INSERTs the note **and all of its content rows** inside one
  transaction (`NotesDao.insertOrUpdateNoteFromDb`) — 5,000 blocks = 5,000 `INSERT OR REPLACE`
  statements per save, even when one text block changed.
- Sync sends the **whole note as one JSON** to the server.

### Media — all registered at once (MINOR PROBLEM)
- `NoteContentRepository` keeps every media block of the open note in memory and the ExoPlayer
  playlist is built with **all** of them at once. Fine for 10 media items; wasteful for 500.

---

## 3. Rough capacity today (honest numbers)

| Scale | What happens |
|---|---|
| ~100 blocks / few pages | Works fine (current sweet spot) |
| ~1,000 blocks | Noticeable: slower note open, jank while typing (whole-list copy + per-frame sort), slow saves |
| ~5,000–10,000 blocks (≈1000–2000 pages) | Painful: seconds to open, every save rewrites thousands of rows, GC pressure, possible ANRs; list screen loads the entire DB |
| One single giant TextContent (megabytes of text) | SQLite stores it easily, but Compose `TextField` re-copies the whole string per keystroke — must be split into smaller blocks |

SQLite itself is not the limit — it handles millions of rows. The limit is loading them all at once.

---

## 4. What is already good (keep as is)

1. **Normalized schema** — one row per content data point, FK + `ON DELETE CASCADE`. ✅
2. **Sealed `NoteContentModel`** — clean typed nodes (Text / Media / Link / Location). ✅
3. **Fractional `position REAL`** — cheap inserts between blocks, no renumber storms. ✅
4. **Media stored as pointers + metadata (thumbnailPath, sizeBytes, w/h)** — heavy bytes off-model. ✅
5. **Stable content ids generated once** (`NoteContentObjectHelper`) — safe for sync and diffing. ✅

These are exactly the building blocks a large-document editor needs. Nothing must be thrown away.

---

## 5. Future scope — the plan, in phases

### Phase 0 — Quick wins (no schema change, days of work)
1. **Turn on paging for the notes list**: un-comment `LIMIT ? OFFSET ?` in `selectWithAllContent`
   and actually use the `limit/page` parameters in `selectAllNotesFromDb`.
   Better: add a **summary query** for the list screen — note header + first text snippet +
   content count + first thumbnail — never join full contents for a list.
2. **Add an index**: `CREATE INDEX idx_notecontent_note_position ON NoteContent(noteId, position);`
   Today only the primary key is indexed, so `WHERE noteId = ? ORDER BY position` scans the table.
3. **Stop sorting per frame**: keep `contents` sorted on insert in the ViewModel; remove
   `sortedBy` from the `LazyColumn` body.
4. **Save only what changed (dirty tracking)**: remember which content ids were edited and
   `INSERT OR REPLACE` only those rows, instead of rewriting the entire note on every save.

### Phase 1 — Windowed content loading (the real scaling step)
1. New queries for **content pages**, using keyset pagination on position (faster than OFFSET):
   `SELECT * FROM NoteContent WHERE noteId = ? AND position > ? ORDER BY position LIMIT ?;`
2. Editor opens with the **first window** (e.g. 50–100 blocks) and loads the next window when the
   user scrolls near the end (LazyColumn already virtualizes rendering — we just stop feeding it
   everything up front).
3. Split the model roles: a light **NoteMeta** (id, title, timestamps, block count) for screens
   and lists, and paged `NoteContentModel` windows for the editor body. The existing `Note` class
   can stay for small notes and API compatibility.
4. **Media playlist windowing**: register only the media blocks in (or near) the loaded window in
   `NoteContentRepository` / ExoPlayer, keeping the id-based selection from 14-Jul-2026.

### Phase 2 — Big text strategy
1. **Cap the size of one TextContent block** (e.g. ~4–8 KB, roughly a paragraph/section).
   The editor auto-splits long text into multiple blocks; fractional `position` makes the split
   cheap. This keeps every keystroke-copy small no matter how long the document is.
2. **Full-text search**: add an SQLite **FTS5** virtual table mirroring `NoteContent.text` so
   searching a 2000-page note (or all notes) stays instant.
3. Lazy thumbnail generation for media so lists/scrolling never decode full images or video frames.

### Phase 3 — Sync & backend
1. **Block-level sync**: send only created/updated/deleted content rows (id + updatedAt/version)
   instead of the whole note JSON; the schema's stable ids + timestamps already support this.
2. **Server-side pagination** for fetching a large note (same windowing idea over the API).
3. Conflict handling per block (last-write-wins per content id is a reasonable start) — much safer
   than whole-note overwrites once notes get big.

### Suggested priority
| Priority | Item | Why first |
|---|---|---|
| 1 | Phase 0.1 + 0.2 (list paging + index) | List screen currently loads the entire DB |
| 2 | Phase 0.4 (dirty-row saves) | Saves are the heaviest repeated cost |
| 3 | Phase 1 (windowed editor loading) | Unlocks 1000+ page notes for real |
| 4 | Phase 2 (text block cap + FTS) | Typing comfort + search at scale |
| 5 | Phase 3 (block-level sync) | Needs 0/1 in place; biggest backend change |

---

## 6. One-line summary

The **associations and schema are already correct and scalable** (contents are real, linked data
points); what must change for 1000–2000 page notes is the **strategy**: page the list queries,
index the content table, window the editor's content loading, cap text-block size, save only dirty
rows, and sync per block instead of per note.
