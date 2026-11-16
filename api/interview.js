import request from '@/utils/request'

export function getSessionList(params) {
  return request.get('/interview/sessions', { params })
}

export function getSessionDetail(id) {
  return request.get(`/interview/sessions/${id}`)
}

export function createSession(data) {
  return request.post('/interview/sessions', data)
}

export function nextQuestion(sessionId) {
  return request.post(`/interview/sessions/${sessionId}/next-question`)
}

export function submitAnswer(sessionId, data) {
  return request.post(`/interview/sessions/${sessionId}/answer`, data)
}

export function finishSession(sessionId) {
  return request.post(`/interview/sessions/${sessionId}/finish`)
}

export function deleteSession(id) {
  return request.delete(`/interview/sessions/${id}`)
}

export function getReport(sessionId) {
  return request.get(`/interview/sessions/${sessionId}/report`)
}
