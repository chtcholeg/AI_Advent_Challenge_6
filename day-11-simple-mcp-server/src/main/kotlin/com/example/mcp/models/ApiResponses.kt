package com.example.mcp.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Standard error response
 */
@Serializable
data class ErrorResponse(
    val error: String
)

/**
 * Health check response
 */
@Serializable
data class HealthResponse(
    val status: String,
    val uptime: Long,
    val uptime_human: String,
    val tools_count: Int,
    val active_sessions: Int
)

/**
 * Tool info for listing
 */
@Serializable
data class ToolInfo(
    val name: String,
    val description: String
)

/**
 * Tools list response
 */
@Serializable
data class ToolsResponse(
    val tools: List<ToolInfo>
)

/**
 * Server info response
 */
@Serializable
data class ServerInfoResponse(
    val name: String,
    val version: String,
    val protocol: String,
    val endpoints: Map<String, String>
)
