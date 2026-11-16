package com.rag.interview

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast

class AppChromeClient(private val activity: Activity) : WebChromeClient() {

    companion object {
        const val TAG = "WebConsole"
        const val FILE_CHOOSER_REQUEST_CODE = 1001
    }

    private var uploadMessage: ValueCallback<Array<Uri>>? = null

    override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
        val msg = "${consoleMessage.message()} [${consoleMessage.sourceId()}:${consoleMessage.lineNumber()}]"
        when (consoleMessage.messageLevel()) {
            ConsoleMessage.MessageLevel.ERROR -> Log.e(TAG, msg)
            ConsoleMessage.MessageLevel.WARNING -> Log.w(TAG, msg)
            else -> Log.d(TAG, msg)
        }
        return true
    }

    override fun onShowFileChooser(
        webView: WebView,
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: FileChooserParams
    ): Boolean {
        uploadMessage?.onReceiveValue(null)
        uploadMessage = filePathCallback

        val intent = fileChooserParams.createIntent()
        try {
            activity.startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
        } catch (e: Exception) {
            uploadMessage = null
            Toast.makeText(activity, "无法打开文件选择器", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    fun handleResult(resultCode: Int, data: Intent?) {
        if (uploadMessage != null) {
            var results: Array<Uri>? = null
            if (resultCode == Activity.RESULT_OK && data != null) {
                val dataUri = data.data
                if (dataUri != null) {
                    results = arrayOf(dataUri)
                }
            }
            uploadMessage?.onReceiveValue(results)
            uploadMessage = null
        }
    }
}
