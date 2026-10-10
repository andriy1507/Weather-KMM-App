package dev.spaceapps.weather.feature.cities.data.local

import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.PrimaryKey

/**
 * A city the user added from search. Coordinates are rounded before insert so the unique index
 * rejects the same place added twice.
 */
@Entity(
    tableName = "saved_city",
    indices = [Index("latitude", "longitude", unique = true)],
)
data class SavedCityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** State/region, when the geocoder returns one (e.g. US states). */
    val state: String?,
    val countryCode: String,
    val latitude: Double,
    val longitude: Double,
    /** Display order. Kept for future drag-to-reorder; currently the order of addition. */
    val position: Int,
    val addedAtMillis: Long,
)
