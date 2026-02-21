# API Contract

Base URL : `https://chetana.dev`

## Authentification

Tous les endpoints necessitent un header d'authentification :

```
Authorization: Bearer <google_id_token>
```

Le backend verifie le token avec `google-auth-library`, extrait l'email et le Google ID, et upsert l'utilisateur dans la table `users`. Toutes les requetes sont scopees au `userId` de l'utilisateur authentifie.

### Reponses d'erreur auth

| Code | Scenario |
|------|----------|
| `401` | Token absent, invalide ou expire |

L'app Android intercepte les 401 via l'OkHttp interceptor, clear le token, et relance le sign-in au prochain cycle.

---

## GET `/api/health/stats`

Recupere les statistiques de l'utilisateur authentifie et le statut du jour.

### Response `200 OK`

```json
{
  "totalPushups": 1080,
  "totalDays": 52,
  "currentStreak": 52,
  "longestStreak": 52,
  "todayValidated": true,
  "todayTarget": 25
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

Recupere l'historique complet des entries push-ups de l'utilisateur authentifie.

### Response `200 OK`

```json
[
  {
    "id": 157,
    "userId": 1,
    "date": "2026-02-21",
    "pushups": 25,
    "validated": true,
    "validatedAt": "2026-02-21T08:30:00.000Z",
    "createdAt": "2026-02-21T08:30:00.000Z"
  }
]
```

### Champs

| Champ | Type | Description |
|-------|------|-------------|
| `id` | `int` | Identifiant unique de l'entry |
| `userId` | `int` | FK vers la table users |
| `date` | `string` | Date au format `YYYY-MM-DD` |
| `pushups` | `int` | Nombre de push-ups pour ce jour |
| `validated` | `boolean` | `true` si le jour est valide |
| `validatedAt` | `string?` | Timestamp ISO 8601 de la validation |
| `createdAt` | `string?` | Timestamp ISO 8601 de la creation |

### Mapping Room

```
List<EntryResponse>  ──►  List<PushUpEntry> (PK = date)
```

---

## POST `/api/health/validate`

Valide les push-ups du jour pour l'utilisateur authentifie.

### Request

```json
{
  "pushups": 25
}
```

| Champ | Type | Description |
|-------|------|-------------|
| `pushups` | `int` | Nombre de push-ups a valider (1-200) |

### Response `200 OK`

```json
{
  "success": true,
  "alreadyValidated": false,
  "date": "2026-02-21",
  "pushups": 25
}
```

### Champs

| Champ | Type | Description |
|-------|------|-------------|
| `success` | `boolean` | `true` si la validation a reussi |
| `alreadyValidated` | `boolean?` | `true` si le jour etait deja valide |
| `date` | `string?` | Date validee au format `YYYY-MM-DD` |
| `pushups` | `int?` | Nombre de push-ups valides |

### Comportement

- Si l'entry du jour existe deja et est validee : retourne `alreadyValidated: true` sans modifier
- Si l'entry existe mais n'est pas validee : met a jour avec `validated: true`
- Si aucune entry : cree une nouvelle entry avec `userId` de l'utilisateur authentifie
- Contrainte unique `(user_id, date)` en base pour eviter les doublons

---

## Schema de base de donnees backend

### Table `users`

| Colonne | Type | Notes |
|---------|------|-------|
| id | serial PK | |
| email | varchar unique | Google email |
| name | varchar | Google display name |
| picture | text | Google profile picture URL |
| google_id | varchar unique | Google sub (unique identifier) |
| created_at | timestamp | |
| last_login_at | timestamp | Updated on each API call |

### Table `health_entries`

| Colonne | Type | Notes |
|---------|------|-------|
| id | serial PK | |
| user_id | integer FK → users.id | Nullable (migration compat) |
| date | varchar | Format `YYYY-MM-DD` |
| pushups | integer | |
| validated | boolean | |
| validated_at | timestamp | |
| created_at | timestamp | |
| | | **Unique constraint**: `(user_id, date)` |

---

## Gestion des erreurs

| Scenario | Comportement |
|----------|-------------|
| API OK | Donnees ecrites en Room, UI mise a jour |
| 401 Unauthorized | Token clear, re-sign-in au prochain cycle |
| API KO (timeout, 500) | `Result.failure()`, Toast d'erreur, donnees cachees affichees |
| Pas de connexion | Mode offline avec le cache Room |

---

## Flux de synchronisation

```
App Start / Pull-to-refresh / WorkManager (30 min)
    │
    │  Authorization: Bearer <idToken>
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
