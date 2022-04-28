package com.example.festunavigator.presentation.common.helpers

import android.content.Context
import com.example.festunavigator.R

class NumberLocationHelper(
    private val context: Context
) {

    operator fun invoke(number: String): String {
        var building = ""
        var floor = 0
        val floorStr = context.getString(R.string.floor)

        if (number.length == 1){
            building = context.getString(R.string.main)
            floor = 0
            return "$building, $floorStr$floor"
        }
        else if (number.length == 3 || number.length == 2){
            if (number[number.length - 2].digitToInt() > 4)
                building = context.getString(R.string.lab)
            else {
                building = context.getString(R.string.main)
            }
        }
        else if (number.length == 4){
            when (number[0]) {
                '1' -> {
                    building = context.getString(R.string.first_tower)
                }
                '2' -> {
                    building = context.getString(R.string.second_tower)
                }
                '3' -> {
                    building = context.getString(R.string.second_building)
                }
            }
        }
        else {
            return ""
        }

        floor = number[number.length - 3].digitToInt()
        return "$building, $floorStr$floor"
    }

}