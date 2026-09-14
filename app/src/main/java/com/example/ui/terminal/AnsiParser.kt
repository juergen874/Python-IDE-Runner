package com.example.ui.terminal

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

object AnsiParser {

    private val ANSI_PATTERN = Regex("\u001B\\[([0-9;]*)m")

    fun parse(text: String, defaultColor: Color = IdeTextPrimary): AnnotatedString {
        if (!text.contains("\u001B[")) {
            return AnnotatedString(text)
        }

        return buildAnnotatedString {
            var currentIndex = 0
            var currentColor = defaultColor
            var isBold = false

            ANSI_PATTERN.findAll(text).forEach { match ->
                val matchStart = match.range.first
                if (matchStart > currentIndex) {
                    val segment = text.substring(currentIndex, matchStart)
                    val style = SpanStyle(
                        color = currentColor,
                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                    )
                    pushStyle(style)
                    append(segment)
                    pop()
                }

                val codes = match.groupValues[1].split(";").filter { it.isNotEmpty() }
                if (codes.isEmpty() || codes.contains("0")) {
                    currentColor = defaultColor
                    isBold = false
                }

                for (code in codes) {
                    when (code) {
                        "0" -> {
                            currentColor = defaultColor
                            isBold = false
                        }
                        "1" -> isBold = true
                        "22" -> isBold = false
                        "30" -> currentColor = Color(0xFF6B7280) // Black/Dark Gray
                        "31" -> currentColor = IdeError          // Red
                        "32" -> currentColor = IdeSecondary      // Green
                        "33" -> currentColor = IdeWarning        // Yellow/Amber
                        "34" -> currentColor = IdePrimary        // Blue
                        "35" -> currentColor = IdeTertiary       // Magenta/Pink
                        "36" -> currentColor = Color(0xFF56D364) // Cyan / Light Green
                        "37" -> currentColor = Color(0xFFE2E8F0) // White/Light Gray
                        "39" -> currentColor = defaultColor
                        "90" -> currentColor = Color(0xFF94A3B8) // Bright Black
                        "91" -> currentColor = Color(0xFFF87171) // Bright Red
                        "92" -> currentColor = Color(0xFF4ADE80) // Bright Green
                        "93" -> currentColor = Color(0xFFFDE047) // Bright Yellow
                        "94" -> currentColor = Color(0xFF93C5FD) // Bright Blue
                        "95" -> currentColor = Color(0xFFF472B6) // Bright Magenta
                        "96" -> currentColor = Color(0xFF67E8F9) // Bright Cyan
                        "97" -> currentColor = Color(0xFFFFFFFF) // Bright White
                    }
                }

                currentIndex = match.range.last + 1
            }

            if (currentIndex < text.length) {
                val remaining = text.substring(currentIndex)
                val style = SpanStyle(
                    color = currentColor,
                    fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal
                )
                pushStyle(style)
                append(remaining)
                pop()
            }
        }
    }
}
