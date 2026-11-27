package com.seminar.biometric.biometric.face

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import org.tensorflow.lite.Interpreter
import java.io.Closeable
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

/**
 * Wrapper cho model TFLite MobileFaceNet (assets/mobile_face_net.tflite)
 * trích xuất vector đặc trưng khuôn mặt từ ảnh Bitmap đã cắt.
 */
class MobileFaceNetEmbedder(
    context: Context,
    modelAssetName: String = DEFAULT_MODEL_ASSET
) : Closeable {

    private val interpreter: Interpreter
    private val inputWidth: Int
    private val inputHeight: Int
    private val embeddingDimension: Int

    init {
        val modelBuffer = loadModelFile(context, modelAssetName)
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        interpreter = Interpreter(modelBuffer, options)

        val inputShape = interpreter.getInputTensor(0).shape() // [1, H, W, 3]
        inputHeight = if (inputShape.size >= 3) inputShape[1] else DEFAULT_INPUT_SIZE
        inputWidth = if (inputShape.size >= 3) inputShape[2] else DEFAULT_INPUT_SIZE

        val outputShape = interpreter.getOutputTensor(0).shape() // [1, D]
        embeddingDimension = if (outputShape.size >= 2) outputShape[1] else DEFAULT_EMBEDDING_DIM
    }

    fun extractEmbedding(frameBitmap: Bitmap, faceBoundingBox: Rect): FloatArray? {
        val cropped = cropFaceWithPadding(frameBitmap, faceBoundingBox) ?: return null
        val scaled = Bitmap.createScaledBitmap(cropped, inputWidth, inputHeight, true)
        if (scaled !== cropped) {
            cropped.recycle()
        }

        val inputBuffer = convertBitmapToFloatBuffer(scaled)
        scaled.recycle()

        val output = Array(1) { FloatArray(embeddingDimension) }
        interpreter.run(inputBuffer, output)
        return output[0]
    }

    private fun cropFaceWithPadding(bitmap: Bitmap, box: Rect): Bitmap? {
        val padX = (box.width() * 0.15f).toInt()
        val padY = (box.height() * 0.15f).toInt()

        val left = max(0, box.left - padX)
        val top = max(0, box.top - padY)
        val right = min(bitmap.width, box.right + padX)
        val bottom = min(bitmap.height, box.bottom + padY)

        val width = right - left
        val height = bottom - top
        if (width <= 0 || height <= 0) return null

        return Bitmap.createBitmap(bitmap, left, top, width, height)
    }

    private fun convertBitmapToFloatBuffer(bitmap: Bitmap): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(4 * 1 * inputHeight * inputWidth * 3)
        buffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputWidth * inputHeight)
        bitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)

        for (pixel in pixels) {
            val r = ((pixel shr 16) and 0xFF)
            val g = ((pixel shr 8) and 0xFF)
            val b = (pixel and 0xFF)

            buffer.putFloat((r - IMAGE_MEAN) / IMAGE_STD)
            buffer.putFloat((g - IMAGE_MEAN) / IMAGE_STD)
            buffer.putFloat((b - IMAGE_MEAN) / IMAGE_STD)
        }
        buffer.rewind()
        return buffer
    }

    private fun loadModelFile(context: Context, assetName: String): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(assetName)
        FileInputStream(assetFileDescriptor.fileDescriptor).use { inputStream ->
            val fileChannel = inputStream.channel
            return fileChannel.map(
                FileChannel.MapMode.READ_ONLY,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.declaredLength
            )
        }
    }

    override fun close() {
        interpreter.close()
    }

    companion object {
        const val DEFAULT_MODEL_ASSET = "mobile_face_net.tflite"
        private const val DEFAULT_INPUT_SIZE = 112
        private const val DEFAULT_EMBEDDING_DIM = 192
        private const val IMAGE_MEAN = 127.5f
        private const val IMAGE_STD = 128.0f
    }
}
