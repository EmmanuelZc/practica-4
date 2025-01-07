package com.example.practica6.data.api

import retrofit2.http.GET
import retrofit2.http.Query
interface OpenLibraryApi {
    @GET("search.json")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("fields") fields: String = "key,title,author_name,first_publish_year"
    ): BookResponse
}

data class BookResponse(
    val docs: List<Book>                      // Lista de resultados de libros
)
