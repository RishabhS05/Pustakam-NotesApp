# Pustakam Database Design (MongoDB)

> Based on the uploaded ERD. Although MongoDB is document-oriented and does not require normalization, the following progression shows how the logical model evolves from 1NF → 2NF → 3NF before mapping to MongoDB collections.

Source: Uploaded ERD fileciteturn0file0

---

# 1NF

```mermaid
erDiagram
    USER ||--o{ NOTE : owns
    CATEGORY ||--o{ NOTE : classifies
    NOTE ||--|{ NOTE_CONTENT : contains
    USER ||--o{ POST : creates
    POST ||--o{ COMMENT : has
    USER ||--o{ COMMENT : writes
    USER ||--o{ CHAT_MESSAGE : sends
    ROOM ||--o{ CHAT_MESSAGE : contains
    ROOM ||--o{ ROOM_USER : members
    USER ||--o{ ROOM_USER : joins
    USER ||--o{ PAYMENT : makes
    USER ||--|| SUBSCRIPTION : has
    USER ||--|| SETTINGS : configures
    NOTE ||--o{ SHARE : shared_with
    USER ||--o{ SHARE : receives
```

---

# 2NF

Remove repeating and partially dependent attributes.

```mermaid
erDiagram
    USER {
        string userId PK
        string name
        string lastName
        string password
    }

    CATEGORY {
        string categoryId PK
        string title
        string ownerId FK
    }

    NOTE {
        string noteId PK
        string ownerId FK
        string categoryId FK
        string title
        datetime createdAt
        datetime updatedAt
        int version
    }

    NOTE_CONTENT {
        string contentId PK
        string noteId FK
        string type
        int position
        string value
    }

    NOTE ||--o{ NOTE_CONTENT : contains
    USER ||--o{ NOTE : owns
    CATEGORY ||--o{ NOTE : classifies
```

---

# 3NF

Separate independent entities.

```mermaid
erDiagram

    USER ||--|| SETTINGS : has
    USER ||--|| SUBSCRIPTION : owns
    USER ||--o{ PAYMENT : payments

    USER ||--o{ NOTE : owns
    CATEGORY ||--o{ NOTE : categorizes
    NOTE ||--o{ NOTE_CONTENT : contains
    NOTE ||--o{ SHARE : shared

    USER ||--o{ POST : creates
    POST ||--o{ COMMENT : has
    USER ||--o{ COMMENT : writes

    ROOM ||--o{ CHAT_MESSAGE : contains
    USER ||--o{ CHAT_MESSAGE : sends
    ROOM ||--o{ ROOM_USER : members
    USER ||--o{ ROOM_USER : joins
```

---

# MongoDB Collection Design

```text
users
 ├── settings (embedded)
 ├── subscription (embedded)
 └── paymentIds (optional references)

notes
 ├── categoryId
 ├── content[]     (embedded)
 ├── share[]
 └── version

categories

posts
 ├── comments[] (or separate collection if very large)

rooms
 ├── participants[]
 └── lastMessage

messages

payments
```

## MongoDB Design Notes

- Embed `note.content[]` because it belongs only to a single note.
- Embed `settings` and `subscription` inside `users`.
- Keep `messages` in a separate collection for scalability.
- Keep `payments` separate because they grow independently.
- `shares` may be embedded inside notes unless access lists become very large.
