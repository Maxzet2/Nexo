package ru.maxzet2.common

import kotlinx.serialization.Serializable

@Serializable
data class ModelOutput(
    val logits: FloatArray
)
