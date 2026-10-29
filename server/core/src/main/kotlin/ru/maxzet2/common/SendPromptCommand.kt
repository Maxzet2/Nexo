package ru.maxzet2.common

import kotlinx.serialization.Serializable

@Serializable
data class SendPromptCommand(
    val message: String,
    val model: String,
    val systemPrompt: String = "",
    val attachments: List<Attachments> = emptyList()
) {
}