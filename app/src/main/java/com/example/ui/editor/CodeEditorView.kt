package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

class PythonVisualTransformation : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): TransformedText {
        return TransformedText(
            PythonSyntaxHighlighter.highlight(text.text),
            OffsetMapping.Identity
        )
    }
}

@Composable
fun CodeEditorView(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    onIndent: () -> Unit,
    onInsertSnippet: (String) -> Unit,
    modifier: Modifier = Modifier,
    fontSizeSp: Int = 14
) {
    val scrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val visualTransformation = remember { PythonVisualTransformation() }
    val focusRequester = remember { FocusRequester() }

    val lines = remember(textFieldValue.text) {
        val count = textFieldValue.text.count { it == '\n' } + 1
        (1..count).toList()
    }

    val quickSymbols = remember {
        listOf(
            "Tab" to "    ",
            ":" to ":",
            "=" to " = ",
            "(" to "(",
            ")" to ")",
            "[" to "[",
            "]" to "]",
            "{" to "{",
            "}" to "}",
            "\"" to "\"",
            "'" to "'",
            "def" to "def ",
            "import" to "import ",
            "print" to "print(",
            "plt." to "plt.",
            "np." to "np.",
            "return" to "return ",
            "if" to "if ",
            "else" to "else:\n    ",
            "for" to "for i in range():",
            "#" to "# "
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeSurface)
    ) {
        // Quick Symbol & Snippet Bar
        Surface(
            color = IdeSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickSymbols) { (label, snippet) ->
                    AssistChip(
                        onClick = {
                            if (label == "Tab") {
                                onIndent()
                            } else {
                                onInsertSnippet(snippet)
                            }
                        },
                        label = {
                            Text(
                                text = label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = IdeTextPrimary
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = IdeSurfaceElevated,
                            labelColor = IdeTextPrimary
                        ),
                        border = AssistChipDefaults.assistChipBorder(
                            enabled = true,
                            borderColor = IdeBorder
                        ),
                        modifier = Modifier.height(32.dp).testTag("quick_symbol_$label")
                    )
                }
            }
        }

        HorizontalDivider(color = IdeBorder, thickness = 1.dp)

        // Editor with Gutter Line Numbers
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Line Numbers Gutter
                Column(
                    modifier = Modifier
                        .background(TerminalGutter)
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .widthIn(min = 36.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    lines.forEach { lineNum ->
                        Text(
                            text = lineNum.toString(),
                            color = IdeTextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp + 6).sp
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(IdeBorder)
                )

                // Actual Code Input
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                        .padding(horizontal = 12.dp, vertical = 12.dp)
                ) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = onValueChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .testTag("python_code_editor"),
                        textStyle = TextStyle(
                            color = IdeTextPrimary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp + 6).sp
                        ),
                        cursorBrush = SolidColor(IdePrimary),
                        visualTransformation = visualTransformation,
                        decorationBox = { innerTextField ->
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "# Write or paste Python code here...\n# Click RUN above to execute.",
                                    color = IdeTextMuted,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = fontSizeSp.sp,
                                    lineHeight = (fontSizeSp + 6).sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }
        }
    }
}
