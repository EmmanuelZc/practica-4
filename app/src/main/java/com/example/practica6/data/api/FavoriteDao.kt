package com.example.practica6.data.api

import androidx.room.*
import com.example.practica6.data.models.FavoriteLocal

@Dao
interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteLocal)

    @Query("SELECT * FROM favorites")
    suspend fun getAllFavorites(): List<FavoriteLocal>

    @Query("DELETE FROM favorites WHERE bookId = :bookId")
    suspend fun deleteFavorite(bookId: String)

    @Query("SELECT * FROM favorites WHERE isSynced = 0")
    suspend fun getPendingFavorites(): List<FavoriteLocal>

    @Update
    suspend fun updateFavorite(favorite: FavoriteLocal)

    @Query("DELETE FROM favorites")
    suspend fun clearFavorites()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteLocal>)
}
