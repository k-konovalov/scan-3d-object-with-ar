package com.arvrlab.reconstructcamera.core

data class FtpSettings(
    val url: String = "",
    val port: Int = 21,
    val user: String = "",
    val password: String = ""
)