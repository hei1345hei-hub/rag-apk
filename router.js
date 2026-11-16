import { createRouter, createWebHashHistory } from 'vue-router'
import { setRouter } from './utils/uni-shim'

import Login from './pages/login/login.vue'
import Dashboard from './pages/dashboard/dashboard.vue'
import Chat from './pages/chat/chat.vue'
import Interview from './pages/interview/interview.vue'
import Room from './pages/interview/room.vue'
import Report from './pages/interview/report.vue'
import Question from './pages/question/question.vue'
import Knowledge from './pages/knowledge/knowledge.vue'
import User from './pages/user/user.vue'

const routes = [
  {
    path: '/',
    redirect: '/pages/dashboard/dashboard'
  },
  {
    path: '/pages/login/login',
    name: 'login',
    component: Login,
    meta: { title: '登录 / 注册', isTab: false }
  },
  {
    path: '/pages/dashboard/dashboard',
    name: 'dashboard',
    component: Dashboard,
    meta: { title: '系统看板', isTab: true, tabKey: 'dashboard' }
  },
  {
    path: '/pages/chat/chat',
    name: 'chat',
    component: Chat,
    meta: { title: 'AI 问答', isTab: true, tabKey: 'chat' }
  },
  {
    path: '/pages/interview/interview',
    name: 'interview',
    component: Interview,
    meta: { title: '模拟面试', isTab: true, tabKey: 'interview' }
  },
  {
    path: '/pages/interview/room',
    name: 'room',
    component: Room,
    meta: { title: '实战面试考场', isTab: false }
  },
  {
    path: '/pages/interview/report',
    name: 'report',
    component: Report,
    meta: { title: '评估报告', isTab: false }
  },
  {
    path: '/pages/question/question',
    name: 'question',
    component: Question,
    meta: { title: '题库精选', isTab: true, tabKey: 'question' }
  },
  {
    path: '/pages/knowledge/knowledge',
    name: 'knowledge',
    component: Knowledge,
    meta: { title: '知识库管理', isTab: false }
  },
  {
    path: '/pages/user/user',
    name: 'user',
    component: User,
    meta: { title: '个人中心', isTab: true, tabKey: 'user' }
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

setRouter(router)

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('rag_token')
  if (to.name !== 'login' && !token) {
    next({ name: 'login' })
  } else {
    next()
  }
})

export default router
