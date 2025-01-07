package com.example.practica6.ui.theme.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.practica6.UserSessionManager
import com.example.practica6.sync.UserRepository

class AuthViewModelFactory(
    private val userRepository: UserRepository,
    private val sessionManager: UserSessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(userRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Clase desconocida")
    }
}
