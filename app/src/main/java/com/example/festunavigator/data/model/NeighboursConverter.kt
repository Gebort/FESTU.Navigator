package com.example.festunavigator.data.model

import androidx.room.TypeConverter
import com.google.ar.sceneform.math.Vector3

class NeighboursConverter {
    companion object {
        @TypeConverter
        @JvmStatic
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

        @TypeConverter
        @JvmStatic
        fun storedStringtoVector3(value: String): Vector3? {
            return if (value == ""){
                null
            }
            else {
                val data = value.split(" ").map { it.toFloat() }
                Vector3(data[0], data[1], data[2])
            }
        }

        @TypeConverter
        @JvmStatic
        fun vector3ToString(value: Vector3?): String {
            return if (value == null) "" else "${value.x} ${value.y} ${value.z}"
        }
    }
}

inline fun <T, R> List<T>.mapIfNotEmpty(transform: (T) -> R): List<R> {
    return when (size) {
        0 -> listOf()
        else -> map(transform)
    }
}