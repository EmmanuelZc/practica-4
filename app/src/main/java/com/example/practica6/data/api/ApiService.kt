package com.example.practica6.data.api

import com.example.practica6.data.models.FavoriteRemote
import com.example.practica6.data.models.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Call
import retrofit2.http.DELETE
import retrofit2.http.PUT

interface ApiService {
    // Obtener nuevo usuario
    @POST("api/auth/registro")
    fun registerUser(@Body user: User): Call<Void>

    // Autenticar usuario (modificado para usar Query)
    @POST("api/auth")
    fun loginUser(@Query("usuario") usuario: String, @Query("password") password: String):Call<User>

    // Obtener perfil del usuario
    @GET("api/auth/perfil/{username}")
    fun getUserProfile(@Path("username") username: String): Call<User>

    // Obtener todos los usuarios
    @POST("api/auth/admin")
    fun getAllUsers(): Call<List<User>>

    // Actualizar perfil del usuario
    @PUT("api/auth/update")
    fun updateUser(@Body user: User): Call<Void>

    // Actualizar perfil del usuario
    @DELETE("api/auth/delete/{username}")
    fun deleteUser(@Path("username") username: String): Call<Void>

    @POST("api/auth/favorites")
    fun addFavorite(@Body favorite: FavoriteRemote): Call<Void>

    @DELETE("api/auth/favorites/{bookId}")
    fun deleteFavorite(@Path("bookId") bookId: String): Call<Void>

    @GET("api/auth/favorites/{userId}")
    fun getFavorites(@Path("userId") userId: Int): Call<List<FavoriteRemote>>

    @GET("api/auth/users")
    suspend fun getAllUsersLocal(): List<User>

    @GET("api/auth/favorites")
    suspend fun getAllFavoritesRemote(): List<FavoriteRemote>


}