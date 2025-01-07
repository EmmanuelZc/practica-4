package com.example.practica6

import android.content.Context
import android.content.SharedPreferences

class UserSessionManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "user_session"
        private const val KEY_USERNAME = "username"
        private const val KEY_AUTH_METHOD = "auth_method" // google, facebook, or normal
        private const val KEY_USER_ID = "user_id" // Añadido para almacenar el user_id

    }

    fun saveSession(username: String, userId: Int, authMethod: String = "normal") {
        with(sharedPreferences.edit()) {
            putString(KEY_USERNAME, username)
            putInt(KEY_USER_ID, userId) // Usar putInt para garantizar que sea un entero
            putString(KEY_AUTH_METHOD, authMethod)
            apply()
        }
    }

    fun getUsername(): String? {
        return sharedPreferences.getString(KEY_USERNAME, null)
    }

    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1) // Devuelve -1 si no existe
    }

    fun clearSession() {
        with(sharedPreferences.edit()) {
            clear()
            apply()
        }
    }

    fun saveUserId(userId: Int) {
        sharedPreferences.edit().putInt(KEY_USER_ID, userId).apply()
    }

    fun isLoggedIn(): Boolean {
        return getUserId() != -1
    }

}
