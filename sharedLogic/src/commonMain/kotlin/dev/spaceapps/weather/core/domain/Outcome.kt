package dev.spaceapps.weather.core.domain

/** Marker for typed, expected failures. Unexpected failures stay exceptions. */
interface AppError

sealed interface Outcome<out D, out E : AppError> {
    data class Success<out D>(val data: D) : Outcome<D, Nothing>
    data class Failure<out E : AppError>(val error: E) : Outcome<Nothing, E>
}

typealias EmptyOutcome<E> = Outcome<Unit, E>

inline fun <D, E : AppError, R> Outcome<D, E>.map(transform: (D) -> R): Outcome<R, E> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
}

fun <D, E : AppError> Outcome<D, E>.asEmpty(): EmptyOutcome<E> = map { }
