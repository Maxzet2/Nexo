package ru.maxzet2.common

import kotlinx.serialization.Serializable

@Serializable
enum class ContentType { // пока не знаю стоит ли добавлять поддержку отдельных языков
    TXT,
    PDF,
    CODE,
    BIN,
    IMAGE
}