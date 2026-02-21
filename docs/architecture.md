# Architecture

## Overview

L'application suit le pattern **MVVM** (Model-View-ViewModel) recommande par Google pour les apps Android, avec une couche **Repository** pour abstraire les sources de donnees.

## Schema d'architecture

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│                                                         │
│  ┌─────────────┐    ┌──────────────┐    ┌────────────┐  │
│  │ MainActivity │    │ SwipeRefresh │    │  Widget    │  │
│  │              │    │   Layout     │    │  Provider  │  │
│  │ - Stats Grid │    └──────────────┘    └─────┬──────┘  │
│  │ - Today Card │                              │         │
│  │ - Calendar   │                              │         │
│  └──────┬───────┘                              │         │
│         │ observes                             │         │
│         ▼                                      │         │
│  ┌──────────────┐                              │         │
│  │ MainViewModel│                              │         │
│  │              │                              │         │
│  │ - stats      │ LiveData                     │         │
│  │ - entries    │ MutableLiveData              │         │
│  │ - isLoading  │                              │         │
│  │ - pushupCount│                              │         │
│  └──────┬───────┘                              │         │
└─────────┼──────────────────────────────────────┼─────────┘
          │ calls                                │ reads/writes
          ▼                                      ▼
┌─────────────────────────────────────────────────────────┐
│                     Data Layer                          │
│                                                         │
│  ┌──────────────────────────────────────────────────┐   │
│  │               PushUpRepository                    │   │
│  │                                                    │   │
│  │  sync()         ─── API GET /stats + /entries     │   │
│  │                     └── cache in Room             │   │
│  │                                                    │   │
│  │  validateToday() ── API POST /validate            │   │
│  │                     └── re-sync on success        │   │
│  │                                                    │   │
│  │  statsLiveData   ── Room LiveData (reactive)      │   │
│  └──────────┬───────────────────┬────────────────────┘   │
│             │                   │                        │
│             ▼                   ▼                        │
│  ┌──────────────────┐  ┌────────────────────┐           │
│  │   Room Database   │  │  Retrofit Client   │           │
│  │                    │  │                    │           │
│  │ ┌──────────────┐  │  │  Base URL:         │           │
│  │ │ pushup_entries│  │  │  chetana.dev       │           │
│  │ │              │  │  │                    │           │
│  │ │ - date (PK)  │  │  │  GET  /stats       │           │
│  │ │ - pushups    │  │  │  GET  /entries     │           │
│  │ │ - validated  │  │  │  POST /validate    │           │
│  │ └──────────────┘  │  └────────────────────┘           │
│  │ ┌──────────────┐  │                                   │
│  │ │ cached_stats  │  │                                   │
│  │ │ (singleton)  │  │                                   │
│  │ │              │  │                                   │
│  │ │ - streaks    │  │                                   │
│  │ │ - totals     │  │                                   │
│  │ │ - today flag │  │                                   │
│  │ └──────────────┘  │                                   │
│  └────────────────────┘                                   │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                   Background Layer                      │
│                                                         │
│  ┌────────────────────┐    ┌─────────────────────────┐  │
│  │    SyncWorker       │    │  PushUpWidgetProvider   │  │
│  │  (WorkManager)      │    │  (AppWidgetProvider)    │  │
│  │                      │    │                         │  │
│  │  Every 30 min:       │    │  onUpdate:              │  │
│  │  1. repo.sync()     │    │  - Read Room stats      │  │
│  │  2. Update widgets  │    │  - Update RemoteViews   │  │
│  └────────────────────┘    │                         │  │
│                              │  ACTION_VALIDATE:       │  │
│                              │  - POST /validate       │  │
│                              │  - Refresh widget       │  │
│                              └─────────────────────────┘  │
└─────────────────────────────────────────────────────────┘
```

## Flux de donnees

### 1. Demarrage de l'app

```
MyApplication.onCreate()
    ├── Init Room database (fallbackToDestructiveMigration)
    └── Enqueue SyncWorker (periodic 30 min)

MainActivity.onCreate()
    └── MainViewModel.init()
        └── refresh()
            └── PushUpRepository.sync()
                ├── GET /api/health/stats  ──► Room cached_stats
                └── GET /api/health/entries ──► Room pushup_entries
```

### 2. Validation du jour

```
User tap "Validate"
    └── MainViewModel.validate()
        └── PushUpRepository.validateToday(pushups)
            ├── POST /api/health/validate { pushups: N }
            └── On success: sync() ──► refresh all data
                └── LiveData updates ──► UI auto-refresh
```

### 3. Pull-to-refresh

```
User swipe down
    └── SwipeRefreshLayout.onRefresh()
        └── MainViewModel.refresh()
            └── PushUpRepository.sync()
```

### 4. Widget update cycle

```
WorkManager (every 30 min)
    └── SyncWorker.doWork()
        ├── PushUpRepository.sync()
        └── PushUpWidgetProvider.updateAllWidgets()

User tap widget "Validate"
    └── Broadcast ACTION_VALIDATE
        └── PushUpWidgetProvider.onReceive()
            ├── POST /api/health/validate
            ├── Update Room stats
            └── Refresh widget UI
```

## Strategie offline-first

```
                    ┌─────────┐
                    │  User   │
                    └────┬────┘
                         │
                    ┌────▼────┐
                    │   UI    │
                    └────┬────┘
                         │ observe LiveData
                    ┌────▼────┐
         ┌──────────│  Room   │──────────┐
         │          │ (cache) │          │
         │          └────┬────┘          │
         │               │               │
    Read (always)   Write (on sync)      │
         │               │               │
         │          ┌────▼────┐          │
         │          │   API   │──────────┘
         │          │ (source │   On failure:
         │          │ of truth│   show cached data
         │          └─────────┘
         │
    Immediate display
    (even if API fails)
```

L'UI lit **toujours** depuis Room via LiveData. Les appels API ecrivent dans Room, ce qui declenche automatiquement la mise a jour de l'UI. Si l'API est injoignable, l'utilisateur voit les dernieres donnees cachees.
