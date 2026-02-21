# Daily Push-Up

Android companion app for [chetana.dev](https://chetana.dev) — track daily push-ups with gamification, Google OAuth, offline caching, and a home screen widget.

## Features

- **Google Sign-In** — Authenticate via Credential Manager, stateless Bearer token auth
- **Stats dashboard** — Current streak 🔥, total push-ups 💪, days completed ✅, best streak 🏆
- **Daily validation** — Stepper to set push-up count and validate for the day
- **Calendar view** — Monthly calendar with visual indicators (gold = validated, red = missed)
- **Offline-first** — Data cached locally via Room, syncs with API when online
- **Home screen widget** — Quick view of streak + one-tap validate button with emojis
- **Periodic sync** — WorkManager syncs data every 30 minutes in background
- **Light mode** — Clean beige/gold design matching chetana.dev portfolio

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      Android App                          │
│                                                           │
│  ┌─────────────┐   ┌──────────────┐   ┌──────────────┐  │
│  │ MainActivity │   │  Widget      │   │ SyncWorker   │  │
│  │              │   │  Provider    │   │ (WorkManager)│  │
│  └──────┬───────┘   └──────┬───────┘   └──────┬───────┘  │
│         │ observes          │ reads             │ syncs    │
│         ▼                   ▼                   ▼          │
│  ┌──────────────┐   ┌──────────────────────────────────┐  │
│  │ MainViewModel│   │       PushUpRepository            │  │
│  └──────┬───────┘   │  sync() → API + Room cache       │  │
│         │ calls     │  validateToday() → API + resync  │  │
│         ▼           └──────┬──────────────┬────────────┘  │
│  ┌──────────────┐          │              │               │
│  │  Repository  │          ▼              ▼               │
│  └──────────────┘   ┌──────────┐   ┌──────────────┐     │
│                      │  Room DB │   │RetrofitClient│     │
│                      │ (cache)  │   │+ Auth Interc.│     │
│                      └──────────┘   └──────┬───────┘     │
│                                             │             │
│  ┌──────────────────────────────────────────┘             │
│  │  Auth Flow                                             │
│  │  ┌─────────────┐   ┌────────────┐                     │
│  │  │GoogleAuthMgr│──▶│ TokenStore │                     │
│  │  │(Credential  │   │(SharedPrefs│                     │
│  │  │ Manager)    │   │ id_token)  │                     │
│  │  └─────────────┘   └────────────┘                     │
│  └────────────────────────────────────────────────────────│
└──────────────────────────┬────────────────────────────────┘
                           │ HTTPS + Bearer token
                           ▼
┌──────────────────────────────────────────────────────────┐
│                Backend (chetana.dev)                       │
│  Nuxt 3 / Nitro — Vercel Serverless                      │
│                                                           │
│  requireAuth() ──▶ google-auth-library verify             │
│       │                                                   │
│       ▼                                                   │
│  ┌────────────┐   ┌──────────────────────────────────┐   │
│  │ users      │   │ health_entries                    │   │
│  │ table      │◀──│ (user_id FK, scoped per user)    │   │
│  └────────────┘   └──────────────────────────────────┘   │
│                                                           │
│  Neon PostgreSQL (serverless) + Drizzle ORM              │
└──────────────────────────────────────────────────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Android Views + ViewBinding |
| Auth | Google Credential Manager + ID Token |
| Network | Retrofit 2.9 + OkHttp 4.12 + Auth Interceptor |
| Local DB | Room 2.6 (SQLite) |
| Architecture | MVVM (ViewModel + LiveData + Repository) |
| Background | WorkManager 2.9 |
| Widget | AppWidgetProvider + RemoteViews |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## Auth Flow

```
1. App launch → TokenStore.isLoggedIn()?
   ├── Yes → initApp() (ViewModel, observers, calendar)
   └── No  → GoogleAuthManager.signIn()
              ├── Success → TokenStore.saveToken() → initApp()
              └── Failure → Toast error

2. Every API call:
   OkHttp Interceptor → adds "Authorization: Bearer <idToken>"

3. On 401 response:
   Interceptor → TokenStore.clear() → next sync triggers re-sign-in

4. Sign-out:
   Long press on target text → GoogleAuthManager.signOut() → TokenStore.clear() → recreate()
```

## API

All endpoints require `Authorization: Bearer <google_id_token>` header.

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health/stats` | Dashboard stats (streak, totals, today status) |
| `GET` | `/api/health/entries` | All push-up entries scoped to authenticated user |
| `POST` | `/api/health/validate` | Validate today's push-ups `{ pushups: N }` |

## Project Structure

```
app/src/main/java/com/cyin/daily_push_up/
├── auth/
│   ├── GoogleAuthManager.kt    # Credential Manager Sign-In/Sign-Out
│   └── TokenStore.kt           # SharedPreferences token + user info
├── api/
│   ├── HealthApiService.kt     # Retrofit interface + DTOs
│   └── RetrofitClient.kt       # Retrofit singleton + auth interceptor
├── data/
│   ├── AppDatabase.kt          # Room database
│   ├── CachedStats.kt          # Room entity — cached stats
│   ├── PushUpDao.kt            # Room DAO
│   ├── PushUpEntry.kt          # Room entity — daily entries
│   └── PushUpRepository.kt     # Offline-first repository
├── widget/
│   ├── PushUpWidgetProvider.kt  # Home screen widget with emojis
│   └── SyncWorker.kt           # WorkManager periodic sync
├── MainActivity.kt              # Sign-in flow + full UI
├── MainViewModel.kt             # MVVM ViewModel
└── MyApplication.kt             # App init (Room + WorkManager + RetrofitClient)
```

## Build & Run

```bash
# Build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Wireless debugging
adb pair <ip>:<pair-port> <code>
adb connect <ip>:<port>
./gradlew installDebug
```

## Google Cloud Console Setup

1. Create OAuth Client ID type **Web application** → used as `serverClientId` in `GoogleAuthManager.kt` and `GOOGLE_CLIENT_ID` on backend
2. Create OAuth Client ID type **Android** with package `com.cyin.daily_push_up` + SHA-1 fingerprint
3. Set `GOOGLE_CLIENT_ID` env var in Vercel

## Documentation

- [Architecture](docs/architecture.md) — System architecture and data flow
- [Technical Choices](docs/technical-choices.md) — Why each technology was chosen
- [API Contract](docs/api-contract.md) — Full API specification

## License

Private project.
