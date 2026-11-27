package com.seminar.biometric.biometric

/**
 * Định nghĩa các loại xác thực sinh trắc học được hỗ trợ
 */
enum class BiometricType {
    FINGERPRINT,
    FACE
}

/**
 * Trạng thái khả dụng của phần cứng và dữ liệu sinh trắc học trên thiết bị
 */
sealed class BiometricAvailability {
    object Available : BiometricAvailability()
    object NoHardware : BiometricAvailability()
    object HardwareUnavailable : BiometricAvailability()
    object NoneEnrolled : BiometricAvailability()
    object SecurityUpdateRequired : BiometricAvailability()
    data class Unknown(val message: String) : BiometricAvailability()

    val isAvailable: Boolean
        get() = this is Available

    fun getDisplayMessage(): String = when (this) {
        is Available -> "Sẵn sàng xác thực"
        is NoHardware -> "Thiết bị không có phần cứng hỗ trợ"
        is HardwareUnavailable -> "Cảm biến sinh trắc học hiện không khả dụng"
        is NoneEnrolled -> "Chưa có dữ liệu sinh trắc học được đăng ký trên máy"
        is SecurityUpdateRequired -> "Cần cập nhật bảo mật để sử dụng"
        is Unknown -> message
    }
}

/**
 * Kết quả sau khi thực hiện xác thực
 */
sealed class BiometricAuthResult {
    data class Success(
        val type: BiometricType,
        val message: String = "Xác thực sinh trắc học thành công",
        val matchingScore: Float? = null,
        val threshold: Float? = null
    ) : BiometricAuthResult()

    data class Error(
        val errorCode: Int,
        val errorMessage: String
    ) : BiometricAuthResult()

    object Failed : BiometricAuthResult()

    object Cancelled : BiometricAuthResult()
}
