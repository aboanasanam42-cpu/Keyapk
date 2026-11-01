package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.SecurityManager
import com.example.data.VaultFileManager
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.VaultDashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : FragmentActivity() {

    private lateinit var securityManager: SecurityManager
    private lateinit var vaultFileManager: VaultFileManager

    private var onBiometricSuccess: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        securityManager = SecurityManager.getInstance(this)
        vaultFileManager = VaultFileManager(this)

        setContent {
            MyApplicationTheme {
                VaultApp(
                    securityManager = securityManager,
                    vaultFileManager = vaultFileManager,
                    onRequestBiometricAuth = {
                        launchBiometricPrompt()
                    }
                )
            }
        }
    }

    private fun launchBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val prompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onBiometricSuccess?.invoke()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If user cancels or no sensor, show brief toast
                    if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                        errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                    ) {
                        Toast.makeText(this@MainActivity, errString, Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        this@MainActivity,
                        "فشلت محاولة التحقق بالبصمة",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("الخزنة السرية")
            .setSubtitle("المصادقة ببصمة الإصبع لفتح الخزنة")
            .setNegativeButtonText("إلغاء")
            .build()

        prompt.authenticate(promptInfo)
    }

    @Composable
    private fun VaultApp(
        securityManager: SecurityManager,
        vaultFileManager: VaultFileManager,
        onRequestBiometricAuth: () -> Unit
    ) {
        var isVaultUnlocked by remember { mutableStateOf(false) }

        // Register biometric success hook
        onBiometricSuccess = {
            isVaultUnlocked = true
        }

        // Handle Back button: lock vault if inside
        BackHandler(enabled = isVaultUnlocked) {
            isVaultUnlocked = false
        }

        if (isVaultUnlocked) {
            VaultDashboardScreen(
                vaultFileManager = vaultFileManager,
                onLockVault = { isVaultUnlocked = false },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LoginScreen(
                securityManager = securityManager,
                onLoginSuccess = { isVaultUnlocked = true },
                onRequestBiometricAuth = onRequestBiometricAuth,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
