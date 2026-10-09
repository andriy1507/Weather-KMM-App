package dev.spaceapps.weather.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import dev.spaceapps.weather.core.util.documentDirectoryPath

fun createDataStore(): DataStore<Preferences> = createDataStore(
    producePath = { "${documentDirectoryPath()}/$DATASTORE_FILE_NAME" },
)
