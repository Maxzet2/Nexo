package ru.maxzet2.common

import kotlinx.serialization.Serializable

@Serializable
data class EditMessageRequest(
    val message: String,
    val model: String,
)
