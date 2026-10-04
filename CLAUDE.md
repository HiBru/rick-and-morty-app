# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

`RickAndMortyShowcase` — a Kotlin Multiplatform app targeting Android and iOS, with the UI shared via Compose Multiplatform. Package root: `de.shinz.rickandmortyshowcase`.

**Screen-by-screen behaviour lives in [docs/SPEC.md](docs/SPEC.md) — read it before implementing a feature.**
**Task order, dependency rationale and locked decisions live in [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md) — read it before starting any task.**

The app is still being built out: the data layer and DI are wired, and `App()` renders a themed placeholder until the navigation shell lands. Most feature work means creating new structure rather than editing existing code. **The plan's checklist is the current state of play** — the first unticked box is what's next.

## Commands

```bash
# Build / run
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug                  # to a connected device/emulator
./gradlew :shared:compileKotlinIosSimulatorArm64    # fast iOS check without Xcode
# iOS: open ./iosApp in Xcode and run (Gradle builds the Shared framework as a dependency)

# Tests
./gradlew :shared:allTests                                   # all targets
./gradlew :shared:testAndroidHostTest                        # JVM/host tests (commonTest + androidHostTest)
./gradlew :shared:iosSimulatorArm64Test                      # iOS tests (commonTest + iosTest)
./gradlew :shared:testAndroidHostTest --tests "de.shinz.rickandmortyshowcase.ToolchainCommonTest"
./gradlew :shared:connectedAndroidDeviceTest                 # instrumented; no device tests exist yet

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
| Date/time | Platform formatters behind one `AppDateTimeManager` (`java.time` on Android, `NSDateFormatter` on iOS) |
| Testing | `kotlin.test` (commonTest) + JUnit5 (androidHostTest), AssertK, Turbine, `kotlinx-coroutines-test` |

**No versions here** — they belong in `gradle/libs.versions.toml`, and no dependency coordinate may be hardcoded in a `build.gradle.kts`.

Several pinned versions are deliberately **not** the newest release, for reasons that are not guessable from the version numbers. `docs/IMPLEMENTATION_PLAN.md` records each choice and its reason — **do not bump a dependency without reading it first.**

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
    domain/            Result, Error, DataError, Character, repository + data-source interfaces,
                       and app-wide use cases (theme) that no single feature owns
    data/              BASE_URL, HttpClientFactory, safeCall helpers, DTOs, mappers,
                       Ktor/Room/DataStore data sources
    database/          @Database, entities, DAOs
    format/            AppDateTimeManager + expect PlatformDateTimeFormatter
    ui/                UiText, ObserveAsEvents, toUiText() mappers
di/
    AppModule.kt       appModule (grouped by feature) + coreDataModule,
                       coreDatabaseModule, coreDataStoreModule
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
2. **`Character`, the repository interface and the data-source interfaces live in `core/domain/`**, not in a feature. All three list/detail features consume them, and `core:domain` is defined as the home for shared domain models, repository interfaces, error types and `Result`. The same applies to **app-wide use cases**: the theme use cases live in `core/domain/usecase/` because the composition root needs them and it is not a feature, so `features/shared/` would be the wrong home.
3. **`App()` is the composition root, not a screen.** It reads the theme mode via `koinInject<ObserveThemeModeUseCase>()` + `collectAsStateWithLifecycle()` and wraps `AppNavHost` in `AppTheme`. This is the *only* sanctioned place a composable touches a use case directly — there is no screen state involved, and the six-piece MVI ceremony for a single enum is not worth it. **Do not copy this pattern into a screen.**

### Layering

`presentation → domain ← data`. Domain depends on nothing.

- **A ViewModel depends on use cases, never on a repository**, DAO, DataStore or network client.
- **A use case depends on repository/data-source interfaces only** — never on another use case, never on anything in `presentation`.
- Promote shared code only on the second consumer: one feature → its own package; two features → `features/shared/`; app-wide → `core/`. The composition root counts as app-wide, not as a feature.
- **Every layer may read `core/domain`** — including `core/designsystem`, which needs it to key styling off a domain enum.

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

Pattern per group: `@Immutable data class App<Group>` → `val LocalApp<Group> = staticCompositionLocalOf { App<Group>() }` → read as `AppTheme.<group>`. The `AppTheme { }` wrapper provides all six.

**Only `colors` varies by theme**, so the other five are hoisted to top-level `val`s instead of being allocated per composition. Typography is among them because the app uses the system font: the reference project remembers its typography inside composition only because Compose Resources' `Font()` is itself `@Composable`, and with no custom font there is nothing to remember.

`AppTheme` is both a composable and an object — the same shape Material 3 uses for `MaterialTheme`. `AppTheme { }` wraps, `AppTheme.colors` reads.

**The six groups are fixed:** `colors`, `spacing`, `radius`, `size`, `border`, `typography`. There is no `FontSize` group.

- `spacing` holds gaps and paddings **only** — 5–6 steps on a 4dp rhythm. Snap to the nearest existing step rather than adding a near-duplicate. Radii, component sizes and stroke widths are separate groups so a change to one cannot silently move another.
- `typography` carries the type scale: **4–5 named `TextStyle`s**. Reuse a Material 3 slot when the recipe is within ~1sp at the same weight; add a named style only when nothing fits.

**No composable contains a literal `dp`, `sp`, color or `FontWeight`.** No `fontSize =` or `fontWeight =` override at a call site — that means the style is missing from the design system. If a value has no token, **add the token**; do not inline it.

Two mechanical traps:

- `size` shadows `DrawScope.size` — alias it: `val sizes = AppTheme.size`.
- Draw lambdas are not composable — read tokens into locals *before* the modifier chain, not inside `Modifier.drawBehind { }`.

**`AppTheme` must default to following the system setting.** `@PreviewLightDark` resolves the preview's `uiMode`, so no preview may pass `themeMode` explicitly and no `…DarkPreview` twin is ever written.

## Conventions

The `android-*` skills (`android-module-structure`, `android-presentation-mvi`, `android-data-layer`, `android-domain-usecases`, `android-di-koin`, `android-navigation`, `android-error-handling`, `android-compose-ui`, `android-date-time-manager`, `android-testing`) are the authoritative source. **Load the relevant one before creating a ViewModel, repository, use case, DI module, route, screen or test.** What follows pins the project-level choices so they are not re-derived each session.

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
- Events use `Channel` + `receiveAsFlow()` and are emitted by the ViewModel directly. **Events are not assembled** — for an *event*, error → `UiText` mapping happens at emission. An error that is part of rendered state (a retryable error state) is held as `DataError?` in `ViewModelState` and mapped by the assembler instead.
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

There is a third variant, `Joined(parts, separator)`, which drops blank parts and their separators — that is how a row reads as "Alive · Human" when the API sent a blank `type`.

`.toUiText()` mappers live next to it. The skill also prescribes a dispatcher over the `Error` marker interface; it is **deliberately deferred** until something returns `Result<T, Error>` — see the plan. Use `UiText` for anything resource-backed or localizable; plain `String` for values that are always dynamic (a raw id, a name straight from the API).

**`strings.xml` does not follow Android escaping conventions** — Compose Resources is not aapt. It unescapes only `\uXXXX`, `\n`, `\t` and `\\`, so an Android-style `\'` ships a *visible backslash* to the user. It also substitutes only indexed placeholders (`%1$s`, `%1$d`); a bare `%s` is emitted literally with no error. Resource *text* can only be asserted in a test on iOS — on the JVM `getString` resolves through `Resources.getSystem()`, which an unmocked host test cannot provide.

### Koin

`appModule` in `di/` registers everything, **grouped by feature with a comment per group** — a flat list wiring four features is unreadable. `coreDataModule` and `coreDatabaseModule` cover core. Assembly lives in a shared `initKoin()` in `di/` that both hosts call — iOS has no `Application` class, so `startKoin { }` cannot live in the Android app module.

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

AssertK, Turbine and `kotlinx-coroutines-test` everywhere; JUnit5 where the test is JVM-only. Tests are named `<Subject>Test` and sit in the **same package as their subject**, so each test source set mirrors the production package tree exactly — same-package tests can reach `internal` declarations, and a mirrored tree is what keeps that working.

The suite is **split by what the test needs**, because JUnit5 is JVM-only and tests placed only in `androidHostTest` would leave `:shared:iosSimulatorArm64Test` green with zero tests:

| Source set | Holds | Framework |
|---|---|---|
| `shared/src/commonTest/` | Pure logic: `Result` helpers, DTO/entity mappers, **assemblers**, pure use cases | `kotlin.test` annotations + AssertK + Turbine — runs on **both** platforms |
| `shared/src/androidHostTest/` | ViewModel tests, and anything needing a JVM-only API | JUnit5 (`@BeforeEach`/`@AfterEach`, `@RegisterExtension`) |

Assemblers are the highest-value tests in the codebase and are pure, so they belong in `commonTest` where iOS runs them too. For ViewModel tests, drive the main dispatcher with a reusable JUnit5 extension (`@JvmField @RegisterExtension`) that does `Dispatchers.setMain(UnconfinedTestDispatcher())` / `resetMain()` — there is no dispatcher rule in the skills, so the extension is ours.

- **Assembler tests** are the cheapest and highest-value: no coroutines, no fakes, no `runTest` — inputs and an expected `UiState`, one test per branch.
- **Use case tests** cover orchestration, rollback and failure paths. Trivial pass-throughs need none — the ViewModel test covers them.
- **ViewModel tests construct the *real* use cases over fake repositories.** Never write a fake per use case; that mocks away the wiring the test exists to check.
- **Fakes (`Fake<Subject>`), never mocks.** `SavedStateHandle` is instantiated directly.
- Robot pattern (`<Screen>Robot`) once a screen has 3+ UI tests or multi-step flows.

## Workflow

Implementation proceeds in small, logically self-contained tasks — one feature slice, one layer, or one infrastructure concern each. Per task:

1. **Implement** the task.
2. **Independent code review.** A *separate* agent reviews the diff against the `android-*` skills, this file **and `docs/IMPLEMENTATION_PLAN.md`** — the plan records conventions this app needs deliberately, and a reviewer who has not read it will flag them every single task. Fix what it finds.
3. **Verification, in two tiers.** Both tiers always run:
   ```bash
   ./gradlew :androidApp:assembleDebug
   ./gradlew :shared:compileKotlinIosSimulatorArm64
   ./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test
   ```
   For foundation tasks that render no UI, that is the whole check and the implementing session runs it. Once a task produces navigable UI, a *separate* agent additionally runs and **operates** the app on both platforms — `:androidApp:installDebug` plus `adb` on the emulator, and the `iosApp` scheme on an iPhone simulator via `xcodebuild` / `xcrun simctl`. `docs/IMPLEMENTATION_PLAN.md` marks where that boundary falls.
4. **Commit** — one commit per completed task, only after review and both-platform verification pass. The commit includes **ticking that task's checkbox** in `docs/IMPLEMENTATION_PLAN.md`, so `git log` and the checklist can never disagree.
5. **Clear the context.** The next task starts from a fresh context.

Review and the operate-the-app verification are done by **independent agents**, not by the session that wrote the code.

Because the context is cleared between tasks, a task must be resumable from the repo alone. `docs/IMPLEMENTATION_PLAN.md` is that handoff: it carries the task checklist with the skills each task needs, the dependency rationale, the locked decisions and the sanctioned conventions. Begin every task by reading, in order, `CLAUDE.md` → `docs/SPEC.md` → `docs/IMPLEMENTATION_PLAN.md` → the skills that task names. Record any new decision in the plan rather than relying on it being remembered.

## Gaps in the skills

The skills leave these unanswered. Most now have a recorded answer in `docs/IMPLEMENTATION_PLAN.md` — **check there before deciding anything here.**

**Answered in the plan — do not re-decide:**

1. **`ObserveAsEvents`** has no definition in any skill, only a call site and a stated home (`core/ui/`). The plan names a working implementation to mirror.
2. **Room on KMP** is covered by no skill. The plan has the full wiring: `@ConstructedBy` + `RoomDatabaseConstructor`, `BundledSQLiteDriver`, per-target KSP, and the two silent-failure traps.
3. **JUnit5 on `androidHostTest`** needs `useJUnitPlatform()` applied to the task, and `junit-platform-launcher` declared explicitly under Gradle 9. Both are in the plan; without them tests are skipped and the build still passes.
4. **The navigation artifact and version** are locked in the plan — a beta, deliberately, because the stable release conflicts with our lifecycle version. Not a free choice.

5. **`.dataOrNull()`** appears in a skill example but is not among the four defined `Result` helpers. Answered: deliberately **not** defined — the plan says why, and to unwrap with an exhaustive `when`.

6. **DataStore has no conventions** in any skill — no key naming, no `Preferences`-vs-typed guidance. Answered in the plan: Preferences (not proto), one file, and the key treated as schema.

Settled, and recorded here because the skills point the wrong way: the error-handling skill's `constructRoute` reads `BuildConfig.BASE_URL`, which does not exist in `commonMain` — this project uses a plain `const val BASE_URL` in `core/data/`, which is safe because the API is public and has no secret.

## Git

Work happens on `develop`; `main` is the default/PR target branch.
