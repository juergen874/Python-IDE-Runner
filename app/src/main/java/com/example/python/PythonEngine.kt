package com.example.python

import android.content.Context
import android.util.Log
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ExecutionResult(
    val success: Boolean,
    val stdout: String,
    val stderr: String,
    val error: String,
    val elapsedMs: Long,
    val plots: List<String>,
    val htmlOutputs: List<String>
)

interface OutputListener {
    fun onOutput(text: String)
    fun onError(text: String)
    fun onVisualUpdate(plots: List<String>, htmlOutputs: List<String>) {}
}

class PythonCallbackProxy(private val listener: OutputListener?) {
    fun onOutput(text: String) {
        try {
            listener?.onOutput(text)
        } catch (e: Throwable) {
            Log.e("PythonEngine", "Error delivering onOutput callback", e)
        }
    }

    fun onError(text: String) {
        try {
            listener?.onError(text)
        } catch (e: Throwable) {
            Log.e("PythonEngine", "Error delivering onError callback", e)
        }
    }

    fun onVisualUpdate(plotsJson: String, htmlJson: String) {
        try {
            val plotsList = mutableListOf<String>()
            val pArr = JSONArray(plotsJson)
            for (i in 0 until pArr.length()) {
                plotsList.add(pArr.getString(i))
            }
            val htmlList = mutableListOf<String>()
            val hArr = JSONArray(htmlJson)
            for (i in 0 until hArr.length()) {
                htmlList.add(hArr.getString(i))
            }
            listener?.onVisualUpdate(plotsList, htmlList)
        } catch (e: Throwable) {
            Log.e("PythonEngine", "Error delivering onVisualUpdate callback", e)
        }
    }
}

class PythonEngine private constructor(private val context: Context) {

    private var isInitialized = false

    fun initialize(): Boolean {
        if (isInitialized && Python.isStarted()) return true
        return try {
            if (!Python.isStarted()) {
                Python.start(AndroidPlatform(context.applicationContext))
            }
            isInitialized = Python.isStarted()
            isInitialized
        } catch (e: Throwable) {
            Log.e("PythonEngine", "Failed to initialize Python runtime", e)
            false
        }
    }

    suspend fun executeScript(
        code: String,
        scriptPath: String?,
        workspaceDir: String,
        outputListener: OutputListener? = null
    ): ExecutionResult = withContext(Dispatchers.IO) {
        if (!initialize()) {
            return@withContext ExecutionResult(
                success = false,
                stdout = "",
                stderr = "Failed to initialize Python runtime environment (Chaquopy).",
                error = "Python Initialization Failed",
                elapsedMs = 0,
                plots = emptyList(),
                htmlOutputs = emptyList()
            )
        }

        try {
            val py = Python.getInstance()
            val runnerModule = py.getModule("runner_hook")

            val proxy = PythonCallbackProxy(outputListener)

            val pyResult: PyObject = runnerModule.callAttr(
                "execute_python_code",
                code,
                scriptPath ?: "",
                workspaceDir,
                proxy
            )

            val jsonString = pyResult.toString()
            val json = JSONObject(jsonString)

            val success = json.optBoolean("success", false)
            val stdout = json.optString("stdout", "")
            val stderr = json.optString("stderr", "")
            val error = json.optString("error", "")
            val elapsedMs = json.optLong("elapsed_ms", 0L)

            val plotsList = mutableListOf<String>()
            val plotsJsonArray = json.optJSONArray("plots")
            if (plotsJsonArray != null) {
                for (i in 0 until plotsJsonArray.length()) {
                    plotsList.add(plotsJsonArray.getString(i))
                }
            }

            val htmlList = mutableListOf<String>()
            val htmlJsonArray = json.optJSONArray("html_outputs")
            if (htmlJsonArray != null) {
                for (i in 0 until htmlJsonArray.length()) {
                    htmlList.add(htmlJsonArray.getString(i))
                }
            }

            ExecutionResult(
                success = success,
                stdout = stdout,
                stderr = stderr,
                error = error,
                elapsedMs = elapsedMs,
                plots = plotsList,
                htmlOutputs = htmlList
            )
        } catch (e: Throwable) {
            val errorMsg = e.localizedMessage ?: e.message ?: e.toString()
            Log.e("PythonEngine", "Execution exception: $errorMsg", e)
            ExecutionResult(
                success = false,
                stdout = "",
                stderr = "Execution Exception:\n$errorMsg\n\n${e.stackTraceToString()}",
                error = errorMsg,
                elapsedMs = 0,
                plots = emptyList(),
                htmlOutputs = emptyList()
            )
        }
    }

    fun stopExecution() {
        try {
            if (Python.isStarted()) {
                val py = Python.getInstance()
                val runnerModule = py.getModule("runner_hook")
                runnerModule.callAttr("request_stop")
            }
        } catch (e: Throwable) {
            Log.e("PythonEngine", "Error requesting script stop", e)
        }
    }

    companion object {
        @Volatile
        private var instance: PythonEngine? = null

        fun getInstance(context: Context): PythonEngine {
            return instance ?: synchronized(this) {
                instance ?: PythonEngine(context.applicationContext).also { instance = it }
            }
        }
    }
}
