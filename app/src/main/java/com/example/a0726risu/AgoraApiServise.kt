package com.example.a0726risu

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class TokenResponse(
    val token: String
)

interface AgoraTokenApiService {
    @GET("/get-token")
    suspend fun getAgoraToken(
        @Query("channelName") channelName: String
    ): TokenResponse
}

object AgoraApi {
    private const val BASE_URL = "https://untrophied-semiprotective-peg.ngrok-free.dev"

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val service: AgoraTokenApiService by lazy {
        retrofit.create(AgoraTokenApiService::class.java)
    }
}
