package com.example.practica6.ui.theme.auth

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.practica6.R
import com.example.practica6.ui.theme.admin.AdminActivity
import com.example.practica6.ui.theme.profile.ProfileActivity


class LoginActivity : ComponentActivity() {

    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar el ViewModel
        viewModel = ViewModelProvider(this).get(AuthViewModel::class.java)

        // Referencias a los elementos de UI
        val usernameEditText = findViewById<EditText>(R.id.etUsername)
        val passwordEditText = findViewById<EditText>(R.id.etPassword)
        val loginButton = findViewById<Button>(R.id.btnLogin)
        val registerButton = findViewById<Button>(R.id.btnRegister)

        loginButton.setOnClickListener {
            val username = usernameEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (validateInput(usernameEditText, username, passwordEditText, password)) {
                // Llamada al ViewModel para hacer el login
                viewModel.login(username, password) { success, user ->
                    if (success && user != null) {
                        Toast.makeText(this, "Bienvenido, ${user.username}", Toast.LENGTH_LONG).show()

                        // Verificar el rol del usuario y redirigir
                        val roles = user.roles.map { it.nombre }
                        when {
                            roles.contains("ROLE_ADMIN") -> {
                                // Si el usuario es administrador, iniciar AdminActivity
                                val intent = Intent(this, AdminActivity::class.java)
                                startActivity(intent)
                            }
                            roles.contains("ROLE_USER") -> {
                                // Si el usuario es un usuario normal, iniciar ProfileActivity con los datos del perfil
                                val intent = Intent(this, ProfileActivity::class.java).apply {
                                    putExtra("nombre", user.nombre)
                                    putExtra("apellidoPaterno", user.apaterno)
                                    putExtra("apellidoMaterno", user.amaterno)
                                    putExtra("fechaNacimiento", user.cumple)
                                    putExtra("username", user.username)
                                }
                                startActivity(intent)
                            }
                            else -> {
                                Toast.makeText(this, "Usuario no válido", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "Error de autenticación", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }


        registerButton.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun validateInput(usernameEditText: EditText, username: String, passwordEditText: EditText, password: String): Boolean {
        var isValid = true

        // Validar formato de correo
        /*if (!Patterns.EMAIL_ADDRESS.matcher(username).matches()) {
            usernameEditText.error = "Correo electrónico inválido"
            usernameEditText.requestFocus()
            animateView(usernameEditText)
            isValid = false
        }*/

        // Validar longitud de contraseña
        if (password.length < 8) {
            passwordEditText.error = "La contraseña debe tener al menos 8 caracteres"
            passwordEditText.requestFocus()
            animateView(passwordEditText)
            isValid = false
        }

        return isValid
    }

    private fun animateView(view: EditText) {
        view.setBackgroundColor(ContextCompat.getColor(this, R.color.errorColor))
        view.animate()
            .setDuration(100)
            .alpha(0.5f)
            .withEndAction {
                view.alpha = 1.0f
                view.setBackgroundColor(ContextCompat.getColor(this, R.color.defaultColor))
            }
    }
}
