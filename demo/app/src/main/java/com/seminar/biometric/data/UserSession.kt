package com.seminar.biometric.data

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AuthMethod(val displayName: String) {
    PASSWORD("Mật khẩu truyền thống"),
    FINGERPRINT("Sinh trắc học: Vân tay"),
    FACE("Sinh trắc học: Khuôn mặt")
}

data class UserSession(
    val username: String,
    val authMethod: AuthMethod,
    val loginTimeMillis: Long = System.currentTimeMillis()
) {
    val formattedLoginTime: String
        get() {
            val sdf = SimpleDateFormat("HH:mm:ss - dd/MM/yyyy", Locale.getDefault())
            return sdf.format(Date(loginTimeMillis))
        }
}
