package com.phusy2001.nativedevicemonitordemo.nativecore.network

data class ApiResponse(
    val statusCode: Int,
    val body: String?,
    val isSuccessful: Boolean,
    val errorMessage: String? = null,
    val durationMs: Long = 0,
    val isTimeout: Boolean = false
)

interface ApiClient {
    suspend fun get(url: String, headers: Map<String, String>? = null): ApiResponse
    suspend fun post(url: String, body: String, headers: Map<String, String>? = null): ApiResponse
}
