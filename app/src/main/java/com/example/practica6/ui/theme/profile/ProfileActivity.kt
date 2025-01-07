package com.example.practica6.ui.theme.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practica6.AppDatabase
import com.example.practica6.MainActivity
import com.example.practica6.R
import com.example.practica6.UserSessionManager
import com.example.practica6.data.api.ApiService
import com.example.practica6.data.api.Book

import com.example.practica6.data.api.RetrofitClient
import com.example.practica6.data.models.FavoriteLocal
import com.example.practica6.data.models.FavoriteRemote
import com.example.practica6.sync.FavsRepository
import com.example.practica6.sync.UserRepository
import com.example.practica6.ui.theme.auth.AuthViewModel
import com.example.practica6.ui.theme.auth.AuthViewModelFactory
import com.example.practica6.ui.theme.auth.BookAdapter
import com.example.practica6.ui.theme.auth.EditUserActivity
import com.example.practica6.ui.theme.auth.LoginActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
    private lateinit var nameView: TextView
    private lateinit var lastNameView: TextView
    private lateinit var middleNameView: TextView
    private lateinit var birthDateView: TextView
    private lateinit var usernameView: TextView
    private lateinit var editButton: Button
    private lateinit var logoutButton: Button
    private lateinit var favsRepository: FavsRepository
    private val favoriteBooks = mutableListOf<FavoriteLocal>()
    private lateinit var viewModel: AuthViewModel
    private lateinit var sessionManager: UserSessionManager
    private var currentUsername: String = ""
    private lateinit var apiService: ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.perfil_usuario_activity)

        // Inicializa Retrofit, Room y SessionManager
        apiService = RetrofitClient.instance

        val userDao = AppDatabase.getInstance(applicationContext).userDao()
        val userRepository = UserRepository(apiService, userDao,applicationContext)
        val favoriteDao = AppDatabase.getInstance(applicationContext).favoriteDao()
        favsRepository = FavsRepository(apiService, favoriteDao) // Inicialización de favsRepository

        sessionManager = UserSessionManager(applicationContext)

        // Configura el ViewModel con la fábrica
        val factory = AuthViewModelFactory(userRepository, sessionManager)
        viewModel = ViewModelProvider(this, factory).get(AuthViewModel::class.java)

        // Inicializar vistas
        nameView = findViewById(R.id.tvNombre)
        lastNameView = findViewById(R.id.tvApellidoPaterno)
        middleNameView = findViewById(R.id.tvApellidoMaterno)
        birthDateView = findViewById(R.id.tvFechaNacimiento)
        usernameView = findViewById(R.id.tvUsuario)
        editButton = findViewById(R.id.btnEditar)
        logoutButton = findViewById(R.id.btnCerrarSesion)

        // Obtener datos del usuario desde SessionManager
        currentUsername = sessionManager.getUsername() ?: ""
        if (currentUsername.isNotEmpty()) {
            setupObservers()
            loadUserProfile(currentUsername)
        } else {
            Toast.makeText(this, "No se encontró una sesión activa.", Toast.LENGTH_SHORT).show()
            redirectToLogin()
        }

        // Configura botón de edición
        val editUserLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                loadUserProfile(currentUsername) // Recarga el perfil tras edición
            }
        }

        editButton.setOnClickListener {
            val intent = Intent(this, EditUserActivity::class.java).apply {
                putExtra("USERNAME", currentUsername)
            }
            editUserLauncher.launch(intent)
        }

        // Configura botón de cerrar sesión
        logoutButton.setOnClickListener {
            sessionManager.clearSession()

            // Regresa a MainActivity
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    Toast.makeText(this, "Inicio seleccionado", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_search -> {
                    showSearchDialog()
                    true
                }
                R.id.nav_favorites -> {
                    if (sessionManager.isLoggedIn()) {
                        val intent = Intent(this, FavoritesActivity::class.java).apply {
                            putExtra("USER_ID", sessionManager.getUserId())
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this, "Por favor, inicie sesión nuevamente.", Toast.LENGTH_SHORT).show()
                        redirectToLogin()
                    }
                    true
                }
                else -> false
            }
        }




    }
    private fun setupObservers() {
        viewModel.userLiveData.observe(this) { userProfile ->
            if (userProfile != null) {
                // Actualiza las vistas con los datos del usuario
                nameView.text = userProfile.nombre
                lastNameView.text = userProfile.apaterno
                middleNameView.text = userProfile.amaterno
                birthDateView.text = userProfile.cumple
                usernameView.text = userProfile.username
            } else {
                Toast.makeText(this, "Error al cargar el perfil del usuario", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.errorMessage.observe(this) { error ->
            error?.let {
                Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
            }
        }

    }









    private fun loadUserProfile(username: String) {
        // Carga el perfil desde el ViewModel
        viewModel.getUserProfile(username)
    }

    private fun logoutUser() {
        sessionManager.clearSession() // Limpia los datos de la sesión
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish() // Finaliza la actividad actual
    }

    private fun showSearchDialog() {
        val searchEditText = EditText(this)
        searchEditText.hint = "Buscar libros..."

        AlertDialog.Builder(this)
            .setTitle("Buscar")
            .setView(searchEditText)
            .setPositiveButton("Buscar") { dialog, _ ->
                val query = searchEditText.text.toString()
                if (query.isNotEmpty()) {
                    searchBooks(query) // Llama a la función para buscar
                } else {
                    Toast.makeText(this, "Por favor, ingresa un término para buscar.", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun searchBooks(query: String) {
        val api = RetrofitClient.openLibraryApi
        lifecycleScope.launch {
            try {
                val response = api.searchBooks(query)
                if (response.docs.isNotEmpty()) {
                    showSearchResults(response.docs)
                } else {
                    Toast.makeText(this@ProfileActivity, "No se encontraron resultados.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ProfileActivity, "Error al buscar libros: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }



    private fun showSearchResults(books: List<Book>) {
        val recyclerView = RecyclerView(this).apply {
            layoutManager = LinearLayoutManager(this@ProfileActivity)
            adapter = BookAdapter(books, favoriteBooks.map { it.bookId }.toMutableList()) { book ->
                toggleFavorite(book)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Resultados de búsqueda")
            .setView(recyclerView)
            .setPositiveButton("Cerrar") { dialog, _ -> dialog.dismiss() }
            .show()
    }
    private fun toggleFavorite(book: Book) {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getUserId()
            Log.d("profile","ID DEL USUARIO EN FAVORITOS $userId")
            if (userId == -1) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "No se encontró el ID del usuario.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                if (favoriteBooks.any { it.bookId == book.key }) {
                    // Intenta eliminar del servidor
                    val response = apiService.deleteFavorite(book.key).execute()
                    if (response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProfileActivity, "Eliminado de favoritos", Toast.LENGTH_SHORT).show()
                            favoriteBooks.removeAll { it.bookId == book.key }
                        }
                    } else {
                        throw Exception("Error al eliminar favorito del servidor: ${response.errorBody()?.string()}")
                    }
                } else {
                    // Sincronizar con el servidor
                    val favoriteRemote = FavoriteRemote(
                        userId = userId,
                        bookId = book.key,
                        title = book.title ?: "Sin título",
                        author = book.author_name?.joinToString(", "),
                        publishYear = book.first_publish_year
                    )

                    val response = apiService.addFavorite(favoriteRemote).execute()
                    if (response.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ProfileActivity, "Agregado a favoritos", Toast.LENGTH_SHORT).show()
                            favoriteBooks.add(FavoriteLocal(
                                userId = userId,
                                bookId = book.key,
                                title = book.title ?: "Sin título",
                                author = book.author_name?.joinToString(", "),
                                publishYear = book.first_publish_year,
                                isSynced = true
                            ))
                        }
                    } else {
                        throw Exception("Error al sincronizar: ${response.errorBody()?.string()}")
                    }
                }
            } catch (e: Exception) {
                // Mostrar error en la UI
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteFavorite(bookId: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Intenta eliminar del servidor
                val response = apiService.deleteFavorite(bookId).execute()
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProfileActivity, "Favorito eliminado correctamente", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    throw Exception("Error al eliminar favorito del servidor: ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProfileActivity, "Error al eliminar favorito: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }





}
