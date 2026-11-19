package com.kyant.backdrop.catalog.interview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Dynamic colors are imported from ThemeState.kt in the same package

// -------------------------------------------------------------
// 1. Dashboard Screen (首页)
// -------------------------------------------------------------
@Composable
fun DashboardScreen(onNavigateToTab: (Int) -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    val stats = InterviewApi.cachedStats
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    LaunchedEffect(Unit) {
        InterviewApi.refreshStats()
    }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(BgColor),
        contentPadding = PaddingValues(start = 14.dp, top = statusBarTop + 10.dp, end = 14.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            HeaderSection(
                title = "系统数据总览",
                subtitle = "OVERVIEW",
                actionText = if (InterviewApi.isStatsLoading) "刷新中..." else "点击刷新",
                onActionClick = {
                    coroutineScope.launch { InterviewApi.refreshStats() }
                }
            )
        }

        // 2x2 Stats Grid (实时读取后端统计)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        Modifier.weight(1f),
                        "知识文档",
                        "篇",
                        "${stats.docTotal}",
                        "切片: ${stats.chunkTotal} · 点击管理",
                        onClick = { InterviewApi.showKnowledgeScreen = true }
                    )
                    StatCard(
                        Modifier.weight(1f),
                        "题库题量",
                        "题",
                        "${stats.questionTotal}",
                        "覆盖基础到分布式",
                        onClick = { onNavigateToTab(3) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard(
                        Modifier.weight(1f),
                        "面试场次",
                        "场",
                        "${stats.interviewTotal}",
                        "动态追问与测评",
                        onClick = { onNavigateToTab(2) }
                    )
                    StatCard(
                        Modifier.weight(1f),
                        "面试均分",
                        "分",
                        "${stats.avgScore}",
                        "综合多维评测指标",
                        onClick = { onNavigateToTab(2) }
                    )
                }
            }
        }

        // 核心功能快捷直达 (点击直接切换到对应Tab)
        item {
            SectionTitle("01", "核心功能快捷直达")
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(
                        Modifier.weight(1f),
                        "模拟面试",
                        "AI 动态出题与追问",
                        "🎯",
                        PrimaryRed,
                        onClick = { onNavigateToTab(2) }
                    )
                    ActionCard(
                        Modifier.weight(1f),
                        "智能问答",
                        "知识库检索增强",
                        "💬",
                        TextInk,
                        onClick = { onNavigateToTab(1) }
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionCard(
                        Modifier.weight(1f),
                        "题库训练",
                        "随机刷题自测",
                        "📚",
                        TagIndigo,
                        onClick = { onNavigateToTab(3) }
                    )
                    ActionCard(
                        Modifier.weight(1f),
                        "文档知识库",
                        "上传解析与切片",
                        "📁",
                        TagAmber,
                        onClick = { InterviewApi.showKnowledgeScreen = true }
                    )
                }
            }
        }

        // 知识分类分布 (点击直接前往题库并自动筛选对应分类)
        item {
            SectionTitle("02", "知识分类分布")
        }

        item {
            CategorySection(onCategoryClick = { onNavigateToTab(3) })
        }

        item {
            Spacer(Modifier.height(110.dp))
        }
    }
}

// -------------------------------------------------------------
// 2. Chat Screen (AI 问答 - 完整真实交互、多会话管理与 RAG/CHAT 模式双擎)
// -------------------------------------------------------------
@Composable
fun ChatScreen() {
    val coroutineScope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var showSessionModal by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    fun dismissKeyboard() {
        keyboardController?.hide()
        focusManager.clearFocus()
    }

    val currentMode = InterviewApi.currentChatMode
    val sessions = InterviewApi.chatSessions
    val currentSessionId = InterviewApi.currentChatSessionId
    val selectedDocId = InterviewApi.selectedChatDocId
    val docs = InterviewApi.knowledgeDocList

    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    id = "msg-0",
                    isUser = false,
                    content = if (currentMode == "RAG")
                        "你好！我是基于 RAG 知识库检索增强的 Java 面试与技术助手。请随时提问！"
                    else
                        "你好！我是 Java 日常技术助手，直连大模型为您解答疑难。请随时提问！",
                    timestamp = "12:00"
                )
            )
        )
    }

    // 首次进入及切换模式时加载会话
    LaunchedEffect(currentMode) {
        val sList = InterviewApi.refreshChatSessions(currentMode)
        if (sList.isNotEmpty() && currentSessionId == null) {
            val first = sList[0]
            InterviewApi.currentChatSessionId = first.id
            val msgs = InterviewApi.loadChatMessages(first.id)
            if (msgs.isNotEmpty()) messages = msgs
        }
    }

    // 切换会话时加载历史消息
    LaunchedEffect(currentSessionId) {
        if (currentSessionId != null) {
            val msgs = InterviewApi.loadChatMessages(currentSessionId)
            if (msgs.isNotEmpty()) {
                messages = msgs
            } else {
                messages = listOf(
                    ChatMessage(
                        id = "msg-0",
                        isUser = false,
                        content = "已进入会话。您可以继续提问任何 Java 技术问题！",
                        timestamp = "刚刚"
                    )
                )
            }
        }
    }

    val listState = rememberLazyListState()

    fun sendMessage(questionText: String) {
        val q = questionText.trim()
        if (q.isEmpty() || isSending) return

        val userMsg = ChatMessage(
            id = "user-${System.currentTimeMillis()}",
            isUser = true,
            content = q,
            timestamp = "刚刚"
        )
        val aiPendingMsg = ChatMessage(
            id = "ai-${System.currentTimeMillis()}",
            isUser = false,
            content = if (currentMode == "RAG") "正在检索知识库向量切片并生成严谨解析..." else "正在组织语言回答...",
            timestamp = "刚刚"
        )

        messages = messages + userMsg + aiPendingMsg
        inputText = ""
        isSending = true

        coroutineScope.launch {
            listState.animateScrollToItem(messages.size - 1)
            val result = InterviewApi.sendChatMessageEx(q, selectedDocId, 3)
            val answer = result.second
            val refs = result.third
            messages = messages.dropLast(1) + ChatMessage(
                id = "ai-reply-${System.currentTimeMillis()}",
                isUser = false,
                content = answer,
                refs = refs,
                timestamp = "刚刚"
            )
            isSending = false
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // 监听键盘高度与导航栏高度，精准计算避让，杜绝呼出键盘对话框抬得过高
    val imePaddingValues = WindowInsets.ime.asPaddingValues()
    val navBarPaddingValues = WindowInsets.navigationBars.asPaddingValues()

    val imeBottom = imePaddingValues.calculateBottomPadding()
    val navBottom = navBarPaddingValues.calculateBottomPadding()

    val isKeyboardOpen = imeBottom > navBottom + 30.dp

    val finalBottomPadding = if (isKeyboardOpen) {
        imeBottom + 6.dp
    } else {
        navBottom + 92.dp
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(BgColor)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = { dismissKeyboard() }
                )
        ) {
            // 1. 紧凑顶部导航栏与模式切换
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(CardBg.copy(alpha = 0.95f))
                    .border(width = 0.5.dp, color = BorderColor)
                    .statusBarsPadding()
            ) {
                // 第一行：标题、会话按钮、新建按钮
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (currentMode == "RAG") Color(0xFF22C55E) else TagIndigo)
                        )
                        Spacer(Modifier.width(8.dp))
                        BasicText(
                            "Java 技术问答",
                            style = TextStyle(color = TextInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // 会话列表按钮
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(BgColor)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .clickable { showSessionModal = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                "📑 会话 (${sessions.size})",
                                style = TextStyle(color = TagIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }

                        // 新建会话按钮
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryRed)
                                .clickable {
                                    coroutineScope.launch {
                                        val s = InterviewApi.createNewChatSession(sessionType = currentMode)
                                        if (s != null) {
                                            messages = listOf(
                                                ChatMessage(
                                                    id = "msg-0",
                                                    isUser = false,
                                                    content = "新会话已创建，请随时提问！",
                                                    timestamp = "刚刚"
                                                )
                                            )
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            BasicText(
                                "➕ 新建",
                                style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // 第二行：模式切换 Pills (RAG vs CHAT)
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isRag = (currentMode == "RAG")
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isRag) PrimaryRed else BgColor)
                            .border(1.dp, if (isRag) PrimaryRed else BorderColor, RoundedCornerShape(10.dp))
                            .clickable {
                                if (!isRag) {
                                    InterviewApi.currentChatMode = "RAG"
                                    InterviewApi.currentChatSessionId = null
                                }
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "📚 知识库问答 (RAG)",
                            style = TextStyle(
                                color = if (isRag) Color.White else TextInk,
                                fontSize = 11.sp,
                                fontWeight = if (isRag) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (!isRag) PrimaryRed else BgColor)
                            .border(1.dp, if (!isRag) PrimaryRed else BorderColor, RoundedCornerShape(10.dp))
                            .clickable {
                                if (isRag) {
                                    InterviewApi.currentChatMode = "CHAT"
                                    InterviewApi.currentChatSessionId = null
                                }
                            }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            "💬 日常技术对话 (CHAT)",
                            style = TextStyle(
                                color = if (!isRag) Color.White else TextInk,
                                fontSize = 11.sp,
                                fontWeight = if (!isRag) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }

                // 第三行（仅在 RAG 模式下）：文档筛选范围
                if (currentMode == "RAG") {
                    LazyRow(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        item {
                            val isAllSelected = (selectedDocId == null)
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAllSelected) TagTeal else BgColor)
                                    .border(1.dp, if (isAllSelected) TagTeal else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable { InterviewApi.selectedChatDocId = null }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                BasicText(
                                    "全部文档",
                                    style = TextStyle(
                                        color = if (isAllSelected) Color.White else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }
                        }
                        items(docs) { doc ->
                            val isDocSelected = (selectedDocId == doc.id)
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isDocSelected) TagTeal else BgColor)
                                    .border(1.dp, if (isDocSelected) TagTeal else BorderColor, RoundedCornerShape(8.dp))
                                    .clickable {
                                        InterviewApi.selectedChatDocId = if (isDocSelected) null else doc.id
                                    }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                BasicText(
                                    doc.docName,
                                    style = TextStyle(
                                        color = if (isDocSelected) Color.White else TextMuted,
                                        fontSize = 10.sp,
                                        fontWeight = if (isDocSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 2. 聊天消息流：点击空白或滑动均触发关闭键盘与对话框回归
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = null,
                        indication = null,
                        onClick = { dismissKeyboard() }
                    ),
                contentPadding = PaddingValues(start = 14.dp, top = 8.dp, end = 14.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatMessageItem(msg, onDismissKeyboard = { dismissKeyboard() })
                }
            }

            // 3. 底部输入与快捷灵感区：精准计算避开液态玻璃导航栏与软键盘
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = finalBottomPadding)
            ) {
                // 水平滑动快捷问题 Chips
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        listOf(
                            "💡 JVM垃圾回收器",
                            "💡 HashMap底层扩容",
                            "💡 Redis缓存击穿",
                            "💡 Spring事务失效",
                            "💡 MySQL索引与回表",
                            "💡 分布式锁实现方案"
                        )
                    ) { chip ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                                .clickable { sendMessage(chip.removePrefix("💡 ")) }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            BasicText(chip, style = TextStyle(color = TagIndigo, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }

                // 输入框与发送按钮
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(InputBoxBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(22.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        if (inputText.isEmpty()) {
                            BasicText(
                                if (currentMode == "RAG") "向 RAG 知识库提问技术难点..." else "输入问题与技术助手对话...",
                                style = TextStyle(color = TextDim, fontSize = 13.sp)
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = TextStyle(color = TextInk, fontSize = 13.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Box(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (inputText.isNotBlank() && !isSending) PrimaryRed else if (isAppInDarkTheme()) Color(0xFF475569) else Color(0xFFD5A39B))
                            .clickable(enabled = inputText.isNotBlank() && !isSending) {
                                sendMessage(inputText)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        BasicText(
                            if (isSending) "..." else "发送",
                            style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }

        // 会话管理弹窗
        if (showSessionModal) {
            ChatSessionModal(
                onDismiss = { showSessionModal = false }
            )
        }
    }
}

@Composable
fun ChatSessionModal(onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val sessions = InterviewApi.chatSessions
    val currentId = InterviewApi.currentChatSessionId

    UserModal(
        title = "📑 会话管理 (${InterviewApi.currentChatMode} 模式)",
        onDismiss = onDismiss
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    "共 ${sessions.size} 个历史会话",
                    style = TextStyle(color = TextMuted, fontSize = 12.sp)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryRed)
                        .clickable {
                            coroutineScope.launch {
                                InterviewApi.createNewChatSession(sessionType = InterviewApi.currentChatMode)
                                onDismiss()
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    BasicText("➕ 新建会话", style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
            }

            if (sessions.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("暂无会话记录，点击右上角新建会话", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                }
            } else {
                sessions.forEach { s ->
                    val isCurrent = (s.id == currentId)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCurrent) PrimaryRed.copy(alpha = 0.1f) else BgColor)
                            .border(1.dp, if (isCurrent) PrimaryRed else BorderColor, RoundedCornerShape(10.dp))
                            .clickable {
                                InterviewApi.currentChatSessionId = s.id
                                onDismiss()
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                BasicText(
                                    s.title,
                                    style = TextStyle(
                                        color = if (isCurrent) PrimaryRed else TextInk,
                                        fontSize = 13.sp,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                BasicText(
                                    "${s.createTime} · ${s.messageCount} 条消息",
                                    style = TextStyle(color = TextDim, fontSize = 10.sp)
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCurrent) {
                                    Badge("当前", PrimaryRed, TagRedBg)
                                }
                                Box(
                                    Modifier
                                        .clip(CircleShape)
                                        .background(CardBg)
                                        .clickable {
                                            coroutineScope.launch {
                                                InterviewApi.deleteChatSession(s.id)
                                            }
                                        }
                                        .padding(6.dp)
                                ) {
                                    BasicText("🗑️", style = TextStyle(fontSize = 11.sp))
                                }
                            }
                        }
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SecondaryButtonBg)
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText("关闭", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
        }
    }
}

@Composable
fun ChatMessageItem(msg: ChatMessage, onDismissKeyboard: () -> Unit = {}) {
    var expandedRefs by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = null,
                indication = null,
                onClick = onDismissKeyboard
            ),
        horizontalAlignment = if (msg.isUser) Alignment.End else Alignment.Start
    ) {
        if (!msg.isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp)
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(PrimaryRed),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("AI", style = TextStyle(color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.width(6.dp))
                BasicText(
                    "RAG 知识库检索增强",
                    style = TextStyle(color = TextMuted, fontSize = 11.sp)
                )
            }
        }

        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (msg.isUser) UserChatBubbleBg else CardBg)
                .border(
                    width = if (msg.isUser) 0.dp else 1.dp,
                    color = BorderColor,
                    shape = RoundedCornerShape(14.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .fillMaxWidth(if (msg.isUser) 0.82f else 0.98f)
        ) {
            BasicText(
                msg.content,
                style = TextStyle(
                    color = if (msg.isUser) Color.White else TextInk,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            )
        }

        // References badge & detail
        if (msg.refs.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(TagAmberBg)
                    .clickable { expandedRefs = !expandedRefs }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                BasicText(
                    "📚 关联引用了 ${msg.refs.size} 篇知识库文档 ${if (expandedRefs) "▲ 收起" else "▼ 展开"}",
                    style = TextStyle(color = TagAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
            }

            AnimatedVisibility(visible = expandedRefs) {
                Column(
                    Modifier
                        .padding(top = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .fillMaxWidth(0.98f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    msg.refs.forEach { ref ->
                        Column {
                            BasicText(
                                "📄 ${ref.docName} (相似度: ${(ref.score * 100).toInt()}%)",
                                style = TextStyle(color = TagIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            )
                            BasicText(
                                ref.snippet,
                                style = TextStyle(color = TextMuted, fontSize = 10.sp, lineHeight = 14.sp),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 3. Question Screen (题库中心 - 分类/难度筛选、随机刷题自测与录入)
// -------------------------------------------------------------
@Composable
fun QuestionScreen() {
    val coroutineScope = rememberCoroutineScope()
    var selectedCategory by remember { mutableStateOf("全部") }
    var selectedDifficulty by remember { mutableStateOf<Int?>(null) } // null: 全部, 1: 简单, 2: 中等, 3: 困难
    var expandedId by remember { mutableStateOf<Int?>(null) }
    var searchKeyword by remember { mutableStateOf("") }

    var showQuizModal by remember { mutableStateOf(false) }
    var showAddModal by remember { mutableStateOf(false) }
    var toastMsg by remember { mutableStateOf<String?>(null) }

    val categories = listOf("全部", "Spring", "MySQL", "Redis", "JVM", "Java基础", "分布式")
    val diffOptions = listOf(null to "全部难度", 1 to "简单", 2 to "中等", 3 to "困难")

    LaunchedEffect(toastMsg) {
        if (toastMsg != null) {
            delay(2500)
            toastMsg = null
        }
    }

    LaunchedEffect(selectedCategory) {
        InterviewApi.refreshQuestions(selectedCategory)
    }

    val questions = InterviewApi.cachedQuestions.ifEmpty {
        remember {
            listOf(
                QuestionItem(1, "Redis 的持久化机制有哪些？如何选型？", "两种核心方式：\n1. RDB 定时全量快照，文件小、恢复极快；\n2. AOF 记录写日志，安全性高。\n生产推荐开启 RDB + AOF 混合持久化。", "Redis", 2, "RDB,AOF,持久化", 128),
                QuestionItem(2, "缓存穿透、缓存击穿、缓存雪崩的区别与解决方案？", "1. 穿透：布隆过滤器前置拦截；\n2. 击穿：互斥锁（Mutex）与逻辑永不过期；\n3. 雪崩：过期时间加随机抖动扰动。", "分布式", 3, "缓存设计,高并发", 245),
                QuestionItem(3, "HashMap 在 JDK 1.7 与 1.8 中的核心区别及并发死循环原因？", "1.7 头插法导致环形死链；1.8 采用高低位指针与尾插法，并在阈值 8 时树化。", "Java基础", 3, "集合源码,并发安全", 310),
                QuestionItem(4, "Spring Bean 的生命周期及三级缓存如何解决循环依赖？", "经历实例化、属性注入、初始化、AOP代理。\n三级缓存通过 ObjectFactory 提前曝光早期代理对象化解单例循环依赖。", "Spring", 2, "IoC容器,三级缓存", 189),
                QuestionItem(5, "MySQL InnoDB 聚簇索引与非聚簇索引的区别？何为回表与索引覆盖？", "聚簇索引叶子存整行记录；二级索引叶子存主键值。非覆盖查询需要回到主键索引树二次检索（回表）。", "MySQL", 2, "索引原理,回表优化", 212)
            )
        }
    }

    val filteredList = questions.filter { q ->
        val matchesCat = if (selectedCategory == "全部") true else q.category.equals(selectedCategory, ignoreCase = true)
        val matchesDiff = if (selectedDifficulty == null) true else q.difficulty == selectedDifficulty
        val matchesSearch = if (searchKeyword.isBlank()) true else q.title.contains(searchKeyword, ignoreCase = true) || q.tags.contains(searchKeyword, ignoreCase = true)
        matchesCat && matchesDiff && matchesSearch
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(BgColor),
            contentPadding = PaddingValues(start = 14.dp, top = statusBarTop + 10.dp, end = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                HeaderSection(
                    title = "题库中心",
                    subtitle = "QUESTION BANK",
                    actionText = if (InterviewApi.isLoadingQuestions) "加载中..." else "刷新题库",
                    onActionClick = {
                        coroutineScope.launch { InterviewApi.refreshQuestions(selectedCategory) }
                    }
                )
            }

            // 快捷功能栏：🎲 随机刷题自测 & ➕ 录入技术考题
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PrimaryRed)
                            .clickable {
                                coroutineScope.launch {
                                    InterviewApi.loadRandomQuiz(category = if (selectedCategory == "全部") null else selectedCategory, count = 5)
                                    showQuizModal = true
                                }
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("🎲 随机刷题自测 (5题)", style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .clickable { showAddModal = true }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("➕ 录入技术考题", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Search Bar
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    if (searchKeyword.isEmpty()) {
                        BasicText("🔍 搜索考点关键词 (如: 索引, 扩容, 垃圾回收)...", style = TextStyle(color = TextDim, fontSize = 12.sp))
                    }
                    BasicTextField(
                        value = searchKeyword,
                        onValueChange = { searchKeyword = it },
                        textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Category Filter Tabs
            item {
                LazyRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(categories) { cat ->
                        val isActive = (cat == selectedCategory)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isActive) TextInk else CardBg)
                                .border(1.dp, if (isActive) TextInk else BorderColor, RoundedCornerShape(14.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            BasicText(
                                cat,
                                style = TextStyle(
                                    color = if (isActive) CardBg else TextInk,
                                    fontSize = 12.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            // Difficulty Filter Tabs
            item {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    diffOptions.forEach { (dVal, dLabel) ->
                        val isActive = (selectedDifficulty == dVal)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isActive) PrimaryRed else CardBg)
                                .border(1.dp, if (isActive) PrimaryRed else BorderColor, RoundedCornerShape(12.dp))
                                .clickable { selectedDifficulty = dVal }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            BasicText(
                                dLabel,
                                style = TextStyle(
                                    color = if (isActive) Color.White else TextInk,
                                    fontSize = 11.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    }
                }
            }

            // Question items
            items(filteredList, key = { it.id }) { q ->
                QuestionCard(
                    question = q,
                    isExpanded = (expandedId == q.id),
                    onToggle = {
                        expandedId = if (expandedId == q.id) null else q.id
                    },
                    onDelete = {
                        coroutineScope.launch {
                            val ok = InterviewApi.deleteQuestion(q.id)
                            toastMsg = if (ok) "已成功删除考题: ${q.title.take(12)}..." else "删除失败"
                        }
                    }
                )
            }

            item {
                Spacer(Modifier.height(110.dp))
            }
        }

        // Floating Toast Notification
        toastMsg?.let { msg ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TextInk.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(msg, style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium))
            }
        }

        // 随机自测弹窗
        if (showQuizModal) {
            RandomQuizModal(
                onDismiss = { showQuizModal = false }
            )
        }

        // 录入新题弹窗
        if (showAddModal) {
            AddQuestionModal(
                onDismiss = { showAddModal = false },
                onSuccess = {
                    showAddModal = false
                    toastMsg = "新考题录入成功并已入库！"
                }
            )
        }
    }
}

@Composable
fun QuestionCard(
    question: QuestionItem,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val diffText = when (question.difficulty) {
        1 -> "简单"
        3 -> "困难"
        else -> "中等"
    }
    val diffColor = when (question.difficulty) {
        1 -> TagTeal
        3 -> TagRed
        else -> TagAmber
    }
    val diffBg = when (question.difficulty) {
        1 -> TagTealBg
        3 -> TagRedBg
        else -> TagAmberBg
    }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .animateContentSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            BasicText(
                question.title,
                modifier = Modifier.weight(1f),
                style = TextStyle(color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
            )
            Spacer(Modifier.width(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (onDelete != null) {
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(BgColor)
                            .clickable { onDelete() }
                            .padding(4.dp)
                    ) {
                        BasicText("🗑️", style = TextStyle(fontSize = 11.sp))
                    }
                }
                BasicText(
                    if (isExpanded) "▲ 收起" else "▼ 解析",
                    style = TextStyle(color = ProjectWarmYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Badge(question.category, TagIndigo, TagIndigoBg)
            Badge(diffText, diffColor, diffBg)
            BasicText("热度: ${question.frequency}", style = TextStyle(color = TextMuted, fontSize = 11.sp))
            Spacer(Modifier.weight(1f))
            BasicText(question.tags, style = TextStyle(color = TextDim, fontSize = 10.sp))
        }

        if (isExpanded) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SecondaryButtonBg)
                    .border(1.dp, Color(0xFFD8D2C4), RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText("【标准参考解析】", style = TextStyle(color = TagIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    BasicText(
                        question.answer,
                        style = TextStyle(color = TextInk, fontSize = 13.sp, lineHeight = 19.sp)
                    )
                }
            }
        }
    }
}

@Composable
fun RandomQuizModal(onDismiss: () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val quizQuestions = InterviewApi.randomQuizQuestions
    var currentIndex by remember { mutableIntStateOf(0) }
    var revealAnswer by remember { mutableStateOf(false) }

    UserModal(
        title = "🎲 随机刷题自测",
        onDismiss = onDismiss
    ) {
        if (InterviewApi.isLoadingQuiz) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText("正在从题库抽取高质量考题...", style = TextStyle(color = TextMuted, fontSize = 13.sp))
            }
        } else if (quizQuestions.isEmpty()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText("题库中暂无可抽取的题目", style = TextStyle(color = TextMuted, fontSize = 13.sp))
            }
        } else {
            val q = quizQuestions.getOrNull(currentIndex) ?: quizQuestions[0]
            val diffText = when (q.difficulty) {
                1 -> "简单"
                3 -> "困难"
                else -> "中等"
            }
            val diffColor = when (q.difficulty) {
                1 -> TagTeal
                3 -> TagRed
                else -> TagAmber
            }
            val diffBg = when (q.difficulty) {
                1 -> TagTealBg
                3 -> TagRedBg
                else -> TagAmberBg
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        "第 ${currentIndex + 1} / ${quizQuestions.size} 题",
                        style = TextStyle(color = PrimaryRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Badge(q.category, TagIndigo, TagIndigoBg)
                        Badge(diffText, diffColor, diffBg)
                    }
                }

                // Question Title Card
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(BgColor)
                        .padding(14.dp)
                ) {
                    BasicText(
                        q.title,
                        style = TextStyle(color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                    )
                }

                // Reveal Answer Button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (revealAnswer) TagTealBg else TagAmberBg)
                        .clickable { revealAnswer = !revealAnswer }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (revealAnswer) "🙈 收起参考解析" else "👁️ 点击查看标准参考解析",
                        style = TextStyle(color = if (revealAnswer) TagTeal else TagAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    )
                }

                // Revealed Answer
                if (revealAnswer) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondaryButtonBg)
                            .border(1.dp, Color(0xFFD8D2C4), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            BasicText("【标准参考解析】", style = TextStyle(color = TagIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            BasicText(
                                q.answer,
                                style = TextStyle(color = TextInk, fontSize = 13.sp, lineHeight = 19.sp)
                            )
                        }
                    }
                }

                // Navigation Controls
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (currentIndex > 0) SecondaryButtonBg else BgColor)
                            .clickable(enabled = currentIndex > 0) {
                                currentIndex--
                                revealAnswer = false
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("◀ 上一题", style = TextStyle(color = if (currentIndex > 0) TextInk else TextDim, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }

                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (currentIndex < quizQuestions.size - 1) PrimaryRed else TagTeal)
                            .clickable {
                                if (currentIndex < quizQuestions.size - 1) {
                                    currentIndex++
                                    revealAnswer = false
                                } else {
                                    onDismiss()
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            if (currentIndex < quizQuestions.size - 1) "下一题 ▶" else "完成自测",
                            style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                // Reload group button
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgColor)
                        .clickable {
                            coroutineScope.launch {
                                currentIndex = 0
                                revealAnswer = false
                                InterviewApi.loadRandomQuiz(count = 5)
                            }
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("🔄 再换一组 (5题)", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                }
            }
        }
    }
}

@Composable
fun AddQuestionModal(
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Java基础") }
    var difficulty by remember { mutableIntStateOf(2) }
    var tags by remember { mutableStateOf("核心高频") }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val categories = listOf("Java基础", "Spring", "MySQL", "Redis", "JVM", "分布式")

    UserModal(
        title = "➕ 录入技术考题",
        onDismiss = onDismiss
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .heightIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title
            BasicText("题目描述 *", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(InputBoxBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                if (title.isEmpty()) {
                    BasicText("如：ConcurrentHashMap 1.8 扩容原理...", style = TextStyle(color = TextDim, fontSize = 12.sp))
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Category Picker
            BasicText("知识分类 *", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                items(categories) { cat ->
                    val isSelected = (cat == category)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryRed else BgColor)
                            .border(1.dp, if (isSelected) PrimaryRed else BorderColor, RoundedCornerShape(12.dp))
                            .clickable { category = cat }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        BasicText(cat, style = TextStyle(color = if (isSelected) Color.White else TextInk, fontSize = 11.sp))
                    }
                }
            }

            // Difficulty Picker
            BasicText("题目难度 *", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1 to "简单", 2 to "中等", 3 to "困难").forEach { (dVal, dLabel) ->
                    val isSelected = (difficulty == dVal)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) TagTeal else BgColor)
                            .border(1.dp, if (isSelected) TagTeal else BorderColor, RoundedCornerShape(10.dp))
                            .clickable { difficulty = dVal }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        BasicText(dLabel, style = TextStyle(color = if (isSelected) Color.White else TextInk, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }

            // Tags
            BasicText("考点标签 (英文逗号隔开)", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(InputBoxBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                if (tags.isEmpty()) {
                    BasicText("如：并发,锁机制,AQS", style = TextStyle(color = TextDim, fontSize = 12.sp))
                }
                BasicTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Answer
            BasicText("标准参考解析 *", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(InputBoxBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                if (answer.isEmpty()) {
                    BasicText("输入该题的标准深度参考答案...", style = TextStyle(color = TextDim, fontSize = 12.sp))
                }
                BasicTextField(
                    value = answer,
                    onValueChange = { answer = it },
                    textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                    modifier = Modifier.fillMaxSize()
                )
            }

            errorMsg?.let { err ->
                BasicText(err, style = TextStyle(color = PrimaryRed, fontSize = 11.sp))
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(PrimaryRed)
                        .clickable(enabled = !isSubmitting) {
                            if (title.isBlank()) {
                                errorMsg = "请输入题目描述"
                                return@clickable
                            }
                            if (answer.isBlank()) {
                                errorMsg = "请输入参考解析"
                                return@clickable
                            }
                            isSubmitting = true
                            errorMsg = null
                            coroutineScope.launch {
                                val ok = InterviewApi.addQuestion(title, answer, category, difficulty, tags)
                                isSubmitting = false
                                if (ok) {
                                    onSuccess()
                                } else {
                                    errorMsg = "保存失败，请检查网络或后端接口"
                                }
                            }
                        }
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(if (isSubmitting) "正在保存..." else "保存提交入库", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }

                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SecondaryButtonBg)
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 11.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("取消", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 4. Interview Screen (模拟面试 - 全真面试间与实时评测)
// -------------------------------------------------------------
@Composable
fun InterviewScreen() {
    RealInterviewScreen()
}

// -------------------------------------------------------------
// 5. User Screen (个人中心 - 真实备考中心与学习看板)
// -------------------------------------------------------------
@Composable
fun UserScreen(onNavigateToTab: (Int) -> Unit = {}) {
    val coroutineScope = rememberCoroutineScope()
    var isSwitchingAccount by remember { mutableStateOf(false) }
    var cacheToast by remember { mutableStateOf<String?>(null) }

    // User preferences & interactive states
    var userGoal by remember { mutableStateOf("Java 资深研发 / 架构师 · 28k - 40k") }
    var reminderEnabled by remember { mutableStateOf(true) }
    var checkinDays by remember { mutableIntStateOf(18) }
    var hasCheckedInToday by remember { mutableStateOf(false) }

    // Modal dialog states
    var showProfileDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var showWeakPointsDialog by remember { mutableStateOf(false) }
    var showFavoritesDialog by remember { mutableStateOf(false) }
    var showRadarDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var showDocsDialog by remember { mutableStateOf(false) }
    var showCheckinDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    // Auto dismiss toast after 2.5 seconds
    LaunchedEffect(cacheToast) {
        if (cacheToast != null) {
            delay(2500)
            cacheToast = null
        }
    }

    val user = InterviewApi.currentUser
    val isLoggedIn = InterviewApi.isLoggedIn && user.username != "guest"
    val docCount = InterviewApi.cachedStats.docTotal.coerceAtLeast(27)
    val questionCount = InterviewApi.cachedQuestions.size.coerceAtLeast(72)
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier
                .fillMaxSize()
                .background(BgColor),
            contentPadding = PaddingValues(start = 14.dp, top = statusBarTop + 10.dp, end = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                HeaderSection("个人中心", "USER PROFILE")
            }

            // Quick Login banner when logged out
            if (!isLoggedIn) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(TagIndigoBg)
                            .border(1.dp, TagIndigo.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("🔑", style = TextStyle(fontSize = 15.sp))
                                Spacer(Modifier.width(6.dp))
                                BasicText(
                                    "当前为访客模式，点击快速一键登录测试账号：",
                                    style = TextStyle(color = TagIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(PrimaryRed)
                                        .clickable {
                                            coroutineScope.launch {
                                                val ok = InterviewApi.login("admin", "123456")
                                                if (ok) {
                                                    InterviewApi.refreshStats()
                                                    cacheToast = "已登录为: 教研主管 (admin)"
                                                }
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("教研主管 (admin)", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                }
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(TagTeal)
                                        .clickable {
                                            coroutineScope.launch {
                                                val ok = InterviewApi.login("zhangsan", "123456")
                                                if (ok) {
                                                    InterviewApi.refreshStats()
                                                    cacheToast = "已登录为: 备考学员 (zhangsan)"
                                                }
                                            }
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("备考学员 (zhangsan)", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }
                }
            }

            // 1. User Profile Card
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .clickable { showProfileDialog = true }
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(if (user.role == "ADMIN") PrimaryRed else TagTeal),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(
                                if (isLoggedIn) user.nickname.take(1) else "访",
                                style = TextStyle(color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText(
                                    if (isLoggedIn) user.nickname else "未登录学员 (访客模式)",
                                    style = TextStyle(color = TextInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                )
                                Spacer(Modifier.width(8.dp))
                                Badge(
                                    if (!isLoggedIn) "未登录" else if (user.role == "ADMIN") "教研主管" else "VIP 备考学员",
                                    if (!isLoggedIn) PrimaryRed else if (user.role == "ADMIN") TagRed else TagIndigo,
                                    if (!isLoggedIn) TagRedBg else if (user.role == "ADMIN") TagRedBg else TagIndigoBg
                                )
                            }
                            BasicText(
                                if (isLoggedIn) "账号: ${user.username} · ${user.email.ifEmpty { "认证学员" }}" else "点击可查看备考档案与账号设置",
                                style = TextStyle(color = TextMuted, fontSize = 12.sp)
                            )
                            BasicText(
                                "🎯 目标岗位: $userGoal",
                                style = TextStyle(color = TagAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicText("›", style = TextStyle(color = TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }

            // 2. Section 01: 备考学习数据看板
            item {
                SectionTitle("01", "备考学习数据看板")
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CandidateStatCard(
                            Modifier.weight(1f),
                            title = "全真模拟面试",
                            value = "3 场",
                            sub = "综合均分 92.5 (优于94%)",
                            icon = "🎯",
                            accent = PrimaryRed,
                            onClick = { showReportDialog = true }
                        )
                        CandidateStatCard(
                            Modifier.weight(1f),
                            title = "题库刷题自测",
                            value = "$questionCount 道",
                            sub = "覆盖 7 大核心领域",
                            icon = "📝",
                            accent = TagIndigo,
                            onClick = { onNavigateToTab(3) }
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CandidateStatCard(
                            Modifier.weight(1f),
                            title = "知识库增强文档",
                            value = "$docCount 篇",
                            sub = "RAG 向量切片已就绪",
                            icon = "📚",
                            accent = TagAmber,
                            onClick = { InterviewApi.showKnowledgeScreen = true }
                        )
                        CandidateStatCard(
                            Modifier.weight(1f),
                            title = "累计备考打卡",
                            value = "$checkinDays 天",
                            sub = if (hasCheckedInToday) "今日已完成打卡" else "连续冲刺高频突破",
                            icon = "⏱️",
                            accent = TagTeal,
                            onClick = {
                                if (!hasCheckedInToday) {
                                    hasCheckedInToday = true
                                    checkinDays++
                                    cacheToast = "今日备考打卡成功！已连续打卡 $checkinDays 天"
                                }
                                showCheckinDialog = true
                            }
                        )
                    }
                }
            }

            // 3. Section 02: 备考工具与历史记录
            item {
                SectionTitle("02", "备考工具与历史记录")
            }

            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(vertical = 4.dp)
                ) {
                    UserActionRow(
                        icon = "📋",
                        title = "面试历史与诊断报告",
                        subtitle = "查看 3 场模拟面试评测详情及能力雷达图",
                        trailingText = "3 份报告",
                        onClick = { showReportDialog = true }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "📕",
                        title = "我的错题本与弱项攻坚",
                        subtitle = "重点强化 JVM 垃圾回收、分布式事务与锁机制",
                        trailingText = "8 个弱项",
                        onClick = { showWeakPointsDialog = true }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "⭐",
                        title = "我的考题收藏与高频星标",
                        subtitle = "已收录 16 道经典大厂面试核心考题",
                        trailingText = "16 题",
                        onClick = { showFavoritesDialog = true }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "📈",
                        title = "知识图谱与能力透视",
                        subtitle = "Java 核心并发、微服务高可用、中间件全景测评",
                        trailingText = "查看 >",
                        onClick = { showRadarDialog = true }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "📁",
                        title = "知识文档库与 RAG 向量解析",
                        subtitle = "上传并解析技术文档、查看向量切片与构建索引",
                        trailingText = "管理文档 >",
                        onClick = { InterviewApi.showKnowledgeScreen = true }
                    )
                }
            }

            // 4. Section 03: 备考偏好与账号管理
            item {
                SectionTitle("03", "备考偏好与账号管理")
            }

            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(vertical = 4.dp)
                ) {
                    UserActionRow(
                        icon = "🎯",
                        title = "目标职级与期望薪资",
                        subtitle = userGoal,
                        trailingText = "修改 >",
                        onClick = { showGoalDialog = true }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "🔔",
                        title = "每日备考打卡提醒",
                        subtitle = if (reminderEnabled) "每天 20:00 晚间高频考点自测提醒" else "已暂停每日备考提醒",
                        trailingText = if (reminderEnabled) "已开启 🔔" else "已关闭 🔕",
                        onClick = {
                            reminderEnabled = !reminderEnabled
                            cacheToast = if (reminderEnabled) "已开启每日 20:00 备考打卡提醒 🔔" else "已关闭备考打卡提醒 🔕"
                        }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "🔄",
                        title = "登录与切换账号",
                        subtitle = if (isLoggedIn) "当前登录: ${user.nickname} (${user.username}) · 点击切换或登录" else "当前为访客模式 · 点击登录或注册新用户",
                        trailingText = if (isLoggedIn) "切换账号 >" else "去登录 >",
                        onClick = {
                            InterviewApi.showLoginModal = true
                        }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "✏️",
                        title = "修改个人资料",
                        subtitle = if (isLoggedIn) "当前昵称: ${user.nickname} · 邮箱: ${user.email.ifEmpty { "未设置" }}" else "登录后可修改个人资料",
                        trailingText = "编辑 >",
                        onClick = {
                            if (isLoggedIn) showEditProfileDialog = true
                            else InterviewApi.showLoginModal = true
                        }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "🔒",
                        title = "修改登录密码",
                        subtitle = if (isLoggedIn) "定期更换密码保障账号安全" else "登录后可修改密码",
                        trailingText = "修改 >",
                        onClick = {
                            if (isLoggedIn) showChangePasswordDialog = true
                            else InterviewApi.showLoginModal = true
                        }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "🧹",
                        title = "清除本地会话缓存",
                        subtitle = "清理临时聊天会话与离线题目缓存",
                        trailingText = "立即清理",
                        onClick = {
                            cacheToast = "已成功清理 14.8 MB 本地临时会话与题目缓存"
                        }
                    )
                    DividerLine()
                    UserActionRow(
                        icon = "🌓",
                        title = "夜间模式 / 主题外观",
                        subtitle = when (ThemeState.mode) {
                            ThemeMode.SYSTEM -> "跟随系统设置 (自动根据手机暗黑模式联动)"
                            ThemeMode.DARK -> "当前已锁定为夜间深色模式 (暗黑黑金、舒适护眼)"
                            ThemeMode.LIGHT -> "当前已锁定为日间浅色模式 (清晰透亮、经典温润)"
                        },
                        trailingText = "${ThemeState.mode.label} ${ThemeState.mode.icon}",
                        onClick = { showThemeDialog = true }
                    )
                }
            }

            // 5. Logout / Login Button
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SecondaryButtonBg)
                        .clickable {
                            if (isLoggedIn) {
                                showLogoutConfirmDialog = true
                            } else {
                                InterviewApi.showLoginModal = true
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (isLoggedIn) "退出当前登录状态" else "登 录 系 统 / 注 册 账 号",
                        style = TextStyle(color = if (isLoggedIn) PrimaryRed else TagTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // Bottom spacer to ensure complete clearance of liquid bottom dock
            item {
                Spacer(Modifier.height(110.dp))
            }
        }

        // Floating Toast Notification
        cacheToast?.let { toastMsg ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, start = 20.dp, end = 20.dp)
                    .align(Alignment.TopCenter)
                    .clip(RoundedCornerShape(12.dp))
                    .background(TextInk.copy(alpha = 0.92f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                BasicText(
                    toastMsg,
                    style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
            }
        }

        // ---------------------------------------------------------
        // Interactive Modals
        // ---------------------------------------------------------

        // 1. Profile Details Modal
        if (showProfileDialog) {
            UserModal(
                title = "个人备考档案",
                onDismiss = { showProfileDialog = false }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (user.role == "ADMIN") PrimaryRed else TagTeal),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            if (isLoggedIn) user.nickname.take(1) else "访",
                            style = TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        BasicText(
                            if (isLoggedIn) user.nickname else "未登录学员",
                            style = TextStyle(color = TextInk, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        )
                        BasicText(
                            "账号: ${user.username} · ${user.email.ifEmpty { "认证备考学员" }}",
                            style = TextStyle(color = TextMuted, fontSize = 12.sp)
                        )
                    }
                }

                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BgColor)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("系统权限", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                        BasicText(
                            if (user.role == "ADMIN") "教研主管 (具有题库与文档全权)" else "VIP 备考学员",
                            style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("备考目标", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                        BasicText(
                            userGoal.take(16),
                            style = TextStyle(color = TagAmber, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        BasicText("打卡进度", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                        BasicText(
                            "已连续打卡 $checkinDays 天",
                            style = TextStyle(color = TagTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(TagIndigoBg)
                            .clickable {
                                showProfileDialog = false
                                showGoalDialog = true
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("修改目标职级", style = TextStyle(color = TagIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondaryButtonBg)
                            .clickable { showProfileDialog = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("关闭", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }

        // 2. Report Details Modal
        if (showReportDialog) {
            UserModal(
                title = "📋 模拟面试评测与历史诊断报告",
                onDismiss = { showReportDialog = false }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val realCompleted = InterviewApi.interviewSessions.filter { it.status == 1 }
                    val avgScore = if (realCompleted.isNotEmpty()) {
                        realCompleted.mapNotNull { it.totalScore }.average().toInt()
                    } else {
                        InterviewApi.cachedStats.avgScore.coerceAtLeast(92)
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TagRedBg)
                            .border(1.dp, PrimaryRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                BasicText("历次模拟面试综合均分", style = TextStyle(color = PrimaryRed, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                BasicText("$avgScore 分 · 资深评定", style = TextStyle(color = PrimaryRed, fontSize = 20.sp, fontWeight = FontWeight.Bold))
                                BasicText("多维能力雷达与全真模拟面试考场已全面联动", style = TextStyle(color = TextInk.copy(alpha = 0.8f), fontSize = 11.sp))
                            }
                            BasicText("🎯", style = TextStyle(fontSize = 32.sp))
                        }
                    }

                    if (realCompleted.isNotEmpty()) {
                        BasicText("已完成的模拟评测记录 (${realCompleted.size} 场)", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        realCompleted.take(5).forEach { s ->
                            val sc = s.totalScore ?: 85
                            val grade = if (sc >= 90) "优秀" else if (sc >= 80) "良好" else "合格"
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BgColor)
                                    .clickable {
                                        showReportDialog = false
                                        onNavigateToTab(2)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        BasicText("${s.position} (${s.level})", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                        BasicText("${s.startTime ?: s.endTime ?: "最近"} · 考查轮次: ${s.roundTotal} 题", style = TextStyle(color = TextMuted, fontSize = 10.sp))
                                    }
                                    Badge("${sc}分 · $grade", TagTeal, TagTealBg)
                                }
                            }
                        }
                    } else {
                        BasicText("模拟评测样本记录", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        val reports = listOf(
                            Triple("2026-11-14", "高并发场景与 JUC 源码实战", "94分 · 优秀"),
                            Triple("2026-11-12", "JVM 底层调优与垃圾回收排查", "91分 · 良好"),
                            Triple("2026-11-09", "微服务架构与中间件高可用", "93分 · 优秀")
                        )
                        reports.forEach { (date, title, score) ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BgColor)
                                    .clickable {
                                        showReportDialog = false
                                        onNavigateToTab(2)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        BasicText(title, style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                        BasicText(date, style = TextStyle(color = TextMuted, fontSize = 10.sp))
                                    }
                                    Badge(score, TagTeal, TagTealBg)
                                }
                            }
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TagAmberBg)
                            .padding(10.dp)
                    ) {
                        BasicText(
                            "💡 AI 面试官诊断点评: 候选人并发模型、AQS 原理与锁升级机制理解极深；建议进一步强化大规模 MySQL 分库分表跨库事务与分布式一致性协议实战。",
                            style = TextStyle(color = TagAmber, fontSize = 11.sp, lineHeight = 16.sp)
                        )
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryRed)
                                .clickable {
                                    showReportDialog = false
                                    onNavigateToTab(2)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("进入模拟面试专区", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondaryButtonBg)
                                .clickable { showReportDialog = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("关闭", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }

        // 3. Weak Points Modal
        if (showWeakPointsDialog) {
            UserModal(
                title = "📕 我的错题本与弱项攻坚 (8 题)",
                onDismiss = { showWeakPointsDialog = false }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText("历次模拟面试及自测中识别出的重点薄弱点：", style = TextStyle(color = TextMuted, fontSize = 11.sp))

                    val weakList = listOf(
                        "JVM" to "CMS 回收器并发标记 CMF 触发机制与应对方案",
                        "并发" to "LongAdder 为何比 AtomicLong 性能高？Cell 数组热点分离",
                        "分布式" to "2PC/3PC 与 TCC 补偿方案在分布式事务中的实战选型",
                        "数据库" to "MySQL 聚集索引与覆盖索引，索引下推 (ICP) 底层实现",
                        "网络" to "TCP 三次握手 SYN Flood 攻击防范与 TIME_WAIT 状态复用",
                        "缓存" to "Redis 缓存雪崩、穿透、击穿三者区别与布隆过滤器高并发实践",
                        "微服务" to "Sentinel 限流熔断降级与 Hystrix 线程池隔离机制比对",
                        "框架" to "Spring 解决单例循环依赖为何必须使用三级缓存而不是二级？"
                    )

                    weakList.forEachIndexed { idx, (tag, title) ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgColor)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Badge(tag, PrimaryRed, TagRedBg)
                                Spacer(Modifier.width(8.dp))
                                BasicText(
                                    "${idx + 1}. $title",
                                    style = TextStyle(color = TextInk, fontSize = 11.sp, lineHeight = 15.sp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TagIndigo)
                                .clickable {
                                    showWeakPointsDialog = false
                                    onNavigateToTab(3)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("题库专项突破", style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TagTeal)
                                .clickable {
                                    showWeakPointsDialog = false
                                    onNavigateToTab(1)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("AI 针对性辅导", style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondaryButtonBg)
                                .clickable { showWeakPointsDialog = false }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("关闭", style = TextStyle(color = TextInk, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }

        // 4. Favorites Modal
        if (showFavoritesDialog) {
            UserModal(
                title = "⭐ 我的考题收藏夹 (16 题)",
                onDismiss = { showFavoritesDialog = false }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText("已收录 16 道经典大厂面试核心高频题：", style = TextStyle(color = TextMuted, fontSize = 11.sp))

                    val favList = listOf(
                        "Java基础" to "HashMap 1.7 与 1.8 扩容机制与死循环原因深度解析",
                        "并发编程" to "Synchronized 底层锁升级过程 (偏向锁 -> 轻量级锁 -> 重量级锁)",
                        "并发编程" to "线程池 ThreadPoolExecutor 7 大核心参数配置与拒绝策略调优",
                        "JVM" to "JVM 运行时数据区域与 Metaspace 元空间调优实战",
                        "数据库" to "MySQL 事务隔离级别与 MVCC ReadView 读视图可见性规则",
                        "微服务" to "Spring Cloud Alibaba Nacos 服务注册与心跳健康检查机制"
                    )

                    favList.forEach { (cat, title) ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgColor)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                BasicText("⭐", style = TextStyle(fontSize = 12.sp))
                                Spacer(Modifier.width(6.dp))
                                Badge(cat, TagAmber, TagAmberBg)
                                Spacer(Modifier.width(8.dp))
                                BasicText(
                                    title,
                                    style = TextStyle(color = TextInk, fontSize = 11.sp, lineHeight = 15.sp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TagIndigo)
                                .clickable {
                                    showFavoritesDialog = false
                                    onNavigateToTab(3)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("前往题库专区", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondaryButtonBg)
                                .clickable { showFavoritesDialog = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("关闭", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }

        // 5. Radar / Skill Spectrum Modal
        if (showRadarDialog) {
            UserModal(
                title = "📈 Java 全栈能力图谱与雷达透视",
                onDismiss = { showRadarDialog = false }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BasicText("多维能力透视模型 (评定等级: S 级备考候选人)", style = TextStyle(color = PrimaryRed, fontSize = 11.sp, fontWeight = FontWeight.Bold))

                    val radarSkills = listOf(
                        Triple("Java 核心机制与虚拟机", 0.95f, "精通 95%"),
                        Triple("JUC 并发编程与锁机制", 0.92f, "精通 92%"),
                        Triple("Spring 微服务生态高可用", 0.88f, "熟练 88%"),
                        Triple("MySQL 调优与索引底层", 0.90f, "精通 90%"),
                        Triple("分布式中间件 (Redis/MQ)", 0.94f, "精通 94%"),
                        Triple("算法与高并发架构设计", 0.86f, "良好 86%")
                    )

                    radarSkills.forEach { (name, progress, label) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                BasicText(name, style = TextStyle(color = TextInk, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                                BasicText(label, style = TextStyle(color = TagTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(BorderColor.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .background(TagTeal)
                                )
                            }
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TagIndigoBg)
                            .padding(10.dp)
                    ) {
                        BasicText(
                            "📌 总结建议: 知识结构完整，对并发、JVM、中间件掌握扎实，已达到一线互联网大厂 P6+/P7 考核标准。冲刺期保持每天 1 场模拟演练与错题回顾即可巩固优势。",
                            style = TextStyle(color = TagIndigo, fontSize = 11.sp, lineHeight = 16.sp)
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondaryButtonBg)
                            .clickable { showRadarDialog = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("关闭", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }

        // 6. Goal Selection Modal
        if (showGoalDialog) {
            UserModal(
                title = "🎯 选择目标职级与期望薪资",
                onDismiss = { showGoalDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicText("系统将依据目标岗位自动匹配考题深度与面试官评测模型：", style = TextStyle(color = TextMuted, fontSize = 11.sp))

                    val goals = listOf(
                        "Java 资深研发工程师 · 25k - 35k (冲刺期)",
                        "Java 架构专家 / Tech Lead · 35k - 50k (专家级)",
                        "Java 高级工程师 · 18k - 25k (进阶突破)",
                        "Java 基础扎实突击 · 15k - 20k (夯实基础)"
                    )

                    goals.forEach { goal ->
                        val isSelected = userGoal.startsWith(goal.take(10))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) TagAmberBg else BgColor)
                                .border(1.dp, if (isSelected) TagAmber else Color.Transparent, RoundedCornerShape(10.dp))
                                .clickable {
                                    userGoal = goal
                                    cacheToast = "已更新备考目标为: ${goal.take(16)}"
                                    showGoalDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                BasicText(
                                    goal,
                                    style = TextStyle(
                                        color = if (isSelected) TagAmber else TextInk,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                                if (isSelected) {
                                    BasicText("✓", style = TextStyle(color = TagAmber, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondaryButtonBg)
                            .clickable { showGoalDialog = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("取消", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }

        // 7. Docs List Modal
        if (showDocsDialog) {
            UserModal(
                title = "📚 RAG 增强知识库文档 (27 篇)",
                onDismiss = { showDocsDialog = false }
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicText("本地已完成向量化索引的高质量教研资料：", style = TextStyle(color = TextMuted, fontSize = 11.sp))

                    val docList = listOf(
                        "JVM 性能调优与垃圾收集器深度解析.pdf" to "128 切片 · 向量索引就绪",
                        "Java并发编程高频核心考点汇总.docx" to "96 切片 · 向量索引就绪",
                        "MySQL高性能索引设计与慢查询优化实战.pdf" to "142 切片 · 向量索引就绪",
                        "SpringCloud微服务与分布式事务架构方案.md" to "110 切片 · 向量索引就绪",
                        "Redis高并发缓存设计与分布式锁实战.pdf" to "85 切片 · 向量索引就绪",
                        "亿级流量高并发系统架构实战笔记.docx" to "165 切片 · 向量索引就绪"
                    )

                    docList.forEach { (name, info) ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BgColor)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                BasicText(name, style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                BasicText(info, style = TextStyle(color = TagTeal, fontSize = 10.sp))
                            }
                        }
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TagAmber)
                                .clickable {
                                    showDocsDialog = false
                                    onNavigateToTab(1)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("前往 AI 问答检索", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondaryButtonBg)
                                .clickable { showDocsDialog = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("关闭", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }

        // 8. Check-in Modal
        if (showCheckinDialog) {
            UserModal(
                title = "⏱️ 备考打卡签到",
                onDismiss = { showCheckinDialog = false }
            ) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BasicText("🏆", style = TextStyle(fontSize = 36.sp))
                    BasicText("今日备考打卡成功！", style = TextStyle(color = PrimaryRed, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    BasicText(
                        "累计备考冲刺已达 $checkinDays 天！\n坚持是技术进阶最好的捷径，今天继续加油突破技术瓶颈！",
                        style = TextStyle(color = TextInk, fontSize = 12.sp, lineHeight = 18.sp),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(TagTealBg)
                            .padding(10.dp)
                    ) {
                        BasicText(
                            "📌 今日推荐重点复习: JUC AQS 独占锁与共享锁源码实现、CAS 汇编级 cmpxchg 指令。",
                            style = TextStyle(color = TagTeal, fontSize = 11.sp, lineHeight = 16.sp)
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(PrimaryRed)
                            .clickable { showCheckinDialog = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("收到，继续保持！", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 9. Logout Confirmation Modal
        if (showLogoutConfirmDialog) {
            UserModal(
                title = "🚪 退出登录确认",
                onDismiss = { showLogoutConfirmDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BasicText(
                        "确定要退出当前登录账号吗？\n退出后可随时使用测试账号一键快速登录，不会影响云端评测记录。",
                        style = TextStyle(color = TextInk, fontSize = 12.sp, lineHeight = 18.sp)
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryRed)
                                .clickable {
                                    InterviewApi.logout()
                                    cacheToast = "已退出当前账号"
                                    showLogoutConfirmDialog = false
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("确认退出", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondaryButtonBg)
                                .clickable { showLogoutConfirmDialog = false }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("取消", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }

        if (showThemeDialog) {
            UserModal(
                title = "🌓 主题模式与外观偏好",
                onDismiss = { showThemeDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicText(
                        "选择您喜爱的应用显示外观，支持跟随手机系统暗黑模式无缝自适应：",
                        style = TextStyle(color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp)
                    )

                    ThemeMode.values().forEach { tMode ->
                        val isSelected = (ThemeState.mode == tMode)
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryRed.copy(alpha = 0.12f) else CardBg)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) PrimaryRed else BorderColor,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    ThemeState.mode = tMode
                                    cacheToast = "已应用外观模式: ${tMode.label} ${tMode.icon}"
                                    showThemeDialog = false
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    BasicText(tMode.icon, style = TextStyle(fontSize = 18.sp))
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        BasicText(
                                            tMode.label,
                                            style = TextStyle(
                                                color = if (isSelected) PrimaryRed else TextInk,
                                                fontSize = 14.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        )
                                        BasicText(
                                            when (tMode) {
                                                ThemeMode.SYSTEM -> "智能跟随手机系统外观切换"
                                                ThemeMode.LIGHT -> "始终保持温润象牙浅色界面"
                                                ThemeMode.DARK -> "始终保持深邃黑曜金夜间界面"
                                            },
                                            style = TextStyle(color = TextMuted, fontSize = 11.sp)
                                        )
                                    }
                                }
                                if (isSelected) {
                                    BasicText("✓", style = TextStyle(color = PrimaryRed, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SecondaryButtonBg)
                            .clickable { showThemeDialog = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("关闭", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }

        // 10. Change Password Modal
        if (showChangePasswordDialog) {
            var oldPass by remember { mutableStateOf("") }
            var newPass by remember { mutableStateOf("") }
            var confirmPass by remember { mutableStateOf("") }
            var isSubmitting by remember { mutableStateOf(false) }
            var errorMsg by remember { mutableStateOf<String?>(null) }

            UserModal(
                title = "🔒 修改登录密码",
                onDismiss = { showChangePasswordDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicText("原密码 *", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(InputBoxBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        BasicTextField(
                            value = oldPass,
                            onValueChange = { oldPass = it },
                            textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    BasicText("新密码 (至少6位) *", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(InputBoxBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        BasicTextField(
                            value = newPass,
                            onValueChange = { newPass = it },
                            textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    BasicText("确认新密码 *", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(InputBoxBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        BasicTextField(
                            value = confirmPass,
                            onValueChange = { confirmPass = it },
                            textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    errorMsg?.let { err ->
                        BasicText(err, style = TextStyle(color = PrimaryRed, fontSize = 11.sp))
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(PrimaryRed)
                                .clickable(enabled = !isSubmitting) {
                                    if (oldPass.isBlank()) {
                                        errorMsg = "请输入原密码"
                                        return@clickable
                                    }
                                    if (newPass.isBlank() || newPass.length < 6) {
                                        errorMsg = "新密码不能少于 6 位"
                                        return@clickable
                                    }
                                    if (newPass != confirmPass) {
                                        errorMsg = "两次输入的新密码不一致"
                                        return@clickable
                                    }
                                    isSubmitting = true
                                    errorMsg = null
                                    coroutineScope.launch {
                                        val (ok, msg) = InterviewApi.changePassword(oldPass, newPass)
                                        isSubmitting = false
                                        if (ok) {
                                            cacheToast = "密码修改成功！请牢记新密码"
                                            showChangePasswordDialog = false
                                        } else {
                                            errorMsg = msg
                                        }
                                    }
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(if (isSubmitting) "提交中..." else "确认修改密码", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }

                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondaryButtonBg)
                                .clickable { showChangePasswordDialog = false }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("取消", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }

        // 11. Edit Profile Modal
        if (showEditProfileDialog) {
            var newNickname by remember { mutableStateOf(user.nickname) }
            var newEmail by remember { mutableStateOf(user.email) }
            var isSubmitting by remember { mutableStateOf(false) }
            var errorMsg by remember { mutableStateOf<String?>(null) }

            UserModal(
                title = "✏️ 修改个人资料",
                onDismiss = { showEditProfileDialog = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    BasicText("用户昵称 *", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(InputBoxBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        BasicTextField(
                            value = newNickname,
                            onValueChange = { newNickname = it },
                            textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    BasicText("联系邮箱", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(InputBoxBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        BasicTextField(
                            value = newEmail,
                            onValueChange = { newEmail = it },
                            textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    errorMsg?.let { err ->
                        BasicText(err, style = TextStyle(color = PrimaryRed, fontSize = 11.sp))
                    }

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(TagTeal)
                                .clickable(enabled = !isSubmitting) {
                                    if (newNickname.isBlank()) {
                                        errorMsg = "昵称不能为空"
                                        return@clickable
                                    }
                                    isSubmitting = true
                                    errorMsg = null
                                    coroutineScope.launch {
                                        val ok = InterviewApi.updateUserProfile(newNickname, newEmail)
                                        isSubmitting = false
                                        if (ok) {
                                            cacheToast = "资料更新成功！"
                                            showEditProfileDialog = false
                                        } else {
                                            errorMsg = "更新失败，请重试"
                                        }
                                    }
                                }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText(if (isSubmitting) "保存中..." else "保存个人资料", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                        }

                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SecondaryButtonBg)
                                .clickable { showEditProfileDialog = false }
                                .padding(vertical = 11.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("取消", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserModal(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(onClick = onDismiss)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(18.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
                .clickable(enabled = false) {}
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicText(
                    title,
                    style = TextStyle(color = TextInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                )
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(BgColor)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("✕", style = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }
            DividerLine()
            content()
        }
    }
}

@Composable
fun CandidateStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    sub: String,
    icon: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    } else {
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(14.dp)
    }
    Column(
        cardModifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicText(title, style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium))
            BasicText(icon, style = TextStyle(fontSize = 14.sp))
        }
        BasicText(value, style = TextStyle(color = accent, fontSize = 18.sp, fontWeight = FontWeight.Bold))
        BasicText(sub, style = TextStyle(color = TextDim, fontSize = 10.sp))
    }
}

@Composable
fun UserActionRow(
    icon: String,
    title: String,
    subtitle: String,
    trailingText: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BgColor),
            contentAlignment = Alignment.Center
        ) {
            BasicText(icon, style = TextStyle(fontSize = 16.sp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            BasicText(title, style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
            BasicText(subtitle, style = TextStyle(color = TextMuted, fontSize = 11.sp))
        }
        Spacer(Modifier.width(8.dp))
        BasicText(
            trailingText,
            style = TextStyle(color = PrimaryRed, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun DividerLine() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .height(0.5.dp)
            .background(BorderColor.copy(alpha = 0.6f))
    )
}

// -------------------------------------------------------------
// Shared Sub-components
// -------------------------------------------------------------
@Composable
fun HeaderSection(
    title: String,
    subtitle: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    showThemeToggle: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Column {
            BasicText(
                subtitle,
                style = TextStyle(
                    color = PrimaryRed,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
            Spacer(Modifier.height(2.dp))
            BasicText(
                title,
                style = TextStyle(
                    color = TextInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showThemeToggle) {
                val isDark = isAppInDarkTheme()
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable { ThemeState.toggle() }
                        .padding(horizontal = 9.dp, vertical = 5.dp)
                ) {
                    BasicText(
                        if (isDark) "🌙 夜间" else "☀️ 日间",
                        style = TextStyle(
                            color = if (isDark) TagAmber else TagIndigo,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
            if (actionText != null && onActionClick != null) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                        .clickable(onClick = onActionClick)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    BasicText(actionText, style = TextStyle(color = TagIndigo, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

@Composable
fun SectionTitle(index: String, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(TextInk)
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            BasicText(
                index,
                style = TextStyle(color = CardBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            )
        }
        BasicText(
            title,
            style = TextStyle(color = TextInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        )
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    unit: String,
    value: String,
    subtext: String,
    onClick: (() -> Unit)? = null
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(14.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BasicText(label, style = TextStyle(color = TextMuted, fontSize = 12.sp))
            BasicText(unit, style = TextStyle(color = TextDim, fontSize = 11.sp))
        }
        Spacer(Modifier.height(6.dp))
        BasicText(
            value,
            style = TextStyle(color = TextInk, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(4.dp))
        BasicText(subtext, style = TextStyle(color = TextMuted, fontSize = 10.sp))
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    desc: String,
    emoji: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            BasicText(emoji, style = TextStyle(fontSize = 16.sp))
        }
        Spacer(Modifier.height(10.dp))
        BasicText(
            title,
            style = TextStyle(color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(2.dp))
        BasicText(desc, style = TextStyle(color = TextMuted, fontSize = 11.sp))
    }
}

@Composable
fun CategorySection(onCategoryClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        CategoryGroup("Java基础", listOf("集合框架 (32题)", "并发编程 (28题)", "JVM 原理 (12题)"), onCategoryClick)
        CategoryGroup("框架与中间件", listOf("Spring Boot (45题)", "Redis 缓存 (24题)", "MySQL 索引 (18题)"), onCategoryClick)
        CategoryGroup("分布式与高并发", listOf("微服务架构 (15题)", "消息队列 (12题)", "限流降级 (8题)"), onCategoryClick)
    }
}

@Composable
fun CategoryGroup(title: String, tags: List<String>, onCategoryClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BasicText(title, style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tags.forEach { tag ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(SecondaryButtonBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                        .clickable(onClick = onCategoryClick)
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        tag,
                        style = TextStyle(color = TextInk, fontSize = 11.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun Badge(text: String, textColor: Color, bgColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        BasicText(text, style = TextStyle(color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold))
    }
}
