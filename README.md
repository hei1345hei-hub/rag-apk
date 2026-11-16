# 基于 RAG 的 Java 面试系统 · 移动端 (Android APK)

本工程为原 Web 面试系统的 Android 客户端。整体架构为 **Vue 3 + Vite 构建的单文件 H5**，运行在自研的**原生 WebView 壳**内（Kotlin），由 `build_apk_final.py` 一键编译打包 APK。原有 Java 后端微服务网关架构未做任何修改。

当前版本：**v1.0.4（versionCode 104）**

---

## 一、APK 下载

前往 [GitHub Releases](https://github.com/hei1345hei-hub/rag-apk/releases) 下载最新安装包：

| 版本 | 文件 | 说明 |
|---|---|---|
| v1.0.4 | `rag-interview-1.0.4.apk` | 液态玻璃导航 + 热更新支持 |

直接安装到 Android 手机（Android 5.0+）即可使用。

---

## 二、系统特性

### 1. 液态玻璃底部导航（核心特性）

参照 [Kyant0/AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) 与 [SukiSU-Ultra](https://github.com/ShirkNeko/SukiSU-Ultra) 的 FloatingBottomBar 实现的三层玻璃导航：

- **录制层**：页面内容固定于视口、内部滚动，保证折射位移图坐标与导航真实位置对齐；
- **玻璃层**：`backdrop-filter: blur(4px) saturate(155%)` 做 vibrancy + blur，SVG `feDisplacementMap` 滤镜对胶囊边缘做真实镜头折射与色散；
- **指示器层**：56dp 高光玻璃胶囊，支持**按住拖动**、松手吸附最近 Tab，按压时带果冻形变与内阴影；
- 图标层永远在折射之上，保证可读性。

### 2. 完整功能移植

- 📊 **总览大盘 (Dashboard)**：文档量、切片量、题库量、面试场次、平均分、分类分布图、最近面试记录。
- 💬 **智能问答 (Chat / RAG)**：RAG 知识库问答（原文切片引用、相似度评分）与 AI 日常对话双模式，多会话管理。
- 🎯 **模拟面试考场 (Interview Room)**：定制岗位、难度、方向与轮数；AI 面试官动态深度追问与多维即时评分。
- 📈 **能力评估报告 (Report)**：环形总分、纯 SVG 动态雷达图、优势短板诊断、逐题答卷明细。
- 📚 **题库自测 (Question Bank)**：分类滑动检索、难度筛选、随机抽题卡片自测、自定义题目录入。
- 📁 **文档知识库 (Knowledge)**：文档状态监控、切片预览、重新解析、本地上传（`.md` / `.pdf`）。
- ⚙️ **个人中心 (User)**：管理员/普通用户切换、修改密码、App 内动态切换与测试后端 IP。

---

## 三、构建方式

### 前端：Vite 单文件构建

```bash
npx vite build
```

产物为 `dist/index.html` —— 所有 JS/CSS 内联的**单文件页面**（约 380KB），由原生壳在 `shouldInterceptRequest` 中本地返回，实现同源加载、无 CORS 限制。

### 原生壳与打包：build_apk_final.py

无需 Gradle / Android Studio，脚本直接调用 Android SDK 工具链（aapt2 / d8 / zipalign / apksigner）编译 Kotlin 源码并签名打包：

```bash
python build_apk_final.py
```

- 包名：`com.rag.interview`，minSdk 21 / targetSdk 34
- 签名：固定 keystore（`keystore/rag-debug.keystore`），保证覆盖安装不换签名
- 产物：`dist/Java面试系统.apk`

### 热更新（免重装 APK）

APK 内置 WebView 每次启动优先加载 `/sdcard/Android/data/com.rag.interview/files/hot/index.html`（不存在时回退 APK 内置资源）：

```bash
adb push dist/index.html /sdcard/Android/data/com.rag.interview/files/hot/index.html
adb shell am force-stop com.rag.interview
adb shell am start -n com.rag.interview/.MainActivity
```

纯前端改动无需重新打包 APK，走热更新即可。

---

## 四、云端连接

服务器 `47.98.190.255` 容器架构：

- **`rag-nginx`（对外端口 81）**：反向代理网关（阿里云安全组已开放 81）
  - `http://47.98.190.255:81/api/` → 内部 `rag-backend:8081/`
  - `http://47.98.190.255:81/ai/` → 大模型接口
- **`rag-backend`（8081）**：内部容器，由 nginx 代理，无需额外开放端口

APK 默认连接 `http://47.98.190.255:81/api`，4G/5G 与任意 Wi-Fi 下均可直连。

---

## 五、工程目录结构

```text
apk前端/
├── android/                  # 原生 WebView 壳工程 (Kotlin)
│   └── app/src/main/java/com/rag/interview/
│       ├── MainActivity.kt           # WebView 宿主：edge-to-edge、120Hz、热更新
│       ├── AppWebViewClient.kt       # 同源拦截 (/app) + 热文件优先 + file:// 回退
│       ├── AppChromeClient.kt        # ChromeClient
│       └── LiquidGlassDockView.kt    # 原生液态玻璃导航 View (实验)
├── api/                      # 接口请求层（对接 Java 后端网关）
│   ├── ai.js                 # 大模型直连对话
│   ├── chat.js               # 智能问答与会话
│   ├── dashboard.js          # 数据总览看板
│   ├── interview.js          # 模拟面试与报告
│   ├── knowledge.js          # 知识文档与切片
│   ├── question.js           # 题库与随机抽取
│   └── user.js               # 用户登录与信息
├── components/               # 通用移动端组件
│   ├── AppModal.vue          # 自定义移动端弹窗
│   ├── EmptyState.vue        # 空状态占位
│   └── MarkdownView.vue      # Markdown 与代码块解析
├── pages/                    # 页面组件
│   ├── dashboard/            # [Tab] 系统总览大盘
│   ├── chat/                 # [Tab] 智能问答 (RAG & 日常对话)
│   ├── interview/            # [Tab] 模拟面试（列表 / room.vue 考场 / report.vue 报告）
│   ├── question/             # [Tab] 题库训练与随机自测
│   ├── knowledge/            # 知识文档库与切片预览
│   ├── user/                 # [Tab] 个人中心与服务端配置
│   └── login/                # 登录与注册
├── static/                   # 静态资源与 TabBar 图标
├── store/                    # Pinia 状态管理 (鉴权 Token)
├── styles/global.scss        # 墨色工程手册移动端样式规范
├── utils/                    # config.js / request.js / toast.js / uni-shim.js
├── dist/                     # 构建产物
│   ├── index.html            # 单文件 H5（热更新载体）
│   └── Java面试系统.apk       # 打包产物
├── keystore/rag-debug.keystore  # 固定签名证书
├── App.vue                   # 根组件（液态玻璃导航实现所在）
├── build_apk_final.py        # 一键打包脚本 (aapt2/d8/apksigner)
├── vite.config.js            # Vite 单文件构建配置
└── pages.json                # 路由声明
```

---

## 六、本地开发调试

```bash
npm install        # 安装依赖
npx vite           # 本地 dev server，浏览器实时预览
```

真机调试可用 `adb logcat | findstr rag` 查看 WebView 控制台输出。
