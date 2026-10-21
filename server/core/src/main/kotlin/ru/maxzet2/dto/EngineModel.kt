package ru.maxzet2.dto

import kotlinx.serialization.Serializable

@Serializable
data class EngineModel(
    val id: String,
    val state: String
)
