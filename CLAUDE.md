# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`RickAndMortyShowcase` — a Kotlin Multiplatform app targeting Android and iOS, with the UI shared via Compose Multiplatform. Package root: `de.shinz.rickandmortyshowcase`.

**Screen-by-screen behaviour lives in [docs/SPEC.md](docs/SPEC.md) — read it before implementing a feature.**

The repo is currently a scaffold: `App.kt` is an empty composable, and the `Platform`/test classes are stubs. Most feature work means creating new structure rather than editing existing code.

## Commands

```bash
# Build / run
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug        # to a connected device/emulator
# iOS: open ./iosApp in Xcode and run (Gradle builds the Shared framework as a dependency)

# Tests
./gradlew :shared:allTests                                   # all targets
./gradlew :shared:testAndroidHostTest                        # JVM/host tests (commonTest + androidHostTest)
./gradlew :shared:iosSimulatorArm64Test                      # iOS tests (commonTest + iosTest)
./gradlew :shared:testAndroidHostTest --tests "de.shinz.rickandmortyshowcase.SharedCommonTest"
./gradlew :shared:connectedAndroidDeviceTest                 # instrumented, needs a device

# Lint / verification
./gradlew :androidApp:lint        # or lintFix to auto-apply safe suggestions
./gradlew check                   # all checks across modules
```

Gradle 9.8.0 via the wrapper, JDK 21 toolchain (auto-provisioned per `gradle/gradle-daemon-jvm.properties`), `jvmTarget = 11`. Configuration cache and build cache are both on, so avoid build-script patterns that break configuration caching.

`adb` is **not** on `PATH` — use `~/Library/Android/sdk/platform-tools/adb`.

## Tech Stack

| Concern | Choice |
|---|---|
| API | Rick and Morty API — base `https://rickandmortyapi.com/api`, characters at `/character`, 20 per page via `?page=N`, envelope `{ info: { count, pages, next, prev }, results: [...] }` |
| Networking | Ktor Client + KotlinX Serialization |
| DI | Koin |
| Local DB | Room — favorites |
| Preferences | DataStore — theme mode |
| Navigation | Compose Navigation, type-safe `@Serializable` routes |
| Images | Coil |
| Testing | JUnit5, AssertK, Turbine, `kotlinx-coroutines-test` |

**No versions here** — they belong in `gradle/libs.versions.toml`, and no dependency coordinate may be hardcoded in a `build.gradle.kts`.

**The app is English only.** Every user-facing string is a Compose Resources entry (`Res.string.…`). No German in code, strings, comments, or UI.

**Theme:** dark / light / system, persisted in DataStore.

## Architecture

Three Gradle projects, but only two are in `settings.gradle.kts` — `iosApp` is an Xcode project, not a Gradle module.

- **`:shared`** — all shared code *and* all UI. Compose Multiplatform lives here; `App.kt` in `commonMain` is the single root composable both platforms render.
- **`:androidApp`** — thin `com.android.application` host. `MainActivity` only does `enableEdgeToEdge()` + `setContent { App() }`.
- **`iosApp/`** — thin SwiftUI host. `ContentView.swift` wraps `MainViewControllerKt.MainViewController()` (from `shared/src/iosMain`) in a `UIViewControllerRepresentable`.

**Put UI in `shared/commonMain`, not in `androidApp`.** Platform hosts exist only to bootstrap `App()`; adding screens to `androidApp` breaks the iOS app silently.

All feature code stays in `commonMain` and KMP-compatible. `androidMain` / `iosMain` hold `expect`/`actual` halves only — see `Platform.kt` with `Platform.android.kt` / `Platform.ios.kt` as the pattern.

### Module style: flat

`android-module-structure` offers two styles — Gradle submodules per layer, or **one module layered by package** — and requires picking one and holding it. **This project is flat: one `:shared` module.** The dependency rules are identical; they are enforced by review rather than by the build. Do not introduce `:core:*` / `:feature:*` Gradle modules.

Layout under `shared/src/commonMain/kotlin/de/shinz/rickandmortyshowcase/`:

```
core/
    designsystem/      AppTheme, AppColors, AppSpacing, AppRadius, AppSize, AppBorder, AppTypography
    domain/            Result, Error, DataError, Character, repository + data-source interfaces
    data/              HttpClientFactory, safeCall helpers, DTOs, mappers, Ktor/Room data sources
    database/          @Database, entities, DAOs
    ui/                UiText, ObserveAsEvents, toUiText() mappers
di/
    AppModule.kt       appModule (grouped by feature) + coreDataModule, coreDatabaseModule
features/
    characterlist/     domain/usecase + presentation
    characterdetail/
    favorites/
    settings/
    shared/            sibling of the features — only what 2+ features inject
navigation/            Routes, AppNavHost, the Dashboard shell
```

Three placement decisions and why:

1. **There is no `features/dashboard/`.** The dashboard has no ViewModel — it is a `Scaffold` + `BottomNavigationBar` + nested `NavHost`. A screen without its own ViewModel is not a feature, so the shell lives in `navigation/`.
2. **`Character`, the repository interface and the data-source interfaces live in `core/domain/`**, not in a feature. All three list/detail features consume them, and `core:domain` is defined as the home for shared domain models, repository interfaces, error types and `Result`.
3. **`App()` is the composition root, not a screen.** It reads the theme mode via `koinInject<ObserveThemeModeUseCase>()` + `collectAsStateWithLifecycle()` and wraps `AppNavHost` in `AppTheme`. This is the *only* sanctioned place a composable touches a use case directly — there is no screen state involved, and the six-piece MVI ceremony for a single enum is not worth it. **Do not copy this pattern into a screen.**

### Layering

`presentation → domain ← data`. Domain depends on nothing.

- **A ViewModel depends on use cases, never on a repository**, DAO, DataStore or network client.
- **A use case depends on repository/data-source interfaces only** — never on another use case, never on anything in `presentation`.
- Promote shared code only on the second consumer: one feature → its own package; two features → `features/shared/`; app-wide → `core/`.

### Navigation

**Central NavHost** (the skill's two layouts; central fits up to ~15 destinations, this app has 2 top-level + 3 tabs). All routes live in one file in `navigation/`, and the `NavHost` holds every `composable<…>` directly.

`@Serializable data object` for parameterless screens, `@Serializable data class` for screens with arguments. **Routes carry ids, never objects** — the destination loads its own data.

### Build-system specifics that trip people up

- `:shared` uses AGP 9's **`com.android.kotlin.multiplatform.library`** plugin, not the classic `com.android.library`. Consequences: Android config goes inside the `kotlin { android { … } }` block, and the source sets are `androidMain` / `androidHostTest` / `androidDeviceTest` — **not** `src/main`, `src/test`, `src/androidTest`.
- iOS targets are `iosArm64` and `iosSimulatorArm64` only. There is no `iosX64`, so Intel simulators are unsupported.
- The iOS framework is named `Shared` and is static — Swift code does `import Shared`. Renaming `baseName` in `shared/build.gradle.kts` requires updating the Swift imports and the Xcode project.
- iOS signing/naming comes from `iosApp/Configuration/Config.xcconfig`. `TEAM_ID` is empty; it must be set for device builds.

## Design System

Lives in `core/designsystem/` and owns **every** design value in the app. There is no reference design or prototype — the design system *is* the source of truth. Visual direction: modern, elegant, restrained.

Pattern per group: `@Immutable data class App<Group>` → `val LocalApp<Group> = staticCompositionLocalOf { App<Group>() }` → read as `AppTheme.<group>`. The `AppTheme { }` wrapper provides all six; only colors and typography vary by theme, so hoist the other four to top-level `val`s instead of allocating per composition.

**The six groups are fixed:** `colors`, `spacing`, `radius`, `size`, `border`, `typography`. There is no `FontSize` group.

- `spacing` holds gaps and paddings **only** — 5–6 steps on a 4dp rhythm. Snap to the nearest existing step rather than adding a near-duplicate. Radii, component sizes and stroke widths are separate groups so a change to one cannot silently move another.
- `typography` carries the type scale: **4–5 named `TextStyle`s**. Reuse a Material 3 slot when the recipe is within ~1sp at the same weight; add a named style only when nothing fits.

**No composable contains a literal `dp`, `sp`, color or `FontWeight`.** No `fontSize =` or `fontWeight =` override at a call site — that means the style is missing from the design system. If a value has no token, **add the token**; do not inline it.

Two mechanical traps:

- `size` shadows `DrawScope.size` — alias it: `val sizes = AppTheme.size`.
- Draw lambdas are not composable — read tokens into locals *before* the modifier chain, not inside `Modifier.drawBehind { }`.

**`AppTheme` must default to following the system setting.** `@PreviewLightDark` resolves the preview's `uiMode`, so no preview may pass `themeMode` explicitly and no `…DarkPreview` twin is ever written.

## Conventions

The `android-*` skills (`android-module-structure`, `android-presentation-mvi`, `android-data-layer`, `android-domain-usecases`, `android-di-koin`, `android-navigation`, `android-error-handling`, `android-compose-ui`, `android-testing`) are the authoritative source. **Load the relevant one before creating a ViewModel, repository, use case, DI module, route, screen or test.** What follows pins the project-level choices so they are not re-derived each session.

### MVI

Six pieces per screen, plus the composable split:

| Piece | Owns |
|---|---|
| `<Screen>UiState` | Everything the composable renders — already decided, already formatted |
| `<Screen>ViewModelState` | Transient state only the screen knows: form fields, open dialogs, in-flight flags |
| `<Screen>UiAction` | Every user-triggered action, as a `sealed interface`, members prefixed `On` |
| `<Screen>UiEvent` | One-time side effects (navigation, snackbar), as a `sealed interface` |
| `<Screen>UiStateAssembler` | A pure mapper: domain data + `ViewModelState` → `UiState` |
| `<Screen>ViewModel` | Holds `StateFlow<UiState>`, processes `UiAction`, emits `UiEvent` |
| `<Screen>Root` / `<Screen>Screen` | Both in one file named after the **Screen** |

UI models are suffixed `Ui` (`CharacterUi`). The exposed state property is `uiState`, never `state`.

The rules that actually get violated:

- **No derived `get()` properties on `UiState`.** The assembler computes plain fields, where they can be tested.
- The ViewModel **never** holds a `MutableStateFlow<UiState>`. Only the assembler builds a `UiState`.
- `assemble(data, vmState)` is pure: no `suspend`, no `Flow`, no coroutine, no repository, no use case, no `SavedStateHandle`. Only synchronous pure formatters may be constructor dependencies.
- Wiring: `combine(useCase(), vmState, assembler::assemble).stateIn(viewModelScope, SharingStarted.Eagerly, assembler.assemble(<X>Data.EMPTY, <X>ViewModelState()))`. Use `Eagerly` by default.
- Events use `Channel` + `receiveAsFlow()` and are emitted by the ViewModel directly. **Events are not assembled** — error → `UiText` mapping happens at emission.
- `ObserveAsEvents(viewModel.events) { … }` is called in the **Root** only. The Root holds the ViewModel and the navigation callbacks; `Screen` takes only `uiState` and `onAction`.
- The composable decides nothing. The test is: *does the choice depend on data?* If yes, it belongs in the assembler. Static labels with no data dependency stay `stringResource(...)` in the composable.

> The `android-testing` examples call the Screen composable with `state = …`. `android-presentation-mvi`'s naming table is authoritative: the parameter is **`uiState`**. Do not "fix" it the other way.

### Naming

| Thing | Convention |
|---|---|
| Read use case | `Observe<Noun>UseCase` / `Get<Noun>UseCase` |
| Command use case | `<Verb><Noun>UseCase` |
| Resolving / validating | `Resolve<Noun>UseCase` / `Validate<Noun>UseCase` |
| Use case entry point | `operator fun invoke` — one public method, always |
| Injected use case property | drops the suffix: `private val observeCharacters: ObserveCharactersUseCase` |
| Screen data aggregate | `<Screen>Data`, with an `EMPTY` companion for `stateIn`'s initial value |
| Data source interface | `<Entity><Local\|Remote>DataSource` |
| DTO / Room entity | `<Model>Dto` / `<Model>Entity` |
| Mapper | extension function: `fun CharacterDto.toCharacter()` |
| Route | `<Screen>Route` |

**Implementations are never suffixed `Impl`** — name them for what they wrap or how they behave: `KtorCharacterRemoteDataSource`, `RoomFavoriteCharacterLocalDataSource`, `LocalFirstCharacterRepository`.

**Use the word "repository" only when the class genuinely coordinates multiple sources.** Everything else is a data source. Every data source and repository has an interface in `core/domain/`.

### Errors

`Result<D, E : Error>` with `Success`/`Error`, `typealias EmptyResult<E>`, and `map` / `onSuccess` / `onFailure` / `asEmptyResult` — all in `core/domain/`. The failure helper is `onFailure`; there is no `onError`.

`DataError.Network` / `DataError.Local` are justified here because the app has both a remote and a local source. Data sources narrow to one of them; a multi-source repository returns plain `DataError`. Feature-specific failures are their own enum implementing `Error`.

Never throw for expected failures — return `Result.Error`. Catch exceptions in the layer responsible for them (HTTP and disk in `data`, validation in `domain`); upper layers never see raw exceptions. A use case over one source keeps that source's concrete error type; only widen to `Result<T, Error>` when the operation genuinely spans domains.

### UiText

```kotlin
sealed interface UiText {
    data class DynamicString(val value: String) : UiText
    data class StringResourceText(val id: StringResource, val args: List<Any> = emptyList()) : UiText
}
```

In `core/ui/`. Note the variant is `StringResourceText` and `args` is a `List`. Rendered with `.asString()`.

`.toUiText()` mappers live next to it, plus one dispatcher over the `Error` marker interface. Use `UiText` for anything resource-backed or localizable; plain `String` for values that are always dynamic (a raw id, a name straight from the API).

### Koin

`appModule` in `di/` registers everything, **grouped by feature with a comment per group** — a flat list wiring four features is unreadable. `coreDataModule` and `coreDatabaseModule` cover core. Assembled in `startKoin { }` from the app entry point.

Prefer the constructor-reference overloads `singleOf` / `viewModelOf` / `factoryOf`; fall back to `single { }` / `viewModel { }` / `factory { }` only when constructor injection is not enough (a factory method, a qualified dependency, post-construction setup).

| Scope | Form | For |
|---|---|---|
| `singleOf` / `single` | `singleOf(::Impl) { bind<Interface>() }` | repositories, data sources, `HttpClient`, DB, DataStore |
| `factoryOf` | `factoryOf(::X)` | use cases and assemblers — stateless, so no state can leak between screens |
| `viewModelOf` | `viewModelOf(::XViewModel)` | ViewModels |

List definitions bottom-up: **use cases → assembler → ViewModel**. Inject ViewModels with `koinViewModel()` in Root composables only — never pass a ViewModel down the composable tree.

### Previews

**Every public composable that renders UI gets a preview** — screens, components, design-system atoms alike.

- `@PreviewLightDark`, `private fun <Name>Preview`, body wrapped in `AppTheme { }`. The preview function stays at the bottom of the composable's own file.
- Sample data comes from an `internal <Name>PreviewParameterProvider` in a `previews/` package, with `getDisplayName` overridden. Variants are provider values, not extra preview functions. Bundle into an `internal <Name>PreviewData` when more than one data parameter varies.
- **Screen previews run the real assembler** over a `<Screen>ViewModelState`, so a preview cannot drift from the screen.
- `<Screen>Root` gets **no** preview — it takes `koinViewModel()` and needs a DI graph. The `Screen` half is the previewable one; that is the point of the split. Dialogs and sheets are previewed through a `<Name>Content` twin, never directly.

### Testing

JUnit5 (`@BeforeEach` / `@AfterEach`), AssertK, Turbine, `kotlinx-coroutines-test`. There is no dispatcher rule — set and reset manually: `Dispatchers.setMain(UnconfinedTestDispatcher())` / `Dispatchers.resetMain()`.

Tests live in `shared/src/androidHostTest/` **mirroring the production package tree exactly**, named `<Subject>Test`. Same-package tests can reach `internal` declarations; a mirrored tree is what keeps that working.

- **Assembler tests** are the cheapest and highest-value: no coroutines, no fakes, no `runTest` — inputs and an expected `UiState`, one test per branch.
- **Use case tests** cover orchestration, rollback and failure paths. Trivial pass-throughs need none — the ViewModel test covers them.
- **ViewModel tests construct the *real* use cases over fake repositories.** Never write a fake per use case; that mocks away the wiring the test exists to check.
- **Fakes (`Fake<Subject>`), never mocks.** `SavedStateHandle` is instantiated directly.
- Robot pattern (`<Screen>Robot`) once a screen has 3+ UI tests or multi-step flows.

## Workflow

Implementation proceeds in small, logically self-contained tasks — one feature slice, one layer, or one infrastructure concern each. Per task:

1. **Implement** the task.
2. **Independent code review.** A *separate* agent reviews the diff against the `android-*` skills and these conventions. Fix what it finds.
3. **Independent verification on both platforms.** A *separate* agent runs the app and exercises the real UI and functionality — not just the build:
   - Android: `./gradlew :androidApp:installDebug`, then drive the emulator via `~/Library/Android/sdk/platform-tools/adb`.
   - iOS: build and run the `iosApp` scheme on an iPhone simulator (`xcodebuild` / `xcrun simctl`) and exercise the same flows.
   - Tests: `./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test`
4. **Commit** — one commit per completed task, only after review and both-platform verification pass.

Review and verification are done by **independent agents**, not by the session that wrote the code.

## Open points the skills do not cover

Decide these when first needed; do not assume a skill prescribes an answer.

1. **`ObserveAsEvents` has no definition in any skill** — only a call site and a stated home (`core/ui/`). We write it ourselves.
2. **Room on KMP is not covered.** Needs `@ConstructedBy` + `RoomDatabaseConstructor`, `BundledSQLiteDriver`, an `expect`/`actual` database builder, and per-target KSP.
3. **DataStore has no conventions** — no key naming, no `Preferences`-vs-typed guidance.
4. **`constructRoute` in the error-handling skill reads `BuildConfig.BASE_URL`, which does not exist in `commonMain`.** Use a plain `const val BASE_URL` in `core/data/` — this is a public API with no secret.
5. **JUnit5 needs `useJUnitPlatform()`** wiring for `androidHostTest`; the catalog currently has only `kotlin-test`.
6. **`.dataOrNull()`** appears in a skill example but is not among the four defined `Result` helpers — add it deliberately or avoid it.
7. **No skill names a navigation artifact or version.** The *pattern* is type-safe `@Serializable` routes; the coordinate is the version catalog's business.

## Git

Work happens on `develop`; `main` is the default/PR target branch.
