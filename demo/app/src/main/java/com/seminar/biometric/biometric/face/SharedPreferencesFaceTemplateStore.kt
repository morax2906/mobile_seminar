package com.seminar.biometric.biometric.face

import android.content.Context
import android.content.SharedPreferences

/**
 * Lưu trữ Biometric Template (vector embedding dạng float) theo từng tài khoản và Threshold
 * trong SharedPreferences thông thường để dễ kiểm tra, debug khi thuyết trình seminar.
 * Không lưu ảnh khuôn mặt gốc.
 */
class SharedPreferencesFaceTemplateStore(
    private val prefs: SharedPreferences
) : FaceTemplateStore {

    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    private fun templateKey(username: String): String =
        "$KEY_TEMPLATE_PREFIX${username.trim().lowercase()}"

    override fun hasTemplate(username: String): Boolean {
        val raw = prefs.getString(templateKey(username), null)
        return !raw.isNullOrBlank()
    }

    override fun saveTemplate(username: String, embedding: FloatArray) {
        val serialized = embedding.joinToString(separator = ",")
        prefs.edit().putString(templateKey(username), serialized).apply()
    }

    override fun getTemplate(username: String): FloatArray? {
        val raw = prefs.getString(templateKey(username), null) ?: return null
        if (raw.isBlank()) return null
        val parts = raw.split(',')
        return FloatArray(parts.size) { index ->
            parts[index].toFloatOrNull() ?: return null
        }
    }

    override fun getThreshold(defaultThreshold: Float): Float {
        return prefs.getFloat(KEY_THRESHOLD, defaultThreshold)
    }

    override fun saveThreshold(threshold: Float) {
        prefs.edit().putFloat(KEY_THRESHOLD, threshold.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)).apply()
    }

    companion object {
        private const val PREFS_NAME = "biometric_face_templates"
        private const val KEY_TEMPLATE_PREFIX = "face_template_"
        private const val KEY_THRESHOLD = "face_matching_threshold"
        const val MIN_THRESHOLD = 0.50f
        const val MAX_THRESHOLD = 0.92f
    }
}
