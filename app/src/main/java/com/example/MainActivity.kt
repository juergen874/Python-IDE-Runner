package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.drawer.WorkspaceDrawer
import com.example.ui.editor.CodeEditorView
import com.example.ui.terminal.TerminalView
import com.example.ui.theme.*
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.visual.VisualWebView
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainPythonIdeScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPythonIdeScreen(viewModel: MainViewModel) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val files by viewModel.files.collectAsStateWithLifecycle()
    val activeFile by viewModel.activeFile.collectAsStateWithLifecycle()
    val editorValue by viewModel.editorValue.collectAsStateWithLifecycle()
    val isUnsaved by viewModel.isUnsaved.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val stdoutText by viewModel.stdout.collectAsStateWithLifecycle()
    val stderrText by viewModel.stderr.collectAsStateWithLifecycle()
    val elapsedMs by viewModel.elapsedMs.collectAsStateWithLifecycle()
    val hasError by viewModel.hasError.collectAsStateWithLifecycle()
    val plots by viewModel.plots.collectAsStateWithLifecycle()
    val htmlOutputs by viewModel.htmlOutputs.collectAsStateWithLifecycle()
    val selectedOutputTab by viewModel.selectedOutputTab.collectAsStateWithLifecycle()
    val fontSizeSp by viewModel.fontSizeSp.collectAsStateWithLifecycle()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            WorkspaceDrawer(
                files = files,
                activeFile = activeFile,
                onSelectFile = { file -> viewModel.openFile(file) },
                onCreateFile = { name -> viewModel.createNewFile(name) },
                onRenameFile = { file, newName -> viewModel.renameFile(file, newName) },
                onDeleteFile = { file -> viewModel.deleteFile(file) },
                onRestoreSamples = { viewModel.restoreSamples() },
                onCloseDrawer = { coroutineScope.launch { drawerState.close() } }
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                Surface(
                    color = IdeSurface,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .windowInsetsPadding(WindowInsets.statusBars)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Left: Drawer Button + Title
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { coroutineScope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_workspace_drawer_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "Open Workspace",
                                        tint = IdePrimary
                                    )
                                }

                                Column(modifier = Modifier.padding(end = 4.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = activeFile?.name ?: "No File Open",
                                            color = IdeTextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (isUnsaved) {
                                            Box(
                                                modifier = Modifier
                                                    .size(7.dp)
                                                    .background(IdeWarning, CircleShape)
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (isRunning) "Python 3.10 Engine • Executing..." else "Python 3.10 • Native Engine",
                                        color = if (isRunning) IdePrimary else IdeTextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Right: Save, Zoom, Stop, Run
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Save Button
                                IconButton(
                                    onClick = { viewModel.saveCurrentFile() },
                                    enabled = isUnsaved,
                                    modifier = Modifier.size(36.dp).testTag("save_file_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Save,
                                        contentDescription = "Save File",
                                        tint = if (isUnsaved) IdePrimary else IdeTextMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                // Font Zoom Buttons
                                IconButton(
                                    onClick = { viewModel.adjustFontSize(-1) },
                                    modifier = Modifier.size(32.dp).testTag("font_zoom_out_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.TextDecrease,
                                        contentDescription = "Decrease Font Size",
                                        tint = IdeTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.adjustFontSize(1) },
                                    modifier = Modifier.size(32.dp).testTag("font_zoom_in_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.TextIncrease,
                                        contentDescription = "Increase Font Size",
                                        tint = IdeTextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Stop / Kill Button
                                if (isRunning) {
                                    Button(
                                        onClick = { viewModel.stopExecution() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = IdeError,
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(34.dp).testTag("stop_python_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Stop,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Stop",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Run Button
                                val transition = rememberInfiniteTransition(label = "run_spin")
                                val rotation by transition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = LinearEasing)
                                    ),
                                    label = "spin"
                                )

                                Button(
                                    onClick = { viewModel.runCurrentScript() },
                                    enabled = !isRunning,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = IdeSecondary,
                                        contentColor = Color.Black
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    modifier = Modifier.height(34.dp).testTag("run_python_button")
                                ) {
                                    if (isRunning) {
                                        Icon(
                                            imageVector = Icons.Default.Autorenew,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .rotate(rotation)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isRunning) "Running" else "Run",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            },
            containerColor = IdeBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Navigation Tab Row: Editor | Terminal | Web / Visual View
                Surface(
                    color = IdeSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TabRow(
                        selectedTabIndex = selectedOutputTab.coerceIn(0, 2),
                        containerColor = Color.Transparent,
                        contentColor = IdePrimary,
                        divider = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Tab 0: Editor
                        Tab(
                            selected = selectedOutputTab == 0,
                            onClick = { viewModel.setOutputTab(0) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Editor",
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedOutputTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isUnsaved) {
                                        Surface(
                                            color = IdeWarning,
                                            shape = CircleShape,
                                            modifier = Modifier.size(6.dp)
                                        ) {}
                                    }
                                }
                            },
                            modifier = Modifier.testTag("editor_tab")
                        )

                        // Tab 1: Terminal
                        Tab(
                            selected = selectedOutputTab == 1,
                            onClick = { viewModel.setOutputTab(1) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Terminal",
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedOutputTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isRunning) {
                                        Surface(
                                            color = IdeSecondary,
                                            shape = CircleShape,
                                            modifier = Modifier.size(6.dp)
                                        ) {}
                                    } else if (hasError == true) {
                                        Surface(
                                            color = IdeError,
                                            shape = CircleShape,
                                            modifier = Modifier.size(6.dp)
                                        ) {}
                                    }
                                }
                            },
                            modifier = Modifier.testTag("terminal_output_tab")
                        )

                        // Tab 2: Web / Visual View
                        Tab(
                            selected = selectedOutputTab == 2,
                            onClick = { viewModel.setOutputTab(2) },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Preview,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Web / Visual View",
                                        fontSize = 13.sp,
                                        fontWeight = if (selectedOutputTab == 2) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (plots.isNotEmpty() || htmlOutputs.isNotEmpty()) {
                                        Surface(
                                            color = IdeSecondary,
                                            shape = CircleShape,
                                            modifier = Modifier.size(6.dp)
                                        ) {}
                                    }
                                }
                            },
                            modifier = Modifier.testTag("visual_webview_tab")
                        )
                    }
                }

                HorizontalDivider(color = IdeBorder, thickness = 1.dp)

                // Full-Screen Dedicated View
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when (selectedOutputTab) {
                        0 -> {
                            CodeEditorView(
                                textFieldValue = editorValue,
                                onValueChange = { viewModel.onEditorChange(it) },
                                onIndent = { viewModel.indentEditor() },
                                onInsertSnippet = { viewModel.insertSnippet(it) },
                                fontSizeSp = fontSizeSp,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        1 -> {
                            TerminalView(
                                stdoutText = stdoutText,
                                stderrText = stderrText,
                                isRunning = isRunning,
                                elapsedMs = elapsedMs,
                                hasError = hasError,
                                onClear = { viewModel.clearTerminal() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            VisualWebView(
                                plotsBase64 = plots,
                                htmlOutputs = htmlOutputs,
                                onClearVisuals = { viewModel.clearVisuals() },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}
