package ru.maxzet2.inference.triton

import com.google.protobuf.ByteString
import inference.GRPCInferenceServiceGrpcKt
import inference.GrpcService
import io.grpc.ManagedChannelBuilder
import ru.maxzet2.common.LogitsOutput
import ru.maxzet2.common.ModelInput
import ru.maxzet2.common.ModelOutput
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.get
import kotlin.div
import kotlin.text.toInt

class GrpcTritonClient(
    host: String,
    port: Int,
) : AutoCloseable {

    private val channel = ManagedChannelBuilder
        .forAddress(host, port)
        .usePlaintext()
        .maxInboundMessageSize(256 * 1024 * 1024)
        .build()

    private val stub = GRPCInferenceServiceGrpcKt.GRPCInferenceServiceCoroutineStub(channel)

    suspend fun isReady(): Boolean = try {
        stub.serverReady(
            GrpcService.ServerReadyRequest.getDefaultInstance()
        ).ready
    } catch (e: Exception) {
        false
    }

    suspend fun infer(
        modelName: String,
        input: ModelInput
    ): ModelOutput {

        val sequenceLength = input.inputIds.size

        val inputIdsTensor = createInt64Tensor(
            name = "input_ids",
            values = input.inputIds,
            sequenceLength = sequenceLength
        )

        val attentionMaskTensor = createInt64Tensor(
            name = "attention_mask",
            values = input.attentionMask,
            sequenceLength = sequenceLength
        )

        val request = GrpcService.ModelInferRequest.newBuilder()
            .setModelName(modelName)
            .addInputs(inputIdsTensor)
            .addInputs(attentionMaskTensor)
            .build()

        val response = stub.modelInfer(request)

        return decodeOutput(response)
    }

    private fun createInt64Tensor(
        name: String,
        values: LongArray,
        sequenceLength: Int,
    ): GrpcService.ModelInferRequest.InferInputTensor {

        return GrpcService.ModelInferRequest.InferInputTensor.newBuilder()
            .setName(name)
            .setDatatype("INT64")
            .addShape(1)
            .addShape(sequenceLength.toLong())
            .setContents(
                GrpcService.InferTensorContents.newBuilder()
                    .addAllInt64Contents(values.toList())
                    .build()
            )
            .build()
    }

    private fun decodeOutput(
        response: GrpcService.ModelInferResponse
    ): ModelOutput {
        val output = response.outputsList.firstOrNull()
            ?: return ModelOutput(FloatArray(0))

        val raw = response.rawOutputContentsList.firstOrNull()
            ?: return ModelOutput(FloatArray(0))

        val buffer = raw.asReadOnlyByteBuffer()
            .order(ByteOrder.LITTLE_ENDIAN)

        val values = FloatArray(output.shapeList.fold(1L) { acc, value ->
            acc * value
        }.toInt())

        for (i in values.indices) {
            values[i] = buffer.float
        }

        return ModelOutput(values)
    }

    private val inputNamesCache = ConcurrentHashMap<String, List<String>>()

    private suspend fun inputNames(model: String): List<String> =
        inputNamesCache[model] ?: stub
            .modelMetadata(GrpcService.ModelMetadataRequest.newBuilder().setName(model).build())
            .inputsList.map { it.name }
            .also { inputNamesCache[model] = it }

    private fun LongArray.toBytes(): ByteString {
        val buf = ByteBuffer.allocate(size * 8).order(ByteOrder.LITTLE_ENDIAN)
        forEach { buf.putLong(it) }
        return ByteString.copyFrom(buf.array())
    }

    suspend fun inferLogits(modelName: String, input: ModelInput): LogitsOutput {
        val n = input.inputIds.size

        val available = mapOf(
            "input_ids" to input.inputIds,
            "attention_mask" to input.attentionMask,
            "token_type_ids" to LongArray(n),
            "position_ids" to LongArray(n) { it.toLong() },
        )

        val names = inputNames(modelName)
        val unknown = names.filter { it !in available }
        require(unknown.isEmpty()) {
            "Модель '$modelName' требует входы, которые мы не умеем формировать: $unknown"
        }

        val request = GrpcService.ModelInferRequest.newBuilder().setModelName(modelName)
        for (name in names) {
            request.addInputs(
                GrpcService.ModelInferRequest.InferInputTensor.newBuilder()
                    .setName(name)
                    .setDatatype("INT64")
                    .addShape(1).addShape(n.toLong())
            )
            request.addRawInputContents(available.getValue(name).toBytes())  // порядок = порядку inputs
        }
        request.addOutputs(GrpcService.ModelInferRequest.InferRequestedOutputTensor.newBuilder().setName("logits"))

        val response = stub.modelInfer(request.build())

        val shape = response.getOutputs(0).shapeList
        val vocab = shape.last().toInt()

        val floats = FloatArray(response.getRawOutputContents(0).size() / 4)
        response.getRawOutputContents(0)
            .asReadOnlyByteBuffer()
            .order(ByteOrder.LITTLE_ENDIAN)
            .asFloatBuffer()
            .get(floats)

        return LogitsOutput(floats, vocab)
    }

    override fun close() {
        channel.shutdown()
    }
}