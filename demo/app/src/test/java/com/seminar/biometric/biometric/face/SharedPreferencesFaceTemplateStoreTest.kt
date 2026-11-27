package com.seminar.biometric.biometric.face

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedPreferencesFaceTemplateStoreTest {

    @Test
    fun `persists face template per normalized username and updates threshold`() {
        val prefs = InMemorySharedPreferences()
        val store = SharedPreferencesFaceTemplateStore(prefs)

        assertFalse(store.hasTemplate("sinhvien@seminar.vn"))
        assertEquals(FaceTemplateStore.DEFAULT_THRESHOLD, store.getThreshold(), 0.0001f)

        store.saveTemplate("  SinhVien@Seminar.vn ", floatArrayOf(0.6f, 0.8f))
        assertTrue(store.hasTemplate("sinhvien@seminar.vn"))
        assertFalse(store.hasTemplate("other@seminar.vn"))

        val loaded = store.getTemplate("sinhvien@seminar.vn")
        assertNotNull(loaded)
        assertEquals(2, loaded!!.size)
        assertEquals(0.6f, loaded[0], 0.0001f)
        assertEquals(0.8f, loaded[1], 0.0001f)

        store.saveThreshold(0.82f)
        assertEquals(0.82f, store.getThreshold(), 0.0001f)
    }
}

private class InMemorySharedPreferences : SharedPreferences {
    private val map = mutableMapOf<String, Any?>()

    override fun getAll(): MutableMap<String, *> = map.toMutableMap()
    override fun getString(key: String?, defValue: String?): String? = map[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues
    override fun getInt(key: String?, defValue: Int): Int = map[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = map[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = map[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = map.containsKey(key)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
        private val pending = mutableMapOf<String, Any?>()
        private var clearAll = false

        override fun putString(key: String?, value: String?) = apply { if (key != null) pending[key] = value }
        override fun putStringSet(key: String?, values: MutableSet<String>?) = apply {}
        override fun putInt(key: String?, value: Int) = apply { if (key != null) pending[key] = value }
        override fun putLong(key: String?, value: Long) = apply { if (key != null) pending[key] = value }
        override fun putFloat(key: String?, value: Float) = apply { if (key != null) pending[key] = value }
        override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) pending[key] = value }
        override fun remove(key: String?) = apply { if (key != null) pending[key] = null }
        override fun clear() = apply { clearAll = true }
        override fun commit(): Boolean { apply(); return true }
        override fun apply() {
            if (clearAll) map.clear()
            for ((k, v) in pending) {
                if (v == null) map.remove(k) else map[k] = v
            }
        }
    }
}
