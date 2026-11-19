package com.kyant.backdrop.catalog.interview

data class UserInfo(
    val id: Int = 1,
    val username: String = "admin",
    val nickname: String = "系统管理员",
    val role: String = "ADMIN",
    val email: String = "admin@rag.com"
)

data class DashboardStats(
    val docTotal: Int = 27,
    val chunkTotal: Int = 19,
    val questionTotal: Int = 72,
    val interviewTotal: Int = 0,
    val parsing: Int = 6,
    val failed: Int = 3,
    val avgScore: Int = 0,
    val chatTotal: Int = 2
)

data class QuestionItem(
    val id: Int,
    val title: String,
    val answer: String,
    val category: String,
    val difficulty: Int, // 1: 简单, 2: 中等, 3: 困难
    val tags: String,
    val frequency: Int = 100
)

data class DocRef(
    val docName: String,
    val snippet: String,
    val score: Double
)

data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val content: String,
    val refs: List<DocRef> = emptyList(),
    val timestamp: String = ""
)

data class MockScenario(
    val id: String,
    val title: String,
    val subtitle: String,
    val desc: String,
    val questionsCount: Int
)

data class InterviewStep(
    val index: Int,
    val question: String,
    var userAnswer: String = "",
    var score: Int? = null,
    var feedback: String = "",
    var isSubmitted: Boolean = false
)

data class KnowledgeDoc(
    val id: Long,
    val docName: String,
    val fileType: String = "MD",
    val fileSize: Long = 0L,
    val category: String = "Java基础",
    val status: Int = 2, // 0: 待处理, 1: 解析中, 2: 已完成, 3: 解析失败
    val chunkCount: Int = 0,
    val charCount: Int = 0,
    val errMsg: String = "",
    val createTime: String = ""
)

data class DocChunk(
    val id: Long,
    val docId: Long,
    val chunkIndex: Int,
    val content: String,
    val charCount: Int = 0
)

data class KnowledgeStats(
    val docTotal: Int = 27,
    val chunkTotal: Int = 19,
    val charTotal: Int = 84520,
    val parsing: Int = 0,
    val failed: Int = 0
)
