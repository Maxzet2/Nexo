package ru.maxzet2.inference.triton

import io.grpc.Status
import io.grpc.StatusException
import io.grpc.StatusRuntimeException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import ru.maxzet2.error.*
import java.io.IOException

internal fun <T> tritonRequest(modelName: String? = null, block: () -> T): T = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: StatusException) {
    throw grpcFailure(error.status, error, modelName)
} catch (error: StatusRuntimeException) {
    throw grpcFailure(error.status, error, modelName)
} catch (error: HttpRequestTimeoutException) {
    throw InferenceTimeoutException(error)
} catch (error: ResponseException) {
    throw if (error.response.status.value == 503) InferenceUnavailableException(error)
    else InferenceFailedException(error)
} catch (error: IOException) {
    throw InferenceUnavailableException(error)
} catch (error: SerializationException) {
    throw InferenceFailedException(error)
}

private fun grpcFailure(status: Status, cause: Throwable, modelName: String?): ServiceException =
    when (status.code) {
        Status.Code.NOT_FOUND -> if (modelName != null) ModelNotFoundException(modelName) else InferenceFailedException(cause)
        Status.Code.UNAVAILABLE -> InferenceUnavailableException(cause)
        Status.Code.DEADLINE_EXCEEDED -> InferenceTimeoutException(cause)
        else -> InferenceFailedException(cause)
    }
