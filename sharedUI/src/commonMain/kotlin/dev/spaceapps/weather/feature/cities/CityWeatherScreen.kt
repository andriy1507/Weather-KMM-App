package dev.spaceapps.weather.feature.cities

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherEffect
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherIntent
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherState
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherViewModel
import dev.spaceapps.weather.ui.component.BackButton
import dev.spaceapps.weather.ui.component.ErrorContent
import dev.spaceapps.weather.ui.component.IconAction
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
import org.koin.core.parameter.parametersOf
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.cancel
import weatherapp.sharedui.generated.resources.delete
import weatherapp.sharedui.generated.resources.delete_city_message
import weatherapp.sharedui.generated.resources.delete_city_title
import weatherapp.sharedui.generated.resources.ic_delete

@Composable
fun CityWeatherRoute(
    cityId: Long,
    onClose: () -> Unit,
    viewModel: CityWeatherViewModel = koinViewModel { parametersOf(cityId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            CityWeatherEffect.Close -> onClose()
            is CityWeatherEffect.ShowError -> scope.launch {
                snackbarHostState.showSnackbar(getString(effect.error.message))
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onIntent(CityWeatherIntent.ScreenStarted)
    }

    CityWeatherScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onBack = onClose,
        onIntent = viewModel::onIntent,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CityWeatherScreen(
    state: CityWeatherState,
    snackbarHostState: SnackbarHostState,
    onBack: () -> Unit,
    onIntent: (CityWeatherIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = {
                    Column {
                        Text(state.title)
                        Text(
                            text = state.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconAction(Res.drawable.ic_delete, stringResource(Res.string.delete)) {
                        onIntent(CityWeatherIntent.DeleteClicked)
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(CityWeatherIntent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            val weather = state.weather
            val error = state.error
            when {
                weather != null -> WeatherContent(weather)
                error != null -> ErrorContent(error = error, onRetry = { onIntent(CityWeatherIntent.Refresh) })
                else -> LoadingContent()
            }
        }
    }

    if (state.showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { onIntent(CityWeatherIntent.DeleteDismissed) },
            title = { Text(stringResource(Res.string.delete_city_title, state.title)) },
            text = { Text(stringResource(Res.string.delete_city_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(CityWeatherIntent.DeleteConfirmed) }) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(CityWeatherIntent.DeleteDismissed) }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }
}

@Preview
@Composable
private fun CityWeatherScreenPreview() {
    WeatherTheme(ThemeMode.LIGHT) {
        CityWeatherScreen(
            state = CityWeatherState(title = "Lviv", subtitle = "UA", weather = previewWeather.copy(location = "Lviv")),
            snackbarHostState = remember { SnackbarHostState() },
            onBack = {},
            onIntent = {},
        )
    }
}
