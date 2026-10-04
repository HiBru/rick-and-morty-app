package de.shinz.rickandmortyshowcase.core.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test

class ResultTest {

    private val success: Result<Int, DataError> = Result.Success(2)
    private val failure: Result<Int, DataError> = Result.Error(DataError.Network.NO_INTERNET)

    @Test
    fun mapTransformsASuccessValue() {
        assertThat(success.map { it * 3 }).isEqualTo(Result.Success(6))
    }

    @Test
    fun mapLeavesAFailureUntouchedAndDoesNotRunTheTransform() {
        var ran = false

        val result = failure.map { ran = true; it * 3 }

        assertThat(result).isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
        assertThat(ran).isEqualTo(false)
    }

    /*
     * The identity assertions are isSameInstanceAs, not isEqualTo: Success and
     * Error are data classes, so a structural check would also pass for an
     * implementation that allocated a fresh wrapper on every call. Handing back
     * the receiver itself is the behaviour that lets these chain.
     */

    @Test
    fun onSuccessRunsOnlyForASuccessAndAlwaysReturnsTheReceiver() {
        val seen = mutableListOf<Int>()

        val fromSuccess = success.onSuccess { seen += it }
        val fromFailure = failure.onSuccess { seen += it }

        assertThat(seen).isEqualTo(listOf(2))
        assertThat(fromSuccess).isSameInstanceAs(success)
        assertThat(fromFailure).isSameInstanceAs(failure)
    }

    @Test
    fun onFailureRunsOnlyForAFailureAndAlwaysReturnsTheReceiver() {
        val seen = mutableListOf<DataError>()

        val fromFailure = failure.onFailure { seen += it }
        val fromSuccess = success.onFailure { seen += it }

        assertThat(seen).isEqualTo(listOf<DataError>(DataError.Network.NO_INTERNET))
        assertThat(fromFailure).isSameInstanceAs(failure)
        assertThat(fromSuccess).isSameInstanceAs(success)
    }

    @Test
    fun helpersChainInBothDirections() {
        val order = mutableListOf<String>()

        success
            .onSuccess { order += "success" }
            .onFailure { order += "failure" }
            .map { it + 1 }
            .onSuccess { order += "mapped:$it" }

        assertThat(order).isEqualTo(listOf("success", "mapped:3"))
    }

    @Test
    fun asEmptyResultDiscardsTheValueButKeepsTheOutcome() {
        assertThat(success.asEmptyResult()).isEqualTo(Result.Success(Unit))
        assertThat(failure.asEmptyResult())
            .isEqualTo(Result.Error(DataError.Network.NO_INTERNET))
    }

    /**
     * The variance that lets `LocalFirstCharacterRepository` (Task 9) return a
     * narrowed `DataError.Local` from the local source through a signature
     * declared as plain `DataError`, with no mapping step.
     *
     * The assignments below are the real proof and they are compile-time — this
     * test only guards against the value being altered on the way through, which
     * is why it asserts the error itself rather than just the branch.
     */
    @Test
    fun aNarrowedErrorSurvivesBeingWidenedAndMapped() {
        val fromLocalSource: Result<Int, DataError.Local> =
            Result.Error(DataError.Local.DISK_FULL)

        val widened: Result<String, DataError> = fromLocalSource.map { it.toString() }

        assertThat(widened).isEqualTo(Result.Error(DataError.Local.DISK_FULL))
    }
}
