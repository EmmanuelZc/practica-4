package com.example.practica6.data.models

fun User.toLocal(): UserLocal {
    return UserLocal(
        id = this.id,
        nombre = this.nombre,
        apaterno = this.apaterno,
        amaterno = this.amaterno,
        cumple = this.cumple,
        username = this.username,
        password = this.password, // Maneja correctamente el hash si es necesario
        enabled = this.enabled,
        isSynced = true // Marcamos como sincronizado porque viene del servidor
    )
}

fun Rol.toLocal(): RolLocal {
    return RolLocal(
        id = this.id,
        nombre = this.nombre
    )
}

fun UserLocal.toRemote(): User {
    return User(
        id = this.id,
        nombre = this.nombre,
        apaterno = this.apaterno,
        amaterno = this.amaterno,
        cumple = this.cumple,
        username = this.username,
        password = this.password ?: "", // Asegúrate de manejar correctamente el hash
        enabled = this.enabled
    )
}

fun RolLocal.toRemote(): Rol {
    return Rol(
        id = this.id,
        nombre = this.nombre
    )
}

fun User.toLocalWithRoles(): Pair<UserLocal, List<RolLocal>> {
    val userLocal = this.toLocal()
    val rolesLocal = this.roles.map { it.toLocal() }
    return Pair(userLocal, rolesLocal)
}

fun UserWithRoles.toRemote(): User {
    return User(
        id = this.user.id,
        nombre = this.user.nombre,
        apaterno = this.user.apaterno,
        amaterno = this.user.amaterno,
        cumple = this.user.cumple,
        username = this.user.username,
        password = this.user.password ?: "",
        enabled = this.user.enabled,
        roles = this.roles.map { it.toRemote() }
    )
}

