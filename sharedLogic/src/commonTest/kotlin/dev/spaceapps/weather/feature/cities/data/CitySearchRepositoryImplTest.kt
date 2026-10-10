package dev.spaceapps.weather.feature.cities.data

import dev.spaceapps.weather.core.domain.DataError
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.location.Coordinates
import dev.spaceapps.weather.core.network.createHttpClient
import dev.spaceapps.weather.core.network.createJson
import dev.spaceapps.weather.feature.cities.data.remote.KtorGeocodingApi
import dev.spaceapps.weather.feature.cities.domain.model.CityCandidate
import dev.spaceapps.weather.feature.cities.domain.model.CitySearchResult
import dev.spaceapps.weather.testing.FakeSavedCityDao
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
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.test.runTest

class CitySearchRepositoryImplTest {

    private val dao = FakeSavedCityDao()
    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(handler: MockRequestHandler) = CitySearchRepositoryImpl(
        api = KtorGeocodingApi(
            createHttpClient(
                engine = MockEngine { request -> requests += request; handler(this, request) },
                json = createJson(),
                apiKey = "test-key",
            ),
        ),
        dao = dao,
    )

    private val okResponse: MockRequestHandler = {
        respond(
            content = GEOCODING_JSON,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    @Test
    fun `search calls direct geocoding with trimmed query and limit`() = runTest {
        repository(okResponse).search("  Springfield ")

        val url = requests.single().url
        assertEquals("/geo/1.0/direct", url.encodedPath)
        assertEquals("Springfield", url.parameters["q"])
        assertEquals("5", url.parameters["limit"])
        assertEquals("test-key", url.parameters["appid"])
    }

    @Test
    fun `results are mapped with rounded coordinates and duplicates removed`() = runTest {
        val result = repository(okResponse).search("Springfield")

        val expected = listOf(
            CitySearchResult(
                CityCandidate("Springfield", "Illinois", "US", Coordinates(39.799, -89.644)),
                isSaved = false,
            ),
            CitySearchResult(
                CityCandidate("Springfield", "Missouri", "US", Coordinates(37.2153, -93.2982)),
                isSaved = false,
            ),
        )
        assertEquals(Outcome.Success(expected), result)
    }

    @Test
    fun `already saved places are marked`() = runTest {
        SavedCitiesRepositoryImpl(dao, FixedClock(Instant.fromEpochMilliseconds(0)))
            .add(CityCandidate("Springfield", "Missouri", "US", Coordinates(37.21533, -93.29824)))

        val results = (repository(okResponse).search("Springfield") as Outcome.Success).data

        assertEquals(listOf(false, true), results.map { it.isSaved })
    }

    @Test
    fun `blank query returns no results without a request`() = runTest {
        assertEquals(Outcome.Success(emptyList()), repository(okResponse).search("   "))
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `failures map to remote errors`() = runTest {
        val result = repository { respondError(HttpStatusCode.Unauthorized) }.search("Lviv")

        assertEquals(Outcome.Failure(DataError.Remote.UNAUTHORIZED), result)
    }

    private companion object {
        /** Shape of `GET /geo/1.0/direct`; the third item duplicates the first place. */
        const val GEOCODING_JSON = """
        [
          { "name": "Springfield", "local_names": { "en": "Springfield" }, "lat": 39.7990175, "lon": -89.6439575, "country": "US", "state": "Illinois" },
          { "name": "Springfield", "lat": 37.2153307, "lon": -93.2982436, "country": "US", "state": "Missouri" },
          { "name": "Springfield", "lat": 39.79902, "lon": -89.64396, "country": "US", "state": "Illinois" }
        ]
        """
    }
}
