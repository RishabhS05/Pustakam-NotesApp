# Refactor M2 — `Note` equals/hashCode (StateFlow emission fix)

From `note-model-redesign.md` §1.1 (M2), executable **now** against the current model — no schema, no wire, no API change. **Documentation only — not applied yet.**

---

## 1. The problem, concretely

`Note.kt` today:

```kotlin
override fun equals(other: Any?): Boolean {
    return other is Note && other.id == this.id          // ← id ONLY
}

override fun hashCode(): Int {
    return 31 * (id.hashCode() +
        (updatedAt?.hashCode() ?: 0) +
        (createdAt?.hashCode() ?: 0) +
        (title?.hashCode() ?: 0) +
        (isSynced?.hashCode() ?: 0) +
        (contents?.count()?.hashCode() ?: 0))            // ← mixes 6 fields
}
```

Two independent defects:

**D1 — id-only `equals` silently suppresses UI updates.** Trace the actual bug path:

```
user edits note title → insertOrUpdateNote(note)
→ _notes.update { notes.copy(notes = newList) }        // newList has the edited note
→ MutableStateFlow compares old vs new value            // StateFlow skips equal values
→ Notes.equals → ArrayList.equals → element-wise Note.equals
→ same id ⇒ "equal" ⇒ StateFlow DOES NOT EMIT
→ list screen keeps showing the old title
```

Any two versions of the same note — different title, different contents — compare equal. Every observer downstream (`notesState`, iOS `observeNotes`, Android collectors) is blind to edits. This is the root cause behind "list doesn't refresh" symptoms and the old iOS workaround of wiping the notes array on tag emissions.

**D2 — broken equals/hashCode contract.** Objects that are `equal` (same id) can have **different hashCodes** (different title/updatedAt/contents count). That violates the `hashCode` contract; any future `HashSet<Note>`/`HashMap<Note, _>` would corrupt (lookups miss, duplicates stored). A latent landmine even where nothing crashes today.

## 2. The fix

Delete both overrides. `Note` is a `data class` — the generated implementations compare/hash **all constructor fields**, which is exactly what StateFlow needs:

```kotlin
@Serializable
data class Note(
    @SerialName("_id")
    val id: String,
    var title: String?,
    var updates: List<String>? = null,
    var updatedAt: String?,
    var createdAt: String?,
    var categoryId: String? = "",
    var isSynced: Boolean? = false,
    var contents: List<NoteContentModel>? = emptyList(),
)   // ← no body needed: data-class equals/hashCode/toString are generated
```

Net diff: **−15 lines, +0.**

> Identity comparisons ("same note?") remain available and explicit: `a.id == b.id`. Equality (`==`) now answers "same note *state*?" — which is what reactive streams must ask.

## 3. Call-site audit (every place `Note` equality is relied on — verified by grep)

| Call site | Uses | Impact of the fix |
|---|---|---|
| `NoteRepository.insertOrUpdateNote` — `indexOfFirst { note.id == n.id }` | explicit id compare | none (already id-based) |
| `NoteRepository.deleteNote` — `find { id == note.id }` then `it.notes.remove(note)` | `remove` uses `equals`, but on the **same instance** just found → reference-equal → still removed | works; hardened anyway (§4) |
| Swift `Note: Identifiable` (`Note.ext.swift`) | `id` property | none |
| SwiftUI `StaggeredGrid`/`ForEach` diffing | `Identifiable.id` | none |
| Compose lists (Android) | keys by `note.id` | none |
| `Set<Note>` / `Map<Note, _>` anywhere | **none found** in shared/android/ios | D2 landmine defused before anyone steps on it |

Adjacent but **out of M2 scope** — flagged so it isn't done accidentally:

- `NoteContentModel`'s custom equals (defect **C7**) has a live dependent: `NoteContentRepository.updateNoteContent` uses `newList.indexOf(note)`, and Android's editor uses `contents.indexOf(find)`. Removing C7's equals must happen **together with** switching those to `indexOfFirst { it.id == content.id }`. Separate piece; do not bundle into M2.

## 4. One defensive hardening (same file pass, `NoteRepository.deleteNote`)

Today's removal works only because the removed object is the found instance. Make it id-based so it can never regress when instances stop being shared (immutable-model future):

```kotlin
// BEFORE
_notes.update {
    val note = it.notes.find { note -> id == note.id }
    if (it.notes.remove(note)) {
        it.copy(notes = it.notes)              // also: in-place mutation (Piece 3 fixes fully)
    } else it
}

// AFTER (minimal M2-scope hardening; Piece 3 still owns the full immutable rewrite)
_notes.update { current ->
    val newList = ArrayList(current.notes.filterNot { n -> n.id == id })
    if (newList.size != current.notes.size) current.copy(notes = newList) else current
}
```

Bonus: this also removes one in-place mutation and — with the equals fix — makes delete emissions reliable.

## 5. Behavior changes to expect (all desirable, verify consciously)

1. **More emissions, not fewer.** Screens now re-render on real edits. If any screen was accidentally depending on *suppressed* emissions (none known), it will surface immediately in testing.
2. `distinct`/`contains` semantics on notes become state-based. No current call sites depend on the old semantics (§3 table).
3. Kotlin's `equals` exports to Swift/ObjC as `isEqual:` — Swift `note1 == note2` follows the new semantics too. No Swift code currently compares Notes with `==` (grep-verified).

## 6. Files touched

| File | Change |
|---|---|
| `commonMain/.../response/notes/Note.kt` | delete `equals` + `hashCode` overrides (15 lines) |
| `commonMain/.../noteRepository/NoteRepository.kt` | `deleteNote` hardening (§4) |
| everything else | untouched — no signature changes anywhere |

## 7. Verification

1. Edit a note's **title only**, save → list screen shows new title without leaving/re-entering (was broken).
2. Edit **content only** (text block), save → list card preview updates (was broken).
3. Delete note → disappears immediately; repeat-delete same id → no-op, no crash.
4. Create note → appears once (no duplicates from the extra emissions).
5. iOS `observeNotes` + Android collector receive one emission per actual change (log-count check).
6. Regression sweep of §3 call sites: create/edit/delete/reorder through both platforms.

## 8. Relation to the series

M2 is the highest-leverage 15-line deletion in the codebase — it makes Piece 3 (notes list state) mostly about immutability hygiene instead of emission bugs, and it's a prerequisite step of `note-model-redesign.md` Phase 1 done early. C7 (content equality) follows as its own piece with the `indexOf` call-site fixes listed above.
