package com.judgemycal.app.data

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** The ADK API server surface the app uses (see AgentDtos for wire shapes). */
interface AgentApi {

    @POST("apps/{appName}/users/{userId}/sessions")
    suspend fun createSession(
        @Path("appName") appName: String,
        @Path("userId") userId: String,
        @Body request: CreateSessionRequest,
    ): WireSession

    @GET("apps/{appName}/users/{userId}/sessions/{sessionId}")
    suspend fun getSession(
        @Path("appName") appName: String,
        @Path("userId") userId: String,
        @Path("sessionId") sessionId: String,
    ): WireSession

    @POST("run")
    suspend fun run(@Body request: RunAgentRequest): List<WireEvent>
}
