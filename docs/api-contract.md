# API Contract

Base URL : `https://chetana.dev`

L'application communique avec trois endpoints REST sur l'API Health hebergee sur Vercel.

---

## GET `/api/health/stats`

Recupere les statistiques globales et le statut du jour.

### Response `200 OK`

```json
{
  "totalPushups": 4530,
  "totalDays": 142,
  "currentStreak": 12,
  "longestStreak": 45,
  "todayValidated": false,
  "todayTarget": 30
}
```

### Champs

| Champ | Type | Description |
|-------|------|-------------|
| `totalPushups` | `int` | Nombre total de push-ups depuis le debut |
| `totalDays` | `int` | Nombre de jours valides |
| `currentStreak` | `int` | Streak consecutive actuelle (en jours) |
| `longestStreak` | `int` | Meilleure streak historique |
| `todayValidated` | `boolean` | `true` si aujourd'hui est deja valide |
| `todayTarget` | `int` | Objectif de push-ups pour aujourd'hui |

### Mapping Room

```
StatsResponse  ──►  CachedStats (id=0, singleton)
```

---

## GET `/api/health/entries`

Recupere l'historique complet des entries push-ups.

### Response `200 OK`

```json
[
  {
    "id": "abc123",
    "date": "2026-02-21",
    "pushups": 30,
    "validated": true,
    "validatedAt": "2026-02-21T08:30:00.000Z",
    "createdAt": "2026-02-21T08:30:00.000Z"
  },
  {
    "id": "def456",
    "date": "2026-02-20",
    "pushups": 35,
    "validated": true,
    "validatedAt": "2026-02-20T07:15:00.000Z",
    "createdAt": "2026-02-20T07:15:00.000Z"
  }
]
```

### Champs

| Champ | Type | Description |
|-------|------|-------------|
| `id` | `string?` | Identifiant unique de l'entry |
| `date` | `string` | Date au format `YYYY-MM-DD` |
| `pushups` | `int` | Nombre de push-ups pour ce jour |
| `validated` | `boolean` | `true` si le jour est valide |
| `validatedAt` | `string?` | Timestamp ISO 8601 de la validation |
| `createdAt` | `string?` | Timestamp ISO 8601 de la creation |

### Mapping Room

```
List<EntryResponse>  ──►  List<PushUpEntry> (PK = date)
```

Les entries sont stockees avec `date` comme cle primaire (format `YYYY-MM-DD`). A chaque sync, la table est videe (`clearEntries()`) puis remplie avec les donnees fraiches.

---

## POST `/api/health/validate`

Valide les push-ups du jour.

### Request

```json
{
  "pushups": 30
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `pushups` | `int` | Nombre de push-ups a valider |

### Response `200 OK`

```json
{
  "success": true,
  "alreadyValidated": false,
  "date": "2026-02-21",
  "pushups": 30
}
```

### Champs

| Champ | Type | Description |
|-------|------|-------------|
| `success` | `boolean` | `true` si la validation a reussi |
| `alreadyValidated` | `boolean?` | `true` si le jour etait deja valide |
| `date` | `string?` | Date validee au format `YYYY-MM-DD` |
| `pushups` | `int?` | Nombre de push-ups valides |

### Comportement app

1. `POST /validate` avec le nombre de push-ups
2. Si `success == true` : declenche un `sync()` complet pour rafraichir stats + entries
3. L'UI se met a jour automatiquement via LiveData (stepper masque, badge "Done" affiche)

---

## Gestion des erreurs

L'app utilise `Result<T>` de Kotlin pour wrapper les appels reseau :

```kotlin
suspend fun sync(): Result<Unit>
suspend fun validateToday(pushups: Int): Result<Boolean>
```

| Scenario | Comportement |
|----------|-------------|
| API OK | Donnees ecrites en Room, UI mise a jour |
| API KO (timeout, 500, etc.) | `Result.failure()`, Toast d'erreur, donnees cachees affichees |
| Pas de connexion | Idem — l'app fonctionne en mode offline avec le cache |

---

## Flux de synchronisation

```
App Start / Pull-to-refresh / WorkManager (30 min)
    │
    ├── GET /api/health/stats
    │   └── ► Room: INSERT cached_stats (id=0, REPLACE)
    │
    └── GET /api/health/entries
        ├── ► Room: DELETE * FROM pushup_entries
        └── ► Room: INSERT entries (REPLACE)

    LiveData observers auto-notified
    └── UI refreshes
```
