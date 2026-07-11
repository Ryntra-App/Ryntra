package com.rinthy.shared.network

class ApiException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)
