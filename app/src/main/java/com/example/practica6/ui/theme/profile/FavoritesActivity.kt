package com.example.practica6.ui.theme.profile

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Database
import com.example.practica6.AppDatabase
import com.example.practica6.R
import com.example.practica6.UserSessionManager
import com.example.practica6.data.api.ApiService
import com.example.practica6.data.api.FavoriteDao
import com.example.practica6.data.api.RetrofitClient
import com.example.practica6.data.models.FavoriteRemote
import com.example.practica6.data.models.UserDao
import com.example.practica6.data.models.toLocal
import com.example.practica6.data.models.toRemote
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FavoritesActivity : AppCompatActivity() {

    private lateinit var favoritesRecyclerView: RecyclerView
    private lateinit var apiService: ApiService
    private lateinit var sessionManager: UserSessionManager
    private val favoriteList = mutableListOf<FavoriteRemote>()
    private lateinit var userDao: UserDao
    private lateinit var favoriteDao: FavoriteDao


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)

        // Inicializa Retrofit y SessionManager
        apiService = RetrofitClient.instance
        sessionManager = UserSessionManager(applicationContext)
        favoriteDao = AppDatabase.getInstance(applicationContext).favoriteDao()


        // Inicializa RecyclerView
        favoritesRecyclerView = findViewById(R.id.favoritesRecyclerView)
        favoritesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Cargar favoritos
        loadFavorites()
    }

    private fun loadFavorites() {
        lifecycleScope.launch(Dispatchers.IO) {
            val userId = sessionManager.getUserId()
            if (userId == -1) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@FavoritesActivity, "No se encontró el ID del usuario.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            try {
                val response = apiService.getFavorites(userId).execute()
                if (response.isSuccessful) {
                    val favorites = response.body() ?: emptyList()

                    // Guarda los favoritos en la base de datos local
                    val localFavorites = favorites.map { it.toLocal() }
                    favoriteDao.insertFavorites(localFavorites)

                    // Carga favoritos desde la base de datos local
                    loadLocalFavorites()
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@FavoritesActivity,
                            "Error al obtener favoritos: ${response.code()} - ${response.message()}",
                            Toast.LENGTH_SHORT
                        ).show()

                        // Carga favoritos desde la base de datos local si falla la API
                        loadLocalFavorites()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@FavoritesActivity,
                        "Error al cargar favoritos: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Carga favoritos desde la base de datos local si ocurre un error
                    loadLocalFavorites()
                }
            }
        }
    }

    private suspend fun loadLocalFavorites() {
        val localFavorites = favoriteDao.getAllFavorites()
        favoriteList.clear()
        favoriteList.addAll(localFavorites.map { it.toRemote() })

        withContext(Dispatchers.Main) {
            if (favoriteList.isEmpty()) {
                Toast.makeText(this@FavoritesActivity, "No tienes favoritos guardados localmente.", Toast.LENGTH_SHORT).show()
            } else {
                // Actualiza el adaptador del RecyclerView
                favoritesRecyclerView.adapter = FavoritesAdapter(favoriteList)
            }
        }
    }

}