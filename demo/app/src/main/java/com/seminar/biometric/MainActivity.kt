package com.seminar.biometric

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.seminar.biometric.data.AuthRepository
import com.seminar.biometric.data.UserSession
import com.seminar.biometric.ui.home.HomeScreen
import com.seminar.biometric.ui.login.LoginScreen
import com.seminar.biometric.ui.login.LoginViewModel
import com.seminar.biometric.ui.theme.BiometricSeminarTheme

class MainActivity : FragmentActivity() {

    private val loginViewModel: LoginViewModel by viewModels()
    private val authRepository = AuthRepository.instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BiometricSeminarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser by authRepository.currentUser.collectAsState()
                    val loginUiState by loginViewModel.uiState.collectAsState()

                    if (currentUser != null) {
                        HomeScreen(
                            session = currentUser!!,
                            onLogout = {
                                authRepository.logout()
                                loginViewModel.clearFeedback()
                            },
                            onEnrollFace = { activity ->
                                loginViewModel.enrollFace(activity, currentUser!!.username)
                            },
                            feedbackMessage = loginUiState.feedbackMessage,
                            isErrorMessage = loginUiState.isErrorMessage
                        )
                    } else {
                        LoginScreen(
                            viewModel = loginViewModel,
                            onLoginSuccess = {
                                // Tự động cập nhật UI khi currentUser Flow phát sinh giá trị
                            }
                        )
                    }
                }
            }
        }
    }
}
