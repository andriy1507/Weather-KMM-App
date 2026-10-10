@file:OptIn(ExperimentalForeignApi::class)

package dev.spaceapps.weather.core.database

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.posix.getenv

/** Validates migrations against the schemas exported to `sharedLogic/schemas`. */
class AppDatabaseMigrationTest {

    private val schemaDirectory = getenv("ROOM_SCHEMA_DIR")?.toKString()
        ?: error("ROOM_SCHEMA_DIR is not set; it is configured for KotlinNativeTest tasks in sharedLogic/build.gradle.kts")

    private val databasePath = NSTemporaryDirectory() + "migration-test.db"

    private fun helper() = MigrationTestHelper(
        schemaDirectoryPath = schemaDirectory,
        fileName = databasePath,
        driver = BundledSQLiteDriver(),
        databaseClass = AppDatabase::class,
        databaseFactory = { AppDatabaseConstructor.initialize() },
    )

    @AfterTest
    fun deleteDatabase() {
        listOf("", "-wal", "-shm").forEach {
            NSFileManager.defaultManager.removeItemAtPath(databasePath + it, error = null)
        }
    }

    @Test
    fun migrate1To2() = runTest {
        val helper = helper()
        helper.createDatabase(version = 1).close()

        // Validates the resulting schema against 2.json, then checks the data-level expectations.
        val connection = helper.runMigrationsAndValidate(version = 2, migrations = emptyList())
        try {
            assertFalse(connection.tableExists("current_weather"), "v1 cache table should be dropped")

            connection.execSQL(
                "INSERT INTO saved_city (name, state, countryCode, latitude, longitude, position, addedAtMillis) " +
                    "VALUES ('Lviv', NULL, 'UA', 49.8397, 24.0297, 0, 0)",
            )
            assertEquals(1L, connection.count("saved_city"))
            assertEquals(0L, connection.count("weather_cache"))
        } finally {
            connection.close()
            helper.finished()
        }
    }

    private fun SQLiteConnection.tableExists(name: String): Boolean =
        prepare("SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?").use { statement ->
            statement.bindText(1, name)
            statement.step()
        }

    private fun SQLiteConnection.count(table: String): Long =
        prepare("SELECT COUNT(*) FROM $table").use { statement ->
            statement.step()
            statement.getLong(0)
        }
}
