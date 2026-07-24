# 23-Jul-2026 · Post-merge change audit & regression map (iOS + Android)

**Purpose:** list every change made in the most recent working session so you can see, without going branch-by-branch, what could have caused the current breakage. For each reported failure it says whether the session changes could plausibly be the cause.

> ⚠️ Important scope note: this working copy is **not a git repository**, so I could **not** diff against commit `201c8f47f41ffb82cb17b73538f2c9ddfcf677fe`. This audit is therefore the list of files **I edited in this session**, not a git diff of that merge. Anything not in this list was already in the tree (from that merge or earlier) and was **not** touched here.

---

## 1. What was changed this session — three independent pieces of work

### A. Theme adoption ("Granth" warm palette) — VISUAL, app-wide impact
Rewired the Material/asset colour roles from the old blue theme to the saffron/copper/parchment spec.

| Platform | File | What changed |
|---|---|---|
| Android | `androidApp/.../theme/Color.kt` | Added Granth palette tokens. **Rewired `lightTheme` and `darkTheme`** (Material 3 colour roles now map to saffron/copper/parchment). Added new `amoledTheme`. Old blue tokens kept but unused. |
| Android | `androidApp/.../theme/Type.kt` | Retuned the whole `Typography` scale to serif display / sans body. |
| Android | `androidApp/.../theme/Shape.kt` | New radius scale (sm 8 / md 14 / lg 22 / xl 30). |
| Android | `androidApp/.../PustakmApplicationTheme.kt` | Added `isAmoled` param + AMOLED branch; warm extended-colour variants. |
| iOS | `iosApp/.../theme/Theme.swift` | Added Granth colour tokens, serif font styles, spacing/radius/elevation structs. |
| iOS | `iosApp/.../theme/ThemeManager.swift` | Added `ThemeMode` enum (light/dark/amoled) + `setMode`; kept old `isDarkMode`/`toggleTheme`/`getTheme`. |
| iOS | `iosApp/Assets.xcassets/colors/*` | Rewired 8 existing colorsets to warm values; added 17 new colorsets. |

**Risk it introduced:** because `MaterialTheme.colorScheme.primary/secondary/...` now mean saffron/copper, **any screen that filled a whole surface with those roles turned into a slab of accent colour** ("wall of orange"). This is a **cross-screen visual regression**, not a crash. Only the note-list card + backgrounds were re-pointed to `surface`/`background` (item C). **Other screens that used `primary`/`secondary` as a fill were NOT swept** and may still look wrong.

### B. iOS "media lost after app update" fix — BEHAVIOUR, hot path
New path resolver; media paths now resolved at read time.

| Platform | File | What changed |
|---|---|---|
| shared | `shared/.../util/LocalFilePathResolver.kt` (**new**) | `expect fun resolveLocalFilePath(path)` |
| shared | `shared/.../util/LocalFilePathResolver.android.kt` (**new**) | Android actual = pass-through |
| shared | `shared/.../util/LocalFilePathResolver.ios.kt` (**new**) | iOS actual = re-anchor an absolute path onto the current app container |
| shared | `shared/.../notes/Note.kt` | **`getMediaUrl()` now runs `localPath` through `resolveLocalFilePath`** |
| shared | `shared/.../notes/Notes.kt` | `toSummary()` resolves `thumbnailPath` |
| shared | `shared/.../database/NotesDao.kt` | summary mapper resolves `thumbnailPath` |
| iOS | `iosApp/.../NoteEditorViewModel.swift` | delete-block resolves path before delete + deletes thumbnail file |
| Android | `androidApp/.../noteEditor/NoteEditorViewModel.kt` | delete-block also deletes the thumbnail file |

**Risk it introduced:** `getMediaUrl()` is on the hot path for **all** playback and image loading on **both** platforms. If the resolver returns an unexpected value, media/images could fail to load. This is the **most likely behavioural regression** if media stopped playing.

### C. Notes-list card / background fixes (follow-up to the orange)

| Platform | File | What changed |
|---|---|---|
| iOS | `iosApp/.../NoteBookView.swift` | Card fill `primary`→`surface`; serif title; copper fold corner; border; shadow; radius 14. |
| iOS | `iosApp/.../NotesView.swift` | Parchment page background; FAB uses accent gradient. |
| iOS | `iosApp/.../NoteEditorView.swift` | Added parchment editor background. |
| Android | `androidApp/.../notes/single/NoteView.kt` | Card `primaryContainer`→`surface`; border; serif title; text colours to `onSurface`/`onSurfaceVariant`. |
| Android | `androidApp/.../notes/list/NotesView.kt` | Grid background blue-gradient → parchment `colorScheme.background`. |

**Risk it introduced:** cosmetic only. Adding an opaque `background` behind a `ZStack`/grid can, in rare SwiftUI cases, cover a sibling — worth a glance, but not a crash source.

---

## 2. Reported breakages → is a session change the cause?

| Reported failure | Platform | In a file I changed this session? | Verdict |
|---|---|---|---|
| **PDF reader crashes in full-screen** | iOS | ❌ No — `BookReaderView.swift`, `Router.swift`, reader/full-screen code was **not touched** | **Not caused by this session.** Came in with the merge (file-import + `saveThenOpen` + reader-refresh work dated 18-Jul/23-Jul in the tree). Needs its own investigation. |
| **Video is not recording** | Android | ❌ No — camera/video capture code was **not touched** | **Not caused by this session.** The only recorder files here are audio; video-capture code is elsewhere and unchanged by me. Regression is from the merge or a pre-existing issue. |
| **Scrolling / reading mode not applied from Settings** | Android | ❌ No — `ReadingMode.kt`, `SettingsView(Model).kt`, `BookReaderScreen.kt` **not touched** | **Not caused by this session.** Reading-mode + settings wiring is untouched here; belongs to the merge. |
| **Colours wrong / too dark in light mode / "wall of orange"** | iOS + Android | ✅ Yes — items **A** and **C** | **Caused by this session (theme rewire).** Partially fixed (note list). Other screens still need the same `primary`-as-fill → `surface` sweep. |
| **Media/images not loading** (if seen) | iOS + Android | ✅ Yes — item **B** (`getMediaUrl`) | **Possible — check first** if playback/images broke. |

---

## 3. Honest boundary statement

The session changed **only**: theme tokens/roles (A), the media-path resolver (B), and the notes-list cards/backgrounds (C).

It did **NOT** change: PDF/book reader, full-screen presentation, video recording, camera, reading/scroll-mode settings, navigation, MVI flow, use cases, the DB schema, or save/record logic. Those areas were already in the tree from the merge tied to commit `201c8f47…` (visible: file-import, `saveThenOpen`, reader-refresh work) and are the place to look for the reader crash, video recording, and scroll-mode failures.

---

## 4. Suggested next actions (no code changed yet — awaiting your call)

1. **Reader crash / video / scroll-mode** → investigate the merge changes (not this session's). If you have git elsewhere, `git diff 201c8f47… -- iosApp/.../bookReader androidApp/.../bookReader androidApp/.../hardware` will show the real culprit.
2. **Media/images** → verify `resolveLocalFilePath` behaviour on-device; if it regressed, revert item **B** (routing `getMediaUrl` through the resolver) — the resolver files can stay, just stop calling them from `getMediaUrl`.
3. **Colours** → either finish the `primary`-as-fill → `surface` sweep across remaining screens, OR revert items **A/C** to the old blue theme if you want the previous look back immediately.

**To fully undo this session:** revert the files in sections A, B, and C above to their pre-session state. To keep the media fix but drop the new look: revert A + C only.
