package com.seminar.biometric.biometric

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import com.seminar.biometric.biometric.face.FaceScanActivity
import com.seminar.biometric.biometric.face.FaceTemplateStore
import com.seminar.biometric.biometric.face.SharedPreferencesFaceTemplateStore
import java.util.UUID

/**
 * Triển khai xác thực khuôn mặt on-device bằng CameraX + ML Kit Face Detection + MobileFaceNet (.tflite).
 * Hỗ trợ đầy đủ cả quy trình Enrollment (5 frame) và Verification 1:1 theo từng username.
 */
class FaceAuthenticator(
    private val templateStoreFactory: (Context) -> FaceTemplateStore = { SharedPreferencesFaceTemplateStore(it) }
) : FaceBiometricAuthenticator {

    override val type: BiometricType = BiometricType.FACE
    override val displayName: String = "Xác thực Khuôn mặt"

    private var targetUsername: String = "sinhvien@seminar.vn"

    override fun setTargetUsername(username: String) {
        targetUsername = username.trim()
    }

    override fun checkAvailability(context: Context): BiometricAvailability {
        if (!hasFrontCamera(context)) {
            return BiometricAvailability.NoHardware
        }
        if (targetUsername.isBlank()) {
            return BiometricAvailability.NoneEnrolled
        }
        val store = templateStoreFactory(context)
        return if (store.hasTemplate(targetUsername)) {
            BiometricAvailability.Available
        } else {
            BiometricAvailability.NoneEnrolled
        }
    }

    override fun authenticate(
        activity: FragmentActivity,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        if (!hasFrontCamera(activity)) {
            onResult(
                BiometricAuthResult.Error(
                    errorCode = 900,
                    errorMessage = "Thiết bị không có camera trước hỗ trợ quét khuôn mặt."
                )
            )
            return
        }
        if (targetUsername.isBlank()) {
            onResult(
                BiometricAuthResult.Error(
                    errorCode = 904,
                    errorMessage = "Vui lòng nhập Tên người dùng trước khi xác thực khuôn mặt."
                )
            )
            return
        }
        launchScan(
            activity = activity,
            mode = FaceScanActivity.MODE_VERIFY,
            username = targetUsername,
            onResult = onResult
        )
    }

    override fun enroll(
        activity: FragmentActivity,
        username: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val trimmed = username.trim()
        setTargetUsername(trimmed)
        if (!hasFrontCamera(activity)) {
            onResult(
                BiometricAuthResult.Error(
                    errorCode = 900,
                    errorMessage = "Thiết bị không có camera trước hỗ trợ đăng ký khuôn mặt."
                )
            )
            return
        }
        launchScan(
            activity = activity,
            mode = FaceScanActivity.MODE_ENROLL,
            username = trimmed,
            onResult = onResult
        )
    }

    private fun launchScan(
        activity: FragmentActivity,
        mode: String,
        username: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val key = "face_scan_${UUID.randomUUID()}"
        var launcher: ActivityResultLauncher<Intent>? = null
        launcher = activity.activityResultRegistry.register(
            key,
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult ->
            launcher?.unregister()
            if (activityResult.resultCode != Activity.RESULT_OK || activityResult.data == null) {
                onResult(BiometricAuthResult.Cancelled)
                return@register
            }

            val data = activityResult.data!!
            when (data.getStringExtra(FaceScanActivity.EXTRA_RESULT_TYPE)) {
                FaceScanActivity.RESULT_SUCCESS -> {
                    val hasScore = data.hasExtra(FaceScanActivity.EXTRA_SCORE)
                    val score = if (hasScore) data.getFloatExtra(FaceScanActivity.EXTRA_SCORE, 0f) else null
                    val threshold = if (data.hasExtra(FaceScanActivity.EXTRA_THRESHOLD)) {
                        data.getFloatExtra(FaceScanActivity.EXTRA_THRESHOLD, FaceTemplateStore.DEFAULT_THRESHOLD)
                    } else null
                    val message = data.getStringExtra(FaceScanActivity.EXTRA_MESSAGE)
                        ?: "Xác thực khuôn mặt thành công!"
                    onResult(
                        BiometricAuthResult.Success(
                            type = BiometricType.FACE,
                            message = message,
                            matchingScore = score,
                            threshold = threshold
                        )
                    )
                }
                FaceScanActivity.RESULT_FAILED -> onResult(BiometricAuthResult.Failed)
                FaceScanActivity.RESULT_ERROR -> {
                    onResult(
                        BiometricAuthResult.Error(
                            errorCode = data.getIntExtra(FaceScanActivity.EXTRA_ERROR_CODE, 999),
                            errorMessage = data.getStringExtra(FaceScanActivity.EXTRA_MESSAGE)
                                ?: "Lỗi xác thực khuôn mặt."
                        )
                    )
                }
                else -> onResult(BiometricAuthResult.Cancelled)
            }
        }
        launcher.launch(FaceScanActivity.createIntent(activity, mode, username))
    }

    private fun hasFrontCamera(context: Context): Boolean {
        val pm = context.packageManager
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return true
        }
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return false
        return runCatching {
            cameraManager.cameraIdList.any { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            }
        }.getOrDefault(false)
    }
}
