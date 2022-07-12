package com.example.festunavigator.data.utils

sealed class Reaction<out T>{
    data class Success<out T>(val data: T): Reaction<T>()
    data class Error(val error: Exception): Reaction<Nothing>()
}