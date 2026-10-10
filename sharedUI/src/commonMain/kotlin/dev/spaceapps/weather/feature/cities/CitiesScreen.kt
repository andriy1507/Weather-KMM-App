package dev.spaceapps.weather.feature.cities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesEffect
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesIntent
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesState
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesViewModel
import dev.spaceapps.weather.feature.cities.presentation.list.CityRowUi
import dev.spaceapps.weather.ui.component.LoadingContent
import dev.spaceapps.weather.ui.message
import dev.spaceapps.weather.ui.theme.WeatherTheme
import dev.spaceapps.weather.ui.util.ObserveAsEvents
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.add_city
import weatherapp.sharedui.generated.resources.cities_empty_message
import weatherapp.sharedui.generated.resources.cities_empty_title
import weatherapp.sharedui.generated.resources.city_deleted
import weatherapp.sharedui.generated.resources.delete
import weatherapp.sharedui.generated.resources.ic_add
import weatherapp.sharedui.generated.resources.ic_delete
import weatherapp.sharedui.generated.resources.tab_cities
import weatherapp.sharedui.generated.resources.undo

@Composable
fun CitiesRoute(
    onOpenCity: (cityId: Long) -> Unit,
    onOpenSearch: () -> Unit,
    viewModel: CitiesViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is CitiesEffect.OpenCity -> onOpenCity(effect.cityId)
            CitiesEffect.OpenSearch -> onOpenSearch()
            is CitiesEffect.ShowUndo -> scope.launch {
                snackbarHostState.currentSnackbarData?.dismiss()
                val result = snackbarHostState.showSnackbar(
                    message = getString(Res.string.city_deleted, effect.cityName),
                    actionLabel = getString(Res.string.undo),
                    duration = SnackbarDuration.Long,
                )
                if (result == SnackbarResult.ActionPerformed) viewModel.onIntent(CitiesIntent.UndoDelete)
            }
            is CitiesEffect.ShowError -> scope.launch {
                snackbarHostState.showSnackbar(getString(effect.error.message))
            }
        }
    }

    // Runs whenever the list is shown again (tab switch, back from a city): refreshes stale cities.
    LaunchedEffect(Unit) {
        viewModel.onIntent(CitiesIntent.ScreenStarted)
    }

    CitiesScreen(state = state, snackbarHostState = snackbarHostState, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitiesScreen(
    state: CitiesState,
    snackbarHostState: SnackbarHostState,
    onIntent: (CitiesIntent) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(Res.string.tab_cities)) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { onIntent(CitiesIntent.AddClicked) }) {
                Icon(painterResource(Res.drawable.ic_add), contentDescription = stringResource(Res.string.add_city))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            onRefresh = { onIntent(CitiesIntent.Refresh) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                !state.isLoaded -> LoadingContent()
                state.isEmpty -> EmptyCities(onAdd = { onIntent(CitiesIntent.AddClicked) })
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.cities, key = { it.id }) { city ->
                        SwipeToDeleteCityRow(
                            city = city,
                            onClick = { onIntent(CitiesIntent.CityClicked(city.id)) },
                            onDelete = { onIntent(CitiesIntent.DeleteCity(city.id)) },
                            modifier = Modifier.animateItem(),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SwipeToDeleteCityRow(
    city: CityRowUi,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dismissState = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onDelete() },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    painterResource(Res.drawable.ic_delete),
                    contentDescription = stringResource(Res.string.delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        CityRow(city, onClick)
    }
}

@Composable
private fun CityRow(city: CityRowUi, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(city.name) },
        supportingContent = {
            Text(listOfNotNull(city.subtitle, city.conditionText).joinToString(" · "))
        },
        leadingContent = {
            Box(Modifier.size(48.dp)) {
                city.iconUrl?.let { AsyncImage(model = it, contentDescription = city.conditionText) }
            }
        },
        trailingContent = {
            Text(city.temperature ?: "–", style = MaterialTheme.typography.headlineSmall)
        },
    )
}

@Composable
private fun EmptyCities(onAdd: () -> Unit) {
    // Scrollable so pull-to-refresh still works.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        Text(stringResource(Res.string.cities_empty_title), style = MaterialTheme.typography.titleLarge)
        Text(
            text = stringResource(Res.string.cities_empty_message),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onAdd) { Text(stringResource(Res.string.add_city)) }
    }
}

@Preview
@Composable
private fun CitiesScreenPreview() {
    WeatherTheme(ThemeMode.LIGHT) {
        CitiesScreen(
            state = CitiesState(
                cities = listOf(
                    CityRowUi(1, "Lviv", "UA", "9°", "Light rain", null),
                    CityRowUi(2, "Springfield", "Illinois, US", null, null, null),
                ),
                isLoaded = true,
            ),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}
