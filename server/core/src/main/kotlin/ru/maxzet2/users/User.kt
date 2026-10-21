package ru.maxzet2.users

import io.ktor.util.Hash
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class User @OptIn(ExperimentalUuidApi::class) constructor(
    val id: Uuid,
    val name : String,
//    val password : Hash, пока не беспокоимся насчет безопасности, безопасность настроим позже
)
