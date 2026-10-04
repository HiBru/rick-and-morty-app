# Implementation Plan

> **Read this before starting any task.** The context is cleared after every commit, so this file — not a chat history — is where the project's decisions live. It records *why* each choice was made, so a fresh session does not re-derive or silently reverse it.
>
> Start a task by reading, in order: [CLAUDE.md](../CLAUDE.md) → [docs/SPEC.md](SPEC.md) → this file → the skills named in the task's row. Nothing else needs to carry over.

## Where we are

Tick the checkbox **in the same commit as the task's code**. That way `git log` and this list can never disagree, and "what's next" is the first unticked box.

### Task 0 — handoff document

- [x] **0.** This file + `CLAUDE.md` amendments (testing split, context rule, version pointer, staleness fixes)

### Foundation — verify with review + both-platform build + tests

- [x] **1.** Dependencies & Gradle wiring — *`android-module-structure`*. Detail below
- [x] **2.** Design system: tokens + `AppTheme` — *`android-compose-ui`*
- [ ] **3.** `core/domain`: `Result`/`Error`/`DataError`, models, **all four data-source + repository interfaces** — *`android-error-handling`, `android-domain-usecases`*. **`ThemeMode` already exists** — Task 2 needed it for `AppTheme(themeMode)`; do not duplicate it
- [ ] **4.** `core/ui`: `UiText`, `ObserveAsEvents`, `toUiText()`, string resources — *`android-presentation-mvi`, `android-error-handling`*
- [ ] **5.** `core/format`: `AppDateTimeManager` + platform formatters — *`android-date-time-manager`*
- [ ] **6.** `core/data`: Ktor client, `safeCall`, DTOs, mappers, `KtorCharacterRemoteDataSource` — *`android-data-layer`, `android-error-handling`*
- [ ] **7.** `core/database`: Room favorites + `RoomFavoriteCharacterLocalDataSource`. **Also verifies Task 1's per-target KSP wiring** — *`android-data-layer`*
- [ ] **8.** DataStore theme data source in `core/data`; theme use cases in `core/domain/usecase` — *`android-data-layer`, `android-domain-usecases`*
- [ ] **9.** `LocalFirstCharacterRepository` + `GetCharacterUseCase` — *`android-data-layer`, `android-domain-usecases`, `android-testing`*
- [ ] **10.** Koin modules + Android/iOS entry points + `App()` theme wiring + scaffold cleanup — *`android-di-koin`*

### UI and features — from Task 12 on, additionally operate the app on both devices

- [ ] **11.** Design-system components + previews — *`android-compose-ui`*
- [ ] **12.** Navigation shell + bottom bar (placeholder screens) — *`android-navigation`*
- [ ] **13.** Shared character row: favorite use cases, `CharacterUi`, `CharacterListItem` — *`android-compose-ui`, `android-presentation-mvi`*
- [ ] **14.** Home: `GetCharacterPageUseCase` + MVI model types — *`android-presentation-mvi`, `android-domain-usecases`*
- [ ] **15.** Home: assembler + tests — *`android-presentation-mvi`, `android-testing`*
- [ ] **16.** Home: ViewModel + tests — *`android-presentation-mvi`, `android-testing`*
- [ ] **17.** Home: screen + previews — *`android-compose-ui`, `android-presentation-mvi`*
- [ ] **18.** Detail: domain, MVI, assembler, ViewModel + tests — *`android-presentation-mvi`, `android-testing`*
- [ ] **19.** Detail: screen + previews — *`android-compose-ui`*
- [ ] **20.** Favorites: complete slice — *`android-presentation-mvi`, `android-compose-ui`, `android-testing`*
- [ ] **21.** Settings: complete slice — *`android-presentation-mvi`, `android-testing`*
- [ ] **22.** Polish & end-to-end acceptance pass

**Task 10 wires `App()` before `AppNavHost` exists (Task 12).** Task 10 therefore wraps a temporary placeholder composable in `AppTheme`; Task 12 replaces it with the real `AppNavHost`.

---

## Dependency set — do not bump without reading this

Versions were chosen **proven-over-newest**. "Proven" (**P**) means the reference project below runs it *and/or* the local Gradle cache holds it with real `iosarm64` + `iossimulatorarm64` artifacts. Several entries are deliberately *not* the latest release, and the reasons are not guessable from the version numbers.

| Library | Version | Why this one |
|---|---|---|
| Koin (via BOM) | `4.1.1` **P** | **Not 4.2.2.** 4.1.1 is proven running *with lifecycle 2.11.0*, the single likeliest breakage point in this set — Koin declares 2.9.6 and Gradle forces it up two minors. 4.2.2 is built against 2.9.6 as well, and has never been tested here |
| Ktor | `3.6.0` | **Not proven** — nothing on this machine has ever resolved Ktor, so an older pin would be no safer. Contemporaneous with Kotlin 2.4.20 |
| Room | `androidx.room:2.8.4` **P** + `androidx.sqlite:sqlite-bundled:2.7.0` **P** | The exact pair the reference project runs. **Not `androidx.room3:3.0.3`** — Room's current line moved to a new group with a new plugin id and a `room3 { }` block; nothing here has used it. Room and sqlite versions are coupled: check the `.module` before changing either independently |
| KSP | `2.3.10` **P** | Proven pair with Room 2.8.4 — though proven against the reference's Kotlin **2.4.10**, not our 2.4.20. Ships two fixes that are exactly our situation: Kotlin 2.4 default module names, and R-class resolution under AGP 9 built-in Kotlin. 2.3.12 exists; **do not change Room and KSP in the same commit.** KSP versions are no longer `<kotlin>-<ksp>` — there is no `2.4.20-x.y.z` to look for |
| DataStore | `1.2.1` **P** | Latest stable (1.3.0 is alpha-only). Use `datastore-preferences`, which really does publish iOS artifacts at this version — not only `-core` |
| Navigation | `org.jetbrains.androidx.navigation:navigation-compose:2.10.0-beta01` | **A beta, deliberately.** The stable 2.9.2 pins lifecycle 2.9.6 / savedstate 1.3.6, colliding with our 2.11.0 / 1.4.0. JetBrains' own CMP 1.12.1 dependency table prescribes 2.10.0-beta01, and CMP runs a compatibility check that flags non-aligned androidx versions |
| Coil | `3.6.3` + `coil-network-ktor3:3.6.3` | Requires CMP ≥ 1.12.0; we are on 1.12.1. (The reference pins 3.5.0 *because* it is on CMP 1.11.1 — that reason does not apply to us) |
| Serialization | `kotlinx-serialization-json:1.11.0` **P** | |
| Coroutines | `1.11.0` (core + test) | **Not proven** — the reference runs 1.10.2. Ktor 3.6.0 declares 1.11.0, and declaring it explicitly avoids a conflict with the scaffold's previously-resolved 1.9.0 |
| JUnit | `junit-jupiter:5.11.4` **P** + `junit-platform-launcher:1.11.4` **P** | **Not JUnit 6**, which is unified-versioned (the launcher would also be 6.x) and unverified on an AGP 9 host-test task |
| Turbine / AssertK | `1.2.1` **P** / `0.28.1` **P** | Latest of each; both multiplatform. AssertK 0.28.1 is from 2024 and nothing newer exists |
| kotlinx-datetime | **not declared — settled in Task 1** | `kotlin.time.Instant.parse` handles the API's ISO-8601 `created` field, needs no opt-in, and is proven on **both** platforms by `ToolchainCommonTest.stdlibInstantParsesTheApiTimestampFormat`. Formatting goes through platform formatters anyway. If some later task genuinely needs the library, declare `0.7.1` (already resolved transitively via `material3 1.12.0-alpha03`), **not** `0.8.0` |

AGP stays at **9.1.1** and `shared/build.gradle.kts` keeps its `kotlin { android { … } }` spelling. AGP 9.2.1 renamed that accessor to `androidLibrary { }`; the reference project uses the newer spelling, so its build files cannot be copied verbatim.

### Gradle wiring that is easy to get silently wrong

- **`gradle.properties`** needs three lines the scaffold lacks: `ksp.useKSP2=true`, `room.useKSP2=true`, `ksp.allow.all.target.configuration=true`. (KSP ≥ 2.3.0 may no longer need the first two, but they are part of the proven configuration and are harmless if redundant.)
- **Room's processor is wired per target.** `ksp(...)` is deprecated for KMP, and `kspAndroidMain` is *silently ignored* — Room generates nothing and the build still succeeds:
  ```kotlin
  add("kspAndroid", libs.room.compiler)
  add("kspIosArm64", libs.room.compiler)
  add("kspIosSimulatorArm64", libs.room.compiler)
  ```
- **JUnit Platform must be enabled on the task.** The KMP library plugin has no `testOptions` DSL, and `tasks.named("testAndroidHostTest")` fails to resolve at configuration time:
  ```kotlin
  tasks.withType<Test>().configureEach {
      if (name == "testAndroidHostTest") useJUnitPlatform()
  }
  ```
  Without it, JUnit5 tests are **skipped and the build reports success**. Gradle 9.8 also removed auto-injection of test-framework runtime deps, so `junit-platform-launcher` must be `runtimeOnly` or the task fails to start. `kotlin-test-junit5` in `androidHostTest` is what makes `commonTest`'s `kotlin.test` annotations run under JUnit Platform on the JVM side.
- **`androidHostTest` must always hold at least one directly `@org.junit.jupiter.api.Test`-annotated test.** It is the only thing that can detect `useJUnitPlatform()` regressing: `commonTest`'s `kotlin.test` tests keep running via `kotlin-test-junit5` either way, so nothing else would report zero discovery. `ToolchainJvmTest` fills that role until a real ViewModel test replaces it in Task 16 — **replace, do not delete.**
- **The Koin BOM does not reach the test source sets.** `platform(libs.koin.bom)` in `commonMain` constrains `androidMain`'s versionless `koin-android` (verified), but `commonTest` / `androidHostTest` do not extend `commonMainImplementation`. The first versionless Koin artifact added there — `koin-test` is the likely one — needs its own `implementation(project.dependencies.platform(libs.koin.bom))` or it fails with "Could not find io.insert-koin:koin-test".
- **No Compose UI-test artifact is wired yet.** `CLAUDE.md` describes `ComposeTestRule` and the robot pattern, but nothing declares the dependency and no task below owns adding it. If a UI test is wanted, wire it in the task that first needs one (Task 17 at the earliest).
- **`:shared:connectedAndroidDeviceTest` has no runner dependency.** `withDeviceTestBuilder` names `AndroidJUnitRunner`, but Task 1 removed the scaffold's `androidx.test` catalog entries as unused. An instrumented test therefore needs `androidx.test:runner` re-added plus an `androidDeviceTest` dependency block — or drop the device-test builder, since no device tests exist.
- **There is no typed `androidHostTest` source-set accessor.** `androidHostTest.dependencies { }` fails to compile the build script; use `getByName("androidHostTest").dependencies { }`. (`androidMain`, `commonMain`, `commonTest` and `iosMain` all do have accessors.)
- **`compose-ui-backhandler`** must be declared explicitly *if* `BackHandler` is ever used: `compose-ui` pulls it in transitively on iOS but not on Android, so such code compiles for iOS and breaks the Android build.
- Set `compose.resources { packageOfResClass = "de.shinz.rickandmortyshowcase.generated.resources" }`. `publicResClass` is unnecessary in a single module.
- Do **not** add `de.mannodermaus.android-junit5` — it drives the old `com.android.library` `testOptions` DSL, which this plugin does not have.

## Task 1 in detail

The one task whose content cannot be derived from the conventions. Files edited: `gradle/libs.versions.toml`, `gradle.properties`, root `build.gradle.kts` (plugin aliases, `apply false`), `shared/build.gradle.kts`.

**Plugins** — add to the catalog and to the root build with `apply false`, then apply in `:shared`:

| Alias | Id |
|---|---|
| `kotlinSerialization` | `org.jetbrains.kotlin.plugin.serialization` (version.ref `kotlin`) |
| `ksp` | `com.google.devtools.ksp` |
| `room` | `androidx.room` (optional — see the risk table) |

Without the serialization plugin, every DTO and every `@Serializable` route fails to compile. It is named in no skill.

**Artifacts by source set:**

| Source set | Artifacts |
|---|---|
| `commonMain` | `koin-core`, `koin-compose`, `koin-compose-viewmodel` (all three versionless, via `koin-bom` platform); `ktor-client-core`, `ktor-client-content-negotiation`, `ktor-serialization-kotlinx-json`, `ktor-client-logging`; `kotlinx-serialization-json`; `kotlinx-coroutines-core`; `androidx.room:room-runtime` (**`api`**, not `implementation`); `androidx.sqlite:sqlite-bundled`; `androidx.datastore:datastore-preferences`; `org.jetbrains.androidx.navigation:navigation-compose`; `io.coil-kt.coil3:coil-compose`; `io.coil-kt.coil3:coil-network-ktor3` |
| `androidMain` | `koin-android`; `ktor-client-okhttp` |
| `iosMain` | `ktor-client-darwin`. The source set already exists via the default hierarchy template (it holds `MainViewController.kt` and `Platform.ios.kt`) and is never declared in `shared/build.gradle.kts` — only its `dependencies { }` block is new |
| `commonTest` | `kotlin-test`; `assertk`; `turbine`; `kotlinx-coroutines-test`; `ktor-client-mock` |
| `androidHostTest` | `junit-jupiter`; `kotlin-test-junit5`; `runtimeOnly(junit-platform-launcher)` — **this dependency block does not exist yet and must be created** |

The reference project's catalog already carries a working Ktor block and bundle (`gradle/libs.versions.toml`, the `# Ktor (networking)` section) — the quickest correct source for those coordinates.

**Also in Task 1:** the catalog's four unused JUnit4/Espresso entries (`junit`, `kotlin-testJunit`, `androidx-testExt-junit`, `androidx-espresso-core`) are leftovers from the scaffold and nothing wires them. Remove them, or leave them with a comment saying why — do not leave the question open.

**Task 1 acceptance — a green build proves almost nothing here.** Three of this task's wirings fail *silently*. So Task 1 must also add one throwaway test in `commonTest` (`kotlin.test` + AssertK) and one in `androidHostTest` (JUnit5), and confirm from the test reports that **both actually ran with a non-zero test count**. A passing task with zero executed tests means `useJUnitPlatform()` is not wired. The Room per-target KSP wiring cannot be exercised until an entity exists — **Task 7 owns verifying it.**

## Local reference project

`/Users/Walnutz/coding/walnutz/app` is the same author's production KMP app on the same `compileSdk 37` / `minSdk 26` and the same `com.android.kotlin.multiplatform.library` plugin. **Mirror it rather than inventing.** Adapt, don't copy: its package names, palette and module split differ from ours.

> This is a path on one machine and will not exist elsewhere. If it is missing, these patterns still have to be written — no skill defines `ObserveAsEvents`, and Room-on-KMP is covered by no skill at all.

| What to take | From (paths relative to that project) |
|---|---|
| `ObserveAsEvents` | `core/ui/src/commonMain/.../core/ui/util/ObserveAsEvents.kt` — lifecycle-aware via `repeatOnLifecycle(STARTED)` + `Dispatchers.Main.immediate` |
| `UiText` + `asString()` | `core/ui/src/commonMain/.../core/ui/UiText.kt`. Its third variant `Joined` fits our "species · status" line — take it |
| `Result<D, E>` / `EmptyResult` | `core/model/src/commonMain/.../core/model/Result.kt` |
| Six-group token theme | `core/designsystem/src/commonMain/.../theme/` — dimension tokens hoisted to private top-level vals; typography `remember`ed inside composition because Compose Resources' `Font()` is itself `@Composable` |
| Room-on-KMP wiring | `build-logic/src/main/kotlin/walnutz.kmp.room.gradle.kts`, plus the `internal expect val platformDatabaseModule: Module` pattern in `core/database/src/{commonMain,androidMain,iosMain}/.../di/DatabaseModule*.kt` — the platform half is a **Koin module**, not an `expect fun` |
| JUnit5 setup | `build-logic/src/main/kotlin/walnutz.kmp.library.gradle.kts`. Its `MainDispatcherExtension` lives in `core/testing/src/`**`androidMain`**`/.../MainDispatcherExtension.kt` — production source, not a test source set, because it is consumed from another module's tests |
| Date/time manager | `core/format/src/{commonMain,androidMain,iosMain}/.../core/format/`. The class is `WalnutzDateTimeManager` (ours is `AppDateTimeManager`), and it depends on a `WalnutzLocale` expect/actual in the same package — take both or drop the locale parameter |

**Not there, write from scratch:** `safeCall` / `HttpClientFactory` (that project is offline-first and never consumed Ktor, though its catalog has the coordinates) and all pagination logic.

**Do not copy** its `:build-logic` module. Convention plugins exist to share config across modules; we are one flat `:shared` module, so its five plugins collapse into `shared/build.gradle.kts`.

**Do not copy** its one-argument `assemble(vmState)` assembler signature — see the locked decisions.

## Locked decisions

| Decision | Choice | Reason |
|---|---|---|
| Module style | Flat: one `:shared` module, layering by package | `android-module-structure` requires picking one style and holding it |
| Pagination | Hand-rolled buffer in `CharacterListViewModelState` | Paging 3's `PagingData` cannot be assembled into a `UiState`; it would travel beside the state as a second channel and the assembler test would stop covering the list |
| Assembler signature | `assemble(data, vmState)` + `combine(...)` | Matches `CLAUDE.md` and the MVI skill's naming table. **Diverges from the reference project**, which folds flows into `vmState` and uses `assemble(vmState)` |
| Tests | Hybrid — see `CLAUDE.md` | JUnit5 is JVM-only; putting everything in `androidHostTest` would leave `iosSimulatorArm64Test` green with zero tests |
| Navigation | One `NavController`, one `NavHost`; bottom bar hidden on the detail destination | Keeps "Central NavHost" literally true and gives tab state preservation via `saveState`/`restoreState` |
| Palette | Cool teal on slate | "Modern, elegant, restrained" per `SPEC.md`; neutral surfaces, one accent |
| Context | Cleared after every commit | This file is the handoff |

## Design system values

```
                LIGHT      DARK
background      #FAFAFA    #0F1113
surface         #FFFFFF    #181B1E
surfaceVariant  #F1F3F5    #21252A
onSurface       #1A1D20    #E8EAED
onSurfaceMuted  #5B636E    #9AA3AD
accent          #0F766E    #2DD4BF
onAccent        #FFFFFF    #0F1113
border          #E4E7EB    #2A2F35
statusAlive     #15803D    #4ADE80
statusDead      #B91C1C    #F87171
statusUnknown   #5B636E    #9AA3AD   (= onSurfaceMuted, deliberately)
```

**These four light values are not the first choices — do not "restore" them.** The originals failed WCAG AA and were corrected in Task 2 against measured ratios: `onSurfaceMuted #6B7280` was 4.35:1 on `surfaceVariant`, `statusAlive #16A34A` was 2.96:1, `statusDead #DC2626` was 4.34:1, and `statusUnknown`'s light/dark pair was transposed, leaving a light grey on white at 2.54:1. All 36 foreground/background pairs now pass 4.5:1, worst case 4.51:1. The status colours tint a label as well as a dot, so the bar is 4.5:1, not the 3:1 a non-text graphic would get.

- `AppSpacing` — scale steps `xs 4`, `sm 8`, `md 12`, `lg 16`, `xl 24` on a 4dp rhythm, plus the named `screenPadding 20`. `screenPadding` is a named token, not a sixth step, so the 5–6 step ceiling in `CLAUDE.md` is not yet reached
- `AppRadius` — `sm 8`, `md 12`, `lg 16`, `full 999`
- `AppSize` — `icon 24`, `iconSmall 18`, `avatar 64`, `statusDot 8`, `touchTarget 48`, `detailImage 220`
- `AppBorder` — `hairline 1`
- `AppTypography` — `displayTitle 28`, `screenTitle 22`, `cardTitle 17`, `body 15`, `caption 13`

**Only `colors` varies by theme**; the other five are hoisted to top-level `val`s. Typography is hoisted too because the app uses the system font — the reference project remembers its typography inside composition only because Compose Resources' `Font()` is `@Composable`, which does not apply here.

The same five styles are also mapped onto the Material 3 slots as `appMaterialTypography` and handed to `MaterialTheme(typography = …)`. Material components read `MaterialTheme.typography`, never `AppTheme.typography`, so without the mapping a dialog renders at stock M3 sizes and does not match the screen behind it.

**An unmapped slot is not a graceful fallback — it is stock Material purple or a stock size.** Finding them means checking which token each component actually reads, not assuming. The three that bit in Task 2: `secondaryContainer`/`onSecondaryContainer` is the `NavigationBar` selected pill (on screen for all three tabs), `surfaceContainerHigh` is every `AlertDialog` background (three screens per `SPEC.md`), and `headlineSmall` — not `headlineMedium` — is the `AlertDialog` **title**. Equally, do not override a slot that already fits: `labelLarge` mapped to `cardTitle` made every dialog button 21% larger than M3 intends, so the label slots are left stock.

> **Name trap:** `border` is both a *color* (`AppTheme.colors.border`) and a *group* (`AppTheme.border.hairline`). Same class of trap as `size` shadowing `DrawScope.size` — read the line twice before assuming which one a call site wants.

> **Silent failure to watch for:** every `staticCompositionLocalOf` has a default, so a token group dropped from `AppTheme`'s `CompositionLocalProvider` still compiles and still renders. For the five dimension groups the default *is* the value, so nothing breaks — but `LocalAppColors` defaults to `appLightColors`, which means a missing `provides` would leave dark mode silently light. No Compose test artifact is wired to catch this, so **Task 10's launch check must look at dark mode on both platforms**, not just that the app starts.

## Target structure

Under `shared/src/commonMain/kotlin/de/shinz/rickandmortyshowcase/`:

```
App.kt                      composition root: reads theme mode, wraps AppNavHost in AppTheme
core/
  designsystem/             6 token groups + AppTheme + components/ + components/previews/
  domain/                   Result, Error, DataError, model/, datasource/ + repository/
                            interfaces, usecase/ (theme — app-wide)
  ui/                       UiText, ObserveAsEvents, toUiText() mappers
  format/                   AppDateTimeManager + expect PlatformDateTimeFormatter
  data/                     BASE_URL, HttpClientFactory, safeCall, DTOs, mappers,
                            Ktor/Room/DataStore data sources, LocalFirstCharacterRepository
  database/                 AppDatabase, FavoriteCharacterEntity, dao
di/                         initKoin, appModule, coreDataModule, coreDatabaseModule
features/
  shared/                   ObserveFavoriteIds/Add/RemoveFavorite use cases,
                            CharacterUi + CharacterListItem (list and favorites share them)
  characterlist/ characterdetail/ favorites/ settings/
navigation/                 Routes, AppNavHost, AppBottomBar
```

### Data layer

| Interface (`core/domain/`) | Implementation | Error type |
|---|---|---|
| `CharacterRemoteDataSource` | `KtorCharacterRemoteDataSource` | `DataError.Network` |
| `FavoriteCharacterLocalDataSource` | `RoomFavoriteCharacterLocalDataSource` | `DataError.Local` |
| `ThemePreferencesDataSource` | `DataStoreThemePreferencesDataSource` | reads: `Flow<ThemeMode>`, cannot fail. writes: `EmptyResult<DataError.Local>` |
| `CharacterRepository` | `LocalFirstCharacterRepository` | `DataError` |

All four interfaces are created in **Task 3**; the implementations land in Tasks 6–9.

`CharacterRepository` exists **only** for `getCharacter(id)` — the one genuinely multi-source read (local row if present, else remote) that the detail screen needs. Everything else talks to a data source directly, because `CLAUDE.md` reserves the word "repository" for multi-source coordination.

Favorites persist every value the API returns. The `episode` url list is stored as a single JSON string column, serialized in the entity mapper — no `TypeConverter`.

### Use cases

- `core/domain/usecase/` — `ObserveThemeModeUseCase`, `SetThemeModeUseCase`. App-wide: the composition root and Settings both need them, so no feature owns them.
- `features/shared/domain/usecase/` — `ObserveFavoriteIdsUseCase`, `AddFavoriteUseCase`, `RemoveFavoriteUseCase`; three consumers each.
- Per feature — `GetCharacterPageUseCase`, `GetCharacterUseCase`, `ObserveFavoritesUseCase`.

Add and Remove are **separate** use cases, not a toggle: adding is immediate, removing opens a confirm dialog, so the ViewModel branches before calling either.

### ViewModel wiring

```kotlin
val uiState: StateFlow<XUiState> =
    combine(observeSomething().map(::XData), vmState, assembler::assemble)
        .stateIn(viewModelScope, SharingStarted.Eagerly,
                 assembler.assemble(XData.EMPTY, XViewModelState()))
```

- **List** — `ViewModelState` holds the paging buffer (`characters`, `nextPage`, `isLoadingNextPage`, `endReached`, `initialLoadError`) **and the pending-removal id**, because `SPEC.md` puts a confirm dialog on this screen too. The only `Flow` is `ObserveFavoriteIdsUseCase`, so `CharacterListData` wraps just `favoriteIds`, and the assembler marks rows favorite by set membership.
- **Detail** — reads `characterId` from `savedStateHandle.toRoute<CharacterDetailRoute>()`, loads once in `init`; `ViewModelState` also holds the pending-removal flag for its own confirm dialog.
- **Favorites** — `ObserveFavoritesUseCase` → `FavoritesData(characters)`; `ViewModelState` holds only the pending-removal id.
- **Settings** — `ObserveThemeModeUseCase` → `SettingsData(themeMode)`.

## Four sanctioned conventions

These are deliberate and already reviewed. **A review agent must not flag them.**

1. **Stateful errors are mapped by the assembler.** `CLAUDE.md`'s "error → `UiText` at emission" rule applies to *events*. A retryable error that is part of rendered state is held as `DataError?` in `ViewModelState` and mapped by the assembler via `toUiText()` — which is pure and imports no Compose.
2. **`AppStatusDot(status: CharacterStatus)`** in `core/designsystem/components/` owns the status→color mapping. The assembler must not import Compose, and a screen must not branch on data — so the branch lives inside a design-system atom. This makes `core/designsystem` depend on `core/domain`, which is allowed: every layer may read `core/domain`.
3. **Bottom-bar visibility derives from the nav back stack**, not from a `UiState`. The navigation shell has no ViewModel and no assembler; it sits outside MVI.
4. **`App()` touches a use case directly** via `koinInject<ObserveThemeModeUseCase>()`. It is the composition root, not a screen — no screen state is involved. **Not to be copied into a screen.**

## Scaffold leftovers to remove

The scaffold contains three things that contradict `CLAUDE.md`. **Task 10 removes all three:**

1. `androidApp/.../MainActivity.kt` — the `@Preview fun AppAndroidPreview()`. `CLAUDE.md` says `androidApp` holds no UI and the root gets no preview.
2. `shared/.../App.kt` — the `@Preview` on `App()` itself. The composition root is not previewable.
3. `androidApp/src/main/AndroidManifest.xml` — `android:theme="@android:style/Theme.Material.Light.NoActionBar"` is a hardcoded **light** platform theme and will flash light on a dark-mode cold start. Replace with a theme that follows the system.

## Known risks and their fallbacks

| Risk | Fallback |
|---|---|
| Koin 4.1.1 forced up to lifecycle 2.11.0 (it declares 2.9.6) | Proven in the reference project with the same forced upgrade. If it still breaks, Koin 4.2.2 is the only move — but it is a coin-flip, not a known-good fallback: it is built against 2.9.6 too. Lifecycle cannot be downgraded, since CMP 1.12.1 requires 2.11.0 |
| `SavedStateHandle` injection into a `koinViewModel()` on iOS requires the `ViewModelStoreOwner` to be a `NavBackStackEntry` | Ours is — the detail ViewModel is resolved inside `composable<CharacterDetailRoute>`. If it still fails: read the route in the Root via `backStackEntry.toRoute()` and pass the id with `koinViewModel { parametersOf(id) }` |
| ~~Room Gradle plugin vs AGP 9.1.1~~ — **applies and configures cleanly** (Task 1: `:shared:copyRoomSchemas` runs as `NO-SOURCE`, and both `kspAndroidMain` and `kspKotlinIosSimulatorArm64` execute). Codegen itself is still unproven until an entity exists | If Task 7 finds it broken, the plugin is optional — drop it and pass `room.schemaLocation` as a KSP argument |
| `navigation-compose:2.10.0-beta01` is a beta | It is what JetBrains prescribe for CMP 1.12.1; the stable release conflicts with lifecycle 2.11.0. No better option exists |
| ~~AssertK 0.28.1 predates Kotlin 2.x native~~ — **resolved and runs on iOS**, proven in Task 1 by `ToolchainCommonTest` executing 3 tests under `iosSimulatorArm64Test`. Turbine likewise | — |
| ~~Ktor has never resolved on this machine~~ — **3.6.0 resolves**, Task 1 built both platforms with it on the classpath. Nothing exercises a request yet; Task 6 does | — |
| A fresh context re-litigates a settled decision — bumps a dependency, splits the module, swaps the assembler signature | This file exists for that: every such decision is recorded with its *reason*, not just its value |
| `xcodebuild -scheme iosApp` relies on a scheme stored under `iosApp/iosApp.xcodeproj/xcuserdata/`, which `.gitignore` excludes | It works on this machine. From a clean clone, either share the scheme in Xcode (writing it to `xcshareddata`, which **is** tracked) or open the project in Xcode once to regenerate it |

## Verification

**Per task:** implement → independent review agent → the commands below → commit (code **and** ticked checkbox) → clear the context.

The review agent must be pointed at `CLAUDE.md`, `docs/SPEC.md` **and this file** — the four sanctioned conventions above live here, and a reviewer who has not read them will flag them every task.

```bash
./gradlew :androidApp:assembleDebug
./gradlew :shared:compileKotlinIosSimulatorArm64
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test
```

For tasks 1–11 the implementing session runs these itself; the independent agent does the review. **From Task 12 on** — the first task with navigable UI — an independent agent additionally runs and *operates* the app on both platforms:

```bash
./gradlew :androidApp:installDebug
~/Library/Android/sdk/platform-tools/adb shell am start -n de.shinz.rickandmortyshowcase/.MainActivity
xcrun simctl boot "iPhone 17"
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' build
```

Task 10 is the first task that renders anything, so it includes a one-off launch check on both platforms — the app starts and the theme applies — without the full operate-the-UI pass.

`adb` is not on `PATH`. The emulator AVD is `Pixel_10_Pro_XL`; Xcode is 26.4 and the scheme is `iosApp`.

### End-to-end acceptance (Task 22) — on both platforms

1. Home loads 20 characters; scrolling appends further pages; paging stops at the last page.
2. Airplane mode on the first load shows the error state; retry recovers.
3. Favoriting on Home appears immediately in Favorites and on the detail screen.
4. Removing a favorite — from Home, from Favorites, and from Detail — always asks first; cancelling changes nothing.
5. The Favorites empty state appears when the last favorite is removed.
6. Detail opened from Favorites with the network off still renders fully (local-first).
7. Detail opened from Home for a non-favorite loads from the API.
8. Theme Dark/Light applies immediately and survives an app restart; System follows the device.
9. Tab switches preserve each tab's scroll position.
10. All copy is English; no hardcoded `dp`, `sp`, color or `FontWeight` survives review.
