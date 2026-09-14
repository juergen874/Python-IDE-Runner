package com.example.ui.visual

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VisualWebView(
    plotsBase64: List<String>,
    htmlOutputs: List<String>,
    onClearVisuals: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var useDirectServerUrl by remember { mutableStateOf(false) }

    val hasPlots = plotsBase64.isNotEmpty()
    val hasHtml = htmlOutputs.isNotEmpty()

    val combinedHtmlContent = remember(plotsBase64, htmlOutputs) {
        if (htmlOutputs.isNotEmpty()) {
            htmlOutputs.last()
        } else if (plotsBase64.isNotEmpty()) {
            buildHtmlWithPlots(plotsBase64)
        } else {
            ""
        }
    }

    val detectedBaseUrl = remember(combinedHtmlContent) {
        if (combinedHtmlContent.contains("8080") || combinedHtmlContent.contains("/api/")) {
            "http://127.0.0.1:8080/"
        } else {
            "http://localhost/"
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(IdeBackground)
    ) {
        // Visual View Header
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
                        imageVector = Icons.Default.Preview,
                        contentDescription = null,
                        tint = IdeSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Visual Output",
                        color = IdeTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    if (hasPlots) {
                        Surface(
                            color = IdePrimary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "${plotsBase64.size} Plot(s)",
                                color = IdePrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (hasHtml) {
                        Surface(
                            color = IdeTertiary.copy(alpha = 0.15f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                text = "HTML Dashboard",
                                color = IdeTertiary,
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
                    if (hasHtml || hasPlots) {
                        // Direct server reload toggle
                        IconButton(
                            onClick = {
                                webViewInstance?.let { wv ->
                                    if (combinedHtmlContent.contains("8080") || combinedHtmlContent.contains("/api/")) {
                                        wv.loadUrl("http://127.0.0.1:8080/")
                                    } else {
                                        wv.reload()
                                    }
                                }
                            },
                            modifier = Modifier.size(28.dp).testTag("reload_webview_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = "Reload",
                                tint = IdeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = onClearVisuals,
                            modifier = Modifier.size(28.dp).testTag("clear_visuals_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "Clear Visuals",
                                tint = IdeTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        HorizontalDivider(color = IdeBorder, thickness = 1.dp)

        if (!hasPlots && !hasHtml) {
            // Empty State
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = IdeSurfaceVariant,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.BarChart,
                                contentDescription = null,
                                tint = IdePrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = "No Visual Output Yet",
                        color = IdeTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Run a script using matplotlib (plt.show()), HTML templates, or local Web Server (port 8080) to view live dashboards and interactive graphics here.",
                        color = IdeTextMuted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.widthIn(max = 320.dp)
                    )
                }
            }
        } else {
            // Content Display: Interactive Android WebView
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_NO_CACHE
                            allowFileAccess = true
                            allowContentAccess = true
                            allowFileAccessFromFileURLs = true
                            allowUniversalAccessFromFileURLs = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                        setBackgroundColor(android.graphics.Color.parseColor("#0B0F19"))
                        webViewClient = object : WebViewClient() {
                            override fun onReceivedError(
                                view: WebView?,
                                errorCode: Int,
                                description: String?,
                                failingUrl: String?
                            ) {
                                super.onReceivedError(view, errorCode, description, failingUrl)
                                // Fallback to embedded HTML if direct URL load fails
                                if (failingUrl?.contains("127.0.0.1:8080") == true && combinedHtmlContent.isNotEmpty()) {
                                    view?.loadDataWithBaseURL(
                                        "http://127.0.0.1:8080/",
                                        combinedHtmlContent,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            }
                        }
                        webChromeClient = WebChromeClient()
                        webViewInstance = this
                    }
                },
                update = { webView ->
                    if (combinedHtmlContent.isNotEmpty()) {
                        webView.loadDataWithBaseURL(
                            detectedBaseUrl,
                            combinedHtmlContent,
                            "text/html",
                            "UTF-8",
                            null
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("visual_output_webview")
            )
        }
    }
}

private fun buildHtmlWithPlots(plotsBase64: List<String>): String {
    val imgTags = plotsBase64.mapIndexed { index, b64 ->
        """
        <div class="plot-card">
            <div class="plot-header">Figure #${index + 1}</div>
            <img src="data:image/png;base64,$b64" class="plot-img" alt="Matplotlib Figure ${index + 1}" />
        </div>
        """.trimIndent()
    }.joinToString("\n")

    return """
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=3.0, user-scalable=yes">
        <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body {
                background-color: #0D1117;
                color: #F0F6FC;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                padding: 16px;
            }
            .container {
                display: flex;
                flex-direction: column;
                gap: 16px;
                max-width: 900px;
                margin: 0 auto;
            }
            .plot-card {
                background: #161B22;
                border: 1px solid #30363D;
                border-radius: 12px;
                overflow: hidden;
                box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            }
            .plot-header {
                padding: 10px 16px;
                background: #21262D;
                font-size: 13px;
                font-weight: 600;
                color: #58A6FF;
                border-bottom: 1px solid #30363D;
            }
            .plot-img {
                width: 100%;
                height: auto;
                display: block;
                background: white;
            }
        </style>
    </head>
    <body>
        <div class="container">
            $imgTags
        </div>
    </body>
    </html>
    """.trimIndent()
}
