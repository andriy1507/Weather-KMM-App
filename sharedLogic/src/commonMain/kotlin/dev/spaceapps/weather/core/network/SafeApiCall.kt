package dev.spaceapps.weather.core.network

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.ContentConvertException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

/** Runs [call] and maps expected network/HTTP/parsing failures to [DataError.Remote]. */
suspend fun <T> safeApiCall(call: suspend () -> T): Outcome<T, DataError.Remote> =
    try {
        Outcome.Success(call())
    } catch (e: CancellationException) {
        throw e
    } catch (e: ResponseException) {
        Outcome.Failure(e.response.status.toRemoteError())
    } catch (e: HttpRequestTimeoutException) {
        Outcome.Failure(DataError.Remote.REQUEST_TIMEOUT)
    } catch (e: ConnectTimeoutException) {
        Outcome.Failure(DataError.Remote.REQUEST_TIMEOUT)
    } catch (e: SocketTimeoutException) {
        Outcome.Failure(DataError.Remote.REQUEST_TIMEOUT)
    } catch (e: SerializationException) {
        Outcome.Failure(DataError.Remote.SERIALIZATION)
    } catch (e: ContentConvertException) {
        Outcome.Failure(DataError.Remote.SERIALIZATION)
    } catch (e: IOException) {
        Outcome.Failure(DataError.Remote.NO_INTERNET)
    } catch (e: Exception) {
        Outcome.Failure(DataError.Remote.UNKNOWN)
    }

private fun HttpStatusCode.toRemoteError(): DataError.Remote = when {
    this == HttpStatusCode.Unauthorized -> DataError.Remote.UNAUTHORIZED
    this == HttpStatusCode.TooManyRequests -> DataError.Remote.TOO_MANY_REQUESTS
    value in 500..599 -> DataError.Remote.SERVER
    else -> DataError.Remote.UNKNOWN
}
