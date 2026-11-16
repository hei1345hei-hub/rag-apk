import os
import sys
import shutil
import subprocess
import math
import zipfile
from PIL import Image, ImageDraw, ImageFont

BUILD_DIR = r"C:\Users\29759\AppData\Local\Temp\apk_build"
ANDROID_SDK = r"D:\Android\Sdk"
AAPT2 = os.path.join(ANDROID_SDK, r"build-tools\34.0.0\aapt2.exe")
D8 = os.path.join(ANDROID_SDK, r"build-tools\34.0.0\d8.bat")
ZIPALIGN = os.path.join(ANDROID_SDK, r"build-tools\34.0.0\zipalign.exe")
APKSIGNER = os.path.join(ANDROID_SDK, r"build-tools\34.0.0\apksigner.bat")
ANDROID_JAR = os.path.join(ANDROID_SDK, r"platforms\android-34\android.jar")
JAVAC = r"D:\java\JDK\bin\javac.exe"
JAR = r"D:\java\JDK\bin\jar.exe"
KEYTOOL = r"D:\java\JDK\bin\keytool.exe"

PROJECT_ROOT = r"d:\java\------项目------\基于RAG的Java面试系统"
DIST_DIR = os.path.join(PROJECT_ROOT, r"apk前端\dist")
# 签名文件固定放在工程目录中，避免每次构建生成新证书导致覆盖安装失败
KEYSTORE_DIR = os.path.join(PROJECT_ROOT, r"apk前端\keystore")
KEYSTORE_PATH = os.path.join(KEYSTORE_DIR, "rag-debug.keystore")

def run(cmd, cwd=None):
    print("RUN:", cmd if isinstance(cmd, str) else " ".join(cmd))
    res = subprocess.run(cmd, cwd=cwd, shell=isinstance(cmd, str), capture_output=True, text=True, encoding='utf-8', errors='replace')
    if res.returncode != 0:
        print("ERROR stdout:", res.stdout)
        print("ERROR stderr:", res.stderr)
        raise RuntimeError(f"Command failed with code {res.returncode}")
    return res.stdout

def clean_and_prepare_dirs():
    if os.path.exists(BUILD_DIR):
        shutil.rmtree(BUILD_DIR, ignore_errors=True)
    os.makedirs(os.path.join(BUILD_DIR, "res", "values"), exist_ok=True)
    os.makedirs(os.path.join(BUILD_DIR, "res", "xml"), exist_ok=True)
    os.makedirs(os.path.join(BUILD_DIR, "res", "mipmap-anydpi-v26"), exist_ok=True)
    os.makedirs(os.path.join(BUILD_DIR, "gen"), exist_ok=True)
    os.makedirs(os.path.join(BUILD_DIR, "classes"), exist_ok=True)
    os.makedirs(os.path.join(BUILD_DIR, "assets", "www"), exist_ok=True)
    os.makedirs(os.path.join(BUILD_DIR, "src", "com", "rag", "interview"), exist_ok=True)

def generate_icons():
    print("Generating modern adaptive & legacy launcher icons...")
    font_bold = ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf', 72)
    font_badge = ImageFont.truetype('C:/Windows/Fonts/msyhbd.ttc', 50)
    
    # 1. Adaptive foreground (1080x1080 canvas, 720px safe circle)
    fg = Image.new('RGBA', (1080, 1080), (0, 0, 0, 0))
    f_draw = ImageDraw.Draw(fg)
    
    # Glow behind cup
    for r in range(320, 0, -10):
        a = int(32 * (1 - r / 320))
        f_draw.ellipse([540 - r, 540 - r, 540 + r, 540 + r], fill=(245, 158, 11, a))
        
    # Outer Tech Rings
    f_draw.ellipse([270, 270, 810, 810], outline=(245, 158, 11, 170), width=8)
    f_draw.ellipse([290, 290, 790, 790], outline=(196, 67, 43, 140), width=4)
    
    # Cup body
    cup_points = [
        (380, 480),
        (700, 480),
        (665, 690),
        (415, 690)
    ]
    f_draw.polygon(cup_points, fill=(196, 67, 43, 255))
    f_draw.arc([630, 510, 750, 650], start=-90, end=90, fill=(245, 158, 11, 255), width=24)
    f_draw.ellipse([380, 455, 700, 505], fill=(224, 76, 50, 255), outline=(245, 158, 11, 255), width=8)
    
    # Rising steam curves
    for t in range(0, 100):
        y = 440 - t * 1.8
        x1 = 540 + math.sin(t * 0.08) * 32
        f_draw.ellipse([x1 - 10, y - 10, x1 + 10, y + 10], fill=(245, 158, 11, int(255 * (1 - t/110))))
        x2 = 470 + math.sin(t * 0.09 + 1.2) * 26
        f_draw.ellipse([x2 - 8, y - 8, x2 + 8, y + 8], fill=(251, 191, 36, int(220 * (1 - t/105))))
        x3 = 610 + math.sin(t * 0.09 - 1.2) * 26
        f_draw.ellipse([x3 - 8, y - 8, x3 + 8, y + 8], fill=(251, 191, 36, int(220 * (1 - t/105))))
        
    f_draw.text((440, 550), 'JAVA', font=font_bold, fill=(255, 255, 255, 255))
    
    # Badge ribbon: 'AI 面试'
    f_draw.rounded_rectangle([385, 725, 695, 805], radius=40, fill=(15, 23, 42, 245), outline=(245, 158, 11, 240), width=6)
    f_draw.text((430, 736), 'AI 面试', font=font_badge, fill=(251, 191, 36, 255))
    
    # 2. Master Composite for legacy launcher
    bg = Image.new('RGBA', (1080, 1080), (15, 23, 42, 255))
    bg_draw = ImageDraw.Draw(bg)
    for r in range(540, 0, -15):
        a = int(48 * (1 - r / 540))
        bg_draw.ellipse([540 - r, 540 - r, 540 + r, 540 + r], fill=(30, 58, 138, a))
        
    full_icon = Image.alpha_composite(bg, fg)
    
    squircle_mask = Image.new('L', (1080, 1080), 0)
    s_draw = ImageDraw.Draw(squircle_mask)
    s_draw.rounded_rectangle([40, 40, 1040, 1040], radius=220, fill=255)
    
    round_mask = Image.new('L', (1080, 1080), 0)
    r_draw = ImageDraw.Draw(round_mask)
    r_draw.ellipse([40, 40, 1040, 1040], fill=255)
    
    standard_icon = Image.new('RGBA', (1080, 1080), (0, 0, 0, 0))
    standard_icon.paste(full_icon, (0, 0), squircle_mask)
    
    round_icon = Image.new('RGBA', (1080, 1080), (0, 0, 0, 0))
    round_icon.paste(full_icon, (0, 0), round_mask)
    
    sizes = {
        'mdpi': 48,
        'hdpi': 72,
        'xhdpi': 96,
        'xxhdpi': 144,
        'xxxhdpi': 192
    }
    
    for name, s in sizes.items():
        folder = os.path.join(BUILD_DIR, "res", f'mipmap-{name}')
        os.makedirs(folder, exist_ok=True)
        std_resized = standard_icon.resize((s, s), Image.Resampling.LANCZOS)
        std_resized.save(os.path.join(folder, 'ic_launcher.png'), 'PNG')
        rnd_resized = round_icon.resize((s, s), Image.Resampling.LANCZOS)
        rnd_resized.save(os.path.join(folder, 'ic_launcher_round.png'), 'PNG')
        fg_s = int(s * 108 / 48)
        fg_resized = fg.resize((fg_s, fg_s), Image.Resampling.LANCZOS)
        fg_resized.save(os.path.join(folder, 'ic_launcher_foreground.png'), 'PNG')

def create_xml_resources():
    print("Writing XML resources (Strings, Colors, Styles, Network Security Config)...")
    strings_xml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Java面试系统</string>
</resources>
"""
    with open(os.path.join(BUILD_DIR, "res", "values", "strings.xml"), "w", encoding="utf-8") as f:
        f.write(strings_xml)

    colors_xml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="app_background">#FFF4F1EA</color>
    <color name="ic_launcher_background">#FF0F172A</color>
</resources>
"""
    with open(os.path.join(BUILD_DIR, "res", "values", "colors.xml"), "w", encoding="utf-8") as f:
        f.write(colors_xml)

    styles_xml = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="AppTheme" parent="@android:style/Theme.Light.NoTitleBar">
        <item name="android:windowBackground">@color/app_background</item>
        <item name="android:colorBackground">@color/app_background</item>
        <item name="android:windowNoTitle">true</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowDrawsSystemBarBackgrounds">true</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:windowLightNavigationBar">true</item>
    </style>
</resources>
"""
    with open(os.path.join(BUILD_DIR, "res", "values", "styles.xml"), "w", encoding="utf-8") as f:
        f.write(styles_xml)

    adaptive_xml = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background"/>
    <foreground android:drawable="@mipmap/ic_launcher_foreground"/>
</adaptive-icon>
"""
    with open(os.path.join(BUILD_DIR, "res", "mipmap-anydpi-v26", "ic_launcher.xml"), "w", encoding="utf-8") as f:
        f.write(adaptive_xml)
    with open(os.path.join(BUILD_DIR, "res", "mipmap-anydpi-v26", "ic_launcher_round.xml"), "w", encoding="utf-8") as f:
        f.write(adaptive_xml)

    network_config = """<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true">
        <trust-anchors>
            <certificates src="system" />
            <certificates src="user" />
        </trust-anchors>
    </base-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">127.0.0.1</domain>
        <domain includeSubdomains="true">47.98.190.255</domain>
    </domain-config>
</network-security-config>
"""
    with open(os.path.join(BUILD_DIR, "res", "xml", "network_security_config.xml"), "w", encoding="utf-8") as f:
        f.write(network_config)

def copy_assets():
    print("Copying mobile frontend assets into APK build assets...")
    dest_www = os.path.join(BUILD_DIR, "assets", "www")
    for root, dirs, files in os.walk(DIST_DIR):
        rel = os.path.relpath(root, DIST_DIR)
        target_dir = dest_www if rel == '.' else os.path.join(dest_www, rel)
        os.makedirs(target_dir, exist_ok=True)
        for file in files:
            if file.lower().endswith(('.apk', '.map')):
                continue
            src_file = os.path.join(root, file)
            dst_file = os.path.join(target_dir, file)
            shutil.copy2(src_file, dst_file)
            
    # Also copy into assets directly
    dest_root = os.path.join(BUILD_DIR, "assets")
    for root, dirs, files in os.walk(DIST_DIR):
        rel = os.path.relpath(root, DIST_DIR)
        target_dir = dest_root if rel == '.' else os.path.join(dest_root, rel)
        os.makedirs(target_dir, exist_ok=True)
        for file in files:
            if file.lower().endswith(('.apk', '.map')):
                continue
            src_file = os.path.join(root, file)
            dst_file = os.path.join(target_dir, file)
            if not os.path.exists(dst_file):
                shutil.copy2(src_file, dst_file)

def create_manifest():
    print("Writing AndroidManifest.xml (Target SDK 34 / Android 14)...")
    manifest = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.rag.interview"
    android:versionCode="104"
    android:versionName="1.0.4">

    <uses-sdk
        android:minSdkVersion="21"
        android:targetSdkVersion="34" />

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />

    <application
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:networkSecurityConfig="@xml/network_security_config"
        android:usesCleartextTraffic="true"
        android:hardwareAccelerated="true"
        android:largeHeap="true"
        android:theme="@style/AppTheme">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:configChanges="orientation|keyboardHidden|screenSize|uiMode"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""
    with open(os.path.join(BUILD_DIR, "AndroidManifest.xml"), "w", encoding="utf-8") as f:
        f.write(manifest)

def create_java_sources():
    print("Writing Java sources with dual interceptors, high refresh rate, and error handling...")
    main_java = """package com.rag.interview;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String TAG_RATE = "RefreshRate";
    private static final String TAG_HOT = "HotUpdate";
    private WebView webView;
    private AppChromeClient chromeClient;
    private long exitTime = 0;
    private float safeTopDp = 24f;
    private float safeBottomDp = 24f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 1. 锁频 120Hz / 144Hz 超高刷新率
        enableHighRefreshRate();

        // 2. 沉浸式全面屏：内容绘制到系统栏下方
        setupEdgeToEdgeImmersion();

        // 3. 根容器使用墨色宣纸风底色 #F4F1EA
        FrameLayout rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(0xFFF4F1EA);
        rootLayout.setFitsSystemWindows(false);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFFF4F1EA);
        webView.setFitsSystemWindows(false);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        rootLayout.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(rootLayout);

        initWebView();

        // 沉浸式安全区：系统 Insets + 资源尺寸兜底，页面加载完成后再注入一次
        listenWindowInsets();

        // 开启调试模式以方便排查
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // 使用与后端同源的安全上下文加载，页面资源由本地拦截提供，
        // 但文档 origin 与后端一致，API 请求属于同源请求，不会被 CORS 拦截
        webView.loadUrl(AppWebViewClient.ASSET_ORIGIN + "/app/index.html");

        // 启动时检查前端热更新（不阻塞界面）
        startHotUpdateCheck();
    }

    /**
     * 前端热更新：
     * 1) 本地热更新文件：/sdcard/Android/data/com.rag.interview/files/hot/index.html
     *    可用 adb push 直接替换，重启 App 即生效，无需重装 APK；
     * 2) 启动时尝试从 ASSET_ORIGIN/app/index.html 下载新版本，
     *    下载成功后自动 reload，当前会话即可看到新界面。
     * 仅替换前端；原生（Java / 权限 / 图标）改动仍需重新打包。
     */
    private void startHotUpdateCheck() {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                File dir = new File(getExternalFilesDir(null), "hot");
                if (!dir.exists() && !dir.mkdirs()) {
                    Log.d(TAG_HOT, "cannot create hot dir");
                    return;
                }
                URL url = new URL(AppWebViewClient.ASSET_ORIGIN + "/app/index.html");
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(8000);
                conn.setRequestProperty("Accept", "text/html");
                conn.setRequestProperty("Cache-Control", "no-cache");

                if (conn.getResponseCode() != 200) {
                    Log.d(TAG_HOT, "no hot update on server: HTTP " + conn.getResponseCode());
                    return;
                }
                String type = conn.getContentType();
                if (type == null || !type.toLowerCase().contains("html")) {
                    Log.d(TAG_HOT, "skip: not html, content-type=" + type);
                    return;
                }

                File tmp = new File(dir, "index.html.tmp");
                InputStream in = conn.getInputStream();
                FileOutputStream out = new FileOutputStream(tmp);
                byte[] buffer = new byte[8192];
                int len;
                long total = 0;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    total += len;
                    if (total > 8L * 1024 * 1024) {
                        break;
                    }
                }
                out.close();
                in.close();

                // 简单校验，避免把错误页缓存成前端
                if (total > 100 * 1024) {
                    File target = new File(dir, "index.html");
                    if (target.exists() && !target.delete()) {
                        tmp.delete();
                        return;
                    }
                    if (tmp.renameTo(target)) {
                        Log.i(TAG_HOT, "frontend hot update downloaded: " + total + " bytes");
                        if (webView != null) {
                            webView.post(() -> webView.reload());
                        }
                    }
                } else {
                    tmp.delete();
                    Log.d(TAG_HOT, "skip: downloaded content too small (" + total + " bytes)");
                }
            } catch (Exception e) {
                Log.d(TAG_HOT, "hot update skipped: " + e.getMessage());
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 部分机型息屏 / 切后台后会重置刷新率，回到前台重新申请高刷
        enableHighRefreshRate();
        // 页面若发生重载，回到前台后补注入一次安全区
        webView.postDelayed(this::injectSafeArea, 300);
    }

    private void enableHighRefreshRate() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        try {
            Display display = getWindowManager().getDefaultDisplay();
            Display.Mode currentMode = display.getMode();
            Display.Mode bestMode = pickHighestRefreshMode(display, currentMode);
            float bestRate = bestMode.getRefreshRate();

            WindowManager.LayoutParams lp = getWindow().getAttributes();
            if (bestMode.getModeId() != currentMode.getModeId()) {
                lp.preferredDisplayModeId = bestMode.getModeId();
            }
            // API 23 起可用的公开字段，部分厂商（小米 / 三星等）需要它才会切高刷
            lp.preferredRefreshRate = bestRate;

            // Android 11+ 的 @hide 字段，运行时可通过反射拿到；锁定刷新率区间
            try {
                Field minField = WindowManager.LayoutParams.class.getField("preferredMinDisplayRefreshRate");
                Field maxField = WindowManager.LayoutParams.class.getField("preferredMaxDisplayRefreshRate");
                minField.setFloat(lp, bestRate);
                maxField.setFloat(lp, bestRate);
            } catch (Exception ignored) {}

            getWindow().setAttributes(lp);
            Log.i(TAG_RATE, "display mode: " + currentMode.getRefreshRate() + "Hz -> "
                    + bestRate + "Hz (" + bestMode.getPhysicalWidth() + "x" + bestMode.getPhysicalHeight() + ")");
        } catch (Exception e) {
            Log.w(TAG_RATE, "enableHighRefreshRate failed: " + e.getMessage());
        }
    }

    /**
     * 挑选刷新率最高的显示模式，但优先保持当前分辨率，
     * 避免为了 120/144Hz 把屏幕降到更低分辨率导致界面变糊。
     */
    private Display.Mode pickHighestRefreshMode(Display display, Display.Mode currentMode) {
        Display.Mode bestMode = currentMode;
        float bestRate = currentMode.getRefreshRate();
        for (Display.Mode mode : display.getSupportedModes()) {
            if (mode.getPhysicalWidth() != currentMode.getPhysicalWidth()
                    || mode.getPhysicalHeight() != currentMode.getPhysicalHeight()) {
                continue;
            }
            if (mode.getRefreshRate() > bestRate + 0.01f) {
                bestMode = mode;
                bestRate = mode.getRefreshRate();
            }
        }
        return bestMode;
    }

    /**
     * 沉浸式系统栏：状态栏 / 导航栏透明，内容绘制到系统栏下方，
     * 具体留白由 listenWindowInsets() 注入 CSS 变量完成。
     */
    private void setupEdgeToEdgeImmersion() {
        Window window = getWindow();
        window.setBackgroundDrawable(new ColorDrawable(0xFFF4F1EA));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                    | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Method method = Window.class.getMethod("setDecorFitsSystemWindows", boolean.class);
                method.invoke(window, false);
            } catch (Exception ignored) {}
        }

        View decor = window.getDecorView();
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                  | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                  | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        decor.setSystemUiVisibility(flags);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                WindowManager.LayoutParams lp = window.getAttributes();
                Field cutoutField = WindowManager.LayoutParams.class.getField("layoutInDisplayCutoutMode");
                cutoutField.setInt(lp, 1);
                window.setAttributes(lp);
            } catch (Exception ignored) {}
        }
    }

    /**
     * 监听真实的系统栏高度并写入页面 CSS 变量。
     * 必须在 setContentView 之后注册，同时对 insets 为 0 的机型用资源尺寸兜底。
     */
    private void listenWindowInsets() {
        final View decor = getWindow().getDecorView();
        decor.setOnApplyWindowInsetsListener((v, insets) -> {
            applyInsets(insets);
            return insets;
        });
        decor.requestApplyInsets();
        decor.post(() -> decor.requestApplyInsets());
        // 部分机型首次 dispatch 时机较晚，再兜底刷新一次
        decor.postDelayed(() -> {
            applyFallbackInsets();
            injectSafeArea();
        }, 600);
    }

    private void applyInsets(WindowInsets insets) {
        int top;
        int bottom;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.graphics.Insets bars = insets.getInsets(
                    WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
            top = bars.top;
            bottom = bars.bottom;
        } else {
            top = insets.getSystemWindowInsetTop();
            bottom = insets.getSystemWindowInsetBottom();
        }
        if (top <= 0) {
            top = getSystemBarHeight("status_bar_height");
        }
        if (bottom <= 0) {
            bottom = getSystemBarHeight("navigation_bar_height");
        }
        float density = getResources().getDisplayMetrics().density;
        if (density <= 0) {
            density = 1f;
        }
        safeTopDp = top / density;
        safeBottomDp = bottom / density;
        Log.d(TAG_HOT, "insets top=" + safeTopDp + "dp bottom=" + safeBottomDp + "dp");
        injectSafeArea();
    }

    private void applyFallbackInsets() {
        float density = getResources().getDisplayMetrics().density;
        if (density <= 0) {
            density = 1f;
        }
        if (safeTopDp <= 0) {
            safeTopDp = getSystemBarHeight("status_bar_height") / density;
        }
        if (safeBottomDp <= 0) {
            safeBottomDp = getSystemBarHeight("navigation_bar_height") / density;
        }
    }

    private int getSystemBarHeight(String name) {
        int id = getResources().getIdentifier(name, "dimen", "android");
        if (id > 0) {
            return getResources().getDimensionPixelSize(id);
        }
        return 0;
    }

    /**
     * 把安全区写入页面 CSS 变量（--safe-top / --safe-bottom）
     */
    private void injectSafeArea() {
        if (webView == null) {
            return;
        }
        String js = "(function(){try{if(document&&document.documentElement&&document.documentElement.style){"
                  + "document.documentElement.style.setProperty('--safe-top','" + safeTopDp + "px');"
                  + "document.documentElement.style.setProperty('--safe-bottom','" + safeBottomDp + "px');"
                  + "}}catch(e){}})();";
        webView.post(() -> webView.evaluateJavascript(js, null));
    }

    private void initWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setBlockNetworkImage(false);
        s.setBlockNetworkLoads(false);
        s.setLoadsImagesAutomatically(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

        chromeClient = new AppChromeClient(this);
        webView.setWebViewClient(new AppWebViewClient(this, this::injectSafeArea));
        webView.setWebChromeClient(chromeClient);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10001 && chromeClient != null) {
            chromeClient.handleResult(resultCode, data);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (webView.canGoBack()) {
                webView.goBack();
                return true;
            } else {
                if (System.currentTimeMillis() - exitTime > 2000) {
                    Toast.makeText(this, "再按一次退出 Java面试系统", Toast.LENGTH_SHORT).show();
                    exitTime = System.currentTimeMillis();
                    return true;
                } else {
                    finish();
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
"""
    with open(os.path.join(BUILD_DIR, "src", "com", "rag", "interview", "MainActivity.java"), "w", encoding="utf-8") as f:
        f.write(main_java)

    client_java = """package com.rag.interview;

import android.content.Context;
import android.net.Uri;
import android.net.http.SslError;
import android.util.Log;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class AppWebViewClient extends WebViewClient {
    private static final String TAG = "AppWebViewClient";
    // 与页面 API 保持同源，避免云端后端未配置 CORS 时请求被浏览器拦截
    public static final String ASSET_HOST = "47.98.190.255";
    public static final String ASSET_ORIGIN = "http://" + ASSET_HOST + ":81";
    private static final String ASSET_PATH_PREFIX = "/app/";
    private Context context;
    private final Runnable onPageFinished;
    private boolean fallbackUsed = false;

    public AppWebViewClient(Context context, Runnable onPageFinished) {
        this.context = context;
        this.onPageFinished = onPageFinished;
    }

    @Override
    public void onPageFinished(WebView view, String url) {
        super.onPageFinished(view, url);
        if (onPageFinished != null) {
            onPageFinished.run();
        }
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
        return false;
    }

    @Override
    public boolean shouldOverrideUrlLoading(WebView view, String url) {
        return false;
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
        if (request != null && request.getUrl() != null) {
            WebResourceResponse res = handleIntercept(request.getUrl());
            if (res != null) {
                return res;
            }
        }
        return super.shouldInterceptRequest(view, request);
    }

    @Override
    public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
        if (url != null) {
            try {
                Uri uri = Uri.parse(url);
                WebResourceResponse res = handleIntercept(uri);
                if (res != null) {
                    return res;
                }
            } catch (Exception ignored) {}
        }
        return super.shouldInterceptRequest(view, url);
    }

    private WebResourceResponse handleIntercept(Uri uri) {
        String host = uri.getHost();
        if (host == null) {
            return null;
        }

        boolean localHost = host.equals("localhost") || host.equals("127.0.0.1")
                || host.equalsIgnoreCase("appassets.androidplatform.net");
        boolean assetOrigin = ASSET_HOST.equals(host);

        if (!localHost && !assetOrigin) {
            return null;
        }

        String path = uri.getPath();
        if (assetOrigin) {
            // 只接管 /app/** 的页面资源，/api/** 继续走真实网络（同源，无 CORS）
            if (path == null || !path.startsWith(ASSET_PATH_PREFIX)) {
                return null;
            }
            path = "/" + path.substring(ASSET_PATH_PREFIX.length());
        }

        if (path == null || path.isEmpty() || path.equals("/")) {
            path = "/index.html";
        }
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        InputStream is = null;
        if ("index.html".equals(path)) {
            is = openIndexHtml();
        } else {
            try {
                is = context.getAssets().open("www/" + path);
            } catch (Exception e1) {
                try {
                    is = context.getAssets().open(path);
                } catch (Exception ignored) {}
            }
        }

        if (is != null) {
            String mime = getMimeType(path);
            // 二进制流（图片、字体）编码必须为 null，文本流为 UTF-8
            String encoding = "UTF-8";
            if (mime.startsWith("image/") || mime.startsWith("font/") || mime.equals("application/octet-stream")) {
                encoding = null;
            }
            WebResourceResponse response = new WebResourceResponse(mime, encoding, is);
            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");
            headers.put("Access-Control-Allow-Methods", "GET, POST, OPTIONS, HEAD");
            headers.put("Access-Control-Allow-Headers", "*");
            response.setResponseHeaders(headers);
            return response;
        }

        // 单页路由 fallback
        if (!path.contains(".")) {
            try {
                InputStream spaStream = openIndexHtml();
                if (spaStream == null) {
                    return null;
                }
                WebResourceResponse response = new WebResourceResponse("text/html", "UTF-8", spaStream);
                Map<String, String> headers = new HashMap<>();
                headers.put("Access-Control-Allow-Origin", "*");
                response.setResponseHeaders(headers);
                return response;
            } catch (Exception ignored) {}
        }
        return null;
    }

    /**
     * 页面来源优先级：热更新文件 > APK 内置页面
     */
    private InputStream openIndexHtml() {
        try {
            File hot = new File(context.getExternalFilesDir(null), "hot/index.html");
            if (hot.exists() && hot.length() > 1024) {
                Log.i(TAG, "using hot update page: " + hot.getAbsolutePath());
                return new FileInputStream(hot);
            }
        } catch (Exception ignored) {}
        try {
            return context.getAssets().open("www/index.html");
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".html") || lower.endsWith(".htm")) return "text/html";
        if (lower.endsWith(".js") || lower.endsWith(".mjs")) return "application/javascript";
        if (lower.endsWith(".css")) return "text/css";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".svg")) return "image/svg+xml";
        if (lower.endsWith(".json")) return "application/json";
        if (lower.endsWith(".ico")) return "image/x-icon";
        if (lower.endsWith(".woff2")) return "font/woff2";
        if (lower.endsWith(".woff")) return "font/woff";
        if (lower.endsWith(".ttf")) return "font/ttf";
        return "application/octet-stream";
    }

    @Override
    public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
        // 允许通过，避免由于自签名或证书异常导致白屏
        handler.proceed();
    }

    @Override
    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
        if (request != null && request.isForMainFrame()) {
            Log.e(TAG, "Main frame error: " + error.getDescription() + " on " + request.getUrl());
            // 资源拦截异常时回退到本地 file:// 资源，避免整页白屏 / 报错页
            if (!fallbackUsed && request.getUrl() != null
                    && (request.getUrl().toString().startsWith("http://localhost")
                        || request.getUrl().toString().startsWith(ASSET_ORIGIN))) {
                fallbackUsed = true;
                view.loadUrl("file:///android_asset/www/index.html");
            }
        }
    }
}
"""
    with open(os.path.join(BUILD_DIR, "src", "com", "rag", "interview", "AppWebViewClient.java"), "w", encoding="utf-8") as f:
        f.write(client_java)

    chrome_java = """package com.rag.interview;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

public class AppChromeClient extends WebChromeClient {
    private static final String TAG = "WebConsole";
    private Activity activity;
    private ValueCallback<Uri[]> uploadMessage;

    public AppChromeClient(Activity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        Log.d(TAG, consoleMessage.message() + " [" + consoleMessage.sourceId() + ":" + consoleMessage.lineNumber() + "]");
        return true;
    }

    @Override
    public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                                     FileChooserParams fileChooserParams) {
        if (uploadMessage != null) {
            uploadMessage.onReceiveValue(null);
            uploadMessage = null;
        }
        uploadMessage = filePathCallback;

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        try {
            activity.startActivityForResult(Intent.createChooser(intent, "选择知识库文档 (.md / .pdf)"), 10001);
        } catch (Exception e) {
            uploadMessage = null;
            Toast.makeText(activity, "无法打开系统文件选择器", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    public void handleResult(int resultCode, Intent data) {
        if (uploadMessage != null) {
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                Uri dataUri = data.getData();
                if (dataUri != null) {
                    results = new Uri[]{dataUri};
                }
            }
            uploadMessage.onReceiveValue(results);
            uploadMessage = null;
        }
    }
}
"""
    with open(os.path.join(BUILD_DIR, "src", "com", "rag", "interview", "AppChromeClient.java"), "w", encoding="utf-8") as f:
        f.write(chrome_java)

def compile_and_package():
    print("Step 1: AAPT2 compile...")
    res_dir = os.path.join(BUILD_DIR, "res")
    res_zip = os.path.join(BUILD_DIR, "res.zip")
    run([AAPT2, "compile", "--dir", res_dir, "-o", res_zip])

    print("Step 2: AAPT2 link...")
    manifest_xml = os.path.join(BUILD_DIR, "AndroidManifest.xml")
    gen_dir = os.path.join(BUILD_DIR, "gen")
    assets_dir = os.path.join(BUILD_DIR, "assets")
    unaligned_apk = os.path.join(BUILD_DIR, "unaligned.apk")
    run([
        AAPT2, "link",
        res_zip,
        "-I", ANDROID_JAR,
        "--manifest", manifest_xml,
        "--java", gen_dir,
        "-A", assets_dir,
        "-o", unaligned_apk
    ])

    print("Step 3: Javac compile...")
    classes_dir = os.path.join(BUILD_DIR, "classes")
    r_java = os.path.join(gen_dir, "com", "rag", "interview", "R.java")
    main_java = os.path.join(BUILD_DIR, "src", "com", "rag", "interview", "MainActivity.java")
    client_java = os.path.join(BUILD_DIR, "src", "com", "rag", "interview", "AppWebViewClient.java")
    chrome_java = os.path.join(BUILD_DIR, "src", "com", "rag", "interview", "AppChromeClient.java")
    run([
        JAVAC,
        "-source", "8",
        "-target", "8",
        "-encoding", "UTF-8",
        "-cp", ANDROID_JAR,
        "-d", classes_dir,
        r_java, main_java, client_java, chrome_java
    ])

    print("Step 4: D8 dex...")
    class_files = []
    for root, dirs, files in os.walk(classes_dir):
        for f in files:
            if f.endswith(".class"):
                class_files.append(os.path.join(root, f))
    run([D8, "--lib", ANDROID_JAR, "--output", BUILD_DIR] + class_files)

    print("Step 5: Pack classes.dex into APK...")
    classes_dex = os.path.join(BUILD_DIR, "classes.dex")
    run([JAR, "-uf", unaligned_apk, "classes.dex"], cwd=BUILD_DIR)

    print("Step 6: Zipalign (4-byte boundary)...")
    aligned_apk = os.path.join(BUILD_DIR, "aligned.apk")
    run([ZIPALIGN, "-f", "4", unaligned_apk, aligned_apk])

    print("Step 7: Sign APK with fixed keystore (v1, v2, v3)...")
    keystore_path = KEYSTORE_PATH
    os.makedirs(KEYSTORE_DIR, exist_ok=True)
    if not os.path.exists(keystore_path):
        run([
            KEYTOOL,
            "-genkeypair",
            "-keystore", keystore_path,
            "-storepass", "android",
            "-alias", "androiddebugkey",
            "-keypass", "android",
            "-keyalg", "RSA",
            "-keysize", "2048",
            "-validity", "10000",
            "-dname", "CN=Android Debug,O=Android,C=US"
        ])
    signed_apk = os.path.join(BUILD_DIR, "signed.apk")
    run([
        APKSIGNER, "sign",
        "--ks", keystore_path,
        "--ks-pass", "pass:android",
        "--ks-key-alias", "androiddebugkey",
        "--key-pass", "pass:android",
        "--v1-signing-enabled", "true",
        "--v2-signing-enabled", "true",
        "--v3-signing-enabled", "true",
        "--out", signed_apk,
        aligned_apk
    ])

    print("Step 8: Verify signature...")
    v_out = run([APKSIGNER, "verify", "--verbose", signed_apk])
    print(v_out)

    print("Step 9: Copy to final locations...")
    target_1 = os.path.join(PROJECT_ROOT, "Java面试系统.apk")
    target_2 = os.path.join(DIST_DIR, "Java面试系统.apk")
    shutil.copy2(signed_apk, target_1)
    shutil.copy2(signed_apk, target_2)
    print("Final APK 1 size:", os.path.getsize(target_1), "bytes at", target_1)
    print("Final APK 2 size:", os.path.getsize(target_2), "bytes at", target_2)

def main():
    print("========================================")
    print("Starting Comprehensive APK Build Process")
    print("========================================")
    clean_and_prepare_dirs()
    generate_icons()
    create_xml_resources()
    copy_assets()
    create_manifest()
    create_java_sources()
    compile_and_package()
    print("========================================")
    print("BUILD SUCCESSFUL!")
    print("========================================")

if __name__ == '__main__':
    main()
