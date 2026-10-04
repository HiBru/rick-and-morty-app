package de.shinz.rickandmortyshowcase.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * Swaps `Dispatchers.Main` for a test dispatcher around every test.
 *
 * Needed because `viewModelScope` dispatches to `Dispatchers.Main`, which has no
 * implementation on a JVM host test — without this a ViewModel test fails with
 * "Module with the Main dispatcher had failed to initialize", not with anything
 * resembling the thing under test.
 *
 * It is a JUnit5 `Extension` rather than a rule: none of the skills supply a
 * dispatcher rule, JUnit5 has no `TestRule`, and `@BeforeEach`/`@AfterEach`
 * copied into every ViewModel test is the thing one of them eventually forgets.
 * Register it with `@JvmField @RegisterExtension`.
 *
 * [UnconfinedTestDispatcher] by default, deliberately: it runs each coroutine
 * eagerly to its first suspension point, so a `viewModelScope.launch` in `init`
 * has already done its work by the time the constructor returns and a test can
 * assert on `uiState.value` without advancing anything. Pass a
 * `StandardTestDispatcher` to a test that needs to observe an intermediate state
 * instead — the loading frame, say.
 *
 * **`runTest` adopts whatever `TestDispatcher` is installed as `Main`**, so the
 * test body and the ViewModel share one scheduler. That linkage is invisible and
 * load-bearing: it is the only reason `advanceUntilIdle()` inside `runTest`
 * advances coroutines launched on `viewModelScope`.
 *
 * In `androidHostTest` rather than a production source set: it is consumed only
 * from tests in this one module, so the reference project's reason for putting
 * its equivalent in `androidMain` — being used across modules — does not apply.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherExtension(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : BeforeEachCallback, AfterEachCallback {

    override fun beforeEach(context: ExtensionContext) {
        Dispatchers.setMain(dispatcher)
    }

    override fun afterEach(context: ExtensionContext) {
        Dispatchers.resetMain()
    }
}
