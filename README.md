# 基于 RAG 的 Java 面试系统 · 移动端 (Android APK)

本工程为原 Web 面试系统的 Android 客户端。包含最新 **v2.0 纯原生架构 (Kotlin Multiplatform + Jetpack Compose + Kyant Backdrop 原生 AGSL 着色器)** 以及历史 **v1.0 混合架构 (Vue 3 + 原生 WebView 壳)**。原有 Java 后端微服务网关架构未做任何修改。

当前推荐版本：**v2.0.0（纯原生 Compose + 液态玻璃 + 完整知识库文档解析与切片）**

---

## 一、APK 直接下载与安装

可以直接前往 [GitHub Releases v2.0.0](https://github.com/hei1345hei-hub/rag-apk/releases/tag/v2.0.0) 下载，或使用仓库中已编译签名的安装包：

| 下载入口 | 文件名 / 说明 | 架构 |
|---|---|---|
| [GitHub Releases v2.0.0](https://github.com/hei1345hei-hub/rag-apk/releases/tag/v2.0.0) | `Java-Interview-System-v2.0.0.apk` (12.6MB) | **v2.0 纯原生 Jetpack Compose + Kyant 液态玻璃** |
| [`Java面试系统.apk`](./Java面试系统.apk) | 仓库直链下载 (12.6MB) | 同上 |
| [`dist/Java面试系统.apk`](./dist/Java面试系统.apk) | 镜像分发版 | 同上 |

直接安装到 Android 手机（Android 8.0+，支持 Android 13/14 120Hz 高刷）即可即开即用。

---

## 二、系统特性 (v2.0 纯原生版)

### 1. 极致 Kyant 液态玻璃底部导航（AGSL 硬件着色器）
* **真·物理折射与色散**：基于 Google Android 13+ AGSL 原生 RuntimeShader，动态模拟物理凹凸透镜光路折射（Lens Refraction）与色散（Chromatic Aberration）。
* **零延迟双向联动**：采用 `HorizontalPager` 与 `targetPage` 实时预测落点同步算法，手指左右划动翻页瞬间底部滑块同频滑行，到达目标页毫无滞后与顿挫。
* **暖黄琥珀高光**：针对 Java 面试系统专属调校的 `#E59819` 琥珀暖黄果冻质感，支持随手指轻甩按压形变与双层柔光阴影。

### 2. 对标 PC 网页端全量功能移植
* 📊 **总览大盘 (Dashboard)**：文档量、切片量、题库量、面试场次、平均分、分类雷达与近期面试卡片。
* 💬 **智能问答 (Chat / RAG)**：知识库混合检索问答（带高亮命中切片引用与置信度评分）与 AI 日常会话多会话切换。
* 🎯 **模拟面试考场 (Interview Room)**：自定义岗位、难度、技术栈与轮数；AI 考官实时深度追问与多维即时评分。
* 📈 **能力评估报告 (Report)**：雷达能力多边形、优劣势诊断、逐题答卷明细与建议。
* 📚 **题库自测 (Question Bank)**：多维度分类与难度检索、抽题自测卡片、录入自定义题目。
* 📁 **文档知识库 (Knowledge)**：全面对标网页端知识库，包含分类统计、文档列表监控、切片预览弹窗（Chunk Inspection）、重新解析（Reparse）、文档删除、本地文档上传（支持 Markdown / 模板文本解析）。
* ⚙️ **个人中心 (User)**：登录/登出状态管理、候选人档案、动态测试与切换云端后端服务地址。

---

## 三、工程结构与构建方式

```text
rag-apk/
├── Java面试系统.apk          # 最新编译完成的 12.6MB 纯原生安装包
├── apk-kotlin/               # ★ v2.0 纯原生 Kotlin Multiplatform + Compose 工程
│   ├── androidApp/           # Android 宿主与清单配置
│   ├── app/                  # 纯原生业务代码 (InterviewApp, KnowledgeScreen, Chat, etc.)
│   └── backdrop/             # Kyant Backdrop 纯原生 AGSL 液态玻璃着色器模块
├── APK_FRONTEND_DEV_RECORD.md # 原生重构与功能演进技术日志
├── dist/                     # 分发产物目录
│   └── Java面试系统.apk
└── (android, pages, etc.)    # v1.0 历史混合架构源码归档
```

### 纯原生构建 (推荐)

进入 `apk-kotlin` 目录直接使用 Gradle 编译：

```bash
cd apk-kotlin
.\gradlew.bat :androidApp:assembleDebug
```

编译产物位于 `apk-kotlin/androidApp/build/outputs/apk/debug/androidApp-debug.apk`。

---

## 四、云端连接配置

默认自动连接阿里云服务器公网网关：`http://47.98.190.255:81/api`，4G/5G 与任意 Wi-Fi 下均可直连。如需切换本地联调，可在 App 内登录页或个人中心直接无感切换。

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
