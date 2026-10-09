package dev.spaceapps.weather.core.domain

sealed interface DataError : AppError {
    enum class Remote : DataError {
        NO_INTERNET,
        REQUEST_TIMEOUT,
        UNAUTHORIZED,
        TOO_MANY_REQUESTS,
        SERVER,
        SERIALIZATION,
        UNKNOWN,
    }
}
