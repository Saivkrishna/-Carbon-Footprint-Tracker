package com.carbonprint.app.data.local

import androidx.room.TypeConverter
import com.carbonprint.app.domain.model.TransportMode

/**
 * Type converters for Room Database persistence.
 */
class Converters {

    @TypeConverter
    fun fromTransportMode(mode: TransportMode?): String {
        return mode?.name ?: TransportMode.UNKNOWN.name
    }

    @TypeConverter
    fun toTransportMode(value: String?): TransportMode {
        return TransportMode.fromString(value)
    }
}
