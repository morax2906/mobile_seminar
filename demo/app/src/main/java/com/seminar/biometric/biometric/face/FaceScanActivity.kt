package com.seminar.biometric.biometric.face

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.seminar.biometric.ui.theme.BiometricSeminarTheme
import com.seminar.biometric.ui.theme.ErrorRed
import com.seminar.biometric.ui.theme.PrimaryBlue
import com.seminar.biometric.ui.theme.SuccessGreen
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs

/**
 * Màn hình CameraX + ML Kit + MobileFaceNet thực hiện Enrollment (5 frame)
 * hoặc Verification 1:1 kèm hiển thị Matching Score & thanh chỉnh Threshold.
 */
class FaceScanActivity : ComponentActivity() {

    private var completed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_VERIFY
        val username = intent.getStringExtra(EXTRA_USERNAME).orEmpty().ifBlank { "sinhvien@seminar.vn" }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finishCancelled()
            }
        })

        setContent {
            BiometricSeminarTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    FaceScanScreen(
                        mode = mode,
                        username = username,
                        onResult = { resultIntent ->
                            if (!completed) {
                                completed = true
                                setResult(Activity.RESULT_OK, resultIntent)
                                finish()
                            }
                        },
                        onCancel = { finishCancelled() }
                    )
                }
            }
        }
    }

    private fun finishCancelled() {
        if (!completed) {
            completed = true
            setResult(Activity.RESULT_CANCELED)
            finish()
        }
    }

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_USERNAME = "extra_username"
        const val MODE_ENROLL = "mode_enroll"
        const val MODE_VERIFY = "mode_verify"

        const val EXTRA_RESULT_TYPE = "extra_result_type"
        const val EXTRA_SCORE = "extra_score"
        const val EXTRA_THRESHOLD = "extra_threshold"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_ERROR_CODE = "extra_error_code"

        const val RESULT_SUCCESS = "success"
        const val RESULT_FAILED = "failed"
        const val RESULT_ERROR = "error"

        fun createIntent(context: Context, mode: String, username: String): Intent {
            return Intent(context, FaceScanActivity::class.java).apply {
                putExtra(EXTRA_MODE, mode)
                putExtra(EXTRA_USERNAME, username)
            }
        }
    }
}

@Composable
private fun FaceScanScreen(
    mode: String,
    username: String,
    onResult: (Intent) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val templateStore = remember(context) { SharedPreferencesFaceTemplateStore(context) }
    val engine = remember(templateStore) { FaceAuthEngine(templateStore) }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        PermissionRequiredView(
            onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onCancel = onCancel
        )
        return
    }

    val isEnroll = mode == FaceScanActivity.MODE_ENROLL
    var faceBox by remember { mutableStateOf<NormalizedFaceBox?>(null) }
    var statusText by remember {
        mutableStateOf(
            if (isEnroll) "Đưa khuôn mặt vào khung hình để thu 5 mẫu"
            else "Nhìn thẳng vào camera trước để xác thực"
        )
    }
    var qualityOk by remember { mutableStateOf(false) }
    var eyesOpenOk by remember { mutableStateOf(false) }
    var frontalOk by remember { mutableStateOf(false) }
    var sizeOk by remember { mutableStateOf(false) }

    var collectedFrames by remember { mutableIntStateOf(0) }
    var latestScore by remember { mutableStateOf<Float?>(null) }
    var threshold by remember { mutableFloatStateOf(templateStore.getThreshold()) }
    var mismatchCount by remember { mutableIntStateOf(0) }
    var showThresholdControls by remember { mutableStateOf(!isEnroll) }
    var lastEnrollAcceptedMillis by remember { mutableStateOf(0L) }
    var lastVerifyEvaluatedMillis by remember { mutableStateOf(0L) }
    var finished by remember { mutableStateOf(false) }

    val embedder = remember(context) {
        runCatching { MobileFaceNetEmbedder(context) }.getOrNull()
    }

    LaunchedEffect(embedder) {
        if (embedder == null && !finished) {
            finished = true
            onResult(
                Intent().apply {
                    putExtra(FaceScanActivity.EXTRA_RESULT_TYPE, FaceScanActivity.RESULT_ERROR)
                    putExtra(FaceScanActivity.EXTRA_ERROR_CODE, 901)
                    putExtra(FaceScanActivity.EXTRA_MESSAGE, "Không thể nạp model MobileFaceNet (.tflite).")
                }
            )
        }
    }

    if (embedder == null) return

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val analyzer = remember(embedder, mode, username) {
        CameraFaceFrameAnalyzer(embedder) { analysis ->
            if (finished) return@CameraFaceFrameAnalyzer

            faceBox = analysis.mirroredNormalizedBox
            val sample = analysis.sample

            if (analysis.faceCount > 1) {
                qualityOk = false
                eyesOpenOk = false
                frontalOk = false
                sizeOk = false
                statusText = FaceQualityIssue.MULTIPLE_FACES.guidanceMessage
                return@CameraFaceFrameAnalyzer
            }

            if (sample == null) {
                qualityOk = false
                eyesOpenOk = false
                frontalOk = false
                sizeOk = false
                statusText = FaceQualityIssue.NO_FACE.guidanceMessage
                return@CameraFaceFrameAnalyzer
            }

            sizeOk = sample.boundingBoxWidthRatio >= FaceAuthEngine.MIN_FACE_WIDTH_RATIO
            frontalOk = abs(sample.headEulerAngleX) <= FaceAuthEngine.MAX_HEAD_ANGLE_DEGREES &&
                abs(sample.headEulerAngleY) <= FaceAuthEngine.MAX_HEAD_ANGLE_DEGREES &&
                abs(sample.headEulerAngleZ) <= FaceAuthEngine.MAX_HEAD_ANGLE_DEGREES
            eyesOpenOk = (sample.leftEyeOpenProbability ?: 0f) >= FaceAuthEngine.MIN_EYE_OPEN_PROBABILITY &&
                (sample.rightEyeOpenProbability ?: 0f) >= FaceAuthEngine.MIN_EYE_OPEN_PROBABILITY

            val now = System.currentTimeMillis()
            if (isEnroll) {
                if (sizeOk && frontalOk && eyesOpenOk && now - lastEnrollAcceptedMillis < 300L) {
                    return@CameraFaceFrameAnalyzer
                }
                when (val update = engine.processEnrollmentSample(username, sample)) {
                    is FaceEnrollmentUpdate.QualityRejected -> {
                        qualityOk = false
                        collectedFrames = update.collectedFrames
                        statusText = update.issue.guidanceMessage
                    }
                    is FaceEnrollmentUpdate.FrameCollected -> {
                        lastEnrollAcceptedMillis = now
                        qualityOk = true
                        collectedFrames = update.collectedFrames
                        statusText = "Giữ nguyên mặt... Đã thu ${update.collectedFrames}/${update.requiredFrames} mẫu"
                    }
                    is FaceEnrollmentUpdate.Completed -> {
                        qualityOk = true
                        collectedFrames = FaceAuthEngine.DEFAULT_REQUIRED_ENROLLMENT_FRAMES
                        finished = true
                        onResult(
                            Intent().apply {
                                putExtra(FaceScanActivity.EXTRA_RESULT_TYPE, FaceScanActivity.RESULT_SUCCESS)
                                putExtra(
                                    FaceScanActivity.EXTRA_MESSAGE,
                                    "Đăng ký khuôn mặt thành công cho $username!"
                                )
                            }
                        )
                    }
                }
            } else {
                if (sizeOk && frontalOk && eyesOpenOk && now - lastVerifyEvaluatedMillis < 350L) {
                    return@CameraFaceFrameAnalyzer
                }
                when (val update = engine.processVerificationSample(username, sample)) {
                    is FaceVerificationUpdate.NotEnrolled -> {
                        finished = true
                        onResult(
                            Intent().apply {
                                putExtra(FaceScanActivity.EXTRA_RESULT_TYPE, FaceScanActivity.RESULT_ERROR)
                                putExtra(FaceScanActivity.EXTRA_ERROR_CODE, 902)
                                putExtra(
                                    FaceScanActivity.EXTRA_MESSAGE,
                                    "Tài khoản $username chưa đăng ký khuôn mặt."
                                )
                            }
                        )
                    }
                    is FaceVerificationUpdate.QualityRejected -> {
                        qualityOk = false
                        statusText = update.issue.guidanceMessage
                    }
                    is FaceVerificationUpdate.MismatchRetry -> {
                        lastVerifyEvaluatedMillis = now
                        qualityOk = true
                        latestScore = update.score
                        mismatchCount = update.consecutiveMismatches
                        statusText = "Đang so khớp... Điểm chưa đạt ngưỡng (${update.consecutiveMismatches}/${update.maxMismatches})"
                    }
                    is FaceVerificationUpdate.Matched -> {
                        qualityOk = true
                        latestScore = update.score
                        finished = true
                        onResult(
                            Intent().apply {
                                putExtra(FaceScanActivity.EXTRA_RESULT_TYPE, FaceScanActivity.RESULT_SUCCESS)
                                putExtra(FaceScanActivity.EXTRA_SCORE, update.score)
                                putExtra(FaceScanActivity.EXTRA_THRESHOLD, update.threshold)
                                putExtra(
                                    FaceScanActivity.EXTRA_MESSAGE,
                                    "Xác thực khuôn mặt thành công!"
                                )
                            }
                        )
                    }
                    is FaceVerificationUpdate.Failed -> {
                        qualityOk = true
                        latestScore = update.lastScore
                        finished = true
                        onResult(
                            Intent().apply {
                                putExtra(FaceScanActivity.EXTRA_RESULT_TYPE, FaceScanActivity.RESULT_FAILED)
                                putExtra(FaceScanActivity.EXTRA_SCORE, update.lastScore)
                                putExtra(FaceScanActivity.EXTRA_THRESHOLD, update.threshold)
                            }
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(lifecycleOwner, analyzer) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(analysisExecutor, analyzer)
                }

            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
            }.onFailure { err ->
                if (!finished) {
                    finished = true
                    onResult(
                        Intent().apply {
                            putExtra(FaceScanActivity.EXTRA_RESULT_TYPE, FaceScanActivity.RESULT_ERROR)
                            putExtra(FaceScanActivity.EXTRA_ERROR_CODE, 903)
                            putExtra(
                                FaceScanActivity.EXTRA_MESSAGE,
                                "Không thể mở camera trước: ${err.localizedMessage ?: "Unknown error"}"
                            )
                        }
                    )
                }
            }
        }, mainExecutor)

        onDispose {
            runCatching { ProcessCameraProvider.getInstance(context).get().unbindAll() }
            analyzer.close()
            embedder.close()
            analysisExecutor.shutdown()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Bounding Box Overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            faceBox?.let { box ->
                val strokeColor = when {
                    !qualityOk -> Color(0xFFFFB300)
                    mismatchCount > 0 && !isEnroll -> ErrorRed
                    else -> SuccessGreen
                }
                val left = box.left * size.width
                val top = box.top * size.height
                val width = (box.right - box.left) * size.width
                val height = (box.bottom - box.top) * size.height
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(left, top),
                    size = Size(width, height),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 6f)
                )
            }
        }

        // Top Bar & Quality Pills
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Face,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isEnroll) "Đăng ký Khuôn mặt" else "Xác thực Khuôn mặt (1:1)",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Tài khoản: $username",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
                Row {
                    if (!isEnroll) {
                        IconButton(onClick = { showThresholdControls = !showThresholdControls }) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = "Chỉnh Threshold",
                                tint = Color.White
                            )
                        }
                    }
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Hủy bỏ",
                            tint = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QualityPill(label = "Mắt mở", ok = eyesOpenOk, modifier = Modifier.weight(1f))
                QualityPill(label = "Nhìn thẳng", ok = frontalOk, modifier = Modifier.weight(1f))
                QualityPill(label = "Cự ly mặt", ok = sizeOk, modifier = Modifier.weight(1f))
            }
        }

        // Bottom Telemetry & Controls Panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xE6121722))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = statusText,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (isEnroll) {
                    LinearProgressIndicator(
                        progress = { collectedFrames / FaceAuthEngine.DEFAULT_REQUIRED_ENROLLMENT_FRAMES.toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = SuccessGreen,
                        trackColor = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tiến độ Enrollment: $collectedFrames / ${FaceAuthEngine.DEFAULT_REQUIRED_ENROLLMENT_FRAMES} frame",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TelemetryStat(
                            label = "Matching Score",
                            value = latestScore?.let { String.format(Locale.US, "%.3f", it) } ?: "--",
                            valueColor = if ((latestScore ?: 0f) >= threshold) SuccessGreen else Color(0xFFFFB300)
                        )
                        TelemetryStat(
                            label = "Threshold",
                            value = String.format(Locale.US, "%.2f", threshold),
                            valueColor = PrimaryBlue
                        )
                        TelemetryStat(
                            label = "Lần không khớp",
                            value = "$mismatchCount / ${FaceAuthEngine.DEFAULT_MAX_CONSECUTIVE_MISMATCHES}",
                            valueColor = if (mismatchCount > 0) ErrorRed else Color.LightGray
                        )
                    }

                    if (showThresholdControls) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Demo FAR / FRR — Điều chỉnh Threshold: ${String.format(Locale.US, "%.2f", threshold)}",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                        Slider(
                            value = threshold,
                            onValueChange = { newValue ->
                                threshold = newValue
                                templateStore.saveThreshold(newValue)
                            },
                            valueRange = SharedPreferencesFaceTemplateStore.MIN_THRESHOLD..SharedPreferencesFaceTemplateStore.MAX_THRESHOLD,
                            colors = SliderDefaults.colors(
                                thumbColor = PrimaryBlue,
                                activeTrackColor = PrimaryBlue
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                ) {
                    Text("Hủy bỏ", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun QualityPill(label: String, ok: Boolean, modifier: Modifier = Modifier) {
    val bg = if (ok) SuccessGreen.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.12f)
    val fg = if (ok) Color(0xFF9DF2B0) else Color.LightGray
    Box(
        modifier = modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = if (ok) "✓ $label" else "• $label", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TelemetryStat(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = Color.Gray, fontSize = 11.sp)
        Text(text = value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PermissionRequiredView(
    onRequestPermission: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Face,
            contentDescription = null,
            tint = PrimaryBlue,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cần quyền Camera trước",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Ứng dụng cần quyền sử dụng camera trước để quét và xác thực khuôn mặt trên thiết bị.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Cấp quyền Camera")
        }
        Spacer(modifier = Modifier.height(10.dp))
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Quay lại")
        }
    }
}
