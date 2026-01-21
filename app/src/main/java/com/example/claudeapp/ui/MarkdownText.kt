package com.example.claudeapp.ui

import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import io.noties.markwon.Markwon

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    isStreaming: Boolean = false
) {
    val context = LocalContext.current
    val markwon = remember { Markwon.create(context) }

    val displayText = if (isStreaming) {
        MarkdownBuffer.getSafeText(text)
    } else {
        text
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextView(ctx).apply {
                textSize = 14f
            }
        },
        update = { textView ->
            markwon.setMarkdown(textView, displayText)
            if (color != Color.Unspecified) {
                textView.setTextColor(color.toArgb())
            }
        }
    )
}