package com.example.practica6.sync
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.practica6.data.api.ApiService
import com.example.practica6.data.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

class UserRepository(
    private val apiService: ApiService,
    val userDao: UserDao,
    private val context: Context
) {


    suspend fun synchronizeData() = withContext(Dispatchers.IO) {
        if (isOnline()) {
            try {
                syncUsersFromServer() // Descarga datos del servidor y actualiza la base local
                syncPendingUsers()    // Sube usuarios pendientes al servidor
                Log.d("Sync", "Sincronización completada exitosamente.")
            } catch (e: Exception) {
                Log.e("Sync", "Error durante la sincronización: ${e.localizedMessage}")
            }
        } else {
            Log.d("Sync", "Sin conexión. Sincronización pospuesta.")
        }
    }

    suspend fun getUserByUsername(username: String): UserLocal? {
        return userDao.getUserByUsername(username)
    }

    suspend fun getAllUsers(): List<UserLocal> {
        return try {
            if (isOnline()) {
                val response = apiService.getAllUsers().execute()
                if (response.isSuccessful) {
                    val remoteUsers = response.body() ?: emptyList()
                    saveUsersFromServer(remoteUsers)
                    remoteUsers.map { it.toLocal() }
                } else {
                    Log.e("Sync", "Error al obtener usuarios del servidor: ${response.message()}")
                    userDao.getAllUsers() // Devuelve datos locales en caso de error
                }
            } else {
                userDao.getAllUsers() // Devuelve datos locales si no hay conexión
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error al obtener usuarios: ${e.localizedMessage}")
            userDao.getAllUsers()
        }
    }

    suspend fun login(username: String, password: String): UserLocal? {
        return try {
            if (isOnline()) {
                val response = apiService.loginUser(username, password).execute()
                if (response.isSuccessful) {
                    val remoteUser = response.body() ?: return null

                    val hashedPassword = remoteUser.password ?: BCrypt.hashpw(password, BCrypt.gensalt())
                    val userLocal = remoteUser.toLocal().copy(password = hashedPassword)
                    userDao.insertUser(userLocal)
                    userLocal
                } else {
                    Log.e("Sync", "Error de autenticación en el servidor: ${response.message()}")
                    null
                }
            } else {
                val localUser = userDao.getUserByUsername(username)
                if (localUser != null && BCrypt.checkpw(password, localUser.password)) {
                    localUser
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error durante el inicio de sesión: ${e.localizedMessage}")
            null
        }
    }




    suspend fun syncUsersFromServer() {
        try {
            val response = apiService.getAllUsers().execute()
            if (response.isSuccessful) {
                val usersFromServer = response.body() ?: emptyList()
                val localUsers = usersFromServer.map { user ->
                    val existingUser = userDao.getUserByUsername(user.username)
                    user.toLocal().copy(password = existingUser?.password ?: BCrypt.hashpw("defaultPassword", BCrypt.gensalt()))
                }
                userDao.clearUsers()
                userDao.insertUsers(localUsers)
                Log.d("Sync", "Usuarios sincronizados correctamente.")
            } else {
                Log.e("Sync", "Error al sincronizar usuarios: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            Log.e("Sync", "Error al sincronizar usuarios: ${e.localizedMessage}")
        }
    }





    suspend fun syncPendingUsers() {
        if (isOnline()) {
            val pendingUsers = userDao.getPendingUsers()
            for (localUser in pendingUsers) {
                try {
                    val userRemote = localUser.toRemote()
                    val response = apiService.registerUser(userRemote).execute()
                    if (response.isSuccessful) {
                        userDao.updateUser(localUser.copy(isSynced = true))
                    } else {
                        Log.e("Sync", "Error al sincronizar ${localUser.username}: ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e("Sync", "Error al sincronizar ${localUser.username}: ${e.localizedMessage}")
                }
            }
        } else {
            Log.d("Sync", "Sin conexión. Los usuarios pendientes no se pueden sincronizar.")
        }
    }

    private fun hashPassword(password: String?): String {
        return password?.takeIf { it.isNotEmpty() }?.let { BCrypt.hashpw(it, BCrypt.gensalt()) } ?: ""
    }


    suspend fun saveUserWithRoles(user: User) {
        val (userLocal, rolesLocal) = user.toLocalWithRoles()
        val hashedPassword = userLocal.password?.takeIf { it.isNotEmpty() }
            ?: BCrypt.hashpw("ContraseñaTemporal123", BCrypt.gensalt()) // Hash predeterminado


        val updatedUserLocal = userLocal.copy(password = hashedPassword)
        val userRoleCrossRefs = rolesLocal.map { role ->
            UserRoleCrossRef(usuario_id = updatedUserLocal.id, rol_id = role.id)
        }

        userDao.insertUser(updatedUserLocal)
        userDao.insertRoles(rolesLocal)
        userDao.insertUserRoleCrossRef(userRoleCrossRefs)
    }



    suspend fun saveUsersFromServer(usersFromServer: List<User>) {
        try {
            val localUsers = usersFromServer.map { it.toLocalWithRoles().first }
            val roles = usersFromServer.flatMap { it.roles }.distinctBy { it.id }.map { it.toLocal() }
            val userRoleCrossRefs = usersFromServer.flatMap { user ->
                user.roles.map { role -> UserRoleCrossRef(user.id, role.id) }
            }

            userDao.clearUserRoleCrossRefs()
            userDao.clearRoles()
            userDao.clearUsers()

            userDao.insertRoles(roles)
            userDao.insertUserRoleCrossRef(userRoleCrossRefs)
            userDao.insertUsers(localUsers)

            Log.d("Sync", "Usuarios sincronizados localmente.")
        } catch (e: Exception) {
            Log.e("Sync", "Error al guardar usuarios del servidor localmente: ${e.localizedMessage}")
        }
    }



    suspend fun syncFavoritesFromServer() {
        if (isOnline()) {
            try {
                // Llama al servicio remoto para obtener los favoritos
                val favoritesFromServer = apiService.getAllFavoritesRemote()

                // Convierte los favoritos remotos a favoritos locales
                val localFavorites = favoritesFromServer.map { it.toLocal() }

                // Limpia la base de datos local de favoritos antes de insertar los nuevos
                userDao.clearFavorites()

                // Inserta los favoritos locales
                userDao.insertFavorites(localFavorites)

                Log.d("Sync", "Favoritos sincronizados correctamente desde el servidor.")
            } catch (e: Exception) {
                Log.e("Sync", "Error al sincronizar favoritos desde el servidor: ${e.localizedMessage}")
            }
        } else {
            Log.d("Sync", "Sin conexión. Los favoritos no se pueden sincronizar.")
        }
    }


    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
