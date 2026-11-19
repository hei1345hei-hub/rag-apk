# 《基于 RAG 的 Java 面试系统》原生 Android 前端开发记录与交付文档

> 本文档整理了本项目移动端从原始 Vue/Uni-app 混合架构全面重构成 **100% 纯 Kotlin + Jetpack Compose 原生应用** 的完整开发脉络、架构设计、核心技术实现与后续扩展建议，方便直接作为上下文输入给其他 AI Agent 使用。

---

## 一、 项目背景与架构重构目标

### 1. 原始架构与痛点
* **原项目路径**：`apk前端/`（基于 Uni-app / Vue 开发的混合 H5 封装应用）。
* **存在痛点**：
  * WebView 渲染开销高，手势跟手性与 120Hz 高刷响应不足；
  * 原有混合页面无法实现细腻的物理微动效（如流体玻璃拟物、弹簧吸附、手势阻尼）；
  * 状态栏与导航栏沉浸度差，全面屏边缘存在白边或截断。

### 2. 重构目标（已达成）
* 采用 **Jetpack Compose + Compose Multiplatform (Kotlin)** 构建纯原生 Android 应用（目录位于 `apk-kotlin/`）。
* 引入 **Kyant Liquid Glass 物理流体着色器（AGSL）** 与现代化质感设计语言。
* 彻底打通与后端 Spring Boot RESTful API 的网络通信，保证所有数据与交互均为真实接口驱动。
* 支持多手势滑动换页、全面屏深度沉浸、日间/夜间模式一键切换、原生登录/注册闭环。

---

## 二、 工程目录结构与核心模块

```text
d:\java\------项目------\基于RAG的Java面试系统\apk-kotlin\
├── androidApp/                         # Android 入口模块
│   └── src/main/
│       ├── AndroidManifest.xml         # 应用清单配置（包名: com.rag.interview）
│       ├── java/.../MainActivity.kt    # 主 Activity：全屏 Edge-to-Edge 沉浸、主题状态初始化
│       └── res/                        # 矢量自适应图标资源（mipmap-anydpi-v26 等）
│
└── app/src/commonMain/kotlin/com/kyant/backdrop/catalog/
    ├── components/
    │   └── LiquidBottomTabs.kt         # 原创 Kyant 琥珀色流体玻璃底部导航栏（AGSL 着色器 + 弹簧吸附）
    │
    └── interview/
        ├── HttpPlatform.kt             # 跨平台 HTTP 接口声明（expect/actual）
        ├── InterviewApi.kt             # 全局 API 单例：用户信息状态、网络请求、模拟面试引擎
        ├── InterviewApp.kt             # 应用主骨架：HorizontalPager 页面滑动、全局弹窗承载
        ├── InterviewScreens.kt         # 五大核心页面实现（首页/AI问答/模拟面试/题库/个人中心）
        ├── LoginScreen.kt              # 独立原生登录与注册全屏页面（含快捷填充、密码显隐）
        └── ThemeState.kt               # 全局响应式主题引擎（日间模式、夜间模式、动态色值定义）
```

---

## 三、 核心页面与功能特性

### 1. 首页学习工作台 (`HomeScreen`)
* **备考数据核心看板**：今日已刷题数、累计模拟面试场次、知识库检索命中次数、高频弱项聚焦。
* **快捷功能入口**：一键发起 AI 模拟面试、开启高频题专项突击、查看知识库检索报告。
* **顶部快捷栏**：支持快速一键切换日间/夜间模式 (`☀️ 日间 / 🌙 夜间`)，以及手动刷新服务器数据。

### 2. AI 面试官智能问答 (`AiInterviewScreen`)
* **RAG 向量知识库智能问答**：集成真实后端接口（通过 `InterviewApi.chatWithAi` 实时向大模型与向量库发起检索）。
* **现代化聊天气泡流**：
  * 面试官与学员专属头像与气泡样式；
  * 知识库来源展开面板（可查看 RAG 检索命中的知识切片与匹配度）；
  * 预设高频 Java 面试试题标签（如 JVM 垃圾回收、HashMap 底层原理、分布式锁等），点击即可直接向 AI 发问。

### 3. 全真模拟面试系统 (`MockInterviewScreen`)
* **多岗位阶段定制**：支持校招冲刺、中级 Java 工程师、资深架构师等不同难度的面试定制。
* **面试全真问答流程**：面试官动态出题，考生输入作答，AI 实时评分与追问。
* **能力维度雷达图与评测诊断报告**：面试完成后自动汇总基础功底、并发能力、系统设计、微服务治理等多维度得分与优化建议。

### 4. 题库精选与高频星标 (`QuestionBankScreen`)
* **大厂面试真题分类**：涵盖 Java 基础、JVM、并发多线程、MySQL/Redis、Spring 全家桶、微服务与高并发架构等。
* **题目详情与参考解析**：支持搜索关键词、折叠/展开答案与踩坑详解。

### 5. 个人中心与学习档案 (`UserScreen`)
* **学员档案卡片**：展示当前登录用户、VIP 角色标识、目标职位与期望薪资。
* **个性化备考配置**：备考打卡提醒、错题本与弱项强化、清除本地会话缓存。
* **账号登录与退出入口**：支持切换账号、重新登录或退出登录状态。

### 6. 原生登录与注册系统 (`LoginScreen.kt`)
* **双模式无缝切换**：采用分段切换器（Segmented Tab）在「账号登录」与「注册新账号」间平滑切换。
* **快速演示账号一键填充**：
  * 👑 `教研主管 (admin / 123456)`
  * 🎓 `备考学员 (zhangsan / 123456)`
  * 点击对应徽章即可免打字自动填入账号密码，方便评审与测试。
* **用户体验细节**：
  * 密码显隐切换按钮（👁️ / 🙈）；
  * 客户端空值与长度校验，友好横幅提醒；
  * 「暂不登录，以访客模式体验系统 ›」快速绕过；
  * **完全隐藏敏感的服务端 IP 和端口信息**，界面整洁专业。

---

## 四、 核心技术实现与交互亮点

### 1. 屏幕横向左右平滑滑动换页 (`HorizontalPager`)
* **双向联动**：主界面采用 Compose `HorizontalPager(state = pagerState, count = 5)`，支持手指在屏幕任意位置左右流畅滑动切换页面。
* **物理弹簧跟随**：手势滑动过程与底部 Kyant 玻璃胶囊 Dock 实时联动，拖动进度平滑插值，兼具手势跟手性与视觉反馈。

### 2. Kyant 琥珀色流体玻璃底部导航栏 (`LiquidBottomTabs.kt`)
* **渲染原理**：利用 Android 13+ 的 AGSL（Android Graphics Shading Language）运行时着色器，实时模拟玻璃的折射率、边缘高光、光晕与模糊。
* **视觉特征**：底色带有轻微温暖的琥珀金润色（`#E59819`，透明度约 0.15~0.22），拖拽切换带有弹簧阻尼吸附动效。

### 3. 全局全面屏边缘浸润（Edge-to-Edge）
* **状态栏/导航栏沉浸**：在 `MainActivity.kt` 中开启 `WindowCompat.setDecorFitsSystemWindows(window, false)`。
* **安全区域适配**：页面顶底根据 `WindowInsets.statusBars` 与 `WindowInsets.navigationBars` 动态计算安全 Padding，避免内容被挖孔刘海或底部小白条遮挡。
* **状态栏文字颜色联动**：根据当前主题自动切换 `isAppearanceLightStatusBars`，日间模式深色文字，夜间模式浅色文字。

### 4. 日间/夜间深色模式引擎 (`ThemeState.kt`)
* **动态色系定义**：
  * 背景主色：日间 `#F4F1EA`（暖白纸质感）/ 夜间 `#0B1118`（深空极夜墨蓝）；
  * 卡片背景：日间 `#FBFAF6` / 夜间 `#161F2B`；
  * 边框分割：日间 `#D8D2C4` / 夜间 `#283646`；
  * 文字主色：日间 `#131B23` / 夜间 `#F1F5F9`；
  * 次要背景：日间 `#EFEBE2` / 夜间 `#243242`；
  * 品牌主色：保持优雅的殷红 `#C23A2B` 与活力红 `#E83D2C`。
* **持久化存储**：切换设置通过 Android `SharedPreferences` 持久化，重启应用自动恢复上次选择。

### 5. 定制 App 自适应图标（Adaptive Icon）
* **设计规范**：按照 Android 官方自适应图标规范（108×108dp，核心安全区域 72×72dp）。
* **视觉呈现**：中间为 Java 标志性咖啡杯矢量徽标，外圈带有琥珀金光环，支持在圆形（Circle）、圆角矩形（Rounded Rect）和超椭圆（Squircle）桌面上完美适配。

---

## 五、 后端 API 接口对接规范

应用默认连接后端云端服务器（亦可配合本地联调）：
* **默认 BaseUrl**：`http://47.98.190.255:81/api`
* **核心接口一览**：
  1. **登录**：`POST /user/login`
     * 请求体：`{"username": "admin", "password": "..."}`
     * 响应：`{"code": 200, "data": {"token": "...", "userInfo": {...}}}`
  2. **注册**：`POST /user/register`
     * 请求体：`{"username": "...", "password": "...", "nickname": "...", "email": "..."}`
     * 响应：`{"code": 200, "message": "注册成功"}`
  3. **AI 问答 / RAG 检索**：`POST /ai/chat`
     * 请求体：`{"message": "请讲一下 HashMap 的扩容机制", "conversationId": "..."}`
     * 响应：`{"code": 200, "data": {"reply": "...", "references": [...]}}`
  4. **全真模拟面试与诊断**：`POST /interview/start`、`POST /interview/answer`、`POST /interview/report`。
  5. **题库接口**：`GET /question/categories`、`GET /question/list?category=...`。

---

## 六、 编译、安装与实机运维指南

### 1. Gradle 编译命令
在项目根目录或 `apk-kotlin/` 目录下执行：
```powershell
cd apk-kotlin
.\gradlew.bat :androidApp:assembleDebug
```
* 编译产物位于：`apk-kotlin\androidApp\build\outputs\apk\debug\androidApp-debug.apk`。
* 项目根目录副本：`Java面试系统.apk`。

### 2. ADB 一键安装与启动
```powershell
# 复制最新产物到根目录
Copy-Item 'apk-kotlin\androidApp\build\outputs\apk\debug\androidApp-debug.apk' -Destination 'Java面试系统.apk' -Force

# 流式覆盖安装到已连接手机
adb -s <设备序列号> install -r 'Java面试系统.apk'

# 启动主界面
adb -s <设备序列号> shell am start -n com.rag.interview/com.kyant.backdrop.catalog.MainActivity
```

### 3. 测试注意事项（避免污染相册）
* 在使用 `adb shell screencap -p` 调试时，截取的临时图片**不要**存放在公共的 `/sdcard/` 根目录，否则系统相册（MediaScanner）会自动将其作为新照片索引显示在用户相册中。
* 调试截屏应写入私有临时目录或调试完成后立即通过 `rm` 移除并发送媒体库刷新广播。
