package dev.spaceapps.weather.feature.cities.presentation.search

import androidx.lifecycle.viewModelScope
import dev.spaceapps.weather.core.domain.Outcome
import dev.spaceapps.weather.core.mvi.MviViewModel
import dev.spaceapps.weather.feature.cities.domain.CitySearchRepository
import dev.spaceapps.weather.feature.cities.domain.SavedCitiesRepository
import dev.spaceapps.weather.feature.cities.domain.model.CitySearchResult
import dev.spaceapps.weather.feature.cities.presentation.subtitle
import dev.spaceapps.weather.feature.weather.presentation.toWeatherError
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class CitySearchViewModel(
    private val searchRepository: CitySearchRepository,
    private val citiesRepository: SavedCitiesRepository,
    private val typingDebounce: Duration = 350.milliseconds,
) : MviViewModel<CitySearchState, CitySearchIntent, CitySearchEffect>(CitySearchState()) {

    private val queries = MutableStateFlow("")
    private var results: List<CitySearchResult> = emptyList()

    init {
        viewModelScope.launch {
            @OptIn(FlowPreview::class)
            queries
                .map { it.trim() }
                // Clearing the field (or going below the minimum) applies at once; real queries wait for a typing pause.
                .debounce { if (it.length < MIN_QUERY_LENGTH) Duration.ZERO else typingDebounce }
                .distinctUntilChanged()
                // A newer query cancels the request still running for the previous one.
                .collectLatest(::search)
        }
    }

    override fun onIntent(intent: CitySearchIntent) {
        when (intent) {
            is CitySearchIntent.QueryChanged -> {
                setState { copy(query = intent.query) }
                queries.value = intent.query
            }
            is CitySearchIntent.ResultClicked -> add(intent.resultId)
            CitySearchIntent.Retry -> viewModelScope.launch { search(currentState.query.trim()) }
        }
    }

    private suspend fun search(query: String) {
        if (query.length < MIN_QUERY_LENGTH) {
            results = emptyList()
            setState { copy(results = emptyList(), isSearching = false, error = null, showNoResults = false) }
            return
        }
        setState { copy(isSearching = true, error = null, showNoResults = false) }
        when (val result = searchRepository.search(query)) {
            is Outcome.Success -> {
                results = result.data
                setState {
                    copy(
                        results = result.data.map { it.toUi() },
                        isSearching = false,
                        showNoResults = result.data.isEmpty(),
                    )
                }
            }
            is Outcome.Failure -> {
                results = emptyList()
                setState { copy(results = emptyList(), isSearching = false, error = result.error.toWeatherError()) }
            }
        }
    }

    private fun add(resultId: String) {
        val result = results.find { it.id == resultId } ?: return
        viewModelScope.launch {
            val city = citiesRepository.add(result.city)
            sendEffect(CitySearchEffect.CityAdded(city.id))
        }
    }

    private val CitySearchResult.id: String
        get() = "${city.coordinates.latitude},${city.coordinates.longitude}"

    private fun CitySearchResult.toUi() = CitySearchResultUi(
        id = id,
        name = city.name,
        subtitle = city.subtitle,
        isSaved = isSaved,
    )

    private companion object {
        const val MIN_QUERY_LENGTH = 2
    }
}
