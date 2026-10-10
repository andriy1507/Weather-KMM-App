package dev.spaceapps.weather.ios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import dev.spaceapps.weather.app.AppViewModel
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherEffect
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherIntent
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherState
import dev.spaceapps.weather.feature.cities.presentation.detail.CityWeatherViewModel
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesEffect
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesIntent
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesState
import dev.spaceapps.weather.feature.cities.presentation.list.CitiesViewModel
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchEffect
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchIntent
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchState
import dev.spaceapps.weather.feature.cities.presentation.search.CitySearchViewModel
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherEffect
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherIntent
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherState
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherViewModel
import dev.spaceapps.weather.feature.settings.presentation.SettingsIntent
import dev.spaceapps.weather.feature.settings.presentation.SettingsState
import dev.spaceapps.weather.feature.settings.presentation.SettingsViewModel
import kotlin.reflect.KClass
import org.koin.core.component.KoinComponent
import org.koin.core.parameter.parametersOf

/**
 * Swift-facing owners of shared ViewModels. Each owns a [ViewModelStore], so calling [clear]
 * (from the Swift owner's `deinit`) cancels the ViewModel's coroutines like Android does.
 * Subclasses are concrete and non-generic, which keeps the Swift signatures fully typed.
 */
abstract class IosViewModelOwner<VM : ViewModel> internal constructor(
    modelClass: KClass<VM>,
) : KoinComponent {
    private val store = ViewModelStore()

    protected abstract fun create(): VM

    protected val viewModel: VM by lazy {
        val factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return this@IosViewModelOwner.create() as T
            }
        }
        ViewModelProvider.create(store, factory)[modelClass]
    }

    fun clear() {
        store.clear()
    }
}

class CurrentWeatherViewModelOwner : IosViewModelOwner<CurrentWeatherViewModel>(CurrentWeatherViewModel::class) {
    override fun create(): CurrentWeatherViewModel = getKoin().get()

    val currentState: CurrentWeatherState
        get() = viewModel.state.value

    fun subscribeState(onState: (CurrentWeatherState) -> Unit): FlowSubscription =
        viewModel.state.subscribe(onState)

    fun subscribeEffects(onEffect: (CurrentWeatherEffect) -> Unit): FlowSubscription =
        viewModel.effects.subscribe(onEffect)

    fun onIntent(intent: CurrentWeatherIntent) {
        viewModel.onIntent(intent)
    }
}

class AppViewModelOwner : IosViewModelOwner<AppViewModel>(AppViewModel::class) {
    override fun create(): AppViewModel = getKoin().get()

    val currentThemeMode: ThemeMode
        get() = viewModel.themeMode.value

    fun subscribeThemeMode(onThemeMode: (ThemeMode) -> Unit): FlowSubscription =
        viewModel.themeMode.subscribe(onThemeMode)
}

class CitiesViewModelOwner : IosViewModelOwner<CitiesViewModel>(CitiesViewModel::class) {
    override fun create(): CitiesViewModel = getKoin().get()

    val currentState: CitiesState
        get() = viewModel.state.value

    fun subscribeState(onState: (CitiesState) -> Unit): FlowSubscription =
        viewModel.state.subscribe(onState)

    fun subscribeEffects(onEffect: (CitiesEffect) -> Unit): FlowSubscription =
        viewModel.effects.subscribe(onEffect)

    fun onIntent(intent: CitiesIntent) {
        viewModel.onIntent(intent)
    }
}

class CitySearchViewModelOwner : IosViewModelOwner<CitySearchViewModel>(CitySearchViewModel::class) {
    override fun create(): CitySearchViewModel = getKoin().get()

    val currentState: CitySearchState
        get() = viewModel.state.value

    fun subscribeState(onState: (CitySearchState) -> Unit): FlowSubscription =
        viewModel.state.subscribe(onState)

    fun subscribeEffects(onEffect: (CitySearchEffect) -> Unit): FlowSubscription =
        viewModel.effects.subscribe(onEffect)

    fun onIntent(intent: CitySearchIntent) {
        viewModel.onIntent(intent)
    }
}

class CityWeatherViewModelOwner(
    private val cityId: Long,
) : IosViewModelOwner<CityWeatherViewModel>(CityWeatherViewModel::class) {
    override fun create(): CityWeatherViewModel = getKoin().get { parametersOf(cityId) }

    val currentState: CityWeatherState
        get() = viewModel.state.value

    fun subscribeState(onState: (CityWeatherState) -> Unit): FlowSubscription =
        viewModel.state.subscribe(onState)

    fun subscribeEffects(onEffect: (CityWeatherEffect) -> Unit): FlowSubscription =
        viewModel.effects.subscribe(onEffect)

    fun onIntent(intent: CityWeatherIntent) {
        viewModel.onIntent(intent)
    }
}

class SettingsViewModelOwner : IosViewModelOwner<SettingsViewModel>(SettingsViewModel::class) {
    override fun create(): SettingsViewModel = getKoin().get()

    val currentState: SettingsState
        get() = viewModel.state.value

    fun subscribeState(onState: (SettingsState) -> Unit): FlowSubscription =
        viewModel.state.subscribe(onState)

    fun onIntent(intent: SettingsIntent) {
        viewModel.onIntent(intent)
    }
}
