package com.kyant.backdrop.catalog.interview

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KnowledgeScreen(
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    var selectedCategory by remember { mutableStateOf("全部") }
    var searchKeyword by remember { mutableStateOf("") }
    var toastMessage by remember { mutableStateOf<String?>(null) }

    // Modals
    var showUploadModal by remember { mutableStateOf(false) }
    var showChunkModal by remember { mutableStateOf(false) }
    var currentDocForChunks by remember { mutableStateOf<KnowledgeDoc?>(null) }
    var chunkList by remember { mutableStateOf<List<DocChunk>>(emptyList()) }
    var isChunksLoading by remember { mutableStateOf(false) }

    // Upload Form State
    var uploadMode by remember { mutableStateOf("text") } // "file" or "text"
    var uploadCategory by remember { mutableStateOf("Java基础") }
    var selectedLocalFileName by remember { mutableStateOf<String?>(null) }
    var selectedLocalFileBytes by remember { mutableStateOf<ByteArray?>(null) }
    var customDocTitle by remember { mutableStateOf("Java高频面试考点精选.md") }
    var customDocContent by remember { mutableStateOf("") }
    var isUploading by remember { mutableStateOf(false) }

    // File picker launcher
    val launchFilePicker = rememberPlatformFilePicker { name, bytes ->
        selectedLocalFileName = name
        selectedLocalFileBytes = bytes
        toastMessage = "已选择: $name (${bytes.size / 1024} KB)"
    }

    // Auto dismiss toast
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2800)
            toastMessage = null
        }
    }

    // Initial load
    LaunchedEffect(selectedCategory) {
        InterviewApi.refreshKnowledge(if (selectedCategory == "全部") null else selectedCategory, searchKeyword)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgColor)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = statusBarTop + 10.dp)
        ) {
            // 1. Top Header with Back button, Title & Upload Button
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(CardBg)
                            .border(1.dp, BorderColor, CircleShape)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("‹", style = TextStyle(color = TextInk, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                    }
                    Column {
                        BasicText("KNOWLEDGE BASE", style = TextStyle(color = PrimaryRed, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                        BasicText("知识文档与 RAG 解析", style = TextStyle(color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                    }
                }

                Box(
                    Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(PrimaryRed)
                        .clickable { showUploadModal = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    BasicText("+ 上传解析", style = TextStyle(color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                }
            }

            // 2. Knowledge Stats Strip (文档总数、切片总数、字符统计、解析中)
            val stats = InterviewApi.knowledgeStats
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp, horizontal = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText("${stats.docTotal}", style = TextStyle(color = TextInk, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                        BasicText("文档总数", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                    }
                    Box(Modifier.width(1.dp).height(24.dp).background(BorderColor))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText("${stats.chunkTotal}", style = TextStyle(color = TagTeal, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                        BasicText("向量切片", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                    }
                    Box(Modifier.width(1.dp).height(24.dp).background(BorderColor))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val charStr = if (stats.charTotal >= 10000) "${stats.charTotal / 1000}k" else "${stats.charTotal}"
                        BasicText(charStr, style = TextStyle(color = TagAmber, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                        BasicText("语义字符", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                    }
                    Box(Modifier.width(1.dp).height(24.dp).background(BorderColor))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        BasicText(
                            "${stats.parsing}",
                            style = TextStyle(
                                color = if (stats.parsing > 0) TagAmber else TagTeal,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        BasicText("解析中", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                    }
                }
            }

            // 3. Category Filter Chips (全部, Java基础, 并发, JVM...)
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InterviewApi.knowledgeCategories.forEach { cat ->
                    val isSelected = selectedCategory == cat
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) PrimaryRed else CardBg)
                            .border(1.dp, if (isSelected) PrimaryRed else BorderColor, RoundedCornerShape(16.dp))
                            .clickable {
                                selectedCategory = cat
                                coroutineScope.launch {
                                    InterviewApi.refreshKnowledge(if (cat == "全部") null else cat, searchKeyword)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            cat,
                            style = TextStyle(
                                color = if (isSelected) Color.White else TextInk,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // 4. Keyword Search Bar
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(InputBoxBg)
                    .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        BasicText("🔍", style = TextStyle(fontSize = 12.sp))
                        if (searchKeyword.isEmpty()) {
                            BasicText("搜索知识文档名称或考点...", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                        }
                        BasicTextField(
                            value = searchKeyword,
                            onValueChange = {
                                searchKeyword = it
                                coroutineScope.launch {
                                    InterviewApi.refreshKnowledge(if (selectedCategory == "全部") null else selectedCategory, it)
                                }
                            },
                            textStyle = TextStyle(color = TextInk, fontSize = 12.sp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchKeyword.isNotEmpty()) {
                        Box(
                            Modifier.clickable {
                                searchKeyword = ""
                                coroutineScope.launch {
                                    InterviewApi.refreshKnowledge(if (selectedCategory == "全部") null else selectedCategory, "")
                                }
                            }
                        ) {
                            BasicText("✕", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                        }
                    }
                }
            }

            // 5. Document List
            val docs = InterviewApi.knowledgeDocList
            if (InterviewApi.isLoadingKnowledge && docs.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    BasicText("正在加载知识文档库...", style = TextStyle(color = TextMuted, fontSize = 13.sp))
                }
            } else if (docs.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BasicText("📄", style = TextStyle(fontSize = 36.sp))
                        BasicText("暂无匹配的知识文档", style = TextStyle(color = TextInk, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                        BasicText("点击右上角「+ 上传解析」上传 Markdown 或 PDF 文档，系统将自动进行 RAG 语义切片与向量索引！", style = TextStyle(color = TextMuted, fontSize = 12.sp, lineHeight = 18.sp), modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(docs, key = { it.id }) { doc ->
                        DocCardItem(
                            doc = doc,
                            onViewChunks = {
                                currentDocForChunks = doc
                                showChunkModal = true
                                isChunksLoading = true
                                coroutineScope.launch {
                                    val chunks = InterviewApi.getChunks(doc.id)
                                    chunkList = chunks
                                    isChunksLoading = false
                                }
                            },
                            onReparse = {
                                coroutineScope.launch {
                                    val (ok, msg) = InterviewApi.reparseDoc(doc.id)
                                    toastMessage = if (ok) "已提交重新解析任务" else msg
                                }
                            },
                            onDelete = {
                                coroutineScope.launch {
                                    val (ok, msg) = InterviewApi.deleteDoc(doc.id)
                                    toastMessage = if (ok) "文档及向量切片已彻底删除" else msg
                                }
                            }
                        )
                    }
                }
            }
        }

        // Toast Message Notification Banner
        AnimatedVisibility(
            visible = toastMessage != null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = statusBarTop + 60.dp)
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                BasicText(toastMessage ?: "", style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium))
            }
        }

        // 6. Chunk Details Modal (查看切片列表)
        if (showChunkModal && currentDocForChunks != null) {
            val doc = currentDocForChunks!!
            UserModal(
                title = "切片明细 · ${doc.docName}",
                onDismiss = { showChunkModal = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicText("技术分类: ${doc.category}", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                        BasicText("总切片数: ${chunkList.size}", style = TextStyle(color = TagTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }

                    if (isChunksLoading) {
                        Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                            BasicText("正在拉取 RAG 向量切片...", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                        }
                    } else if (chunkList.isEmpty()) {
                        Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                            BasicText("暂无切片数据或解析尚未完成", style = TextStyle(color = TextMuted, fontSize = 12.sp))
                        }
                    } else {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 380.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            chunkList.forEach { chunk ->
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(InputBoxBg)
                                        .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(TagIndigo.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                BasicText("#${chunk.chunkIndex}", style = TextStyle(color = TagIndigo, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
                                            }
                                            BasicText("${chunk.content.length} 字符", style = TextStyle(color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace))
                                        }
                                        BasicText(
                                            chunk.content,
                                            style = TextStyle(color = TextInk, fontSize = 12.sp, lineHeight = 17.sp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecondaryButtonBg)
                            .clickable { showChunkModal = false }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("关 闭", style = TextStyle(color = TextInk, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }

        // 7. Upload & Parsing Modal (上传本地文件 或 编写/粘贴 Markdown)
        if (showUploadModal) {
            UserModal(
                title = "📤 上传知识文档并执行 RAG 解析",
                onDismiss = { showUploadModal = false }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Category Picker Row
                    BasicText("1. 选择所属技术分类：", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val cats = listOf("Java基础", "并发编程", "JVM", "Spring", "MySQL", "Redis", "分布式", "面试手册")
                        cats.forEach { cat ->
                            val isSel = uploadCategory == cat
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSel) PrimaryRed else InputBoxBg)
                                    .border(1.dp, if (isSel) PrimaryRed else BorderColor, RoundedCornerShape(12.dp))
                                    .clickable { uploadCategory = cat }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                BasicText(cat, style = TextStyle(color = if (isSel) Color.White else TextInk, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal))
                            }
                        }
                    }

                    // Mode Switcher (Tab 1: 编写/粘贴 Markdown, Tab 2: 浏览手机文件)
                    BasicText("2. 选择文档录入方式：", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecondaryButtonBg)
                            .padding(2.dp)
                    ) {
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (uploadMode == "text") CardBg else Color.Transparent)
                                .clickable { uploadMode = "text" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("✍️ 输入/粘贴 Markdown", style = TextStyle(color = if (uploadMode == "text") PrimaryRed else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (uploadMode == "file") CardBg else Color.Transparent)
                                .clickable { uploadMode = "file" }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicText("📁 从手机选择文件", style = TextStyle(color = if (uploadMode == "file") PrimaryRed else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }

                    if (uploadMode == "file") {
                        // File picker view
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(InputBoxBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                .clickable { launchFilePicker() }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                BasicText("📎", style = TextStyle(fontSize = 24.sp))
                                BasicText(
                                    selectedLocalFileName ?: "点击选择手机中的 .md 或 .pdf 文档",
                                    style = TextStyle(color = if (selectedLocalFileName != null) PrimaryRed else TextInk, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                )
                                BasicText("支持 Markdown 文档与 PDF 格式（最大 20MB）", style = TextStyle(color = TextMuted, fontSize = 10.sp))
                            }
                        }
                    } else {
                        // Text & Markdown input view
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            // Document title
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(InputBoxBg)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicText("文档标题: ", style = TextStyle(color = TextMuted, fontSize = 11.sp))
                                    BasicTextField(
                                        value = customDocTitle,
                                        onValueChange = { customDocTitle = it },
                                        textStyle = TextStyle(color = TextInk, fontSize = 11.sp, fontWeight = FontWeight.Bold),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // Preset Templates
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SecondaryButtonBg)
                                        .clickable {
                                            uploadCategory = "JVM"
                                            customDocTitle = "JVM垃圾回收三色标记与漏标防御.md"
                                            customDocContent = """
# JVM 垃圾回收三色标记与漏标防御

## 1. 三色标记定义
* **白色**：尚未被垃圾收集器访问过的对象。在可达性分析之初，所有对象都是白色的；若分析结束后仍为白色，代表不可达，将被回收。
* **灰色**：已被访问过，但该对象至少还有一个直接引用未被扫描完。
* **黑色**：已被访问过，且该对象的所有直接引用都已被扫描完。黑色对象一定是存活的。

## 2. 漏标的两个充要条件
1. 黑色对象插入了一条或多条指向白色对象的引用；
2. 灰色对象删除了该白色对象的直接或间接引用。

## 3. CMS 与 G1 解决方案
* **CMS 增量更新 (Incremental Update)**：破坏条件 1，当黑色对象插入新引用时记录下来，并发标记结束后重新标记。
* **G1 原始快照 (SATB)**：破坏条件 2，当灰色对象断开引用时记录被断开的白色对象，依然将其视为存活对象保留在本轮中。
                                            """.trimIndent()
                                            toastMessage = "已填入《JVM三色标记》模版"
                                        }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("JVM三色标记", style = TextStyle(color = TextInk, fontSize = 9.sp))
                                }

                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SecondaryButtonBg)
                                        .clickable {
                                            uploadCategory = "并发编程"
                                            customDocTitle = "ConcurrentHashMap并发原语与扩容机制.md"
                                            customDocContent = """
# ConcurrentHashMap 并发原语与扩容机制

## 1. JDK 1.8 架构演进
放弃了 1.7 的 Segment 分段锁设计，改用 **Node 数组 + CAS + synchronized**。
* **锁粒度**：只锁链表头节点或红黑树根节点，并发度大幅提升（理论支持数组长度个线程同时写入）；
* **hash 计算**：`(h ^ (h >>> 16)) & HASH_BITS`，强制将最高符号位转为 0，防止负数索引冲突。

## 2. 多线程协同协助扩容 (transfer)
* 引入 `ForwardingNode`（hash 值为 -1 的特殊节点）；
* 当某个桶的数据迁移完成后，用 ForwardingNode 占位；
* 其他线程 get 请求遇到 ForwardingNode 会自动转发到 `nextTable` 查询；put 请求遇到 ForwardingNode 则会协助参与其他未迁移桶的数据复制。
                                            """.trimIndent()
                                            toastMessage = "已填入《ConcurrentHashMap》模版"
                                        }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("ConcurrentMap", style = TextStyle(color = TextInk, fontSize = 9.sp))
                                }

                                Box(
                                    Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SecondaryButtonBg)
                                        .clickable {
                                            uploadCategory = "Spring"
                                            customDocTitle = "Spring三级缓存化解循环依赖全流程.md"
                                            customDocContent = """
# Spring 三级缓存化解循环依赖全流程

## 1. 三级缓存结构
* `singletonObjects`（一级缓存）：存放完全初始化好的成品 Bean；
* `earlySingletonObjects`（二级缓存）：存放提前暴露的半成品原始或代理 Bean；
* `singletonFactories`（三级缓存）：存放生成 Bean 的 `ObjectFactory` Lambda 表达式。

## 2. 为什么需要三级缓存而不是二级？
关键在于 **AOP 动态代理**。若只有两级缓存，任何 Bean 在实例化后必须立刻生成代理对象，违背了 Spring 设计中「在 Bean 生命周期完全初始化后再尝试创建代理」的核心原则。三级缓存实现了按需懒加载代理。
                                            """.trimIndent()
                                            toastMessage = "已填入《Spring三级缓存》模版"
                                        }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    BasicText("Spring三级缓存", style = TextStyle(color = TextInk, fontSize = 9.sp))
                                }
                            }

                            // Markdown content input
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(InputBoxBg)
                                    .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                if (customDocContent.isEmpty()) {
                                    BasicText("输入或粘贴 Markdown 面经文章，或点击上方预设模版快速填入...", style = TextStyle(color = TextMuted, fontSize = 11.sp, lineHeight = 16.sp))
                                }
                                BasicTextField(
                                    value = customDocContent,
                                    onValueChange = { customDocContent = it },
                                    textStyle = TextStyle(color = TextInk, fontSize = 11.sp, lineHeight = 16.sp),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }

                    // Upload Action Button
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isUploading) PrimaryRed.copy(alpha = 0.6f) else PrimaryRed)
                            .clickable(enabled = !isUploading) {
                                val fileName: String
                                val fileBytes: ByteArray

                                if (uploadMode == "file") {
                                    if (selectedLocalFileBytes == null || selectedLocalFileName == null) {
                                        toastMessage = "请先点击上方浏览并选择本地文档"
                                        return@clickable
                                    }
                                    fileName = selectedLocalFileName!!
                                    fileBytes = selectedLocalFileBytes!!
                                } else {
                                    if (customDocContent.isBlank()) {
                                        toastMessage = "请输入或粘贴 Markdown 技术文档正文"
                                        return@clickable
                                    }
                                    fileName = if (customDocTitle.endsWith(".md", ignoreCase = true)) customDocTitle else "$customDocTitle.md"
                                    fileBytes = customDocContent.toByteArray(Charsets.UTF_8)
                                }

                                coroutineScope.launch {
                                    isUploading = true
                                    val (ok, msg) = InterviewApi.uploadDoc(fileName, fileBytes, uploadCategory)
                                    isUploading = false
                                    if (ok) {
                                        toastMessage = "上传成功！正在后台进行 RAG 向量切片"
                                        showUploadModal = false
                                        selectedLocalFileName = null
                                        selectedLocalFileBytes = null
                                    } else {
                                        toastMessage = msg
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText(
                            if (isUploading) "正在上传并解析切片..." else "确定上传并启动 RAG 语义切片",
                            style = TextStyle(color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                    }

                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SecondaryButtonBg)
                            .clickable { showUploadModal = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        BasicText("取 消", style = TextStyle(color = TextInk, fontSize = 12.sp, fontWeight = FontWeight.Medium))
                    }
                }
            }
        }
    }
}

@Composable
private fun DocCardItem(
    doc: KnowledgeDoc,
    onViewChunks: () -> Unit,
    onReparse: () -> Unit,
    onDelete: () -> Unit
) {
    val statusText = when (doc.status) {
        0 -> "待处理"
        1 -> "解析中"
        2 -> "已完成"
        3 -> "解析失败"
        else -> "就绪"
    }
    val statusColor = when (doc.status) {
        0 -> TagIndigo
        1 -> TagAmber
        2 -> TagTeal
        3 -> TagRed
        else -> TagTeal
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBg)
            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Top: Doc title and format tag
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        if (doc.fileType.equals("pdf", ignoreCase = true)) "📕" else "📄",
                        style = TextStyle(fontSize = 16.sp)
                    )
                    BasicText(
                        doc.docName,
                        style = TextStyle(color = TextInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                }

                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    BasicText(statusText, style = TextStyle(color = statusColor, fontSize = 10.sp, fontWeight = FontWeight.Bold))
                }
            }

            // Mid: Category, Chunk Count, Time
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(SecondaryButtonBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        BasicText(doc.category, style = TextStyle(color = TextInk, fontSize = 10.sp))
                    }
                    BasicText("切片数: ${doc.chunkCount}", style = TextStyle(color = TagTeal, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold))
                }
                BasicText(doc.createTime, style = TextStyle(color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace))
            }

            Box(Modifier.fillMaxWidth().height(1.dp).background(BorderColor))

            // Bot: Actions (切片明细, 重新解析, 删除)
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (doc.status == 2) TagTeal.copy(alpha = 0.15f) else SecondaryButtonBg)
                            .clickable(enabled = doc.status == 2, onClick = onViewChunks)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        BasicText("切片明细", style = TextStyle(color = if (doc.status == 2) TagTeal else TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                    }

                    if (doc.status == 3) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TagAmber.copy(alpha = 0.15f))
                                .clickable(onClick = onReparse)
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            BasicText("重新解析", style = TextStyle(color = TagAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Box(
                    Modifier
                        .clickable(onClick = onDelete)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    BasicText("删除", style = TextStyle(color = TagRed, fontSize = 11.sp, fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}
