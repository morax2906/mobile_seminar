package com.seminar.biometric.biometric

import android.content.Context
import androidx.fragment.app.FragmentActivity

/**
 * Interface chuẩn cho mọi phương thức xác thực sinh trắc học trong ứng dụng.
 * Áp dụng Strategy Pattern để dễ dàng mở rộng thêm các loại sinh trắc học mới (Face, Iris, ...).
 */
interface BiometricAuthenticator {
    /**
     * Loại sinh trắc học (Vân tay hoặc Khuôn mặt)
     */
    val type: BiometricType

    /**
     * Tên hiển thị trên giao diện người dùng
     */
    val displayName: String

    /**
     * Kiểm tra phần cứng và dữ liệu sinh trắc học đã sẵn sàng trên máy chưa
     */
    fun checkAvailability(context: Context): BiometricAvailability

    /**
     * Kích hoạt quy trình xác thực (Verification)
     * @param activity FragmentActivity để gắn dialog BiometricPrompt
     * @param onResult Callback nhận kết quả xác thực
     */
    fun authenticate(
        activity: FragmentActivity,
        onResult: (BiometricAuthResult) -> Unit
    )
}
