package com.test.measurement

data class Vector(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f)
data class Triangle(
    var objectVector: Vector = Vector(),
    var previousCameraVector: Vector = Vector(),
    var currentCameraVector: Vector = Vector()
)