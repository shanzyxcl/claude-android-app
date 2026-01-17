package com.example.claudeapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Main chat screen composable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaudeScreen(
    viewModel: ClaudeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(uiState.messages.size - 1)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Claude Chat")
                        Text(
                            text = if (uiState.isStreamingEnabled) "Streaming Mode" else "Standard Mode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Toggle streaming mode
                    Switch(
                        checked = uiState.isStreamingEnabled,
                        onCheckedChange = { viewModel.toggleStreamingMode() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    
                    // Clear messages button
                    IconButton(onClick = { viewModel.clearMessages() }) {
                        Text("Clear")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
        ) {
            // Error message
            AnimatedVisibility(visible = uiState.error != null) {
                ErrorBanner(
                    error = uiState.error ?: "",
                    onDismiss = { viewModel.dismissError() }
                )
            }
            
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.messages) { message ->
                    MessageBubble(message = message)
                }
                
                // Loading indicator
                if (uiState.isLoading) {
                    item {
                        LoadingIndicator()
                    }
                }
            }
            
            // Input field
            MessageInput(
                onSendMessage = { message ->
                    if (uiState.isStreamingEnabled) {
                        viewModel.sendStreamingMessage(message)
                    } else {
                        viewModel.sendMessage(message)
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Individual message bubble.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (message.isUser) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            },
            modifier = Modifier
                .widthIn(max = 300.dp)
                .padding(horizontal = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = if (message.isUser) "You" else "Claude",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    }
                )
            }
        }
    }
}

/**
 * Message input field with send button.
 */
@Composable
fun MessageInput(
    onSendMessage: (String) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var text by remember { mutableStateOf("") }
    
    Surface(
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                enabled = enabled,
                maxLines = 4
            )
            
            Button(
                onClick = {
                    if (text.isNotBlank()) {
                        onSendMessage(text)
                        text = ""
                    }
                },
                enabled = enabled && text.isNotBlank()
            ) {
                Text("Send")
            }
        }
    }
}

/**
 * Loading indicator shown while waiting for response.
 */
@Composable
fun LoadingIndicator(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(20.dp),
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Claude is thinking...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Error banner shown at the top when errors occur.
 */
@Composable
fun ErrorBanner(
    error: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}
