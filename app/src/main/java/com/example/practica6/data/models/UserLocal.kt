package com.example.practica6.data.models

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverters

@Entity(tableName = "users")
data class UserLocal(
    @PrimaryKey val id: Int,
    val nombre: String,
    val apaterno: String,
    val amaterno: String,
    val cumple: String,
    val username: String,
    val password: String?,
    val enabled: Boolean,
    val isSynced: Boolean = false
)

@Entity(tableName = "roles")
data class RolLocal(
    @PrimaryKey  val id: Int = 2,
    val nombre: String
)

data class UserWithRoles(
    @Embedded val user: UserLocal,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = UserRoleCrossRef::class,
            parentColumn = "usuario_id",
            entityColumn = "rol_id"
        )
    )
    val roles: List<RolLocal>
)
