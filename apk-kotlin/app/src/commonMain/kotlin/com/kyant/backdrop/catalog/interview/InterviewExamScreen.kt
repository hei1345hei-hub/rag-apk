package com.kyant.backdrop.catalog.interview

import androidx.compose.animation.*
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * 模拟面试全流程原生页面 (对标 Web 端 Interview.vue & InterviewReport.vue)
 * 包含：面试中心列表、创建面试弹窗、全真面试考场、能力评估报告
 */
@Composable
fun RealInterviewScreen() {
    val coroutineScope = rememberCoroutineScope()
    var selectedFilterStatus by remember { mutableStateOf<Int?>(null) } // null: 全部, 0: 进行中, 1: 已结束
    var showSetupModal by remember { mutableStateOf(false) }
    var inRoomSessionId by remember { mutableStateOf<Long?>(null) }
    var viewingReportSessionId by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(selectedFilterStatus) {
        InterviewApi.refreshInterviewSessions(selectedFilterStatus)
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // 1. 考场作答全屏页面 (覆盖在最顶层)
    if (inRoomSessionId != null) {
        InterviewRoomView(
            sessionId = inRoomSessionId!!,
            onExit = {
                inRoomSessionId = null
                coroutineScope.launch { InterviewApi.refreshInterviewSessions(selectedFilterStatus) }
            },
            onFinishToReport = { sid ->
                inRoomSessionId = null
                viewingReportSessionId = sid
                coroutineScope.launch { InterviewApi.refreshInterviewSessions(selectedFilterStatus) }
            }
        )
        return
    }

    // 2. 评估报告全屏弹窗
    if (viewingReportSessionId != null) {
        InterviewReportModal(
            sessionId = viewingReportSessionId!!,
            onDismiss = {
                viewingReportSessionId = null
                coroutineScope.launch { InterviewApi.refreshInterviewSessions(selectedFilterStatus) }
            }
        )
    }

    // 3. 新建面试配置弹窗
    if (showSetupModal) {
        InterviewSetupModal(
            onDismiss = { showSetupModal = false },
            onStartInterview = { sessionId ->
                showSetupModal = false
                inRoomSessionId = sessionId
            }
        )
    }

    // 4. 面试中心主界面列表
    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(BgColor),
        contentPadding = PaddingValues(start = 14.dp, top = statusBarTop + 10.dp, end = 14.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 页头
        item {
            HeaderSection(
                title = "模拟面试中心",
                subtitle = "AI MOCK INTERVIEW",
                actionText = if (InterviewApi.isLoadingInterviews) "加载中..." else "刷新记录",
                onActionClick = {
                    coroutineScope.launch { InterviewApi.refreshInterviewSessions(selectedFilterStatus) }
                }
            )
        }

        // 顶部行动卡片：发起全真面试
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CardBg)
                    .border(1.dp, PrimaryRed.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryRed.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("🎯", style = TextStyle(fontSize = 18.sp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                BasicText("AI 动态深度追问面试", style = TextStyle(color = TextInk, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                                BasicText("多轮递进考核 · 维度智能雷达评测", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                            }
                        }
                    }

                    BasicText(
                        "基于企业真实 Java 岗位画像与已上传知识库文档，AI 考官将根据你的每次作答针对性追问与多维度打分，并生成完整评估报告。",
                        style = TextStyle(color = TextMuted, fontSize = 12.sp, lineHeight = 17.sp)
                    )

                    Spacer(Modifier.height(4.dp))

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(PrimaryRed)
                            .clickable { showSetupModal = true }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("🚀 立即创建一场模拟面试", style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 状态筛选 Tab (全部 / 进行中 / 已结束)
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filters = listOf(null to "全部记录", 0 to "进行中", 1 to "已结束")
                filters.forEach { (st, label) ->
                    val selected = selectedFilterStatus == st
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) PrimaryRed else CardBg)
                            .border(1.dp, if (selected) PrimaryRed else BorderColor, RoundedCornerShape(14.dp))
                            .clickable { selectedFilterStatus = st }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        BasicText(
                            label,
                            style = TextStyle(
                                color = if (selected) Color.White else TextInk,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            }
        }

        // 面试历史记录列表
        val list = InterviewApi.interviewSessions
        if (list.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicText("📋", style = TextStyle(fontSize = 32.sp))
                        BasicText(
                            if (InterviewApi.isLoadingInterviews) "正在同步云端面试记录..." else "暂无该状态的面试记录",
                            style = TextStyle(color = TextMuted, fontSize = 13.sp)
                        )
                        if (!InterviewApi.isLoadingInterviews) {
                            BasicText(
                                "点击上方「立即创建一场模拟面试」即可开启实战",
                                style = TextStyle(color = TextDim, fontSize = 11.sp)
                            )
                        }
                    }
                }
            }
        } else {
            items(list, key = { it.id }) { session ->
                InterviewSessionCard(
                    session = session,
                    onContinue = { inRoomSessionId = session.id },
                    onViewReport = { viewingReportSessionId = session.id },
                    onDelete = {
                        coroutineScope.launch {
                            InterviewApi.deleteInterview(session.id)
                        }
                    }
                )
            }
        }
    }
}

/**
 * 面试记录卡片
 */
@Composable
fun InterviewSessionCard(
    session: InterviewSession,
    onContinue: () -> Unit,
    onViewReport: () -> Unit,
    onDelete: () -> Unit
) {
    val isOngoing = session.status == 0
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBg)
            .border(1.dp, if (isOngoing) TagAmber.copy(alpha = 0.4f) else BorderColor, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 头部：岗位 + 状态
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    BasicText(
                        session.position,
                        style = TextStyle(color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TagIndigo.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(session.level, style = TextStyle(color = TagIndigo, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                    }
                }

                // 状态 Tag
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isOngoing) TagAmber.copy(alpha = 0.15f) else Color(0xFF22C55E).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    BasicText(
                        if (isOngoing) "进行中 · ${session.roundDone}/${session.roundTotal}轮" else "已完成 · ${session.totalScore ?: 0}分",
                        style = TextStyle(color = if (isOngoing) TagAmber else Color(0xFF16A34A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // 属性标签行
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!session.category.isNullOrBlank()) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText("方向: ${session.category}", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                    }
                }
                if (!session.docName.isNullOrBlank()) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText("考卷: ${session.docName}", style = TextStyle(color = TextMuted, fontSize = 11.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (!session.startTime.isNullOrBlank()) {
                    BasicText(
                        session.startTime.take(16),
                        style = TextStyle(color = TextDim, fontSize = 11.sp),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            // 底部操作按钮行
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 删除按钮
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDelete() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    BasicText("删除", style = TextStyle(color = Color(0xFFEF4444), fontSize = 11.sp))
                }

                // 主操作按钮
                if (isOngoing) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(TagAmber)
                            .clickable { onContinue() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        BasicText("继续作答 →", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                } else {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(TagIndigo)
                            .clickable { onViewReport() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        BasicText("查看评估报告 📊", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

/**
 * 新建面试弹窗 (岗位、难度、轮数、方向选择)
 */
@Composable
fun InterviewSetupModal(
    onDismiss: () -> Unit,
    onStartInterview: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var position by remember { mutableStateOf("Java 后端开发工程师") }
    var level by remember { mutableStateOf("中级") }
    var roundTotal by remember { mutableIntStateOf(5) }
    var category by remember { mutableStateOf("综合考察") }
    var isCreating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val positions = listOf(
        "Java 后端开发工程师",
        "Java 高级开发工程师",
        "后端开发实习生",
        "大数据开发工程师",
        "全栈开发工程师"
    )
    val levels = listOf("初级", "中级", "高级")
    val rounds = listOf(3, 5, 8)
    val categories = listOf("综合考察", "Java基础", "并发编程", "JVM", "Spring", "MySQL", "Redis", "分布式")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(20.dp))
                .clickable(interactionSource = null, indication = null) {}
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText("🎯 创建模拟面试", style = TextStyle(color = TextInk, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(BgColor)
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("✕", style = TextStyle(color = TextMuted, fontSize = 13.sp))
                    }
                }

                // 目标岗位
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText("目标岗位", style = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(positions) { pos ->
                            val sel = position == pos
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) PrimaryRed else BgColor)
                                    .clickable { position = pos }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                BasicText(pos, style = TextStyle(color = if (sel) Color.White else TextInk, fontSize = 11.sp))
                            }
                        }
                    }
                }

                // 难度等级
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText("难度等级", style = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        levels.forEach { lv ->
                            val sel = level == lv
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) TagIndigo else BgColor)
                                    .clickable { level = lv }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText(lv, style = TextStyle(color = if (sel) Color.White else TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // 计划轮数
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText("计划提问轮数", style = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        rounds.forEach { r ->
                            val sel = roundTotal == r
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) TagAmber else BgColor)
                                    .clickable { roundTotal = r }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                BasicText("$r 轮考题", style = TextStyle(color = if (sel) Color.White else TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            }
                        }
                    }
                }

                // 考核方向
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BasicText("专项考核方向", style = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(categories) { cat ->
                            val sel = category == cat
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) PrimaryRed else BgColor)
                                    .clickable { category = cat }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                BasicText(cat, style = TextStyle(color = if (sel) Color.White else TextInk, fontSize = 11.sp))
                            }
                        }
                    }
                }

                if (errorMsg != null) {
                    BasicText(errorMsg!!, style = TextStyle(color = Color.Red, fontSize = 11.sp))
                }

                // 开始面试按钮
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryRed)
                        .clickable(enabled = !isCreating) {
                            isCreating = true
                            errorMsg = null
                            coroutineScope.launch {
                                val catParam = if (category == "综合考察") null else category
                                val result = InterviewApi.startNewInterview(position, level, catParam, roundTotal)
                                isCreating = false
                                if (result != null) {
                                    onStartInterview(result.first)
                                } else {
                                    errorMsg = "创建面试失败，请检查网络通信后重试"
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (isCreating) "AI 面试官准备考题中..." else "🚀 开始面试",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * 全真模拟面试考场作答全屏视图
 */
@Composable
fun InterviewRoomView(
    sessionId: Long,
    onExit: () -> Unit,
    onFinishToReport: (Long) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var session by remember { mutableStateOf<InterviewSession?>(null) }
    var currentQuestion by remember { mutableStateOf("") }
    var currentRoundNo by remember { mutableIntStateOf(1) }
    var userAnswer by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var lastComment by remember { mutableStateOf<String?>(null) }
    var lastScore by remember { mutableStateOf<Int?>(null) }
    var isFinished by remember { mutableStateOf(false) }
    var isEnding by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // 载入会话详情
    LaunchedEffect(sessionId) {
        val s = InterviewApi.fetchInterviewDetail(sessionId)
        if (s != null) {
            session = s
            val roundDone = s.roundDone
            if (roundDone >= s.roundTotal || s.status == 1) {
                isFinished = true
            } else {
                currentRoundNo = roundDone + 1
                val latestQa = s.qaList.find { it.roundNo == currentRoundNo }
                if (latestQa != null) {
                    currentQuestion = latestQa.question
                } else if (s.qaList.isNotEmpty()) {
                    currentQuestion = s.qaList.last().question
                }
            }
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Column(
        Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        // 考场顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .background(CardBg)
                .border(0.5.dp, BorderColor)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF22C55E))
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    BasicText("🎙️ 全真考场 · ${session?.position ?: "Java 面试"}", style = TextStyle(color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    BasicText(
                        "进度: 第 $currentRoundNo/${session?.roundTotal ?: 5} 轮 · ${session?.level ?: "中级"}",
                        style = TextStyle(color = TagAmber, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // 提前交卷按钮
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryRed.copy(alpha = 0.12f))
                        .clickable(enabled = !isEnding) {
                            isEnding = true
                            coroutineScope.launch {
                                val rep = InterviewApi.finishInterview(sessionId)
                                isEnding = false
                                if (rep != null) {
                                    onFinishToReport(sessionId)
                                }
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    BasicText(if (isEnding) "生成中..." else "提前交卷", style = TextStyle(color = PrimaryRed, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }

                // 退出考场按钮
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BgColor)
                        .clickable { onExit() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    BasicText("退出", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                }
            }
        }

        // 考场问答滚动区
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 历史轮次记录回顾
            val qaList = session?.qaList ?: emptyList()
            qaList.filter { it.roundNo < currentRoundNo && !it.answer.isNullOrBlank() }.forEach { qa ->
                item(key = "qa-${qa.roundNo}") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CardBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                BasicText("第 ${qa.roundNo} 轮提问", style = TextStyle(color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                BasicText("得分: ${qa.score ?: 0}分", style = TextStyle(color = if ((qa.score ?: 0) >= 80) Color(0xFF16A34A) else TagAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                            BasicText(qa.question, style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Medium))
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BgColor)
                                    .padding(8.dp)
                            ) {
                                BasicText("你的作答：${qa.answer}", style = TextStyle(color = TextMuted, fontSize = 12.sp, lineHeight = 16.sp))
                            }
                            if (!qa.comment.isNullOrBlank()) {
                                BasicText("考官点评：${qa.comment}", style = TextStyle(color = TagIndigo, fontSize = 11.sp, lineHeight = 15.sp))
                            }
                        }
                    }
                }
            }

            // 当前正在作答或展示评语的主题目区
            item(key = "current-qa") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardBg)
                        .border(1.5.dp, PrimaryRed.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PrimaryRed)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                BasicText("当前考题 · 第 $currentRoundNo 轮", style = TextStyle(color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                            }
                            if (lastScore != null) {
                                BasicText("本题得分: $lastScore 分", style = TextStyle(color = PrimaryRed, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                            }
                        }

                        BasicText(
                            currentQuestion.ifEmpty { "AI 考官正在分析岗位画像并拟定考题..." },
                            style = TextStyle(color = TextInk, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 21.sp)
                        )

                        // 刚提交完成时的点评展示
                        if (lastComment != null) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF22C55E).copy(alpha = 0.1f))
                                    .border(1.dp, Color(0xFF22C55E).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    BasicText("💡 AI 考官即时深度点评：", style = TextStyle(color = Color(0xFF15803D), fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                    BasicText(lastComment!!, style = TextStyle(color = TextInk, fontSize = 12.sp, lineHeight = 17.sp))
                                }
                            }
                        }
                    }
                }
            }

            // 当前轮次回答完毕，等待下一题或交卷
            if (lastComment != null) {
                item {
                    val totalRounds = session?.roundTotal ?: 5
                    if (currentRoundNo >= totalRounds) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PrimaryRed)
                                .clickable {
                                    coroutineScope.launch {
                                        val rep = InterviewApi.finishInterview(sessionId)
                                        if (rep != null) onFinishToReport(sessionId)
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("🎉 答卷全部完成，查看综合能力评估报告", style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        }
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(TagIndigo)
                                .clickable {
                                    coroutineScope.launch {
                                        lastComment = null
                                        lastScore = null
                                        userAnswer = ""
                                        val next = InterviewApi.nextInterviewQuestion(sessionId)
                                        if (next != null) {
                                            currentRoundNo = next.first
                                            currentQuestion = next.second
                                            InterviewApi.fetchInterviewDetail(sessionId)?.let { session = it }
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("下一题 · 进入第 ${currentRoundNo + 1} 轮深度追问 →", style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
        }

        // 底部作答输入区 (仅在未提交点评时展示)
        if (lastComment == null) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(CardBg)
                    .border(0.5.dp, BorderColor)
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    BasicText("✍️ 请在此输入你的技术解答：", style = TextStyle(color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    BasicText("${userAnswer.length} 字", style = TextStyle(color = TextDim, fontSize = 11.sp))
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 80.dp, max = 150.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(InputBoxBg)
                        .border(1.dp, BorderColor, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    if (userAnswer.isEmpty()) {
                        BasicText(
                            "结合原理、底层源码、并发安全与实际生产场景作答，得分更高...",
                            style = TextStyle(color = TextDim, fontSize = 13.sp)
                        )
                    }
                    BasicTextField(
                        value = userAnswer,
                        onValueChange = { userAnswer = it },
                        textStyle = TextStyle(color = TextInk, fontSize = 13.sp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 提交本轮回答按钮
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (userAnswer.trim().isNotEmpty() && !isSubmitting) PrimaryRed else PrimaryRed.copy(alpha = 0.5f))
                        .clickable(enabled = userAnswer.trim().isNotEmpty() && !isSubmitting) {
                            isSubmitting = true
                            coroutineScope.launch {
                                val result = InterviewApi.answerInterviewRound(sessionId, currentRoundNo, userAnswer.trim())
                                isSubmitting = false
                                if (result != null) {
                                    lastScore = result.first
                                    lastComment = result.second
                                    InterviewApi.fetchInterviewDetail(sessionId)?.let { session = it }
                                    listState.animateScrollToItem(listState.layoutInfo.totalItemsCount - 1)
                                }
                            }
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText(
                        if (isSubmitting) "AI 考官正在深度评审你的答案..." else "提交本轮回答",
                        style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

/**
 * 评估报告弹窗 (对标 Web 端 InterviewReport.vue)
 */
@Composable
fun InterviewReportModal(
    sessionId: Long,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var report by remember { mutableStateOf<InterviewReportData?>(null) }
    var session by remember { mutableStateOf<InterviewSession?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(sessionId) {
        isLoading = true
        report = InterviewApi.loadInterviewReport(sessionId)
        session = InterviewApi.fetchInterviewDetail(sessionId)
        isLoading = false
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(interactionSource = null, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(22.dp))
                .background(CardBg)
                .border(1.dp, BorderColor, RoundedCornerShape(22.dp))
                .clickable(interactionSource = null, indication = null) {}
                .padding(18.dp)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BasicText("正在拉取 AI 面试综合评估报告...", style = TextStyle(color = TextMuted, fontSize = 13.sp))
                }
            } else if (report == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BasicText("报告不存在或面试尚未结束", style = TextStyle(color = TextMuted, fontSize = 13.sp))
                }
            } else {
                val rep = report!!
                Column(Modifier.fillMaxSize()) {
                    // 报告顶栏
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            BasicText("📊 面试能力评估报告", style = TextStyle(color = TextInk, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                            BasicText("${session?.position ?: "Java 后端工程师"} · ${session?.level ?: "中级"}", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                        }
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BgColor)
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("✕", style = TextStyle(color = TextMuted, fontSize = 13.sp))
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // 滚动报告正文
                    Column(
                        Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // 1. 总分卡片与评级
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(PrimaryRed.copy(alpha = 0.08f))
                                .border(1.dp, PrimaryRed.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .padding(16.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    BasicText("综合得分", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        BasicText("${rep.totalScore}", style = TextStyle(color = PrimaryRed, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold))
                                        BasicText(" / 100", style = TextStyle(color = TextDim, fontSize = 13.sp))
                                    }
                                }

                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(PrimaryRed)
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    BasicText(rep.grade, style = TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                                }
                            }
                        }

                        // 2. 维度评分 (dimScores)
                        if (rep.dimScores.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                BasicText("维度能力雷达评分", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    rep.dimScores.entries.take(4).forEach { (dim, score) ->
                                        Box(
                                            Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(BgColor)
                                                .padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                                BasicText(dim, style = TextStyle(color = TextMuted, fontSize = 11.sp))
                                                BasicText("$score", style = TextStyle(color = if (score >= 80) Color(0xFF16A34A) else TagAmber, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 3. 综合评述
                        if (rep.summary.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                BasicText("📝 考官综合总结", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BgColor)
                                        .padding(12.dp)
                                ) {
                                    BasicText(rep.summary, style = TextStyle(color = TextInk, fontSize = 12.sp, lineHeight = 18.sp))
                                }
                            }
                        }

                        // 4. 优势分析
                        if (rep.strengths.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                BasicText("✨ 核心技术优势", style = TextStyle(color = Color(0xFF15803D), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF22C55E).copy(alpha = 0.08f))
                                        .padding(12.dp)
                                ) {
                                    BasicText(rep.strengths, style = TextStyle(color = TextInk, fontSize = 12.sp, lineHeight = 18.sp))
                                }
                            }
                        }

                        // 5. 待提升短板
                        if (rep.weaknesses.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                BasicText("⚠️ 待提升短板", style = TextStyle(color = TagAmber, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TagAmber.copy(alpha = 0.08f))
                                        .padding(12.dp)
                                ) {
                                    BasicText(rep.weaknesses, style = TextStyle(color = TextInk, fontSize = 12.sp, lineHeight = 18.sp))
                                }
                            }
                        }

                        // 6. 复习建议
                        if (rep.suggestion.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                BasicText("💡 后续提升建议", style = TextStyle(color = TagIndigo, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(TagIndigo.copy(alpha = 0.08f))
                                        .padding(12.dp)
                                ) {
                                    BasicText(rep.suggestion, style = TextStyle(color = TextInk, fontSize = 12.sp, lineHeight = 18.sp))
                                }
                            }
                        }

                        // 7. 问答题目与作答回顾
                        val qas = session?.qaList ?: emptyList()
                        if (qas.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                BasicText("逐题回顾与评分 (${qas.size} 题)", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                                qas.forEach { qa ->
                                    Box(
                                        Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(BgColor)
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                BasicText("第 ${qa.roundNo} 题", style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                                                BasicText("${qa.score ?: 0} 分", style = TextStyle(color = PrimaryRed, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                            }
                                            BasicText(qa.question, style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                                            if (!qa.answer.isNullOrBlank()) {
                                                BasicText("作答: ${qa.answer}", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
