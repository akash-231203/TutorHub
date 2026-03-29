package com.example.tutorHub.ui.theme.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.tutorHub.ViewModel.AuthViewModel
import com.example.tutorHub.data.classes.AuthState
import com.example.tutorHub.data.classes.UserRole
import com.example.tutorHub.data.Domains
import com.example.tutorHub.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignupScreen(
    navController: NavController,
    viewModel: AuthViewModel
) {
    var selectedRole by remember { mutableStateOf(UserRole.STUDENT) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var selectedDomains by remember { mutableStateOf(setOf<String>()) }
    var showDomainDropdown by remember { mutableStateOf(false) }
    val showVerificationDialog by viewModel.showVerificationDialog.observeAsState(false)
    val authState by viewModel.authState.observeAsState(AuthState())
    val domains = Domains.list

    if (showVerificationDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Email Verification", fontWeight = FontWeight.Bold) },
            text = { Text("A verification email has been sent. Please verify your email and sign in to continue.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.onVerificationDialogDismissed()
                        navController.navigate("login")
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("OK")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    LaunchedEffect(authState.isLoggedIn) {
        if (authState.isLoggedIn) {
            navController.navigate("home")
        }
    }

    Scaffold(
        containerColor = BackgroundLight
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            SurfaceBlue,
                            BackgroundLight
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(24.dp))

                // Header
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(PrimaryBlue)
                        .padding(14.dp),
                    tint = Color.White
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Join TutorHub",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp
                    ),
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Create your account to get started",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Role Selection
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CardWhite
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "I am a...",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            FilterChip(
                                onClick = { selectedRole = UserRole.STUDENT },
                                label = { Text("Student") },
                                selected = selectedRole == UserRole.STUDENT,
                                leadingIcon = if (selectedRole == UserRole.STUDENT) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )

                            FilterChip(
                                onClick = { selectedRole = UserRole.TUTOR },
                                label = { Text("Tutor") },
                                selected = selectedRole == UserRole.TUTOR,
                                leadingIcon = if (selectedRole == UserRole.TUTOR) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                } else null,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderGray,
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Email Field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderGray,
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryBlue
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Password Field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = BorderGray,
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = PrimaryBlue
                    )
                )

                // Domain selection for tutors
                if (selectedRole == UserRole.TUTOR) {
                    Spacer(modifier = Modifier.height(14.dp))

                    ExposedDropdownMenuBox(
                        expanded = showDomainDropdown,
                        onExpandedChange = { showDomainDropdown = !showDomainDropdown }
                    ) {
                        OutlinedTextField(
                            value = if (selectedDomains.isEmpty()) "Select Domains" else "${selectedDomains.size} domain(s) selected",
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Teaching Domains") },
                            leadingIcon = {
                                Icon(Icons.Default.School, contentDescription = null)
                            },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDomainDropdown)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryBlue,
                                unfocusedBorderColor = BorderGray,
                                focusedContainerColor = CardWhite,
                                unfocusedContainerColor = CardWhite,
                                disabledContainerColor = CardWhite,
                                disabledTextColor = TextPrimary
                            )
                        )

                        ExposedDropdownMenu(
                            expanded = showDomainDropdown,
                            onDismissRequest = { showDomainDropdown = false }
                        ) {
                            domains.forEach { domain ->
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectedDomains.contains(domain),
                                                onCheckedChange = { checked ->
                                                    selectedDomains = if (checked) {
                                                        selectedDomains + domain
                                                    } else {
                                                        selectedDomains - domain
                                                    }
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(domain)
                                        }
                                    },
                                    onClick = {
                                        selectedDomains = if (selectedDomains.contains(domain)) {
                                            selectedDomains - domain
                                        } else {
                                            selectedDomains + domain
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                if (authState.errorMessage != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = SurfaceRed
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = authState.errorMessage!!,
                            color = ErrorRed,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Create Account Button
                Button(
                    onClick = {
                        if (selectedRole == UserRole.STUDENT) {
                            viewModel.signupStudent(name, email, password)
                        } else {
                            viewModel.signupTutor(name, email, password, selectedDomains.toList())
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = isDetailsComplete(selectedRole, name, email, password, selectedDomains) && !authState.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    )
                ) {
                    if (authState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            "Create Account",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign In Link
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Already have an account?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    TextButton(onClick = { navController.navigate("login") }) {
                        Text(
                            "Sign In",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = PrimaryBlue
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

private fun isDetailsComplete(
    role: UserRole,
    name: String,
    email: String,
    password: String,
    domains: Set<String>
): Boolean {
    val basicComplete = name.isNotBlank() && email.isNotBlank() && password.length >= 6
    return if (role == UserRole.TUTOR) {
        basicComplete && domains.isNotEmpty()
    } else {
        basicComplete
    }
}
