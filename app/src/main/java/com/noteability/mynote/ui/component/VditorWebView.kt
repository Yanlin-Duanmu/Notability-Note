package com.noteability.mynote.ui.component

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "VditorWebView"

// Controller interface for editor formatting commands
interface VditorController {
    fun formatBold()
    fun formatItalic()
    fun formatList()
    fun formatQuote()
    fun formatCode()
    fun insertImage(url: String, description: String)
    fun insertLink(url: String, text: String)
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VditorWebView(
    content: String,
    onContentChange: (String) -> Unit,
    onControllerReady: (VditorController) -> Unit = {},
    modifier: Modifier = Modifier
) {
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

    // Capture theme colors from MaterialTheme
    val backgroundColor = MaterialTheme.colorScheme.background
    val textColor = MaterialTheme.colorScheme.onBackground
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val isDarkTheme = isSystemInDarkTheme()

    val themeJs = remember(isDarkTheme) {
        buildThemeInjectionJs(
            backgroundColor = backgroundColor,
            textColor = textColor,
            textSecondaryColor = textSecondaryColor,
            borderColor = borderColor,
            primaryColor = primaryColor
        )
    }
    
    // Thread-safe tracking of WebView's actual content
    val webViewContent = remember { AtomicReference(content) }
    val isEditorReady = remember { mutableStateOf(false) }
    val initialContent = remember { content }
    
    // WebView reference for controller
    val webViewRef = remember { AtomicReference<WebView?>(null) }
    
    // Ensure callback is always up-to-date in closures
    val currentOnContentChange = rememberUpdatedState(onContentChange)
    val currentOnControllerReady = rememberUpdatedState(onControllerReady)
    
    // Create and provide controller when editor is ready
    LaunchedEffect(isEditorReady.value) {
        if (isEditorReady.value) {
            webViewRef.get()?.let { webView ->
                val controller = WebViewVditorController(webView)
                currentOnControllerReady.value(controller)
            }
        }
    }

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
                        // Atomic update first to prevent race condition with update block
                        val oldContent = webViewContent.getAndSet(newContent)
                        if (oldContent != newContent) {
                            post {
                                currentOnContentChange.value(newContent)
                            }
                        }
                    }
                    
                    @JavascriptInterface
                    fun onEditorReady() {
                        Log.d(TAG, "onEditorReady received")
                        post {
                            isEditorReady.value = true
                            evaluateJavascript(themeJs, null)
                        }
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
                
                // Store WebView reference
                webViewRef.set(this)
            }
        },
        update = { webView ->
            // Only sync external updates (e.g., AI features) to WebView
            // Skip if content matches WebView's tracked content to avoid cursor reset
            val currentWebViewContent = webViewContent.get()
            if (isEditorReady.value && content != currentWebViewContent) {
                webViewContent.set(content)
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

private fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

private fun buildThemeInjectionJs(
    backgroundColor: Color,
    textColor: Color,
    textSecondaryColor: Color,
    borderColor: Color,
    primaryColor: Color
): String {
    val bgHex = backgroundColor.toHexString()
    val textHex = textColor.toHexString()
    val textSecondaryHex = textSecondaryColor.toHexString()
    val borderHex = borderColor.toHexString()
    val primaryHex = primaryColor.toHexString()
    
    return """
        (function() {
            var root = document.documentElement;
            root.style.setProperty('--editor-bg', '$bgHex');
            root.style.setProperty('--editor-text', '$textHex');
            root.style.setProperty('--editor-text-secondary', '$textSecondaryHex');
            root.style.setProperty('--editor-border', '$borderHex');
            root.style.setProperty('--editor-placeholder', '$textSecondaryHex');
            root.style.setProperty('--panel-background-color', '$bgHex');
            root.style.setProperty('--textarea-background-color', '$bgHex');
            root.style.setProperty('--textarea-text-color', '$textHex');
            root.style.setProperty('--border-color', '$borderHex');
            root.style.setProperty('--second-color', '$textSecondaryHex');
            root.style.setProperty('--ir-heading-color', '$primaryHex');
            root.style.setProperty('--ir-link-color', '$primaryHex');
            root.style.setProperty('--ir-title-color', '$primaryHex');
            root.style.setProperty('--ir-bracket-color', '$primaryHex');
            document.body.style.backgroundColor = '$bgHex';
        })();
    """.trimIndent()
}

// Controller implementation that executes JS commands via WebView
private class WebViewVditorController(private val webView: WebView) : VditorController {
    
    override fun formatBold() {
        webView.evaluateJavascript("formatBold()", null)
    }
    
    override fun formatItalic() {
        webView.evaluateJavascript("formatItalic()", null)
    }
    
    override fun formatList() {
        webView.evaluateJavascript("formatList()", null)
    }
    
    override fun formatQuote() {
        webView.evaluateJavascript("formatQuote()", null)
    }
    
    override fun formatCode() {
        webView.evaluateJavascript("formatCode()", null)
    }
    
    override fun insertImage(url: String, description: String) {
        val escapedUrl = escapeJsString(url)
        val escapedDesc = escapeJsString(description)
        webView.evaluateJavascript("insertImage(`$escapedUrl`, `$escapedDesc`)", null)
    }
    
    override fun insertLink(url: String, text: String) {
        val escapedUrl = escapeJsString(url)
        val escapedText = escapeJsString(text)
        webView.evaluateJavascript("insertLink(`$escapedUrl`, `$escapedText`)", null)
    }
}