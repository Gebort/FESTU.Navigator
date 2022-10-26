package com.example.festunavigator.domain.pathfinding.path_restoring

import kotlin.math.pow

class LinearRegressionModel(
    private val independentVariables: List<Float>,
    private val dependentVariables: List<Float>,
) {

    private val meanX: Float = independentVariables.sum().div(independentVariables.count())
    private val meanY: Float = dependentVariables.sum().div(dependentVariables.count())
    private val variance: Float = independentVariables.asSequence().map { (it - meanX).pow(2) }.sum()
    private val covariance = covariance()
    private val b1 = covariance.div(variance)
    private val b0 = meanY - b1 * meanX

    private fun covariance(): Float {
        var covariance = 0f
        for (i in independentVariables.indices) {
            val xPart = independentVariables[i] - meanX
            val yPart = dependentVariables[i] - meanY
            covariance += xPart * yPart
        }
        return covariance
    }

    fun predict(independentVariable: Float) = b0 + b1 * independentVariable

}