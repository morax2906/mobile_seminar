package com.seminar.biometric.ui.home

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.seminar.biometric.data.AuthMethod
import com.seminar.biometric.data.UserSession
import com.seminar.biometric.ui.theme.ErrorRed
import com.seminar.biometric.ui.theme.PrimaryBlue
import com.seminar.biometric.ui.theme.SuccessGreen
import java.util.Locale

@Composable
fun HomeScreen(
    session: UserSession,
    onLogout: () -> Unit,
    onEnrollFace: ((FragmentActivity) -> Unit)? = null,
    feedbackMessage: String? = null,
    isErrorMessage: Boolean = false,
    modifier: Modifier = Modifier
) {
    val activity = LocalContext.current as? FragmentActivity
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Success Icon
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(SuccessGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = SuccessGreen,
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Xác Thực Thành Công!",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Chào mừng bạn đã truy cập vào hệ thống",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Card Thông tin phiên đăng nhập
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thông tin phiên đăng nhập",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                InfoRow(label = "Tài khoản:", value = session.username)
                Spacer(modifier = Modifier.height(10.dp))
                InfoRow(label = "Phương thức:", value = session.authMethod.displayName)
                Spacer(modifier = Modifier.height(10.dp))
                InfoRow(label = "Thời điểm:", value = session.formattedLoginTime)
                if (session.authMethod == AuthMethod.FACE && session.matchingScore != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow(
                        label = "Matching Score:",
                        value = String.format(Locale.US, "%.3f", session.matchingScore)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    InfoRow(
                        label = "Threshold:",
                        value = String.format(
                            Locale.US,
                            "%.2f",
                            session.threshold ?: 0.75f
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Card Giải thích kỹ thuật cho Seminar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Khái niệm kỹ thuật Seminar (1:1)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (session.authMethod == AuthMethod.FACE && session.matchingScore != null) {
                        val scoreText = String.format(Locale.US, "%.3f", session.matchingScore)
                        val thresholdText = String.format(Locale.US, "%.2f", session.threshold ?: 0.75f)
                        "• Quy trình: Verification 1:1 on-device (CameraX + ML Kit Face Detection + MobileFaceNet).\n" +
                            "• Matching Score thực tế: $scoreText >= Threshold: $thresholdText (Cosine Similarity trên vector L2).\n" +
                            "• Template: Chỉ lưu vector đặc trưng theo tài khoản trong SharedPreferences, không lưu ảnh gốc."
                    } else {
                        "• Quy trình: Verification (So khớp mẫu vừa quét với Biometric Template trong phần cứng TEE/SE an toàn).\n" +
                            "• Matching Score: Đạt ngưỡng Threshold cho phép của Android Biometric Subsystem.\n" +
                            "• Bảo mật: Template không bị lưu ra ngoài phần cứng bảo mật; ứng dụng chỉ nhận tín hiệu Success/Failed."
                    },
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = Color.DarkGray
                )
            }
        }

        if (onEnrollFace != null) {
            Spacer(modifier = Modifier.height(20.dp))
            OutlinedButton(
                onClick = { activity?.let { onEnrollFace(it) } },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Face, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Đăng ký / Cập nhật khuôn mặt", fontSize = 15.sp)
            }
        }

        feedbackMessage?.let { msg ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isErrorMessage) ErrorRed.copy(alpha = 0.1f)
                    else SuccessGreen.copy(alpha = 0.1f)
                )
            ) {
                Text(
                    text = msg,
                    modifier = Modifier.padding(14.dp),
                    color = if (isErrorMessage) ErrorRed else SuccessGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Nút Đăng xuất
        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Default.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Đăng xuất (Quay lại đăng nhập)", fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color.Gray, fontSize = 14.sp)
        Text(text = value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}
