package com.jp.scantwo

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiEntryExit {
    @POST("registerEntryExit")
    suspend fun registerEntryExit(@Body requestBody: EntryExitRequest): Response<EntryExitResponse>
}