# Pustakam Notes Backend — Implementation Roadmap & Planning

> **Status:** Planning document only. **No code reviewed, no code changed.**
> **Date:** 2026-07-17
> **Author basis:** Derived exclusively from project docs, architecture notes, API route declarations, and config files — never from source-code logic.
> **Goal:** Take `PustakmServer` from its current "exists but unverified" state to a production-ready Notes backend on par with Apple Notes / Samsung Notes: offline-first clients, reliable sync, media handling, sharing, and a path to AI features.
> **Scope target for this round:** a working, secure, sync-capable Notes API in **10 days**.

---

## 0. How to read this document

This plan is written in plain English first, then formalized. It answers five questions in order:

1. **What is the backend for, and how does it work today?** (§1)
2. **What already exists versus what must still be built?** (§2)
3. **How do we build the missing parts — in what order, with what dependencies?** (§3–§8)
4. **Can it realistically be done in 10 days, and where are the risks?** (§9–§10)
5. **How do we run, test, optimize, and grow it?** (§11–§15)

Sources this plan is built on (all in-repo):
`bug-fixes-docs/BACKEND_MODERNIZATION_PLAN.md`, `bug-fixes-docs/note-model-redesign.md`,
`future-scope/app-vision-feature-roadmap.md`, `future-scope/15jul2026-FIALBD-note-content-scaling-analysis-and-future-scope.md`,
plus `PustakmServer/package.json`, `server.js`, `routes/routes.js`, and `docker/mongodb-docker/docker-compose.yml`.

---

## 1. Current backend workflow, in simple English

Pustakam is a cross-platform notes app (Android + iOS, sharing a Kotlin Multiplatform core) whose entire product strategy is **offline-first**: everything works on the device with no account, and the backend exists to **sync, share, and later run heavy AI** — it is never a gatekeeper. Today the clients run fully offline; the server-side calls in the client repository are deliberately commented out.

The backend itself is a small **Node.js + Express 4** service written in **plain JavaScript** (ES modules), talking to **MongoDB** through the **native `mongodb` driver** (no Mongoose, by project rule). It boots from `server.js`, which parses JSON/form bodies, mounts one router, connects to Mongo, and listens on port 3000.

The workflow a client would follow today looks like this:

1. **Register / Login.** A user posts email + password to `/register` or `/login`. On login the server issues a **JWT** and (per the client contract) returns it in the `Authorization` response header. The client stores the token and sends it on later calls.
2. **Work with notes.** For a given user, the client can list notes, create a note, fetch a single note, update it, or delete it, through the `/notes/:userId` and `/notes/:userId/:id` routes.
3. **Manage the user record.** `/users/:id` supports get/update/delete of the user profile.
4. **Upload media.** `/profile` uploads a profile image and `/images` uploads note images, handled by Multer, written to a local `uploads/` folder on the server.
5. **Device config.** `/deviceConfig` is a stub endpoint intended to record device information (its CRUD is unfinished).

Every response is wrapped in a shared envelope, `BaseResponse { isSuccessful, data }`, which the KMM client depends on. **This wire contract must not change** without a coordinated client update.

**The honest caveat:** while these routes *exist*, the modernization audit found that several of them currently throw at runtime or contain security holes, and the notes data model on the server has drifted from the client model. So "exists" does not mean "works end-to-end." The current server is best understood as a **scaffold that compiles and boots but is not yet trustworthy against the real clients.** Turning that scaffold into a dependable Notes backend is what this plan is about.

### 1.1 Current API surface (as declared in `routes/routes.js`)

| Method | Path | Purpose | Auth expected |
|---|---|---|---|
| GET | `/` | Health/welcome message | No |
| POST | `/login` | Email+password login → JWT in `Authorization` header | No |
| POST | `/register` | Create user account | No |
| GET | `/notes/:userId` | List a user's notes | Yes |
| POST | `/notes/:userId` | Create a note for a user | Yes |
| GET | `/notes/:userId/:id` | Get one note | Yes |
| POST | `/notes/:userId/:id` | Update one note | Yes |
| DELETE | `/notes/:userId/:id` | Delete one note | Yes |
| GET | `/users/:id` | Get user profile | Yes |
| POST | `/users/:id` | Update user profile | Yes |
| DELETE | `/users/:id` | Delete user | Yes |
| POST | `/profile` | Upload profile image (Multer) | Yes |
| POST | `/images` | Upload note images (Multer) | Yes |
| POST | `/deviceConfig` | Record device info (stub) | Yes |

### 1.2 Backend building blocks that already exist (from directory layout)

The server is organized into clear layers: `routes/` (one router), `middleware/` (per-resource handlers for notes, users, auth, device), `db/` (Mongo connection + per-collection data functions), `models/` (Note, User, device, BaseResponse), `auth/` (JWT sign/verify), `config/` (server config + base URL), `helpers/` (an async try/catch wrapper), `errors/` (custom error, error handler, not-found), `fileupload/` (Multer upload), and `utils/`. A `docker-compose.yml` exists that runs MongoDB (Atlas-Search image) on port 27017 with a persistent volume.

### 1.3 Workflow diagram — how a request flows today

```
  ┌──────────────┐        HTTPS (today HTTP:3000)         ┌───────────────────────────┐
  │  Android /   │ ─────────────────────────────────────▶ │        Express 4 app        │
  │  iOS client  │   Authorization: <JWT> (after login)   │        (server.js)          │
  │ (KMM shared) │ ◀───────────────────────────────────── │  bodyParser → router        │
  └──────────────┘        BaseResponse{isSuccessful,data}  └─────────────┬─────────────┘
        ▲                                                                │
        │ offline-first: server calls                     ┌──────────────▼──────────────┐
        │ currently commented out in                      │  middleware handlers         │
        │ NoteRepository                                   │  notes / users / auth / dev  │
                                                           └──────┬───────────────┬──────┘
                                                                  │               │
                                                    ┌─────────────▼──┐     ┌──────▼───────┐
                                                    │  JWT auth       │     │  Multer      │
                                                    │  (sign/verify)  │     │  file upload │
                                                    └─────────────────┘     └──────┬───────┘
                                                                  │               │
                                                    ┌─────────────▼──┐     ┌──────▼───────┐
                                                    │  db/ functions  │     │ local uploads/│
                                                    │ (native driver) │     │   folder      │
                                                    └─────────────────┘     └──────────────┘
                                                                  │
                                                          ┌───────▼────────┐
                                                          │   MongoDB      │
                                                          │ (docker/local) │
                                                          └────────────────┘
```

---

## 2. What exists vs what must be built

Legend: ✅ exists & usable · 🟡 exists but broken/unverified/incomplete · ❌ not started

### 2.1 Core plumbing

| Capability | State | Notes |
|---|---|---|
| Express app + router mounting | ✅ | `server.js`, single router; but error-vs-404 handler order is inverted (M6 in modernization plan) |
| MongoDB connection (native driver) | ✅ | `db/dbconfig.js`; docker-compose provides Mongo |
| `BaseResponse` envelope | ✅ | Client depends on `{ isSuccessful, data }` shape |
| Async error wrapper | 🟡 | `helpers/ApiWrapper.js` exists; upload handlers bypass it |
| Central error + 404 handling | 🟡 | Exists; ordering bug to fix |
| Config / env loading | 🟡 | No validation; missing var → silent `undefined` |

### 2.2 Auth & users

| Capability | State | Notes |
|---|---|---|
| Register endpoint | 🟡 | Exists; **stores password in plaintext** |
| Login endpoint | 🟡 | Exists; **compares plaintext password in the query**; buggy `email == null && password == null` null-check (should be OR) |
| JWT issue/verify | 🟡 | Exists; **no `expiresIn`, no algorithm allow-list**; tokens never expire |
| Auth as route middleware | ❌ | Auth is called inside handlers, return value ignored → does **not** actually block unauthenticated requests; also missing import → `ReferenceError` on `/users/:id` |
| Password hashing | ❌ | No `bcrypt` yet |
| Rate limiting / helmet / CORS | ❌ | None mounted |
| User CRUD correctness | 🟡 | `updateUser` reads `res.body` instead of `req.body` → always sends `undefined` |

### 2.3 Notes

| Capability | State | Notes |
|---|---|---|
| Note list/create/get/update/delete routes | 🟡 | Declared and mounted; unverified against clients |
| Server note model | 🟡 | Field drift: server uses `content`, client uses `contents`; no `categoryId`; media/location fields dropped |
| `LocationContent` class | 🟡→❌ | Broken: `super(this._id,…)` reads `this` before `super()` runs → throws at runtime |
| Unified content model (TEXT/MEDIA/LINK/LOCATION with `type` discriminator) | ❌ | Designed in `note-model-redesign.md`; **not implemented on server** |
| Collection JSON-schema validation | ❌ | No validation; any shape lands in Mongo |
| Soft delete / `isDeleted` on server | ❌ | Needed for sync |
| Server-authoritative timestamps (epoch millis) | ❌ | Currently string/absent |

### 2.4 Media / files

| Capability | State | Notes |
|---|---|---|
| Image upload (profile + note images) | 🟡 | Multer works, but weak/unanchored extension regex, no size limit, filenames echoed back, path built from `Date.now()+originalname` |
| Media served back to clients | 🟡 | Stored in local `uploads/`; no CDN/object store; URL strategy undefined |
| Checksum / integrity for sync | ❌ | `MediaAsset.checksum` designed client-side; server ignores it |
| Media associated to note content rows | ❌ | Media sync flow undefined |

### 2.5 Sync (the whole point of the backend)

| Capability | State | Notes |
|---|---|---|
| `syncStatus` lifecycle on the wire | 🟡 | Enum exists in client model (`LOCAL_ONLY/SYNCED/PENDING_UPDATE/PENDING_DELETE`); server does nothing with it |
| Push (client → server) | ❌ | Client `*Api` calls are commented out |
| Pull (server → client, delta by `updatedAt`) | ❌ | No delta endpoint |
| Conflict handling | ❌ | No strategy implemented (plan: last-write-wins per note, later per content) |
| Block-level (per-content) sync | ❌ | Future; note-granular first |

### 2.6 Sharing, versions, AI (future phases)

| Capability | State | Notes |
|---|---|---|
| Version share links (immutable snapshot) | ❌ | Roadmap P3 |
| AI chat / explanation proxy (`/ai/chat`) | ❌ | Roadmap P4; keeps API keys off devices |
| Notebook grouping entity | ❌ | Product decision pending (roadmap G6) |

### 2.7 Environments, tooling, tests

| Capability | State | Notes |
|---|---|---|
| Dev environment | 🟡 | Runs via `node server.js` / nodemon; docker Mongo available |
| Staging environment | ❌ | Not defined |
| Production environment | ❌ | Not defined |
| Linting / formatting | ❌ | No ESLint/Prettier |
| Automated tests | ❌ | `npm test` is `exit 1` |
| CI | ❌ | None |
| Structured logging | 🟡→❌ | Only `console.log` (some log tokens/PII) |

**One-line summary of §2:** the *shape* of a Notes backend is present, but it is insecure, partly broken at runtime, model-drifted from the clients, and has **no sync, no tests, no staging/prod, and no validation.** The build work is to make it secure and correct (foundation), align the note model, add real sync, and stand up environments + tests.

---

## 3. Target architecture (what "done" looks like)

The North Star is a backend that mirrors Apple/Samsung Notes behavior for a small team/product: **the device is the source of truth; the server is a durable, conflict-aware mirror plus a share/AI hub.**

Design commitments (locked from the source docs):

- **Language stays plain JavaScript**, native Mongo driver, Express 4 for this round (TS/Express 5 deferred).
- **One wire contract for notes.** The kotlinx-serialization JSON of the shared model IS the API contract; Mongo stores the same shape (embedded `contents` array with a `type` discriminator: `TEXT | MEDIA | LINK | LOCATION`). No per-layer translation.
- **`BaseResponse { isSuccessful, data }` is frozen**, and the JWT is returned via the `Authorization` header on `/login`. Field renames require coordinated client changes and are out of scope.
- **Sync-ready fields everywhere:** `syncStatus`, `isDeleted`, and comparable `Long` epoch-millis timestamps on notes (and later contents).
- **Offline-first is sacred:** the server is additive; it never becomes required for the app to function.

### 3.1 Target request flow (with sync)

```
 DEVICE (offline-first, source of truth)                     SERVER (durable mirror + hub)
 ┌──────────────────────────┐                               ┌───────────────────────────────┐
 │ local SQLite (document    │   1. login → JWT              │  auth: bcrypt + JWT(exp+alg)   │
 │ storage: contents JSON)   │ ────────────────────────────▶ │  helmet + cors + rate-limit    │
 │ every note has syncStatus │                               ├───────────────────────────────┤
 │  LOCAL_ONLY / PENDING_*   │   2. PUSH changed notes       │  validate($jsonSchema + Zod)   │
 │                           │      (syncStatus != SYNCED)   │  strip client-only fields      │
 │                           │ ────────────────────────────▶ │  set server updatedAt authority │
 │                           │                               ├───────────────────────────────┤
 │                           │   3. PULL delta               │  notes collection (Mongo)      │
 │                           │      since lastSyncedAt       │  contents[] embedded w/ type    │
 │                           │ ◀──────────────────────────── │  soft delete (isDeleted)       │
 │  reconcile by updatedAt;  │                               ├───────────────────────────────┤
 │  purge PENDING_DELETE      │   media: upload → url          │  object storage / uploads/     │
 │  after server ack          │ ◀───────────────────────────▶ │  (checksum-verified)           │
 └──────────────────────────┘                               └───────────────────────────────┘
```

---

## 4. Feature planning (backend deliverables, grouped)

Ordered by dependency, not by glamour. Each feature notes the source doc that specifies it.

**F1 — Security foundation** *(BACKEND_MODERNIZATION_PLAN §1, Day 1)*
bcrypt password hashing; JWT `expiresIn` + `HS256` allow-list; strip all token/PII logging; `helmet`, `cors` allow-list, `express-rate-limit` on `/login` + `/register`; fix handler ordering.

**F2 — Runtime-bug fixes** *(BACKEND_MODERNIZATION_PLAN §2)*
`authenticate` middleware that actually short-circuits (fixes the ignored-return + missing-import `ReferenceError`); `res.body`→`req.body`; null-guard the auth header; repair or cleanly stub the broken `LocationContent` and device handler.

**F3 — Input validation & config safety** *(BACKEND_MODERNIZATION_PLAN §3, §S5)*
Zod schemas + a `validate(schema)` middleware for login/register/note/user/params; fix login `&&`→`||`; validate env at boot and fail fast; add `.env.example`.

**F4 — Note model alignment** *(note-model-redesign §2.6)*
Adopt client field name `contents`; pass contents through as-is discriminated by `type`; add Mongo `$jsonSchema` validator; numeric timestamps; server sets authoritative `updatedAt`; strip client-only fields (`localPath`, `thumbnailPath`, client `syncStatus`).

**F5 — Media hardening** *(BACKEND_MODERNIZATION_PLAN §S6)*
Anchored extension regex, UUID filenames, size limits; route uploads through the async wrapper; define the media URL strategy (see §7.3); accept/verify `checksum`.

**F6 — Sync endpoints** *(app-vision-feature-roadmap §2, scaling doc §Phase 3)*
Push (upsert changed notes), pull-delta (notes changed since a timestamp), soft-delete propagation, note-granular last-write-wins conflict resolution keyed on `updatedAt`.

**F7 — Environments, tooling, tests, CI** *(BACKEND_MODERNIZATION_PLAN §6, Day 4)*
ESLint flat + Prettier; Vitest + Supertest happy-path + auth + validation tests; Dev/Staging/Prod config; GitHub Actions `lint + test`.

**F8 (future, out of the 10-day scope) — Sharing, versions, AI proxy** *(app-vision-feature-roadmap §4.5, §4.2)*
Version share links; `/ai/chat` and explanation proxy; block-level sync. Sequenced after this round.

---

## 5. API planning

### 5.1 Keep unchanged (contract-frozen)

`/`, `/login`, `/register`, `/notes/:userId` (GET/POST), `/notes/:userId/:id` (GET/POST/DELETE), `/users/:id` (GET/POST/DELETE), `/profile`, `/images`. Response envelope and header behavior stay identical.

### 5.2 Harden in place (no shape change)

- All `/notes/*` and `/users/*` routes gain the real `authenticate` middleware in the chain.
- `/notes/:userId/:id` POST (update) accepts and stores the aligned `contents` array (§4).
- `/register`, `/login`, note create/update, user update gain Zod body/param validation returning `400` with field errors.
- `/profile`, `/images` gain size limits, UUID filenames, anchored MIME check.

### 5.3 New endpoints for sync (additive, versioned to avoid breaking pre-release clients)

| Method | Path | Purpose |
|---|---|---|
| POST | `/sync/:userId/push` | Upsert a batch of notes whose `syncStatus != SYNCED`; server sets authoritative `updatedAt`, returns acks + server versions |
| GET | `/sync/:userId/pull?since=<epochMillis>` | Return notes (incl. `isDeleted`) changed since `since`, for delta reconciliation |
| GET | `/notes/:userId/:id/meta` | Lightweight note header (id, title, updatedAt, block count) — supports list screens & scaling (see §12) |

> **Versioning note (from note-model-redesign §4):** old app versions can't read the new server note shape. Pre-release this is acceptable; otherwise expose the aligned model under `/v2/notes` and keep `/notes` for legacy until clients migrate.

### 5.4 Future endpoints (Phase F8)

`POST /notes/:userId/:id/versions` (snapshot), `GET /share/:versionId` (immutable share link), `POST /ai/chat` (proxy). Documented now so the model choices don't box them out.

---

## 6. Database changes

The clients are moving to **document storage** (contents as one JSON array on the note), and the server adopts the same shape so there is **one wire contract** end-to-end.

### 6.1 `notes` collection — target document

```json
{
  "_id": "n_9f2c",
  "userId": "u_123",
  "title": "Trip plan",
  "categoryId": "t_1",
  "createdAt": 1752105600000,
  "updatedAt": 1752192000000,
  "isDeleted": false,
  "contents": [
    { "type": "TEXT", "_id": "c_1", "noteId": "n_9f2c", "position": 1.0,
      "createdAt": 1752105600000, "updatedAt": 1752105600000, "text": "Pack list…" },
    { "type": "MEDIA", "_id": "c_2", "noteId": "n_9f2c", "position": 2.0,
      "createdAt": 1752105600000, "updatedAt": 1752105600000,
      "mediaType": "VIDEO", "title": "Beach",
      "asset": { "url": "https://…/v.mp4", "mimeType": "video/mp4",
                 "durationMs": 12000, "width": 1920, "height": 1080, "checksum": "…" } },
    { "type": "LOCATION", "_id": "c_3", "noteId": "n_9f2c", "position": 3.0,
      "createdAt": 1752105600000, "updatedAt": 1752105600000,
      "latitude": 15.29, "longitude": 73.91, "address": "Goa" }
  ]
}
```

### 6.2 Changes required (all from `note-model-redesign.md`)

1. **Rename `content` → `contents`** to match the client (kills field drift B4).
2. **Replace the broken `Content`/`LocationContent` classes** with a plain pass-through + a Mongo `$jsonSchema` validator (fixes B1/B2/B3).
3. **`$jsonSchema` collection validator** requiring `_id, userId, title, createdAt, updatedAt, contents`; each content entry requires `_id, noteId, type, position, createdAt, updatedAt` and `type ∈ {TEXT, MEDIA, LINK, LOCATION}`; timestamps `bsonType: long`.
4. **Add `categoryId`, `isDeleted`** on the server note.
5. **Numeric epoch-millis timestamps**, with the **server as the authority** on write.
6. **Strip client-only fields** (`localPath`, `thumbnailPath`, client `syncStatus`) in the route before insert; server keeps `url`.

### 6.3 Indexes

- `notes`: `{ userId: 1, updatedAt: -1 }` — the dominant access pattern (a user's notes newest-first, and the delta-pull filter).
- `notes`: `{ userId: 1, isDeleted: 1 }` — list excludes soft-deleted quickly.
- `users`: unique index on `email` — correctness + login lookup.

### 6.4 One-time server data migration

If any legacy notes exist server-side, a one-off script converts `content`→`contents`, string→long timestamps, and old `Content`/`LocationContent` entries into typed `{type,…}` entries. Per the project's no-delete rule, the legacy collection is **renamed to a `_v1_backup`**, not dropped, until verified.

---

## 7. Sync strategy for offline-first

The clients already carry the sync primitives; the server must honor them. This is the design the roadmap and scaling docs prescribe.

### 7.1 The state machine (client-owned, server-honored)

`LOCAL_ONLY → PENDING_UPDATE ↘`
`               PENDING_DELETE → (push) → SYNCED`

- A new/edited note is `LOCAL_ONLY` or `PENDING_UPDATE`; a removed note is `PENDING_DELETE` (soft delete, not gone).
- **Push:** the client sends everything not `SYNCED`. The server upserts, stamps its authoritative `updatedAt`, and returns the server version. On ack the client marks `SYNCED` and **purges** `PENDING_DELETE` rows.
- **Pull:** the client asks for everything changed since its `lastSyncedAt`; the server returns changed notes including soft-deleted ones so deletions propagate.

### 7.2 Conflict resolution

- **This round: note-granular last-write-wins by `updatedAt` (Long).** Simple, correct enough for a single-user-multi-device product, and safe because timestamps are now comparable.
- **Later (scaling doc §Phase 3): block-level (per-content) last-write-wins** — send only created/updated/deleted content rows keyed on content `_id` + `updatedAt`. The stable content ids and per-content timestamps already in the model make this a drop-in evolution, not a rewrite.
- **Real-time collaboration / CRDT is explicitly deferred** (roadmap §3) — snapshots first.

### 7.3 Media in sync

Media rows carry **pointers, not bytes**. The device holds `localPath`; on upload the server returns a `url`; `checksum` guards integrity and enables dedupe. For this round media lands in the server `uploads/` folder behind the API; the **recommended production path is an object store (S3/GCS-compatible) or MongoDB GridFS**, decided in §14. Media upload and note sync are separate calls — a note references media by `url` once uploaded.

### 7.4 Sync sequence diagram

```
 Client                          Server                         Mongo
   │  POST /sync/:u/push          │                              │
   │  [notes where !=SYNCED] ────▶│  validate + strip client     │
   │                              │  fields; set updatedAt ──────▶│ upsert
   │                              │◀───────────────────────────── │ ok
   │◀── acks + server versions ── │                              │
   │  mark SYNCED; purge deletes  │                              │
   │                              │                              │
   │  GET /sync/:u/pull?since=T ─▶│  find updatedAt > T ─────────▶│
   │                              │◀───────────────────────────── │ changed[]
   │◀── changed notes (incl.      │                              │
   │     isDeleted) ───────────── │                              │
   │  reconcile by updatedAt      │                              │
```

---

## 8. Implementation roadmap — milestones, priorities, dependencies

The build is organized into **five milestones** across **10 working days**. Each milestone ends with the server **booting, running, and (from M4 on) tested**. Priorities: security and correctness before features; model alignment before sync; sync before environments/tests can meaningfully cover it.

### Dependency graph

```
 M1 Security+bug fixes ──▶ M2 Validation+config ──▶ M3 Model alignment+DB ──▶ M4 Sync endpoints ──▶ M5 Envs+tests+CI
      (must be first:            (needs auth &            (needs a safe,           (needs the aligned      (needs everything
       everything else            error handling            validated base)          model + validation)      to test against)
       runs on top of it)          in place)
```

Priority key: **P0 = blocker for launch**, **P1 = required for a real Notes backend**, **P2 = strongly recommended**, **P3 = future**.

### Milestone M1 — Security & critical runtime bugs (Days 1–2) · P0

**Depends on:** nothing. **Blocks:** everything.
Adds `bcrypt`, `helmet`, `cors`, `express-rate-limit`, `pino`/`pino-http`. Hash passwords on register; login fetches by email then `bcrypt.compare`; strip `password` from every response. JWT `expiresIn` + `HS256` allow-list. Remove token/PII logging. Build the real `authenticate` middleware and attach it in the router (fixes the ignored-return, the missing import `ReferenceError`, and the `res.body`→`req.body` bug). Mount helmet/cors/rate-limit; fix error-vs-404 handler order.
**Done when:** register→login round-trips with a hashed password; `/users/:id` returns instead of throwing; no secrets in logs; unauthenticated protected routes are actually rejected.

### Milestone M2 — Validation & config safety (Day 3) · P0/P1

**Depends on:** M1. **Blocks:** M3/M4 (a safe base to validate against).
Add Zod + a `validate(schema)` middleware; schemas for login, register, note create/update, user update, and route params; fix the `&&`→`||` login check. Validate env at boot, fail fast, add `.env.example`. Harden `upload.js` (anchored regex, UUID names, size limit) and route uploads through the async wrapper.
**Done when:** malformed bodies return `400` with field errors; a missing env var stops boot with a clear message; uploads reject oversized/wrong-type files.

### Milestone M3 — Note model alignment & DB (Days 4–5) · P1

**Depends on:** M2. **Blocks:** M4.
Adopt the unified `contents` model on the server (§4/§6): rename `content`→`contents`, replace broken classes with pass-through + `$jsonSchema` validator, numeric timestamps with server authority, `categoryId` + `isDeleted`, strip client-only fields. Add indexes (§6.3). Write the one-time legacy migration script (rename-to-backup, no drop).
**Done when:** a client-shaped note round-trips Mongo↔API byte-for-byte on required fields; malformed content is rejected by the validator; the old `LocationContent` crash path is gone.

### Milestone M4 — Sync endpoints (Days 6–7) · P1

**Depends on:** M3. **Blocks:** meaningful sync tests.
Implement `/sync/:userId/push` (batch upsert, server `updatedAt` authority, acks), `/sync/:userId/pull?since=` (delta incl. soft-deleted), and `/notes/:userId/:id/meta`. Note-granular last-write-wins by `updatedAt`. Version behind `/v2` if any legacy clients must coexist.
**Done when:** a simulated two-device flow converges — push from A, pull on B yields A's change; a delete on A propagates as `isDeleted` to B; conflicting edits resolve by newest `updatedAt`.

### Milestone M5 — Environments, tests, tooling, CI (Days 8–10) · P0 (tests) / P1 (envs)

**Depends on:** M1–M4. **Blocks:** launch confidence.
ESLint flat + Prettier; Vitest + Supertest covering register→login→token, auth rejection, note CRUD, validation `400`s, and the sync push/pull/conflict paths. Stand up Dev/Staging/Prod config (§11). GitHub Actions `lint + test`. Full regression against the client contract (envelope + header unchanged). Short changelog in `bug-fixes-docs/`.
**Done when:** `npm run lint` clean; `npm test` green in CI; a staging deploy passes the same suite; clients still authenticate and CRUD/sync notes.

### Milestone timeline (Gantt-style)

```
 Day        1    2    3    4    5    6    7    8    9   10
 M1 Sec/bug [==========]
 M2 Valid              [====]
 M3 Model                   [=========]
 M4 Sync                              [=========]
 M5 Env/Test                                    [==============]
                                                 ^ lint/CI    ^ regression + sign-off
```

---

## 9. Risk analysis

| # | Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|---|
| R1 | Auth refactor breaks the login flow the clients depend on | Med | High | Contract-freeze the envelope + `Authorization` header; cover with Supertest in M5; test against a real client build before sign-off |
| R2 | Note model alignment diverges from the client's kotlinx shape | Med | High | Use the exact wire example in §6.1 as the single source; round-trip test (verification matrix, note-redesign §6) |
| R3 | Sync conflict logic loses data (whole-note overwrite) | Med | High | Start note-granular LWW **only** with comparable `updatedAt`; soft-delete (never hard-delete) so nothing is unrecoverable; plan block-level next |
| R4 | Media URL/storage decision (uploads/ vs object store) slips and blocks media sync | Med | Med | Decide in §14 before M4; ship folder-based for the round, keep the `url` indirection so the store can change later |
| R5 | 10-day window is tight if any milestone overruns | High | Med | M1–M2 are P0 and self-contained; if time slips, **descope M4 to push-only + basic pull** and defer meta/versioning — the app still works offline |
| R6 | No existing tests → regressions invisible during refactor | High | High | Front-load a thin smoke suite right after M1 so later milestones have a safety net (not only at M5) |
| R7 | Env misconfig leaks secrets or points prod at dev Mongo | Low | High | Boot-time env validation (M2); separate `.env` per environment; never commit `.env` (already gitignored) |
| R8 | Pre-release clients can't read the new note shape | Med | Med | `/v2` versioning path (§5.3); or accept the break pre-release with a coordinated client update |
| R9 | Scaling limits (large notes) surface during sync of big documents | Low (now) | Med | Out of scope for 10 days; scaling doc §Phase 3 is the follow-up; keep note-granular sync so block-level is a clean upgrade |

---

## 10. Timeline feasibility — can this be done in 10 days?

**Verdict: yes for a secure, model-aligned, note-granular-sync backend with tests and environments — provided scope holds to F1–F7 and F8 (sharing/versions/AI) stays out.**

The reasoning:

- The security + bug-fix + validation + tooling work (F1–F3, F7) is **already scoped to 4 days** in `BACKEND_MODERNIZATION_PLAN.md` and is well understood. That maps to M1, M2, and most of M5.
- The genuinely new build is **model alignment (M3)** and **sync (M4)** — roughly 4 days. Both are specified in detail in `note-model-redesign.md` and the roadmap, which de-risks them substantially (design already done).
- That leaves ~2 days of buffer inside the 10, consumed by tests, environment setup, and a client regression pass.

**Where it gets tight:** if M1 uncovers deeper auth/client-contract issues, or if the media storage decision (R4) isn't made up front, M4 can slip. The realistic **descope lever** is M4: ship **push + basic pull** first, defer per-content sync, `/meta`, and versioning. Because the app is offline-first, a partial backend is still a shippable app — sync just does less. **Do not descope M1/M2** (security/validation) — those are the launch blockers.

A conservative reading: **10 days is feasible for M1–M3 + a functional-but-minimal M4 + M5 tests.** A polished, fully-tested per-content sync would want ~13–14 days.

---

## 11. Development / Staging / Production environments

Three environments, identical code, config-only differences, selected by `NODE_ENV`. Env is validated at boot (M2) so a misconfigured environment fails fast rather than silently.

| Concern | Development | Staging | Production |
|---|---|---|---|
| Purpose | Local coding & fast iteration | Client-integration + pre-release testing | Live users |
| Mongo | Local docker (`docker-compose.yml`, Atlas-Search image) | Managed cluster (small tier) or dedicated docker host | Managed cluster with backups + auth |
| Run mode | `node --watch server.js` (hot reload) | `node server.js` behind a process manager | Process manager + reverse proxy (TLS) |
| JWT key | Dev-only key in local `.env` | Distinct staging secret | Distinct prod secret, rotated |
| CORS allow-list | `localhost` + emulator origins | Staging client origins | Production client origins only |
| Rate limits | Loose | Production-like | Strict on `/login` `/register` `/sync` |
| Media storage | Local `uploads/` | Object store (staging bucket) | Object store (prod bucket) + CDN |
| Logging | `pino` pretty, `LOG_LEVEL=debug` | `info`, structured | `info`/`warn`, structured, shipped to a log sink |
| Deploy trigger | Manual | Auto on merge to `main` (CI green) | Tagged release, manual approval |

**Promotion flow:** feature branch → CI `lint + test` → merge → auto-deploy **staging** → client regression pass → tagged release → manual-approve **production**. TLS terminates at a reverse proxy in staging/prod; the app keeps listening on 3000 internally.

Config hygiene: one `.env` per environment (never committed — already gitignored), plus a committed `.env.example` documenting every required var (`JWT_KEY`, Mongo URI, `NODE_ENV`, `LOG_LEVEL`, CORS origins, rate-limit knobs, media store creds).

---

## 12. Testing strategy

A pyramid, thin at the top, honoring the project's "just tell me how to test" preference — every layer has a one-command entry point.

**Layer 1 — Unit (fast, many).** Pure functions: JWT sign/verify (expiry + algorithm), password hash/compare, Zod schema accept/reject, the client-field-strip and timestamp-authority logic, sync conflict resolver (given two `updatedAt`s, pick the winner). Tool: **Vitest**.

**Layer 2 — API/integration (Supertest).** Each route group end-to-end against a test Mongo (docker or in-memory):
- Auth: `register → login → token`; protected route **rejects** without/with a bad/expired token.
- Notes: create → get → update → delete happy path; validation `400`s on malformed bodies/params.
- Sync: push a `PENDING_UPDATE` note → pull on a second "device" returns it; delete propagates as `isDeleted`; conflicting edits resolve by newest `updatedAt`.
- Media: oversized/wrong-MIME upload rejected; valid upload returns a `url`.

**Layer 3 — Contract/regression.** Assert the frozen wire contract: `BaseResponse { isSuccessful, data }` shape and the `Authorization` header on `/login` are unchanged. Round-trip a §6.1-shaped note Mongo↔API and diff required fields (the `note-model-redesign.md §6` verification matrix). Ideally run a real Android/iOS build against staging before sign-off.

**Layer 4 — Smoke (manual + scripted).** A curl/Postman collection for the core flows, runnable against any environment as a post-deploy check.

**How to run (target scripts to add in M5):**
`npm run lint` · `npm test` (Vitest+Supertest) · `npm run test:watch` · CI runs `npm ci && npm run lint && npm test`.

**Coverage priority:** auth and sync paths first (highest risk, R1/R3), validation next, then media. Front-load a smoke suite right after M1 (per R6) rather than waiting for M5.

---

## 13. Optimization opportunities

Backend-side, drawn from the scaling analysis and general service hygiene:

- **Indexes before features.** `{userId, updatedAt}` and `{userId, isDeleted}` on `notes`, unique `email` on `users` — the delta-pull and list queries live or die on these (§6.3).
- **Lightweight list/meta endpoint.** `/notes/:userId/:id/meta` and a summary list query return header + block count + first snippet, never the full `contents` array — mirrors the client's "don't join full contents for a list" lesson (scaling doc §Phase 0).
- **Delta sync, not full sync.** Pull only `updatedAt > since`; never ship the whole collection. This is the single biggest bandwidth/CPU saver.
- **Server-set timestamps + projections.** Strip client-only fields and return only needed fields to keep payloads small.
- **Rate-limit + payload caps.** Protects `/login`, `/register`, `/sync` and bounds request body size (large-note guardrail).
- **Connection reuse.** One shared Mongo client/pool at boot (already the native-driver idiom) — never connect per request.
- **Gzip/compression** at the reverse proxy for JSON responses.
- **Async wrapper uniformly applied** (incl. uploads) so no unhandled rejection stalls the event loop.

Explicitly **not** optimizing yet (premature for a small product): sharding, read replicas, caching layers. Revisit under §14.

---

## 14. Future scalability recommendations

Sequenced so nothing here blocks the 10-day round, but each is unlocked cleanly by the choices above.

1. **Block-level (per-content) sync** *(scaling doc §Phase 3).* Send only created/updated/deleted content rows keyed on content `_id` + `updatedAt`; per-block last-write-wins. The stable ids + per-content timestamps already in the model make this additive. Biggest correctness win for large, multi-device notes.
2. **Server-side pagination for large notes.** Same windowing idea as the client — return `contents` in position-keyed windows for 1000–2000-page notes, so a single note never ships as one giant payload.
3. **Media to object storage + CDN.** Move `uploads/` to an S3/GCS-compatible store (or GridFS) with `checksum` dedupe and CDN delivery; the `url` indirection means clients don't change.
4. **Sharing & versions** *(roadmap §4.2).* Immutable snapshot documents + `/share/:versionId` links; a `versions` collection referencing note snapshots.
5. **AI proxy** *(roadmap §4.5).* `/ai/chat` and explanation endpoints that keep provider keys server-side and enforce usage limits; note context built from the same MD serialization the export pipeline uses.
6. **Notebook grouping** *(roadmap G6).* A real `Notebook` entity (id, title, noteIds, cover) once "share a version of a notebook" lands — cleaner than overloading tags.
7. **Horizontal scale when needed.** Stateless app (JWT, no server session) means it scales out behind a load balancer trivially; Mongo scales via managed cluster → replica set → sharding on `userId` if a single tenant ever gets huge. Add caching (Redis) only when read profiles justify it.
8. **TypeScript + Express 5 migration** *(deferred from this round).* Once the surface is stable and tested, migrate for long-term maintainability — the tests written in M5 become the safety net that makes it low-risk.

---

## 15. Contract-safety note (carried from the modernization plan)

The KMM client expects `BaseResponse { isSuccessful, data }`, the JWT via the `Authorization` response header on `/login`, and the existing route shapes. **None of these wire formats change** in the hardening milestones (M1–M2). The note-model alignment (M3) changes the **note body shape** (`content`→`contents`, typed entries, numeric timestamps); that is a coordinated client change already designed in `note-model-redesign.md`, and is versioned behind `/v2` if legacy clients must coexist. Any other field rename is out of scope and would require a coordinated client update.

---

## Appendix A — Source-doc traceability

| This plan's section | Grounded in |
|---|---|
| §1 current workflow, §1.1 API surface | `routes/routes.js`, `server.js`, `package.json`, docker-compose |
| §2 exists-vs-build (security/bugs) | `BACKEND_MODERNIZATION_PLAN.md` §1–§6 |
| §2 exists-vs-build (model) | `note-model-redesign.md` §1.4 |
| §2 exists-vs-build (features/sync) | `app-vision-feature-roadmap.md` §1.4, §2, §6 |
| §4 feature planning | modernization plan + roadmap phases |
| §6 database changes | `note-model-redesign.md` §2.5–§2.6, §3 |
| §7 sync strategy | `app-vision-feature-roadmap.md` §2, scaling doc §Phase 3 |
| §12–§13 testing & optimization | modernization plan §T2/§6; scaling doc §Phase 0–1 |
| §14 scalability | scaling doc §Phase 3, roadmap §4 |

*No application source-code logic was read to produce this document — only route declarations (as the API specification), config, and the planning/architecture docs listed above.*
