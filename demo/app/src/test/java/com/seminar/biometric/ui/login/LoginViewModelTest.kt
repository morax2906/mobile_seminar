package com.seminar.biometric.ui.login

import android.content.Context
import androidx.fragment.app.FragmentActivity
import com.seminar.biometric.biometric.BiometricAuthResult
import com.seminar.biometric.biometric.BiometricAuthenticator
import com.seminar.biometric.biometric.BiometricAvailability
import com.seminar.biometric.biometric.BiometricType
import com.seminar.biometric.biometric.FaceBiometricAuthenticator
import com.seminar.biometric.data.AuthMethod
import com.seminar.biometric.data.AuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `face authentication success updates user session with matching score and threshold`() {
        val repository = AuthRepository()
        val fakeFaceAuth = FakeBiometricAuthenticator(
            type = BiometricType.FACE,
            availability = BiometricAvailability.Available,
            nextResult = BiometricAuthResult.Success(
                type = BiometricType.FACE,
                message = "Xác thực khuôn mặt thành công!",
                matchingScore = 0.88f,
                threshold = 0.75f
            )
        )
        val viewModel = LoginViewModel(
            authRepository = repository,
            authenticators = mapOf(BiometricType.FACE to fakeFaceAuth)
        )
        val activity = Mockito.mock(FragmentActivity::class.java)
        var succeeded = false

        viewModel.authenticateWithBiometric(
            activity = activity,
            type = BiometricType.FACE,
            onSuccess = { succeeded = true }
        )

        assertTrue(succeeded)
        val session = repository.currentUser.value
        assertNotNull(session)
        assertEquals("sinhvien@seminar.vn", session!!.username)
        assertEquals(AuthMethod.FACE, session.authMethod)
        assertEquals(0.88f, session.matchingScore!!, 0.0001f)
        assertEquals(0.75f, session.threshold!!, 0.0001f)
    }

    @Test
    fun `face enrollment updates availability for current username and sets success feedback`() {
        val repository = AuthRepository()
        val fakeFaceAuth = FakeFaceBiometricAuthenticator()
        val viewModel = LoginViewModel(
            authRepository = repository,
            authenticators = mapOf(BiometricType.FACE to fakeFaceAuth)
        )
        val activity = Mockito.mock(FragmentActivity::class.java)

        viewModel.onUsernameChange("  alice@seminar.vn  ")
        viewModel.refreshBiometricStatus(activity)
        assertEquals(BiometricAvailability.NoneEnrolled, viewModel.uiState.value.faceStatus)

        viewModel.enrollFace(activity)

        assertEquals(BiometricAvailability.Available, viewModel.uiState.value.faceStatus)
        assertFalse(viewModel.uiState.value.isErrorMessage)
        assertTrue(viewModel.uiState.value.feedbackMessage!!.contains("thành công", ignoreCase = true))
        assertNull(repository.currentUser.value)
    }

    @Test
    fun `failed and cancelled face auth do not create session and report expected feedback`() {
        val repository = AuthRepository()
        val fakeFaceAuth = FakeFaceBiometricAuthenticator(enrolledUsers = mutableSetOf("sinhvien@seminar.vn"))
        val viewModel = LoginViewModel(
            authRepository = repository,
            authenticators = mapOf(BiometricType.FACE to fakeFaceAuth)
        )
        val activity = Mockito.mock(FragmentActivity::class.java)

        fakeFaceAuth.nextAuthResult = BiometricAuthResult.Failed
        viewModel.authenticateWithBiometric(activity, BiometricType.FACE, onSuccess = {})
        assertNull(repository.currentUser.value)
        assertTrue(viewModel.uiState.value.isErrorMessage)

        fakeFaceAuth.nextAuthResult = BiometricAuthResult.Cancelled
        viewModel.authenticateWithBiometric(activity, BiometricType.FACE, onSuccess = {})
        assertNull(repository.currentUser.value)
        assertFalse(viewModel.uiState.value.isErrorMessage)
    }
}

private class FakeFaceBiometricAuthenticator(
    private val enrolledUsers: MutableSet<String> = mutableSetOf(),
    var nextAuthResult: BiometricAuthResult = BiometricAuthResult.Failed
) : FaceBiometricAuthenticator {
    override val type: BiometricType = BiometricType.FACE
    override val displayName: String = "Fake Face"
    private var targetUsername: String = ""

    override fun setTargetUsername(username: String) {
        targetUsername = username.trim().lowercase()
    }

    override fun checkAvailability(context: Context): BiometricAvailability {
        return if (enrolledUsers.contains(targetUsername)) {
            BiometricAvailability.Available
        } else {
            BiometricAvailability.NoneEnrolled
        }
    }

    override fun authenticate(activity: FragmentActivity, onResult: (BiometricAuthResult) -> Unit) {
        onResult(nextAuthResult)
    }

    override fun enroll(
        activity: FragmentActivity,
        username: String,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        enrolledUsers.add(username.trim().lowercase())
        onResult(BiometricAuthResult.Success(BiometricType.FACE, "Đăng ký khuôn mặt thành công!"))
    }
}

private class FakeBiometricAuthenticator(
    override val type: BiometricType,
    private val availability: BiometricAvailability = BiometricAvailability.Available,
    private val nextResult: BiometricAuthResult = BiometricAuthResult.Failed
) : BiometricAuthenticator {
    override val displayName: String = "Fake $type"

    override fun checkAvailability(context: Context): BiometricAvailability = availability

    override fun authenticate(
        activity: FragmentActivity,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        onResult(nextResult)
    }
}
