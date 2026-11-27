package com.seminar.biometric.data

import com.seminar.biometric.biometric.BiometricType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthRepository {

    private val _currentUser = MutableStateFlow<UserSession?>(null)
    val currentUser: StateFlow<UserSession?> = _currentUser.asStateFlow()

    /**
     * Đăng nhập thông thường bằng tài khoản & mật khẩu
     */
    fun loginWithPassword(username: String, password: String):Result<UserSession> {
        if (username.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Tên đăng nhập và mật khẩu không được để trống"))
        }

        // Tài khoản demo mặc định hoặc chấp nhận tài khoản hợp lệ
        val session = UserSession(
            username = username.trim(),
            authMethod = AuthMethod.PASSWORD
        )
        _currentUser.value = session
        return Result.success(session)
    }

    /**
     * Đăng nhập sau khi hoàn thành xác thực sinh trắc học
     */
    fun loginWithBiometric(type: BiometricType, username: String = "BiometricUser"): UserSession {
        val method = when (type) {
            BiometricType.FINGERPRINT -> AuthMethod.FINGERPRINT
            BiometricType.FACE -> AuthMethod.FACE
        }
        val session = UserSession(
            username = username,
            authMethod = method
        )
        _currentUser.value = session
        return session
    }

    /**
     * Đăng xuất xóa phiên làm việc
     */
    fun logout() {
        _currentUser.value = null
    }

    companion object {
        val instance by lazy { AuthRepository() }
    }
}
