package com.gerbort.database.converters

import androidx.room.TypeConverter

class NeighboursConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        fun storedStringToNeighbours(value: String): MutableList<Int> {
            return value
                .split(",")
                .map { it.toInt() }
                .toMutableList()
        }

        @TypeConverter
        @JvmStatic
        fun neighboursToStoredString(list: MutableList<Int>): String {
            return list.joinToString(",")
        }
    }
}