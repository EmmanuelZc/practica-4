package com.example.practica6.ui.theme.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.practica6.UserSessionManager
import com.example.practica6.data.api.ApiService
import com.example.practica6.data.api.RetrofitClient
import com.example.practica6.data.models.Rol
import com.example.practica6.data.models.RolLocal
import com.example.practica6.data.models.User
import com.example.practica6.data.models.UserDao
import com.example.practica6.data.models.UserLocal
import com.example.practica6.data.models.UserRoleCrossRef
import com.example.practica6.data.models.toLocal
import com.example.practica6.data.models.toRemote
import com.example.practica6.sync.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mindrot.jbcrypt.BCrypt

class AuthViewModel(
    private val userRepository: UserRepository,
    private val sessionManager: UserSessionManager


) : ViewModel() {

    val localUsersLiveData = MutableLiveData<List<UserLocal>?>()
    val userLiveData = MutableLiveData<User?>()
    val usersLiveData = MutableLiveData<List<User>?>()
    val errorMessage = MutableLiveData<String?>()
    val registerStatus = MutableLiveData<Int?>()
    val updateStatus = MutableLiveData<Int?>()
    val deleteStatus = MutableLiveData<Int?>()
    val localInsertStatus = MutableLiveData<Boolean?>()
    val syncStatus = MutableLiveData<Boolean>()



    fun getAllLocalUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val users = userRepository.userDao.getAllUsers()
                localUsersLiveData.postValue(users)
            } catch (e: Exception) {
                errorMessage.postValue("Error al obtener usuarios locales: ${e.message}")
            }
        }
    }

    fun insertLocalUser(user: UserLocal) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.userDao.insertUser(user)
                getAllLocalUsers()
            } catch (e: Exception) {
                errorMessage.postValue("Error al insertar usuario local: ${e.message}")
            }
        }
    }

    fun synchronizeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.synchronizeData()
                syncStatus.postValue(true)
                getAllLocalUsers()
            } catch (e: Exception) {
                errorMessage.postValue("Error durante la sincronización: ${e.localizedMessage}")
                syncStatus.postValue(false)
            }
        }
    }

    fun loadUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val users = userRepository.getAllUsers()
                localUsersLiveData.postValue(users) // Actualiza LiveData con los datos obtenidos
            } catch (e: Exception) {
                errorMessage.postValue("Error al cargar usuarios: ${e.message}")
            }
        }
    }

    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false
        return activeNetwork.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    fun registerUser(user: User) {

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userLocal = user.toLocal()
                userRepository.userDao.insertUser(userLocal)

                if (userRepository.isOnline()) {
                    val response = RetrofitClient.instance.registerUser(user).execute()
                    if (response.isSuccessful) {
                        errorMessage.postValue("Usuario registrado y sincronizado.")
                    } else {
                        errorMessage.postValue("Error al sincronizar usuario: ${response.message()}")
                    }
                } else {
                    errorMessage.postValue("Usuario guardado localmente. Sin conexión al servidor.")
                }

                getAllLocalUsers()
            } catch (e: Exception) {
                errorMessage.postValue("Error al registrar usuario: ${e.localizedMessage}")
            }
        }
    }


    fun updateUser(user: User) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.instance.updateUser(user).execute()
                if (response.isSuccessful) {
                    updateStatus.postValue(200) // Código HTTP 200 indica éxito
                } else {
                    updateStatus.postValue(response.code()) // Publica el código de error en caso de fallo
                }
            } catch (e: Exception) {
                errorMessage.postValue("Error al actualizar usuario: ${e.localizedMessage}")
            }
        }
    }


    fun getUserProfile(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val userWithRoles = userRepository.userDao.getUserWithRoles(username)
                userWithRoles?.let {
                    userLiveData.postValue(it.user.toRemote())
                } ?: run {
                    errorMessage.postValue("Usuario no encontrado.")
                }
            } catch (e: Exception) {
                errorMessage.postValue("Error al obtener datos del usuario: ${e.localizedMessage}")
            }
        }
    }


    fun deleteUser(username: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val localUser = userRepository.userDao.getUserByUsername(username)
                localUser?.let {
                    userRepository.userDao.deleteUser(it)
                    getAllLocalUsers()
                    errorMessage.postValue("Usuario eliminado localmente.")
                } ?: run {
                    errorMessage.postValue("Usuario no encontrado.")
                }
            } catch (e: Exception) {
                errorMessage.postValue("Error al eliminar usuario: ${e.localizedMessage}")
            }
        }
    }

    fun login(username: String, password: String, callback: (Boolean, User?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (userRepository.isOnline()) {
                    // Autenticación en línea
                    val response = RetrofitClient.instance.loginUser(username, password).execute()
                    if (response.isSuccessful) {
                        val user = response.body()
                        if (user != null) {
                            // Guarda el usuario y el hash de la contraseña localmente
                            val userLocal = user.copy(password = BCrypt.hashpw(password, BCrypt.gensalt())).toLocal()
                            userRepository.userDao.insertUser(userLocal)

                            sessionManager.saveUserId(user.id)
                            callback(true, user)
                        } else {
                            callback(false, null)
                        }
                    } else {
                        callback(false, null)
                    }
                } else {
                    // Autenticación fuera de línea
                    val localUser = userRepository.userDao.getUserByUsername(username)
                    if (localUser != null && BCrypt.checkpw(password, localUser.password)) {
                        callback(true, localUser.toRemote())
                    } else {
                        callback(false, null)
                    }
                }
            } catch (e: Exception) {
                callback(false, null)
            }
        }
    }




    fun insertUserWithRoles(userLocal: UserLocal, roles: List<RolLocal>, crossRefs: List<UserRoleCrossRef>) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Inserta el usuario local
                userRepository.userDao.insertUser(userLocal)

                // Inserta los roles locales
                userRepository.userDao.insertRoles(roles)

                // Inserta las relaciones entre usuario y roles
                userRepository.userDao.insertUserRoleCrossRef(crossRefs)

                // Refresca la lista de usuarios locales
                getAllLocalUsers()
            } catch (e: Exception) {
                errorMessage.postValue("Error al insertar usuario con roles: ${e.localizedMessage}")
            }
        }
    }

    fun syncPendingUsers() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.syncPendingUsers()
                syncStatus.postValue(true)
            } catch (e: Exception) {
                syncStatus.postValue(false)
                errorMessage.postValue("Error al sincronizar usuarios locales: ${e.localizedMessage}")
            }
        }
    }

    fun syncUsersFromServer() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.syncUsersFromServer()
                syncStatus.postValue(true)
            } catch (e: Exception) {
                syncStatus.postValue(false)
                errorMessage.postValue("Error al sincronizar usuarios desde el servidor: ${e.localizedMessage}")
            }
        }
    }

    fun syncFavorites() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                userRepository.syncFavoritesFromServer()
                Log.d("Sync", "Favoritos sincronizados correctamente.")
            } catch (e: Exception) {
                Log.e("Sync", "Error al sincronizar favoritos: ${e.localizedMessage}")
            }
        }
    }





}
