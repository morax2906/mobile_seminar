package com.seminar.biometric.ui.login

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seminar.biometric.biometric.BiometricAuthResult
import com.seminar.biometric.biometric.BiometricAuthenticator
import com.seminar.biometric.biometric.BiometricAvailability
import com.seminar.biometric.biometric.BiometricType
import com.seminar.biometric.biometric.FaceAuthenticator
import com.seminar.biometric.biometric.FingerprintAuthenticator
import com.seminar.biometric.data.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val usernameInput: String = "sinhvien@seminar.vn",
    val passwordInput: String = "123456",
    val isLoading: Boolean = false,
    val feedbackMessage: String? = null,
    val isErrorMessage: Boolean = false,
    val fingerprintStatus: BiometricAvailability = BiometricAvailability.Available,
    val faceStatus: BiometricAvailability = BiometricAvailability.Available
)

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository.instance
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // Bảng đăng ký các authenticator (Dễ dàng mở rộng hoặc thay thế bởi teammate)
    private val authenticators: Map<BiometricType, BiometricAuthenticator> = mapOf(
        BiometricType.FINGERPRINT to FingerprintAuthenticator(),
        BiometricType.FACE to FaceAuthenticator()
    )

    fun onUsernameChange(newVal: String) {
        _uiState.update { it.copy(usernameInput = newVal, feedbackMessage = null) }
    }

    fun onPasswordChange(newVal: String) {
        _uiState.update { it.copy(passwordInput = newVal, feedbackMessage = null) }
    }

    /**
     * Kiểm tra trạng thái cảm biến sinh trắc học của thiết bị
     */
    fun refreshBiometricStatus(context: Context) {
        val fpStatus = authenticators[BiometricType.FINGERPRINT]?.checkAvailability(context)
            ?: BiometricAvailability.NoHardware
        val faceStatus = authenticators[BiometricType.FACE]?.checkAvailability(context)
            ?: BiometricAvailability.NoHardware

        _uiState.update {
            it.copy(
                fingerprintStatus = fpStatus,
                faceStatus = faceStatus
            )
        }
    }

    /**
     * Đăng nhập thông thường bằng tài khoản mật khẩu
     */
    fun loginWithPassword(onSuccess: () -> Unit) {
        val state = _uiState.value
        val result = authRepository.loginWithPassword(state.usernameInput, state.passwordInput)
        if (result.isSuccess) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "Đăng nhập mật khẩu thành công!",
                    isErrorMessage = false
                )
            }
            onSuccess()
        } else {
            _uiState.update {
                it.copy(
                    feedbackMessage = result.exceptionOrNull()?.message ?: "Đăng nhập thất bại",
                    isErrorMessage = true
                )
            }
        }
    }

    /**
     * Kích hoạt xác thực sinh trắc học (Vân tay hoặc Khuôn mặt)
     */
    fun authenticateWithBiometric(
        activity: FragmentActivity,
        type: BiometricType,
        onSuccess: () -> Unit
    ) {
        val authenticator = authenticators[type]
        if (authenticator == null) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "Phương thức xác thực chưa được cài đặt",
                    isErrorMessage = true
                )
            }
            return
        }

        // Kiểm tra phần cứng trước khi gọi prompt
        val availability = authenticator.checkAvailability(activity)
        if (!availability.isAvailable) {
            _uiState.update {
                it.copy(
                    feedbackMessage = "${authenticator.displayName}: ${availability.getDisplayMessage()}",
                    isErrorMessage = true
                )
            }
            return
        }

        // Gọi xác thực
        authenticator.authenticate(activity) { result ->
            viewModelScope.launch {
                when (result) {
                    is BiometricAuthResult.Success -> {
                        authRepository.loginWithBiometric(type, _uiState.value.usernameInput)
                        _uiState.update {
                            it.copy(
                                feedbackMessage = result.message,
                                isErrorMessage = false
                            )
                        }
                        onSuccess()
                    }
                    is BiometricAuthResult.Failed -> {
                        _uiState.update {
                            it.copy(
                                feedbackMessage = "Mẫu sinh trắc học không khớp (Verification Failed)!",
                                isErrorMessage = true
                            )
                        }
                    }
                    is BiometricAuthResult.Cancelled -> {
                        _uiState.update {
                            it.copy(
                                feedbackMessage = "Người dùng đã hủy xác thực.",
                                isErrorMessage = false
                            )
                        }
                    }
                    is BiometricAuthResult.Error -> {
                        _uiState.update {
                            it.copy(
                                feedbackMessage = "Lỗi (${result.errorCode}): ${result.errorMessage}",
                                isErrorMessage = true
                            )
                        }
                    }
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}
