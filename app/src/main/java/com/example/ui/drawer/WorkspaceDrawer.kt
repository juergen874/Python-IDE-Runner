package com.example.ui.drawer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PythonFile
import com.example.ui.theme.*

@Composable
fun WorkspaceDrawer(
    files: List<PythonFile>,
    activeFile: PythonFile?,
    onSelectFile: (PythonFile) -> Unit,
    onCreateFile: (String) -> Unit,
    onRenameFile: (PythonFile, String) -> Unit,
    onDeleteFile: (PythonFile) -> Unit,
    onRestoreSamples: () -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNewFileDialog by remember { mutableStateOf(false) }
    var fileToRename by remember { mutableStateOf<PythonFile?>(null) }
    var fileToDelete by remember { mutableStateOf<PythonFile?>(null) }

    ModalDrawerSheet(
        drawerContainerColor = IdeSurface,
        drawerContentColor = IdeTextPrimary,
        modifier = modifier.widthIn(max = 320.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = IdePrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = IdePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "Workspace",
                            color = IdeTextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "${files.size} file(s)",
                            color = IdeTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier.size(32.dp).testTag("close_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Drawer",
                        tint = IdeTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(
                color = IdeBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Action Buttons: New File & Samples
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showNewFileDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IdePrimary,
                        contentColor = IdeBackground
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .testTag("create_new_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "New File",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                OutlinedButton(
                    onClick = onRestoreSamples,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = IdeTextPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(IdeBorder)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp).testTag("restore_samples_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = "Samples",
                        tint = IdeTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(files, key = { it.path }) { pythonFile ->
                    val isSelected = activeFile?.path == pythonFile.path

                    Surface(
                        color = if (isSelected) IdeSurfaceVariant else androidx.compose.ui.graphics.Color.Transparent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onSelectFile(pythonFile)
                                onCloseDrawer()
                            }
                            .testTag("workspace_file_${pythonFile.name}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                val fileIcon = when {
                                    pythonFile.name.endsWith(".py") -> Icons.Default.Code
                                    pythonFile.name.endsWith(".json") -> Icons.Default.DataArray
                                    pythonFile.name.endsWith(".html") -> Icons.Default.Language
                                    else -> Icons.Default.Description
                                }
                                val iconTint = when {
                                    pythonFile.name.endsWith(".py") -> IdePrimary
                                    pythonFile.name.endsWith(".json") -> IdeWarning
                                    pythonFile.name.endsWith(".html") -> IdeTertiary
                                    else -> IdeTextSecondary
                                }

                                Icon(
                                    imageVector = fileIcon,
                                    contentDescription = null,
                                    tint = iconTint,
                                    modifier = Modifier.size(18.dp)
                                )

                                Column {
                                    Text(
                                        text = pythonFile.name,
                                        color = if (isSelected) IdePrimary else IdeTextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = pythonFile.formattedSize,
                                        color = IdeTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            // Actions Menu (Rename / Delete)
                            var menuExpanded by remember { mutableStateOf(false) }
                            Box {
                                IconButton(
                                    onClick = { menuExpanded = true },
                                    modifier = Modifier.size(24.dp).testTag("file_options_${pythonFile.name}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "File Options",
                                        tint = IdeTextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false },
                                    modifier = Modifier.background(IdeSurfaceElevated)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Rename", color = IdeTextPrimary) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Edit,
                                                contentDescription = null,
                                                tint = IdeTextSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            fileToRename = pythonFile
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = IdeError) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.Delete,
                                                contentDescription = null,
                                                tint = IdeError,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            fileToDelete = pythonFile
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = IdeBorder,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Python IDE v1.1",
                    color = IdeTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Chaquopy 3.10",
                    color = IdeTextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }

    // Dialog: Create New File
    if (showNewFileDialog) {
        var newFileName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            containerColor = IdeSurfaceElevated,
            title = { Text("Create Python File", color = IdeTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("e.g. analysis.py", color = IdeTextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary,
                        focusedBorderColor = IdePrimary,
                        unfocusedBorderColor = IdeBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("new_file_input_field")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileName.isNotBlank()) {
                            onCreateFile(newFileName.trim())
                            showNewFileDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_file_button")
                ) {
                    Text("Create", color = IdePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancel", color = IdeTextSecondary)
                }
            }
        )
    }

    // Dialog: Rename File
    fileToRename?.let { target ->
        var renameName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { fileToRename = null },
            containerColor = IdeSurfaceElevated,
            title = { Text("Rename File", color = IdeTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameName,
                    onValueChange = { renameName = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = IdeTextPrimary,
                        unfocusedTextColor = IdeTextPrimary,
                        focusedBorderColor = IdePrimary,
                        unfocusedBorderColor = IdeBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("rename_file_input_field")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameName.isNotBlank()) {
                            onRenameFile(target, renameName.trim())
                            fileToRename = null
                        }
                    },
                    modifier = Modifier.testTag("confirm_rename_file_button")
                ) {
                    Text("Rename", color = IdePrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToRename = null }) {
                    Text("Cancel", color = IdeTextSecondary)
                }
            }
        )
    }

    // Dialog: Delete File
    fileToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            containerColor = IdeSurfaceElevated,
            title = { Text("Delete File?", color = IdeTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Are you sure you want to delete '${target.name}'? This action cannot be undone.",
                    color = IdeTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteFile(target)
                        fileToDelete = null
                    },
                    modifier = Modifier.testTag("confirm_delete_file_button")
                ) {
                    Text("Delete", color = IdeError, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = IdeTextSecondary)
                }
            }
        )
    }
}
