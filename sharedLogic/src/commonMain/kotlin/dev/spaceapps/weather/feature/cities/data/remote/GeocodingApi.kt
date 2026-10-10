package dev.spaceapps.weather.feature.cities.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.Serializable

/** Item of `GET /geo/1.0/direct` (OpenWeather Geocoding API). */
@Serializable
data class GeocodingResultDto(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String,
    val state: String? = null,
)

interface GeocodingApi {
    suspend fun searchCities(query: String, limit: Int): List<GeocodingResultDto>
}

class KtorGeocodingApi(
    private val client: HttpClient,
) : GeocodingApi {

    override suspend fun searchCities(query: String, limit: Int): List<GeocodingResultDto> =
        client.get(DIRECT_GEOCODING_PATH) {
            parameter("q", query)
            parameter("limit", limit)
        }.body()

    private companion object {
        const val DIRECT_GEOCODING_PATH = "geo/1.0/direct"
    }
}
