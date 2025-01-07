package com.example.practica6.data.models


data class User(
    val amaterno: String = "",
    val apaterno: String = "",
    val cumple: String = "",
    val enabled: Boolean = true,
    val id: Int,
    val nombre: String = "",
    val password: String = "",
    val roles: List<Rol> = emptyList(),
    val username: String = ""

)

data class Rol(
    val id: Int = 2,
    val nombre: String
)


