# PustakmServer — Backend Modernization: Change List & 4-Day Plan

> **Status:** Plan only. **No code changed yet.**
> **Date:** 2026-06-30
> **Scope:** `PustakmServer` (Node.js + Express 4 + native MongoDB driver, ESM).

## Decisions locked for this round

- **Language:** Stay **plain JavaScript**. TypeScript migration is deferred to a later round.
- **Data layer:** Keep the **native `mongodb` driver** (no Mongoose) — least churn; add validation at the route boundary instead.
- **Express:** Stay on **Express 4** for now (the `tryCatchWrapper` async helper keeps working). Express 5 upgrade rides with the future TS migration.
- **Security:** **In scope.** Several findings below are real runtime errors or plaintext-credential issues, not cosmetics — they lead Day 1.

---

## Part 1 — Detailed change list

Grouped by theme. `[BUG]` = currently broken/throws or a security hole. `[STYLE]` = modernization only.

### 1. Security (🔴)

| # | File | Problem | Change |
| - | ---- | ------- | ------ |
| S1 `[BUG]` | `db/usersdb.js`, `middleware/handleAuth.js` | **Passwords stored & compared in plaintext.** `createUser` saves `req.body.password` as-is; `getUserWithEmail(email, password)` matches the raw password in the Mongo query. | Hash with **`bcrypt`** on register; on login fetch by email only, then `bcrypt.compare`. Never store or return `password`. |
| S2 `[BUG]` | `auth/jwtAuth.js` | `jwt.sign(payload, key)` has **no `expiresIn`**; `verify` has no algorithm allow-list. Tokens never expire. | Add `expiresIn` (e.g. `7d`), sign/verify with `{ algorithms: ['HS256'] }`. |
| S3 `[BUG]` | `auth/jwtAuth.js`, `middleware/handleAuth.js` | **Tokens & user objects logged in plaintext** (`console.log(\`token : ${token}\`)`, `console.log(user)`, `console.log(req.body)` with credentials). | Remove all credential/PII logging; route the rest through a logger (see L1). |
| S4 | `server.js` | No `helmet`, no `cors`, no rate limiting. | Add `helmet()`, `cors()` with an origin allow-list, and `express-rate-limit` on `/login` + `/register`. |
| S5 | `config/serverConfig.js`, `.env` | `JWT_KEY`/DB vars read with no validation — a missing var yields `undefined` and silent failure. | Validate env at boot (small Zod schema or manual assert); fail fast. Add `.env.example`. |
| S6 | `fileupload/upload.js` | Filenames built from `Date.now()+file.originalname` and echoed back; weak extension regex (`/.jpeg|.jpg/` — unanchored, the `.` matches any char). No size limit. | Sanitize/uuid filenames, anchor the regex (`/\.(jpe?g|png|gif|pdf)$/i`), add `limits: { fileSize }`. |

### 2. Correctness bugs surfaced while reading (🔴/🟠)

| # | File | Problem | Change |
| - | ---- | ------- | ------ |
| C1 `[BUG]` | `middleware/usersHandler.js` | Calls `verifyUserAuthenticationToken(req,res,next)` in 3 handlers but **never imports it** → `ReferenceError` on every `/users/:id` request. | Apply auth as real route middleware (see M1); remove the in-body calls. |
| C2 `[BUG]` | `middleware/usersHandler.js` `updateUserApi` | Reads **`res.body`** instead of `req.body` → update always sends `undefined`. | Fix to `req.body`. |
| C3 `[BUG]` | `middleware/deviceHandler.js` | Uses `DeviceInfo(req.body)` but **never imports** `models/device.js`, and the class has no default export and a positional constructor. `data` is unused. | Import/normalize the model, or stub the endpoint cleanly until CRUD is built (comment says "crud is remaining"). |
| C4 `[BUG]` | `middleware/notesHandler.js` `getAllNotes` | References undefined `notesList` → `ReferenceError`. Handler is also unrouted/dead. | Remove the dead handler (with your permission) or fix the variable + route it. |
| C5 `[BUG]` | `auth/jwtAuth.js` `verifyUserAuthenticationToken` | Called inside handlers and its return ignored, so it **doesn't short-circuit** an unauthenticated request; also `req.header(key).split(" ")` throws if the header is absent. | Convert to middleware that calls `next()`/`next(err)` and sits in the route chain; null-guard the header. |
| C6 `[BUG]` | `models/Note.js` `LocationContent` | `super(this._id, …)` reads `this` **before** `super()` runs → broken inheritance. | Pass the constructor arg fields to `super`, not `this.*`. |
| C7 | `models/BaseResponse.js` | `create()` calls `BaseResponse(...)` **without `new`**. | Make it a `static` factory using `new`, or delete (unused). |
| C8 | `db/note_const.js` | `import { text } from "body-parser"` — unused, nonsensical import. | Remove. |
| C9 | `config/serverConfig.js` | `getBaseUrl()` prod/dev branches are **identical**, comments inverted; uses `var`. | Simplify to one clear resolution; `const`/`let`. |

### 3. Input validation (🟠)

| # | Routes | Change |
| - | ------ | ------ |
| V1 | `/login`, `/register` | Validate body with **Zod** (email format, password length). Currently `userLoginWithEmail` only null-checks with a buggy `email == null && password == null` (AND should be OR). |
| V2 | `/notes/:userId`, `/notes/:userId/:id` | Validate params (`userId`, `id`) and note body (`title`, `content`, …) before hitting the DB. |
| V3 | `/users/:id` | Validate update payload; whitelist updatable fields. |
| V4 | shared | Add a `validate(schema)` middleware factory so routes read declaratively. |

### 4. Logging (🟠)

| # | File | Change |
| - | ---- | ------ |
| L1 | all (11 `console.log`, 1 `console.error`) | Replace with **`pino`** (+ `pino-http` for request logs). Levels via `LOG_LEVEL`. Strip the payload/token logs entirely. |

### 5. Structure & idioms (🟡)

| # | File | Change |
| - | ---- | ------ |
| M1 | `routes/routes.js`, `auth/jwtAuth.js` | Introduce an `authenticate` middleware and attach it to protected routes (`router.route('/notes/:userId').all(authenticate)…`), removing per-handler auth calls (fixes C1/C5). |
| M2 | `helpers/ApiWrapper.js` | Keep `tryCatchWrapper` (valid on Express 4) but apply it uniformly; the file-upload handlers currently bypass it. |
| M3 | `models/*.js` | Normalize constructors (object-destructure consistently), fix the `this.x = y,` trailing-comma typos (`User.js`, `Note.js`, `device.js`). |
| M4 | `db/usersdb.js` `createUser` | Uses `.then/.catch` returning the error as a value; mix of styles. Standardize on `async/await` + throw. |
| M5 | `package.json` | Move `nodemon` to `devDependencies`; add `"engines": { "node": ">=20" }`; add `.nvmrc`; switch dev script to `node --watch server.js`. |
| M6 | `errors/notfound.js` ordering | `app.use(handleError)` is registered **before** `notFound` in `server.js` — 404 handler should come before the error handler. Reorder. |

### 6. Tooling (🟠)

| # | Change |
| - | ------ |
| T1 | **ESLint flat config** (`eslint.config.js`, `@eslint/js`) + **Prettier**; `lint`/`format` npm scripts. |
| T2 | **Vitest + Supertest**; replace the `exit 1` test script; happy-path + auth tests for each route group. |
| T3 | `.editorconfig` at repo root. |
| T4 | (Optional) GitHub Actions: `npm ci && npm run lint && npm test`. |

---

## Part 2 — The 4-day plan

Each day is self-contained: it ends compiling, running, and (from Day 4 back) tested. Per the project rule, **nothing is deleted without your sign-off** — items marked "remove" (C4, C7, C8) are flagged for approval at the start of the day they land.

### Day 1 — Security hardening + critical runtime bugs
**Goal:** server is safe to run and no longer throws on `/users` or auth paths.

- Add deps: `bcrypt`, `helmet`, `cors`, `express-rate-limit`, `pino`, `pino-http`. *(S1–S4, L1)*
- S1: hash passwords on register; login fetches by email then `bcrypt.compare`; strip `password` from every response.
- S2: JWT `expiresIn` + algorithm allow-list.
- S3 + L1: rip out token/credential `console.log`s; wire `pino`.
- C1 + C2 + C5 → **M1**: build `authenticate` middleware, attach in `routes.js`, fix `res.body`→`req.body`.
- S4: mount `helmet`/`cors`/rate-limit in `server.js`; M6 handler ordering.

**Done when:** login/register work with a hashed password round-trip; `/users/:id` returns instead of `ReferenceError`; no secrets in logs. Manual curl/Postman pass.

### Day 2 — Validation + config + error handling
**Goal:** bad input is rejected cleanly; config fails fast.

- T-prep: add **Zod**; build `validate(schema)` middleware (V4).
- V1–V3: schemas for login, register, note create/update, user update, route params; fix the `&&`→`||` login check.
- S5 + C9: env-schema validation at boot; collapse `serverConfig.js` to one clear path; `.env.example`.
- S6: harden `upload.js` (anchored regex, uuid filenames, size limit).
- C6/C7/C8/M3: model constructor cleanup (C7/C8 removals confirmed with you first).

**Done when:** malformed login/register/note bodies return 400 with field errors; missing env var stops boot with a clear message.

### Day 3 — Structure, idioms & tooling
**Goal:** consistent style, lint passes, dead code resolved.

- M2: route every handler (incl. uploads) through the async wrapper uniformly.
- M4: convert `usersdb.js` `.then/.catch` to `async/await`.
- C3: finish or cleanly stub `deviceHandler` + `device.js`.
- C4: remove dead `getAllNotes` (with approval).
- T1 + T3: ESLint flat + Prettier + `.editorconfig`; run autofix; resolve warnings.
- M5: `package.json` housekeeping (engines, nvmrc, nodemon→dev, watch script).

**Done when:** `npm run lint` is clean; app starts via `node --watch`; no unused/dead handlers.

### Day 4 — Tests, CI & verification
**Goal:** behavior is locked by tests; changes are provably safe.

- T2: Vitest + Supertest. Cover: register→login→token; auth-required routes reject without/with bad token; note CRUD happy path; validation 400s.
- T4: GitHub Actions workflow (`lint` + `test`).
- Full regression pass against the Android/iOS clients' expected contracts (response shapes unchanged: `BaseResponse { isSuccessful, data }`, `Authorization` header on login).
- Update `bug-fixes-docs/` with a short "what changed" changelog; tick off items in this plan and in `CODING_STYLE_MODERNIZATION.md` (BE-3, BE-4, BE-5, BE-8, BE-9 done; BE-1/BE-2 still deferred to TS round).

**Done when:** `npm test` green in CI; clients still authenticate and CRUD notes against the updated server.

---

## Contract safety note

The KMM client expects: `BaseResponse { isSuccessful, data }`, the JWT returned via the `Authorization` response header on `/login`, and the existing route shapes. **None of these wire-formats change** in this plan — only internals, security, and validation. Any field rename would require a coordinated client change and is explicitly out of scope here.

## Effort summary

| Day | Theme | Risk |
| --- | ----- | ---- |
| 1 | Security + critical bugs | Med (auth flow touched — covered by Day 4 tests) |
| 2 | Validation + config | Low |
| 3 | Structure + tooling | Low |
| 4 | Tests + CI | Low |
