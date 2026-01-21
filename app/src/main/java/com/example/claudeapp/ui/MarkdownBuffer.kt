package com.example.claudeapp.ui

/**
 * Extracts the "safe to render" portion of streaming markdown.
 * Hides unclosed formatting tokens to prevent visual flicker.
 */
object MarkdownBuffer {

    fun getSafeText(text: String): String {
        var result = text

        // Order matters! Code fences FIRST
        result = hideUnclosedCodeFence(result)

        // Then other markers
        result = hideUnclosed(result, "**")
        result = hideUnclosedItalic(result)
        result = hideUnclosedInlineCode(result)

        return result
    }

    private fun hideUnclosedCodeFence(text: String): String {
        val pattern = "```"
        var count = 0
        var index = 0
        var lastFenceStart = -1

        while (index <= text.length - 3) {
            if (text.substring(index, index + 3) == pattern) {
                count++
                lastFenceStart = index
                index += 3
            } else {
                index++
            }
        }

        if (count % 2 == 1 && lastFenceStart != -1) {
            return text.substring(0, lastFenceStart)
        }
        return text
    }

    private fun hideUnclosed(text: String, marker: String): String {
        var count = 0
        var index = 0
        var lastIndex = -1

        while (index <= text.length - marker.length) {
            if (text.substring(index, index + marker.length) == marker) {
                count++
                lastIndex = index
                index += marker.length
            } else {
                index++
            }
        }

        if (count % 2 == 1 && lastIndex != -1) {
            return text.substring(0, lastIndex)
        }
        return text
    }

    private fun hideUnclosedItalic(text: String): String {
        var count = 0
        var lastSingleIndex = -1
        var i = 0

        while (i < text.length) {
            if (text[i] == '*') {
                val prevIsStar = i > 0 && text[i - 1] == '*'
                val nextIsStar = i < text.length - 1 && text[i + 1] == '*'

                if (!prevIsStar && !nextIsStar) {
                    count++
                    lastSingleIndex = i
                } else if (!prevIsStar && nextIsStar) {
                    i++ // Skip next star (part of **)
                }
            }
            i++
        }

        if (count % 2 == 1 && lastSingleIndex != -1) {
            return text.substring(0, lastSingleIndex)
        }
        return text
    }

    private fun hideUnclosedInlineCode(text: String): String {
        var count = 0
        var lastSingleIndex = -1
        var i = 0

        while (i < text.length) {
            if (text[i] == '`') {
                // Check if this is part of ```
                val isTripleStart = i <= text.length - 3 &&
                        text.substring(i, i + 3) == "```"
                val isTripleMid = i > 0 && i < text.length - 1 &&
                        text[i - 1] == '`' && text[i + 1] == '`'
                val isTripleEnd = i >= 2 &&
                        text.substring(i - 2, i + 1) == "```"

                if (!isTripleStart && !isTripleMid && !isTripleEnd) {
                    count++
                    lastSingleIndex = i
                } else if (isTripleStart) {
                    i += 2 // Skip next two backticks
                }
            }
            i++
        }

        if (count % 2 == 1 && lastSingleIndex != -1) {
            return text.substring(0, lastSingleIndex)
        }
        return text
    }
}
