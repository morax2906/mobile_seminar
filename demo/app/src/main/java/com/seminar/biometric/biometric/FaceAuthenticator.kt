package com.seminar.biometric.biometric

import android.content.Context
import androidx.fragment.app.FragmentActivity

/**
 * =================================================================================
 * DÀNH CHO THÀNH VIÊN PHỤ TRÁCH NHẬN DIỆN KHUÔN MẶT (FACE RECOGNITION):
 * =================================================================================
 * Class này đã được thiết kế sẵn theo interface [BiometricAuthenticator].
 * Bạn có thể triển khai tính năng theo 1 trong 2 hướng tiếp cận:
 *
 * HƯỚNG 1 - Sử dụng BiometricPrompt của Android (nếu máy có Face Unlock chuẩn):
 *   - Sử dụng BiometricPrompt tương tự FingerprintAuthenticator,
 *   - Yêu cầu người dùng nhìn vào camera trước để mở khóa.
 *
 * HƯỚNG 2 - Tự mở Camera trước + thư viện AI/ML (Google ML Kit / OpenCV / TensorFlow Lite):
 *   - Khởi chạy một Activity hoặc Compose Screen riêng để mở CameraX.
 *   - Nhận diện khuôn mặt thời gian thực (Liveness Detection, Face Mesh, hoặc so khớp embedding).
 *   - Khi xác thực xong, gọi callback [onResult] với [BiometricAuthResult.Success] hoặc [BiometricAuthResult.Failed].
 * =================================================================================
 */
class FaceAuthenticator : BiometricAuthenticator {

    override val type: BiometricType = BiometricType.FACE
    override val displayName: String = "Xác thực Khuôn mặt"

    override fun checkAvailability(context: Context): BiometricAvailability {
        // TODO: Thành viên phụ trách khuôn mặt bổ sung kiểm tra quyền Camera hoặc phần cứng Face Unlock
        return BiometricAvailability.Available
    }

    override fun authenticate(
        activity: FragmentActivity,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        // TODO: Triển khai logic quét khuôn mặt tại đây (CameraX / ML Kit hoặc BiometricPrompt)
        
        // Hiện tại trả về thông báo sẵn sàng tích hợp để demo không bị lỗi khi bấm thử
        onResult(
            BiometricAuthResult.Error(
                errorCode = 999,
                errorMessage = "Chức năng Nhận diện khuôn mặt đang được phát triển bởi thành viên phụ trách (Nhánh: feature/biometric-face)."
            )
        )
    }
}
