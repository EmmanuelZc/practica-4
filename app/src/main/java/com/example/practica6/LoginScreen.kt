package com.example.practica6

import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.practica6.ui.theme.admin.AdminActivity
import com.example.practica6.ui.theme.auth.AuthViewModel
import com.example.practica6.ui.theme.auth.RegisterActivity
import com.example.practica6.ui.theme.profile.ProfileActivity

@Composable
fun LoginScreen(
    onGoogleSignInClick: () -> Unit,
    onFacebookSignInClick: () -> Unit,
    onLoginSuccess: (String, Any?) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf("") }
    val loginState = remember { mutableStateOf(false) }
    val roles = remember { mutableStateOf<List<String>>(emptyList()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(id = R.drawable.escudo),
            contentDescription = "Login image",
            modifier = Modifier.size(300.dp)
        )
        Text(text = "Bienvenido!!", fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Text(text = "Inicia sesión o regístrate")
        Spacer(modifier = Modifier.height(4.dp))

        OutlinedTextField(
            value = username.value,
            onValueChange = { username.value = it },
            label = { Text(text = "Email address") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text(text = "Password") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            try {
                val intent = Intent(context, RegisterActivity::class.java)
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error al abrir la pantalla de registro: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }) {
            Text(text = "Regístrate")
        }

        Button(onClick = {
            if (validateInput(username.value, password.value)) {
                viewModel.login(username.value, password.value) { success, user ->
                    if (success && user != null) {
                        val userId = user.id ?: -1 // Asegura que `id` siempre tenga un valor por defecto
                        Log.d("LoginScreen", "Usuario logueado con ID: $userId")

                        onLoginSuccess(user.username, userId)
                    } else {
                        errorMessage.value = "Error de autenticación"
                    }
                }
            } else {
                errorMessage.value = "Verifique sus datos"
            }
        }) {
            Text(text = "Iniciar sesión")
        }




        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "¿Olvidaste tu contraseña?",
            modifier = Modifier.clickable {
                Toast.makeText(context, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(text = "Inicia sesión con:")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Image(
                painter = painterResource(id = R.drawable.fb),
                contentDescription = "Facebook",
                modifier = Modifier
                    .size(60.dp)
                    .clickable { onFacebookSignInClick() }
            )
            Image(
                painter = painterResource(id = R.drawable.google),
                contentDescription = "Google",
                modifier = Modifier
                    .size(60.dp)
                    .clickable { onGoogleSignInClick() }
            )
        }

        // Mostrar error si lo hay
        if (errorMessage.value.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { errorMessage.value = "" },
                title = { Text(text = "Error") },
                text = { Text(text = errorMessage.value) },
                confirmButton = {
                    Button(onClick = { errorMessage.value = "" }) {
                        Text("Cerrar")
                    }
                }
            )
        }

        // Redirigir según el rol
        LaunchedEffect(roles.value) {
            if (roles.value.isNotEmpty()) {
                when {
                    roles.value.contains("ROLE_ADMIN") -> {
                        val intent = Intent(context, AdminActivity::class.java)
                        context.startActivity(intent)
                    }
                    roles.value.contains("ROLE_USER") -> {
                        val intent = Intent(context, ProfileActivity::class.java)
                        context.startActivity(intent)
                    }
                    else -> {
                        Toast.makeText(context, "Rol no válido", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}

fun validateInput(username: String, password: String): Boolean {
    return username.isNotBlank() && password.length >= 8
}

