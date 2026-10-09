package dev.spaceapps.weather.core.location

import dev.spaceapps.weather.core.domain.AppError
import dev.spaceapps.weather.core.domain.Outcome

enum class LocationError : AppError {
    PERMISSION_DENIED,
    LOCATION_DISABLED,
    UNAVAILABLE,
}

/**
 * Device location. Requesting the permission is a UI concern (platform dialogs);
 * this only checks the current grant and reads the location.
 */
interface LocationProvider {
    fun hasLocationPermission(): Boolean

    suspend fun getCurrentLocation(): Outcome<Coordinates, LocationError>
}
