package com.rag.interview

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets

class AppWebViewClient(
    private val context: Context,
    private val onPageFinishedListener: ((String) -> Unit)? = null
) : WebViewClient() {

    companion object {
        const val TAG = "AppWebViewClient"
        const val ASSET_ORIGIN = "http://47.98.190.255:81"
        const val HOT_FILE_PATH = "/sdcard/Android/data/com.rag.interview/files/hot/index.html"
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val uri = request.url ?: return null
        return handleIntercept(uri) ?: super.shouldInterceptRequest(view, request)
    }

    override fun shouldInterceptRequest(view: WebView, url: String): WebResourceResponse? {
        val uri = Uri.parse(url)
        return handleIntercept(uri) ?: super.shouldInterceptRequest(view, url)
    }

    private fun handleIntercept(uri: Uri): WebResourceResponse? {
        val urlStr = uri.toString()
        if (!urlStr.startsWith(ASSET_ORIGIN)) {
            return null
        }

        val path = uri.path ?: ""
        if (!path.startsWith("/app/")) {
            return null
        }

        val relativePath = path.substring("/app/".length).ifEmpty { "index.html" }

        // 1. Check hot update file
        if (relativePath == "index.html") {
            try {
                val hotFile = File(HOT_FILE_PATH)
                if (hotFile.exists() && hotFile.length() > 0) {
                    val stream = FileInputStream(hotFile)
                    val response = WebResourceResponse("text/html", "utf-8", stream)
                    val headers = hashMapOf(
                        "Access-Control-Allow-Origin" to "*",
                        "Cache-Control" to "no-cache, no-store, must-revalidate"
                    )
                    response.responseHeaders = headers
                    Log.d(TAG, "Served index.html from HOT UPDATE file: ${hotFile.length()} bytes")
                    return response
                }
            } catch (e: Exception) {
                Log.w(TAG, "Hot update file read failed, falling back to assets", e)
            }
        }

        // 2. Fallback to APK assets/www/
        try {
            val assetPath = "www/$relativePath"
            val stream: InputStream = context.assets.open(assetPath)
            val mime = getMimeType(relativePath)
            val response = WebResourceResponse(mime, "utf-8", stream)
            val headers = hashMapOf(
                "Access-Control-Allow-Origin" to "*",
                "Cache-Control" to "no-cache"
            )
            response.responseHeaders = headers
            return response
        } catch (e: Exception) {
            Log.e(TAG, "Asset not found: www/$relativePath", e)
            val errHtml = "<html><body style='font-family:sans-serif;padding:30px;'>" +
                    "<h2>Resource not found</h2><p>www/$relativePath</p></body></html>"
            val stream = ByteArrayInputStream(errHtml.toByteArray(StandardCharsets.UTF_8))
            return WebResourceResponse("text/html", "utf-8", 404, "Not Found", emptyMap(), stream)
        }
    }

    private fun getMimeType(path: String): String {
        return when {
            path.endsWith(".html") -> "text/html"
            path.endsWith(".js") -> "application/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".json") -> "application/json"
            path.endsWith(".woff2") -> "font/woff2"
            path.endsWith(".woff") -> "font/woff"
            path.endsWith(".ttf") -> "font/ttf"
            else -> "application/octet-stream"
        }
    }

    override fun onPageFinished(view: WebView, url: String) {
        super.onPageFinished(view, url)
        Log.d(TAG, "Page finished loading: $url")
        onPageFinishedListener?.invoke(url)
    }
}
