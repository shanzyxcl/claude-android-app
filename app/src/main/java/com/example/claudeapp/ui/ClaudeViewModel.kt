package com.example.claudeapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.claudeapp.network.ClaudeApiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel for managing Claude chat interactions.
 * Handles both streaming and non-streaming modes.
 */
class ClaudeViewModel(
    private val apiService: ClaudeApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ClaudeUiState())
    val uiState: StateFlow<ClaudeUiState> = _uiState.asStateFlow()
    
    /**
     * Send a message with streaming response.
     * Response chunks are accumulated and displayed in real-time.
     */
    fun sendStreamingMessage(message: String) {
        if (message.isBlank()) return
        
        viewModelScope.launch {
            // Add user message
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + ChatMessage(
                        text = message,
                        isUser = true
                    ),
                    isLoading = true,
                    error = null
                )
            }
            
            // Start accumulating assistant response
            val assistantMessageIndex = _uiState.value.messages.size
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + ChatMessage(
                        text = "",
                        isUser = false
                    )
                )
            }
            
            try {
                apiService.sendStreamingMessage(message)
                    .catch { error ->
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = false,
                                error = "Error: ${error.message}"
                            )
                        }
                    }
                    .collect { chunk ->
                        _uiState.update { currentState ->
                            val updatedMessages = currentState.messages.toMutableList()
                            val currentAssistantMessage = updatedMessages[assistantMessageIndex]
                            updatedMessages[assistantMessageIndex] = currentAssistantMessage.copy(
                                text = currentAssistantMessage.text + chunk
                            )
                            currentState.copy(
                                messages = updatedMessages,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Send a message with non-streaming response.
     * Response is displayed once complete.
     */
    fun sendMessage(message: String) {
        if (message.isBlank()) return
        
        viewModelScope.launch {
            // Add user message
            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + ChatMessage(
                        text = message,
                        isUser = true
                    ),
                    isLoading = true,
                    error = null
                )
            }
            
            try {
                val response = apiService.sendMessage(message)
                
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages + ChatMessage(
                            text = response,
                            isUser = false
                        ),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { currentState ->
                    currentState.copy(
                        isLoading = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Toggle between streaming and non-streaming modes.
     */
    fun toggleStreamingMode() {
        _uiState.update { currentState ->
            currentState.copy(isStreamingEnabled = !currentState.isStreamingEnabled)
        }
    }
    
    /**
     * Clear all messages and reset state.
     */
    fun clearMessages() {
        _uiState.update { ClaudeUiState() }
    }
    
    /**
     * Dismiss the current error.
     */
    fun dismissError() {
        _uiState.update { currentState ->
            currentState.copy(error = null)
        }
    }
}

/**
 * UI state for the Claude chat screen.
 */
data class ClaudeUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isStreamingEnabled: Boolean = true,
    val error: String? = null
)

/**
 * Represents a single chat message.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
