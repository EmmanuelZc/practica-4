package com.example.practica6.sync

import com.example.practica6.data.api.ApiService
import com.example.practica6.data.api.FavoriteDao
import com.example.practica6.data.models.FavoriteLocal
import com.example.practica6.data.models.FavoriteRemote

class FavsRepository(
    private val apiService: ApiService,
    private val favoriteDao: FavoriteDao
) {
    suspend fun addFavorite(favorite: FavoriteLocal) {
        favoriteDao.insertFavorite(favorite)
    }

    suspend fun updateFavorite(favorite: FavoriteLocal) {
        favoriteDao.updateFavorite(favorite) // Actualiza el estado en la base de datos local
    }

    suspend fun removeFavorite(bookId: String) {
        favoriteDao.deleteFavorite(bookId)
    }

    suspend fun getAllFavorites(): List<FavoriteLocal> {
        return favoriteDao.getAllFavorites()
    }

    suspend fun syncFavoritesWithServer(userId: Int) {
        val pendingFavorites = favoriteDao.getPendingFavorites()
        if (pendingFavorites.isNotEmpty()) {
            pendingFavorites.forEach { favorite ->
                try {
                    val favoriteRemote = FavoriteRemote(
                        userId = userId,
                        bookId = favorite.bookId,
                        title = favorite.title,
                        author = favorite.author,
                        publishYear = favorite.publishYear
                    )
                    val response = apiService.addFavorite(favoriteRemote).execute()
                    if (response.isSuccessful) {
                        favoriteDao.updateFavorite(favorite.copy(isSynced = true))
                    } else {
                        throw Exception("Error al sincronizar favorito: ${response.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Manejo adicional según necesidades (logs, reintentos, etc.)
                }
            }
        }
    }
}
