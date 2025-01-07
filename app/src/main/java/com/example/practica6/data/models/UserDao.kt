package com.example.practica6.data.models
import androidx.room.*
@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserLocal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRoles(roles: List<RolLocal>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserRoleCrossRef(ref: List<UserRoleCrossRef>)

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserWithRoles(userId: Int): UserWithRoles?

    @Transaction
    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserWithRoles(username: String): UserWithRoles?

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserLocal>

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserLocal?

    @Query("SELECT * FROM users WHERE username = :username AND password = :password")
    suspend fun getUserByCredentials(username: String, password: String): UserLocal?


    @Query("DELETE FROM users")
    suspend fun clearUsers()

    @Query("DELETE FROM roles")
    suspend fun clearRoles()

    @Query("DELETE FROM usuarios_roles")
    suspend fun clearUserRoleCrossRefs()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserLocal>)

    @Delete
    suspend fun deleteUser(user: UserLocal)

    @Query("SELECT * FROM users WHERE isSynced = 0")
    suspend fun getPendingUsers(): List<UserLocal>

    @Update
    suspend fun updateUser(user: UserLocal)



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
