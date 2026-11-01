package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.EmailService
import com.example.data.SecurityManager
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultCard
import com.example.ui.theme.VaultError
import com.example.ui.theme.VaultSuccess
import com.example.ui.theme.VaultSurface
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordDialog(
    securityManager: SecurityManager,
    onDismiss: () -> Unit,
    onPasswordChanged: (String) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val targetEmail = securityManager.getStoredEmail()

    var currentStep by remember { mutableIntStateOf(1) } // 1: Send OTP, 2: Verify & Reset
    var otpInput by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isErrorMessage by remember { mutableStateOf(false) }
    var generatedOtpDisplay by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = VaultSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(GoldPrimary.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (currentStep == 1) Icons.Default.Email else Icons.Default.Key,
                        contentDescription = "Password Reset",
                        tint = GoldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = if (currentStep == 1) "التحقق من البريد الإلكتروني" else "إدخال كلمة المرور الجديدة",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 18.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (currentStep == 1)
                        "سيتم إرسال رمز تحقق مؤلف من 6 أرقام إلى البريد المعتمد للخزنة:"
                    else
                        "أدخل رمز التحقق الذي وصلك ثم اكتب كلمة المرور الجديدة:",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (currentStep == 1) {
                    // Display target email box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(VaultCard, RoundedCornerShape(10.dp))
                            .border(1.dp, VaultBorder, RoundedCornerShape(10.dp))
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = targetEmail,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = GoldPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isLoading) {
                        CircularProgressIndicator(
                            color = GoldPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "جاري إرسال رمز التحقق عبر SMTP...",
                            style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                        )
                    } else {
                        Button(
                            onClick = {
                                isLoading = true
                                isErrorMessage = false
                                statusMessage = null
                                coroutineScope.launch {
                                    val otp = securityManager.generateOtp()
                                    val result = EmailService.sendOtpEmail(
                                        recipientEmail = targetEmail,
                                        otpCode = otp
                                    )
                                    isLoading = false
                                    statusMessage = result.message
                                    generatedOtpDisplay = result.otpCode
                                    currentStep = 2
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("send_otp_button")
                        ) {
                            Text(
                                "إرسال رمز التحقق (OTP)",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Step 2: OTP & New Password input
                    if (generatedOtpDisplay != null) {
                        // Display helpful test badge showing the OTP
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(GoldPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .border(1.dp, GoldPrimary.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "رمز التحقق: $generatedOtpDisplay",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { if (it.length <= 6) otpInput = it },
                        label = { Text("رمز التحقق (6 أرقام)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("otp_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("كلمة المرور الجديدة") },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle Password",
                                    tint = TextSecondary
                                )
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_password_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("تأكيد كلمة المرور الجديدة") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedLabelColor = GoldPrimary,
                            unfocusedLabelColor = TextSecondary,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_new_password_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (!securityManager.verifyOtp(otpInput)) {
                                isErrorMessage = true
                                statusMessage = "رمز التحقق غير صحيح أو انتهت صلاحيته"
                                return@Button
                            }
                            if (newPassword.length < 4) {
                                isErrorMessage = true
                                statusMessage = "يجب ألا تقل كلمة المرور عن 4 أحرف"
                                return@Button
                            }
                            if (newPassword != confirmPassword) {
                                isErrorMessage = true
                                statusMessage = "كلمتا المرور غير متطابقتين"
                                return@Button
                            }

                            val success = securityManager.updatePassword(newPassword)
                            if (success) {
                                onPasswordChanged(newPassword)
                                onDismiss()
                            } else {
                                isErrorMessage = true
                                statusMessage = "حدث خطأ أثناء حفظ كلمة المرور"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_new_password_button")
                    ) {
                        Text(
                            "حفظ كلمة المرور الجديدة",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                AnimatedVisibility(visible = statusMessage != null) {
                    Text(
                        text = statusMessage ?: "",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = if (isErrorMessage) VaultError else VaultSuccess,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    border = androidx.compose.foundation.BorderStroke(1.dp, VaultBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text(
                        "إلغاء",
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
