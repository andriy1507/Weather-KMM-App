package dev.spaceapps.weather.feature.cities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchEffect
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchIntent
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchResultUi
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchState
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchViewModel
import dev.spaceapps.weather.ui.component.BackButton
import dev.spaceapps.weather.ui.component.ErrorContent
import dev.spaceapps.weather.ui.component.IconAction
import dev.spaceapps.weather.ui.theme.WeatherTheme
import dev.spaceapps.weather.ui.util.ObserveAsEvents
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.city_saved
import weatherapp.sharedui.generated.resources.clear_search
import weatherapp.sharedui.generated.resources.ic_check
import weatherapp.sharedui.generated.resources.ic_close
import weatherapp.sharedui.generated.resources.search_hint
import weatherapp.sharedui.generated.resources.search_no_results

@Composable
fun CitySearchRoute(
    onBack: () -> Unit,
    onCityAdded: (cityId: Long) -> Unit,
    viewModel: CitySearchViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.effects) { effect ->
        when (effect) {
            is CitySearchEffect.CityAdded -> onCityAdded(effect.cityId)
        }
    }

    CitySearchScreen(state = state, onBack = onBack, onIntent = viewModel::onIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchScreen(
    state: CitySearchState,
    onBack: () -> Unit,
    onIntent: (CitySearchIntent) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = { BackButton(onBack) },
                title = {
                    TextField(
                        value = state.query,
                        onValueChange = { onIntent(CitySearchIntent.QueryChanged(it)) },
                        placeholder = { Text(stringResource(Res.string.search_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        ),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    )
                },
                actions = {
                    if (state.query.isNotEmpty()) {
                        IconAction(Res.drawable.ic_close, stringResource(Res.string.clear_search)) {
                            onIntent(CitySearchIntent.QueryChanged(""))
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.isSearching) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            val error = state.error
            when {
                error != null -> ErrorContent(error = error, onRetry = { onIntent(CitySearchIntent.Retry) })
                state.showNoResults -> Text(
                    text = stringResource(Res.string.search_no_results, state.query.trim()),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.results, key = { it.id }) { result ->
                        SearchResultRow(result, onClick = { onIntent(CitySearchIntent.ResultClicked(result.id)) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchResultRow(result: CitySearchResultUi, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(result.name) },
        supportingContent = { Text(result.subtitle) },
        trailingContent = if (result.isSaved) {
            {
                Icon(
                    painterResource(Res.drawable.ic_check),
                    contentDescription = stringResource(Res.string.city_saved),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            null
        },
    )
}

@Preview
@Composable
private fun CitySearchScreenPreview() {
    WeatherTheme(ThemeMode.LIGHT) {
        CitySearchScreen(
            state = CitySearchState(
                query = "Springfield",
                results = listOf(
                    CitySearchResultUi("1", "Springfield", "Illinois, US", isSaved = true),
                    CitySearchResultUi("2", "Springfield", "Missouri, US", isSaved = false),
                ),
            ),
            onBack = {},
            onIntent = {},
        )
    }
}
