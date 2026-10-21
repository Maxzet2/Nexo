package ru.maxzet2.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModelInfo( // пока тоже по плану. может позже будет убран
    val id: String,
    val name: String,
    val status: String,
    val type: String
)
