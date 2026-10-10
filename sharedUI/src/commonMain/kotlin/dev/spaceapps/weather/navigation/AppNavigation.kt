package dev.spaceapps.weather.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import dev.spaceapps.weather.feature.cities.CitiesRoute
import dev.spaceapps.weather.feature.cities.CitySearchRoute
import dev.spaceapps.weather.feature.cities.CityWeatherRoute
import dev.spaceapps.weather.feature.currentweather.CurrentWeatherRoute
import dev.spaceapps.weather.feature.settings.SettingsRoute
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bottom navigation with one back stack per tab. Each tab's entries are decorated separately, so a
 * tab keeps its screens, scroll positions and ViewModels while another tab is shown.
 */
@Composable
fun AppNavigation() {
    var currentTab by rememberSaveable { mutableStateOf(TopLevelTab.WEATHER) }
    val backStacks = TopLevelTab.entries.associateWith { tab ->
        rememberNavBackStack(navSavedStateConfiguration, tab.root)
    }
    val citiesStack = backStacks.getValue(TopLevelTab.CITIES)

    val entryProvider = entryProvider<NavKey> {
        entry<WeatherKey> { CurrentWeatherRoute() }
        entry<CitiesKey> {
            CitiesRoute(
                onOpenCity = { cityId -> citiesStack.add(CityWeatherKey(cityId)) },
                onOpenSearch = { citiesStack.add(CitySearchKey) },
            )
        }
        entry<CitySearchKey> { key ->
            CitySearchRoute(
                onBack = { citiesStack.popIfTop(key) },
                onCityAdded = { citiesStack.popIfTop(key) },
            )
        }
        entry<CityWeatherKey> { key ->
            CityWeatherRoute(cityId = key.cityId, onClose = { citiesStack.popIfTop(key) })
        }
        entry<SettingsKey> { SettingsRoute() }
    }

    val entriesByTab = TopLevelTab.entries.associateWith { tab ->
        rememberDecoratedNavEntries(
            backStack = backStacks.getValue(tab),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator<NavKey>(),
                // Each screen gets its own ViewModelStore, cleared when the screen is popped.
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider,
        )
    }

    // Weather is the home tab: other tabs are shown on top of it, so back from their root returns home.
    val weatherEntries = entriesByTab.getValue(TopLevelTab.WEATHER)
    val displayedEntries = if (currentTab == TopLevelTab.WEATHER) {
        weatherEntries
    } else {
        weatherEntries + entriesByTab.getValue(currentTab)
    }

    Scaffold(
        // The screens draw their own top bars and handle status bar insets.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                TopLevelTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == currentTab,
                        onClick = {
                            if (tab == currentTab) {
                                backStacks.getValue(tab).popToRoot()
                            } else {
                                currentTab = tab
                            }
                        },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavDisplay(
            entries = displayedEntries,
            onBack = {
                val stack = backStacks.getValue(currentTab)
                if (stack.size > 1) stack.removeLastOrNull() else currentTab = TopLevelTab.WEATHER
            },
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        )
    }
}

/** Pops [key] only if it is still on top (guards against double callbacks, e.g. effect + back press). */
private fun NavBackStack<NavKey>.popIfTop(key: NavKey) {
    if (lastOrNull() == key) removeLastOrNull()
}

private fun NavBackStack<NavKey>.popToRoot() {
    while (size > 1) removeLastOrNull()
}
