# Pustakam Mobile Rich Text Editor
## Feature Specification

Version: 1.0

---

# Goal

Implement a modern Rich Text Editor for the Pustakam mobile application (Android & iOS).

The editor should provide a smooth, intuitive mobile editing experience similar to Apple Notes, Google Docs, Notion, and Microsoft Word while remaining optimized for touch interactions.

The scope of this document is **only text formatting**.

---

# Core Principles

- Mobile-first UI
- Fast and responsive
- Touch-friendly interactions
- Minimal taps for common actions
- Rich formatting without compromising performance
- Formatting should persist after saving and reopening notes

---

# Text Styles

Support the following paragraph styles:

- Paragraph
- Title
- Subtitle
- Heading 1
- Heading 2
- Heading 3
- Heading 4
- Heading 5
- Heading 6
- Caption

Only one paragraph style may be applied to a text block.

---

# Character Formatting

Support formatting on selected text.

- Bold
- Italic
- Underline
- Strikethrough
- Highlight
- Inline Code
- Superscript
- Subscript
- Clear Formatting

Multiple formatting options should work together.

Examples

- Bold + Italic
- Bold + Underline
- Highlight + Italic
- Bold + Highlight + Underline

---

# Font Formatting

Support

- Font Size
- Font Weight
- Text Color
- Background Color

Future-ready support for custom fonts.

---

# Paragraph Formatting

Support

## Alignment

- Left
- Center
- Right
- Justify

## Spacing

- Line Height
- Paragraph Spacing

## Indentation

- Increase Indent
- Decrease Indent
- First Line Indent

---

# Lists

Support

- Bullet List
- Numbered List
- Checklist
- Nested Bullet Lists
- Nested Numbered Lists
- Mixed Nested Lists
- Restart Numbering
- Continue Numbering

Nested lists should support unlimited levels.

---

# Quote

Support Quote Block formatting.

---

# Code

Support

- Inline Code
- Multi-line Code Block

Code blocks should

- Preserve whitespace
- Use monospace font
- Support horizontal scrolling
- Support Copy Code action

---

# Divider

Support inserting a horizontal divider.

---

# Hyperlinks

Support

- Insert Link
- Edit Link
- Remove Link
- Open Link

Links should open using the device browser.

---

# Tables

Support

- Insert Table
- Add Row
- Delete Row
- Add Column
- Delete Column
- Merge Cells
- Split Cells
- Header Row
- Header Column
- Cell Alignment
- Cell Background Color
- Resize Columns

Tables should support horizontal scrolling on small screens.

---

# Markdown Shortcuts

Automatically convert while typing.

Supported shortcuts

- # Heading 1
- ## Heading 2
- ### Heading 3
- - Bullet List
- 1. Numbered List
- [] Checklist
- > Quote
- ``` Code Block
- --- Divider
- **Bold**
- *Italic*

---

# Text Selection Toolbar

Display a floating toolbar whenever text is selected.

Primary Actions

- Bold
- Italic
- Underline
- Highlight

Secondary Actions

- Text Style
- Text Color
- Background Color
- Alignment
- Lists
- Link
- Code
- Quote
- More

---

# Mobile Formatting Toolbar

Display a toolbar above the keyboard while editing.

Visible Actions

- Bold
- Italic
- Underline
- Lists
- Heading
- Undo
- Redo

Expandable Actions

- Highlight
- Text Color
- Background Color
- Font Size
- Alignment
- Indentation
- Quote
- Code
- Divider
- Table
- Hyperlink
- Clear Formatting

Toolbar should be horizontally scrollable.

---

# Editing Features

Support

- Undo
- Redo
- Copy
- Cut
- Paste
- Paste Without Formatting
- Select All
- Duplicate Selection

---

# Find & Replace

Support

- Find
- Find Next
- Find Previous
- Replace
- Replace All

Highlight search results while typing.

---

# Mobile Gestures

Support intuitive gestures.

- Double Tap → Select Word
- Triple Tap → Select Paragraph
- Long Press → Context Menu
- Drag Selection Handles
- Drag Cursor Magnifier
- Pinch to Zoom Text (Optional)

---

# Auto Formatting

Automatically detect and format

- URLs
- Email Addresses
- Phone Numbers

Automatically create clickable links.

---

# Smart Editing

Support

- Auto Save
- Smart Quotes
- Smart Lists
- Smart Numbering
- Auto Continue Lists
- Auto Close Markdown Syntax
- Preserve Cursor Position

---

# Keyboard Shortcuts (out of scope) 

For hardware keyboards.

- Cmd/Ctrl + B → Bold
- Cmd/Ctrl + I → Italic
- Cmd/Ctrl + U → Underline
- Cmd/Ctrl + Z → Undo
- Cmd/Ctrl + Shift + Z → Redo
- Tab → Increase Indent
- Shift + Tab → Decrease Indent

---

# Accessibility

Support

- Dynamic Type
- Screen Reader Labels
- VoiceOver / TalkBack
- High Contrast
- Keyboard Navigation
- Large Touch Targets

---

# Performance Requirements

- Instant formatting updates
- Smooth scrolling
- No typing lag
- Responsive selection
- Efficient rendering for long notes
- Low memory usage

---

# Data Requirements

Formatting must persist after

- Saving
- Reloading
- Editing
- Exporting
- Synchronization

Formatting should be represented as structured rich text metadata rather than embedded plain text markup.

---

# Acceptance Criteria

The user can:

- Apply multiple formatting styles to selected text.
- Create and modify headings.
- Create bullet, numbered, and checklist items.
- Create nested lists.
- Insert quote and code blocks.
- Insert and edit hyperlinks.
- Create and edit tables.
- Change font size, colors, and alignment.
- Use undo and redo without losing formatting.
- Copy and paste formatted text while preserving styles.
- Search and replace text within a note.
- Reopen notes with all formatting preserved.
- Experience smooth editing on both Android and iOS.