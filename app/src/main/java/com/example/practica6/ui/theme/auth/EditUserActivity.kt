package com.example.practica6.ui.theme.auth


import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.practica6.AppDatabase

import com.example.practica6.R
import com.example.practica6.UserSessionManager
import com.example.practica6.data.api.RetrofitClient
import com.example.practica6.data.models.User
import com.example.practica6.sync.UserRepository
import org.mindrot.jbcrypt.BCrypt

class EditUserActivity : AppCompatActivity() {
    private lateinit var viewModel: AuthViewModel
    private lateinit var currentUser: User

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.edit_user_activity)

        // Inicializa Retrofit y Room
        val apiService = RetrofitClient.instance
        val userDao = AppDatabase.getInstance(applicationContext).userDao()
        val userRepository = UserRepository(apiService, userDao,applicationContext)
        val sessionManager = UserSessionManager(applicationContext) // Inicializa el UserSessionManager

        // Configura el ViewModel con la fábrica
        val factory = AuthViewModelFactory(userRepository,sessionManager)
        viewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)

        // Referencia a los campos de edición
        val nameField: EditText = findViewById(R.id.etNombre)
        val lastNameField: EditText = findViewById(R.id.etApellidoPaterno)
        val lastName2Field: EditText = findViewById(R.id.etApellidoMaterno)
        val birthdateField: EditText = findViewById(R.id.etFechaNacimiento)
        val usernameField: EditText = findViewById(R.id.etUsuario)
        val passwordField: EditText = findViewById(R.id.etContrasena)
        val updateButton: Button = findViewById(R.id.btnGuardarCambios)
        val backButton: Button = findViewById(R.id.btnRegresar)

        // Obtén el nombre de usuario del Intent
        val username = intent.getStringExtra("USERNAME") ?: "defaultUsername"

        // Carga los datos del usuario
        viewModel.getUserProfile(username)

        // Observa los datos del usuario y actualiza los campos de texto
        viewModel.userLiveData.observe(this) { user ->
            if (user != null) {
                currentUser = user
                // Coloca los valores actuales en los campos
                nameField.setText(user.nombre)
                lastNameField.setText(user.apaterno)
                lastName2Field.setText(user.amaterno)
                birthdateField.setText(user.cumple)
                usernameField.setText(user.username)
                passwordField.setText("") // Deja el campo de contraseña vacío por seguridad
            } else {
                Toast.makeText(this, "Error al cargar los datos del usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Observa el estado de la actualización
        viewModel.updateStatus.observe(this) { status ->
            if (status == 200) {
                Toast.makeText(this, "Usuario actualizado correctamente", Toast.LENGTH_LONG).show()
                setResult(RESULT_OK, intent)
                finish()
            } else {
                Toast.makeText(this, "Error al actualizar usuario", Toast.LENGTH_SHORT).show()
            }
        }

        // Configura el botón de guardar cambios
        updateButton.setOnClickListener {
            val name = nameField.text.toString()
            val lastName = lastNameField.text.toString()
            val lastName2 = lastName2Field.text.toString()
            val birthdate = birthdateField.text.toString()
            val password = passwordField.text.toString()

            if (name.isNotEmpty() && lastName.isNotEmpty() && lastName2.isNotEmpty() && birthdate.isNotEmpty()) {
                // Encripta la nueva contraseña solo si se ingresó una nueva
                val hashedPassword = if (password.isNotEmpty()) BCrypt.hashpw(
                    password,
                    BCrypt.gensalt()
                ) else currentUser.password

                // Actualiza los datos del usuario
                val updatedUser = currentUser.copy(
                    nombre = name,
                    apaterno = lastName,
                    amaterno = lastName2,
                    cumple = birthdate,
                    password = hashedPassword
                )

                // Llama al ViewModel para actualizar el usuario
                viewModel.updateUser(updatedUser)
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Configura el botón de regresar
        backButton.setOnClickListener {
            finish()
        }
    }
}

