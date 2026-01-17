package com.example.claudeapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.example.claudeapp.network.ClaudeApiService
import com.example.claudeapp.ui.ClaudeScreen
import com.example.claudeapp.ui.ClaudeViewModel
import com.example.claudeapp.ui.theme.ClaudeAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Claude API Service
        val apiKey = BuildConfig.CLAUDE_API_KEY
        val apiService = ClaudeApiService(apiKey)
        val viewModel = ClaudeViewModel(apiService)

        // Test: Check which models are available
        // Test: Check which models are available
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("https://api.anthropic.com/v1/models")
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .build()

                client.newCall(request).execute().use { response ->
                    android.util.Log.d("ClaudeApp", "Response code: ${response.code}")
                    android.util.Log.d("ClaudeApp", "Models response: ${response.body?.string()}")
                }
            } catch (e: Exception) {
                android.util.Log.e("ClaudeApp", "Models error: ${e.javaClass.simpleName}")
                android.util.Log.e("ClaudeApp", "Models error details", e)
            }
        }

        setContent {
            ClaudeAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ClaudeScreen(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}