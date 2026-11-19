package com.kyant.backdrop.catalog.interview

import androidx.compose.runtime.Composable

expect object HttpPlatform {
    suspend fun login(baseUrl: String, username: String, pass: String): Pair<String?, UserInfo?>
    suspend fun register(baseUrl: String, username: String, pass: String, nickname: String, email: String): Pair<Boolean, String>
    suspend fun fetchDashboardStats(baseUrl: String, token: String?): DashboardStats?
    suspend fun fetchQuestionList(baseUrl: String, token: String?, category: String?): List<QuestionItem>
    suspend fun sendChatQuestion(baseUrl: String, token: String?, question: String): Pair<String, List<DocRef>>
    suspend fun fetchKnowledgeStats(baseUrl: String, token: String?): KnowledgeStats?
    suspend fun fetchKnowledgeCategories(baseUrl: String, token: String?): List<String>
    suspend fun fetchDocuments(baseUrl: String, token: String?, category: String?, keyword: String?, status: String?): List<KnowledgeDoc>
    suspend fun uploadKnowledgeDocument(baseUrl: String, token: String?, fileName: String, content: ByteArray, category: String?): Pair<Boolean, String>
    suspend fun fetchDocumentChunks(baseUrl: String, token: String?, docId: Long): List<DocChunk>
    suspend fun reparseDocument(baseUrl: String, token: String?, docId: Long): Pair<Boolean, String>
    suspend fun deleteDocument(baseUrl: String, token: String?, docId: Long): Pair<Boolean, String>

    // Interview APIs
    suspend fun fetchInterviewSessions(baseUrl: String, token: String?, pageNum: Int = 1, pageSize: Int = 10, status: Int? = null): Pair<Int, List<InterviewSession>>
    suspend fun createInterviewSession(baseUrl: String, token: String?, position: String, level: String, category: String?, roundTotal: Int, docId: Long?, docName: String?): Triple<Long, String, Int>?
    suspend fun fetchInterviewDetail(baseUrl: String, token: String?, sessionId: Long): InterviewSession?
    suspend fun submitInterviewAnswer(baseUrl: String, token: String?, sessionId: Long, roundNo: Int, answer: String): Pair<Int, String>?
    suspend fun fetchNextInterviewQuestion(baseUrl: String, token: String?, sessionId: Long): Pair<Int, String>?
    suspend fun finishInterviewSession(baseUrl: String, token: String?, sessionId: Long): InterviewReportData?
    suspend fun fetchInterviewReport(baseUrl: String, token: String?, sessionId: Long): InterviewReportData?
    suspend fun deleteInterviewSession(baseUrl: String, token: String?, sessionId: Long): Boolean

    // Chat Session APIs
    suspend fun fetchChatSessions(baseUrl: String, token: String?, sessionType: String = "RAG"): List<ChatSessionItem>
    suspend fun createChatSession(baseUrl: String, token: String?, title: String?, sessionType: String = "RAG"): ChatSessionItem?
    suspend fun fetchChatMessages(baseUrl: String, token: String?, sessionId: Long): List<ChatMessage>
    suspend fun deleteChatSession(baseUrl: String, token: String?, sessionId: Long): Boolean
    suspend fun sendChatQuestionEx(baseUrl: String, token: String?, sessionId: Long?, question: String, topK: Int = 3, docId: Long? = null): Triple<Long, String, List<DocRef>>

    // Question Bank APIs
    suspend fun fetchRandomQuestions(baseUrl: String, token: String?, category: String?, count: Int = 5): List<QuestionItem>
    suspend fun addQuestion(baseUrl: String, token: String?, title: String, answer: String, category: String, difficulty: Int, tags: String): Boolean
    suspend fun deleteQuestion(baseUrl: String, token: String?, id: Int): Boolean

    // User Profile APIs
    suspend fun changePassword(baseUrl: String, token: String?, oldPass: String, newPass: String): Pair<Boolean, String>
    suspend fun updateUserInfo(baseUrl: String, token: String?, nickname: String, email: String): Boolean
}

@Composable
expect fun rememberPlatformFilePicker(onFilePicked: (name: String, bytes: ByteArray) -> Unit): () -> Unit
