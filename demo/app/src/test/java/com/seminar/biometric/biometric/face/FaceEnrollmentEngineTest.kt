package com.seminar.biometric.biometric.face

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FaceEnrollmentEngineTest {

    @Test
    fun `enrollment rejects frames when eyes are closed, head is turned, or face is too small`() {
        val store = InMemoryFaceTemplateStore()
        val engine = FaceAuthEngine(templateStore = store)

        val closedEyes = DetectedFaceSample(
            boundingBoxWidthRatio = 0.45f,
            leftEyeOpenProbability = 0.2f,
            rightEyeOpenProbability = 0.9f,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            embedding = floatArrayOf(1f, 0f)
        )
        val turnedHead = DetectedFaceSample(
            boundingBoxWidthRatio = 0.45f,
            leftEyeOpenProbability = 0.95f,
            rightEyeOpenProbability = 0.95f,
            headEulerAngleX = 0f,
            headEulerAngleY = 22f,
            headEulerAngleZ = 0f,
            embedding = floatArrayOf(1f, 0f)
        )
        val tooSmall = DetectedFaceSample(
            boundingBoxWidthRatio = 0.15f,
            leftEyeOpenProbability = 0.95f,
            rightEyeOpenProbability = 0.95f,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            embedding = floatArrayOf(1f, 0f)
        )

        val r1 = engine.processEnrollmentSample("sinhvien@seminar.vn", closedEyes)
        val r2 = engine.processEnrollmentSample("sinhvien@seminar.vn", turnedHead)
        val r3 = engine.processEnrollmentSample("sinhvien@seminar.vn", tooSmall)

        assertTrue(r1 is FaceEnrollmentUpdate.QualityRejected && r1.issue == FaceQualityIssue.EYES_CLOSED)
        assertTrue(r2 is FaceEnrollmentUpdate.QualityRejected && r2.issue == FaceQualityIssue.FACE_NOT_FRONTAL)
        assertTrue(r3 is FaceEnrollmentUpdate.QualityRejected && r3.issue == FaceQualityIssue.FACE_TOO_SMALL)
        assertEquals(0, (r3 as FaceEnrollmentUpdate.QualityRejected).collectedFrames)
    }

    @Test
    fun `enrollment averages 5 valid frames, L2 normalizes embedding, and isolates per username`() {
        val store = InMemoryFaceTemplateStore()
        val engine = FaceAuthEngine(templateStore = store)

        fun validSample(embedding: FloatArray) = DetectedFaceSample(
            boundingBoxWidthRatio = 0.45f,
            leftEyeOpenProbability = 0.95f,
            rightEyeOpenProbability = 0.95f,
            headEulerAngleX = 0f,
            headEulerAngleY = 0f,
            headEulerAngleZ = 0f,
            embedding = embedding
        )

        val u1 = engine.processEnrollmentSample("userA@seminar.vn", validSample(floatArrayOf(1f, 0f)))
        val u2 = engine.processEnrollmentSample("userA@seminar.vn", validSample(floatArrayOf(1f, 0f)))
        val u3 = engine.processEnrollmentSample("userA@seminar.vn", validSample(floatArrayOf(1f, 0f)))
        val u4 = engine.processEnrollmentSample("userA@seminar.vn", validSample(floatArrayOf(0f, 2f)))
        val u5 = engine.processEnrollmentSample("userA@seminar.vn", validSample(floatArrayOf(0f, 2f)))

        assertEquals(FaceEnrollmentUpdate.FrameCollected(1, 5), u1)
        assertEquals(FaceEnrollmentUpdate.FrameCollected(2, 5), u2)
        assertEquals(FaceEnrollmentUpdate.FrameCollected(3, 5), u3)
        assertEquals(FaceEnrollmentUpdate.FrameCollected(4, 5), u4)
        assertTrue(u5 is FaceEnrollmentUpdate.Completed)

        assertTrue(store.hasTemplate("userA@seminar.vn"))
        assertFalse(store.hasTemplate("userB@seminar.vn"))

        val saved = store.getTemplate("userA@seminar.vn")
        assertNotNull(saved)
        // Sum of [1,0]*3 + [0,2]*2 = [3, 4]; avg = [0.6, 0.8]; L2-normalized = [0.6, 0.8]
        assertEquals(0.6f, saved!![0], 0.0001f)
        assertEquals(0.8f, saved[1], 0.0001f)
    }
}
