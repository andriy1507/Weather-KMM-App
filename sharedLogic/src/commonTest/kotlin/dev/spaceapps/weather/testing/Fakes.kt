package dev.spaceapps.weather.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dev.spaceapps.weather.feature.currentweather.data.local.CurrentWeatherDao
import dev.spaceapps.weather.feature.currentweather.data.local.CurrentWeatherEntity
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FixedClock(var now: Instant) : Clock {
    override fun now(): Instant = now
}

class FakeCurrentWeatherDao(initial: CurrentWeatherEntity? = null) : CurrentWeatherDao {
    val state = MutableStateFlow(initial)

    override fun observe(): Flow<CurrentWeatherEntity?> = state

    override suspend fun get(): CurrentWeatherEntity? = state.value

    override suspend fun upsert(entity: CurrentWeatherEntity) {
        state.value = entity
    }
}

class FakePreferencesDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow(emptyPreferences())

    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
        transform(state.value).also { state.value = it }
}
