# MasterEditor Canvas — UI Specification

13-Aug-2026 · iOS + Android · UI only, no data or persistence concerns.

This is the target behaviour. Where today's build differs, the row is marked **GAP**.

---

## 1. Vocabulary

| Term | Meaning |
|---|---|
| **Canvas** | The infinite workspace. One canvas per note. |
| **Page** | A resizable sheet of paper on the canvas. A top-level node. |
| **Widget** | Anything placed **inside** a page — text, image, video, audio, document, link, location, table, drawing. |
| **Viewport** | What the screen currently shows of the canvas: offset + zoom. |
| **Tool** | The persistent mode the user selected: Select, Hand, Zoom, Lock. |
| **Gesture** | The transient interaction in flight: Dragging, Resizing, Editing, or None. |

A page can hold **many** widgets. **GAP — today one page effectively carries one widget; child widgets are not laid out inside the page bounds.**

---

## 2. Canvas surface

### 2.1 Zoom

| Property | Value |
|---|---|
| Range | 10% – 2000% |
| Default on open | **100%** |
| Zoom stops | 10, 25, 50, 75, 100, 150, 200, 400, 800, 2000 |
| Readout | Percentage in the bottom bar, live |
| Pinch | Two fingers anywhere, centred on the pinch midpoint |
| Buttons | −, +, fit-to-screen |

**GAP — the canvas currently opens at 10%.** Fit-to-screen computes a scale from the union of all page bounds; with the page at 560×760 that clamps to `MIN_SCALE`. Page dimensions must be chosen so a single page reads 100% on a phone, and the initial viewport must not run fit-to-screen.

### 2.2 Pan

- One finger with the Hand tool, or two fingers at any time.
- One finger with the Select tool does **not** pan — it goes to the page underneath for selection and text.
- Panning is captured before the page sees the touch, so a pinch that starts over text still zooms the canvas.

### 2.3 Background

Flat page colour, no grid or dot pattern. A tap on bare background clears the selection.

---

## 3. Page

### 3.1 Appearance

| Element | Spec |
|---|---|
| Surface | `colors.surface`, 8dp corner radius |
| Border, unselected | 1dp `colors.divider` |
| Border, selected | 1.5dp `colors.accent` |
| Name bar | Top strip, 12sp, 10dp horizontal / 6dp vertical padding |
| Name bar background | `colors.surface`, or `colors.accentSoft` when selected |
| Default name | "Page 1", "Page 2", … by index |
| Content inset | 16dp on all sides |

### 3.2 Size

- Default size is fixed and chosen so one page fills a phone width at 100% zoom.
- Resizable by dragging the handle at the bottom-right of a selected page.
- Minimum 24×24.
- The handle is 22dp, `colors.accent`, 4dp radius, and only visible while the page is selected and resizing is permitted.

### 3.3 Overlap

Pages **must never overlap**. A new page is placed to the right of the last page with a 48-unit gap. Dragging a page so it would intersect another must push it to the nearest free position on release. **GAP — no collision handling exists today; pages can be dragged on top of one another.**

### 3.4 Content clipping

All page content is **clipped to the page bounds**. Content longer than the page scrolls **inside** the page; it never renders outside the border. **GAP — the text widget currently grows past the page's bottom edge instead of scrolling within it.**

---

## 4. Widgets inside a page

### 4.1 Layout

- Widgets stack vertically inside the page, 24-unit padding from the page edge.
- A new widget is placed below whatever is already on the page.
- Widget width defaults to page width minus padding on both sides.
- Widgets are clipped to the page.

### 4.2 Types and rendering

| Type | Rendering |
|---|---|
| Text | `MasterTextWidget` — see §6 |
| Image / GIF | Image card, fills its widget frame, tap opens full screen |
| Video | Video card with play affordance, tap opens full screen |
| Audio | Inline player with waveform, play/pause, delete |
| Document (PDF/DOCX/MD/EPUB) | File card with name and icon, tap opens the reader |
| Link | Link icon + URL, accent colour, 3-line clamp, tap opens the browser |
| Location | Pin icon + address or lat/long, 3-line clamp, tap opens maps |
| Table | Placeholder — not implemented |
| Drawing | Placeholder — not implemented |

### 4.3 Moving between pages

Dragging a widget onto another page re-parents it to that page on release. Dropping on bare canvas detaches it. While a page is dragged, its widgets move with it.

---

## 5. Tools and gestures

### 5.1 The four tools

| Tool | Icon | Pan | Zoom | Select | Drag node | Resize | Edit text |
|---|---|---|---|---|---|---|---|
| **Select** | touch | ✓ | ✓ | ✓ | ✓ | ✓ | ✓ |
| **Hand** | hand | ✓ | ✓ | – | – | – | – |
| **Zoom** | magnifier | ✓ | ✓ | – | – | – | – |
| **Lock** | padlock | – | – | ✓ | – | – | – |

### 5.2 Gestures override tools

While a gesture is in flight it is exclusive — everything else is suspended:

| Gesture | What still works |
|---|---|
| **Dragging** | dragging only |
| **Resizing** | resizing only |
| **Editing** | typing only, plus caret reveal |
| **None** | whatever the tool permits |

Selection is always allowed, even mid-edit — tapping another page leaves edit mode and selects it.

### 5.3 Gesture reference

| Gesture | Result |
|---|---|
| Tap bare canvas | Clear selection |
| Tap a page | Select it |
| Tap page text | Enter edit mode |
| Double-tap a page | Enter edit mode, zoom to the page |
| Double-tap bare canvas | Exit edit mode, else fit to screen |
| Drag a selected page | Move it |
| Drag the corner handle | Resize |
| Pinch | Zoom about the midpoint |
| One-finger drag (Hand) | Pan |

---

## 6. Text widget on a page

### 6.1 Sizing

- Opens at **5 lines** tall.
- Grows downward as text is added, so other widgets stay visible beneath it.
- Stops growing at the page's inner bottom edge, then scrolls internally. **GAP — currently grows past the page.**

### 6.2 Typography

| Property | Value |
|---|---|
| Body size | 16sp × 1.15 canvas scale |
| Line spacing | **1.2×** — normal reading density. **GAP — currently 1.45×, visibly too loose.** |
| Paragraph spacing | 8 units |
| Colour | `colors.onSurface`; captions `colors.onSurfaceMuted` |
| Placeholder | "Keep your thoughts alive." in muted colour |

### 6.3 Lists

| Marker | Spec |
|---|---|
| Bullet | Filled dot, **vertically centred on the first line's cap height**, sized ~0.35× body size, drawn in the gutter. **GAP — currently mis-sized and top-aligned.** |
| Numbered | "1." … right-aligned in the gutter, same baseline as the text |
| Checklist | Tappable box, 16×16, 4dp radius, accent fill when checked, 1.5dp muted outline when unchecked |
| Gutter width | 26 units |
| Indent step | 20 units per level |

**GAP — checkboxes are not togglable.** Tapping the box must flip the checked state; the tap target is the full gutter width by one line height.

### 6.4 Caret visibility

The caret must **never** sit behind the keyboard.

- On every keystroke and selection change, the caret's screen rect is compared against the visible area (screen height minus keyboard height).
- If the caret is below the visible bottom, the canvas pans up until the caret clears it by 24 units.
- If the caret is above the visible top, the canvas pans down.
- Panning for caret reveal is permitted during edit mode even though ordinary panning is locked.

### 6.5 Edit mode

Entering edit mode:

1. The page zooms to fill the screen **from top to bottom**, pinned under the safe area. **GAP — currently fits width only, leaving the page short of the screen bottom.**
2. Canvas pan, zoom, drag and resize are locked.
3. The formatting toolbar attaches above the keyboard.
4. The previous viewport is remembered.

Leaving edit mode — tapping another page, tapping bare canvas, or the toolbar's dismiss — restores the previous viewport.

---

## 7. Formatting toolbar

Pinned directly above the keyboard, never floating over it.

**Primary row:** bold, italic, underline, highlight, more.

**Expanded row:** text style, text colour, background colour, alignment, bullet list, numbered list, checklist, indent, outdent, quote, inline code, link, clear formatting.

Sheet-backed actions — text style, text colour, background colour, font size, alignment, link — dismiss the keyboard and open a bottom sheet. Everything else applies immediately.

Undo and redo live in the screen's top bar, not the formatting toolbar, because they are note-wide.

---

## 8. Bottom bar

Left to right: zoom out, zoom percentage, zoom in, fit to screen, the four tool buttons.

- Horizontally scrollable, so nothing is pushed off a narrow screen.
- Sits above the system navigation bar.
- Active tool tinted `colors.accent`; the rest `colors.onSurface`.

**Add** is a separate floating button at the bottom-right, always visible, never inside the scrolling bar.

---

## 9. Add menu

Opened from the Add button.

| Item | Behaviour |
|---|---|
| New page | Creates a page beside the last one and selects it |
| Photo or video | Camera, then places the result in the selected page |
| Record audio | Recorder, then places the clip in the selected page |
| Location | Current location as a widget |
| Import a file | File picker or URL |
| Table | Placeholder widget |
| Drawing | Placeholder widget |

Every widget lands **in the selected page**. If the note has no page yet, one is created first and the widget goes into it.

---

## 10. Selection and focus

- One page or widget selected at a time.
- Selected node draws the accent border and shows its resize handle.
- Selecting brings the node to the front.
- Selection survives zoom and pan.
- Renaming: tap the name bar, edit inline, commit on Done.

---

## 11. Empty states

| Condition | Shown |
|---|---|
| Note has no pages | One empty page with the text placeholder |
| Page has no widgets | Just the text widget at 5 lines |
| Widget has no content | Muted placeholder card naming the type |

---

## 12. Open GAP summary

| # | Item | Section |
|---|---|---|
| 1 | Canvas opens at 10% instead of 100%; page size not tuned to 100% | §2.1 |
| 2 | Pages can overlap; no collision handling | §3.3 |
| 3 | Page content renders outside the page instead of clipping and scrolling | §3.4, §6.1 |
| 4 | One page carries one widget; multi-widget layout inside a page missing | §1, §4.1 |
| 5 | Edit mode fits width only, not top-to-bottom | §6.5 |
| 6 | Checkboxes not togglable | §6.3 |
| 7 | Bullet size and vertical alignment wrong | §6.3 |
| 8 | Line spacing 1.45× is too loose; should be 1.2× | §6.2 |
