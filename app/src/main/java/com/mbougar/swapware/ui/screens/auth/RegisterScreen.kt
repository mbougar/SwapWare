package com.mbougar.swapware.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mbougar.swapware.viewmodel.AuthViewModel
import com.mbougar.swapware.viewmodel.AuthState

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf<String?>(null) }


    LaunchedEffect(authState.isLoginSuccess) {
        if (authState.isLoginSuccess) {
            onRegisterSuccess()
            viewModel.consumeLoginSuccess()
        }
    }

    RegisterScreenContent(
        authState = authState,
        email = email,
        password = password,
        confirmPassword = confirmPassword,
        onEmailChange = { email = it },
        onPasswordChange = {
            password = it
            passwordError = null
        },
        onConfirmPasswordChange = {
            confirmPassword = it
            passwordError = null
        },
        onSignupClick = {
            if (password != confirmPassword) {
                passwordError = "Passwords do not match."
            } else if (password.length < 6) {
                passwordError = "Password should be at least 6 characters."
            }
            else {
                viewModel.signup(email, password)
            }
        },
        onNavigateToLogin = onNavigateToLogin,
        passwordError = passwordError
    )
}

@Composable
fun RegisterScreenContent(
    authState: AuthState,
    email: String,
    password: String,
    confirmPassword: String,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSignupClick: () -> Unit,
    onNavigateToLogin: () -> Unit,
    passwordError: String?
) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Create Account", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                isError = authState.error != null && authState.error.contains("Email", ignoreCase = true)
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = passwordError != null || (authState.error != null && authState.error.contains("password", ignoreCase = true))
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                isError = passwordError != null
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (passwordError != null) {
                Text(
                    text = passwordError,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else if (authState.error != null) {
                Text(
                    text = authState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))


            if (authState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(bottom = 16.dp))
            } else {
                Button(
                    onClick = onSignupClick,
                    enabled = !authState.isLoading && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Up")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Already have an account? ")
                ClickableText(
                    text = AnnotatedString("Login"),
                    onClick = { onNavigateToLogin() },
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                )
            }
        }
    }
}