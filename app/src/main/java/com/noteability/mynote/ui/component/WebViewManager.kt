package com.noteability.mynote.ui.component

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages a single pre-warmed WebView instance for the editor.
 * Uses Application Context to avoid Activity memory leaks.
 */
@SuppressLint("SetJavaScriptEnabled", "StaticFieldLeak")
object WebViewManager {

    // We use Application Context and need a static reference for prewarming.
    private var webView: WebView? = null
    
    private val isPrewarmed = AtomicBoolean(false)
    private val isPageLoaded = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // Callbacks
    private var onContentChangeCallback: ((String) -> Unit)? = null
    private var onEditorReadyCallback: (() -> Unit)? = null
    private var onPageLoadedCallback: (() -> Unit)? = null
    
    private const val EDITOR_URL = "file:///android_asset/editor.html"
    
    fun prewarm(context: Context) {
        if (isPrewarmed.getAndSet(true)) return
        
        // Use Application Context to prevent Activity leaks
        val appContext = context.applicationContext
        mainHandler.post {
            webView = createWebView(appContext)
            webView?.loadUrl(EDITOR_URL)
        }
    }
    
    fun getWebView(): WebView? = webView
    
    fun bindCallbacks(
        onContentChange: (String) -> Unit,
        onEditorReady: () -> Unit,
        onPageLoaded: () -> Unit
    ) {
        onContentChangeCallback = onContentChange
        onEditorReadyCallback = onEditorReady
        onPageLoadedCallback = onPageLoaded
        
        // If page is already loaded, trigger callback immediately
        if (isPageLoaded.get()) {
            mainHandler.post { onPageLoaded() }
        }
    }
    
    fun unbindCallbacks() {
        onContentChangeCallback = null
        onEditorReadyCallback = null
        onPageLoadedCallback = null
    }
    
    fun initEditor(content: String) {
        executeJs("initVditor(`${escapeJs(content)}`)")
    }
    
    fun setContent(content: String) {
        executeJs("setContent(`${escapeJs(content)}`)")
    }
    
    fun evaluateJs(js: String) {
        executeJs(js)
    }
    
    fun flushContent() {
        executeJs("flushContent()")
    }
    
    fun detachFromParent() {
        (webView?.parent as? ViewGroup)?.removeView(webView)
    }

    /**
     * Release WebView resources. Call from Application.onTerminate() or when no longer needed.
     */
    fun destroy() {
        mainHandler.post {
            webView?.let { wv ->
                (wv.parent as? ViewGroup)?.removeView(wv)
                wv.stopLoading()
                wv.clearHistory()
                wv.clearCache(true)
                wv.removeAllViews()
                wv.destroy()
            }
            webView = null
            isPrewarmed.set(false)
            isPageLoaded.set(false)
            unbindCallbacks()
        }
    }

    private fun executeJs(script: String) {
        webView?.evaluateJavascript(script, null)
    }

    private fun escapeJs(content: String): String {
        return content.replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
    }
    
    private fun createWebView(context: Context): WebView {
        return WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
            
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                setSupportZoom(false)
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                
                // Required for loading local images from app's internal storage (file://)
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
            }
            
            webChromeClient = WebChromeClient()
            
            // Add interface BEFORE page loads
            addJavascriptInterface(JsBridge(), "Android")
            
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isPageLoaded.set(true)
                }
            }
        }
    }
    
    // Suppress unused: Methods are called from JavaScript
    @Suppress("unused")
    private class JsBridge {
        @JavascriptInterface
        fun onContentChange(content: String) {
            mainHandler.post {
                onContentChangeCallback?.invoke(content)
            }
        }
        
        @JavascriptInterface
        fun onEditorReady() {
            mainHandler.post {
                onEditorReadyCallback?.invoke()
            }
        }
        
        @JavascriptInterface
        fun onPageLoaded() {
            mainHandler.post {
                onPageLoadedCallback?.invoke()
            }
        }
    }
}