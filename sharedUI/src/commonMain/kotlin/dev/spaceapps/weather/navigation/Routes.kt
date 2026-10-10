package dev.spaceapps.weather.navigation

import androidx.navigation3.runtime.NavKey
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import weatherapp.sharedui.generated.resources.Res
import weatherapp.sharedui.generated.resources.ic_cloud
import weatherapp.sharedui.generated.resources.ic_list
import weatherapp.sharedui.generated.resources.ic_settings
import weatherapp.sharedui.generated.resources.tab_cities
import weatherapp.sharedui.generated.resources.tab_settings
import weatherapp.sharedui.generated.resources.tab_weather

@Serializable
data object WeatherKey : NavKey

@Serializable
data object CitiesKey : NavKey

@Serializable
data object CitySearchKey : NavKey

@Serializable
data class CityWeatherKey(val cityId: Long) : NavKey

@Serializable
data object SettingsKey : NavKey

/** Bottom navigation tabs, each with its own back stack starting at [root]. */
enum class TopLevelTab(
    val root: NavKey,
    val label: StringResource,
    val icon: DrawableResource,
) {
    WEATHER(WeatherKey, Res.string.tab_weather, Res.drawable.ic_cloud),
    CITIES(CitiesKey, Res.string.tab_cities, Res.drawable.ic_list),
    SETTINGS(SettingsKey, Res.string.tab_settings, Res.drawable.ic_settings),
}

/** Back stacks are saved across process death; every route must be registered here. */
internal val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(WeatherKey::class, WeatherKey.serializer())
            subclass(CitiesKey::class, CitiesKey.serializer())
            subclass(CitySearchKey::class, CitySearchKey.serializer())
            subclass(CityWeatherKey::class, CityWeatherKey.serializer())
            subclass(SettingsKey::class, SettingsKey.serializer())
        }
    }
}
