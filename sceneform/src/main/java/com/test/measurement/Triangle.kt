package com.test.measurement

data class Vector(var x: Float, var y: Float, var z: Float)
data class Triangle(
    var objectVector: Vector,
    var previousCameraVector: Vector,
    var currentCameraVector: Vector
)