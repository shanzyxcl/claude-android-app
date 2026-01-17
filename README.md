# Claude Android App

A modern Android application demonstrating integration with Claude's API using OkHttp, Kotlin Coroutines, and Jetpack Compose.

## Features

- 🚀 **Streaming & Non-Streaming Modes**: Toggle between real-time streaming responses and standard complete responses
- 💬 **Clean Chat UI**: Material 3 design with message bubbles and smooth animations
- 🔄 **Reactive Architecture**: Uses Kotlin Flow for reactive state management
- 🌐 **OkHttp Integration**: Robust networking with proper error handling and timeouts
- 📱 **Modern Android Stack**: Jetpack Compose, ViewModel, Coroutines

## Architecture

```
app/
├── network/
│   └── ClaudeApiService.kt      # OkHttp-based API client with streaming support
├── ui/
│   ├── ClaudeViewModel.kt       # State management and business logic
│   ├── ClaudeScreen.kt          # Compose UI components
│   └── theme/                   # Material 3 theming
└── MainActivity.kt              # App entry point
```

### Key Components

#### ClaudeApiService
- Implements streaming and non-streaming API calls using OkHttp
- Parses Server-Sent Events (SSE) for streaming responses
- Handles JSON serialization/deserialization with kotlinx.serialization
- Proper error handling and timeout configuration

#### ClaudeViewModel
- Manages chat state using StateFlow
- Provides methods for sending messages in both streaming and non-streaming modes
- Handles message accumulation for streaming responses
- Error handling and loading states

#### ClaudeScreen
- Material 3 Compose UI with chat bubbles
- Auto-scrolling message list
- Input field with send button
- Toggle for streaming/non-streaming modes
- Error banner for displaying API errors

## Setup Instructions

### 1. Prerequisites
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK with API level 24+

### 2. Get Your Claude API Key

1. Go to [Anthropic Console](https://console.anthropic.com/)
2. Sign in or create an account
3. Navigate to "API Keys" section
4. Click "Create Key"
5. Copy your API key (starts with `sk-ant-`)

### 3. Configure API Key

**Option 1: Using gradle.properties (Recommended for production)**

Add to your `local.properties` file (create if it doesn't exist):
```properties
CLAUDE_API_KEY=sk-ant-your-api-key-here
```

Then update `app/build.gradle.kts`:
```kotlin
android {
    defaultConfig {
        // Read API key from local.properties
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        buildConfigField("String", "CLAUDE_API_KEY", "\"${properties.getProperty("CLAUDE_API_KEY")}\"")
    }
    buildFeatures {
        buildConfig = true
    }
}
```

**Option 2: Direct in Code (For testing only)**

In `MainActivity.kt`, replace:
```kotlin
val apiKey = BuildConfig.CLAUDE_API_KEY
```

With:
```kotlin
val apiKey = "sk-ant-your-api-key-here"
```

⚠️ **Security Warning**: Never commit API keys to version control!

### 4. Build and Run

1. Open project in Android Studio
2. Sync Gradle files
3. Select a device/emulator
4. Click Run ▶️

## Usage

### Streaming Mode (Default)
- Messages appear character-by-character as Claude generates them
- Lower perceived latency
- Better user experience for long responses

### Non-Streaming Mode
- Complete response appears at once
- Useful for specific use cases where you need the full response

### Toggle Between Modes
Use the switch in the top app bar to toggle between streaming and non-streaming modes.

## API Details

### Models Used
- Default: `claude-sonnet-4-20250514` (Claude Sonnet 4)
- Configurable in `ClaudeApiService.kt`

### Request Format
```kotlin
{
    "model": "claude-sonnet-4-20250514",
    "max_tokens": 1024,
    "messages": [
        {
            "role": "user",
            "content": "Your message here"
        }
    ],
    "stream": true  // or false for non-streaming
}
```

### Streaming Response Processing
The app parses Server-Sent Events (SSE) format:
```
event: content_block_delta
data: {"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"Hello"}}

event: message_stop
data: {"type":"message_stop"}
```

## Dependencies

```kotlin
// Network
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

// Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.material3:material3")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
```

## Error Handling

The app handles various error scenarios:
- Network connectivity issues
- API rate limits (429)
- Invalid API keys (401)
- Malformed requests (400)
- Server errors (5xx)

Errors are displayed in a dismissible banner at the top of the screen.

## Performance Considerations

### Streaming Backpressure
- Uses Kotlin Flow for natural backpressure handling
- UI updates are batched through state management
- No risk of overwhelming the UI thread

### Memory Management
- Messages are stored in memory (consider Room DB for persistence)
- Large conversations may need pagination
- Consider implementing message limits

### Network Optimization
- Connection pooling via OkHttp
- Configurable timeouts (connect: 30s, read: 60s, write: 30s)
- HTTP/2 support for multiplexing

## Future Enhancements

- [ ] Message persistence with Room database
- [ ] Multi-turn conversation context
- [ ] Image support for vision models
- [ ] Custom system prompts
- [ ] Conversation history export
- [ ] Token usage tracking
- [ ] Retry mechanism for failed requests
- [ ] Offline mode with queue

## Testing

### Unit Tests
Test the API service and ViewModel logic:
```kotlin
class ClaudeViewModelTest {
    @Test
    fun `sendMessage should update messages list`() {
        // Test implementation
    }
}
```

### Integration Tests
Test the complete flow with a test API key:
```kotlin
@Test
fun `streaming message should accumulate chunks`() = runTest {
    // Test implementation
}
```

## Troubleshooting

### "API call failed: 401"
- Check your API key is correct
- Ensure API key is properly configured in gradle.properties or code

### "API call failed: 429"
- You've hit rate limits
- Wait and retry, or upgrade your API plan

### No response appearing
- Check LogCat for error messages
- Verify internet permission in AndroidManifest.xml
- Test API key with curl command

### Streaming not working
- Check network interceptor logs
- Verify SSE parsing logic
- Ensure proper Flow collection in UI

## Resources

- [Claude API Documentation](https://docs.anthropic.com/)
- [OkHttp Documentation](https://square.github.io/okhttp/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## License

This is a sample project for educational purposes. Claude API usage is subject to Anthropic's terms of service.

## Contributing

This is a demonstration project. Feel free to fork and customize for your needs!

---

Built with ❤️ using Claude, OkHttp, and Jetpack Compose
