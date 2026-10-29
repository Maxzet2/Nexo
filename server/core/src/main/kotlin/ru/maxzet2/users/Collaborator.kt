package ru.maxzet2.users

import ru.maxzet2.common.Access
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class Collaborator @OptIn(ExperimentalUuidApi::class) constructor( // пока остается в планах
    val user: User,
    val chatId: Uuid,
    val access: Access
)