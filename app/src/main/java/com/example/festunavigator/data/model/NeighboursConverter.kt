package com.example.festunavigator.data.model

import androidx.room.TypeConverter
import com.google.ar.sceneform.math.Vector3
import dev.romainguy.kotlin.math.Quaternion

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
        fun storedStringtoQuaternion(value: String): Quaternion? {
            return if (value == ""){
                null
            }
            else {
                val data = value.split(" ").map { it.toFloat() }
                Quaternion(data[0], data[1], data[2], data[3])
            }
        }

        @TypeConverter
        @JvmStatic
        fun quaternionToString(value: Quaternion?): String {
            return if (value == null) "" else "${value.x} ${value.y} ${value.z} ${value.w}"
        }
    }
}

inline fun <T, R> List<T>.mapIfNotEmpty(transform: (T) -> R): List<R> {
    return when (size) {
        0 -> listOf()
        else -> map(transform)
    }
}