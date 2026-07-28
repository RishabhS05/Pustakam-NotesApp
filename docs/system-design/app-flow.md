## App file KMM app
```

Pustakam
│
├── androidApp/                         # Android (Jetpack Compose)
│
├── iosApp/                             # iOS (SwiftUI)
│
└── shared/
│    │
│    ├── core/
│    │   ├── common/
│    │   ├── model/
│    │   ├── util/
│    │   ├── logger/
│    │   ├── coroutine/
│    │   ├── dispatcher/
│    │   ├── navigation/
│    │   └── ui-state/
│    │
│    ├── network/
│    │   ├── api/
│    │   ├── ktor/
│    │   ├── auth/
│    │   └── websocket/
│    │
│    ├── database/
│    │   ├── sqldelight/
│    │   ├── dao/
│    │   ├── mapper/
│    │   └── cache/
│    │
│    ├── hardware/
│    │   ├── permission/
│    │   ├── camera/
│    │   ├── microphone/
│    │   ├── storage/
│    │   ├── location/
│    │   └── biometric/
│    │
│    ├── media/
│    │   ├── player/
│    │   ├── recorder/
│    │   ├── thumbnail/
│    │   ├── compression/
│    │   └── streaming/
│    │
│    ├── sync/
│    │
│    ├── ai/
│    │
│    ├── analytics/
│    │
│    └── feature/
│        │
│        ├── notes/
│        │   │
│        │   ├── presentation/
│        │   │   │
│        │   │   ├── home/
│        │   │   │   ├── common files
│        │   │   │   └── components/
│        │   │   │
│        │   │   ├── editor/
│        │   │   │   ├── (common files) 
│        │   │   │   └── components/
│        │   │   │
│        │   │   ├── search/
│        │   │   │
│        │   │   ├── folder/
│        │   │   │
│        │   │   ├── workspace/
│        │   │   │
│        │   │   ├── reader/
│        │   │   │
│        │   │   ├── settings/
│        │   │   │
│        │   │   ├── shared/
│        │   │   │   ├── components/
│        │   │   │   ├── navigation/
│        │   │   │   └── theme/
│        │   │   │
│        │   │   └── mapper/
│        │   │
│        │   ├── domain/
│        │   │   ├── model/
│        │   │   ├── repository/
│        │   │   ├── usecase/
│        │   │   └── validation/
│        │   │
│        │   ├── data/
│        │   │   ├── repository/
│        │   │   ├── datasource/
│        │   │   ├── local/
│        │   │   ├── remote/
│        │   │   └── mapper/
│        │   │
│        │   └── api/
│        │       └── NotesApi.kt
│        │
│        ├── text/
│        │   ├── presentation/
│        │   ├── domain/
│        │   ├── data/
│        │   └── api/
│        │
│        ├── image/
│        │   ├── presentation/
│        │   ├── domain/
│        │   ├── data/
│        │   └── api/
│        │
│        ├── audio/
│        │   ├── presentation/
│        │   ├── domain/
│        │   ├── data/
│        │   └── api/
│        │
│        ├── video/
│        │   ├── presentation/
│        │   ├── domain/
│        │   ├── data/
│        │   └── api/
│        │
│        ├── document/
│        │   ├── presentation/
│        │   ├── domain/
│        │   ├── data/
│        │   └── api/
│        │
│        ├── folder/
│        │
│        ├── workspace/
│        │
│        ├── search/
│        │
│        ├── reminder/
│        │
│        ├── settings/
│        │
│        ├── profile/
│        │
│        ├── scanner/
│        │
│        ├── ocr/
│        │
│        ├── pdf/
│        │
│        ├── markdown/
│        │
│        ├── import/
│        │
│        ├── export/
│        │
│        ├── tts/
│        │
│        ├── stt/
│        │
│        └── translation/
│
├── test/        (not focusing currently out of scope )
│   ├── presentation/
│   │   ├── HomeViewModelTest.kt (not exaxtly same but something )
│   │   ├── EditorViewModelTest.kt (not exaxtly same but something )
│   │   └── SearchViewModelTest.kt (not exaxtly same but something )
│   │
│   ├── domain/
│   │   ├── CreateNoteUseCaseTest.kt (not exaxtly same but something )
│   │   ├── DeleteNoteUseCaseTest.kt (not exaxtly same but something )
│   │   └── SearchNotesUseCaseTest.kt (not exaxtly same but something )
│   │
│   ├── data/
│   │   ├── NotesRepositoryTest.kt
│   │   ├── LocalDataSourceTest.kt
│   │   └── RemoteDataSourceTest.kt
│   │
│   └── fake/
│       ├── FakeNotesRepository.kt
│       ├── FakeDao.kt
│       └── FakeApi.kt
│
└── integration-test/
    ├── NotesRepositoryIntegrationTest.kt
    └── NotesSyncIntegrationTest.kt
```
## App Architecture
```
Android Compose                 SwiftUI
       │                           │
       └──────────────┬────────────┘
                      │
             Shared ViewModel (MVI/MVVM)
                      │
                  Domain (Use Cases)
                      │
               Repository Interfaces
                      │
        ┌─────────────┴─────────────┐
        │                           │
    Database                    Network
        │                           │
        └─────────────┬─────────────┘
                      │
                     Core


```
## Dependency Flow 

```
androidApp (Compose)
         │
         │
iosApp (SwiftUI)
         │
         ▼
Feature Presentation
(Home / Editor / Reader / ...)
         │
         ▼
ViewModel (MVI/MVVM)
         │
         ▼
UseCases (Domain)
         │
         ▼
Repository Interface
         │
         ▼
Repository Implementation (Data)
         │
   ┌─────┴──────────┐
   │                │
Database        Network
   │                │
   ├──────┬─────────┤
   │      │         │
 Media  Hardware   Sync
         │
         ▼
        Core
```

# Current code flow  modify according to multimodule 
### iosApp (SwiftUI) 
```
SwiftUI
    │
    ▼
Bridge
    │
    ▼
Shared ViewModel
    │
    ▼
UseCase
    │
    ▼
Repository
    │
    ▼
Database / Network
```
### androidApp 
``
Android Compose (ui)
      │
      ▼
presentation (ui)
      │
      ▼
ViewModel
      │
      ▼
UseCase
      │
      ▼
Repository
      │
      ▼
Database / Network
``