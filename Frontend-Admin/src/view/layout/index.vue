<!-- build: 20260330-v8 Admin Layout · Blog-aligned UI + refined topbar + polished sidebar -->
<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from "vue"
import { useRoute } from "vue-router"
import { useUserStore } from "@/stores"

const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)
const mobileMenuOpen = ref(false)
const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
  if (!isMobile.value) mobileMenuOpen.value = false
}

onMounted(() => {
  checkMobile()
  window.addEventListener("resize", checkMobile)
})
onUnmounted(() => {
  window.removeEventListener("resize", checkMobile)
})

const activeMenu = computed(() => route.path)
const isEditorPage = computed(() => route.path.startsWith("/article/edit"))

watch(route, () => {
  if (isMobile.value) mobileMenuOpen.value = false
})

const navGroups = [
  {
    label: "概览",
    items: [
      { path: "/dashboard", icon: "icon-yibiaopan", label: "仪表盘" },
    ],
  },
  {
    label: "内容管理",
    items: [
      { path: "/article/list", icon: "icon-bianjiwenzhang_huaban", label: "文章管理" },
      { path: "/category", icon: "icon-folder", label: "分类 / 标签" },
      { path: "/music", icon: "icon-music", label: "音乐管理" },
      { path: "/file-library", icon: "icon-image-fill", label: "文件库" },
    ],
  },
  {
    label: "互动管理",
    items: [
      { path: "/comment", icon: "icon-comment", label: "评论管理" },
      { path: "/message", icon: "icon-liuyan", label: "留言管理" },
      { path: "/rss", icon: "icon-rss", label: "RSS 订阅" },
    ],
  },
  {
    label: "站点生态",
    items: [
      { path: "/friend-link", icon: "icon-link", label: "友链管理" },
    ],
  },
  {
    label: "数据统计",
    items: [
      { path: "/visitor", icon: "icon-user", label: "访客管理" },
      { path: "/view-record", icon: "icon-eye", label: "浏览记录" },
      { path: "/operation-log", icon: "icon-wj-rz", label: "操作日志" },
    ],
  },
  {
    label: "系统",
    items: [
      { path: "/profile", icon: "icon-iconfontprofile", label: "个人资料" },
      { path: "/settings", icon: "icon-setting", label: "系统设置" },
    ],
  },
]

const handleLogout = () => {
  ElMessageBox.confirm("确认退出登录？", "提示", {
    confirmButtonText: "退出",
    cancelButtonText: "取消",
    type: "warning",
  }).then(() => {
    userStore.logoutAction()
  })
}
</script>

<template>
  <div class="admin-shell">
    <!-- Mobile overlay -->
    <transition name="overlay-fade">
      <div
        v-if="isMobile && mobileMenuOpen"
        class="sidebar-overlay"
        @click="mobileMenuOpen = false"
      />
    </transition>

    <!-- Sidebar -->
    <aside
      :class="[
        'sidebar',
        { collapsed: !isMobile && collapsed },
        { 'mobile-open': isMobile && mobileMenuOpen },
        { 'mobile-hidden': isMobile && !mobileMenuOpen }
      ]"
    >
      <!-- Logo -->
      <div class="sidebar-logo">
        <div class="logo-mark">
          <span class="iconfont icon-guanliduan" />
        </div>
        <transition name="fade-text">
          <span v-if="isMobile || !collapsed" class="logo-text">FelixZoe</span>
        </transition>
        <button v-if="isMobile" class="mobile-close-btn" @click="mobileMenuOpen = false">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>

      <!-- Navigation -->
      <nav class="sidebar-nav">
        <div v-for="(group, gi) in navGroups" :key="gi" class="nav-group">
          <div v-if="isMobile || !collapsed" class="nav-group-label">
            <span class="nav-group-label-text">{{ group.label }}</span>
          </div>
          <div v-else-if="gi > 0" class="nav-group-divider" />

          <router-link
            v-for="item in group.items"
            :key="item.path"
            :to="item.path"
            :class="['nav-item', { active: activeMenu.startsWith(item.path) }]"
          >
            <span :class="['nav-icon', 'iconfont', item.icon]" />
            <transition name="fade-text">
              <span v-if="isMobile || !collapsed" class="nav-label">{{ item.label }}</span>
            </transition>
            <span v-if="activeMenu.startsWith(item.path) && (isMobile || !collapsed)" class="nav-active-dot" />
          </router-link>
        </div>
      </nav>

      <!-- Collapse toggle (desktop) -->
      <button v-if="!isMobile" class="collapse-btn" @click="collapsed = !collapsed">
        <span class="collapse-btn-inner">
          <svg :class="{ rotated: collapsed }" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"/></svg>
        </span>
      </button>
    </aside>

    <!-- Main content area -->
    <div class="main-wrapper">
      <!-- Topbar -->
      <header class="topbar">
        <div class="topbar-left">
          <button v-if="isMobile" class="hamburger-btn" @click="mobileMenuOpen = !mobileMenuOpen">
            <span class="hamburger-line" />
            <span class="hamburger-line" />
            <span class="hamburger-line" />
          </button>
          <el-breadcrumb separator="/" class="topbar-breadcrumb">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item>{{ $route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <div class="user-chip">
            <div class="user-avatar">
              <span class="iconfont icon-user" />
            </div>
            <span class="user-name-text">{{ userStore.userInfo?.nickname || "管理员" }}</span>
          </div>
          <button class="logout-btn" @click="handleLogout" title="退出登录">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
            <span class="logout-text">退出</span>
          </button>
        </div>
      </header>

      <!-- Page content -->
      <main :class="['page-main', { 'editor-page': isEditorPage }]">
        <router-view v-slot="{ Component }">
          <transition name="page-fade" mode="out-in">
            <component :is="Component" :key="$route.fullPath" />
          </transition>
        </router-view>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* ======== Shell ======== */
.admin-shell {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
  overflow: hidden;
}

/* ======== Sidebar ======== */
.sidebar {
  width: 240px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  transition: width 0.3s cubic-bezier(.4,0,.2,1), transform 0.3s ease, box-shadow 0.3s ease;
  flex-shrink: 0;
  z-index: 1001;
  position: relative;
  overflow: hidden;
  border-right: 1px solid #ebeef5;
}

.sidebar.collapsed {
  width: 68px;
}

/* ---- Logo ---- */
.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  gap: 10px;
  flex-shrink: 0;
  position: relative;
}

.sidebar-logo::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 16px;
  right: 16px;
  height: 1px;
  background: #f0f2f5;
}

.logo-mark {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: transform 0.3s cubic-bezier(.4,0,.2,1);
}

.logo-mark:hover {
  transform: rotate(-6deg) scale(1.04);
}

.logo-mark .iconfont {
  font-size: 15px;
  color: #ffffff;
}

.logo-text {
  font-size: 16px;
  font-weight: 700;
  color: #1a1a1a;
  letter-spacing: 0.3px;
  white-space: nowrap;
}

/* ---- Nav ---- */
.sidebar-nav {
  flex: 1;
  padding: 8px 10px;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.sidebar-nav::-webkit-scrollbar {
  width: 3px;
}

.sidebar-nav::-webkit-scrollbar-thumb {
  background: transparent;
  border-radius: 3px;
}

.sidebar-nav:hover::-webkit-scrollbar-thumb {
  background: #e4e7ed;
}

.nav-group {
  margin-bottom: 2px;
}

.nav-group-label {
  padding: 14px 8px 6px;
  user-select: none;
}

.nav-group-label-text {
  font-size: 11px;
  font-weight: 700;
  color: #c0c4cc;
  text-transform: uppercase;
  letter-spacing: 1.5px;
}

.nav-group-divider {
  height: 1px;
  background: #f0f2f5;
  margin: 8px 10px;
}

/* ---- Nav items ---- */
.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 10px;
  height: 38px;
  margin: 1px 0;
  color: #606266;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(.4,0,.2,1);
  text-decoration: none;
  white-space: nowrap;
  overflow: hidden;
  border-radius: 8px;
  -webkit-tap-highlight-color: transparent;
}

.nav-item:hover {
  background: #f5f7fa;
  color: #303133;
}

.nav-item:active {
  transform: scale(0.98);
}

.nav-item.active {
  background: #1a1a1a;
  color: #ffffff;
  font-weight: 600;
  box-shadow: 0 2px 8px rgba(0,0,0,0.12);
}

.nav-item.active:hover {
  background: #2d2d2d;
  color: #ffffff;
}

.nav-icon {
  font-size: 16px;
  flex-shrink: 0;
  width: 20px;
  text-align: center;
  transition: transform 0.2s ease;
}

.nav-item:hover .nav-icon {
  transform: translateX(1px);
}

.nav-item.active .nav-icon {
  transform: none;
}

.nav-label {
  transition: opacity 0.2s;
}

/* Active dot indicator */
.nav-active-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ffffff;
  margin-left: auto;
  flex-shrink: 0;
  opacity: 0.7;
  animation: dotPulse 2s ease-in-out infinite;
}

@keyframes dotPulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.2); }
}

/* ---- Collapse btn ---- */
.collapse-btn {
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
  -webkit-tap-highlight-color: transparent;
  position: relative;
}

.collapse-btn::before {
  content: '';
  position: absolute;
  top: 0;
  left: 16px;
  right: 16px;
  height: 1px;
  background: #f0f2f5;
}

.collapse-btn-inner {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 6px;
  color: #c0c4cc;
  transition: all 0.2s ease;
}

.collapse-btn-inner svg {
  transition: transform 0.3s cubic-bezier(.4,0,.2,1);
}

.collapse-btn-inner svg.rotated {
  transform: rotate(180deg);
}

.collapse-btn:hover .collapse-btn-inner {
  color: #303133;
  background: #f5f7fa;
}

.collapse-btn:active .collapse-btn-inner {
  transform: scale(0.9);
}

/* ---- Mobile close ---- */
.mobile-close-btn {
  margin-left: auto;
  background: none;
  border: none;
  color: #909399;
  cursor: pointer;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.15s;
  -webkit-tap-highlight-color: transparent;
}

.mobile-close-btn:hover {
  color: #303133;
}

/* ---- Overlay ---- */
.sidebar-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  backdrop-filter: blur(3px);
  z-index: 1000;
  -webkit-tap-highlight-color: transparent;
}

.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.25s ease;
}
.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}

/* ---- Text fade transition ---- */
.fade-text-enter-active { transition: opacity 0.2s ease 0.1s; }
.fade-text-leave-active { transition: opacity 0.15s ease; }
.fade-text-enter-from,
.fade-text-leave-to { opacity: 0; }

/* ======== Main Wrapper ======== */
.main-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

/* ======== Topbar ======== */
.topbar {
  height: 56px;
  background: #ffffff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
}

/* ---- Breadcrumb ---- */
:deep(.el-breadcrumb) {
  font-size: 14px;
}

:deep(.el-breadcrumb__inner) {
  color: #909399 !important;
  font-weight: 400 !important;
}

:deep(.el-breadcrumb__inner.is-link) {
  color: #606266 !important;
  font-weight: 500 !important;
}

:deep(.el-breadcrumb__inner.is-link:hover) {
  color: #303133 !important;
}

/* ---- User chip ---- */
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 3px 12px 3px 3px;
  border-radius: 20px;
  background: #f5f7fa;
  transition: all 0.2s ease;
  cursor: default;
}

.user-chip:hover {
  background: #ebeef5;
}

.user-avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #1a1a1a;
  flex-shrink: 0;
}

.user-avatar .iconfont {
  font-size: 12px;
  color: #ffffff;
}

.user-name-text {
  font-size: 13px;
  font-weight: 500;
  color: #606266;
}

/* ---- Logout ---- */
.logout-btn {
  display: flex;
  align-items: center;
  gap: 5px;
  background: none;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 6px 12px;
  font-size: 13px;
  color: #909399;
  cursor: pointer;
  transition: all 0.25s ease;
  -webkit-tap-highlight-color: transparent;
}

.logout-btn:hover {
  border-color: #f56c6c;
  color: #f56c6c;
  background: #fef0f0;
}

.logout-btn:active {
  transform: scale(0.96);
}

/* ---- Hamburger ---- */
.hamburger-btn {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  background: none;
  border: none;
  cursor: pointer;
  padding: 6px;
  flex-shrink: 0;
  -webkit-tap-highlight-color: transparent;
}

.hamburger-line {
  width: 18px;
  height: 2px;
  background: #303133;
  border-radius: 1px;
  transition: all 0.2s;
}

/* ======== Page Main ======== */
.page-main {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  -webkit-overflow-scrolling: touch;
}

.page-main.editor-page {
  padding: 0;
  overflow: hidden;
}

/* Smooth page scrollbar */
.page-main::-webkit-scrollbar {
  width: 6px;
}

.page-main::-webkit-scrollbar-track {
  background: transparent;
}

.page-main::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.page-main::-webkit-scrollbar-thumb:hover {
  background: #c0c4cc;
}

/* ---- Transition ---- */
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.page-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}

.page-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}

/* ======== Mobile ======== */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 260px;
    transform: translateX(-100%);
    box-shadow: none;
    border-right: none;
  }

  .sidebar.mobile-open {
    transform: translateX(0);
    box-shadow: 4px 0 24px rgba(0, 0, 0, 0.1);
  }

  .sidebar.mobile-hidden {
    transform: translateX(-100%);
  }

  .topbar {
    padding: 0 12px;
    height: 50px;
  }

  .topbar-breadcrumb {
    font-size: 13px;
  }

  .user-name-text {
    display: none;
  }

  .logout-text {
    display: none;
  }

  .logout-btn {
    padding: 6px 8px;
  }

  .topbar-right {
    gap: 8px;
  }

  .page-main {
    padding: 12px;
  }

  .nav-item {
    height: 44px;
    padding: 0 14px;
    font-size: 15px;
  }

  .user-chip {
    padding: 2px;
    background: transparent;
  }
}
</style>
