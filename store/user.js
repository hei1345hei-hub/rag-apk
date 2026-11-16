import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { login as loginApi, getUserInfo } from '@/api/user'

const TOKEN_KEY = 'rag_token'
const USER_KEY = 'rag_user'

export const useUserStore = defineStore('user', () => {
  const token = ref(uni.getStorageSync(TOKEN_KEY) || '')
  let storedUser = null
  try {
    storedUser = uni.getStorageSync(USER_KEY) ? JSON.parse(uni.getStorageSync(USER_KEY)) : null
  } catch (e) {
    storedUser = null
  }
  const userInfo = ref(storedUser)

  const isLogin = computed(() => !!token.value)
  const isAdmin = computed(() => userInfo.value?.role === 'ADMIN')
  const nickname = computed(() => userInfo.value?.nickname || userInfo.value?.username || '未登录用户')

  function setToken(value) {
    token.value = value || ''
    if (value) {
      uni.setStorageSync(TOKEN_KEY, value)
    } else {
      uni.removeStorageSync(TOKEN_KEY)
    }
  }

  function setUserInfo(info) {
    userInfo.value = info || null
    if (info) {
      uni.setStorageSync(USER_KEY, JSON.stringify(info))
    } else {
      uni.removeStorageSync(USER_KEY)
    }
  }

  async function login(form) {
    const data = await loginApi(form)
    const tokenVal = data.token || data.accessToken
    const userVal = data.userInfo || data.user
    setToken(tokenVal)
    if (userVal) {
      setUserInfo(userVal)
    } else {
      await fetchUserInfo()
    }
    return data
  }

  async function fetchUserInfo() {
    try {
      const info = await getUserInfo()
      setUserInfo(info)
      return info
    } catch (e) {
      console.warn('获取用户信息失败', e)
    }
  }

  function logout() {
    setToken('')
    setUserInfo(null)
    uni.removeStorageSync(TOKEN_KEY)
    uni.removeStorageSync(USER_KEY)
    uni.reLaunch({
      url: '/pages/login/login'
    })
  }

  return {
    token,
    userInfo,
    isLogin,
    isAdmin,
    nickname,
    setToken,
    setUserInfo,
    login,
    fetchUserInfo,
    logout
  }
})
