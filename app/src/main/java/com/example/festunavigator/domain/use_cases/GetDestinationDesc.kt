package com.example.festunavigator.domain.use_cases

import android.content.Context
import com.gerbort.app.R

class GetDestinationDesc {

    operator fun invoke(number: String, context: Context): String {

        var building = ""
        var floor = 0
        val floorStr = context.getString(R.string.floor)
        val tnumber = if (number.last().isDigit()) number else number.dropLast(1)

        if (tnumber.length == 1){
            building = context.getString(R.string.main)
            floor = 0
            return "$building, $floorStr$floor"
        }
        else if (tnumber.length == 3 || tnumber.length == 2){
            if (tnumber[tnumber.length - 2].digitToInt() > 4)
                building = context.getString(R.string.lab)
            else {
                building = context.getString(R.string.main)
            }
            if (tnumber.length == 2 ){
                floor = 0
                return "$building, $floorStr$floor"
            }
        }
        else if (tnumber.length == 4){
            when (tnumber[0]) {
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

        floor = tnumber[tnumber.length - 3].digitToInt()
        return "$building, $floorStr$floor"
    }

}