package com.rag.interview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : Activity() {

    companion object {
        private const val TAG = "MainActivity"

        // Kyant0/AndroidLiquidGlass Pure Optical Glass Refraction with Continuous Gaussian Diffusion (Zero Ghosting)
        private const val AGSL_KYANT_REFRACTION = """
            uniform shader content;

            // Outer capsule uniforms
            uniform float2 size;
            uniform float2 offset;
            uniform float4 cornerRadii;
            uniform float refractionHeight;
            uniform float refractionAmount;
            uniform float depthEffect;

            // Pill secondary liquid droplet glass lens
            uniform float2 pillSize;
            uniform float2 pillOffset;
            uniform float pillRadius;
            uniform float pillRefractAmount;

            float radiusAt(float2 coord, float4 radii) {
                if (coord.x >= 0.0) {
                    if (coord.y <= 0.0) return radii.y;
                    else return radii.z;
                } else {
                    if (coord.y <= 0.0) return radii.x;
                    else return radii.w;
                }
            }

            float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
                float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
                float outside = length(max(cornerCoord, 0.0)) - radius;
                float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
                return outside + inside;
            }

            float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
                float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
                if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
                    return sign(coord) * normalize(max(cornerCoord, 0.0));
                } else {
                    float gradX = step(cornerCoord.y, cornerCoord.x);
                    return sign(coord) * float2(gradX, 1.0 - gradX);
                }
            }

            float circleMap(float x) {
                float cx = clamp(x, 0.0, 1.0);
                return 1.0 - sqrt(max(0.0, 1.0 - cx * cx));
            }

            half4 main(float2 coord) {
                float2 halfSize = size * 0.5;
                float2 centeredCoord = (coord + offset) - halfSize;
                float radius = radiusAt(centeredCoord, cornerRadii);

                float sd = sdRoundedRect(centeredCoord, halfSize, radius);
                if (sd > 1.0) {
                    return content.eval(coord);
                }

                float d = 0.0;
                float2 grad = float2(0.0);

                // 1. Outer capsule rim lens refraction
                if (-sd < refractionHeight) {
                    d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
                    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
                    grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord + float2(1e-5)));
                }

                // 2. Pill secondary liquid glass droplet lens
                float pillDispersion = 0.0;
                float2 pillGrad = float2(0.0);
                if (pillRefractAmount > 0.001) {
                    float2 pillHalf = pillSize * 0.5;
                    float2 pillCentered = (coord + pillOffset) - pillHalf;
                    float pillSd = sdRoundedRect(pillCentered, pillHalf, pillRadius);
                    if (pillSd <= 0.0) {
                        float pd = circleMap(clamp(1.0 - -pillSd / 24.0, 0.0, 1.0)) * pillRefractAmount;
                        pillGrad = normalize(gradSdRoundedRect(pillCentered, pillHalf, pillRadius) + 0.15 * normalize(pillCentered + float2(1e-5)));
                        d += pd;
                        grad = normalize(grad + pillGrad);
                        pillDispersion = 2.0 * pd;
                    }
                }

                float2 refractedCoord = coord + d * grad;

                // 3. Continuous sub-character Gaussian diffusion (step = 3.5px) -> NEVER produces double letters!
                float s = 3.5;
                half4 c = content.eval(refractedCoord) * 0.36;
                c += content.eval(refractedCoord + float2(s, 0.0)) * 0.12;
                c += content.eval(refractedCoord - float2(s, 0.0)) * 0.12;
                c += content.eval(refractedCoord + float2(0.0, s)) * 0.12;
                c += content.eval(refractedCoord - float2(0.0, s)) * 0.12;
                c += content.eval(refractedCoord + float2(s * 0.707, s * 0.707)) * 0.04;
                c += content.eval(refractedCoord - float2(s * 0.707, s * 0.707)) * 0.04;
                c += content.eval(refractedCoord + float2(-s * 0.707, s * 0.707)) * 0.04;
                c += content.eval(refractedCoord + float2(s * 0.707, -s * 0.707)) * 0.04;

                // 4. Subtle chromatic dispersion on the pill droplet
                if (pillDispersion > 0.1) {
                    float2 disp = pillGrad * pillDispersion;
                    half4 rSample = content.eval(refractedCoord + disp);
                    half4 bSample = content.eval(refractedCoord - disp);
                    c.r = mix(c.r, rSample.r, 0.30);
                    c.b = mix(c.b, bSample.b, 0.30);
                }

                if (sd > 0.0) {
                    float aa = clamp(1.0 - sd, 0.0, 1.0);
                    return mix(content.eval(coord), c, aa);
                }

                return c;
            }
        """
    }

    private lateinit var rootLayout: FrameLayout
    private lateinit var webView: WebView
    private lateinit var dockView: LiquidGlassDockView
    private var chromeClient: AppChromeClient? = null
    private var runtimeShader: RuntimeShader? = null

    private var exitTime: Long = 0
    private var safeBottomDp: Float = 24f
    private var safeTopDp: Float = 24f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Enable 120Hz / 144Hz high refresh rate
        enableHighRefreshRate()

        // 2. Setup transparent edge-to-edge system bars
        setupEdgeToEdge()

        // 3. Build UI hierarchy
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(0xFFF4F1EA.toInt())
            fitsSystemWindows = false
        }

        webView = WebView(this).apply {
            setBackgroundColor(0xFFF4F1EA.toInt())
            fitsSystemWindows = false
            setLayerType(View.LAYER_TYPE_HARDWARE, null)
        }

        rootLayout.addView(
            webView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // 4. Add Backdrop-Catalog-Styled LiquidGlassDockView
        dockView = LiquidGlassDockView(this).apply {
            fitsSystemWindows = false
        }

        val dockParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            dpToPx(64f).toInt()
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            val horizMargin = dpToPx(16f).toInt()
            leftMargin = horizMargin
            rightMargin = horizMargin
            bottomMargin = dpToPx(24f).toInt()
        }
        rootLayout.addView(dockView, dockParams)

        setContentView(rootLayout)

        initWebView()
        setupAgslRefraction()
        listenWindowInsets()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        webView.loadUrl("${AppWebViewClient.ASSET_ORIGIN}/app/index.html")
        startHotUpdateCheck()
    }

    private fun setupAgslRefraction() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val shader = RuntimeShader(AGSL_KYANT_REFRACTION)
                runtimeShader = shader

                val density = resources.displayMetrics.density
                dockView.onShaderParamUpdateListener = { dockX, dockY, dockW, dockH, pillCenterX, pillCenterY, pillW, pillH, pressProgress ->
                    val radius = dockH * 0.5f

                    // Outer capsule uniforms
                    shader.setFloatUniform("size", dockW, dockH)
                    shader.setFloatUniform("offset", -dockX, -dockY)
                    shader.setFloatUniform("cornerRadii", radius, radius, radius, radius)
                    shader.setFloatUniform("refractionHeight", 16f * density)
                    shader.setFloatUniform("refractionAmount", -10f * density)
                    shader.setFloatUniform("depthEffect", 0.08f)

                    // Pill secondary liquid droplet lens uniforms
                    val pillLeft = pillCenterX - pillW * 0.5f
                    val pillTop = pillCenterY - pillH * 0.5f
                    shader.setFloatUniform("pillSize", pillW, pillH)
                    shader.setFloatUniform("pillOffset", -pillLeft, -pillTop)
                    shader.setFloatUniform("pillRadius", pillH * 0.5f)
                    shader.setFloatUniform("pillRefractAmount", (8f + 6f * pressProgress) * density)

                    // Apply Kyant AGSL Lens Refraction directly to WebView
                    val lensEffect = RenderEffect.createRuntimeShaderEffect(shader, "content")
                    webView.setRenderEffect(lensEffect)
                    webView.invalidate()
                }

                dockView.post {
                    dockView.updateShaderParams()
                }

                Log.d(TAG, "Kyant0 Optical AGSL Shader successfully initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize AGSL shader", e)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.allowFileAccess = true
        settings.allowContentAccess = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.mediaPlaybackRequiresUserGesture = false

        chromeClient = AppChromeClient(this)
        webView.webChromeClient = chromeClient

        webView.webViewClient = AppWebViewClient(this) {
            injectSafeAreaAndBridge()
        }

        // Bridge for native Kotlin dock communication
        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun onRouteChanged(path: String) {
                runOnUiThread {
                    dockView.selectTabByPath(path)
                }
            }
        }, "AndroidBridge")

        // When user touches native dock, route in WebView
        dockView.onTabSelectedListener = { tab ->
            webView.evaluateJavascript("window.location.hash = '${tab.path}';", null)
        }
    }

    private fun injectSafeAreaAndBridge() {
        val js = """
            (function() {
                try {
                    document.documentElement.style.setProperty('--safe-top', '${safeTopDp}px');
                    document.documentElement.style.setProperty('--safe-bottom', '${safeBottomDp}px');
                    
                    var style = document.getElementById('native-dock-override');
                    if (!style) {
                        style = document.createElement('style');
                        style.id = 'native-dock-override';
                        style.textContent = '.lg-dock-wrapper, .uni-tabbar, .custom-tabbar, .dock-ambient-aurora { display: none !important; }';
                        document.head.appendChild(style);
                    }

                    window.addEventListener('hashchange', function() {
                        if (window.AndroidBridge) {
                            var hash = window.location.hash || '';
                            if (hash.startsWith('#')) hash = hash.substring(1);
                            window.AndroidBridge.onRouteChanged(hash);
                        }
                    });
                } catch(e) {}
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    private fun listenWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val density = resources.displayMetrics.density
            safeTopDp = (systemBars.top / density).coerceAtLeast(24f)
            safeBottomDp = (systemBars.bottom / density).coerceAtLeast(16f)

            val params = dockView.layoutParams as? FrameLayout.LayoutParams
            if (params != null) {
                params.bottomMargin = (systemBars.bottom + dpToPx(10f)).toInt()
                dockView.layoutParams = params
            }

            injectSafeAreaAndBridge()
            insets
        }
    }

    private fun enableHighRefreshRate() {
        try {
            val window = window
            val lp = window.attributes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val display = window.windowManager.defaultDisplay
                val modes = display.supportedModes
                var bestMode: Display.Mode? = null
                var maxRate = 60f
                for (m in modes) {
                    if (m.refreshRate > maxRate) {
                        maxRate = m.refreshRate
                        bestMode = m
                    }
                }
                if (bestMode != null && maxRate >= 90f) {
                    lp.preferredDisplayModeId = bestMode.modeId
                    window.attributes = lp
                    Log.d(TAG, "Preferred display mode set: ${bestMode.modeId} (${maxRate}Hz)")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "High refresh rate config failed", e)
        }
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    private fun startHotUpdateCheck() {
        Thread {
            try {
                val url = URL("${AppWebViewClient.ASSET_ORIGIN}/app/index.html")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.requestMethod = "GET"
                if (conn.responseCode == 200) {
                    val hotDir = File(getExternalFilesDir(null), "hot")
                    if (!hotDir.exists()) hotDir.mkdirs()
                    val targetFile = File(hotDir, "index.html")
                    val tmpFile = File(hotDir, "index.tmp")

                    conn.inputStream.use { input ->
                        FileOutputStream(tmpFile).use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (tmpFile.length() > 50000) {
                        tmpFile.renameTo(targetFile)
                        Log.d(TAG, "Hot update index.html successfully cached: ${targetFile.length()} bytes")
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Hot update check skipped: ${e.message}")
            }
        }.start()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AppChromeClient.FILE_CHOOSER_REQUEST_CODE) {
            chromeClient?.handleResult(resultCode, data)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack()
                return true
            }
            if (System.currentTimeMillis() - exitTime > 2000) {
                Toast.makeText(this, "再按一次退出系统", Toast.LENGTH_SHORT).show()
                exitTime = System.currentTimeMillis()
                return true
            }
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }
}
