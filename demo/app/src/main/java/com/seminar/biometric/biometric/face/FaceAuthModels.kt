package com.seminar.biometric.biometric.face

/**
 * Đặc trưng khuôn mặt đã phát hiện trên 1 frame camera.
 */
data class DetectedFaceSample(
    val boundingBoxWidthRatio: Float,
    val leftEyeOpenProbability: Float?,
    val rightEyeOpenProbability: Float?,
    val headEulerAngleX: Float,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,
    val embedding: FloatArray?
)

enum class FaceQualityIssue(val guidanceMessage: String) {
    NO_FACE("Không tìm thấy khuôn mặt. Hãy đưa mặt vào khung hình."),
    MULTIPLE_FACES("Chỉ để một khuôn mặt trong khung hình."),
    FACE_TOO_SMALL("Đưa khuôn mặt lại gần camera hơn."),
    FACE_NOT_FRONTAL("Giữ khuôn mặt nhìn thẳng vào camera."),
    EYES_CLOSED("Vui lòng mở mắt tự nhiên khi quét."),
    EMBEDDING_MISSING("Chưa trích xuất được đặc trưng khuôn mặt.")
}

sealed class FaceEnrollmentUpdate {
    data class QualityRejected(
        val issue: FaceQualityIssue,
        val collectedFrames: Int,
        val requiredFrames: Int
    ) : FaceEnrollmentUpdate()

    data class FrameCollected(
        val collectedFrames: Int,
        val requiredFrames: Int
    ) : FaceEnrollmentUpdate()

    data class Completed(
        val username: String,
        val template: FloatArray
    ) : FaceEnrollmentUpdate()
}

sealed class FaceVerificationUpdate {
    object NotEnrolled : FaceVerificationUpdate()

    data class QualityRejected(
        val issue: FaceQualityIssue
    ) : FaceVerificationUpdate()

    data class MismatchRetry(
        val score: Float,
        val threshold: Float,
        val consecutiveMismatches: Int,
        val maxMismatches: Int
    ) : FaceVerificationUpdate()

    data class Matched(
        val score: Float,
        val threshold: Float
    ) : FaceVerificationUpdate()

    data class Failed(
        val lastScore: Float,
        val threshold: Float
    ) : FaceVerificationUpdate()
}

interface FaceTemplateStore {
    fun hasTemplate(username: String): Boolean
    fun saveTemplate(username: String, embedding: FloatArray)
    fun getTemplate(username: String): FloatArray?
    fun getThreshold(defaultThreshold: Float = DEFAULT_THRESHOLD): Float
    fun saveThreshold(threshold: Float)

    companion object {
        const val DEFAULT_THRESHOLD = 0.75f
    }
}
