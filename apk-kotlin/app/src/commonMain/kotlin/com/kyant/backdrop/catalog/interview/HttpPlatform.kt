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
}

@Composable
expect fun rememberPlatformFilePicker(onFilePicked: (name: String, bytes: ByteArray) -> Unit): () -> Unit
