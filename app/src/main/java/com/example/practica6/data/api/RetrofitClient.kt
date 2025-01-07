    package com.example.practica6.data.api

    import okhttp3.OkHttpClient
    import okhttp3.logging.HttpLoggingInterceptor
    import retrofit2.Retrofit
    import retrofit2.converter.gson.GsonConverterFactory

    object RetrofitClient {
        private val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        private val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        private fun createRetrofit(baseUrl: String): Retrofit {
            return Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build()
        }

        val instance: ApiService by lazy {
            createRetrofit("http://192.168.24.225:8080/").create(ApiService::class.java)
        }

        val openLibraryApi: OpenLibraryApi by lazy {
            createRetrofit("https://openlibrary.org/").create(OpenLibraryApi::class.java)
        }
    }
