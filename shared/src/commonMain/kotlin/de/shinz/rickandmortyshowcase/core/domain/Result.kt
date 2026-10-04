package de.shinz.rickandmortyshowcase.core.domain

/**
 * Marker for everything that can go wrong. Every error type in the app
 * implements it, which is what lets a use case spanning two domains return
 * `Result<T, Error>` without inventing a union type per operation.
 */
interface Error

/**
 * A typed success-or-failure. Expected failures are returned, never thrown:
 * exceptions are caught by the layer responsible for them (HTTP and disk in
 * `data`, validation in `domain`) and mapped to an [Error] here, so no upper
 * layer ever sees a raw exception for something that was always possible.
 */
sealed interface Result<out D, out E : Error> {

    data class Success<out D>(val data: D) : Result<D, Nothing>

    /**
     * The bound is fully qualified on purpose: this class shadows the [Error]
     * marker interface inside `Result`'s own scope, so the short name here would
     * resolve to itself.
     */
    data class Error<out E : de.shinz.rickandmortyshowcase.core.domain.Error>(
        val error: E,
    ) : Result<Nothing, E>
}

/** For an operation whose success carries no value — a write, a delete. */
typealias EmptyResult<E> = Result<Unit, E>

/** Maps the success value and leaves a failure untouched. */
inline fun <T, E : Error, R> Result<T, E>.map(map: (T) -> R): Result<R, E> = when (this) {
    is Result.Success -> Result.Success(map(data))
    is Result.Error -> Result.Error(error)
}

/** Runs [action] on success and returns the receiver, so calls can be chained. */
inline fun <T, E : Error> Result<T, E>.onSuccess(action: (T) -> Unit): Result<T, E> =
    also { if (this is Result.Success) action(data) }

/**
 * Runs [action] on failure and returns the receiver.
 *
 * Named for the outcome, not the class: the subtype is `Result.Error`, but there
 * is no `onError` — a failure is handled here.
 */
inline fun <T, E : Error> Result<T, E>.onFailure(action: (E) -> Unit): Result<T, E> =
    also { if (this is Result.Error) action(error) }

/** Discards a success value when only the outcome matters. */
fun <T, E : Error> Result<T, E>.asEmptyResult(): EmptyResult<E> = map { }
