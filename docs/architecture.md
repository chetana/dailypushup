# Architecture

## Overview

L'application suit le pattern **MVVM** (Model-View-ViewModel) recommande par Google pour les apps Android, avec une couche **Repository** pour abstraire les sources de donnees. L'authentification utilise **Google Sign-In** via Credential Manager avec des **ID Tokens** verifies cote backend.

## Schema d'architecture

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│                                                         │
│  ┌─────────────┐    ┌──────────────┐    ┌────────────┐  │
│  │ MainActivity │    │ SwipeRefresh │    │  Widget    │  │
│  │              │    │   Layout     │    │  Provider  │  │
│  │ - Sign-In   │    └──────────────┘    └─────┬──────┘  │
│  │ - Stats Grid│                              │         │
│  │ - Today Card│                              │         │
│  │ - Calendar  │                              │         │
│  │ - Sign-Out  │                              │         │
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
│  │ ┌──────────────┐  │  │  Auth Interceptor  │           │
│  │ │ pushup_entries│  │  │  ┌──────────────┐ │           │
│  │ │              │  │  │  │ TokenStore    │ │           │
│  │ │ - date (PK)  │  │  │  │ (SharedPrefs)│ │           │
│  │ │ - pushups    │  │  │  └──────────────┘ │           │
│  │ │ - validated  │  │  │                    │           │
│  │ └──────────────┘  │  │  Bearer <idToken>  │           │
│  │ ┌──────────────┐  │  │  on every request  │           │
│  │ │ cached_stats  │  │  │                    │           │
│  │ │ (singleton)  │  │  │  GET  /stats       │           │
│  │ └──────────────┘  │  │  GET  /entries     │           │
│  └────────────────────┘  │  POST /validate    │           │
│                           └────────────────────┘           │
└─────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────┐
│                    Auth Layer                           │
│                                                         │
│  ┌────────────────────┐    ┌─────────────────────────┐  │
│  │  GoogleAuthManager  │    │      TokenStore         │  │
│  │  (Credential Mgr)   │    │   (SharedPreferences)   │  │
│  │                      │    │                         │  │
│  │  signIn() → Google   │───▶│  saveToken(idToken,    │  │
│  │  signOut() → clear   │    │    email, name)         │  │
│  │                      │    │  getToken() → String?   │  │
│  │  Web Client ID       │    │  isLoggedIn() → Bool    │  │
│  │  (serverClientId)    │    │  clear()                │  │
│  └────────────────────┘    └─────────────────────────┘  │
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
│  └────────────────────┘    │  - Show emojis 🔥💪     │  │
│                              │                         │  │
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
    ├── Force light mode (MODE_NIGHT_NO)
    ├── RetrofitClient.init(context)
    ├── Init Room database (fallbackToDestructiveMigration)
    └── Enqueue SyncWorker (periodic 30 min)

MainActivity.onCreate()
    ├── TokenStore.isLoggedIn()?
    │   ├── No  → GoogleAuthManager.signIn()
    │   │         ├── Success → TokenStore.saveToken() → initApp()
    │   │         └── Failure → Toast error
    │   └── Yes → initApp()
    │
    └── initApp()
        └── MainViewModel.init()
            └── refresh()
                └── PushUpRepository.sync()
                    ├── GET /api/health/stats  ──► Room cached_stats
                    └── GET /api/health/entries ──► Room pushup_entries
```

### 2. Validation du jour

```
User tap "✅ Validate"
    └── MainViewModel.validate()
        └── PushUpRepository.validateToday(pushups)
            ├── POST /api/health/validate { pushups: N }
            │   (with Authorization: Bearer <idToken>)
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

User tap widget "✅ Validate"
    └── Broadcast ACTION_VALIDATE
        └── PushUpWidgetProvider.onReceive()
            ├── POST /api/health/validate
            ├── Update Room stats
            └── Refresh widget UI (🎉 Done today!)
```

### 5. Auth token expiration

```
API returns 401
    └── OkHttp Auth Interceptor
        └── TokenStore.clear()
            └── Next refresh() detects no token
                └── MainActivity re-triggers signIn()
                    └── Credential Manager (autoSelectEnabled=true)
                        └── Silent re-auth → new token → resume
```

### 6. Sign-out

```
User long-press on target text
    └── AlertDialog "Sign Out?"
        └── Confirm
            ├── GoogleAuthManager.signOut()
            ├── TokenStore.clear()
            └── Activity.recreate() → back to sign-in flow
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
         │          │ + Bearer│   On failure:
         │          │  token  │   show cached data
         │          └─────────┘   On 401: re-auth
         │
    Immediate display
    (even if API fails)
```

L'UI lit **toujours** depuis Room via LiveData. Les appels API ecrivent dans Room, ce qui declenche automatiquement la mise a jour de l'UI. Si l'API est injoignable, l'utilisateur voit les dernieres donnees cachees. Si le token a expire (401), l'interceptor clear le token et le prochain cycle relance le sign-in.
