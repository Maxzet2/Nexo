package ru.maxzet2.error

sealed class ServiceException(
    val code: String,
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)

class InvalidRequestException(message: String) : ServiceException("invalid_request", message)
class ChatBusyException : ServiceException("chat_busy", "В этом чате уже идёт генерация")
class ChatNotFoundException(id: String) : ServiceException("chat_not_found", "Чат $id не найден")
class MessageNotFoundException(id: String) : ServiceException("message_not_found", "Сообщение $id не найдено")
class ModelNotFoundException(name: String) : ServiceException("model_not_found", "Модель '$name' не найдена")
class InferenceUnavailableException(cause: Throwable) :
    ServiceException("inference_unavailable", "Inference service is unavailable", cause)
class InferenceTimeoutException(cause: Throwable) :
    ServiceException("inference_timeout", "Inference service timed out", cause)
class InferenceFailedException(cause: Throwable) :
    ServiceException("inference_failed", "Inference service failed", cause)
