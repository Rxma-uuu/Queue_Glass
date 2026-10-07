package com.example.myapplication.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Patterns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

sealed interface AuthState {
    data object Unauthenticated : AuthState
    data class Authenticated(val email: String, val fullName: String) : AuthState
    data object OfflineDemo : AuthState
}

data class AuthResult(
    val success: Boolean,
    val errorMessage: String? = null
)

class AuthRepository(context: Context) {

    private val db = InvestigationDatabase.get(context)
    private val userDao = db.userDao()
    private val prefs: SharedPreferences = context.getSharedPreferences("queglass_auth_prefs", Context.MODE_PRIVATE)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        restoreSession()
    }

    private fun restoreSession() {
        val mode = prefs.getString("auth_mode", null)
        val email = prefs.getString("auth_email", null)
        val name = prefs.getString("auth_name", "") ?: ""

        if (mode == "AUTHENTICATED" && !email.isNullOrBlank()) {
            _authState.value = AuthState.Authenticated(email, name)
        } else if (mode == "OFFLINE_DEMO") {
            _authState.value = AuthState.OfflineDemo
        } else {
            _authState.value = AuthState.Unauthenticated
        }
    }

    suspend fun signIn(emailInput: String, passwordInput: String): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        if (!isValidEmail(email)) {
            return@withContext AuthResult(false, "Please enter a valid email address.")
        }
        if (passwordInput.isBlank()) {
            return@withContext AuthResult(false, "Password cannot be empty.")
        }

        try {
            val user = userDao.findByEmail(email)
                ?: return@withContext AuthResult(false, "No account found with this email. Please create an account.")

            val computedHash = hashPassword(passwordInput, user.salt)
            if (computedHash == user.passwordHash) {
                userDao.updateUser(user.copy(lastLoginMs = System.currentTimeMillis()))
                
                prefs.edit()
                    .putString("auth_mode", "AUTHENTICATED")
                    .putString("auth_email", user.email)
                    .putString("auth_name", user.fullName)
                    .apply()

                _authState.value = AuthState.Authenticated(user.email, user.fullName)
                AuthResult(true)
            } else {
                AuthResult(false, "Incorrect password. Please try again or reset your password.")
            }
        } catch (e: Exception) {
            AuthResult(false, "Authentication unavailable: ${e.localizedMessage}")
        }
    }

    suspend fun createAccount(emailInput: String, passwordInput: String, fullNameInput: String): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        if (!isValidEmail(email)) {
            return@withContext AuthResult(false, "Please enter a valid email address.")
        }
        if (passwordInput.length < 6) {
            return@withContext AuthResult(false, "Password must be at least 6 characters long.")
        }

        try {
            val existing = userDao.findByEmail(email)
            if (existing != null) {
                return@withContext AuthResult(false, "An account with this email already exists. Please sign in.")
            }

            val salt = generateSalt()
            val hash = hashPassword(passwordInput, salt)
            val newUser = UserAccount(
                email = email,
                passwordHash = hash,
                salt = salt,
                fullName = fullNameInput.trim(),
                createdAtMs = System.currentTimeMillis(),
                lastLoginMs = System.currentTimeMillis()
            )

            userDao.insertUser(newUser)

            prefs.edit()
                .putString("auth_mode", "AUTHENTICATED")
                .putString("auth_email", newUser.email)
                .putString("auth_name", newUser.fullName)
                .apply()

            _authState.value = AuthState.Authenticated(newUser.email, newUser.fullName)
            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, "Unable to create account: ${e.localizedMessage}")
        }
    }

    suspend fun resetPassword(emailInput: String, newPasswordInput: String): AuthResult = withContext(Dispatchers.IO) {
        val email = emailInput.trim().lowercase()
        if (!isValidEmail(email)) {
            return@withContext AuthResult(false, "Please enter a valid email address.")
        }
        if (newPasswordInput.length < 6) {
            return@withContext AuthResult(false, "New password must be at least 6 characters long.")
        }

        try {
            val user = userDao.findByEmail(email)
                ?: return@withContext AuthResult(false, "No account found with email '$email'.")

            val salt = generateSalt()
            val hash = hashPassword(newPasswordInput, salt)
            userDao.updateUser(user.copy(passwordHash = hash, salt = salt))

            AuthResult(true)
        } catch (e: Exception) {
            AuthResult(false, "Password reset failed: ${e.localizedMessage}")
        }
    }

    fun enterOfflineDemo() {
        prefs.edit()
            .putString("auth_mode", "OFFLINE_DEMO")
            .remove("auth_email")
            .remove("auth_name")
            .apply()
        _authState.value = AuthState.OfflineDemo
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _authState.value = AuthState.Unauthenticated
    }

    fun getAuthAvailabilityStatus(): String {
        return "Local Encrypted Account Store Active (Salted SHA-256)"
    }

    private fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun generateSalt(): String {
        val random = SecureRandom()
        val saltBytes = ByteArray(16)
        random.nextBytes(saltBytes)
        return saltBytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(saltHex.toByteArray(Charsets.UTF_8))
        val hashedBytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }
}
