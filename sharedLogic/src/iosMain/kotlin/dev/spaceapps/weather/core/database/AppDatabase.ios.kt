package dev.spaceapps.weather.core.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import dev.spaceapps.weather.core.util.documentDirectoryPath

fun appDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> =
    Room.databaseBuilder<AppDatabase>(
        name = "${documentDirectoryPath()}/$DATABASE_FILE_NAME",
    )
