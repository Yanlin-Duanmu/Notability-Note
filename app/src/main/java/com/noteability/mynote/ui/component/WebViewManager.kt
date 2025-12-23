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

@SuppressLint("SetJavaScriptEnabled")
object WebViewManager {
    
    private var webView: WebView? = null
    private val isPrewarmed = AtomicBoolean(false)
    private val isPageLoaded = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    
    private var onContentChangeCallback: ((String) -> Unit)? = null
    private var onEditorReadyCallback: (() -> Unit)? = null
    private var onPageLoadedCallback: (() -> Unit)? = null
    
    private const val EDITOR_URL = "file:///android_asset/editor.html"
    
    fun prewarm(context: Context) {
        if (isPrewarmed.getAndSet(true)) return
        
        mainHandler.post {
            webView = createWebView(context.applicationContext)
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
        
        // If page already loaded during prewarm, trigger immediately
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
        webView?.let { wv ->
            val escaped = content.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            wv.evaluateJavascript("initVditor(`$escaped`)", null)
        }
    }
    
    fun setContent(content: String) {
        webView?.let { wv ->
            val escaped = content.replace("\\", "\\\\").replace("`", "\\`").replace("$", "\\$")
            wv.evaluateJavascript("setContent(`$escaped`)", null)
        }
    }
    
    fun resetEditor() {
        isPageLoaded.set(false)
        webView?.evaluateJavascript("if(vditor){vditor.destroy();vditor=null;}", null)
        webView?.loadUrl(EDITOR_URL)
    }
    
    fun evaluateJs(js: String) {
        webView?.evaluateJavascript(js, null)
    }
    
    fun flushContent() {
        webView?.evaluateJavascript("flushContent()", null)
    }
    
    fun detachFromParent() {
        (webView?.parent as? ViewGroup)?.removeView(webView)
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
                databaseEnabled = true
                setSupportZoom(false)
                @Suppress("DEPRECATION")
                allowUniversalAccessFromFileURLs = true
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
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