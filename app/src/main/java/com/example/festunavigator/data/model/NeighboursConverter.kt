package com.example.festunavigator.data.model

import androidx.room.TypeConverter

class NeighboursConverter {
    companion object {
        @TypeConverter
        @JvmStatic
        //TODO если строка пустая, то в map происходит ошибка
        fun storedStringToNeighbours(value: String): MutableList<Int> {
            return value
                .split(",")
                .filter { it != "" }
                .mapIfNotEmpty {
                    it.toInt()
                }
                .toMutableList()
        }

        @TypeConverter
        @JvmStatic
        fun neighboursToStoredString(list: MutableList<Int>): String {
            var value = ""
            for (id in list) value += "$id,"
            return value.dropLast(1)
        }
    }
}

public inline fun <T, R> List<T>.mapIfNotEmpty(transform: (T) -> R): List<R> {
    return when {
        size == 0  -> listOf()
        else -> map(transform)
    }
}