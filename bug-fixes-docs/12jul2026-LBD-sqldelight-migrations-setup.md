# SQLDelight Migrations — Setup & Workflow (P1/6)
**Date:** 12-Jul-2026 · **Platform:** LBD (local database, shared KMM) · SQLDelight 2.0.2

## What changed

`shared/build.gradle.kts` → `sqldelight` block now has `schemaOutputDirectory` (schema snapshots) and `verifyMigrations = true` (build fails if a migration doesn't produce exactly the `.sq` schema). **No driver changes were needed** — both `AndroidSqliteDriver` and `NativeSqliteDriver` already auto-run `.sqm` files when the schema version bumps (version = number of `.sqm` files + 1, tracked in SQLite's `user_version`).

## Baseline decision

**Current schema = version 1. No migration written today.** Old-shape databases are already gone (the recent model refactor forced reinstalls), so every live dev install is on the current shape at `user_version = 1`. Writing a retroactive `1.sqm` for the old→new shape would *break* those installs (duplicate columns). From this point forward, **schema changes never require a reinstall again**.

## One-time action (on your Mac, once)

```bash
cd Pustakm
./gradlew generateCommonMainNotesDatabaseSchema
```

This writes `shared/src/commonMain/sqldelight/databases/1.db` — the baseline snapshot. **Commit it.** Future migrations are verified against it at build time.

## Workflow for every future schema change

1. **Edit `NotesDatabase.sq`** to the new shape (this stays the single source of truth).
2. **Create `<n>.sqm`** next to it — `shared/src/commonMain/sqldelight/com/app/pustakam/database/1.sqm` for the first change (migrates v1→v2), `2.sqm` for the next, etc. Plain SQL only.
3. **Regenerate + commit the snapshot:** `./gradlew generateCommonMainNotesDatabaseSchema` (produces `2.db`).
4. Build — `verifyMigrations` proves snapshot + your `.sqm` == the new `.sq`. If it fails, the migration is wrong, not the build.
5. Ship. Both apps upgrade user data in place on next launch.

## Worked example (what the recent refactor WOULD have shipped as `1.sqm`)

Additive columns are one-liners; **type changes need the SQLite 12-step rebuild** (SQLite can't ALTER a column type):

```sql
-- additive columns: trivial
ALTER TABLE NoteContent ADD COLUMN title TEXT;
ALTER TABLE NoteContent ADD COLUMN mimeType TEXT;
ALTER TABLE NoteContent ADD COLUMN sizeBytes INTEGER;
ALTER TABLE NoteContent ADD COLUMN width INTEGER;
ALTER TABLE NoteContent ADD COLUMN height INTEGER;
ALTER TABLE NoteContent ADD COLUMN thumbnailPath TEXT;

-- type change (position INTEGER → REAL): rebuild
ALTER TABLE NoteContent RENAME TO NoteContent_old;
CREATE TABLE NoteContent(
  id TEXT NOT NULL PRIMARY KEY,
  noteId TEXT NOT NULL REFERENCES Notes(id) ON DELETE CASCADE,
  type TEXT NOT NULL,
  position REAL,
  createdAt TEXT, updatedAt TEXT,
  localPath TEXT, url TEXT, text TEXT,
  lat REAL, long REAL, address TEXT, duration INTEGER,
  title TEXT, mimeType TEXT, sizeBytes INTEGER,
  width INTEGER, height INTEGER, thumbnailPath TEXT,
  metaData TEXT
);
INSERT INTO NoteContent SELECT * FROM NoteContent_old;
DROP TABLE NoteContent_old;
```

Notes: `.sqm` is raw SQL — no `AS RichTextMetadata` annotations (adapters come from the `.sq`). The temporary `DROP TABLE ..._old` inside a migration is standard practice (it's the migration's own scratch table, not user data).

## Rules of thumb

- **Never edit an already-shipped `.sqm`** — installs that ran it won't run it again. Fix-forward with the next version.
- Prefer **additive** changes (new nullable columns with defaults) — they're one-line migrations and match the model's additive-with-defaults policy (`coerceInputValues` + Kotlin defaults).
- If `verifyMigrations` complains before you've generated the first snapshot, run the generate task from the one-time action above first.

## Status

- [x] Gradle config (`schemaOutputDirectory`, `verifyMigrations`)
- [x] Baseline decision documented (current = v1)
- [ ] **You:** run `generateCommonMainNotesDatabaseSchema` once + commit `1.db`
- [ ] Next schema change ships as `1.sqm` using the workflow above
