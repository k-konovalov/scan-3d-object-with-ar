package com.test.measurement

import kotlin.math.acos
import kotlin.math.pow
import kotlin.math.sqrt

enum class Angles {
    ZERO,
    FORTY_FIVE,
    EIGHTY_FIVE
}

data class Vector(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f)
data class Triangle(
    var objectVector: Vector = Vector(),
    var previousCameraVector: Vector = Vector(),
    var currentCameraVector: Vector = Vector(),
    var modelAngle: Angles = Angles.ZERO,
    var currentHeight: Int = 0,
    var modelHeight: Int = 0,
    var currentWidth: Int = 0,
    var modelWidth: Int = 0,
    var currentDistance: Float = 0f,
    var modelDistance: Int = 0,
    var currentAngle: Int = 0
)

/**
 * рассчитываем по формуле: угол A = arccos(cos BC), где cos BC =  скалярное произведение векторов на произведение векторов
 *
 * @param triangle Triangle - (положение объекта, начальная точка съемки, текущее положение камеры)
 * @return Int - значение угла в градусах
 */
fun Triangle.calculateABAngle(): Int {
    // скалярное произведение координатным способом (перемножение координат двух веторов)
    val scalarMultiplication = previousCameraVector.x * currentCameraVector.x + previousCameraVector.y * previousCameraVector.y + previousCameraVector.z * previousCameraVector.z

    val currentVector = sqrt(currentCameraVector.x.pow(2) + previousCameraVector.y.pow(2) + previousCameraVector.z.pow(2))
    val previousVector = sqrt(previousCameraVector.x.pow(2) +previousCameraVector.y.pow(2) + previousCameraVector.z.pow(2))

    val angleCos = scalarMultiplication / (previousVector * currentVector)

    // умножаем на 180 / Math.PI, чтобы перевести радианы в градусы
    val angle = acos(angleCos) * 180 / Math.PI
    return angle.toInt()
}
