package com.seminar.biometric.biometric.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceVerificationEngineTest {

    private fun validSample(embedding: FloatArray) = DetectedFaceSample(
        boundingBoxWidthRatio = 0.45f,
        leftEyeOpenProbability = 0.95f,
        rightEyeOpenProbability = 0.95f,
        headEulerAngleX = 0f,
        headEulerAngleY = 0f,
        headEulerAngleZ = 0f,
        embedding = embedding
    )

    @Test
    fun `verification returns Matched when cosine similarity reaches threshold`() {
        val store = InMemoryFaceTemplateStore().apply {
            saveTemplate("sinhvien@seminar.vn", floatArrayOf(0.6f, 0.8f))
            saveThreshold(0.75f)
        }
        val engine = FaceAuthEngine(templateStore = store)

        // Cosine similarity of [0.6, 0.8] and [1.0, 0.0] = 0.60 < 0.75
        val below = engine.processVerificationSample("sinhvien@seminar.vn", validSample(floatArrayOf(1f, 0f)))
        assertTrue(below is FaceVerificationUpdate.MismatchRetry)
        assertEquals(0.60f, (below as FaceVerificationUpdate.MismatchRetry).score, 0.001f)
        assertEquals(1, below.consecutiveMismatches)

        // Cosine similarity of [0.6, 0.8] and [0.8, 0.6] = 0.48 + 0.48 = 0.96 >= 0.75
        val matched = engine.processVerificationSample("sinhvien@seminar.vn", validSample(floatArrayOf(0.8f, 0.6f)))
        assertTrue(matched is FaceVerificationUpdate.Matched)
        assertEquals(0.96f, (matched as FaceVerificationUpdate.Matched).score, 0.001f)
        assertEquals(0.75f, matched.threshold, 0.001f)
    }

    @Test
    fun `verification ignores quality rejections and fails after 4 consecutive mismatches`() {
        val store = InMemoryFaceTemplateStore().apply {
            saveTemplate("sinhvien@seminar.vn", floatArrayOf(1f, 0f))
            saveThreshold(0.75f)
        }
        val engine = FaceAuthEngine(templateStore = store, maxConsecutiveMismatches = 4)

        val closedEyes = DetectedFaceSample(
            boundingBoxWidthRatio = 0.45f,
            leftEyeOpenProbability = 0.1f,
            rightEyeOpenProbability = 0.9f,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            embedding = floatArrayOf(0f, 1f)
        )
        val imposter = validSample(floatArrayOf(0f, 1f)) // score = 0.0

        assertTrue(engine.processVerificationSample("sinhvien@seminar.vn", closedEyes) is FaceVerificationUpdate.QualityRejected)
        assertEquals(1, (engine.processVerificationSample("sinhvien@seminar.vn", imposter) as FaceVerificationUpdate.MismatchRetry).consecutiveMismatches)
        assertEquals(2, (engine.processVerificationSample("sinhvien@seminar.vn", imposter) as FaceVerificationUpdate.MismatchRetry).consecutiveMismatches)
        assertEquals(3, (engine.processVerificationSample("sinhvien@seminar.vn", imposter) as FaceVerificationUpdate.MismatchRetry).consecutiveMismatches)

        val fourth = engine.processVerificationSample("sinhvien@seminar.vn", imposter)
        assertTrue(fourth is FaceVerificationUpdate.Failed)
        assertEquals(0f, (fourth as FaceVerificationUpdate.Failed).lastScore, 0.001f)
    }

    @Test
    fun `verification returns NotEnrolled when user has no template and responds dynamically to threshold changes`() {
        val store = InMemoryFaceTemplateStore().apply {
            saveThreshold(0.80f)
        }
        val engine = FaceAuthEngine(templateStore = store)

        assertTrue(engine.processVerificationSample("missing@seminar.vn", validSample(floatArrayOf(1f, 0f))) is FaceVerificationUpdate.NotEnrolled)

        store.saveTemplate("sinhvien@seminar.vn", floatArrayOf(1f, 0f))
        val borderline = validSample(floatArrayOf(0.7071f, 0.7071f)) // cosine similarity ~= 0.7071

        assertTrue(engine.processVerificationSample("sinhvien@seminar.vn", borderline) is FaceVerificationUpdate.MismatchRetry)

        // Lowering threshold simulates higher FAR / lower FRR
        store.saveThreshold(0.65f)
        val matched = engine.processVerificationSample("sinhvien@seminar.vn", borderline)
        assertTrue(matched is FaceVerificationUpdate.Matched)
        assertEquals(0.7071f, (matched as FaceVerificationUpdate.Matched).score, 0.001f)
        assertEquals(0.65f, matched.threshold, 0.001f)
    }
}
