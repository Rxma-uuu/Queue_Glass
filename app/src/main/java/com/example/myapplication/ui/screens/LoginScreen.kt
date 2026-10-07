package com.example.myapplication.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.BorderColor
import com.example.myapplication.ui.theme.CyanAccent
import com.example.myapplication.ui.theme.ElevatedSurface
import com.example.myapplication.ui.theme.MainBackground
import com.example.myapplication.ui.theme.NegativeRed
import com.example.myapplication.ui.theme.PanelBackground
import com.example.myapplication.ui.theme.PositiveGreen
import com.example.myapplication.ui.theme.PrimaryText
import com.example.myapplication.ui.theme.SecondaryText

enum class AuthTab {
    SignIn, CreateAccount, ResetPassword
}

@Composable
fun LoginScreen(
    authAvailabilityStatus: String,
    onSignIn: (email: String, pass: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onCreateAccount: (email: String, pass: String, name: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onResetPassword: (email: String, newPass: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onExploreOfflineDemo: () -> Unit
) {
    var activeTab by remember { mutableStateOf(AuthTab.SignIn) }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MainBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // QUEUEGLASS Branding
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(PanelBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "QUEUEGLASS",
                    color = PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "L3 Match Order Book & Quantitative Research Platform",
            color = SecondaryText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Main Login Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(PanelBackground)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(20.dp)
        ) {
            Column {
                // Tab Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ElevatedSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    AuthTab.entries.forEach { tab ->
                        val selected = tab == activeTab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) CyanAccent else ElevatedSurface)
                                .clickable {
                                    activeTab = tab
                                    errorMessage = null
                                    successMessage = null
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (tab) {
                                    AuthTab.SignIn -> "Sign In"
                                    AuthTab.CreateAccount -> "Register"
                                    AuthTab.ResetPassword -> "Reset"
                                },
                                color = if (selected) MainBackground else PrimaryText,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Error / Success Banners
                AnimatedVisibility(visible = errorMessage != null) {
                    errorMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(NegativeRed.copy(alpha = 0.15f))
                                .border(1.dp, NegativeRed, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = NegativeRed,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = successMessage != null) {
                    successMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(PositiveGreen.copy(alpha = 0.15f))
                                .border(1.dp, PositiveGreen, RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg,
                                color = PositiveGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Full Name (Only for Create Account)
                if (activeTab == AuthTab.CreateAccount) {
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name", color = SecondaryText, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = CyanAccent) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedContainerColor = ElevatedSurface,
                            unfocusedContainerColor = ElevatedSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address", color = SecondaryText, fontSize = 12.sp) },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = CyanAccent) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedContainerColor = ElevatedSurface,
                        unfocusedContainerColor = ElevatedSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(
                            if (activeTab == AuthTab.ResetPassword) "New Password (min 6 chars)" else "Password",
                            color = SecondaryText,
                            fontSize = 12.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CyanAccent) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle password visibility",
                                tint = SecondaryText
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanAccent,
                        unfocusedBorderColor = BorderColor,
                        focusedTextColor = PrimaryText,
                        unfocusedTextColor = PrimaryText,
                        focusedContainerColor = ElevatedSurface,
                        unfocusedContainerColor = ElevatedSurface
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Confirm Password (Only for Create Account)
                if (activeTab == AuthTab.CreateAccount) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password", color = SecondaryText, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = CyanAccent) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = SecondaryText
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = BorderColor,
                            focusedTextColor = PrimaryText,
                            unfocusedTextColor = PrimaryText,
                            focusedContainerColor = ElevatedSurface,
                            unfocusedContainerColor = ElevatedSurface
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Submit Button
                Button(
                    onClick = {
                        errorMessage = null
                        successMessage = null

                        if (email.isBlank()) {
                            errorMessage = "Please enter an email address."
                            return@Button
                        }

                        if (activeTab == AuthTab.CreateAccount && password != confirmPassword) {
                            errorMessage = "Passwords do not match."
                            return@Button
                        }

                        isLoading = true
                        when (activeTab) {
                            AuthTab.SignIn -> {
                                onSignIn(email, password) { success, err ->
                                    isLoading = false
                                    if (!success) errorMessage = err
                                }
                            }
                            AuthTab.CreateAccount -> {
                                onCreateAccount(email, password, fullName) { success, err ->
                                    isLoading = false
                                    if (!success) errorMessage = err
                                }
                            }
                            AuthTab.ResetPassword -> {
                                onResetPassword(email, password) { success, err ->
                                    isLoading = false
                                    if (success) {
                                        successMessage = "Password successfully reset! You can now sign in."
                                        activeTab = AuthTab.SignIn
                                    } else {
                                        errorMessage = err
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyanAccent,
                        contentColor = MainBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MainBackground,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = when (activeTab) {
                                AuthTab.SignIn -> "Sign In to Account"
                                AuthTab.CreateAccount -> "Create Account"
                                AuthTab.ResetPassword -> "Reset Password"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Offline Demo Divider
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(BorderColor)
            )
            Text(
                text = " OR ",
                color = SecondaryText,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(BorderColor)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Explore Offline Demo Button
        OutlinedButton(
            onClick = onExploreOfflineDemo,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CyanAccent
            ),
            border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = SolidColor(CyanAccent)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.PlayCircle,
                    contentDescription = null,
                    tint = CyanAccent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Explore Offline Demo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = CyanAccent
                    )
                    Text(
                        text = "Instant access to native L3 market simulation & replay",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = SecondaryText
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Security / Availability Footer
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = null,
                tint = PositiveGreen,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = authAvailabilityStatus,
                color = SecondaryText,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
