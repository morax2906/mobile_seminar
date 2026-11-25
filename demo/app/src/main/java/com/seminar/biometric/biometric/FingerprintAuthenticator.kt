package com.seminar.biometric.biometric

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Triển khai xác thực vân tay sử dụng AndroidX BiometricPrompt API.
 * Phần này phục vụ cho seminar và demo luồng Verification (1:1).
 */
class FingerprintAuthenticator : BiometricAuthenticator {

    override val type: BiometricType = BiometricType.FINGERPRINT
    override val displayName: String = "Xác thực Vân tay"

    // Mức xác thực sinh trắc học yêu cầu (Strong/Weak)
    private val authenticators = Authenticators.BIOMETRIC_STRONG or Authenticators.BIOMETRIC_WEAK

    override fun checkAvailability(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.Available
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricAvailability.NoHardware
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricAvailability.HardwareUnavailable
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NoneEnrolled
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> BiometricAvailability.SecurityUpdateRequired
            else -> BiometricAvailability.Unknown("Trạng thái sinh trắc học không xác định")
        }
    }

    override fun authenticate(
        activity: FragmentActivity,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(
                    BiometricAuthResult.Success(
                        type = BiometricType.FINGERPRINT,
                        message = "Xác thực vân tay thành công!"
                    )
                )
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        onResult(BiometricAuthResult.Cancelled)
                    }
                    else -> {
                        onResult(
                            BiometricAuthResult.Error(
                                errorCode = errorCode,
                                errorMessage = errString.toString()
                            )
                        )
                    }
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // Cảm biến không nhận dạng được vân tay (so khớp thất bại)
                onResult(BiometricAuthResult.Failed)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Đăng nhập bằng Vân tay")
            .setSubtitle("Đặt ngón tay lên cảm biến để xác minh danh tính")
            .setDescription("Sử dụng dữ liệu sinh trắc học đã lưu trên thiết bị này.")
            .setNegativeButtonText("Hủy bỏ")
            .setAllowedAuthenticators(authenticators)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}
