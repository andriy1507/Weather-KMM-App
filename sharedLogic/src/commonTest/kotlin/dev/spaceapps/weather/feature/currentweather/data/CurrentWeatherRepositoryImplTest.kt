package dev.spaceapps.weather.feature.currentweather.data

import app.cash.turbine.test
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.network.createHttpClient
import dev.spaceapps.weather.core.network.createJson
import dev.spaceapps.weather.feature.currentweather.data.remote.KtorOpenWeatherApi
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.testing.CURRENT_WEATHER_JSON
import dev.spaceapps.weather.testing.FakeCurrentWeatherDao
import dev.spaceapps.weather.testing.FixedClock
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class CurrentWeatherRepositoryImplTest {

    private val kyiv = Coordinates(latitude = 50.45, longitude = 30.52)
    private val clock = FixedClock(Instant.fromEpochSeconds(1_791_540_100))
    private val dao = FakeCurrentWeatherDao()
    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(handler: MockRequestHandler) = CurrentWeatherRepositoryImpl(
        api = KtorOpenWeatherApi(
            createHttpClient(
                engine = MockEngine { request -> requests += request; handler(this, request) },
                json = createJson(),
                apiKey = "test-key",
            ),
        ),
        dao = dao,
        clock = clock,
    )

    /** Gives lambdas the [MockRequestHandler] receiver where it cannot be inferred (e.g. inside pairs). */
    private fun handler(block: MockRequestHandler): MockRequestHandler = block

    private val okResponse: MockRequestHandler = {
        respond(
            content = CURRENT_WEATHER_JSON,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    @Test
    fun `refresh calls current weather endpoint with coordinates, metric units and api key`() = runTest {
        repository(okResponse).refresh(kyiv)

        val url = requests.single().url
        assertEquals("api.openweathermap.org", url.host)
        assertEquals("/data/2.5/weather", url.encodedPath)
        assertEquals("50.45", url.parameters["lat"])
        assertEquals("30.52", url.parameters["lon"])
        assertEquals("metric", url.parameters["units"])
        assertEquals("test-key", url.parameters["appid"])
    }

    @Test
    fun `successful refresh is cached and emitted`() = runTest {
        val repository = repository(okResponse)

        repository.observeCurrentWeather().test {
            assertNull(awaitItem())

            assertEquals(Outcome.Success(Unit), repository.refresh(kyiv))

            val weather = assertNotNull(awaitItem())
            assertEquals("Kyiv", weather.locationName)
            assertEquals(clock.now, weather.fetchedAt)
        }
    }

    @Test
    fun `failed refresh keeps cached weather`() = runTest {
        repository(okResponse).refresh(kyiv)
        val cached = dao.state.value

        val result = repository { respondError(HttpStatusCode.InternalServerError) }.refresh(kyiv)

        assertEquals(Outcome.Failure(DataError.Remote.SERVER), result)
        assertEquals(cached, dao.state.value)
    }

    @Test
    fun `http and io failures map to remote errors`() = runTest {
        val cases = listOf(
            handler { respondError(HttpStatusCode.Unauthorized) } to DataError.Remote.UNAUTHORIZED,
            handler { respondError(HttpStatusCode.TooManyRequests) } to DataError.Remote.TOO_MANY_REQUESTS,
            handler { respondError(HttpStatusCode.ServiceUnavailable) } to DataError.Remote.SERVER,
            handler { respondError(HttpStatusCode.NotFound) } to DataError.Remote.UNKNOWN,
            handler { throw IOException("offline") } to DataError.Remote.NO_INTERNET,
            handler {
                respond(
                    content = "{ not json",
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } to DataError.Remote.SERIALIZATION,
        )

        cases.forEach { (handler, expected) ->
            assertEquals(Outcome.Failure(expected), repository(handler).refresh(kyiv))
        }
    }

    @Test
    fun `last known coordinates come from the cached response`() = runTest {
        val repository = repository(okResponse)
        assertNull(repository.lastKnownCoordinates())

        repository.refresh(kyiv)

        assertEquals(Coordinates(latitude = 50.4501, longitude = 30.5234), repository.lastKnownCoordinates())
    }
}
