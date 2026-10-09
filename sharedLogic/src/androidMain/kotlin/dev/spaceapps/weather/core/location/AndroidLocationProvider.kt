package dev.spaceapps.weather.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dev.spaceapps.weather.core.domain.Outcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** City-level accuracy is enough for weather, so only coarse location is requested. */
class AndroidLocationProvider(context: Context) : LocationProvider {

    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager = appContext.getSystemService(LocationManager::class.java)

    override fun hasLocationPermission(): Boolean =
        LOCATION_PERMISSIONS.any { appContext.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission") // Checked by hasLocationPermission().
    override suspend fun getCurrentLocation(): Outcome<Coordinates, LocationError> {
        if (!hasLocationPermission()) return Outcome.Failure(LocationError.PERMISSION_DENIED)
        if (!isLocationEnabled()) return Outcome.Failure(LocationError.LOCATION_DISABLED)

        return try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .setMaxUpdateAgeMillis(MAX_LOCATION_AGE_MS)
                .setDurationMillis(LOCATION_TIMEOUT_MS)
                .build()
            val cancellation = CancellationTokenSource()
            val location = fusedClient.getCurrentLocation(request, cancellation.token).await(cancellation)
                ?: fusedClient.lastLocation.await()

            if (location != null) {
                Outcome.Success(Coordinates(location.latitude, location.longitude))
            } else {
                Outcome.Failure(LocationError.UNAVAILABLE)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Outcome.Failure(LocationError.PERMISSION_DENIED)
        } catch (e: Exception) {
            Outcome.Failure(LocationError.UNAVAILABLE)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val manager = locationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            manager.isLocationEnabled
        } else {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    companion object {
        val LOCATION_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
        )

        private const val MAX_LOCATION_AGE_MS = 10 * 60 * 1000L
        private const val LOCATION_TIMEOUT_MS = 15_000L
    }
}
