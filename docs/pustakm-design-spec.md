# Granth — Design System & Figma Handoff Spec

**Notes & Knowledge Management · Android (Material 3) + iOS (HIG)**
*"Preserving India's timeless knowledge with modern technology."*

This document is the build guide for recreating the prototype (`granth-prototype.html`) as a native Figma file with variables, components, auto layout, and a clickable prototype. It is organized the way a Figma file should be structured: Tokens → Foundations → Components → Screens → Prototype → Handoff.

---

## 1. Design Principles

1. **Premium, warm, minimal** — generous whitespace, soft paper texture, never busy.
2. **Writing & reading first** — UI recedes; content (serif body) leads.
3. **Heritage as texture, not theme** — manuscript inspiration shows through color, paper grain, fold corners, copper-seal accents, and serif display type — never skeuomorphic "old scroll" decoration.
4. **Native on each platform** — same tokens and content, but platform-correct chrome (FAB shape, nav, motion, typography weight).
5. **Distraction-free** — focus mode, reader mode, and a single saffron→copper accent gradient as the only loud element.

---

## 2. Color Tokens (Figma Variables)

Create a **Variable Collection: `color`** with three modes: `Light`, `Dark`, `AMOLED`. Define brand primitives once, then map semantic tokens per mode.

### 2.1 Brand primitives (mode-independent)

| Token | Hex | Use |
|---|---|---|
| `brand/ivory` | `#FBF7EC` | Lightest paper |
| `brand/parchment` | `#F2E8D2` | Page background (light) |
| `brand/sand` | `#E6D7B8` | Warm sand, depth |
| `brand/saffron` | `#E8941A` | Primary accent / energy |
| `brand/saffron-deep` | `#D17C0A` | Pressed / gradient end |
| `brand/copper` | `#A65C34` | Secondary accent, seals, dividers |
| `brand/gold` | `#C9A227` | Tertiary / highlights / PRO |
| `brand/forest` | `#2F5D4A` | Success, sync-complete, checkboxes |
| `brand/indigo` | `#33407C` | Links, info, work category |
| `brand/ink` | `#2A2118` | Darkest text / dark-mode base |

**Signature gradient** `accent/grad` = `135° saffron → copper` (`#E8941A → #A65C34`). Used on FAB, primary button, Continue-Writing card, brand seal.

### 2.2 Semantic tokens by mode

| Semantic token | Light | Dark | AMOLED |
|---|---|---|---|
| `bg` | `#EDE2C8` | `#16120D` | `#000000` |
| `surface` | `#FBF7EC` | `#221B14` | `#0C0A07` |
| `surface-2` | `#F6EEDB` | `#2A2219` | `#13100B` |
| `surface-raise` (nav/sheets) | `#FFFFFF` | `#2E261C` | `#171309` |
| `text` | `#2A2118` | `#EFE4CE` | `#F0E6D0` |
| `text-2` (secondary) | `#6B5B45` | `#B7A684` | `#AE9D7C` |
| `text-3` (tertiary/meta) | `#9C8A6E` | `#7E7158` | `#6E6248` |
| `border` | `rgba(42,33,24,.10)` | `rgba(239,228,206,.10)` | `rgba(240,230,208,.09)` |
| `accent` | `#A65C34` | `#D98A53` | `#E8941A` |
| `link` | `#33407C` | `#8E9BD8` | `#94A1E0` |

**Notes on dark/AMOLED:** keep dark *warm* (ink-brown, never pure cool gray). AMOLED uses true `#000` bg for OLED battery savings; surfaces are near-black with the warm tint preserved.

---

## 3. Typography

**Variable Collection: `type`.** Two families:

- **Display (serif):** Iowan Old Style / Palatino / Noto Serif (recommended ship font: **Noto Serif** or **Tiro Devanagari** for Sanskrit support). Used for titles, note headings, and *reader body*.
- **Body/UI (sans):** SF Pro (iOS) / Roboto (Android) via platform default; web fallback system-ui.
- **Mono:** SF Mono / Roboto Mono for code blocks.

| Style | Size / line | Weight | Family |
|---|---|---|---|
| Display XL (page title) | 30 / 33 | 600 | Serif |
| Display L (note title editor) | 26 / 30 | 600 | Serif |
| Section title | 17–20 / 24 | 600 | Serif |
| Card title | 14.5 / 18 | 600 | Serif |
| Reader body | 17 / 31 | 400 | Serif |
| Body | 15 / 26 | 400 | Sans |
| Meta / caption | 11–12 / 16 | 500 | Sans |
| Code | 12–13 / 20 | 400 | Mono |

**Platform nuance:** on Android, page titles switch to **Roboto 700** (Material's preference for sans display), while serif is retained for note content. iOS keeps serif titles with `-0.02em` tracking.

---

## 4. Spacing, Radius, Elevation

- **Spacing scale (4pt base):** 4, 8, 12, 16, 20, 24, 32. Screen gutter = 20.
- **Radius:** `sm 8` (chips/code), `md 14` (cards/inputs), `lg 22` (search/hero), `xl 30` (sheets). FAB: **20 on Android**, **full circle on iOS**.
- **Elevation:**
  - `shadow-1` (cards): `0 1px 2px rgba(42,33,24,.06), 0 2px 6px rgba(42,33,24,.05)`
  - `shadow-2` (hero/dialog): `0 6px 20px rgba(42,33,24,.12)`
  - `shadow-fab`: `0 8px 24px rgba(166,92,52,.40)`
- **Paper texture:** 18–55% opacity fractal-noise overlay (lighter in dark/AMOLED), plus two faint radial tints (copper top-left, gold bottom-right).

---

## 5. Components (build as Figma Components with Variants + Auto Layout)

| Component | Variants / properties | Auto-layout notes |
|---|---|---|
| **Button** | `type`: primary / secondary / ghost · `state`: default / pressed / disabled | Hug contents, 11×20 padding, radius 999 |
| **Note Card** | `state`: pinned / favorite / default · `media`: none/image/audio · `sync`: synced/syncing | Vertical AL, 15 pad, gap 6; fold-corner ▸ pinned |
| **List Row** | `view`: list / compact · `shared`: yes/no | Horizontal AL: 4px accent bar + body; compact hides preview |
| **Input / Search** | `state`: rest / focus / filled · `type`: text / search / title | Fill `surface`, border `border-strong` on focus |
| **FAB** | `platform`: ios (circle) / android (rounded-20) · `expanded`: yes/no | 60×60, accent gradient, `shadow-fab` |
| **Chip / Tag** | `kind`: tag (soft tint) / filter (outline) · `selected`: on/off | Hug, radius 999, 12px label |
| **Toggle** | `on / off` | 44×26 track, 20 thumb |
| **Bottom Nav / Tab Bar** | `platform`: ios / android · 5 items, `active` index | Android shows pill behind active icon; iOS tint-only |
| **Dialog** | `type`: alert / confirm / destructive | radius 22, `shadow-2`, centered |
| **Bottom Sheet** | `height`: peek / half / full · drag handle | radius-xl top, `surface-raise` |
| **Formatting Toolbar** | scrollable, grouped with separators | sticky bottom, `surface-raise`, top divider |
| **Icons** | 1.8–2px stroke, rounded caps, 24px grid | duotone for sync/category states |

**Manuscript motifs to draw as components:** corner *fold* (copper triangle = pinned), *copper-seal* brand mark, hairline *palm-leaf divider* (thin SVG with center dot), *drop-cap* in reader.

---

## 6. Screens

Build each at **390×844 (iOS)** and **360×800 (Android)** frames.

### Home Dashboard
Greeting + "Your Library" title · global search bar · **Continue Writing** hero (progress bar, % + time left) · 4 Quick Actions (New Note, Checklist, Audio, Template) · horizontal carousels: **Pinned**, **Recent** (note cards) · **Folders** 2-col grid (colored icons + counts) · FAB · bottom nav. Sections also exist for Favorites, Recent Audio, Checklists, Tags.

### Notes List
View toggle **List / Grid / Compact** (segmented). Filter chips: All / Favorites / Shared / Recently edited / Archive / Trash. Each note shows **title, preview, folder, tags, last edited, reading time, word count, attachment icons, favorite, pin, sync status** (dot: forest=synced, gold=syncing). Sort + filter affordance in the view bar.

### Note Editor
Distraction-free. Title (serif) + meta row (folder, tags, word count, reading time, "Autosaved" forest dot). Body supports rich text, markdown, checklists, callouts/quotes, code blocks, audio embed with waveform, images, tables, links, drawings, attachments. Top actions: version-history, reader-mode toggle, overflow (focus mode, export, markdown preview, undo/redo). **Sticky formatting toolbar** (B/I/U/S · H1/H2 · list/checklist · image/link · code/table). Autosave + version history.

### Reader Mode
Book-like. Reading-progress bar (top), drop-cap first paragraph, serif 17/31, highlight spans (gold). Floating tool dock: font size, line spacing, **Sepia/Night**, bookmark, highlight/annotate. Bookmarks, highlights, annotations, reading progress persist.

### Templates *(spec only — recreate as a sheet/grid)*
Grid of cards: Journal, Meeting Notes, Daily/Weekly Planner, Study Notes, Research, Project Docs, API Docs, Blog, Story, Recipe, Travel, Diary, Lecture Notes, Blank + **Create custom**. Each card = template thumbnail + name + "Use".

### Search
Search field with voice-search mic. Scope chips: All / Text / **OCR** / Audio transcript / date. Saved searches. Results show match type (text / transcript / OCR) with highlighted query term. Filters by folder, tag, date.

### Folder & Tag System *(spec)*
Nested folders, folder color + icon picker, drag & drop reorder, smart folders (rules), archive. Colored tags, tag suggestions, tag merging.

### Settings
Profile card (avatar, email, devices, PRO badge). Groups: **Appearance** (theme tiles Light/Dark/AMOLED), **Editor & Reading** (font, markdown shortcuts, focus dimming, version history), **Sync & Backup** (cloud sync, auto backup, offline mode), **More** (language, accessibility, privacy & security/app-lock, developer options). Plus Markdown settings, Storage.

### Account *(spec)*
Profile, Devices list, Storage usage meter, Subscription, Cloud sync status, Backup status, Logout, Delete account (destructive dialog).

### Additional surfaces *(spec)*
Favorites, Archive, Trash (restore/permanent-delete), Shared Notes, Notification Center, Activity History, Import/Export, Backup & Restore.

---

## 7. Micro-interactions & Motion

| Interaction | Spec |
|---|---|
| Screen transition | 350ms, `cubic-bezier(.22,.61,.36,1)`, 8px rise + fade |
| Shared element | Note card → editor title morph (match title bounds) |
| FAB press | scale .92 + 8° rotate, haptic light |
| Swipe gestures | row swipe → pin/archive/delete reveal |
| Folder open | accordion expand with stagger |
| Loading | skeleton shimmer on cards (surface-2 → surface sweep) |
| Empty states | palm-leaf illustration + 1 line + primary CTA |
| Success | checkmark draw-on (forest) + subtle haptic |

---

## 8. Platform Differences (Material 3 vs HIG)

| | iOS (HIG) | Android (Material 3) |
|---|---|---|
| FAB | circle | rounded-square 20 |
| Bottom nav active | tint only | pill behind active icon |
| Title font | serif, -0.02em tracking | Roboto 700 sans |
| Sheets | iOS sheet w/ grabber | M3 bottom sheet |
| Back affordance | ‹ chevron + label | top app-bar back arrow |
| Switches | iOS toggle | M3 switch (slightly larger thumb) |
| Ripple | opacity highlight | M3 ink ripple |

---

## 9. Figma File Structure (recommended pages)

1. **🎨 Foundations** — color variables (3 modes), type styles, grid, effects.
2. **🧩 Components** — all components above with variants + interactive component states.
3. **📱 iOS Screens** — light + dark frames.
4. **🤖 Android Screens** — light + dark frames.
5. **🔗 Prototype** — wire bottom nav, FAB→editor, card→editor, editor↔reader, search; use Smart Animate for shared-element transitions.
6. **📦 Handoff** — redlines, token table, icon sheet, spacing notes, export settings (1×/2×/3×, SVG icons).

**Dev handoff tips:** publish variables as a library; name tokens to match code (`color/surface`, `radius/md`) so engineers map 1:1; annotate motion with duration + easing; provide the noise-texture asset as a tiled PNG/SVG.

---

*Prototype companion: `granth-prototype.html` — open in a browser; toggle Theme (Light/Dark/AMOLED) and Platform (iOS/Android) at the top, and tap through Home, Notes, Editor, Reader, Search, Settings, and the live Design System screen.*
