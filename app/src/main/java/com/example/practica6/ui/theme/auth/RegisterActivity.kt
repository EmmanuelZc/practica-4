package com.example.practica6.ui.theme.auth

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.practica6.AppDatabase
import com.example.practica6.R
import com.example.practica6.UserSessionManager
import com.example.practica6.data.api.RetrofitClient
import com.example.practica6.data.models.Rol
import com.example.practica6.data.models.RolLocal
import com.example.practica6.data.models.User
import com.example.practica6.data.models.UserLocal
import com.example.practica6.data.models.UserRoleCrossRef
import com.example.practica6.sync.UserRepository
import org.mindrot.jbcrypt.BCrypt

class RegisterActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registro_activity)

        // Inicializa Retrofit y Room
        val apiService = RetrofitClient.instance
        val userDao = AppDatabase.getInstance(applicationContext).userDao()
        val userRepository = UserRepository(apiService, userDao,applicationContext)
        val sessionManager = UserSessionManager(applicationContext) // Inicializa el UserSessionManager

        // Configura el ViewModel
        val factory = AuthViewModelFactory(userRepository,sessionManager)
        viewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)

        // Referencia a los campos de texto en el layout
        val nameField: EditText = findViewById(R.id.etNombre)
        val lastNameField: EditText = findViewById(R.id.etApellidoPaterno)
        val lastName2Field: EditText = findViewById(R.id.etApellidoMaterno)
        val birthdateField: EditText = findViewById(R.id.etFechaNacimiento)
        val usernameField: EditText = findViewById(R.id.etUsuario)
        val passwordField: EditText = findViewById(R.id.etContrasena)
        val registerButton: Button = findViewById(R.id.btnRegistrar)
        val backButton: Button = findViewById(R.id.btnRegresar)

        // Observa el estado del registro
        viewModel.registerStatus.observe(this) { status ->
            if (status == 201) {
                Toast.makeText(this, "Usuario registrado correctamente en el servidor", Toast.LENGTH_LONG).show()
                setResult(RESULT_OK)
                finish()
            } else {
                Toast.makeText(this, "Error al registrar usuario en el servidor", Toast.LENGTH_SHORT).show()
            }
        }



        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

        // Configura el botón de registro
        registerButton.setOnClickListener {
            val name = nameField.text.toString()
            val lastName = lastNameField.text.toString()
            val lastName2 = lastName2Field.text.toString()
            val birthdate = birthdateField.text.toString()
            val username = usernameField.text.toString()
            val password = passwordField.text.toString()

            if (validateInput(passwordField, password)) {
                val hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt())
                val user = User(
                    id = 0,
                    nombre = name,
                    apaterno = lastName,
                    amaterno = lastName2,
                    cumple = birthdate,
                    username = username,
                    password = hashedPassword,
                    enabled = true,
                    roles = listOf(Rol(id = 2, nombre = "ROLE_USER"))
                )

                // Inserta el usuario localmente
                val userLocal = user.toLocal().copy(isSynced = false)
                val rolesLocal = user.roles.map { it.toLocal() }
                val crossRefs = user.roles.map { role -> UserRoleCrossRef(userLocal.id, role.id) }

                viewModel.insertUserWithRoles(userLocal, rolesLocal, crossRefs)
                // Sincroniza con el servidor
                // Verifica si hay conexión antes de sincronizar
                if (viewModel.isOnline(this)) {
                    viewModel.registerUser(user)
                } else {
                    Toast.makeText(
                        this,
                        "Usuario registrado localmente. Sin conexión al servidor.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
            }

    }

    private fun validateInput(passwordField: EditText, password: String): Boolean {
        if (password.isBlank()) {
            passwordField.error = "La contraseña no puede estar vacía"
            passwordField.requestFocus()
            return false
        }
        if (password.length < 8) {
            passwordField.error = "La contraseña debe tener al menos 8 caracteres"
            passwordField.requestFocus()
            return false
        }
        return true
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

    private fun User.toLocal(): UserLocal {
        return UserLocal(
            id = this.id,
            nombre = this.nombre,
            apaterno = this.apaterno,
            amaterno = this.amaterno,
            cumple = this.cumple,
            username = this.username,
            password = this.password,
            enabled = this.enabled
        )
    }

    private fun Rol.toLocal(): RolLocal {
        return RolLocal(
            id = this.id,
            nombre = this.nombre
        )
    }
}
