package com.noteability.mynote.ui.component

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView

private const val TAG = "VditorWebView"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VditorWebView(
    content: String,
    onContentChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // WebView is not supported in Compose Preview (LayoutLib)
    if (LocalInspectionMode.current) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Vditor Editor Preview\n(WebView not available in Preview)",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val webViewClient = remember { WebViewClient() }
    
    // Use remember to keep track of the last content sent to JS to avoid redundant updates
    val lastSentContent = remember { mutableStateOf(content) }
    val isEditorReady = remember { mutableStateOf(false) }

    // Use a stable reference for the initial content to avoid capturing changing state in factory
    val initialContent = remember { content }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                Log.d(TAG, "WebView factory creating...")
                layoutParams = android.view.ViewGroup.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // Performance: Enable hardware acceleration
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    databaseEnabled = true
                    setSupportZoom(false)
                    allowUniversalAccessFromFileURLs = true
                    allowFileAccessFromFileURLs = true
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    
                    // Performance: Cache settings
                    cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                }
                
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        val message = consoleMessage?.message() ?: ""
                        val level = consoleMessage?.messageLevel() ?: ConsoleMessage.MessageLevel.LOG
                        val source = "${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}"
                        
                        when (level) {
                            ConsoleMessage.MessageLevel.ERROR -> Log.e(TAG, "JS Error: $message at $source")
                            ConsoleMessage.MessageLevel.WARNING -> Log.w(TAG, "JS Warning: $message at $source")
                            ConsoleMessage.MessageLevel.LOG -> Log.d(TAG, "JS Log: $message at $source")
                            else -> Log.v(TAG, "JS Console: $message at $source")
                        }
                        return true
                    }
                }

                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onContentChange(newContent: String) {
                        // Use post to ensure we don't block the JS thread
                        post {
                            if (lastSentContent.value != newContent) {
                                lastSentContent.value = newContent
                                onContentChange(newContent)
                            }
                        }
                    }
                    
                    @JavascriptInterface
                    fun onEditorReady() {
                        Log.d(TAG, "onEditorReady received")
                        isEditorReady.value = true
                    }

                    @JavascriptInterface
                    fun onPageLoaded() {
                        Log.d(TAG, "onPageLoaded received from JS")
                        post {
                            val escapedContent = escapeJsString(initialContent)
                            evaluateJavascript("initVditor(`${escapedContent}`)", null)
                        }
                    }
                }, "Android")
                
                this.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d(TAG, "onPageFinished: $url")
                        // Fallback removed or shortened to avoid double init
                    }

                    override fun onReceivedError(view: WebView?, request: android.webkit.WebResourceRequest?, error: android.webkit.WebResourceError?) {
                        super.onReceivedError(view, request, error)
                        Log.e(TAG, "WebView Error: ${error?.description} at ${request?.url}")
                    }
                }
                loadUrl("file:///android_asset/editor.html")
            }
        },
        update = { webView ->
            // Handle external content updates (e.g., from AI features)
            if (isEditorReady.value && content != lastSentContent.value) {
                lastSentContent.value = content
                val escapedContent = escapeJsString(content)
                webView.evaluateJavascript("setContent(`${escapedContent}`)", null)
            }
        },
        onRelease = { webView ->
            webView.destroy()
        }
    )
}

private fun escapeJsString(content: String): String {
    return content.replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("$", "\\$")
}