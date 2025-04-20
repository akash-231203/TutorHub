package com.example.learnandearn.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role.Companion.Button
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun Login(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

    ) {
        Text(
            text = "Login",
            fontSize = 50.sp,
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(text = "Email")
            }
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            singleLine = true,
            placeholder = {
                Text(text = "Password")
            },

            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),

            trailingIcon = {
                val icon = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(
                    onClick = { isPasswordVisible = !isPasswordVisible }
                ) {
                    Icon(imageVector = icon, contentDescription = "Visibility")
                }
            }
        )

        Text(
            text = "Forgot Password?",
            color = Color(51, 204, 255),
            modifier = Modifier
                .clickable {
                    navController.navigate("forgot_password")
                }
        )

        Button(
            onClick = {
                navController.navigate("home")
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonColors(Color(0, 153, 255), Color.White, Color.Blue, Color.White),
            modifier = Modifier.padding(20.dp).fillMaxWidth().size(50.dp)
        ) {
            Text(
                text = "Login",
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
        }

        Row() {
            Text(
                text = "Don't have an account?",
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Sign Up",
                color = Color(51, 204, 255),
                modifier = Modifier
                    .clickable {
                        navController.navigate("signUp")
                    }
            )
        }
    }
}