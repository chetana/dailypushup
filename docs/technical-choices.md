# Choix techniques

Ce document detaille les decisions techniques prises pour le projet et les raisons derriere chaque choix.

---

## 1. Architecture : MVVM + Repository

**Choix** : Model-View-ViewModel avec pattern Repository

**Alternatives envisagees** :
- MVI (Model-View-Intent)
- MVP (Model-View-Presenter)

**Raisons** :
- MVVM est le pattern recommande par Google pour Android
- `LiveData` gere automatiquement le cycle de vie (pas de memory leaks)
- Le Repository abstrait la source de donnees (API vs cache) de facon transparente
- Single Activity = pas besoin de navigation complexe, l'app est simple et directe

---

## 2. Reseau : Retrofit 2.9 + OkHttp 4.12

**Choix** : Retrofit avec Gson converter

**Alternatives envisagees** :
- Ktor Client
- Volley
- HttpURLConnection natif

**Raisons** :
- Retrofit est le standard de facto pour les appels REST en Android
- Interface declarative (annotations `@GET`, `@POST`) = code minimal et lisible
- Support natif des coroutines Kotlin (`suspend fun`)
- OkHttp logging interceptor facilite le debug en developpement
- Gson converter pour la serialisation JSON automatique des DTOs

---

## 3. Base de donnees locale : Room 2.6

**Choix** : Room (abstraction SQLite)

**Alternatives envisagees** :
- SQLDelight
- Realm
- SharedPreferences (pour les stats uniquement)
- DataStore

**Raisons** :
- Room est la solution officielle de Jetpack, bien integree avec LiveData
- Verification des requetes SQL a la compilation via KSP
- `LiveData<CachedStats?>` permet une UI reactive sans polling
- Deux tables simples (`pushup_entries` + `cached_stats`) suffisent
- `fallbackToDestructiveMigration()` : les donnees sont un cache de l'API, pas une source de verite — perdre le cache n'est pas grave

---

## 4. UI : Views (XML) et non Jetpack Compose

**Choix** : Android Views classiques avec ViewBinding

**Alternatives envisagees** :
- Jetpack Compose

**Raisons** :
- L'UI est simple (une seule activite, pas de navigation complexe)
- Le calendrier custom est construit dynamiquement via `GridLayout` — facile avec Views
- ViewBinding genere du code type-safe sans overhead
- Pas de dependance supplementaire (Compose ajouterait ~2-3 MB a l'APK)

---

## 5. Calendrier : Implementation custom (GridLayout)

**Choix** : Calendrier construit dynamiquement avec `GridLayout`

**Alternatives envisagees** :
- `MaterialCalendarView` (prolificinteractive) — utilise dans l'ancienne version
- `CalendarView` natif Android
- Librairie Kizitonwose CalendarView

**Raisons** :
- Controle total sur l'apparence (gold/red/dimmed cells, check/cross icons)
- Fidelite au design du site web chetana.dev
- Pas de dependance externe = moins de maintenance
- `MaterialCalendarView` n'est plus maintenue et utilisait JitPack

---

## 6. Theme : Dark-only avec accent dore

**Choix** : `Theme.MaterialComponents.NoActionBar` avec palette dark/gold

**Palette** :
| Token | Couleur | Usage |
|-------|---------|-------|
| `bg_dark` | `#0A0A0A` | Fond principal |
| `bg_card` | `#1A1A1A` | Fond des cartes |
| `bg_card_elevated` | `#252525` | Boutons stepper |
| `gold_accent` | `#C4963C` | Accent principal, nombres, streak |
| `missed_red` | `#DC3C3C` | Jours manques |
| `done_blue` | `#1A7FB5` | Badge "Done" |
| `text_primary` | `#FFFFFF` | Texte principal |
| `text_secondary` | `#B3FFFFFF` | Texte secondaire (70% alpha) |
| `text_muted` | `#66FFFFFF` | Labels, jours futurs (40% alpha) |

**Raisons** :
- Coherence avec le site web chetana.dev (dark theme, meme accent dore)
- Un seul theme (pas de mode clair) = simplicite, meme theme dans `values/` et `values-night/`
- `NoActionBar` car l'app n'a pas besoin de toolbar — le titre est integre au layout

---

## 7. Widget : AppWidgetProvider + RemoteViews

**Choix** : Widget classique Android

**Alternatives envisagees** :
- Glance (Jetpack Compose pour widgets)

**Raisons** :
- Glance est encore en alpha/beta — moins stable
- RemoteViews est eprouve et supporte par toutes les versions Android
- Le widget est tres simple (3 textes + 1 bouton)
- Mise a jour via `PendingIntent` broadcast pour l'action "Validate"

---

## 8. Synchronisation background : WorkManager 2.9

**Choix** : `PeriodicWorkRequest` toutes les 30 minutes

**Alternatives envisagees** :
- `AlarmManager`
- `JobScheduler`
- Firebase Cloud Messaging (push)

**Raisons** :
- WorkManager est la solution recommandee pour le travail en arriere-plan
- Gere automatiquement les contraintes (Doze mode, App Standby)
- Garantit l'execution meme apres un redemarrage
- 30 minutes = bon compromis entre fraicheur des donnees et batterie
- Le worker met aussi a jour le widget apres chaque sync

---

## 9. Serialisation : Gson

**Choix** : Gson via `converter-gson`

**Alternatives envisagees** :
- Moshi
- Kotlinx.serialization

**Raisons** :
- Integration native avec Retrofit (`GsonConverterFactory`)
- Les DTOs sont de simples data classes — pas besoin de features avancees
- Zero configuration requise, fonctionne out-of-the-box

---

## 10. Strategie de cache : Offline-first destructive

**Choix** : Cache Room comme miroir de l'API, avec `fallbackToDestructiveMigration()`

**Raisons** :
- L'API est la source de verite unique
- Le cache Room sert uniquement a afficher des donnees quand l'API est injoignable
- En cas de changement de schema, detruire le cache et re-sync est acceptable
- Pas besoin de migrations complexes pour un simple cache

---

## 11. Build : KSP (et non KAPT)

**Choix** : Kotlin Symbol Processing pour Room

**Alternatives envisagees** :
- KAPT (Kotlin Annotation Processing Tool)

**Raisons** :
- KSP est jusqu'a 2x plus rapide que KAPT
- Recommande par Google pour Room depuis la version 2.5+
- Meilleure compatibilite avec les futures versions de Kotlin

---

## Dependances completes

```kotlin
// Core Android
androidx.core:core-ktx
androidx.appcompat:appcompat
com.google.android.material:material
androidx.constraintlayout:constraintlayout
androidx.lifecycle:lifecycle-livedata-ktx
androidx.lifecycle:lifecycle-viewmodel-ktx

// Database
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1
androidx.room:room-compiler:2.6.1 (KSP)

// Network
com.squareup.retrofit2:retrofit:2.9.0
com.squareup.retrofit2:converter-gson:2.9.0
com.squareup.okhttp3:logging-interceptor:4.12.0

// Background
androidx.work:work-runtime-ktx:2.9.0

// UI
androidx.swiperefreshlayout:swiperefreshlayout:1.1.0
```

Aucune dependance tierce non-Google/Square. Stack 100% mainline Android.
