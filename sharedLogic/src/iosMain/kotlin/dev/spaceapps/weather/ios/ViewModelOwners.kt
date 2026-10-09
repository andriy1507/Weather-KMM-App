package dev.spaceapps.weather.ios

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.CreationExtras
import dev.spaceapps.weather.app.AppViewModel
import dev.spaceapps.weather.core.settings.ThemeMode
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherEffect
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherIntent
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherState
import dev.spaceapps.weather.feature.currentweather.presentation.CurrentWeatherViewModel
import kotlin.reflect.KClass
import org.koin.core.component.KoinComponent

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
