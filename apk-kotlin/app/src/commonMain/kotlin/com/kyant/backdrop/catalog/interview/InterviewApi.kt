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
}
