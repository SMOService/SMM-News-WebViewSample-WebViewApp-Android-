package com.smoservice.medianews

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.Uri
import android.net.http.SslError
import android.os.Bundle
import android.os.Environment
import android.os.PersistableBundle
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.smoservice.medianews.databinding.ActivityMainBinding


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var binding: ActivityMainBinding

    private var uploadMessageAboveL: ValueCallback<Array<Uri?>?>? = null
    private val FILE_CHOOSER_RESULT_CODE = 150

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        with(binding.webView) {
            settings.javaScriptEnabled = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.domStorageEnabled = true
            settings.allowContentAccess = true
            settings.allowFileAccess = true
            settings.setGeolocationEnabled(true)
            settings.useWideViewPort = true
            settings.blockNetworkLoads = false
            settings.databasePath = applicationContext.filesDir.absolutePath + "/databases"
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.databaseEnabled = true
            settings.pluginState = WebSettings.PluginState.ON
            settings.loadWithOverviewMode = true
            CookieManager.getInstance().setAcceptCookie(true)
            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true);

            setDownloadListener { url, _, contentDisposition, mimeType, _ ->
                val request = DownloadManager.Request(
                    Uri.parse(url)
                )
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                        url, contentDisposition, mimeType
                    )
                )

                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request);
            }

            webViewClient = MyWebViewClient()
            webChromeClient = MyWebChromeClient()

            loadUrl("https://smoservice.media/news/")
        }

        onBackPressedDispatcher.addCallback(this) {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
            } else {
                moveTaskToBack(true)
            }
        }

        binding.reloadBtn.setOnClickListener {
            binding.noInternetLayout.isVisible = false
            binding.splash.isVisible = true
            binding.webView.clearHistory()
            binding.webView.loadUrl("https://smoservice.media/news/")

            lifecycleScope.launch {
                delay(3000L)
                binding.splash.isVisible = false
            }
        }

        binding.blogBtn.setOnClickListener {
            binding.webView.loadUrl("https://smoservice.media/news/")
        }
        binding.toolsBtn.setOnClickListener {
            binding.webView.loadUrl("https://smoservice.media/pages/tools/")
        }
        binding.faqBtn.setOnClickListener {
            binding.webView.loadUrl("https://smoservice.media/faq.php")
        }

        val uri: Uri =
            Uri.parse("android.resource://" + packageName.toString() + "/" + R.raw.intro)
        binding.intro.setVideoURI(uri)
        binding.intro.setBackgroundColor(Color.TRANSPARENT)
        binding.intro.setZOrderOnTop(true)
        binding.intro.start()
        binding.intro.setOnCompletionListener {
            binding.splash.isVisible = false
            binding.intro.isVisible = false
            if (checkInternetConnection(this@MainActivity)) {
                binding.noInternetLayout.isVisible = false
            }
            firstStart = false
        }
    }

    private var firstStart = true

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        binding.webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        binding.webView.restoreState(savedInstanceState)
    }

    private inner class MyWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            if (!firstStart) {
                binding.splash.isVisible = false
                if (checkInternetConnection(this@MainActivity)) {
                    binding.noInternetLayout.isVisible = false
                }
            }
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            val url = request?.url.toString()
            Log.d(TAG, "shouldOverrideUrlLoading: $url")

            return if (URLUtil.isNetworkUrl(url)) {
                false
            } else {
                val intent = Intent(Intent.ACTION_VIEW, request?.url)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                true
            }
        }

        override fun onReceivedError(
            view: WebView?,
            request: WebResourceRequest?,
            error: WebResourceError?
        ) {
            Log.d(TAG, "onReceivedError: ")
            if (checkInternetConnection(this@MainActivity)) {
                super.onReceivedError(view, request, error)
            } else {
                binding.noInternetLayout.isVisible = true
                binding.webView.loadUrl("about:blank")
            }
        }

        override fun onReceivedHttpError(
            view: WebView?,
            request: WebResourceRequest?,
            errorResponse: WebResourceResponse?
        ) {
            Log.d(TAG, "onReceivedHttpError: ")
            if (checkInternetConnection(this@MainActivity)) {
                super.onReceivedHttpError(view, request, errorResponse)
            } else {
                binding.noInternetLayout.isVisible = true
                binding.webView.loadUrl("about:blank")
            }
        }

        override fun onReceivedSslError(
            view: WebView?,
            handler: SslErrorHandler?,
            error: SslError?
        ) {
            Log.d(TAG, "onReceivedSslError: ")
            if (checkInternetConnection(this@MainActivity)) {
                super.onReceivedSslError(view, handler, error)
            } else {
                binding.noInternetLayout.isVisible = true
                binding.webView.loadUrl("about:blank")
            }
        }

        fun checkInternetConnection(context: Context): Boolean {
            val con_manager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return (con_manager.activeNetworkInfo != null && con_manager.activeNetworkInfo!!.isAvailable
                    && con_manager.activeNetworkInfo!!.isConnected)
        }
    }

    fun checkInternetConnection(context: Context): Boolean {
        val con_manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return (con_manager.activeNetworkInfo != null && con_manager.activeNetworkInfo!!.isAvailable
                && con_manager.activeNetworkInfo!!.isConnected)
    }

    private inner class MyWebChromeClient : WebChromeClient() {
        private var mCustomView: View? = null
        private var mCustomViewCallback: CustomViewCallback? = null
        private var mOriginalOrientation = 0
        private var mOriginalSystemUiVisibility = 0

        //For Android API >= 21 (5.0 OS)
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri?>?>,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            uploadMessageAboveL = filePathCallback
            openChooserActivity()
            return true
        }

        override fun onPermissionRequest(request: PermissionRequest) {
            request.grant(request.resources)
        }

        override fun getDefaultVideoPoster(): Bitmap? {
            return if (mCustomView == null) {
                null
            } else BitmapFactory.decodeResource(applicationContext.resources, 2130837573)
        }

        override fun onHideCustomView() {
            (window.decorView as FrameLayout).removeView(mCustomView)
            mCustomView = null
            window.decorView.systemUiVisibility = mOriginalSystemUiVisibility
            requestedOrientation = mOriginalOrientation
            mCustomViewCallback!!.onCustomViewHidden()
            mCustomViewCallback = null
        }

        override fun onShowCustomView(
            paramView: View?,
            paramCustomViewCallback: CustomViewCallback?
        ) {
            if (mCustomView != null) {
                onHideCustomView()
                return
            }
            mCustomView = paramView
            mOriginalSystemUiVisibility = window.decorView.systemUiVisibility
            mOriginalOrientation = requestedOrientation
            mCustomViewCallback = paramCustomViewCallback
            (window.decorView as FrameLayout).addView(
                mCustomView,
                FrameLayout.LayoutParams(-1, -1)
            )
            window.decorView.systemUiVisibility = 3846 or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }
    }

    private fun openChooserActivity() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.addCategory(Intent.CATEGORY_OPENABLE)
        i.type = "*/*"
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(i, "Choose"), FILE_CHOOSER_RESULT_CODE)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webView.destroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_CHOOSER_RESULT_CODE) {
            if (uploadMessageAboveL != null) {
                onActivityResultAboveL(requestCode, resultCode, data)
            }
        }
    }

    private fun onActivityResultAboveL(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode != FILE_CHOOSER_RESULT_CODE || uploadMessageAboveL == null) return
        var results: Array<Uri?>? = null
        if (resultCode == RESULT_OK) {
            if (intent != null) {
                val dataString = intent.dataString
                val clipData = intent.clipData
                if (clipData != null) {
                    results = arrayOfNulls(clipData.itemCount)
                    for (i in 0 until clipData.itemCount) {
                        val item = clipData.getItemAt(i)
                        results[i] = item.uri
                    }
                }
                if (dataString != null) results = arrayOf(Uri.parse(dataString))
            }
        }
        uploadMessageAboveL?.onReceiveValue(results)
        uploadMessageAboveL = null
    }
}