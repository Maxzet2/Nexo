package ru.maxzet2.common

import kotlinx.serialization.Serializable

@Serializable
data class Attachments( // пока лежит никого не трогает
    val location: String,
    val contentType: ContentType
)
