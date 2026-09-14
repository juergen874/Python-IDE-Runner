package com.example.ui.editor

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.*
import java.util.regex.Pattern

object PythonSyntaxHighlighter {

    private val KEYWORDS = setOf(
        "def", "class", "import", "from", "return", "if", "elif", "else", "while",
        "for", "in", "try", "except", "finally", "with", "as", "lambda", "pass",
        "yield", "async", "await", "global", "nonlocal", "raise", "assert",
        "break", "continue", "is", "not", "and", "or", "del"
    )

    private val LITERALS = setOf("True", "False", "None", "self", "cls")

    private val BUILTINS = setOf(
        "print", "range", "len", "int", "str", "float", "list", "dict", "set",
        "tuple", "open", "type", "sum", "min", "max", "enumerate", "zip",
        "map", "filter", "sorted", "any", "all", "abs", "round", "isinstance",
        "input", "super", "dir", "help", "repr", "id", "hash"
    )

    private val TOKEN_PATTERN = Pattern.compile(
        "(#[^\\n]*)" + // 1: comments
        "|(\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?''')" + // 2: triple quotes
        "|(\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*')" + // 3: single line strings
        "|(@\\w+)" + // 4: decorators
        "|(\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b)" + // 5: numbers
        "|(\\b[a-zA-Z_]\\w*\\b)" + // 6: identifiers
        "|([+\\-*/%=<>!&|^~]+)", // 7: operators
        Pattern.DOTALL
    )

    fun highlight(code: String): AnnotatedString {
        if (code.isEmpty()) return AnnotatedString("")

        val builder = AnnotatedString.Builder(code)
        val matcher = TOKEN_PATTERN.matcher(code)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            when {
                // Comment
                matcher.group(1) != null -> {
                    builder.addStyle(
                        SpanStyle(
                            color = SyntaxComment,
                            fontStyle = FontStyle.Italic
                        ),
                        start,
                        end
                    )
                }
                // Triple-quoted string or single-line string
                matcher.group(2) != null || matcher.group(3) != null -> {
                    builder.addStyle(
                        SpanStyle(color = SyntaxString),
                        start,
                        end
                    )
                }
                // Decorator
                matcher.group(4) != null -> {
                    builder.addStyle(
                        SpanStyle(
                            color = SyntaxSpecial,
                            fontWeight = FontWeight.SemiBold
                        ),
                        start,
                        end
                    )
                }
                // Number
                matcher.group(5) != null -> {
                    builder.addStyle(
                        SpanStyle(color = SyntaxNumber),
                        start,
                        end
                    )
                }
                // Word (Keyword, Builtin, Literal, or Identifier)
                matcher.group(6) != null -> {
                    val word = matcher.group(6)
                    when {
                        KEYWORDS.contains(word) -> {
                            builder.addStyle(
                                SpanStyle(
                                    color = SyntaxKeyword,
                                    fontWeight = FontWeight.Bold
                                ),
                                start,
                                end
                            )
                        }
                        LITERALS.contains(word) -> {
                            builder.addStyle(
                                SpanStyle(
                                    color = SyntaxSpecial,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                start,
                                end
                            )
                        }
                        BUILTINS.contains(word) -> {
                            builder.addStyle(
                                SpanStyle(color = SyntaxFunction),
                                start,
                                end
                            )
                        }
                    }
                }
                // Operator
                matcher.group(7) != null -> {
                    builder.addStyle(
                        SpanStyle(color = SyntaxOperator),
                        start,
                        end
                    )
                }
            }
        }

        return builder.toAnnotatedString()
    }
}
