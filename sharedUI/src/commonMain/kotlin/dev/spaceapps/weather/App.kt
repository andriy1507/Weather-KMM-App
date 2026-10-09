package dev.spaceapps.weather

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.spaceapps.weather.app.AppViewModel
import dev.spaceapps.weather.feature.currentweather.CurrentWeatherRoute
import dev.spaceapps.weather.ui.theme.WeatherTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    val appViewModel = koinViewModel<AppViewModel>()
    val themeMode by appViewModel.themeMode.collectAsStateWithLifecycle()

    WeatherTheme(themeMode) {
        CurrentWeatherRoute()
    }
}
