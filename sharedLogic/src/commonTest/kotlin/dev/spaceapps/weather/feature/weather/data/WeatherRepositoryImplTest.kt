package dev.spaceapps.weather.feature.weather.data

import app.cash.turbine.test
import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.network.createHttpClient
import dev.spaceapps.weather.core.network.createJson
import dev.spaceapps.weather.feature.weather.data.remote.KtorOpenWeatherApi
import dev.spaceapps.weather.feature.weather.domain.WeatherLocation
import dev.spaceapps.weather.testing.CURRENT_WEATHER_JSON
import dev.spaceapps.weather.testing.FakeWeatherCacheDao
import dev.spaceapps.weather.testing.FixedClock
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException

class WeatherRepositoryImplTest {

    private val kyiv = Coordinates(latitude = 50.45, longitude = 30.52)
    private val current = WeatherLocation.Current
    private val city = WeatherLocation.SavedCity(cityId = 7)
    private val clock = FixedClock(Instant.fromEpochSeconds(1_791_540_100))
    private val dao = FakeWeatherCacheDao()
    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(handler: MockRequestHandler) = WeatherRepositoryImpl(
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
    fun `refresh calls current weather endpoint with coordinates and metric units and api key`() = runTest {
        repository(okResponse).refresh(current, kyiv)

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

        repository.observeWeather(current).test {
            assertNull(awaitItem())

            assertEquals(Outcome.Success(Unit), repository.refresh(current, kyiv))

            val weather = assertNotNull(awaitItem())
            assertEquals("Kyiv", weather.locationName)
            assertEquals(clock.now, weather.fetchedAt)
        }
    }

    @Test
    fun `locations are cached independently`() = runTest {
        val repository = repository(okResponse)

        repository.refresh(city, kyiv)

        assertEquals(setOf("city:7"), dao.rows.value.keys)
        repository.observeWeather(current).test { assertNull(awaitItem()) }
        repository.observeWeather(city).test { assertNotNull(awaitItem()) }
    }

    @Test
    fun `failed refresh keeps cached weather`() = runTest {
        repository(okResponse).refresh(current, kyiv)
        val cached = dao.rows.value

        val result = repository { respondError(HttpStatusCode.InternalServerError) }.refresh(current, kyiv)

        assertEquals(Outcome.Failure(DataError.Remote.SERVER), result)
        assertEquals(cached, dao.rows.value)
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
            assertEquals(Outcome.Failure(expected), repository(handler).refresh(current, kyiv))
        }
    }

    @Test
    fun `refresh if stale fetches when nothing is cached`() = runTest {
        repository(okResponse).refreshIfStale(city, kyiv, maxAge = 10.minutes)

        assertEquals(1, requests.size)
    }

    @Test
    fun `refresh if stale skips fresh weather and fetches once it is old`() = runTest {
        val repository = repository(okResponse)
        repository.refresh(city, kyiv)

        clock.now += 9.minutes
        assertEquals(Outcome.Success(Unit), repository.refreshIfStale(city, kyiv, maxAge = 10.minutes))
        assertEquals(1, requests.size)

        clock.now += 2.minutes
        repository.refreshIfStale(city, kyiv, maxAge = 10.minutes)
        assertEquals(2, requests.size)
    }

    @Test
    fun `cached coordinates come from the cached response`() = runTest {
        val repository = repository(okResponse)
        assertNull(repository.cachedCoordinates(current))

        repository.refresh(current, kyiv)

        assertEquals(Coordinates(latitude = 50.4501, longitude = 30.5234), repository.cachedCoordinates(current))
        assertNull(repository.cachedCoordinates(city))
    }
}
