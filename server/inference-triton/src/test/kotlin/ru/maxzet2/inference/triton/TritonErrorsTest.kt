package ru.maxzet2.inference.triton

import io.grpc.Status
import kotlinx.coroutines.CancellationException
import ru.maxzet2.error.*
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class TritonErrorsTest {
    @Test
    fun `success is preserved`() {
        assertEquals("OK", tritonRequest { "OK" })
    }

    @Test
    fun `grpc checked and runtime failures are translated`() {
        assertFailsWith<InferenceUnavailableException> {
            tritonRequest { throw Status.UNAVAILABLE.asException() }
        }
        assertFailsWith<InferenceUnavailableException> {
            tritonRequest { throw Status.UNAVAILABLE.asRuntimeException() }
        }
        assertFailsWith<InferenceTimeoutException> {
            tritonRequest { throw Status.DEADLINE_EXCEEDED.asException() }
        }
        assertFailsWith<InferenceFailedException> {
            tritonRequest { throw Status.INTERNAL.asException() }
        }
        Unit
    }

    @Test
    fun `missing model is distinguished from other failures`() {
        assertFailsWith<ModelNotFoundException> {
            tritonRequest("missing") { throw Status.NOT_FOUND.asException() }
        }
        assertFailsWith<InferenceFailedException> {
            tritonRequest { throw Status.NOT_FOUND.asException() }
        }
        Unit
    }

    @Test
    fun `connection failure retains cause`() {
        val cause = IOException("connection refused")
        val error = assertFailsWith<InferenceUnavailableException> { tritonRequest { throw cause } }
        assertSame(cause, error.cause)
    }

    @Test
    fun `cancellation is propagated unchanged`() {
        val cancellation = CancellationException("cancelled")
        assertSame(cancellation, assertFailsWith<CancellationException> { tritonRequest { throw cancellation } })
    }
}
