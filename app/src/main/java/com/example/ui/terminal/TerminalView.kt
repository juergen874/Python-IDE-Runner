package com.example.ui.terminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun TerminalView(
    stdoutText: String,
    stderrText: String,
    isRunning: Boolean,
    elapsedMs: Long?,
    hasError: Boolean?,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    var autoScroll by remember { mutableStateOf(true) }

    LaunchedEffect(stdoutText, stderrText) {
        if (autoScroll) {
            verticalScroll.animateScrollTo(verticalScroll.maxValue)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TerminalBackground)
    ) {
        // Terminal Header Bar
        Surface(
            color = IdeSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = IdePrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Python Console",
                        color = IdeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (isRunning) {
                        CircularProgressIndicator(
                            color = IdePrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "Running...",
                            color = IdePrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else if (elapsedMs != null) {
                        Surface(
                            color = if (hasError == true) IdeError.copy(alpha = 0.2f) else IdeSuccess.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = if (hasError == true) "Exit with error (${elapsedMs}ms)" else "Done (${elapsedMs}ms)",
                                color = if (hasError == true) IdeError else IdeSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            val combined = buildString {
                                if (stdoutText.isNotEmpty()) append(stdoutText)
                                if (stderrText.isNotEmpty()) {
                                    if (isNotEmpty()) append("\n")
                                    append(stderrText)
                                }
                            }
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Terminal Output", combined))
                            Toast.makeText(context, "Terminal output copied", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(28.dp).testTag("copy_terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "Copy Output",
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier.size(28.dp).testTag("clear_terminal_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteSweep,
                            contentDescription = "Clear Terminal",
                            tint = IdeTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = IdeBorder, thickness = 1.dp)

        // Terminal Content Box
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(verticalScroll)
                .horizontalScroll(horizontalScroll)
                .padding(12.dp)
        ) {
            SelectionContainer {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (stdoutText.isEmpty() && stderrText.isEmpty() && !isRunning) {
                        Text(
                            text = ">>> Python interactive output will appear here.\n>>> Click 'Run' to execute the current script.",
                            color = IdeTextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    } else {
                        if (stdoutText.isNotEmpty()) {
                            val annotatedStdout = remember(stdoutText) {
                                AnsiParser.parse(stdoutText, defaultColor = IdeTextPrimary)
                            }
                            Text(
                                text = annotatedStdout,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }

                        if (stderrText.isNotEmpty()) {
                            val annotatedStderr = remember(stderrText) {
                                AnsiParser.parse(stderrText, defaultColor = TerminalStderr)
                            }
                            Text(
                                text = annotatedStderr,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                lineHeight = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
