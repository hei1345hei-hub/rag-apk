package com.kyant.backdrop.catalog.interview

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object InterviewApi {
    var baseUrl by mutableStateOf("http://47.98.190.255:81/api")
    var token by mutableStateOf("eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIiwidXNlcklkIjoxLCJ1c2VybmFtZSI6ImFkbWluIiwicm9sZSI6IkFETUlOIiwiaWF0IjoxNzk0NzE3NDM0LCJleHAiOjE3OTUzMjIyMzR9.ZWhthimzz5vza2KHA6GsshQ8UxzBTtOcyDClKlu6Cx0")
    var currentUser by mutableStateOf(UserInfo(1, "admin", "系统管理员", "ADMIN", "admin@rag.com"))
    var isLoggedIn by mutableStateOf(true)
    var showLoginModal by mutableStateOf(false)

    // Cached states
    var cachedStats by mutableStateOf(DashboardStats())
    var cachedQuestions by mutableStateOf<List<QuestionItem>>(emptyList())
    var isLoadingQuestions by mutableStateOf(false)
    var isStatsLoading by mutableStateOf(false)

    // Knowledge & Document Parsing states
    var showKnowledgeScreen by mutableStateOf(false)
    var knowledgeStats by mutableStateOf(KnowledgeStats())
    var knowledgeCategories by mutableStateOf(listOf("全部", "Java基础", "并发编程", "JVM", "Spring", "MySQL", "Redis", "分布式", "面试手册"))
    var knowledgeDocList by mutableStateOf<List<KnowledgeDoc>>(emptyList())
    var isLoadingKnowledge by mutableStateOf(false)

    // Interview Management states
    var interviewSessions by mutableStateOf<List<InterviewSession>>(emptyList())
    var isLoadingInterviews by mutableStateOf(false)
    var currentInterviewSession by mutableStateOf<InterviewSession?>(null)
    var currentReportData by mutableStateOf<InterviewReportData?>(null)

    // Chat Management states
    var chatSessions by mutableStateOf<List<ChatSessionItem>>(emptyList())
    var currentChatSessionId by mutableStateOf<Long?>(null)
    var currentChatMode by mutableStateOf("RAG") // "RAG" or "CHAT"
    var selectedChatDocId by mutableStateOf<Long?>(null)
    var isLoadingChatSessions by mutableStateOf(false)

    // Question Bank states
    var randomQuizQuestions by mutableStateOf<List<QuestionItem>>(emptyList())
    var isLoadingQuiz by mutableStateOf(false)

    suspend fun login(username: String, pass: String): Boolean {
        return try {
            val (newToken, info) = HttpPlatform.login(baseUrl, username, pass)
            if (newToken != null && info != null) {
                token = newToken
                currentUser = info
                isLoggedIn = true
                showLoginModal = false
                refreshStats()
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    suspend fun register(username: String, pass: String, nickname: String, email: String): Pair<Boolean, String> {
        return try {
            HttpPlatform.register(baseUrl, username, pass, nickname, email)
        } catch (e: Exception) {
            false to (e.message ?: "网络通信异常")
        }
    }

    fun logout() {
        token = ""
        currentUser = UserInfo(0, "guest", "未登录学员", "GUEST", "")
        isLoggedIn = false
        showLoginModal = true
    }

    suspend fun refreshStats(): DashboardStats {
        isStatsLoading = true
        return try {
            val stats = HttpPlatform.fetchDashboardStats(baseUrl, token)
            if (stats != null) {
                cachedStats = stats
                stats
            } else {
                cachedStats
            }
        } catch (_: Exception) {
            cachedStats
        } finally {
            isStatsLoading = false
        }
    }

    suspend fun refreshQuestions(category: String? = null): List<QuestionItem> {
        isLoadingQuestions = true
        return try {
            val list = HttpPlatform.fetchQuestionList(baseUrl, token, category)
            if (list.isNotEmpty()) {
                cachedQuestions = list
                list
            } else {
                cachedQuestions
            }
        } catch (_: Exception) {
            cachedQuestions
        } finally {
            isLoadingQuestions = false
        }
    }

    suspend fun sendChatMessage(question: String): Pair<String, List<DocRef>> {
        return try {
            HttpPlatform.sendChatQuestion(baseUrl, token, question)
        } catch (e: Exception) {
            "无法连接知识库服务，请检查网络或后端通信状态。" to emptyList()
        }
    }

    suspend fun refreshKnowledge(category: String? = null, keyword: String? = null): List<KnowledgeDoc> {
        isLoadingKnowledge = true
        return try {
            val stats = HttpPlatform.fetchKnowledgeStats(baseUrl, token)
            if (stats != null) knowledgeStats = stats
            val cats = HttpPlatform.fetchKnowledgeCategories(baseUrl, token)
            if (cats.isNotEmpty()) knowledgeCategories = listOf("全部") + cats.filter { it != "全部" }
            val list = HttpPlatform.fetchDocuments(baseUrl, token, if (category == "全部") null else category, keyword, null)
            knowledgeDocList = list
            list
        } catch (_: Exception) {
            knowledgeDocList
        } finally {
            isLoadingKnowledge = false
        }
    }

    suspend fun uploadDoc(fileName: String, bytes: ByteArray, category: String?): Pair<Boolean, String> {
        val result = HttpPlatform.uploadKnowledgeDocument(baseUrl, token, fileName, bytes, category)
        if (result.first) {
            refreshKnowledge(category)
            refreshStats()
        }
        return result
    }

    suspend fun reparseDoc(docId: Long): Pair<Boolean, String> {
        val result = HttpPlatform.reparseDocument(baseUrl, token, docId)
        if (result.first) refreshKnowledge()
        return result
    }

    suspend fun deleteDoc(docId: Long): Pair<Boolean, String> {
        val result = HttpPlatform.deleteDocument(baseUrl, token, docId)
        if (result.first) {
            refreshKnowledge()
            refreshStats()
        }
        return result
    }

    suspend fun getChunks(docId: Long): List<DocChunk> {
        return HttpPlatform.fetchDocumentChunks(baseUrl, token, docId)
    }

    // -------------------------------------------------------------
    // Interview Session Methods
    // -------------------------------------------------------------
    suspend fun refreshInterviewSessions(status: Int? = null): List<InterviewSession> {
        isLoadingInterviews = true
        return try {
            val (_, list) = HttpPlatform.fetchInterviewSessions(baseUrl, token, 1, 20, status)
            interviewSessions = list
            list
        } catch (_: Exception) {
            interviewSessions
        } finally {
            isLoadingInterviews = false
        }
    }

    suspend fun startNewInterview(
        position: String,
        level: String,
        category: String?,
        roundTotal: Int,
        docId: Long? = null,
        docName: String? = null
    ): Triple<Long, String, Int>? {
        val result = HttpPlatform.createInterviewSession(baseUrl, token, position, level, category, roundTotal, docId, docName)
        if (result != null) {
            refreshInterviewSessions()
            refreshStats()
        }
        return result
    }

    suspend fun fetchInterviewDetail(sessionId: Long): InterviewSession? {
        val detail = HttpPlatform.fetchInterviewDetail(baseUrl, token, sessionId)
        if (detail != null) {
            currentInterviewSession = detail
        }
        return detail
    }

    suspend fun answerInterviewRound(sessionId: Long, roundNo: Int, answer: String): Pair<Int, String>? {
        return HttpPlatform.submitInterviewAnswer(baseUrl, token, sessionId, roundNo, answer)
    }

    suspend fun nextInterviewQuestion(sessionId: Long): Pair<Int, String>? {
        return HttpPlatform.fetchNextInterviewQuestion(baseUrl, token, sessionId)
    }

    suspend fun finishInterview(sessionId: Long): InterviewReportData? {
        val rep = HttpPlatform.finishInterviewSession(baseUrl, token, sessionId)
        currentReportData = rep
        refreshInterviewSessions()
        refreshStats()
        return rep
    }

    suspend fun loadInterviewReport(sessionId: Long): InterviewReportData? {
        val rep = HttpPlatform.fetchInterviewReport(baseUrl, token, sessionId)
        currentReportData = rep
        return rep
    }

    suspend fun deleteInterview(sessionId: Long): Boolean {
        val ok = HttpPlatform.deleteInterviewSession(baseUrl, token, sessionId)
        if (ok) {
            refreshInterviewSessions()
            refreshStats()
        }
        return ok
    }

    // -------------------------------------------------------------
    // Chat Session Methods
    // -------------------------------------------------------------
    suspend fun refreshChatSessions(sessionType: String = currentChatMode): List<ChatSessionItem> {
        isLoadingChatSessions = true
        return try {
            val list = HttpPlatform.fetchChatSessions(baseUrl, token, sessionType)
            chatSessions = list
            if (currentChatSessionId == null && list.isNotEmpty()) {
                currentChatSessionId = list[0].id
            }
            list
        } catch (_: Exception) {
            chatSessions
        } finally {
            isLoadingChatSessions = false
        }
    }

    suspend fun createNewChatSession(title: String? = null, sessionType: String = currentChatMode): ChatSessionItem? {
        val item = HttpPlatform.createChatSession(baseUrl, token, title, sessionType)
        if (item != null) {
            currentChatSessionId = item.id
            refreshChatSessions(sessionType)
        }
        return item
    }

    suspend fun loadChatMessages(sessionId: Long): List<ChatMessage> {
        return HttpPlatform.fetchChatMessages(baseUrl, token, sessionId)
    }

    suspend fun deleteChatSession(sessionId: Long): Boolean {
        val ok = HttpPlatform.deleteChatSession(baseUrl, token, sessionId)
        if (ok) {
            if (currentChatSessionId == sessionId) currentChatSessionId = null
            refreshChatSessions()
        }
        return ok
    }

    suspend fun sendChatMessageEx(question: String, docId: Long? = selectedChatDocId, topK: Int = 3): Triple<Long, String, List<DocRef>> {
        val result = HttpPlatform.sendChatQuestionEx(baseUrl, token, currentChatSessionId, question, topK, docId)
        if (currentChatSessionId == null && result.first > 0) {
            currentChatSessionId = result.first
            refreshChatSessions()
        }
        return result
    }

    // -------------------------------------------------------------
    // Question Bank Random Quiz & CRUD
    // -------------------------------------------------------------
    suspend fun loadRandomQuiz(category: String? = null, count: Int = 5): List<QuestionItem> {
        isLoadingQuiz = true
        return try {
            val list = HttpPlatform.fetchRandomQuestions(baseUrl, token, category, count)
            randomQuizQuestions = list
            list
        } catch (_: Exception) {
            randomQuizQuestions
        } finally {
            isLoadingQuiz = false
        }
    }

    suspend fun addQuestion(title: String, answer: String, category: String, difficulty: Int, tags: String): Boolean {
        val ok = HttpPlatform.addQuestion(baseUrl, token, title, answer, category, difficulty, tags)
        if (ok) {
            refreshQuestions()
            refreshStats()
        }
        return ok
    }

    suspend fun deleteQuestion(id: Int): Boolean {
        val ok = HttpPlatform.deleteQuestion(baseUrl, token, id)
        if (ok) {
            refreshQuestions()
            refreshStats()
        }
        return ok
    }

    // -------------------------------------------------------------
    // User Profile & Password
    // -------------------------------------------------------------
    suspend fun changePassword(oldPass: String, newPass: String): Pair<Boolean, String> {
        return HttpPlatform.changePassword(baseUrl, token, oldPass, newPass)
    }

    suspend fun updateUserProfile(nickname: String, email: String): Boolean {
        val ok = HttpPlatform.updateUserInfo(baseUrl, token, nickname, email)
        if (ok) {
            currentUser = currentUser.copy(nickname = nickname, email = email)
        }
        return ok
    }
}
