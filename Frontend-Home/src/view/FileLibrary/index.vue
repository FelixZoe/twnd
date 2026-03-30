<script setup>
import { onMounted, ref, computed, watch } from "vue"
import { getFileListAPI, getFileStatsAPI } from "@/api/fileLibrary"
import { getPersonalInfoAPI } from "@/api/personalInfo"

// 个人信息
const personalInfo = ref({})
// 文件列表
const fileList = ref([])
// 统计信息
const stats = ref([])
// 当前分类
const currentCategory = ref("")
// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
// 加载状态
const loading = ref(false)
// 预览
const previewVisible = ref(false)
const previewUrl = ref("")
const previewType = ref("")

// 主题
const isDarkMode = ref(false)

const initTheme = () => {
  const prefersDark = window.matchMedia("(prefers-color-scheme: dark)").matches
  isDarkMode.value = prefersDark
  applyTheme()
  window
    .matchMedia("(prefers-color-scheme: dark)")
    .addEventListener("change", (e) => {
      isDarkMode.value = e.matches
      applyTheme()
    })
}

const applyTheme = () => {
  const root = document.documentElement
  if (isDarkMode.value) {
    root.style.setProperty("--bg-light", "#1a1a1a")
    root.style.setProperty("--card-light", "#242424")
    root.style.setProperty("--border-light", "#333333")
    root.style.setProperty("--text-light", "#f0f0f0")
    root.style.setProperty("--muted-light", "#9e9e9e")
    root.style.setProperty("--hover-light", "#333333")
  } else {
    root.style.setProperty("--bg-light", "#fafafa")
    root.style.setProperty("--card-light", "#ffffff")
    root.style.setProperty("--border-light", "#e0e0e0")
    root.style.setProperty("--text-light", "#121212")
    root.style.setProperty("--muted-light", "#666666")
    root.style.setProperty("--hover-light", "#f0f0f0")
  }
}

onMounted(async () => {
  initTheme()
  await Promise.all([fetchStats(), fetchFiles(), fetchPersonalInfo()])
})

const fetchPersonalInfo = async () => {
  try {
    const res = await getPersonalInfoAPI()
    personalInfo.value = res.data.data || {}
  } catch (e) {
    console.error("获取个人信息失败:", e)
  }
}

const fetchStats = async () => {
  try {
    const res = await getFileStatsAPI()
    stats.value = res.data.data || []
  } catch (e) {
    console.error("获取统计失败:", e)
  }
}

const fetchFiles = async () => {
  loading.value = true
  try {
    const params = {
      page: currentPage.value,
      pageSize: pageSize.value
    }
    if (currentCategory.value) {
      params.category = currentCategory.value
    }
    const res = await getFileListAPI(params)
    const data = res.data.data || {}
    fileList.value = data.records || []
    total.value = data.total || 0
  } catch (e) {
    console.error("获取文件列表失败:", e)
    fileList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 分类切换
const changeCategory = (cat) => {
  currentCategory.value = cat
  currentPage.value = 1
  fetchFiles()
}

// 翻页
const handlePageChange = (page) => {
  currentPage.value = page
  fetchFiles()
}

// 总统计
const totalStat = computed(() => stats.value.find((s) => s.category === "all") || { count: 0, size: 0 })

// 分类统计（排除 all）
const categoryStats = computed(() => stats.value.filter((s) => s.category !== "all"))

// 分类名称映射
const categoryNames = {
  image: "图片",
  audio: "音频",
  video: "视频",
  document: "文档",
  other: "其他"
}

// 分类图标
const categoryIcons = {
  image: "📷",
  audio: "🎵",
  video: "🎬",
  document: "📄",
  other: "📦"
}

// 格式化文件大小
const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return "0 B"
  const units = ["B", "KB", "MB", "GB"]
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) {
    size /= 1024
    i++
  }
  return size.toFixed(i === 0 ? 0 : 1) + " " + units[i]
}

// 格式化日期
const formatDate = (timestamp) => {
  if (!timestamp) return ""
  const d = new Date(timestamp)
  return d.toLocaleDateString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit" })
}

// 判断是否图片
const isImage = (ext) => ["jpg", "jpeg", "png", "gif", "webp", "svg", "bmp", "avif", "ico"].includes(ext)

// 判断是否音频
const isAudio = (ext) => ["mp3", "wav", "ogg", "flac", "aac", "m4a"].includes(ext)

// 判断是否视频
const isVideo = (ext) => ["mp4", "webm", "mov", "avi", "mkv"].includes(ext)

// 预览文件
const previewFile = (file) => {
  if (isImage(file.extension)) {
    previewUrl.value = file.url
    previewType.value = "image"
    previewVisible.value = true
  } else if (isAudio(file.extension)) {
    previewUrl.value = file.url
    previewType.value = "audio"
    previewVisible.value = true
  } else if (isVideo(file.extension)) {
    previewUrl.value = file.url
    previewType.value = "video"
    previewVisible.value = true
  } else {
    window.open(file.url, "_blank")
  }
}

// 关闭预览
const closePreview = () => {
  previewVisible.value = false
  previewUrl.value = ""
  previewType.value = ""
}

// 复制链接
const copyUrl = async (url) => {
  try {
    await navigator.clipboard.writeText(url)
    ElMessage.success("链接已复制")
  } catch {
    ElMessage.error("复制失败")
  }
}

// 获取文件图标
const getFileIcon = (ext) => {
  if (isImage(ext)) return "📷"
  if (isAudio(ext)) return "🎵"
  if (isVideo(ext)) return "🎬"
  if (["pdf"].includes(ext)) return "📕"
  if (["doc", "docx"].includes(ext)) return "📝"
  if (["xls", "xlsx"].includes(ext)) return "📊"
  if (["ppt", "pptx"].includes(ext)) return "📑"
  if (["zip", "rar", "7z", "tar", "gz"].includes(ext)) return "📦"
  return "📄"
}
</script>

<template>
  <div class="file-library-container">
    <main class="card" role="main">
      <!-- 头部导航 -->
      <div class="header">
        <router-link to="/" class="back-link" title="返回主页">
          <span class="back-icon">←</span>
          <span>{{ personalInfo.nickname || "主页" }}</span>
        </router-link>
        <h1>文件库</h1>
        <p class="subtitle">共 {{ totalStat.count }} 个文件，{{ formatSize(totalStat.size) }}</p>
      </div>

      <!-- 分类标签 -->
      <div class="category-bar">
        <button
          class="category-chip"
          :class="{ active: currentCategory === '' }"
          @click="changeCategory('')"
        >
          全部
          <span class="chip-count">{{ totalStat.count }}</span>
        </button>
        <button
          v-for="cat in categoryStats"
          :key="cat.category"
          class="category-chip"
          :class="{ active: currentCategory === cat.category }"
          @click="changeCategory(cat.category)"
        >
          <span class="chip-icon">{{ categoryIcons[cat.category] || "📁" }}</span>
          {{ categoryNames[cat.category] || cat.category }}
          <span class="chip-count">{{ cat.count }}</span>
        </button>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <!-- 空状态 -->
      <div v-else-if="fileList.length === 0" class="empty-state">
        <span class="empty-icon">📂</span>
        <p>暂无文件</p>
      </div>

      <!-- 文件网格 -->
      <div v-else class="file-grid">
        <div
          v-for="file in fileList"
          :key="file.name"
          class="file-card"
          @click="previewFile(file)"
        >
          <!-- 缩略图/图标 -->
          <div class="file-thumb">
            <img
              v-if="isImage(file.extension)"
              :src="file.url"
              :alt="file.name"
              loading="lazy"
            />
            <span v-else class="file-icon">{{ getFileIcon(file.extension) }}</span>
          </div>
          <!-- 文件信息 -->
          <div class="file-info">
            <span class="file-ext">.{{ file.extension }}</span>
            <span class="file-size">{{ formatSize(file.size) }}</span>
          </div>
          <!-- 操作 -->
          <div class="file-actions" @click.stop>
            <button class="action-btn" @click="copyUrl(file.url)" title="复制链接">🔗</button>
            <a class="action-btn" :href="file.url" download :title="下载  + file.name">⬇️</a>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > pageSize" class="pagination">
        <button
          class="page-btn"
          :disabled="currentPage <= 1"
          @click="handlePageChange(currentPage - 1)"
        >
          ‹ 上一页
        </button>
        <span class="page-info">{{ currentPage }} / {{ Math.ceil(total / pageSize) }}</span>
        <button
          class="page-btn"
          :disabled="currentPage >= Math.ceil(total / pageSize)"
          @click="handlePageChange(currentPage + 1)"
        >
          下一页 ›
        </button>
      </div>
    </main>

    <!-- 预览弹窗 -->
    <div v-if="previewVisible" class="preview-overlay" @click="closePreview">
      <div class="preview-content" @click.stop>
        <button class="preview-close" @click="closePreview">✕</button>
        <img v-if="previewType === image" :src="previewUrl" class="preview-image" />
        <audio v-else-if="previewType === audio" :src="previewUrl" controls autoplay class="preview-audio" />
        <video v-else-if="previewType === video" :src="previewUrl" controls autoplay class="preview-video" />
      </div>
    </div>
  </div>
</template>

<style>
.file-library-container {
  width: 100vw;
  min-height: 100vh;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 2rem 1rem;
  background-color: var(--bg-light);
  transition: background-color 0.3s ease;
}

.file-library-container .card {
  width: 100%;
  max-width: 1200px;
  margin-inline: auto;
  padding: clamp(2rem, 5vw, 3.5rem);
  border-radius: 20px;
  background-color: var(--card-light);
  border: 1px solid var(--border-light);
  box-shadow: 0 4px 25px rgba(0, 0, 0, 0.06);
  transition: all 0.3s ease;
  animation: fadeIn 0.6s ease-out forwards;
}

/* 头部 */
.header {
  text-align: center;
  margin-bottom: 2rem;
  animation: fadeIn 0.5s ease-out 0.1s forwards;
  opacity: 0;
}

.back-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--muted-light);
  text-decoration: none;
  font-size: 0.95rem;
  margin-bottom: 1rem;
  transition: color 0.2s;
}

.back-link:hover {
  color: var(--text-light);
}

.back-icon {
  font-size: 1.2rem;
}

.header h1 {
  font-size: clamp(1.8rem, 5vw, 2.5rem);
  letter-spacing: -0.02em;
  margin: 0.5rem 0;
}

.subtitle {
  color: var(--muted-light);
  font-size: 1rem;
}

/* 分类标签 */
.category-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-bottom: 2rem;
  animation: fadeIn 0.5s ease-out 0.2s forwards;
  opacity: 0;
}

.category-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 20px;
  border: 1px solid var(--border-light);
  background: transparent;
  color: var(--text-light);
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
  font-family: inherit;
}

.category-chip:hover {
  background-color: var(--hover-light);
  border-color: var(--text-light);
}

.category-chip.active {
  background-color: var(--text-light);
  color: var(--card-light);
  border-color: var(--text-light);
}

.chip-count {
  font-size: 0.8rem;
  opacity: 0.7;
}

.chip-icon {
  font-size: 1rem;
}

/* 文件网格 */
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 16px;
  animation: fadeIn 0.5s ease-out 0.3s forwards;
  opacity: 0;
}

.file-card {
  border-radius: 14px;
  border: 1px solid var(--border-light);
  overflow: hidden;
  cursor: pointer;
  transition: all 0.25s ease;
  background-color: var(--card-light);
  position: relative;
}

.file-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: var(--text-light);
}

/* 缩略图 */
.file-thumb {
  width: 100%;
  height: 140px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-color: var(--hover-light);
}

.file-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.file-card:hover .file-thumb img {
  transform: scale(1.05);
}

.file-icon {
  font-size: 3rem;
}

/* 文件信息 */
.file-info {
  padding: 10px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.file-ext {
  font-weight: 600;
  font-size: 0.85rem;
  text-transform: uppercase;
  color: var(--text-light);
}

.file-size {
  font-size: 0.8rem;
  color: var(--muted-light);
}

/* 操作按钮 */
.file-actions {
  display: flex;
  gap: 4px;
  padding: 0 12px 10px;
}

.action-btn {
  padding: 4px 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 1rem;
  border-radius: 6px;
  transition: background 0.2s;
  text-decoration: none;
  display: inline-flex;
  align-items: center;
}

.action-btn:hover {
  background-color: var(--hover-light);
}

/* 分页 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 1.5rem;
  margin-top: 2rem;
  animation: fadeIn 0.5s ease-out 0.4s forwards;
  opacity: 0;
}

.page-btn {
  padding: 8px 18px;
  border-radius: 10px;
  border: 1px solid var(--border-light);
  background: transparent;
  color: var(--text-light);
  cursor: pointer;
  font-size: 0.9rem;
  transition: all 0.2s;
  font-family: inherit;
}

.page-btn:hover:not(:disabled) {
  background-color: var(--hover-light);
  border-color: var(--text-light);
}

.page-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.page-info {
  font-size: 0.9rem;
  color: var(--muted-light);
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 4rem 0;
  color: var(--muted-light);
  animation: fadeIn 0.5s ease-out 0.3s forwards;
  opacity: 0;
}

.empty-icon {
  font-size: 4rem;
  display: block;
  margin-bottom: 1rem;
}

/* 加载中 */
.loading-state {
  text-align: center;
  padding: 4rem 0;
  color: var(--muted-light);
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid var(--border-light);
  border-top-color: var(--text-light);
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
  margin: 0 auto 1rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* 预览 */
.preview-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  animation: fadeIn 0.2s ease;
}

.preview-content {
  position: relative;
  max-width: 90vw;
  max-height: 90vh;
}

.preview-close {
  position: absolute;
  top: -40px;
  right: 0;
  background: none;
  border: none;
  color: #fff;
  font-size: 1.5rem;
  cursor: pointer;
  padding: 4px 8px;
  opacity: 0.8;
  transition: opacity 0.2s;
}

.preview-close:hover {
  opacity: 1;
}

.preview-image {
  max-width: 90vw;
  max-height: 85vh;
  border-radius: 8px;
  object-fit: contain;
}

.preview-audio {
  width: 400px;
  max-width: 90vw;
}

.preview-video {
  max-width: 90vw;
  max-height: 85vh;
  border-radius: 8px;
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式 */
@media (max-width: 768px) {
  .file-library-container {
    padding: 1rem 0.5rem;
  }

  .file-library-container .card {
    padding: 1.5rem 1rem;
  }

  .file-grid {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
    gap: 10px;
  }

  .file-thumb {
    height: 110px;
  }
}

@media (max-width: 480px) {
  .file-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .file-thumb {
    height: 100px;
  }

  .file-info {
    padding: 8px 10px;
  }
}
</style>
