package com.seminar.biometric.biometric.face

class InMemoryFaceTemplateStore : FaceTemplateStore {
    private val templates = mutableMapOf<String, FloatArray>()
    private var threshold: Float = FaceTemplateStore.DEFAULT_THRESHOLD

    private fun normalizeKey(username: String): String = username.trim().lowercase()

    override fun hasTemplate(username: String): Boolean = templates.containsKey(normalizeKey(username))

    override fun saveTemplate(username: String, embedding: FloatArray) {
        templates[normalizeKey(username)] = embedding.copyOf()
    }

    override fun getTemplate(username: String): FloatArray? = templates[normalizeKey(username)]?.copyOf()

    override fun getThreshold(defaultThreshold: Float): Float = threshold

    override fun saveThreshold(threshold: Float) {
        this.threshold = threshold
    }
}
