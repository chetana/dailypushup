# Daily Push-Up

Android companion app for [chetana.dev](https://chetana.dev) — track daily push-ups with a dark/gold UI, offline-first caching, and a home screen widget.

## Features

- **Stats dashboard** — Current streak, total push-ups, days completed, best streak
- **Daily validation** — Stepper to set push-up count and validate for the day
- **Calendar view** — Monthly calendar with visual indicators (gold = validated, red = missed, gold ring = today)
- **Offline-first** — Data cached locally via Room, syncs with API when online
- **Home screen widget** — Quick view of streak + one-tap validate button
- **Periodic sync** — WorkManager syncs data every 30 minutes in background

## Screenshots

| Dashboard | Calendar |
|-----------|----------|
| Dark theme, gold accent (#C4963C), stats grid + stepper | Monthly view with day-by-day status |

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Android Views + ViewBinding |
| Network | Retrofit 2.9 + OkHttp 4.12 |
| Local DB | Room 2.6 (SQLite) |
| Architecture | MVVM (ViewModel + LiveData + Repository) |
| Background | WorkManager 2.9 |
| Widget | AppWidgetProvider + RemoteViews |
| Min SDK | 24 (Android 7.0) |
| Target SDK | 36 |

## API

The app connects to three endpoints on `https://chetana.dev`:

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/health/stats` | Dashboard stats (streak, totals, today status) |
| `GET` | `/api/health/entries` | All push-up entries (date, count, validated) |
| `POST` | `/api/health/validate` | Validate today's push-ups `{ pushups: N }` |

## Project Structure

```
app/src/main/java/com/cyin/daily_push_up/
├── api/
│   ├── HealthApiService.kt    # Retrofit interface + DTOs
│   └── RetrofitClient.kt      # Retrofit singleton
├── data/
│   ├── AppDatabase.kt         # Room database (v2)
│   ├── CachedStats.kt         # Room entity — cached stats
│   ├── PushUpDao.kt           # Room DAO
│   ├── PushUpEntry.kt         # Room entity — daily entries
│   └── PushUpRepository.kt    # Offline-first repository
├── widget/
│   ├── PushUpWidgetProvider.kt # Home screen widget
│   └── SyncWorker.kt          # WorkManager periodic sync
├── MainActivity.kt             # Single activity — full UI
├── MainViewModel.kt            # MVVM ViewModel
└── MyApplication.kt            # App init (Room + WorkManager)
```

## Build & Run

```bash
# Build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Wireless debugging (pair first, then connect)
adb pair <ip>:<pair-port> <code>
adb connect <ip>:<port>
./gradlew installDebug
```

## Documentation

Detailed technical documentation is available in the [`docs/`](docs/) folder:

- [Architecture](docs/architecture.md) — System architecture and data flow
- [Technical Choices](docs/technical-choices.md) — Why each technology was chosen
- [API Contract](docs/api-contract.md) — Full API specification

## License

Private project.
