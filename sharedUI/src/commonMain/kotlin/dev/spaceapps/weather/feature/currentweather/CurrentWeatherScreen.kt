package dev.spaceapps.weather.feature.currentweather

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.core.settings.UnitSystem
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherEffect
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherError
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherIntent
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherState
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherUi
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherViewModel
import dev.spaceapps.weather.feature.currentweather.presentation.LocationPermission
import dev.spaceapps.weather.ui.theme.WeatherTheme
import dev.spaceapps.weather.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.app_title
import weatherapp.sharedui.generated.resources.cloudiness
import weatherapp.sharedui.generated.resources.error_invalid_api_key
import weatherapp.sharedui.generated.resources.error_location_disabled
import weatherapp.sharedui.generated.resources.error_location_permission
import weatherapp.sharedui.generated.resources.error_location_unavailable
import weatherapp.sharedui.generated.resources.error_no_internet
import weatherapp.sharedui.generated.resources.error_rate_limited
import weatherapp.sharedui.generated.resources.error_server
import weatherapp.sharedui.generated.resources.error_timeout
import weatherapp.sharedui.generated.resources.error_title
import weatherapp.sharedui.generated.resources.error_unknown
import weatherapp.sharedui.generated.resources.feels_like
import weatherapp.sharedui.generated.resources.high_low
import weatherapp.sharedui.generated.resources.humidity
import weatherapp.sharedui.generated.resources.ic_more_vert
import weatherapp.sharedui.generated.resources.permission_allow
import weatherapp.sharedui.generated.resources.permission_banner
import weatherapp.sharedui.generated.resources.pressure
import weatherapp.sharedui.generated.resources.retry
import weatherapp.sharedui.generated.resources.settings
import weatherapp.sharedui.generated.resources.settings_theme
import weatherapp.sharedui.generated.resources.settings_units
import weatherapp.sharedui.generated.resources.sunrise
import weatherapp.sharedui.generated.resources.sunset
import weatherapp.sharedui.generated.resources.theme_dark
import weatherapp.sharedui.generated.resources.theme_light
import weatherapp.sharedui.generated.resources.theme_system
import weatherapp.sharedui.generated.resources.units_imperial
import weatherapp.sharedui.generated.resources.units_metric
import weatherapp.sharedui.generated.resources.updated_at
import weatherapp.sharedui.generated.resources.visibility
import weatherapp.sharedui.generated.resources.wind

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
                actions = {
                    SettingsMenu(
                        unitSystem = state.unitSystem,
                        themeMode = state.themeMode,
                        onIntent = onIntent,
                    )
                },
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
                weather != null -> WeatherContent(
                    weather = weather,
                    showPermissionBanner = state.permission == LocationPermission.DENIED,
                    onAllowLocation = { onIntent(CurrentWeatherIntent.GrantPermissionClicked) },
                )
                error != null -> ErrorContent(
                    error = error,
                    onRetry = { onIntent(CurrentWeatherIntent.Refresh) },
                    onAllowLocation = { onIntent(CurrentWeatherIntent.GrantPermissionClicked) },
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun SettingsMenu(
    unitSystem: UnitSystem,
    themeMode: ThemeMode,
    onIntent: (CurrentWeatherIntent) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(painterResource(Res.drawable.ic_more_vert), contentDescription = stringResource(Res.string.settings))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        MenuHeader(Res.string.settings_units)
        UnitSystem.entries.forEach { option ->
            RadioMenuItem(
                text = stringResource(option.label),
                selected = option == unitSystem,
                onClick = {
                    expanded = false
                    onIntent(CurrentWeatherIntent.ChangeUnitSystem(option))
                },
            )
        }
        HorizontalDivider()
        MenuHeader(Res.string.settings_theme)
        ThemeMode.entries.forEach { option ->
            RadioMenuItem(
                text = stringResource(option.label),
                selected = option == themeMode,
                onClick = {
                    expanded = false
                    onIntent(CurrentWeatherIntent.ChangeThemeMode(option))
                },
            )
        }
    }
}

@Composable
private fun MenuHeader(title: StringResource) {
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun RadioMenuItem(text: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
    )
}

@Composable
private fun WeatherContent(
    weather: CurrentWeatherUi,
    showPermissionBanner: Boolean,
    onAllowLocation: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showPermissionBanner) {
            PermissionBanner(onAllowLocation)
        }

        AsyncImage(
            model = weather.iconUrl,
            contentDescription = weather.conditionText,
            modifier = Modifier.size(128.dp),
        )
        Text(weather.temperature, style = MaterialTheme.typography.displayLarge)
        Text(weather.conditionText, style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(Res.string.feels_like, weather.feelsLike) + "  ·  " +
                stringResource(Res.string.high_low, weather.maxTemperature, weather.minTemperature),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))
        WeatherDetails(weather)

        Text(
            text = stringResource(Res.string.updated_at, weather.updatedAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WeatherDetails(weather: CurrentWeatherUi) {
    val details = buildList {
        add(Detail(stringResource(Res.string.humidity), weather.humidity))
        add(Detail(stringResource(Res.string.wind), weather.wind, windDirectionDegrees = weather.windDirectionDegrees))
        add(Detail(stringResource(Res.string.pressure), weather.pressure))
        weather.visibility?.let { add(Detail(stringResource(Res.string.visibility), it)) }
        add(Detail(stringResource(Res.string.cloudiness), weather.cloudiness))
        add(Detail(stringResource(Res.string.sunrise), weather.sunrise))
        add(Detail(stringResource(Res.string.sunset), weather.sunset))
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        details.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { DetailCard(it, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

private data class Detail(
    val label: String,
    val value: String,
    val windDirectionDegrees: Int? = null,
)

@Composable
private fun DetailCard(detail: Detail, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(
                text = detail.label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(detail.value, style = MaterialTheme.typography.titleLarge)
                detail.windDirectionDegrees?.let { degrees ->
                    // Meteorological degrees say where the wind comes from; the arrow shows where it blows.
                    Text(
                        text = "↑",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp).rotate(degrees + 180f),
                    )
                }
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

@Composable
private fun ErrorContent(
    error: CurrentWeatherError,
    onRetry: () -> Unit,
    onAllowLocation: () -> Unit,
) {
    // Scrollable so pull-to-refresh also works from the error state.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.error_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(error.message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (error == CurrentWeatherError.LOCATION_PERMISSION_DENIED) {
            Button(onClick = onAllowLocation) { Text(stringResource(Res.string.permission_allow)) }
        }
        OutlinedButton(onClick = onRetry) { Text(stringResource(Res.string.retry)) }
    }
}

private val CurrentWeatherError.message: StringResource
    get() = when (this) {
        CurrentWeatherError.NO_INTERNET -> Res.string.error_no_internet
        CurrentWeatherError.TIMEOUT -> Res.string.error_timeout
        CurrentWeatherError.INVALID_API_KEY -> Res.string.error_invalid_api_key
        CurrentWeatherError.RATE_LIMITED -> Res.string.error_rate_limited
        CurrentWeatherError.SERVER -> Res.string.error_server
        CurrentWeatherError.LOCATION_PERMISSION_DENIED -> Res.string.error_location_permission
        CurrentWeatherError.LOCATION_DISABLED -> Res.string.error_location_disabled
        CurrentWeatherError.LOCATION_UNAVAILABLE -> Res.string.error_location_unavailable
        CurrentWeatherError.UNKNOWN -> Res.string.error_unknown
    }

private val UnitSystem.label: StringResource
    get() = when (this) {
        UnitSystem.METRIC -> Res.string.units_metric
        UnitSystem.IMPERIAL -> Res.string.units_imperial
    }

private val ThemeMode.label: StringResource
    get() = when (this) {
        ThemeMode.SYSTEM -> Res.string.theme_system
        ThemeMode.LIGHT -> Res.string.theme_light
        ThemeMode.DARK -> Res.string.theme_dark
    }

private val previewWeather = CurrentWeatherUi(
    location = "Kyiv, UA",
    conditionText = "Broken clouds",
    conditionId = 803,
    iconUrl = "https://openweathermap.org/img/wn/04d@4x.png",
    temperature = "12°",
    feelsLike = "11°",
    minTemperature = "11°",
    maxTemperature = "14°",
    humidity = "71%",
    pressure = "1015 hPa",
    wind = "4.1 m/s",
    windDirectionDegrees = 240,
    visibility = "10 km",
    cloudiness = "75%",
    sunrise = "07:00",
    sunset = "18:00",
    updatedAt = "10:02",
)

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
            state = CurrentWeatherState(error = CurrentWeatherError.LOCATION_PERMISSION_DENIED),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}
