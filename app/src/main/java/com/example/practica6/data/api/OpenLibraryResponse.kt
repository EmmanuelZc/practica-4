package com.example.practica6.data.api

data class OpenLibraryResponse(
    val docs: List<Book>
)

data class Book(
    val key: String,
    val title: String,
    val author_name: List<String>?,
    val first_publish_year: Int?
)