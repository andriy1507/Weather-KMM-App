package dev.spaceapps.weather.core.location

import dev.spaceapps.weather.core.domain.Outcome
import kotlin.coroutines.resume
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLErrorDenied
import platform.CoreLocation.kCLLocationAccuracyKilometer
import platform.Foundation.NSError
import platform.darwin.NSObject

/**
 * One-shot location via [CLLocationManager.requestLocation].
 * The permission prompt itself is shown by the SwiftUI layer.
 */
class IosLocationProvider : LocationProvider {

    override fun hasLocationPermission(): Boolean {
        val status = CLLocationManager().authorizationStatus
        return status == kCLAuthorizationStatusAuthorizedWhenInUse ||
            status == kCLAuthorizationStatusAuthorizedAlways
    }

    override suspend fun getCurrentLocation(): Outcome<Coordinates, LocationError> {
        if (!hasLocationPermission()) return Outcome.Failure(LocationError.PERMISSION_DENIED)
        // Apple warns against calling this on the main thread.
        val servicesEnabled = withContext(Dispatchers.IO) { CLLocationManager.locationServicesEnabled() }
        if (!servicesEnabled) return Outcome.Failure(LocationError.LOCATION_DISABLED)

        return withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
            // CLLocationManager delivers delegate callbacks on the run loop of the thread it was created on.
            withContext(Dispatchers.Main) { requestLocation() }
        } ?: Outcome.Failure(LocationError.UNAVAILABLE)
    }

    private suspend fun requestLocation(): Outcome<Coordinates, LocationError> =
        suspendCancellableCoroutine { continuation ->
            val manager = CLLocationManager()
            val delegate = OneShotLocationDelegate { result ->
                if (continuation.isActive) continuation.resume(result)
            }
            // The delegate property is weak: keep `delegate` referenced until the request finishes.
            manager.delegate = delegate
            manager.desiredAccuracy = kCLLocationAccuracyKilometer
            manager.requestLocation()

            continuation.invokeOnCancellation {
                manager.stopUpdatingLocation()
                delegate.cancel()
            }
        }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 15_000L
    }
}

private class OneShotLocationDelegate(
    private var onResult: ((Outcome<Coordinates, LocationError>) -> Unit)?,
) : NSObject(), CLLocationManagerDelegateProtocol {

    fun cancel() {
        onResult = null
    }

    @OptIn(ExperimentalForeignApi::class)
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation
        val result = location?.coordinate?.useContents { Outcome.Success(Coordinates(latitude, longitude)) }
            ?: Outcome.Failure(LocationError.UNAVAILABLE)
        finish(manager, result)
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        val error = if (didFailWithError.code == kCLErrorDenied) {
            LocationError.PERMISSION_DENIED
        } else {
            LocationError.UNAVAILABLE
        }
        finish(manager, Outcome.Failure(error))
    }

    private fun finish(manager: CLLocationManager, result: Outcome<Coordinates, LocationError>) {
        manager.delegate = null
        onResult?.invoke(result)
        onResult = null
    }
}
