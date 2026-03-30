<script setup>
import { ref, inject, onMounted } from 'vue'
import { getFriendLinks } from '@/api/friendLink'
import SidebarCard from '@/components/SidebarCard.vue'

const { articleTitle, articleMeta } = inject('setHero')

const links = ref([])
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const res = await getFriendLinks()
    links.value = res.data.data ?? []
  } catch {
    links.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  articleTitle.value = '友链'
  articleMeta.value = '朋友们的小站'
  load()
})
</script>

<template>
  <div class="links-page">
    <div class="links-layout">
      <div class="links-main">
        <div class="content-card">
          <div class="card-header">
            <i class="iconfont icon-lianjie" />
            <span>共 {{ links.length }} 位朋友</span>
          </div>

          <div v-if="loading" class="placeholder-grid">
            <div v-for="i in 6" :key="i" class="sk-card" />
          </div>

          <div v-else-if="links.length" class="link-grid">
            <a
              v-for="link in links"
              :key="link.id"
              :href="link.url"
              target="_blank"
              rel="noopener noreferrer"
              class="link-card"
            >
              <img
                v-if="link.avatarUrl"
                :src="link.avatarUrl"
                class="link-avatar"
                loading="lazy"
              />
              <span v-else class="link-avatar-letter">{{
                link.name ? link.name.charAt(0) : '?'
              }}</span>
              <div class="link-body">
                <p class="link-name">{{ link.name }}</p>
                <p class="link-desc">{{ link.description }}</p>
              </div>
            </a>
          </div>

          <div v-else class="empty-state">
            <div class="empty-icon">
              <svg viewBox="0 0 24 24" width="48" height="48" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M10 13a5 5 0 007.54.54l3-3a5 5 0 00-7.07-7.07l-1.72 1.71"/>
                <path d="M14 11a5 5 0 00-7.54-.54l-3 3a5 5 0 007.07 7.07l1.71-1.71"/>
              </svg>
            </div>
            <p class="empty-text">暂无友链</p>
            <p class="empty-sub">快来成为第一个朋友吧~</p>
          </div>
        </div>
      </div>

      <SidebarCard />
    </div>
  </div>
</template>

<style scoped>
.links-page {
  width: 100%;
}
.links-layout {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}
.links-main {
  flex: 1;
  min-width: 0;
}
.content-card {
  background: var(--blog-card, #fff);
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  border: 1px solid var(--blog-border-light, #ebeef5);
  padding: 24px 28px;
}
.card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--blog-text3, #909399);
  margin-bottom: 20px;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--blog-border-light, #ebeef5);
}
.card-header .iconfont {
  font-size: 16px;
  color: var(--blog-text2, #606266);
}

.placeholder-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}
.sk-card {
  height: 80px;
  background: var(--blog-border-light, #ebeef5);
  border-radius: 8px;
}

.link-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 14px;
}
.link-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--blog-border-light, #ebeef5);
  border-radius: 8px;
  text-decoration: none;
  color: inherit;
  background: var(--blog-hover, #fafafa);
  transition:
    transform 0.2s,
    box-shadow 0.2s,
    border-color 0.15s;
}
.link-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  border-color: #c0c4cc;
}
.link-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  object-fit: cover;
  flex-shrink: 0;
  border: 2px solid var(--blog-border-light, #ebeef5);
}
.link-avatar-letter {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e4e7ed;
  color: #606266;
  font-size: 20px;
  font-weight: 700;
  user-select: none;
}
.link-body {
  min-width: 0;
}
.link-name {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 3px;
  color: var(--blog-text, #303133);
}
.link-desc {
  font-size: 12px;
  color: var(--blog-text3, #909399);
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.5;
}

/* 空状态 */
.empty-state {
  text-align: center;
  padding: 50px 20px;
}
.empty-icon {
  color: var(--blog-text3, #c0c4cc);
  margin-bottom: 16px;
}
.empty-text {
  font-size: 16px;
  font-weight: 600;
  color: var(--blog-text2, #606266);
  margin: 0 0 6px;
}
.empty-sub {
  font-size: 13px;
  color: var(--blog-text3, #909399);
  margin: 0;
}

@media (max-width: 960px) {
  .links-layout {
    flex-direction: column;
  }
  .links-main {
    width: 100%;
  }
}
@media (max-width: 600px) {
  .content-card {
    padding: 16px;
  }
  .link-grid {
    grid-template-columns: 1fr;
  }
  .link-card {
    overflow: hidden;
    max-width: 100%;
  }
  .link-body {
    overflow: hidden;
  }
  .link-desc {
    white-space: normal;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
}
</style>
