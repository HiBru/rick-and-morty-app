package de.shinz.rickandmortyshowcase

import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Proves JUnit5 — not JUnit4 — is the engine for `androidHostTest`, and that
 * `@BeforeEach` runs. If `useJUnitPlatform()` were missing from the test task, these
 * would be skipped and the build would still pass, so a green `testAndroidHostTest`
 * with a zero test count is the failure mode this guards against.
 *
 * This is the only directly Jupiter-annotated test in the repo, so deleting it
 * re-arms that trap — `commonTest`'s `kotlin.test` tests would keep running via
 * `kotlin-test-junit5` and nothing would report zero discovery. Replace it with a
 * real ViewModel test (Task 16); do not simply remove it.
 */
class ToolchainJvmTest {

    private var beforeEachRan = false

    @BeforeEach
    fun setUp() {
        beforeEachRan = true
    }

    @Test
    fun `junit5 lifecycle runs in androidHostTest`() {
        assertThat(beforeEachRan).isTrue()
    }
}
