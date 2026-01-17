package com.example.claudeapp

import com.example.claudeapp.network.ClaudeApiService
import com.example.claudeapp.ui.ClaudeViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import io.mockk.mockk
import io.mockk.coEvery

/**
 * Example unit test for ClaudeViewModel.
 * 
 * To run tests, you'll need to add MockK dependency:
 * testImplementation("io.mockk:mockk:1.13.8")
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClaudeViewModelTest {

    private lateinit var mockApiService: ClaudeApiService
    private lateinit var viewModel: ClaudeViewModel

    @Before
    fun setup() {
        mockApiService = mockk<ClaudeApiService>()
        viewModel = ClaudeViewModel(mockApiService)
    }

    @Test
    fun `initial state should be empty`() = runTest {
        val state = viewModel.uiState.first()
        
        assertTrue(state.messages.isEmpty())
        assertFalse(state.isLoading)
        assertTrue(state.isStreamingEnabled)
        assertNull(state.error)
    }

    @Test
    fun `sendMessage should add user message to state`() = runTest {
        // Mock API response
        coEvery { mockApiService.sendMessage(any()) } returns "Hello from Claude"
        
        // Send message
        viewModel.sendMessage("Hello")
        
        // Wait for state update
        kotlinx.coroutines.delay(100)
        
        val state = viewModel.uiState.first()
        
        // Should have both user and assistant messages
        assertEquals(2, state.messages.size)
        assertEquals("Hello", state.messages[0].text)
        assertTrue(state.messages[0].isUser)
        assertEquals("Hello from Claude", state.messages[1].text)
        assertFalse(state.messages[1].isUser)
    }

    @Test
    fun `toggleStreamingMode should change streaming state`() = runTest {
        val initialState = viewModel.uiState.first()
        assertTrue(initialState.isStreamingEnabled)
        
        viewModel.toggleStreamingMode()
        
        val newState = viewModel.uiState.first()
        assertFalse(newState.isStreamingEnabled)
    }

    @Test
    fun `clearMessages should reset state`() = runTest {
        // Add some messages first
        coEvery { mockApiService.sendMessage(any()) } returns "Response"
        viewModel.sendMessage("Test")
        
        kotlinx.coroutines.delay(100)
        
        // Clear messages
        viewModel.clearMessages()
        
        val state = viewModel.uiState.first()
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun `error should be set when API call fails`() = runTest {
        // Mock API failure
        coEvery { mockApiService.sendMessage(any()) } throws Exception("API Error")
        
        viewModel.sendMessage("Test")
        
        kotlinx.coroutines.delay(100)
        
        val state = viewModel.uiState.first()
        assertNotNull(state.error)
        assertTrue(state.error?.contains("API Error") == true)
    }
}
