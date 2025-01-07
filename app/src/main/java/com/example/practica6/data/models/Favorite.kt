package com.example.practica6.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class FavoriteLocal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val bookId: String, // Identificador del libro en Open Library
    val title: String,
    val author: String?,
    val publishYear: Int?,
    val isSynced: Boolean = false // Bandera para indicar si está sincronizado con el servidor
)

data class FavoriteRemote(
    val userId: Int, // Debe coincidir con lo que espera tu backend
    val bookId: String,
    val title: String?,
    val author: String?,
    val publishYear: Int?
)
fun FavoriteRemote.toLocal(): FavoriteLocal {
    return FavoriteLocal(
        userId = this.userId,
        bookId = this.bookId,
        title = this.title ?: "",
        author = this.author,
        publishYear = this.publishYear,
        isSynced = true
    )
}

fun FavoriteLocal.toRemote(): FavoriteRemote {
    return FavoriteRemote(
        userId = this.userId,
        bookId = this.bookId,
        title = this.title,
        author = this.author,
        publishYear = this.publishYear
    )
}




