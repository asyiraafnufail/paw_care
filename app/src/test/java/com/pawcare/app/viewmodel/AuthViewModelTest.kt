package com.pawcare.app.viewmodel

import com.pawcare.app.data.model.Role
import com.pawcare.app.data.model.User
import com.pawcare.app.data.repository.FakeAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var authRepo: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepo = FakeAuthRepository()
        viewModel = AuthViewModel(authRepo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testCheckSession_RoutesAdminAndOwner() = runTest {
        authRepo.setMockUser(User(uid = "a1", email = "admin@pawcare.com", role = "admin"))
        viewModel.checkSession()
        assertEquals(Role.ADMIN, viewModel.uiState.value.authenticatedRole)
        assertFalse(viewModel.uiState.value.isCheckingSession)

        authRepo.setMockUser(User(uid = "o1", email = "owner@pawcare.com", role = "owner"))
        viewModel.checkSession()
        assertEquals(Role.OWNER, viewModel.uiState.value.authenticatedRole)

        authRepo.setMockUser(null)
        viewModel.checkSession()
        assertNull(viewModel.uiState.value.authenticatedRole)
    }

    @Test
    fun testSignIn_ValidationErrorBlocksRepositoryCall() = runTest {
        viewModel.signIn("bad-email", "123")
        assertNotNull(viewModel.uiState.value.emailError)
        assertNotNull(viewModel.uiState.value.passwordError)
        assertNull(viewModel.uiState.value.authenticatedRole)
    }

    @Test
    fun testSignUp_ForcesOwnerRoleAndValidatesConfirmPassword() = runTest {
        viewModel.signUp("owner@pawcare.com", "secret123", "different")
        assertNotNull(viewModel.uiState.value.confirmPasswordError)
        assertNull(viewModel.uiState.value.authenticatedRole)

        viewModel.signUp("owner@pawcare.com", "secret123", "secret123")
        assertNull(viewModel.uiState.value.confirmPasswordError)
        assertEquals(Role.OWNER, viewModel.uiState.value.authenticatedRole)
    }
}
