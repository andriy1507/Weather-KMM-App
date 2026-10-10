package dev.spaceapps.weather.feature.currentweather

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherEffect
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherIntent
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherState
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherViewModel
import dev.spaceapps.weather.feature.currentweather.presentation.LocationPermission
import dev.spaceapps.weather.feature.weather.presentation.WeatherError
import dev.spaceapps.weather.ui.component.ErrorContent
import dev.spaceapps.weather.ui.component.LoadingContent
import dev.spaceapps.weather.ui.component.WeatherContent
import dev.spaceapps.weather.ui.component.previewWeather
import dev.spaceapps.weather.ui.message
import dev.spaceapps.weather.ui.theme.WeatherTheme
import dev.spaceapps.weather.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.app_title
import weatherapp.sharedui.generated.resources.permission_allow
import weatherapp.sharedui.generated.resources.permission_banner

@Composable
fun CurrentWeatherRoute(viewModel: CurrentWeatherViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val requestLocationPermission = rememberLocationPermissionRequester { granted ->
        viewModel.onIntent(CurrentWeatherIntent.PermissionResult(granted))
    }

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            CurrentWeatherEffect.RequestLocationPermission -> requestLocationPermission()
            // Launched separately so a visible snackbar doesn't hold back later effects.
            is CurrentWeatherEffect.ShowError -> scope.launch {
                snackbarHostState.showSnackbar(getString(effect.error.message))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(CurrentWeatherIntent.ScreenStarted)
    }

    CurrentWeatherScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrentWeatherScreen(
    state: CurrentWeatherState,
    snackbarHostState: SnackbarHostState,
    onIntent: (CurrentWeatherIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.weather?.location ?: stringResource(Res.string.app_title)) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(CurrentWeatherIntent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            // Locals: properties of a class from another module can't be smart-cast.
            val weather = state.weather
            val error = state.error
            when {
                weather != null -> WeatherContent(weather) {
                    if (state.permission == LocationPermission.DENIED) {
                        PermissionBanner(onAllowLocation = { onIntent(CurrentWeatherIntent.GrantPermissionClicked) })
                    }
                }
                error != null -> ErrorContent(
                    error = error,
                    onRetry = { onIntent(CurrentWeatherIntent.Refresh) },
                    primaryAction = {
                        if (error == WeatherError.LOCATION_PERMISSION_DENIED) {
                            Button(onClick = { onIntent(CurrentWeatherIntent.GrantPermissionClicked) }) {
                                Text(stringResource(Res.string.permission_allow))
                            }
                        }
                    },
                )
                else -> LoadingContent()
            }
        }
    }
}

@Composable
private fun PermissionBanner(onAllowLocation: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.permission_banner),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAllowLocation) {
                Text(stringResource(Res.string.permission_allow))
            }
        }
    }
}

@Preview
@Composable
private fun CurrentWeatherScreenPreview() {
    WeatherTheme(ThemeMode.LIGHT) {
        CurrentWeatherScreen(
            state = CurrentWeatherState(weather = previewWeather, permission = LocationPermission.DENIED),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun CurrentWeatherErrorPreview() {
    WeatherTheme(ThemeMode.DARK) {
        CurrentWeatherScreen(
            state = CurrentWeatherState(error = WeatherError.LOCATION_PERMISSION_DENIED),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}
