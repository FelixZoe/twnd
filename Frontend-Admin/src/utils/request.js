import axios from "axios"
import router from "@/router"

const baseURL = "/api"

/**
 * Axios 实例 - 安全增强版
 */
const http = axios.create({
  baseURL,
  timeout: 300000,
  // 默认使用 JSON
  headers: {
    "Content-Type": "application/json",
  },
  // 禁止自动发送 cookies 到其他域
  withCredentials: false,
})

/**
 * 读取本地 Token（安全检查）
 * @returns {string}
 */
const getToken = () => {
  const token = localStorage.getItem("admin_token") || ""
  // 基础 token 格式校验 - 防止注入
  if (token && !/^[\w\-\.]+$/.test(token)) {
    console.warn("Invalid token format detected, clearing...")
    localStorage.removeItem("admin_token")
    return ""
  }
  return token
}

/**
 * 请求拦截器 - 自动附加 Token + 安全头
 */
http.interceptors.request.use(
  (config) => {
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers["Authorization"] = token
    }
    // 添加请求时间戳防止缓存（针对 GET）
    if (config.method === "get") {
      config.params = { ...config.params, _t: Date.now() }
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器 - 统一错误处理
 */
http.interceptors.response.use(
  (response) => {
    const { data } = response
    if (data?.code === 1) {
      return data
    }
    // 对错误消息做 XSS 防护 - 不直接渲染 HTML
    const safeMsg = (data?.msg || "请求失败").replace(/[<>]/g, "")
    ElMessage.error(safeMsg)
    return Promise.reject(data)
  },
  (error) => {
    const status = error?.response?.status
    if (status === 401) {
      // 防止多个并发请求同时 401 弹出多个提示
      if (!http._isRedirecting401) {
        http._isRedirecting401 = true
        ElMessage.warning("登录状态失效，请重新登录")
        localStorage.removeItem("admin_token")
        router.push("/login")
        setTimeout(() => {
          http._isRedirecting401 = false
        }, 2000)
      }
    } else if (status === 403) {
      ElMessage.error("权限不足，无法执行该操作")
    } else if (status === 429) {
      ElMessage.warning("操作过于频繁，请稍后再试")
    } else {
      // 尝试从响应体获取具体错误信息
      const respData = error?.response?.data
      const backendMsg = respData?.msg
      if (backendMsg) {
        const safeMsg = String(backendMsg).replace(/[<>]/g, "")
        ElMessage.error(safeMsg)
      } else if (!error?.response && error?.code === "ECONNABORTED") {
        ElMessage.error("请求超时，请检查网络后重试")
      } else if (!error?.response) {
        ElMessage.error("网络连接失败，请检查网络")
      } else {
        ElMessage.error("请求失败，请稍后重试")
      }
    }
    return Promise.reject(error)
  }
)

export default http
export { baseURL }
