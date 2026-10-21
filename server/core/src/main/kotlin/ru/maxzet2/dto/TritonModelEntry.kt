package ru.maxzet2.dto

import kotlinx.serialization.Serializable

@Serializable
data class TritonModelEntry(
    val name: String,
    val version: String? = null,
    val state: String? = null,
    val reason: String? = null,
)