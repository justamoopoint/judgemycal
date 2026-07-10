package com.judgemycal.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Wire DTOs for the ADK API server (google-adk 2.3.0), verified against the
 * server's OpenAPI schema (camelCase field aliases). The backend test suite
 * exercises the same endpoints, so a drift between these DTOs and the deployed
 * ADK version shows up in CI, not in production.
 */

@Serializable
data class WireBlob(
    val mimeType: String? = null,
    /** base64-encoded bytes */
    val data: String? = null,
)

@Serializable
data class WireFunctionResponse(
    val id: String? = null,
    val name: String? = null,
    val response: JsonObject? = null,
)

@Serializable
data class WirePart(
    val text: String? = null,
    val inlineData: WireBlob? = null,
    val functionResponse: WireFunctionResponse? = null,
)

@Serializable
data class WireContent(
    val role: String? = null,
    val parts: List<WirePart> = emptyList(),
)

@Serializable
data class CreateSessionRequest(
    val state: JsonObject? = null,
)

@Serializable
data class WireSession(
    val id: String,
    val appName: String? = null,
    val userId: String? = null,
    val state: JsonObject? = null,
)

@Serializable
data class RunAgentRequest(
    val appName: String,
    val userId: String,
    val sessionId: String,
    val newMessage: WireContent,
    val streaming: Boolean = false,
    val stateDelta: JsonObject? = null,
)

@Serializable
data class WireEvent(
    val id: String? = null,
    val author: String? = null,
    val partial: Boolean? = null,
    val content: WireContent? = null,
)
