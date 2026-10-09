package dev.spaceapps.weather.core.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

fun appDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
    )
}
