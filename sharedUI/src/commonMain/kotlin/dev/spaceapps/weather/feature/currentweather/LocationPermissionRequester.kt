package dev.spaceapps.weather.feature.currentweather

import androidx.compose.runtime.Composable

/**
 * Returns a function that asks for location access and reports the outcome to [onResult]:
 * the system dialog, or the app's settings page once the user has permanently denied it.
 */
@Composable
internal expect fun rememberLocationPermissionRequester(onResult: (granted: Boolean) -> Unit): () -> Unit
