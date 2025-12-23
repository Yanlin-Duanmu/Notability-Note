package com.noteability.mynote.ui.component

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
    
    val webViewContent = remember { AtomicReference(content) }
    val isEditorReady = remember { mutableStateOf(false) }
    val hasInitialized = remember { mutableStateOf(false) }
    val initialContent = remember { content }
    
    val currentOnContentChange = rememberUpdatedState(onContentChange)
    val currentOnControllerReady = rememberUpdatedState(onControllerReady)
    
    // Provide controller when editor is ready
    LaunchedEffect(isEditorReady.value) {
        if (isEditorReady.value) {
            WebViewManager.getWebView()?.let { webView ->
                val controller = WebViewVditorController(webView)
                currentOnControllerReady.value(controller)
            }
        }
    }
    
    // Bind callbacks and cleanup
    DisposableEffect(Unit) {
        WebViewManager.bindCallbacks(
            onContentChange = { newContent ->
                val oldContent = webViewContent.getAndSet(newContent)
                if (oldContent != newContent) {
                    currentOnContentChange.value(newContent)
                }
            },
            onEditorReady = {
                isEditorReady.value = true
                WebViewManager.evaluateJs(themeJs)
            },
            onPageLoaded = {
                if (!hasInitialized.value) {
                    hasInitialized.value = true
                    WebViewManager.initEditor(initialContent)
                }
            }
        )
        
        onDispose {
            WebViewManager.unbindCallbacks()
            WebViewManager.detachFromParent()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { _ ->
            WebViewManager.detachFromParent()
            WebViewManager.getWebView()!!
        },
        update = { _ ->
            val currentWebViewContent = webViewContent.get()
            if (isEditorReady.value && content != currentWebViewContent) {
                webViewContent.set(content)
                WebViewManager.setContent(content)
            }
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