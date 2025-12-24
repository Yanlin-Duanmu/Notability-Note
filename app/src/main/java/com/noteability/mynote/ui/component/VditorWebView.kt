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
    modifier: Modifier = Modifier,
    onControllerReady: (VditorController) -> Unit = {}
) {
    // Handle Preview mode
    if (LocalInspectionMode.current) {
        VditorPreviewPlaceholder(modifier)
        return
    }

    // Theme configuration
    val colors = VditorColors(
        background = MaterialTheme.colorScheme.background,
        text = MaterialTheme.colorScheme.onBackground,
        textSecondary = MaterialTheme.colorScheme.onSurfaceVariant,
        border = MaterialTheme.colorScheme.outlineVariant,
        primary = MaterialTheme.colorScheme.primary
    )
    val isDarkTheme = isSystemInDarkTheme()

    // Generate theme JS only when colors or theme changes
    val themeJs = remember(isDarkTheme, colors) {
        buildThemeInjectionJs(colors)
    }

    // State management
    val webViewContent = remember { AtomicReference(content) }
    val isEditorReady = remember { mutableStateOf(false) }
    val hasInitialized = remember { mutableStateOf(false) }
    
    // Store initial content to init editor once ready
    val initialContent = remember { content }

    // Keep latest lambdas for callbacks
    val currentOnContentChange = rememberUpdatedState(onContentChange)
    val currentOnControllerReady = rememberUpdatedState(onControllerReady)

    // Notify controller ready
    LaunchedEffect(isEditorReady.value) {
        if (isEditorReady.value) {
            WebViewManager.getWebView()?.let { webView ->
                currentOnControllerReady.value(WebViewVditorController(webView))
            }
        }
    }

    // Manage WebView lifecycle and callbacks
    DisposableEffect(Unit) {
        WebViewManager.bindCallbacks(
            onContentChange = { newContent ->
                // Avoid loop: only notify if content actually changed from WebView side
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

    // WebView Interop
    AndroidView(
        modifier = modifier,
        factory = { _ ->
            // Ensure WebView is detached from any previous parent before reuse
            WebViewManager.detachFromParent()
            WebViewManager.getWebView() ?: throw IllegalStateException("WebView not prewarmed")
        },
        update = { _ ->
            // Sync content from Compose to WebView if changed externally
            val currentStored = webViewContent.get()
            if (isEditorReady.value && content != currentStored) {
                webViewContent.set(content)
                WebViewManager.setContent(content)
            }
        }
    )
}

@Composable
private fun VditorPreviewPlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Vditor Editor Preview\n(WebView not available in Preview)",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// Wrapper for colors to simplify passing around
private data class VditorColors(
    val background: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val primary: Color
)

private fun buildThemeInjectionJs(colors: VditorColors): String {
    val bgHex = colors.background.toHexString()
    val textHex = colors.text.toHexString()
    val textSecondaryHex = colors.textSecondary.toHexString()
    val borderHex = colors.border.toHexString()
    val primaryHex = colors.primary.toHexString()

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

private fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

private fun escapeJsString(content: String): String {
    return content.replace("\\", "\\\\")
        .replace("`", "\\`")
        .replace("$", "\\$")
}

private class WebViewVditorController(private val webView: WebView) : VditorController {
    override fun formatBold() = evaluate("formatBold()")
    override fun formatItalic() = evaluate("formatItalic()")
    override fun formatList() = evaluate("formatList()")
    override fun formatQuote() = evaluate("formatQuote()")
    override fun formatCode() = evaluate("formatCode()")
    
    override fun insertImage(url: String, description: String) {
        evaluate("insertImage(`${escapeJsString(url)}`, `${escapeJsString(description)}`)")
    }
    
    override fun insertLink(url: String, text: String) {
        evaluate("insertLink(`${escapeJsString(url)}`, `${escapeJsString(text)}`)")
    }

    private fun evaluate(script: String) {
        webView.evaluateJavascript(script, null)
    }
}