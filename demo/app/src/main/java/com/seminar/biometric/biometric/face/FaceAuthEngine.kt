package com.seminar.biometric.biometric.face

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * State machine thuần Kotlin xử lý quality gate, Enrollment (gom 5 frame) và Verification 1:1.
 */
class FaceAuthEngine(
    private val templateStore: FaceTemplateStore,
    private val requiredEnrollmentFrames: Int = DEFAULT_REQUIRED_ENROLLMENT_FRAMES,
    private val maxConsecutiveMismatches: Int = DEFAULT_MAX_CONSECUTIVE_MISMATCHES
) {
    private val collectedEmbeddings = mutableListOf<FloatArray>()
    private var consecutiveMismatches = 0

    fun resetSession() {
        collectedEmbeddings.clear()
        consecutiveMismatches = 0
    }

    fun processEnrollmentSample(username: String, sample: DetectedFaceSample?): FaceEnrollmentUpdate {
        val qualityIssue = validateSampleQuality(sample)
        if (qualityIssue != null) {
            return FaceEnrollmentUpdate.QualityRejected(
                issue = qualityIssue,
                collectedFrames = collectedEmbeddings.size,
                requiredFrames = requiredEnrollmentFrames
            )
        }

        val embedding = sample!!.embedding!!
        collectedEmbeddings.add(embedding.copyOf())

        if (collectedEmbeddings.size < requiredEnrollmentFrames) {
            return FaceEnrollmentUpdate.FrameCollected(
                collectedFrames = collectedEmbeddings.size,
                requiredFrames = requiredEnrollmentFrames
            )
        }

        val averaged = averageEmbeddings(collectedEmbeddings)
        val normalized = l2Normalize(averaged)
        templateStore.saveTemplate(username, normalized)
        collectedEmbeddings.clear()

        return FaceEnrollmentUpdate.Completed(
            username = username,
            template = normalized
        )
    }

    fun processVerificationSample(username: String, sample: DetectedFaceSample?): FaceVerificationUpdate {
        val template = templateStore.getTemplate(username)
            ?: return FaceVerificationUpdate.NotEnrolled

        val qualityIssue = validateSampleQuality(sample)
        if (qualityIssue != null) {
            return FaceVerificationUpdate.QualityRejected(issue = qualityIssue)
        }

        val probe = l2Normalize(sample!!.embedding!!)
        val normalizedTemplate = l2Normalize(template)
        val score = cosineSimilarity(probe, normalizedTemplate)
        val threshold = templateStore.getThreshold()

        if (score >= threshold) {
            consecutiveMismatches = 0
            return FaceVerificationUpdate.Matched(score = score, threshold = threshold)
        }

        consecutiveMismatches += 1
        return if (consecutiveMismatches >= maxConsecutiveMismatches) {
            consecutiveMismatches = 0
            FaceVerificationUpdate.Failed(lastScore = score, threshold = threshold)
        } else {
            FaceVerificationUpdate.MismatchRetry(
                score = score,
                threshold = threshold,
                consecutiveMismatches = consecutiveMismatches,
                maxMismatches = maxConsecutiveMismatches
            )
        }
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
        }
        return dot
    }

    private fun averageEmbeddings(vectors: List<FloatArray>): FloatArray {
        val dim = vectors.first().size
        val sum = FloatArray(dim)
        for (vec in vectors) {
            for (i in 0 until dim) {
                sum[i] += vec[i]
            }
        }
        val count = vectors.size.toFloat()
        for (i in 0 until dim) {
            sum[i] /= count
        }
        return sum
    }

    private fun l2Normalize(vector: FloatArray): FloatArray {
        var normSq = 0f
        for (v in vector) {
            normSq += v * v
        }
        val norm = sqrt(normSq)
        if (norm <= 1e-12f) return vector.copyOf()
        return FloatArray(vector.size) { i -> vector[i] / norm }
    }

    private fun validateSampleQuality(sample: DetectedFaceSample?): FaceQualityIssue? {
        if (sample == null) return FaceQualityIssue.NO_FACE
        if (sample.boundingBoxWidthRatio < MIN_FACE_WIDTH_RATIO) return FaceQualityIssue.FACE_TOO_SMALL
        if (
            abs(sample.headEulerAngleX) > MAX_HEAD_ANGLE_DEGREES ||
            abs(sample.headEulerAngleY) > MAX_HEAD_ANGLE_DEGREES ||
            abs(sample.headEulerAngleZ) > MAX_HEAD_ANGLE_DEGREES
        ) {
            return FaceQualityIssue.FACE_NOT_FRONTAL
        }
        val leftOpen = sample.leftEyeOpenProbability ?: 0f
        val rightOpen = sample.rightEyeOpenProbability ?: 0f
        if (leftOpen < MIN_EYE_OPEN_PROBABILITY || rightOpen < MIN_EYE_OPEN_PROBABILITY) {
            return FaceQualityIssue.EYES_CLOSED
        }
        if (sample.embedding == null || sample.embedding.isEmpty()) {
            return FaceQualityIssue.EMBEDDING_MISSING
        }
        return null
    }

    companion object {
        const val DEFAULT_REQUIRED_ENROLLMENT_FRAMES = 5
        const val DEFAULT_MAX_CONSECUTIVE_MISMATCHES = 4
        const val MIN_FACE_WIDTH_RATIO = 0.22f
        const val MAX_HEAD_ANGLE_DEGREES = 15f
        const val MIN_EYE_OPEN_PROBABILITY = 0.5f
    }
}
