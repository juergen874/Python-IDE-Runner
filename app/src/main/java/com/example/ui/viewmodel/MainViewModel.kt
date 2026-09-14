package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FileManager
import com.example.data.PythonFile
import com.example.python.OutputListener
import com.example.python.PythonEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val fileManager = FileManager(application)
    private val pythonEngine = PythonEngine.getInstance(application)

    private val _files = MutableStateFlow<List<PythonFile>>(emptyList())
    val files: StateFlow<List<PythonFile>> = _files.asStateFlow()

    private val _activeFile = MutableStateFlow<PythonFile?>(null)
    val activeFile: StateFlow<PythonFile?> = _activeFile.asStateFlow()

    private val _editorValue = MutableStateFlow(TextFieldValue(""))
    val editorValue: StateFlow<TextFieldValue> = _editorValue.asStateFlow()

    private val _isUnsaved = MutableStateFlow(false)
    val isUnsaved: StateFlow<Boolean> = _isUnsaved.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _stdout = MutableStateFlow("")
    val stdout: StateFlow<String> = _stdout.asStateFlow()

    private val _stderr = MutableStateFlow("")
    val stderr: StateFlow<String> = _stderr.asStateFlow()

    private val _elapsedMs = MutableStateFlow<Long?>(null)
    val elapsedMs: StateFlow<Long?> = _elapsedMs.asStateFlow()

    private val _hasError = MutableStateFlow<Boolean?>(null)
    val hasError: StateFlow<Boolean?> = _hasError.asStateFlow()

    private val _plots = MutableStateFlow<List<String>>(emptyList())
    val plots: StateFlow<List<String>> = _plots.asStateFlow()

    private val _htmlOutputs = MutableStateFlow<List<String>>(emptyList())
    val htmlOutputs: StateFlow<List<String>> = _htmlOutputs.asStateFlow()

    // 0: Editor, 1: Terminal, 2: Visual / Web View
    private val _selectedOutputTab = MutableStateFlow(0)
    val selectedOutputTab: StateFlow<Int> = _selectedOutputTab.asStateFlow()

    private val _fontSizeSp = MutableStateFlow(14)
    val fontSizeSp: StateFlow<Int> = _fontSizeSp.asStateFlow()

    private var executionJob: Job? = null
    private var lastSavedContent: String = ""

    init {
        loadWorkspace()
    }

    fun loadWorkspace() {
        viewModelScope.launch {
            val fileList = fileManager.initializeWorkspace()
            _files.value = fileList

            if (_activeFile.value == null && fileList.isNotEmpty()) {
                openFile(fileList.first())
            }
        }
    }

    fun openFile(file: PythonFile) {
        viewModelScope.launch {
            // Auto save current file before switching if needed
            val current = _activeFile.value
            if (current != null && _isUnsaved.value) {
                fileManager.saveFile(current, _editorValue.value.text)
            }

            val content = fileManager.readFile(file)
            _activeFile.value = file
            _editorValue.value = TextFieldValue(text = content, selection = TextRange(0))
            lastSavedContent = content
            _isUnsaved.value = false
            _selectedOutputTab.value = 0 // Switch to Editor tab
        }
    }

    fun onEditorChange(newValue: TextFieldValue) {
        _editorValue.value = newValue
        _isUnsaved.value = newValue.text != lastSavedContent
    }

    fun saveCurrentFile() {
        val current = _activeFile.value ?: return
        viewModelScope.launch {
            val content = _editorValue.value.text
            val updated = fileManager.saveFile(current, content)
            _activeFile.value = updated
            lastSavedContent = content
            _isUnsaved.value = false
            _files.value = fileManager.getFiles()
        }
    }

    fun createNewFile(name: String, content: String = "") {
        viewModelScope.launch {
            val defaultScript = content.ifEmpty {
                "# ${if (name.endsWith(".py")) name else "$name.py"}\n# Created with Python IDE\n\nprint(\"Hello from $name!\")\n"
            }
            val newFile = fileManager.createFile(name, defaultScript)
            _files.value = fileManager.getFiles()
            openFile(newFile)
        }
    }

    fun renameFile(file: PythonFile, newName: String) {
        viewModelScope.launch {
            val updated = fileManager.renameFile(file, newName)
            _files.value = fileManager.getFiles()
            if (_activeFile.value?.path == file.path) {
                _activeFile.value = updated
            }
        }
    }

    fun deleteFile(file: PythonFile) {
        viewModelScope.launch {
            fileManager.deleteFile(file)
            val remaining = fileManager.getFiles()
            _files.value = remaining

            if (_activeFile.value?.path == file.path) {
                if (remaining.isNotEmpty()) {
                    openFile(remaining.first())
                } else {
                    _activeFile.value = null
                    _editorValue.value = TextFieldValue("")
                    lastSavedContent = ""
                    _isUnsaved.value = false
                }
            }
        }
    }

    fun restoreSamples() {
        viewModelScope.launch {
            val reseeded = fileManager.resetAllSamples()
            _files.value = reseeded
            if (reseeded.isNotEmpty()) {
                openFile(reseeded.first())
            }
        }
    }

    fun runCurrentScript() {
        val codeToRun = _editorValue.value.text
        if (codeToRun.isBlank()) return

        // Auto save on run
        saveCurrentFile()

        // Cancel previous if running
        stopExecution()

        _isRunning.value = true
        _stdout.value = ""
        _stderr.value = ""
        _elapsedMs.value = null
        _hasError.value = null
        _selectedOutputTab.value = 1 // Switch to Terminal to watch execution

        val currentFile = _activeFile.value
        val scriptPath = currentFile?.path
        val workspaceDir = fileManager.workspaceDir.absolutePath

        executionJob = viewModelScope.launch {
            val result = pythonEngine.executeScript(
                code = codeToRun,
                scriptPath = scriptPath,
                workspaceDir = workspaceDir,
                outputListener = object : OutputListener {
                    override fun onOutput(text: String) {
                        if (text.contains("\u001B[H\u001B[J") || text.contains("\u001B[2J") || text.contains("\u001B[1;1H\u001B[2J")) {
                            val lastFrame = text.split(Regex("\u001B\\[H\u001B\\[J|\u001B\\[2J|\u001B\\[1;1H\u001B\\[2J")).lastOrNull() ?: text
                            _stdout.value = lastFrame
                        } else {
                            _stdout.value += text
                        }
                    }

                    override fun onError(text: String) {
                        val filtered = filterInternalWarnings(text)
                        if (filtered.isNotEmpty()) {
                            _stderr.value += filtered
                        }
                    }

                    override fun onVisualUpdate(plots: List<String>, htmlOutputs: List<String>) {
                        if (plots.isNotEmpty()) {
                            _plots.value = plots
                        }
                        if (htmlOutputs.isNotEmpty()) {
                            _htmlOutputs.value = htmlOutputs
                            _selectedOutputTab.value = 2
                        }
                    }
                }
            )

            _isRunning.value = false
            _stdout.value = cleanFinalStdout(result.stdout)
            _stderr.value = filterInternalWarnings(result.stderr)
            _elapsedMs.value = result.elapsedMs
            _hasError.value = !result.success && _stderr.value.isNotBlank()
            _plots.value = result.plots
            _htmlOutputs.value = result.htmlOutputs

            // Automatically switch to Visual tab if plots or HTML are detected
            if (result.plots.isNotEmpty() || result.htmlOutputs.isNotEmpty()) {
                _selectedOutputTab.value = 2
            }
        }
    }

    private fun cleanFinalStdout(stdout: String): String {
        return if (stdout.contains("\u001B[H\u001B[J") || stdout.contains("\u001B[2J")) {
            stdout.split(Regex("\u001B\\[H\u001B\\[J|\u001B\\[2J")).lastOrNull() ?: stdout
        } else {
            stdout
        }
    }

    private fun filterInternalWarnings(stderr: String): String {
        if (stderr.isBlank()) return ""
        val lines = stderr.lines()
        val filtered = lines.filterNot { line ->
            val trimmed = line.trim()
            trimmed.startsWith("ResourceWarning:") ||
            trimmed.contains("Enable tracemalloc to get the object allocation traceback") ||
            (trimmed.contains("ResourceWarning") && trimmed.contains("unclosed")) ||
            trimmed.startsWith("sys:1: ResourceWarning") ||
            (trimmed.startsWith("try:") && lines.any { it.contains("ResourceWarning") }) ||
            (trimmed.startsWith("if not had_real_read:") && lines.any { it.contains("ResourceWarning") })
        }
        return filtered.joinToString("\n").trim()
    }

    fun stopExecution() {
        if (_isRunning.value) {
            pythonEngine.stopExecution()
            executionJob?.cancel()
            _isRunning.value = false
            _stderr.value += "\n⚠️ [Execution interrupted / stopped]"
            _hasError.value = true
        }
    }

    fun clearTerminal() {
        _stdout.value = ""
        _stderr.value = ""
        _elapsedMs.value = null
        _hasError.value = null
    }

    fun clearVisuals() {
        _plots.value = emptyList()
        _htmlOutputs.value = emptyList()
    }

    fun setOutputTab(tabIndex: Int) {
        _selectedOutputTab.value = tabIndex
    }

    fun indentEditor() {
        insertSnippet("    ")
    }

    fun insertSnippet(snippet: String) {
        val current = _editorValue.value
        val text = current.text
        val selection = current.selection

        val start = selection.min
        val end = selection.max

        val newText = text.substring(0, start) + snippet + text.substring(end)
        val newCursor = start + snippet.length

        onEditorChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        )
    }

    fun adjustFontSize(delta: Int) {
        val newSize = (_fontSizeSp.value + delta).coerceIn(10, 26)
        _fontSizeSp.value = newSize
    }
}
