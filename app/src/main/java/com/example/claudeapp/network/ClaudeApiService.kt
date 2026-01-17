package com.example.claudeapp.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.BufferedReader
import java.util.concurrent.TimeUnit

/**
 * Service for interacting with Claude API using OkHttp.
 * Supports both streaming and non-streaming responses.
 */
class ClaudeApiService(private val apiKey: String) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = false
    }
    
    private val client = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Send a streaming request to Claude API.
     * Returns a Flow that emits text chunks as they arrive.
     */
    fun sendStreamingMessage(
        message: String,
        model: String = "claude-sonnet-4-5-20250929"
    ): Flow<String> = flow {
        val requestBody = ClaudeRequest(
            model = model,
            maxTokens = 1024,
            messages = listOf(
                Message(role = "user", content = message)
            ),
            stream = true
        )

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()

        // ⭐ REMOVE withContext(Dispatchers.IO) wrapper
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ClaudeApiException("API call failed: ${response.code} ${response.message}")
            }

            response.body?.let { responseBody ->
                responseBody.charStream().buffered().use { reader ->
                    processStreamingResponse(reader) { chunk ->
                        emit(chunk)
                    }
                }
            }
        }
    }.flowOn(Dispatchers.IO)  // ⭐ ADD THIS LINE
    
    /**
     * Send a non-streaming request to Claude API.
     * Returns the complete response as a single string.
     */
    suspend fun sendMessage(
        message: String,
        model: String = "claude-sonnet-4-5-20250929"
    ): String = withContext(Dispatchers.IO) {
        val requestBody = ClaudeRequest(
            model = model,
            maxTokens = 1024,
            messages = listOf(
                Message(role = "user", content = message)
            ),
            stream = false
        )
        
        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("content-type", "application/json")
            .post(json.encodeToString(requestBody).toRequestBody("application/json".toMediaType()))
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ClaudeApiException("API call failed: ${response.code} ${response.message}")
            }
            
            val responseBody = response.body?.string() 
                ?: throw ClaudeApiException("Empty response body")
            
            val claudeResponse = json.decodeFromString<ClaudeResponse>(responseBody)
            claudeResponse.content.firstOrNull()?.text ?: ""
        }
    }
    
    /**
     * Process streaming response from Claude API.
     * Parses Server-Sent Events (SSE) format.
     */
    private suspend fun processStreamingResponse(
        reader: BufferedReader,
        onChunk: suspend (String) -> Unit
    ) {
        var line: String?
        var eventType: String? = null
        val dataBuilder = StringBuilder()

        android.util.Log.d("ClaudeApp", "Starting to process streaming response")  // ⭐ ADD

        while (reader.readLine().also { line = it } != null) {
            android.util.Log.d("ClaudeApp", "SSE line: $line")  // ⭐ ADD

            when {
                line!!.startsWith("event:") -> {
                    eventType = line!!.substring(6).trim()
                    android.util.Log.d("ClaudeApp", "Event type: $eventType")  // ⭐ ADD
                }
                line!!.startsWith("data:") -> {
                    dataBuilder.append(line!!.substring(5).trim())
                    android.util.Log.d("ClaudeApp", "Data: ${line!!.substring(5).trim()}")  // ⭐ ADD
                }
                line!!.isEmpty() -> {
                    // End of event
                    if (dataBuilder.isNotEmpty()) {
                        android.util.Log.d("ClaudeApp", "Processing event: $eventType with data: ${dataBuilder}")  // ⭐ ADD
                        processStreamEvent(eventType, dataBuilder.toString(), onChunk)
                        dataBuilder.clear()
                    }
                    eventType = null
                }
            }
        }
        android.util.Log.d("ClaudeApp", "Finished processing streaming response")  // ⭐ ADD
    }
    
    /**
     * Process individual stream events.
     */
    private suspend fun processStreamEvent(
        eventType: String?,
        data: String,
        onChunk: suspend (String) -> Unit
    ) {
        android.util.Log.d("ClaudeApp", "processStreamEvent called: type=$eventType")  // ⭐ ADD

        when (eventType) {
            "content_block_delta" -> {
                try {
                    val delta = json.decodeFromString<ContentBlockDelta>(data)
                    delta.delta.text?.let {
                        android.util.Log.d("ClaudeApp", "Emitting chunk: $it")  // ⭐ ADD
                        onChunk(it)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ClaudeApp", "Error parsing delta: ${e.message}", e)  // ⭐ CHANGE
                }
            }
            "message_stop" -> {
                android.util.Log.d("ClaudeApp", "Stream ended (message_stop)")  // ⭐ ADD
            }
            else -> {
                android.util.Log.d("ClaudeApp", "Ignoring event type: $eventType")  // ⭐ ADD
            }
        }
    }
}

// Data classes for API requests/responses

@Serializable
data class ClaudeRequest(
    val model: String,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int,
    val messages: List<Message>,
    val stream: Boolean = false
)

@Serializable
data class Message(
    val role: String,
    val content: String
)

@Serializable
data class ClaudeResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @kotlinx.serialization.SerialName("stop_reason")
    val stopReason: String? = null
)

@Serializable
data class ContentBlock(
    val type: String,
    val text: String? = null
)

@Serializable
data class ContentBlockDelta(
    val type: String,
    val index: Int,
    val delta: Delta
)

@Serializable
data class Delta(
    val type: String,
    val text: String? = null
)

class ClaudeApiException(message: String) : Exception(message)
