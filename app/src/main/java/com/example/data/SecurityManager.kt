package com.example.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.SecureRandom

class SecurityManager(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    companion object {
        private const val TAG = "SecurityManager"
        private const val PREFS_FILE = "secret_vault_secure_prefs"
        private const val KEY_INITIALIZED = "key_initialized"
        private const val KEY_EMAIL = "key_vault_email"
        private const val KEY_PASSWORD = "key_vault_password"
        private const val KEY_BIOMETRIC_ENABLED = "key_biometric_enabled"

        // Default credentials as specified by user
        const val DEFAULT_EMAIL = "aboanasanam42@gmail.com"
        const val DEFAULT_PASSWORD = "malek@123"

        @Volatile
        private var instance: SecurityManager? = null

        fun getInstance(context: Context): SecurityManager {
            return instance ?: synchronized(this) {
                instance ?: SecurityManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private var currentOtp: String? = null
    private var otpGeneratedAt: Long = 0L

    init {
        // Initialize default credentials on first launch
        if (!prefs.getBoolean(KEY_INITIALIZED, false)) {
            prefs.edit()
                .putString(KEY_EMAIL, DEFAULT_EMAIL)
                .putString(KEY_PASSWORD, DEFAULT_PASSWORD)
                .putBoolean(KEY_BIOMETRIC_ENABLED, false)
                .putBoolean(KEY_INITIALIZED, true)
                .apply()
            Log.d(TAG, "Initialized default credentials for $DEFAULT_EMAIL")
        }
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "EncryptedSharedPreferences fallback to standard prefs: ${e.message}")
            context.getSharedPreferences("vault_fallback_prefs", Context.MODE_PRIVATE)
        }
    }

    fun getStoredEmail(): String {
        return prefs.getString(KEY_EMAIL, DEFAULT_EMAIL) ?: DEFAULT_EMAIL
    }

    fun verifyCredentials(emailInput: String, passwordInput: String): Boolean {
        val storedEmail = getStoredEmail().trim()
        val storedPassword = prefs.getString(KEY_PASSWORD, DEFAULT_PASSWORD) ?: DEFAULT_PASSWORD

        val emailMatch = emailInput.trim().equals(storedEmail, ignoreCase = true)
        val passwordMatch = passwordInput == storedPassword

        return emailMatch && passwordMatch
    }

    fun isBiometricEnabled(): Boolean {
        return prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
    }

    fun setBiometricEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun updatePassword(newPassword: String): Boolean {
        if (newPassword.isBlank()) return false
        prefs.edit().putString(KEY_PASSWORD, newPassword).apply()
        // Invalidate current OTP once used
        currentOtp = null
        otpGeneratedAt = 0L
        return true
    }

    /**
     * Generates a 6-digit secure OTP valid for 10 minutes.
     */
    fun generateOtp(): String {
        val random = SecureRandom()
        val code = (100000 + random.nextInt(900000)).toString()
        currentOtp = code
        otpGeneratedAt = System.currentTimeMillis()
        return code
    }

    fun getCurrentOtpForTesting(): String? {
        return currentOtp
    }

    /**
     * Verifies the OTP code within 10 minutes window.
     */
    fun verifyOtp(inputOtp: String): Boolean {
        val activeOtp = currentOtp ?: return false
        val isExpired = (System.currentTimeMillis() - otpGeneratedAt) > (10 * 60 * 1000)
        if (isExpired) return false
        return activeOtp.trim() == inputOtp.trim()
    }
}
