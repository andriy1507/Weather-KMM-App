package dev.spaceapps.weather.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.SIMPLE
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val REQUEST_TIMEOUT_MS = 15_000L
private const val CONNECT_TIMEOUT_MS = 10_000L

fun createJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
}

/**
 * OpenWeather authenticates via the `appid` query parameter, so it is attached to every request
 * and masked in logs.
 */
fun createHttpClient(
    engine: HttpClientEngine,
    json: Json,
    apiKey: String,
): HttpClient = HttpClient(engine) {
    expectSuccess = true

    install(ContentNegotiation) {
        json(json)
    }
    install(HttpTimeout) {
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
    }
    install(Logging) {
        logger = ApiKeyRedactingLogger
        level = LogLevel.INFO
    }
    defaultRequest {
        url(OpenWeatherConfig.BASE_URL)
        url.parameters.append(OpenWeatherConfig.API_KEY_PARAM, apiKey)
    }
}

private object ApiKeyRedactingLogger : Logger {
    private val apiKeyPattern = Regex("${OpenWeatherConfig.API_KEY_PARAM}=[^&\\s]+")

    override fun log(message: String) {
        Logger.SIMPLE.log(message.replace(apiKeyPattern, "${OpenWeatherConfig.API_KEY_PARAM}=***"))
    }
}
