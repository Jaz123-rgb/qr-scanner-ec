package com.jp.scantwo.data.api

import com.jp.scantwo.data.model.CheckInResponse
import com.jp.scantwo.data.model.LoginRequest
import com.jp.scantwo.data.model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiEntryExit {
    @POST("api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("api/user-events/{uuid}/check-in")
    suspend fun checkIn(
        @Header("Authorization") authToken: String,
        @Path("uuid") uuid: String
    ): Response<CheckInResponse>
}