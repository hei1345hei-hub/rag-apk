import request from '@/utils/request'

export function getDocumentList(params) {
  return request.get('/knowledge/documents', { params })
}

export function uploadDocument(filePath, extra = {}) {
  return request.upload('/knowledge/upload', filePath, 'file', extra)
}

export function deleteDocument(id) {
  return request.delete(`/knowledge/documents/${id}`)
}

export function getChunkList(docId, params) {
  return request.get(`/knowledge/documents/${docId}/chunks`, { params })
}

export function reparseDocument(id) {
  return request.post(`/knowledge/documents/${id}/reparse`)
}

export function getKnowledgeStats() {
  return request.get('/knowledge/stats')
}

export function getCategories() {
  return request.get('/knowledge/categories')
}
