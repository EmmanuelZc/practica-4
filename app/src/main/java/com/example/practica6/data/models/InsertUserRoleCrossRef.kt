package com.example.practica6.data.models

import androidx.room.Entity

@Entity(
    tableName = "usuarios_roles",
    primaryKeys = ["usuario_id", "rol_id"]
)
data class UserRoleCrossRef(
    val usuario_id: Int,
    val rol_id: Int
)

