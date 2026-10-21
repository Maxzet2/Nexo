package ru.maxzet2.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    val code: String,
    val message: String
)
