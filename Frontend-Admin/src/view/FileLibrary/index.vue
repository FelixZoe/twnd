<!-- build: 20260330-v3 FileLibrary · Premium UI Overhaul + Enhanced Features -->
<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { uploadFileLibrary, getFileList, getFileStats, deleteFile, batchDeleteFiles } from '@/api/file'

/* ================================================================
   State
   ================================================================ */
const loading = ref(false)
const uploadLoading = ref(false)
const fileList = ref([])
const stats = ref([])
const selectedFiles = ref([])
const isDragging = ref(false)
const viewMode = ref(localStorage.getItem('fl_viewMode') || 'grid')
const searchKeyword = ref('')
const searchFocused = ref(false)
const uploadProgress = ref(0)
const uploadFileName = ref('')
const uploadQueue = ref([])
const uploadQueueIndex = ref(0)
const sortField = ref('modifiedTime')
const sortOrder = ref('desc')
const showFileDetail = ref(false)
const detailFile = ref(null)
const contextMenu = reactive({ visible: false, x: 0, y: 0, file: null })

/* rename */
const renamingFile = ref(null)
const renameValue = ref('')

/* preview / lightbox */
const previewVisible = ref(false)
const previewUrl = ref('')
const previewType = ref('')
const previewName = ref('')
const previewIndex = ref(-1)
const previewZoom = ref(1)
const previewDrag = reactive({ active: false, x: 0, y: 0, startX: 0, startY: 0 })

/* multi-file upload queue */
const multiUploadFiles = ref([])  // {name, progress, status: 'pending'|'uploading'|'done'|'error'}
const showUploadPanel = ref(false)

/* toast */
const toasts = ref([])
let toastId = 0

const pagination = reactive({ page: 1, pageSize: 24, total: 0 })
const filter = reactive({ category: '' })

/* persist viewMode */
watch(viewMode, (v) => localStorage.setItem('fl_viewMode', v))

/* ================================================================
   Toast system
   ================================================================ */
const addToast = (msg, type = 'success', duration = 2500) => {
  const id = ++toastId
  toasts.value.push({ id, msg, type })
  setTimeout(() => {
    toasts.value = toasts.value.filter(t => t.id !== id)
  }, duration)
}

/* ================================================================
   Category definitions
   ================================================================ */
const catMap = {
  image: '图片', audio: '音频', video: '视频', text: '文本',
  pdf: 'PDF', word: 'Word', excel: 'Excel', archive: '压缩包',
  font: '字体', lyric: '歌词', other: '其他',
}

const catIcons = {
  '': `<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M2 4a2 2 0 012-2h3.172a2 2 0 011.414.586l.828.828A2 2 0 0010.828 4H12a2 2 0 012 2v6a2 2 0 01-2 2H4a2 2 0 01-2-2V4z" stroke="currentColor" stroke-width="1.5" fill="none"/></svg>`,
  image: `<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="2" y="2" width="12" height="12" rx="2" stroke="currentColor" stroke-width="1.5"/><circle cx="5.5" cy="5.5" r="1.5" fill="currentColor"/><path d="M2 11l3-3 2 2 3-3 4 4v1a2 2 0 01-2 2H4a2 2 0 01-2-2v-1z" fill="currentColor" opacity="0.3"/></svg>`,
  audio: `<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><path d="M12 2v9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/><circle cx="9" cy="11" r="3" stroke="currentColor" stroke-width="1.5"/><path d="M12 2l-4 2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>`,
  video: `<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="1" y="3" width="10" height="10" rx="2" stroke="currentColor" stroke-width="1.5"/><path d="M11 6l4-2v8l-4-2" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>`,
  text: `<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="3" y="1" width="10" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/><line x1="5.5" y1="5" x2="10.5" y2="5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><line x1="5.5" y1="8" x2="10.5" y2="8" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/><line x1="5.5" y1="11" x2="8.5" y2="11" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>`,
  pdf: `<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="3" y="1" width="10" height="14" rx="2" stroke="currentColor" stroke-width="1.5"/><text x="8" y="10.5" text-anchor="middle" font-size="5" font-weight="700" fill="currentColor">P</text></svg>`,
  archive: `<svg width="16" height="16" viewBox="0 0 16 16" fill="none"><rect x="3" y="2" width="10" height="12" rx="2" stroke="currentColor" stroke-width="1.5"/><line x1="8" y1="2" x2="8" y2="9" stroke="currentColor" stroke-width="1.5"/><rect x="6.5" y="9" width="3" height="2" rx="0.5" stroke="currentColor" stroke-width="1"/></svg>`,
}

const categories = computed(() => {
  const cats = [{ label: '全部', value: '', count: 0, size: 0 }]
  if (stats.value.length) {
    const allStat = stats.value.find(s => s.category === 'all')
    if (allStat) { cats[0].count = allStat.count; cats[0].size = allStat.size }
    stats.value.filter(s => s.category !== 'all').forEach(s => {
      cats.push({ label: catMap[s.category] || s.category, value: s.category, count: s.count, size: s.size })
    })
  }
  return cats
})

const totalUsage = computed(() => {
  const s = stats.value.find(s => s.category === 'all')
  return s ? s.size : 0
})
const totalCount = computed(() => {
  const s = stats.value.find(s => s.category === 'all')
  return s ? s.count : 0
})

/* ================================================================
   Computed: sorted + filtered file list
   ================================================================ */
const filteredFileList = computed(() => {
  let list = [...fileList.value]
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.toLowerCase().trim()
    list = list.filter(f => f.name.toLowerCase().includes(kw))
  }
  list.sort((a, b) => {
    let va = a[sortField.value], vb = b[sortField.value]
    if (sortField.value === 'name') { va = va.toLowerCase(); vb = vb.toLowerCase() }
    if (va < vb) return sortOrder.value === 'asc' ? -1 : 1
    if (va > vb) return sortOrder.value === 'asc' ? 1 : -1
    return 0
  })
  return list
})

const previewableFiles = computed(() =>
  filteredFileList.value.filter(f => isImage(f.extension) || isAudio(f.extension) || isVideo(f.extension))
)

const allSelected = computed(() =>
  filteredFileList.value.length > 0 && selectedFiles.value.length === filteredFileList.value.length
)

/* ================================================================
   Utility functions
   ================================================================ */
const formatSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let i = 0, size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return size.toFixed(i > 0 ? 1 : 0) + ' ' + units[i]
}

const formatDate = (ts) => {
  if (!ts) return '-'
  const d = new Date(ts)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  if (diff < 604800000) return Math.floor(diff / 86400000) + '天前'
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

const formatDateFull = (ts) => {
  if (!ts) return '-'
  const d = new Date(ts)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`
}

const isImage = ext => ['jpg','jpeg','png','gif','webp','bmp','svg','ico','avif'].includes(ext)
const isAudio = ext => ['mp3','wav','ogg','aac','flac','m4a','wma'].includes(ext)
const isVideo = ext => ['mp4','avi','mov','mkv','wmv','flv','webm'].includes(ext)

const fileTypeColors = {
  image:  { bg: '#ecfdf5', fg: '#059669', accent: '#10b981', glow: 'rgba(16,185,129,0.1)' },
  audio:  { bg: '#fffbeb', fg: '#d97706', accent: '#f59e0b', glow: 'rgba(245,158,11,0.1)' },
  video:  { bg: '#eff6ff', fg: '#2563eb', accent: '#3b82f6', glow: 'rgba(59,130,246,0.1)' },
  pdf:    { bg: '#fef2f2', fg: '#dc2626', accent: '#ef4444', glow: 'rgba(239,68,68,0.1)' },
  word:   { bg: '#eff6ff', fg: '#2563eb', accent: '#3b82f6', glow: 'rgba(59,130,246,0.1)' },
  excel:  { bg: '#ecfdf5', fg: '#059669', accent: '#10b981', glow: 'rgba(16,185,129,0.1)' },
  archive:{ bg: '#fefce8', fg: '#ca8a04', accent: '#eab308', glow: 'rgba(234,179,8,0.1)' },
  other:  { bg: '#f8fafc', fg: '#64748b', accent: '#94a3b8', glow: 'rgba(148,163,184,0.1)' },
}

const getFileTypeKey = ext => {
  if (isImage(ext)) return 'image'
  if (isAudio(ext)) return 'audio'
  if (isVideo(ext)) return 'video'
  if (['pdf'].includes(ext)) return 'pdf'
  if (['doc','docx'].includes(ext)) return 'word'
  if (['xls','xlsx'].includes(ext)) return 'excel'
  if (['zip','rar','7z','tar','gz'].includes(ext)) return 'archive'
  return 'other'
}

const getFileColors = ext => fileTypeColors[getFileTypeKey(ext)] || fileTypeColors.other

const getFileTypeLabel = ext => {
  const key = getFileTypeKey(ext)
  const labels = { image:'图片', audio:'音频', video:'视频', pdf:'PDF', word:'Word', excel:'Excel', archive:'压缩包', other:'文件' }
  return labels[key] || '文件'
}

/* ================================================================
   Data fetching
   ================================================================ */
const fetchStats = async () => {
  try { const res = await getFileStats(); stats.value = res.data || [] }
  catch (e) { console.error('stats err', e) }
}

const fetchFiles = async () => {
  loading.value = true
  try {
    const params = { page: pagination.page, pageSize: pagination.pageSize }
    if (filter.category) params.category = filter.category
    const res = await getFileList(params)
    const data = res.data || {}
    fileList.value = data.records || []
    pagination.total = data.total || 0
  } catch (e) { console.error('list err', e) }
  finally { loading.value = false }
}

/* ================================================================
   Category switching
   ================================================================ */
const handleCategoryChange = cat => {
  filter.category = cat
  pagination.page = 1
  selectedFiles.value = []
  searchKeyword.value = ''
  fetchFiles()
}

/* ================================================================
   Pagination
   ================================================================ */
const handlePageChange = p => { pagination.page = p; selectedFiles.value = []; fetchFiles() }
const handleSizeChange = s => { pagination.pageSize = s; pagination.page = 1; selectedFiles.value = []; fetchFiles() }

/* ================================================================
   Sorting
   ================================================================ */
const handleSort = field => {
  if (sortField.value === field) {
    sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortField.value = field
    sortOrder.value = field === 'name' ? 'asc' : 'desc'
  }
}

/* ================================================================
   Selection (supports Shift+click range select)
   ================================================================ */
let lastSelectedIndex = -1
const isSelected = file => selectedFiles.value.includes(file.name)

const toggleSelect = (file, event) => {
  const idx = filteredFileList.value.findIndex(f => f.name === file.name)

  if (event && event.shiftKey && lastSelectedIndex >= 0) {
    const start = Math.min(lastSelectedIndex, idx)
    const end = Math.max(lastSelectedIndex, idx)
    const rangeNames = filteredFileList.value.slice(start, end + 1).map(f => f.name)
    const merged = new Set([...selectedFiles.value, ...rangeNames])
    selectedFiles.value = [...merged]
  } else {
    const pos = selectedFiles.value.indexOf(file.name)
    if (pos >= 0) selectedFiles.value.splice(pos, 1)
    else selectedFiles.value.push(file.name)
  }
  lastSelectedIndex = idx
}

const selectAll = () => {
  if (allSelected.value) selectedFiles.value = []
  else selectedFiles.value = filteredFileList.value.map(f => f.name)
}

/* ================================================================
   Preview / Lightbox
   ================================================================ */
const handlePreview = file => {
  if (isImage(file.extension) || isAudio(file.extension) || isVideo(file.extension)) {
    previewUrl.value = file.url
    previewName.value = file.name
    previewType.value = isImage(file.extension) ? 'image' : isAudio(file.extension) ? 'audio' : 'video'
    previewIndex.value = previewableFiles.value.findIndex(f => f.name === file.name)
    previewZoom.value = 1
    previewDrag.x = 0
    previewDrag.y = 0
    previewVisible.value = true
  } else {
    window.open(file.url, '_blank')
  }
}

const navigatePreview = dir => {
  const next = previewIndex.value + dir
  if (next < 0 || next >= previewableFiles.value.length) return
  previewIndex.value = next
  const f = previewableFiles.value[next]
  previewUrl.value = f.url
  previewName.value = f.name
  previewType.value = isImage(f.extension) ? 'image' : isAudio(f.extension) ? 'audio' : 'video'
  previewZoom.value = 1
  previewDrag.x = 0
  previewDrag.y = 0
}

/* zoom */
const handleLightboxWheel = e => {
  if (!previewVisible.value || previewType.value !== 'image') return
  e.preventDefault()
  const delta = e.deltaY > 0 ? -0.15 : 0.15
  previewZoom.value = Math.max(0.3, Math.min(5, previewZoom.value + delta))
}

/* ================================================================
   File detail sidebar
   ================================================================ */
const openDetail = file => {
  detailFile.value = file
  showFileDetail.value = true
}
const closeDetail = () => { showFileDetail.value = false }

/* ================================================================
   Copy URL (with Markdown support)
   ================================================================ */
const handleCopyUrl = async file => {
  try {
    await navigator.clipboard.writeText(file.url)
    addToast('链接已复制到剪贴板')
  } catch {
    const t = document.createElement('textarea')
    t.value = file.url; document.body.appendChild(t); t.select(); document.execCommand('copy'); document.body.removeChild(t)
    addToast('链接已复制')
  }
}

const handleCopyMarkdown = async file => {
  const md = isImage(file.extension)
    ? `![${file.name}](${file.url})`
    : `[${file.name}](${file.url})`
  try {
    await navigator.clipboard.writeText(md)
    addToast('Markdown 链接已复制')
  } catch {
    const t = document.createElement('textarea')
    t.value = md; document.body.appendChild(t); t.select(); document.execCommand('copy'); document.body.removeChild(t)
    addToast('Markdown 链接已复制')
  }
}

/* ================================================================
   Download
   ================================================================ */
const handleDownload = file => {
  const a = document.createElement('a')
  a.href = file.url; a.download = file.name; a.target = '_blank'; a.rel = 'noopener'
  document.body.appendChild(a); a.click(); document.body.removeChild(a)
}

/* ================================================================
   Delete
   ================================================================ */
const handleDelete = async file => {
  try {
    await ElMessageBox.confirm(`确认删除「${file.name}」？此操作不可恢复。`, '删除文件', {
      confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning',
      confirmButtonClass: 'el-button--danger',
    })
    await deleteFile(file.name)
    addToast('文件已删除')
    if (showFileDetail.value && detailFile.value?.name === file.name) closeDetail()
    fetchFiles(); fetchStats()
  } catch (e) { if (e !== 'cancel') console.error('del err', e) }
}

const handleBatchDelete = async () => {
  if (!selectedFiles.value.length) return
  try {
    await ElMessageBox.confirm(
      `确认删除选中的 ${selectedFiles.value.length} 个文件？此操作不可恢复。`,
      '批量删除', {
        confirmButtonText: '确认删除', cancelButtonText: '取消', type: 'warning',
        confirmButtonClass: 'el-button--danger',
      })
    await batchDeleteFiles(selectedFiles.value)
    addToast(`已删除 ${selectedFiles.value.length} 个文件`)
    selectedFiles.value = []; fetchFiles(); fetchStats()
  } catch (e) { if (e !== 'cancel') console.error('batch del err', e) }
}

/* ================================================================
   Upload with real progress + multi-file queue
   ================================================================ */
const handleUpload = async options => {
  const file = options.file

  // Add to multi upload panel
  const entry = { name: file.name, progress: 0, status: 'uploading', size: file.size }
  multiUploadFiles.value.push(entry)
  showUploadPanel.value = true

  uploadLoading.value = true
  uploadFileName.value = file.name
  uploadProgress.value = 0

  try {
    const formData = new FormData()
    formData.append('file', file)
    await uploadFileLibrary(formData)
    entry.progress = 100
    entry.status = 'done'
    uploadProgress.value = 100
    addToast(`${file.name} 上传成功`)
    fetchFiles(); fetchStats()
  } catch (e) {
    entry.status = 'error'
    addToast(`上传失败：${file.name}`, 'error')
  } finally {
    setTimeout(() => {
      uploadLoading.value = false
      uploadProgress.value = 0
      uploadFileName.value = ''
    }, 600)
  }
}

const clearUploadPanel = () => {
  multiUploadFiles.value = []
  showUploadPanel.value = false
}

/* Drag & drop */
let dragCounter = 0
const handleDragEnter = e => { e.preventDefault(); dragCounter++; isDragging.value = true }
const handleDragLeave = e => { e.preventDefault(); dragCounter--; if (dragCounter <= 0) { isDragging.value = false; dragCounter = 0 } }
const handleDragOver = e => { e.preventDefault() }
const handleDrop = async e => {
  e.preventDefault(); isDragging.value = false; dragCounter = 0
  const files = e.dataTransfer.files
  if (!files.length) return
  for (const file of files) await handleUpload({ file })
}

/* ================================================================
   Context menu
   ================================================================ */
const handleContextMenu = (e, file) => {
  e.preventDefault()
  contextMenu.visible = true
  // Keep menu in viewport
  const mx = Math.min(e.clientX, window.innerWidth - 200)
  const my = Math.min(e.clientY, window.innerHeight - 300)
  contextMenu.x = mx
  contextMenu.y = my
  contextMenu.file = file
}
const closeContextMenu = () => { contextMenu.visible = false }

/* ================================================================
   Keyboard shortcuts
   ================================================================ */
const searchRef = ref(null)
const handleKeydown = e => {
  // Lightbox shortcuts
  if (previewVisible.value) {
    if (e.key === 'ArrowLeft') navigatePreview(-1)
    if (e.key === 'ArrowRight') navigatePreview(1)
    if (e.key === 'Escape') { previewVisible.value = false; return }
    if (e.key === '+' || e.key === '=') { previewZoom.value = Math.min(5, previewZoom.value + 0.25); return }
    if (e.key === '-') { previewZoom.value = Math.max(0.3, previewZoom.value - 0.25); return }
    if (e.key === '0') { previewZoom.value = 1; previewDrag.x = 0; previewDrag.y = 0; return }
    return
  }

  // Ctrl/Cmd+F -> focus search
  if ((e.ctrlKey || e.metaKey) && e.key === 'f') {
    e.preventDefault()
    searchRef.value?.focus()
    return
  }

  // Delete selected
  if (e.key === 'Delete' && selectedFiles.value.length) {
    handleBatchDelete()
    return
  }

  // Escape -> close detail
  if (e.key === 'Escape') {
    if (showFileDetail.value) closeDetail()
    if (contextMenu.visible) closeContextMenu()
  }
}

const handleGlobalClick = () => { if (contextMenu.visible) closeContextMenu() }

/* ================================================================
   Lifecycle
   ================================================================ */
onMounted(() => {
  fetchStats(); fetchFiles()
  document.addEventListener('keydown', handleKeydown)
  document.addEventListener('click', handleGlobalClick)
})
onUnmounted(() => {
  document.removeEventListener('keydown', handleKeydown)
  document.removeEventListener('click', handleGlobalClick)
})
</script>

<template>
  <div
    class="file-library"
    @dragenter="handleDragEnter"
    @dragleave="handleDragLeave"
    @dragover="handleDragOver"
    @drop="handleDrop"
  >
    <!-- ==================== Toast ==================== -->
    <Teleport to="body">
      <TransitionGroup name="toast" tag="div" class="toast-stack">
        <div v-for="t in toasts" :key="t.id" :class="['toast-item', `toast-${t.type}`]">
          <svg v-if="t.type==='success'" width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M5 8l2 2 4-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none"><circle cx="8" cy="8" r="7" stroke="currentColor" stroke-width="1.5"/><path d="M8 5v3M8 10.5h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
          <span>{{ t.msg }}</span>
        </div>
      </TransitionGroup>
    </Teleport>

    <!-- ==================== Drag overlay ==================== -->
    <Transition name="fade">
      <div v-if="isDragging" class="drag-overlay">
        <div class="drag-content">
          <div class="drag-icon-ring">
            <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2" stroke-linecap="round">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/>
              <polyline points="17 8 12 3 7 8"/>
              <line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </div>
          <h3 class="drag-title">释放文件以上传</h3>
          <p class="drag-hint">支持同时上传多个文件</p>
        </div>
      </div>
    </Transition>

    <!-- ==================== Upload progress ==================== -->
    <Transition name="slide-down">
      <div v-if="uploadLoading" class="upload-bar">
        <div class="upload-bar-inner">
          <div class="upload-bar-icon">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </svg>
          </div>
          <div class="upload-bar-text">
            <span class="upload-filename">{{ uploadFileName }}</span>
            <span class="upload-percent">{{ uploadProgress }}%</span>
          </div>
          <div class="upload-bar-track"><div class="upload-bar-fill" :style="{ width: uploadProgress + '%' }"></div></div>
        </div>
      </div>
    </Transition>

    <!-- ==================== Multi-upload panel ==================== -->
    <Transition name="slide-up">
      <div v-if="showUploadPanel && multiUploadFiles.length" class="upload-panel">
        <div class="up-header">
          <span class="up-title">上传列表 ({{ multiUploadFiles.filter(f=>f.status==='done').length }}/{{ multiUploadFiles.length }})</span>
          <button class="up-close" @click="clearUploadPanel">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M3 3l8 8M11 3l-8 8"/></svg>
          </button>
        </div>
        <div class="up-list">
          <div v-for="(uf, i) in multiUploadFiles" :key="i" class="up-item">
            <div :class="['up-status', `up-${uf.status}`]">
              <svg v-if="uf.status==='done'" width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2.5 6L5 8.5L9.5 3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              <svg v-else-if="uf.status==='error'" width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M3 3l6 6M9 3l-6 6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              <div v-else class="up-spinner"></div>
            </div>
            <span class="up-name">{{ uf.name }}</span>
            <span class="up-size">{{ formatSize(uf.size) }}</span>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ==================== Header ==================== -->
    <div class="page-header">
      <div class="header-main">
        <div class="header-title-group">
          <h1 class="page-title">
            <span class="title-icon">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M22 19a2 2 0 01-2 2H4a2 2 0 01-2-2V5a2 2 0 012-2h5l2 3h9a2 2 0 012 2z"/>
              </svg>
            </span>
            文件库
          </h1>
          <div class="header-stats">
            <span class="stat-badge">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/></svg>
              {{ totalCount }} 个文件
            </span>
            <span class="stat-badge stat-badge-size">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>
              {{ formatSize(totalUsage) }}
            </span>
          </div>
        </div>
        <div class="header-actions">
          <el-upload :show-file-list="false" :http-request="handleUpload" :disabled="uploadLoading" multiple>
            <button class="btn-upload" :disabled="uploadLoading">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
              </svg>
              <span>上传文件</span>
            </button>
          </el-upload>
        </div>
      </div>
    </div>

    <!-- ==================== Categories ==================== -->
    <div class="category-bar">
      <div class="category-scroll">
        <button
          v-for="cat in categories"
          :key="cat.value"
          :class="['cat-chip', { active: filter.category === cat.value }]"
          @click="handleCategoryChange(cat.value)"
        >
          <span class="cat-chip-icon" v-html="catIcons[cat.value] || catIcons['']"></span>
          <span class="cat-chip-label">{{ cat.label }}</span>
          <span v-if="cat.count" class="cat-chip-count">{{ cat.count }}</span>
        </button>
      </div>
    </div>

    <!-- ==================== Toolbar ==================== -->
    <div class="toolbar-row">
      <div class="toolbar-left">
        <button v-if="filteredFileList.length" class="tool-btn" @click="selectAll" :title="allSelected ? '取消全选' : '全选'">
          <svg v-if="!allSelected" width="16" height="16" viewBox="0 0 16 16" fill="none">
            <rect x="1.5" y="1.5" width="13" height="13" rx="3" stroke="currentColor" stroke-width="1.5"/>
          </svg>
          <svg v-else width="16" height="16" viewBox="0 0 16 16" fill="none">
            <rect x="1.5" y="1.5" width="13" height="13" rx="3" fill="#1a1a2e" stroke="#1a1a2e" stroke-width="1.5"/>
            <path d="M4.5 8L7 10.5L11.5 5.5" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <span>{{ allSelected ? '取消' : '全选' }}</span>
        </button>

        <Transition name="fade">
          <button v-if="selectedFiles.length" class="tool-btn tool-btn-danger" @click="handleBatchDelete">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
              <path d="M2 4h12M5.33 4V2.67a1.33 1.33 0 011.34-1.34h2.66a1.33 1.33 0 011.34 1.34V4M12.67 4v9.33a1.33 1.33 0 01-1.34 1.34H4.67a1.33 1.33 0 01-1.34-1.34V4"/>
            </svg>
            <span>删除 ({{ selectedFiles.length }})</span>
          </button>
        </Transition>

        <div class="sort-group">
          <button class="tool-btn" @click="handleSort(sortField)">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round">
              <path d="M2 4h12M4 8h8M6 12h4"/>
            </svg>
            <span>{{ sortField === 'name' ? '名称' : sortField === 'size' ? '大小' : '时间' }}</span>
            <svg :class="['sort-arrow', { asc: sortOrder === 'asc' }]" width="10" height="10" viewBox="0 0 10 10" fill="currentColor">
              <path d="M2 4l3 3 3-3"/>
            </svg>
          </button>
          <div class="sort-dropdown">
            <button :class="{ active: sortField === 'modifiedTime' }" @click="handleSort('modifiedTime')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
              按时间
            </button>
            <button :class="{ active: sortField === 'name' }" @click="handleSort('name')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 7h16M4 12h10M4 17h6"/></svg>
              按名称
            </button>
            <button :class="{ active: sortField === 'size' }" @click="handleSort('size')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 16V8a2 2 0 00-1-1.73l-7-4a2 2 0 00-2 0l-7 4A2 2 0 003 8v8a2 2 0 001 1.73l7 4a2 2 0 002 0l7-4A2 2 0 0021 16z"/></svg>
              按大小
            </button>
          </div>
        </div>
      </div>

      <div class="toolbar-right">
        <div :class="['search-box', { focused: searchFocused }]">
          <svg class="search-icon" width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" stroke-width="1.5">
            <circle cx="7" cy="7" r="4.5"/><path d="M10.5 10.5L14 14" stroke-linecap="round"/>
          </svg>
          <input
            ref="searchRef"
            v-model="searchKeyword"
            type="text"
            placeholder="搜索文件… (Ctrl+F)"
            class="search-input"
            @focus="searchFocused = true"
            @blur="searchFocused = false"
          />
          <button v-if="searchKeyword" class="search-clear" @click="searchKeyword = ''">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round">
              <path d="M3 3l6 6M9 3l-6 6"/>
            </svg>
          </button>
        </div>

        <div class="view-toggle">
          <button :class="['vt-btn', { active: viewMode === 'grid' }]" @click="viewMode = 'grid'" title="网格视图">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <rect x="1" y="1" width="6" height="6" rx="1.5"/><rect x="9" y="1" width="6" height="6" rx="1.5"/>
              <rect x="1" y="9" width="6" height="6" rx="1.5"/><rect x="9" y="9" width="6" height="6" rx="1.5"/>
            </svg>
          </button>
          <button :class="['vt-btn', { active: viewMode === 'list' }]" @click="viewMode = 'list'" title="列表视图">
            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
              <rect x="1" y="2" width="14" height="2.5" rx="1"/><rect x="1" y="6.75" width="14" height="2.5" rx="1"/><rect x="1" y="11.5" width="14" height="2.5" rx="1"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== Grid View ==================== -->
    <div v-if="viewMode === 'grid'" class="file-grid" v-loading="loading">
      <div v-if="!loading && filteredFileList.length === 0" class="empty-state">
        <div class="empty-visual">
          <svg width="140" height="120" viewBox="0 0 140 120" fill="none">
            <rect x="20" y="25" width="100" height="70" rx="10" fill="#f1f5f9" stroke="#e2e8f0" stroke-width="1.5"/>
            <path d="M20 40c0-5.523 4.477-10 10-10h20.172a6 6 0 014.243 1.757l4.585 4.586A6 6 0 0063.243 38H110c5.523 0 10 4.477 10 10v37c0 5.523-4.477 10-10 10H30c-5.523 0-10-4.477-10-10V40z" fill="#f8fafc" stroke="#e2e8f0" stroke-width="1.5"/>
            <path d="M56 64v-10m0 0l-5 5m5-5l5 5" stroke="#94a3b8" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M74 72h18M74 80h12" stroke="#cbd5e1" stroke-width="2.5" stroke-linecap="round"/>
          </svg>
        </div>
        <h3 class="empty-title">{{ searchKeyword ? '未找到匹配文件' : '暂无文件' }}</h3>
        <p class="empty-desc">{{ searchKeyword ? '请尝试其他关键词搜索' : '拖拽文件到此处，或点击上方「上传文件」按钮' }}</p>
      </div>

      <!-- File cards -->
      <div
        v-for="(file, idx) in filteredFileList"
        :key="file.name"
        :class="['file-card', { selected: isSelected(file) }]"
        :style="{ '--anim-delay': Math.min(idx * 30, 300) + 'ms' }"
        @contextmenu="handleContextMenu($event, file)"
      >
        <div class="card-checkbox" @click.stop="toggleSelect(file, $event)">
          <div :class="['ck-box', { checked: isSelected(file) }]">
            <svg v-if="isSelected(file)" width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M2.5 6L5 8.5L9.5 3.5" stroke="white" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>

        <div class="card-thumb" @click="handlePreview(file)">
          <img v-if="isImage(file.extension)" :src="file.url" :alt="file.name" loading="lazy" class="thumb-img" />
          <div v-else class="thumb-placeholder" :style="{ background: getFileColors(file.extension).bg }">
            <div class="thumb-icon-wrap" :style="{ background: getFileColors(file.extension).accent + '18' }">
              <svg width="28" height="28" viewBox="0 0 24 24" fill="none" :stroke="getFileColors(file.extension).fg" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round">
                <path v-if="isAudio(file.extension)" d="M9 18V5l12-2v13M9 18a3 3 0 11-6 0 3 3 0 016 0zM21 16a3 3 0 11-6 0 3 3 0 016 0z"/>
                <template v-else-if="isVideo(file.extension)">
                  <rect x="2" y="4" width="14" height="16" rx="2"/><path d="M16 9l5-3v12l-5-3"/>
                </template>
                <template v-else-if="['pdf'].includes(file.extension)">
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
                </template>
                <template v-else>
                  <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
                </template>
              </svg>
            </div>
            <span class="thumb-ext">.{{ file.extension }}</span>
          </div>
        </div>

        <div class="card-info">
          <div class="card-name" :title="file.name">{{ file.name }}</div>
          <div class="card-meta">
            <span class="card-size">{{ formatSize(file.size) }}</span>
            <span class="card-time">{{ formatDate(file.modifiedTime) }}</span>
          </div>
        </div>

        <div class="card-actions">
          <button class="ca-btn" @click.stop="openDetail(file)" title="详情">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
          </button>
          <button class="ca-btn" @click.stop="handleCopyUrl(file)" title="复制链接">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/></svg>
          </button>
          <button class="ca-btn" @click.stop="handleDownload(file)" title="下载">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          </button>
          <button class="ca-btn ca-danger" @click.stop="handleDelete(file)" title="删除">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
          </button>
        </div>
      </div>
    </div>

    <!-- ==================== List View ==================== -->
    <div v-if="viewMode === 'list'" class="file-list" v-loading="loading">
      <div v-if="!loading && filteredFileList.length === 0" class="empty-state">
        <div class="empty-visual">
          <svg width="120" height="90" viewBox="0 0 120 90" fill="none">
            <rect x="15" y="20" width="90" height="55" rx="8" fill="#f1f5f9" stroke="#e2e8f0" stroke-width="1.5"/>
            <rect x="28" y="35" width="64" height="5" rx="2.5" fill="#e2e8f0"/>
            <rect x="28" y="47" width="48" height="5" rx="2.5" fill="#e2e8f0"/>
            <rect x="28" y="59" width="56" height="5" rx="2.5" fill="#e2e8f0"/>
          </svg>
        </div>
        <h3 class="empty-title">{{ searchKeyword ? '未找到匹配文件' : '暂无文件' }}</h3>
        <p class="empty-desc">{{ searchKeyword ? '请尝试其他关键词搜索' : '拖拽文件到此处，或点击上方「上传文件」按钮' }}</p>
      </div>

      <div v-if="filteredFileList.length" class="list-head">
        <div class="lh-check" @click="selectAll">
          <div :class="['ck-box ck-mini', { checked: allSelected }]">
            <svg v-if="allSelected" width="10" height="10" viewBox="0 0 12 12" fill="none">
              <path d="M2.5 6L5 8.5L9.5 3.5" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        </div>
        <div class="lh-name" @click="handleSort('name')">
          文件名
          <svg v-if="sortField === 'name'" :class="['sort-arrow', { asc: sortOrder === 'asc' }]" width="10" height="10" viewBox="0 0 10 10" fill="currentColor"><path d="M2 4l3 3 3-3"/></svg>
        </div>
        <div class="lh-type">类型</div>
        <div class="lh-size" @click="handleSort('size')">
          大小
          <svg v-if="sortField === 'size'" :class="['sort-arrow', { asc: sortOrder === 'asc' }]" width="10" height="10" viewBox="0 0 10 10" fill="currentColor"><path d="M2 4l3 3 3-3"/></svg>
        </div>
        <div class="lh-date" @click="handleSort('modifiedTime')">
          修改时间
          <svg v-if="sortField === 'modifiedTime'" :class="['sort-arrow', { asc: sortOrder === 'asc' }]" width="10" height="10" viewBox="0 0 10 10" fill="currentColor"><path d="M2 4l3 3 3-3"/></svg>
        </div>
        <div class="lh-actions">操作</div>
      </div>

      <TransitionGroup name="list-item" tag="div">
        <div
          v-for="file in filteredFileList"
          :key="file.name"
          :class="['list-row', { selected: isSelected(file) }]"
          @click="handlePreview(file)"
          @contextmenu="handleContextMenu($event, file)"
        >
          <div class="lr-check" @click.stop>
            <div :class="['ck-box ck-mini', { checked: isSelected(file) }]" @click.stop="toggleSelect(file, $event)">
              <svg v-if="isSelected(file)" width="10" height="10" viewBox="0 0 12 12" fill="none">
                <path d="M2.5 6L5 8.5L9.5 3.5" stroke="white" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </div>
          </div>
          <div class="lr-name">
            <div class="lr-icon-wrap" :style="{ background: isImage(file.extension) ? 'transparent' : getFileColors(file.extension).bg }">
              <img v-if="isImage(file.extension)" :src="file.url" loading="lazy" class="lr-thumb" />
              <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" :stroke="getFileColors(file.extension).fg" stroke-width="2" stroke-linecap="round">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
              </svg>
            </div>
            <span class="lr-filename" :title="file.name">{{ file.name }}</span>
          </div>
          <div class="lr-type">
            <span class="type-tag" :style="{ color: getFileColors(file.extension).fg, background: getFileColors(file.extension).bg }">
              {{ getFileTypeLabel(file.extension) }}
            </span>
          </div>
          <div class="lr-size">{{ formatSize(file.size) }}</div>
          <div class="lr-date">{{ formatDate(file.modifiedTime) }}</div>
          <div class="lr-actions" @click.stop>
            <button class="la-btn" @click.stop="handleCopyUrl(file)" title="复制链接">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/></svg>
            </button>
            <button class="la-btn" @click.stop="handleDownload(file)" title="下载">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            </button>
            <button class="la-btn la-danger" @click.stop="handleDelete(file)" title="删除">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
            </button>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <!-- ==================== Pagination ==================== -->
    <div v-if="pagination.total > 0" class="pagination-wrap">
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.pageSize"
        :total="pagination.total"
        :page-sizes="[12, 24, 48, 96]"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
        small
      />
    </div>

    <!-- ==================== File Detail Sidebar ==================== -->
    <Transition name="slide-right">
      <div v-if="showFileDetail && detailFile" class="detail-overlay" @click.self="closeDetail">
        <div class="detail-panel">
          <div class="detail-header">
            <h3 class="detail-title">文件详情</h3>
            <button class="detail-close" @click="closeDetail">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round">
                <path d="M4 4l10 10M14 4L4 14"/>
              </svg>
            </button>
          </div>

          <div class="detail-preview">
            <img v-if="isImage(detailFile.extension)" :src="detailFile.url" class="detail-img" @click="handlePreview(detailFile)" />
            <div v-else-if="isAudio(detailFile.extension)" class="detail-audio-area">
              <div class="detail-audio-icon" :style="{ background: getFileColors(detailFile.extension).bg }">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" :stroke="getFileColors(detailFile.extension).fg" stroke-width="1.5"><path d="M9 18V5l12-2v13"/><circle cx="6" cy="18" r="3"/><circle cx="18" cy="16" r="3"/></svg>
              </div>
              <audio :src="detailFile.url" controls class="detail-audio-player"></audio>
            </div>
            <div v-else-if="isVideo(detailFile.extension)" class="detail-video-area">
              <video :src="detailFile.url" controls class="detail-video-player"></video>
            </div>
            <div v-else class="detail-icon-area" :style="{ background: getFileColors(detailFile.extension).bg }">
              <svg width="48" height="48" viewBox="0 0 24 24" fill="none" :stroke="getFileColors(detailFile.extension).fg" stroke-width="1.5" stroke-linecap="round">
                <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><polyline points="14 2 14 8 20 8"/>
              </svg>
              <span class="detail-ext">.{{ detailFile.extension }}</span>
            </div>
          </div>

          <div class="detail-meta">
            <div class="dm-row">
              <span class="dm-label">文件名</span>
              <span class="dm-value dm-filename">{{ detailFile.name }}</span>
            </div>
            <div class="dm-row">
              <span class="dm-label">类型</span>
              <span class="dm-value">
                <span class="type-tag" :style="{ color: getFileColors(detailFile.extension).fg, background: getFileColors(detailFile.extension).bg }">
                  {{ getFileTypeLabel(detailFile.extension) }}
                </span>
              </span>
            </div>
            <div class="dm-row">
              <span class="dm-label">大小</span>
              <span class="dm-value">{{ formatSize(detailFile.size) }}</span>
            </div>
            <div class="dm-row">
              <span class="dm-label">修改时间</span>
              <span class="dm-value">{{ formatDateFull(detailFile.modifiedTime) }}</span>
            </div>
            <div class="dm-row">
              <span class="dm-label">扩展名</span>
              <span class="dm-value">.{{ detailFile.extension }}</span>
            </div>
          </div>

          <div class="detail-actions">
            <button class="da-btn da-primary" @click="handleCopyUrl(detailFile)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/></svg>
              复制链接
            </button>
            <button class="da-btn" @click="handleCopyMarkdown(detailFile)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7V4a2 2 0 012-2h8.5L20 7.5V20a2 2 0 01-2 2H6a2 2 0 01-2-2v-3"/><polyline points="14 2 14 8 20 8"/><path d="M3 15l2-2 2 2"/><path d="M5 13v5"/><path d="M9 18v-5l2.5 3 2.5-3v5"/></svg>
              复制 Markdown
            </button>
            <button class="da-btn" @click="handleDownload(detailFile)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              下载文件
            </button>
            <button class="da-btn da-danger" @click="handleDelete(detailFile)">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
              删除文件
            </button>
          </div>

          <div class="detail-url">
            <span class="dm-label">文件链接</span>
            <div class="url-box" @click="handleCopyUrl(detailFile)">
              <span class="url-text">{{ detailFile.url }}</span>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="9" y="9" width="13" height="13" rx="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- ==================== Context Menu ==================== -->
    <Teleport to="body">
      <Transition name="ctx">
        <div
          v-if="contextMenu.visible"
          class="ctx-menu"
          :style="{ left: contextMenu.x + 'px', top: contextMenu.y + 'px' }"
        >
          <button class="ctx-item" @click="handlePreview(contextMenu.file); closeContextMenu()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>
            预览
          </button>
          <button class="ctx-item" @click="openDetail(contextMenu.file); closeContextMenu()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/></svg>
            详情
          </button>
          <div class="ctx-divider"></div>
          <button class="ctx-item" @click="handleCopyUrl(contextMenu.file); closeContextMenu()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/></svg>
            复制链接
          </button>
          <button class="ctx-item" @click="handleCopyMarkdown(contextMenu.file); closeContextMenu()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7V4a2 2 0 012-2h8.5L20 7.5V20a2 2 0 01-2 2H6a2 2 0 01-2-2v-3"/><polyline points="14 2 14 8 20 8"/></svg>
            复制 Markdown
          </button>
          <button class="ctx-item" @click="handleDownload(contextMenu.file); closeContextMenu()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
            下载
          </button>
          <div class="ctx-divider"></div>
          <button class="ctx-item ctx-danger" @click="handleDelete(contextMenu.file); closeContextMenu()">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2"/></svg>
            删除
          </button>
        </div>
      </Transition>
    </Teleport>

    <!-- ==================== Lightbox ==================== -->
    <Teleport to="body">
      <Transition name="lb">
        <div v-if="previewVisible" class="lb-overlay" @click.self="previewVisible = false" @wheel.prevent="handleLightboxWheel">
          <div class="lb-wrap">
            <button class="lb-close" @click="previewVisible = false">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M4 4l12 12M16 4L4 16"/></svg>
            </button>
            <div class="lb-title">{{ previewName }}</div>

            <button v-if="previewIndex > 0" class="lb-nav lb-prev" @click="navigatePreview(-1)">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 19l-7-7 7-7"/></svg>
            </button>

            <div class="lb-content">
              <img
                v-if="previewType === 'image'"
                :src="previewUrl"
                :alt="previewName"
                class="lb-image"
                :style="{ transform: `scale(${previewZoom}) translate(${previewDrag.x}px, ${previewDrag.y}px)` }"
              />
              <audio v-else-if="previewType === 'audio'" :src="previewUrl" controls autoplay class="lb-audio" />
              <video v-else-if="previewType === 'video'" :src="previewUrl" controls autoplay class="lb-video" />
            </div>

            <button v-if="previewIndex < previewableFiles.length - 1" class="lb-nav lb-next" @click="navigatePreview(1)">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 5l7 7-7 7"/></svg>
            </button>

            <div v-if="previewableFiles.length > 1" class="lb-counter">{{ previewIndex + 1 }} / {{ previewableFiles.length }}</div>

            <div class="lb-actions">
              <template v-if="previewType === 'image'">
                <button class="lb-act-btn" @click="previewZoom = Math.min(5, previewZoom + 0.25)" title="放大">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="11" y1="8" x2="11" y2="14"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
                </button>
                <button class="lb-act-btn" @click="previewZoom = Math.max(0.3, previewZoom - 0.25)" title="缩小">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/><line x1="8" y1="11" x2="14" y2="11"/></svg>
                </button>
                <button class="lb-act-btn" @click="previewZoom = 1; previewDrag.x = 0; previewDrag.y = 0" title="重置">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="1 4 1 10 7 10"/><path d="M3.51 15a9 9 0 102.13-9.36L1 10"/></svg>
                </button>
                <div class="lb-zoom-val">{{ Math.round(previewZoom * 100) }}%</div>
              </template>
              <div class="lb-act-sep"></div>
              <button class="lb-act-btn" @click="handleCopyUrl({ url: previewUrl, name: previewName })" title="复制链接">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/><path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/></svg>
              </button>
              <button class="lb-act-btn" @click="handleDownload({ url: previewUrl, name: previewName })" title="下载">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
/* ================================================================
   Base
   ================================================================ */
.file-library {
  min-height: 100%;
  position: relative;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ================================================================
   Toast
   ================================================================ */
.toast-stack {
  position: fixed; top: 16px; right: 16px; z-index: 9999;
  display: flex; flex-direction: column; gap: 8px;
  pointer-events: none;
}
.toast-item {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 16px; border-radius: 10px;
  font-size: 13px; font-weight: 500;
  background: #fff; color: #1e293b;
  box-shadow: 0 8px 24px rgba(0,0,0,0.12), 0 2px 8px rgba(0,0,0,0.04);
  border: 1px solid #e2e8f0; pointer-events: auto;
  animation: toast-in 0.3s ease;
}
.toast-success svg { color: #10b981; }
.toast-error { border-color: #fecaca; }
.toast-error svg { color: #ef4444; }
@keyframes toast-in {
  from { opacity: 0; transform: translateX(20px) scale(0.95); }
  to { opacity: 1; transform: translateX(0) scale(1); }
}
.toast-enter-active { transition: all 0.3s ease; }
.toast-leave-active { transition: all 0.2s ease; }
.toast-enter-from { opacity: 0; transform: translateX(20px); }
.toast-leave-to { opacity: 0; transform: translateX(20px) scale(0.9); }

/* ================================================================
   Drag overlay
   ================================================================ */
.drag-overlay {
  position: fixed; inset: 0; z-index: 1000;
  background: rgba(15, 23, 42, 0.75);
  backdrop-filter: blur(12px);
  display: flex; align-items: center; justify-content: center;
}
.drag-content { text-align: center; color: #fff; }
.drag-icon-ring {
  width: 88px; height: 88px; margin: 0 auto 24px;
  border: 2.5px dashed rgba(255,255,255,0.45);
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  animation: float-pulse 2s ease-in-out infinite;
}
@keyframes float-pulse {
  0%, 100% { transform: translateY(0) scale(1); opacity: 1; }
  50% { transform: translateY(-6px) scale(1.04); opacity: 0.85; }
}
.drag-title { font-size: 20px; font-weight: 600; margin-bottom: 6px; letter-spacing: 0.5px; }
.drag-hint { font-size: 14px; opacity: 0.6; }

/* ================================================================
   Upload progress bar
   ================================================================ */
.upload-bar {
  margin-bottom: 16px;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px;
  padding: 14px 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.04);
}
.upload-bar-inner { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.upload-bar-icon { color: #3b82f6; flex-shrink: 0; }
.upload-bar-text { flex: 1; min-width: 0; display: flex; align-items: center; gap: 8px; font-size: 13px; color: #475569; }
.upload-filename { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; font-weight: 500; }
.upload-percent { color: #3b82f6; font-weight: 600; flex-shrink: 0; }
.upload-bar-track { width: 100%; height: 4px; background: #e2e8f0; border-radius: 2px; overflow: hidden; margin-top: 2px; }
.upload-bar-fill { height: 100%; background: linear-gradient(90deg, #3b82f6, #6366f1); border-radius: 2px; transition: width 0.3s ease; }

/* ================================================================
   Multi-upload panel
   ================================================================ */
.upload-panel {
  position: fixed; bottom: 20px; right: 20px; z-index: 1200;
  width: 320px; max-height: 280px;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 14px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.1), 0 4px 12px rgba(0,0,0,0.04);
  overflow: hidden;
}
.up-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 16px; border-bottom: 1px solid #f1f5f9;
}
.up-title { font-size: 13px; font-weight: 600; color: #1e293b; }
.up-close {
  width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
  border: none; background: #f1f5f9; border-radius: 6px; cursor: pointer;
  color: #64748b; transition: all 0.15s;
}
.up-close:hover { background: #e2e8f0; color: #1e293b; }
.up-list { overflow-y: auto; max-height: 220px; padding: 8px; }
.up-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px; border-radius: 8px; transition: background 0.15s;
}
.up-item:hover { background: #f8fafc; }
.up-status {
  width: 20px; height: 20px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.up-done { background: #ecfdf5; color: #10b981; }
.up-error { background: #fef2f2; color: #ef4444; }
.up-uploading { background: #eff6ff; }
.up-spinner {
  width: 14px; height: 14px; border: 2px solid #e2e8f0;
  border-top-color: #3b82f6; border-radius: 50%;
  animation: spin 0.7s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }
.up-name {
  flex: 1; font-size: 12px; color: #475569; font-weight: 500;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}
.up-size { font-size: 11px; color: #94a3b8; flex-shrink: 0; }

/* ================================================================
   Page Header
   ================================================================ */
.page-header { margin-bottom: 24px; }
.header-main { display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.header-title-group { display: flex; align-items: center; gap: 14px; flex-wrap: wrap; }
.page-title {
  font-size: 22px; font-weight: 700; color: #0f172a; margin: 0;
  letter-spacing: -0.3px; display: flex; align-items: center; gap: 10px;
}
.title-icon {
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1a1a2e, #16213e); border-radius: 10px; color: #fff;
}
.header-stats { display: flex; gap: 8px; }
.stat-badge {
  display: inline-flex; align-items: center; gap: 5px;
  font-size: 12.5px; font-weight: 500; color: #64748b;
  background: #f1f5f9; padding: 4px 10px; border-radius: 20px;
}
.stat-badge svg { opacity: 0.6; }
.stat-badge-size { background: #eff6ff; color: #3b82f6; }
.stat-badge-size svg { opacity: 0.7; }

.btn-upload {
  display: inline-flex; align-items: center; gap: 8px;
  padding: 9px 20px; border: none; border-radius: 10px;
  background: linear-gradient(135deg, #1a1a2e, #16213e);
  color: #fff; font-size: 14px; font-weight: 600;
  cursor: pointer; transition: all 0.2s;
  box-shadow: 0 2px 8px rgba(26, 26, 46, 0.2);
}
.btn-upload:hover { transform: translateY(-1px); box-shadow: 0 4px 16px rgba(26, 26, 46, 0.3); }
.btn-upload:active { transform: translateY(0); }
.btn-upload:disabled { opacity: 0.6; cursor: not-allowed; transform: none; }

/* ================================================================
   Category bar
   ================================================================ */
.category-bar { margin-bottom: 16px; }
.category-scroll {
  display: flex; gap: 6px; overflow-x: auto; padding-bottom: 4px;
  scrollbar-width: none; -webkit-overflow-scrolling: touch;
}
.category-scroll::-webkit-scrollbar { display: none; }
.cat-chip {
  display: inline-flex; align-items: center; gap: 6px;
  padding: 7px 14px; border: 1.5px solid #e2e8f0; border-radius: 20px;
  background: #fff; cursor: pointer; font-size: 13px; font-weight: 500;
  color: #64748b; white-space: nowrap; transition: all 0.2s; flex-shrink: 0;
}
.cat-chip:hover { border-color: #cbd5e1; background: #f8fafc; color: #475569; }
.cat-chip.active { background: #0f172a; border-color: #0f172a; color: #fff; box-shadow: 0 2px 8px rgba(15,23,42,0.2); }
.cat-chip-icon { display: flex; align-items: center; width: 16px; height: 16px; }
.cat-chip-count {
  font-size: 11px; font-weight: 600; min-width: 18px; height: 18px;
  line-height: 18px; text-align: center; border-radius: 9px;
  background: #f1f5f9; color: #94a3b8; padding: 0 5px;
}
.cat-chip.active .cat-chip-count { background: rgba(255,255,255,0.18); color: rgba(255,255,255,0.8); }

/* ================================================================
   Toolbar
   ================================================================ */
.toolbar-row {
  display: flex; align-items: center; justify-content: space-between;
  gap: 12px; margin-bottom: 18px; flex-wrap: wrap;
}
.toolbar-left, .toolbar-right { display: flex; align-items: center; gap: 8px; }

.tool-btn {
  display: inline-flex; align-items: center; gap: 5px;
  padding: 6px 12px; border: 1.5px solid #e2e8f0; border-radius: 8px;
  background: #fff; cursor: pointer; font-size: 13px; font-weight: 500;
  color: #475569; transition: all 0.15s; white-space: nowrap;
}
.tool-btn:hover { border-color: #cbd5e1; background: #f8fafc; }
.tool-btn-danger { color: #ef4444; border-color: #fecaca; }
.tool-btn-danger:hover { background: #fef2f2; border-color: #fca5a5; }

.sort-arrow { transition: transform 0.2s; }
.sort-arrow.asc { transform: rotate(180deg); }

.sort-group { position: relative; }
.sort-dropdown {
  display: none; position: absolute; top: calc(100% + 6px); left: 0;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.1); padding: 6px; z-index: 100; min-width: 130px;
}
.sort-group:hover .sort-dropdown { display: block; }
.sort-dropdown button {
  display: flex; align-items: center; gap: 8px; width: 100%; text-align: left; padding: 8px 12px;
  border: none; background: none; border-radius: 8px; font-size: 13px;
  color: #475569; cursor: pointer; transition: background 0.12s;
}
.sort-dropdown button:hover { background: #f1f5f9; }
.sort-dropdown button.active { color: #0f172a; font-weight: 600; background: #f1f5f9; }
.sort-dropdown button svg { opacity: 0.5; }
.sort-dropdown button.active svg { opacity: 1; }

/* Search */
.search-box { position: relative; width: 220px; transition: width 0.25s; }
.search-box.focused { width: 280px; }
.search-icon { position: absolute; left: 10px; top: 50%; transform: translateY(-50%); color: #94a3b8; pointer-events: none; }
.search-input {
  width: 100%; height: 34px; padding: 0 30px 0 34px;
  border: 1.5px solid #e2e8f0; border-radius: 8px; font-size: 13px;
  color: #1e293b; background: #fff; outline: none; transition: all 0.2s;
}
.search-input::placeholder { color: #94a3b8; }
.search-input:focus { border-color: #94a3b8; box-shadow: 0 0 0 3px rgba(148,163,184,0.12); }
.search-clear {
  position: absolute; right: 8px; top: 50%; transform: translateY(-50%);
  width: 20px; height: 20px; display: flex; align-items: center; justify-content: center;
  border: none; background: #e2e8f0; border-radius: 50%; cursor: pointer;
  color: #64748b; transition: all 0.15s;
}
.search-clear:hover { background: #cbd5e1; color: #334155; }

/* View toggle */
.view-toggle { display: flex; background: #f1f5f9; border-radius: 8px; padding: 3px; gap: 2px; }
.vt-btn {
  width: 32px; height: 30px; display: flex; align-items: center; justify-content: center;
  border: none; background: none; border-radius: 6px; cursor: pointer;
  color: #94a3b8; transition: all 0.15s;
}
.vt-btn.active { background: #fff; color: #1e293b; box-shadow: 0 1px 3px rgba(0,0,0,0.08); }
.vt-btn:hover:not(.active) { color: #64748b; }

/* ================================================================
   File grid
   ================================================================ */
.file-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 16px; min-height: 200px;
}

.empty-state { grid-column: 1 / -1; text-align: center; padding: 60px 20px; }
.empty-visual { margin-bottom: 16px; opacity: 0.8; }
.empty-title { font-size: 16px; font-weight: 600; color: #475569; margin: 0 0 6px; }
.empty-desc { font-size: 13px; color: #94a3b8; margin: 0; }

/* ================================================================
   File card
   ================================================================ */
.file-card {
  position: relative; background: #fff;
  border: 1.5px solid #f1f5f9; border-radius: 14px;
  overflow: hidden; transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  animation: card-in 0.35s ease backwards;
  animation-delay: var(--anim-delay, 0ms);
}
@keyframes card-in {
  from { opacity: 0; transform: translateY(12px) scale(0.97); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
.file-card:hover {
  border-color: #e2e8f0;
  box-shadow: 0 8px 28px rgba(0,0,0,0.06), 0 2px 8px rgba(0,0,0,0.03);
  transform: translateY(-3px);
}
.file-card.selected {
  border-color: #1e293b;
  box-shadow: 0 0 0 2px rgba(30, 41, 59, 0.12);
}

.card-checkbox {
  position: absolute; top: 10px; left: 10px; z-index: 3;
  cursor: pointer; opacity: 0; transition: opacity 0.2s;
}
.file-card:hover .card-checkbox, .file-card.selected .card-checkbox { opacity: 1; }

.ck-box {
  width: 22px; height: 22px; border: 2px solid rgba(255,255,255,0.85);
  border-radius: 6px; background: rgba(0,0,0,0.2); display: flex;
  align-items: center; justify-content: center; transition: all 0.15s;
  backdrop-filter: blur(4px);
}
.ck-box.checked { background: #1e293b; border-color: #1e293b; }
.ck-box.ck-mini {
  width: 18px; height: 18px; border: 2px solid #d1d5db; background: #fff;
  backdrop-filter: none; cursor: pointer;
}
.ck-box.ck-mini.checked { background: #1e293b; border-color: #1e293b; }

.card-thumb {
  height: 160px; background: #f8fafc; display: flex;
  align-items: center; justify-content: center;
  cursor: pointer; overflow: hidden; position: relative;
}
.thumb-img {
  width: 100%; height: 100%; object-fit: cover;
  transition: transform 0.45s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}
.file-card:hover .thumb-img { transform: scale(1.06); }

.thumb-placeholder {
  width: 100%; height: 100%; display: flex; flex-direction: column;
  align-items: center; justify-content: center; gap: 10px;
}
.thumb-icon-wrap {
  width: 56px; height: 56px; border-radius: 16px;
  display: flex; align-items: center; justify-content: center;
  transition: transform 0.3s ease;
}
.file-card:hover .thumb-icon-wrap { transform: scale(1.08); }
.thumb-ext {
  font-size: 11px; font-weight: 700; text-transform: uppercase;
  letter-spacing: 0.8px; color: #94a3b8; background: rgba(0,0,0,0.04);
  padding: 2px 10px; border-radius: 6px;
}

.card-info { padding: 12px 14px 10px; }
.card-name {
  font-size: 13px; font-weight: 600; color: #1e293b;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
  margin-bottom: 4px; line-height: 1.4;
}
.card-meta { display: flex; justify-content: space-between; font-size: 11px; color: #94a3b8; }

.card-actions {
  position: absolute; top: 10px; right: 10px; z-index: 3;
  display: flex; gap: 4px; opacity: 0; transition: opacity 0.2s;
}
.file-card:hover .card-actions { opacity: 1; }

.ca-btn {
  width: 30px; height: 30px; display: flex; align-items: center; justify-content: center;
  border: none; background: rgba(255,255,255,0.92); border-radius: 8px;
  cursor: pointer; color: #475569; transition: all 0.15s;
  backdrop-filter: blur(6px); box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.ca-btn:hover { background: #fff; color: #1e293b; box-shadow: 0 2px 8px rgba(0,0,0,0.1); transform: scale(1.08); }
.ca-danger:hover { color: #ef4444; }

/* ================================================================
   List view
   ================================================================ */
.file-list { min-height: 200px; }

.list-head {
  display: flex; align-items: center; padding: 8px 12px;
  background: #f8fafc; border-radius: 10px; margin-bottom: 2px;
  font-size: 12px; font-weight: 600; color: #94a3b8;
  text-transform: uppercase; letter-spacing: 0.3px;
}
.list-head > div { cursor: pointer; display: flex; align-items: center; gap: 4px; }
.list-head > div:hover { color: #64748b; }

.lh-check { width: 40px; flex-shrink: 0; cursor: pointer; }
.lh-name  { flex: 1; min-width: 0; }
.lh-type  { width: 80px; flex-shrink: 0; }
.lh-size  { width: 90px; flex-shrink: 0; }
.lh-date  { width: 120px; flex-shrink: 0; }
.lh-actions { width: 110px; flex-shrink: 0; text-align: right; }

.list-row {
  display: flex; align-items: center; padding: 10px 12px;
  border-radius: 10px; cursor: pointer; transition: all 0.15s;
  border: 1.5px solid transparent; margin-bottom: 2px;
}
.list-row:hover { background: #f8fafc; border-color: #f1f5f9; }
.list-row.selected { background: #f1f5f9; border-color: #e2e8f0; }

.lr-check { width: 40px; flex-shrink: 0; }
.lr-name  { flex: 1; min-width: 0; display: flex; align-items: center; gap: 10px; }
.lr-type  { width: 80px; flex-shrink: 0; }
.lr-size  { width: 90px; flex-shrink: 0; font-size: 13px; color: #64748b; }
.lr-date  { width: 120px; flex-shrink: 0; font-size: 13px; color: #94a3b8; }
.lr-actions { width: 110px; flex-shrink: 0; display: flex; gap: 4px; justify-content: flex-end; }

.lr-icon-wrap {
  width: 36px; height: 36px; border-radius: 8px; overflow: hidden; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center;
}
.lr-thumb { width: 100%; height: 100%; object-fit: cover; border-radius: 8px; }
.lr-filename {
  font-size: 13px; font-weight: 500; color: #1e293b;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
}

.type-tag {
  font-size: 11px; font-weight: 600; padding: 2px 8px;
  border-radius: 6px; white-space: nowrap;
}

.la-btn {
  width: 28px; height: 28px; display: flex; align-items: center; justify-content: center;
  border: none; background: transparent; border-radius: 6px;
  cursor: pointer; color: #94a3b8; transition: all 0.15s; opacity: 0;
}
.list-row:hover .la-btn { opacity: 1; }
.la-btn:hover { background: #f1f5f9; color: #1e293b; }
.la-danger:hover { background: #fef2f2; color: #ef4444; }

/* list item transition */
.list-item-enter-active { transition: all 0.3s ease; }
.list-item-leave-active { transition: all 0.2s ease; }
.list-item-enter-from { opacity: 0; transform: translateX(-12px); }
.list-item-leave-to { opacity: 0; transform: translateX(12px); }

/* ================================================================
   Pagination
   ================================================================ */
.pagination-wrap { margin-top: 24px; display: flex; justify-content: center; }

/* ================================================================
   File detail sidebar
   ================================================================ */
.detail-overlay {
  position: fixed; inset: 0; z-index: 1500;
  background: rgba(0,0,0,0.2); backdrop-filter: blur(2px);
}
.detail-panel {
  position: absolute; top: 0; right: 0; bottom: 0; width: 400px;
  background: #fff; box-shadow: -8px 0 32px rgba(0,0,0,0.1);
  display: flex; flex-direction: column; overflow-y: auto;
}
.detail-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 20px 24px 16px; border-bottom: 1px solid #f1f5f9;
  position: sticky; top: 0; background: #fff; z-index: 1;
}
.detail-title { font-size: 16px; font-weight: 700; color: #0f172a; margin: 0; }
.detail-close {
  width: 32px; height: 32px; display: flex; align-items: center; justify-content: center;
  border: none; background: #f1f5f9; border-radius: 8px; cursor: pointer;
  color: #64748b; transition: all 0.15s;
}
.detail-close:hover { background: #e2e8f0; color: #1e293b; }

.detail-preview { padding: 20px 24px; }
.detail-img {
  width: 100%; border-radius: 12px; cursor: pointer;
  transition: transform 0.2s; max-height: 240px; object-fit: cover;
}
.detail-img:hover { transform: scale(1.02); }

.detail-audio-area { text-align: center; }
.detail-audio-icon {
  width: 80px; height: 80px; border-radius: 20px; margin: 0 auto 16px;
  display: flex; align-items: center; justify-content: center;
}
.detail-audio-player { width: 100%; border-radius: 8px; }
.detail-video-area { }
.detail-video-player { width: 100%; border-radius: 12px; max-height: 240px; }

.detail-icon-area {
  width: 100%; height: 160px; border-radius: 12px;
  display: flex; flex-direction: column; align-items: center;
  justify-content: center; gap: 8px;
}
.detail-ext { font-size: 13px; font-weight: 700; text-transform: uppercase; color: #64748b; }

.detail-meta { padding: 0 24px 20px; }
.dm-row {
  display: flex; align-items: flex-start; justify-content: space-between;
  padding: 10px 0; border-bottom: 1px solid #f8fafc;
}
.dm-row:last-child { border-bottom: none; }
.dm-label { font-size: 13px; color: #94a3b8; flex-shrink: 0; width: 80px; }
.dm-value { font-size: 13px; color: #1e293b; font-weight: 500; text-align: right; flex: 1; min-width: 0; }
.dm-filename { word-break: break-all; line-height: 1.5; }

.detail-actions { padding: 0 24px 20px; display: flex; flex-direction: column; gap: 8px; }
.da-btn {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  padding: 10px; border: 1.5px solid #e2e8f0; border-radius: 10px;
  background: #fff; font-size: 13px; font-weight: 600; color: #475569;
  cursor: pointer; transition: all 0.15s;
}
.da-btn:hover { background: #f8fafc; border-color: #cbd5e1; }
.da-primary { background: #0f172a; border-color: #0f172a; color: #fff; }
.da-primary:hover { background: #1e293b; }
.da-danger { color: #ef4444; border-color: #fecaca; }
.da-danger:hover { background: #fef2f2; }

.detail-url { padding: 0 24px 24px; }
.url-box {
  display: flex; align-items: center; gap: 8px; padding: 10px 12px;
  background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px;
  cursor: pointer; transition: all 0.15s; margin-top: 6px;
}
.url-box:hover { background: #f1f5f9; border-color: #cbd5e1; }
.url-text {
  flex: 1; font-size: 12px; color: #64748b; white-space: nowrap;
  overflow: hidden; text-overflow: ellipsis; font-family: 'SF Mono', 'Fira Code', monospace;
}

/* ================================================================
   Context menu
   ================================================================ */
.ctx-menu {
  position: fixed; z-index: 2100;
  background: #fff; border: 1px solid #e2e8f0; border-radius: 14px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.14), 0 4px 12px rgba(0,0,0,0.04);
  padding: 6px; min-width: 170px;
  transform-origin: top left;
}
.ctx-item {
  display: flex; align-items: center; gap: 10px; width: 100%;
  padding: 9px 14px; border: none; background: none; border-radius: 8px;
  font-size: 13px; color: #475569; cursor: pointer; transition: all 0.12s;
}
.ctx-item:hover { background: #f1f5f9; color: #1e293b; }
.ctx-danger { color: #ef4444; }
.ctx-danger:hover { background: #fef2f2; color: #dc2626; }
.ctx-divider { height: 1px; background: #f1f5f9; margin: 4px 8px; }

/* ctx transitions */
.ctx-enter-active { transition: all 0.15s cubic-bezier(0.4, 0, 0.2, 1); }
.ctx-leave-active { transition: all 0.1s ease; }
.ctx-enter-from { opacity: 0; transform: scale(0.92); }
.ctx-leave-to { opacity: 0; transform: scale(0.95); }

/* ================================================================
   Lightbox
   ================================================================ */
.lb-overlay {
  position: fixed; inset: 0; z-index: 2000;
  background: rgba(0,0,0,0.92); backdrop-filter: blur(16px);
  display: flex; align-items: center; justify-content: center;
}
.lb-wrap {
  position: relative; width: 100%; height: 100%;
  display: flex; align-items: center; justify-content: center;
}
.lb-close {
  position: absolute; top: 16px; right: 20px; z-index: 10;
  width: 40px; height: 40px; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.08); border: 1px solid rgba(255,255,255,0.08); border-radius: 50%;
  cursor: pointer; color: #fff; transition: all 0.2s;
}
.lb-close:hover { background: rgba(255,255,255,0.16); border-color: rgba(255,255,255,0.12); }
.lb-title {
  position: absolute; top: 20px; left: 20px; right: 80px;
  color: rgba(255,255,255,0.7); font-size: 14px; font-weight: 500;
  white-space: nowrap; overflow: hidden; text-overflow: ellipsis; z-index: 10;
}
.lb-nav {
  position: absolute; top: 50%; transform: translateY(-50%); z-index: 10;
  width: 44px; height: 44px; display: flex; align-items: center; justify-content: center;
  background: rgba(255,255,255,0.06); border: 1px solid rgba(255,255,255,0.06); border-radius: 50%;
  cursor: pointer; color: #fff; transition: all 0.2s;
}
.lb-nav:hover { background: rgba(255,255,255,0.14); border-color: rgba(255,255,255,0.1); }
.lb-prev { left: 20px; }
.lb-next { right: 20px; }
.lb-content { max-width: 90vw; max-height: 82vh; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.lb-image {
  max-width: 90vw; max-height: 82vh; object-fit: contain; border-radius: 4px;
  user-select: none; transition: transform 0.15s ease;
}
.lb-audio { width: 400px; max-width: 90vw; }
.lb-video { max-width: 90vw; max-height: 82vh; border-radius: 4px; }
.lb-counter {
  position: absolute; bottom: 68px; left: 50%; transform: translateX(-50%);
  color: rgba(255,255,255,0.5); font-size: 13px; font-weight: 500;
  background: rgba(0,0,0,0.35); padding: 4px 14px; border-radius: 12px;
}
.lb-actions {
  position: absolute; bottom: 20px; left: 50%; transform: translateX(-50%);
  display: flex; align-items: center; gap: 6px;
  background: rgba(0,0,0,0.45); padding: 6px 8px;
  border-radius: 14px; backdrop-filter: blur(12px);
  border: 1px solid rgba(255,255,255,0.06);
}
.lb-act-btn {
  width: 36px; height: 36px; display: flex; align-items: center; justify-content: center;
  border: none; background: rgba(255,255,255,0.06); border-radius: 8px;
  cursor: pointer; color: rgba(255,255,255,0.75); transition: all 0.15s;
}
.lb-act-btn:hover { background: rgba(255,255,255,0.15); color: #fff; }
.lb-zoom-val {
  font-size: 11px; color: rgba(255,255,255,0.5); font-weight: 600;
  min-width: 40px; text-align: center; font-variant-numeric: tabular-nums;
}
.lb-act-sep { width: 1px; height: 20px; background: rgba(255,255,255,0.1); margin: 0 2px; }

/* lb transitions */
.lb-enter-active { transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1); }
.lb-leave-active { transition: all 0.2s ease; }
.lb-enter-from { opacity: 0; }
.lb-leave-to { opacity: 0; }
.lb-enter-from .lb-content { transform: scale(0.92); }

/* ================================================================
   Transitions
   ================================================================ */
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s ease; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

.slide-down-enter-active, .slide-down-leave-active { transition: all 0.3s ease; }
.slide-down-enter-from, .slide-down-leave-to { opacity: 0; transform: translateY(-8px); }

.slide-up-enter-active { transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-up-leave-active { transition: all 0.25s ease; }
.slide-up-enter-from { opacity: 0; transform: translateY(20px) scale(0.95); }
.slide-up-leave-to { opacity: 0; transform: translateY(10px) scale(0.97); }

.slide-right-enter-active, .slide-right-leave-active { transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1); }
.slide-right-enter-from .detail-panel, .slide-right-leave-to .detail-panel { transform: translateX(100%); }
.slide-right-enter-from, .slide-right-leave-to { opacity: 0; }

/* ================================================================
   Mobile responsive
   ================================================================ */
@media (max-width: 768px) {
  .page-title { font-size: 18px; }
  .title-icon { width: 32px; height: 32px; }
  .title-icon svg { width: 20px; height: 20px; }
  .header-main { flex-direction: column; align-items: stretch; }
  .header-actions { display: flex; }

  .category-scroll { flex-wrap: nowrap; }
  .toolbar-row { flex-direction: column; align-items: stretch; }
  .toolbar-left { flex-wrap: wrap; }
  .toolbar-right { justify-content: space-between; }

  .search-box, .search-box.focused { flex: 1; width: auto; }

  .file-grid {
    grid-template-columns: repeat(auto-fill, minmax(155px, 1fr));
    gap: 10px;
  }
  .card-thumb { height: 130px; }
  .card-checkbox { opacity: 1; }
  .card-actions { opacity: 1; }

  .lr-date { display: none; }
  .lh-date { display: none; }
  .la-btn { opacity: 1; }

  .detail-panel { width: 100%; }
  .lb-nav { width: 36px; height: 36px; }
  .lb-prev { left: 8px; }
  .lb-next { right: 8px; }

  .upload-panel { width: calc(100% - 32px); right: 16px; bottom: 16px; }
}

@media (max-width: 480px) {
  .file-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }
  .card-thumb { height: 110px; }
  .lr-size { display: none; }
  .lh-size { display: none; }
  .lr-type { display: none; }
  .lh-type { display: none; }
  .stat-badge { font-size: 11px; padding: 3px 8px; }
  .header-stats { flex-wrap: wrap; }
}
</style>
