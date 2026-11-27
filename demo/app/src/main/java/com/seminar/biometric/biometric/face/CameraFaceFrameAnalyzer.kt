package com.seminar.biometric.biometric.face

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean

data class NormalizedFaceBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class FaceFrameAnalysis(
    val faceCount: Int,
    val sample: DetectedFaceSample?,
    val mirroredNormalizedBox: NormalizedFaceBox?
)

/**
 * Bộ phân tích CameraX sử dụng ML Kit Face Detection + MobileFaceNetEmbedder.
 */
class CameraFaceFrameAnalyzer(
    private val embedder: MobileFaceNetEmbedder,
    private val onFrameAnalyzed: (FaceFrameAnalysis) -> Unit
) : ImageAnalysis.Analyzer, Closeable {

    private val isProcessing = AtomicBoolean(false)

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.18f)
            .build()
    )

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !isProcessing.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }

        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)
        val rotatedWidth = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.height else imageProxy.width
        val rotatedHeight = if (rotationDegrees == 90 || rotationDegrees == 270) imageProxy.width else imageProxy.height

        detector.process(inputImage)
            .addOnSuccessListener { faces ->
                val result = buildAnalysisResult(
                    imageProxy = imageProxy,
                    rotationDegrees = rotationDegrees,
                    frameWidth = rotatedWidth,
                    frameHeight = rotatedHeight,
                    faces = faces
                )
                onFrameAnalyzed(result)
            }
            .addOnFailureListener {
                onFrameAnalyzed(FaceFrameAnalysis(faceCount = 0, sample = null, mirroredNormalizedBox = null))
            }
            .addOnCompleteListener {
                isProcessing.set(false)
                imageProxy.close()
            }
    }

    private fun buildAnalysisResult(
        imageProxy: ImageProxy,
        rotationDegrees: Int,
        frameWidth: Int,
        frameHeight: Int,
        faces: List<Face>
    ): FaceFrameAnalysis {
        if (faces.isEmpty()) {
            return FaceFrameAnalysis(faceCount = 0, sample = null, mirroredNormalizedBox = null)
        }

        val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
            ?: return FaceFrameAnalysis(faceCount = 0, sample = null, mirroredNormalizedBox = null)

        val box = primaryFace.boundingBox
        val safeW = frameWidth.coerceAtLeast(1).toFloat()
        val safeH = frameHeight.coerceAtLeast(1).toFloat()

        // Preview camera trước hiển thị dạng gương (mirror theo trục X)
        val mirroredBox = NormalizedFaceBox(
            left = (1f - box.right / safeW).coerceIn(0f, 1f),
            top = (box.top / safeH).coerceIn(0f, 1f),
            right = (1f - box.left / safeW).coerceIn(0f, 1f),
            bottom = (box.bottom / safeH).coerceIn(0f, 1f)
        )

        val rotatedBitmap = rotateBitmap(imageProxy.toBitmap(), rotationDegrees)
        val embedding = try {
            embedder.extractEmbedding(rotatedBitmap, box)
        } finally {
            rotatedBitmap.recycle()
        }

        val sample = DetectedFaceSample(
            boundingBoxWidthRatio = box.width() / safeW,
            leftEyeOpenProbability = primaryFace.leftEyeOpenProbability,
            rightEyeOpenProbability = primaryFace.rightEyeOpenProbability,
            headEulerAngleX = primaryFace.headEulerAngleX,
            headEulerAngleY = primaryFace.headEulerAngleY,
            headEulerAngleZ = primaryFace.headEulerAngleZ,
            embedding = embedding
        )

        return FaceFrameAnalysis(
            faceCount = faces.size,
            sample = sample,
            mirroredNormalizedBox = mirroredBox
        )
    }

    private fun rotateBitmap(source: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return source
        val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated !== source) {
            source.recycle()
        }
        return rotated
    }

    override fun close() {
        detector.close()
    }
}
