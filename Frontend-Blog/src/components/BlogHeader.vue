<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from "vue"
import { useRouter } from "vue-router"
import { useBlogStore, useThemeStore } from "@/stores"

const router = useRouter()
const blogStore = useBlogStore()
const themeStore = useThemeStore()

const isDark = computed(() => {
  if (themeStore.mode === "dark") return true
  if (themeStore.mode === "light") return false
  return window.matchMedia("(prefers-color-scheme: dark)").matches
})

const scrolled = ref(false)
const handleScroll = () => { scrolled.value = window.scrollY > 60 }
onMounted(() => window.addEventListener("scroll", handleScroll, { passive: true }))
onUnmounted(() => window.removeEventListener("scroll", handleScroll))

const searchVisible = ref(false)
const keyword = ref("")
const searchInputRef = ref(null)
const mobileSearchInputRef = ref(null)
const mobileNavVisible = ref(false)

const isPlaying = ref(false)
const audioRef = ref(null)
const musicIndex = ref(0)
const musicListVisible = ref(false)
const currentTrack = computed(() => blogStore.musics[musicIndex.value] || null)

const togglePlay = () => {
  if (!audioRef.value || !currentTrack.value) return
  isPlaying.value ? audioRef.value.pause() : audioRef.value.play()
  isPlaying.value = !isPlaying.value
}
const playTrack = (index) => {
  musicIndex.value = index
  isPlaying.value = false
  nextTick(() => {
    if (audioRef.value) {
      audioRef.value.load()
      audioRef.value.play().then(() => { isPlaying.value = true }).catch(() => {})
    }
  })
}
const nextTrack = () => {
  if (!blogStore.musics.length) return
  playTrack((musicIndex.value + 1) % blogStore.musics.length)
}
const toggleMusicList = () => { musicListVisible.value = !musicListVisible.value }

const navItems = [
  { label: "主页", icon: "icon-zhuye", href: "https://darker.one", external: true },
  { label: "博客", icon: "icon-boke", to: "/" },
  { label: "归档", icon: "icon-guidang", to: "/archive" },
  { label: "友链", icon: "icon-lianjie", to: "/links" },
  { label: "留言", icon: "icon-liuyan", to: "/message" },
  { label: "关于", icon: "icon-guanyu", to: "/about" },
  { label: "开往", icon: "icon-subway", href: "https://www.travellings.cn/go.html", external: true },
]

const doSearch = () => {
  const kw = keyword.value.trim()
  if (!kw) return
  searchVisible.value = false
  mobileNavVisible.value = false
  keyword.value = ""
  router.push({ path: "/", query: { search: kw } })
}

/* Desktop: inline toggle (original) */
const toggleSearch = () => {
  mobileNavVisible.value = false
  searchVisible.value = !searchVisible.value
  if (!searchVisible.value) {
    keyword.value = ""
  } else {
    nextTick(() => {
      if (window.innerWidth <= 768) {
        mobileSearchInputRef.value?.focus()
      } else {
        searchInputRef.value?.focus()
      }
    })
  }
}

const toggleMobileNav = () => {
  searchVisible.value = false
  keyword.value = ""
  mobileNavVisible.value = !mobileNavVisible.value
}

const navTo = (item) => {
  mobileNavVisible.value = false
  if (item.external) { window.open(item.href, "_blank") }
  else { router.push(item.to) }
}
</script>

<template>
  <header class="site-header" :class="{ scrolled, dark: isDark }">
    <div class="header-inner">
      <div class="header-left">
        <router-link to="/" class="site-title">My Blog</router-link>
        <nav class="nav-desktop">
          <template v-for="item in navItems" :key="item.label">
            <a v-if="item.external" :href="item.href" target="_blank" rel="noopener" class="nav-link">
              <i :class="['iconfont', item.icon]" /> {{ item.label }}
            </a>
            <router-link v-else :to="item.to" class="nav-link">
              <i :class="['iconfont', item.icon]" /> {{ item.label }}
            </router-link>
          </template>
        </nav>
      </div>
      <div class="header-right">
        <div v-if="currentTrack" class="mini-player-wrap">
          <div class="mini-player" @click="toggleMusicList">
            <img v-if="currentTrack.coverImage" :src="currentTrack.coverImage" class="player-cover" :class="{ spinning: isPlaying }" />
            <span class="player-title">{{ currentTrack.title }}</span>
          </div>
          <button class="player-btn" @click.stop="togglePlay" title="播放/暂停">
            <i class="iconfont" :class="isPlaying ? 'icon-zanting' : 'icon-play-full'" />
          </button>
          <button class="player-btn" @click.stop="nextTrack" title="下一首">
            <i class="iconfont icon-next" />
          </button>
          <audio ref="audioRef" :src="currentTrack.musicUrl" preload="none" @ended="nextTrack" />
          <transition name="fade">
            <div v-show="musicListVisible" class="music-panel">
              <div class="music-panel-header">
                <span><i class="iconfont icon-yinle" /> 播放列表</span>
                <span class="music-panel-count">{{ blogStore.musics.length }} 首</span>
              </div>
              <ul class="music-panel-list">
                <li v-for="(m, idx) in blogStore.musics" :key="m.id" class="music-panel-item" :class="{ active: idx === musicIndex }" @click="playTrack(idx)">
                  <img v-if="m.coverImage" :src="m.coverImage" class="music-panel-cover" />
                  <div class="music-panel-info">
                    <span class="music-panel-name">{{ m.title }}</span>
                    <span class="music-panel-artist">{{ m.artist }}</span>
                  </div>
                  <i v-if="idx === musicIndex && isPlaying" class="iconfont icon-yinle playing-icon" />
                </li>
              </ul>
            </div>
          </transition>
        </div>
        <button class="theme-toggle" :title="isDark ? '切换到浅色模式' : '切换到暗色模式'" @click="themeStore.toggle">
          <svg v-if="isDark" viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
          <svg v-else viewBox="0 0 24 24" width="17" height="17" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/></svg>
        </button>

        <!-- Desktop: inline expanding search (original style) -->
        <div class="search-area">
          <div class="search-box" :class="{ expanded: searchVisible }">
            <input
              ref="searchInputRef"
              v-show="searchVisible"
              v-model="keyword"
              type="text"
              placeholder="搜索文章..."
              class="search-input"
              @keyup.enter="doSearch"
              @blur="searchVisible = false"
            />
          </div>
          <button class="search-toggle" @click="toggleSearch" title="搜索">
            <i class="iconfont icon-sousuo" />
          </button>
        </div>

        <button class="mobile-menu-btn" @click="toggleMobileNav"><span :class="['bar', { open: mobileNavVisible }]" /></button>
      </div>
    </div>

    <!-- Mobile: slide-down search bar -->
    <transition name="mobile-search-slide">
      <div v-if="searchVisible" class="mobile-search-bar">
        <div class="mobile-search-bar-inner">
          <input ref="mobileSearchInputRef" v-model="keyword" type="text" placeholder="搜索文章…" class="mobile-search-input" @keyup.enter="doSearch" />
          <button class="mobile-search-btn" @click="doSearch">
            <i class="iconfont icon-sousuo" />
          </button>
        </div>
      </div>
    </transition>

    <nav v-show="mobileNavVisible" class="nav-mobile">
      <a v-for="item in navItems" :key="item.label" class="nav-mobile-link" @click="navTo(item)">
        <i :class="['iconfont', item.icon]" /> {{ item.label }}
      </a>
      <a class="nav-mobile-link" @click="toggleSearch"><i class="iconfont icon-sousuo" /> 搜索</a>
      <a class="nav-mobile-link" @click="themeStore.toggle">
        <template v-if="isDark"><svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px;margin-right:4px"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>浅色模式</template>
        <template v-else><svg viewBox="0 0 24 24" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="vertical-align:-2px;margin-right:4px"><path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/></svg>暗色模式</template>
      </a>
    </nav>
  </header>
</template>

<style scoped>
.site-header{position:fixed;top:0;left:0;right:0;z-index:100;background:transparent;transition:background .3s,box-shadow .3s,backdrop-filter .3s}
.site-header.scrolled{background:rgba(255,255,255,.92);backdrop-filter:blur(12px);box-shadow:0 1px 12px rgba(0,0,0,.06)}
.site-header.dark.scrolled{background:rgba(30,30,30,.92);backdrop-filter:blur(12px);box-shadow:0 1px 12px rgba(0,0,0,.35)}
.header-inner{max-width:1200px;margin:0 auto;padding:0 28px;height:58px;display:flex;align-items:center;justify-content:space-between}
.header-left{display:flex;align-items:center;gap:28px;min-width:0}
.site-title{font-family:var(--blog-serif);font-size:18px;font-weight:800;color:#fff;text-decoration:none;letter-spacing:.5px;white-space:nowrap;text-shadow:0 1px 4px rgba(0,0,0,.3);transition:color .3s,text-shadow .3s}
.scrolled .site-title{color:#303133;text-shadow:none}
.dark.scrolled .site-title{color:#e5e5e5}
.nav-desktop{display:flex;align-items:center;gap:2px}
.nav-link{font-size:13px;color:rgba(255,255,255,.88);text-decoration:none;padding:6px 11px;border-radius:6px;white-space:nowrap;display:inline-flex;align-items:center;gap:4px;text-shadow:0 1px 3px rgba(0,0,0,.2);transition:color .2s,background .2s}
.nav-link .iconfont{font-size:14px}
.nav-link:hover{color:#fff;background:rgba(255,255,255,.15)}
.scrolled .nav-link{color:#5a5e66;text-shadow:none}
.scrolled .nav-link:hover{color:#303133;background:#f2f4f7}
.dark.scrolled .nav-link{color:#b0b0b0}
.dark.scrolled .nav-link:hover{color:#e5e5e5;background:rgba(255,255,255,.08)}
.header-right{display:flex;align-items:center;gap:8px;flex-shrink:0}

.mini-player-wrap{display:flex;align-items:center;gap:4px;position:relative}
.mini-player{display:flex;align-items:center;gap:8px;padding:4px 10px 4px 4px;border-radius:20px;background:rgba(255,255,255,.12);border:1px solid rgba(255,255,255,.2);cursor:pointer;backdrop-filter:blur(6px);transition:background .2s,border-color .2s,box-shadow .2s}
.mini-player:hover{background:rgba(255,255,255,.18)}
.scrolled .mini-player{background:#f5f7fa;border-color:#e4e7ed}
.scrolled .mini-player:hover{background:#edf0f4;box-shadow:0 1px 4px rgba(0,0,0,.05)}
.dark.scrolled .mini-player{background:rgba(255,255,255,.06);border-color:rgba(255,255,255,.12)}
.dark.scrolled .mini-player:hover{background:rgba(255,255,255,.1)}
.player-cover{width:28px;height:28px;border-radius:50%;object-fit:cover;border:1px solid rgba(255,255,255,.3);flex-shrink:0}
.scrolled .player-cover{border-color:#e4e7ed}
.player-cover.spinning{animation:spin 8s linear infinite}
@keyframes spin{from{transform:rotate(0)}to{transform:rotate(360deg)}}
.player-title{font-size:12px;color:rgba(255,255,255,.9);max-width:80px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;transition:color .3s}
.scrolled .player-title{color:#606266}
.dark.scrolled .player-title{color:#b0b0b0}
.player-btn{background:none;border:none;cursor:pointer;padding:4px;color:rgba(255,255,255,.85);font-size:15px;transition:color .2s,transform .15s;display:flex;align-items:center}
.player-btn:hover{color:#fff;transform:scale(1.1)}
.player-btn:active{transform:scale(.9)}
.scrolled .player-btn{color:#606266}
.scrolled .player-btn:hover{color:#303133}
.dark.scrolled .player-btn{color:#b0b0b0}
.dark.scrolled .player-btn:hover{color:#e5e5e5}

.music-panel{position:absolute;top:calc(100% + 8px);right:0;width:280px;max-height:360px;background:var(--blog-card);border:1px solid var(--blog-border);border-radius:8px;box-shadow:0 6px 24px rgba(0,0,0,.1);z-index:200;overflow:hidden}
.music-panel-header{display:flex;justify-content:space-between;align-items:center;padding:10px 14px;border-bottom:1px solid #ebeef5;font-size:13px;font-weight:600;color:#303133}
.music-panel-count{font-size:12px;color:#909399;font-weight:400}
.music-panel-list{list-style:none;margin:0;padding:4px 0;max-height:300px;overflow-y:auto}
.music-panel-item{display:flex;align-items:center;gap:10px;padding:8px 14px;cursor:pointer;transition:background .12s}
.music-panel-item:hover{background:#f5f7fa}
.music-panel-item.active .music-panel-name{color:#000;font-weight:600}
.music-panel-cover{width:36px;height:36px;border-radius:4px;object-fit:cover;flex-shrink:0;border:1px solid #ebeef5}
.music-panel-info{flex:1;min-width:0}
.music-panel-name{display:block;font-size:13px;color:#303133;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.music-panel-artist{display:block;font-size:11px;color:#909399;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.playing-icon{font-size:14px;color:#000;flex-shrink:0}
.fade-enter-active,.fade-leave-active{transition:opacity .15s,transform .15s}
.fade-enter-from,.fade-leave-to{opacity:0;transform:translateY(-4px)}

.theme-toggle{background:none;border:none;cursor:pointer;padding:6px;color:rgba(255,255,255,.85);display:flex;align-items:center;transition:color .2s,transform .2s;border-radius:50%}
.theme-toggle:hover{color:#fff;transform:scale(1.1)}
.theme-toggle:active{transform:scale(.95)}
.scrolled .theme-toggle{color:#606266}
.scrolled .theme-toggle:hover{color:#303133;background:rgba(0,0,0,.04)}
.dark.scrolled .theme-toggle{color:#b0b0b0}
.dark.scrolled .theme-toggle:hover{color:#e5e5e5;background:rgba(255,255,255,.06)}

/* Desktop: inline expanding search */
.search-area{display:flex;align-items:center;gap:0}
.search-box{overflow:hidden;width:0;transition:width .35s cubic-bezier(.4,0,.2,1)}
.search-box.expanded{width:220px}
.search-input{width:100%;border:1.5px solid rgba(255,255,255,.35);border-radius:20px;padding:7px 16px;font-size:13.5px;background:rgba(255,255,255,.15);backdrop-filter:blur(8px);color:#fff;outline:none;font-family:inherit;transition:border-color .25s,background .25s,color .25s,box-shadow .25s}
.search-input::placeholder{color:rgba(255,255,255,.6)}
.search-input:focus{border-color:rgba(255,255,255,.7);background:rgba(255,255,255,.22);box-shadow:0 0 0 3px rgba(255,255,255,.08)}
.scrolled .search-input{background:#f5f7fa;border-color:#dcdfe6;color:#303133;box-shadow:none}
.scrolled .search-input::placeholder{color:#b0b0b0}
.scrolled .search-input:focus{border-color:#a0a4ab;background:#fff;box-shadow:0 0 0 3px rgba(0,0,0,.04)}
.dark.scrolled .search-input{background:rgba(255,255,255,.08);border-color:rgba(255,255,255,.18);color:#e5e5e5}
.dark.scrolled .search-input::placeholder{color:rgba(255,255,255,.4)}
.dark.scrolled .search-input:focus{border-color:rgba(255,255,255,.35);background:rgba(255,255,255,.12);box-shadow:0 0 0 3px rgba(255,255,255,.06)}

.search-toggle{background:none;border:none;cursor:pointer;padding:6px 8px;color:rgba(255,255,255,.85);font-size:16px;transition:color .2s,transform .2s;display:flex;align-items:center;border-radius:50%}
.search-toggle:hover{color:#fff;transform:scale(1.1)}
.search-toggle:active{transform:scale(.95)}
.scrolled .search-toggle{color:#606266}
.scrolled .search-toggle:hover{color:#303133;background:rgba(0,0,0,.04)}
.dark.scrolled .search-toggle{color:#b0b0b0}
.dark.scrolled .search-toggle:hover{color:#e5e5e5;background:rgba(255,255,255,.06)}

.mobile-menu-btn{display:none;background:none;border:none;cursor:pointer;width:28px;height:28px;position:relative;flex-shrink:0}
.bar,.bar::before,.bar::after{display:block;width:18px;height:2px;background:#fff;position:absolute;left:5px;transition:transform .2s,background .3s}
.scrolled .bar,.scrolled .bar::before,.scrolled .bar::after{background:#303133}
.dark.scrolled .bar,.dark.scrolled .bar::before,.dark.scrolled .bar::after{background:#e5e5e5}
.bar{top:13px}
.bar::before{content:"";top:-6px}
.bar::after{content:"";top:6px}
.bar.open{background:transparent}
.bar.open::before{top:0;transform:rotate(45deg)}
.bar.open::after{top:0;transform:rotate(-45deg)}

.nav-mobile{display:none;background:rgba(255,255,255,.96);backdrop-filter:blur(8px);padding:8px 24px 12px;border-top:1px solid #ebeef5;box-shadow:0 4px 12px rgba(0,0,0,.04)}
.dark .nav-mobile{background:rgba(35,35,35,.96);border-top-color:#333}
.nav-mobile-link{display:flex;align-items:center;gap:6px;padding:10px 0;font-size:14px;color:#606266;text-decoration:none;border-bottom:1px solid #f2f6fc;cursor:pointer}
.nav-mobile-link .iconfont{font-size:15px}
.nav-mobile-link:hover{color:#303133}
.dark .nav-mobile-link{color:#b0b0b0;border-bottom-color:#333}
.dark .nav-mobile-link:hover{color:#e5e5e5}

/* Mobile slide-down search bar (hidden on desktop) */
.mobile-search-bar{display:none}

@media(max-width:768px){
  .nav-desktop{display:none}
  .mini-player-wrap{gap:2px}
  .mini-player{padding:3px 6px 3px 3px;gap:4px}
  .player-cover{width:24px;height:24px}
  .player-title{max-width:60px;font-size:11px}
  .player-btn{padding:3px;font-size:13px}
  .music-panel{position:fixed;top:58px;right:8px;left:8px;width:auto;max-height:50vh}
  .theme-toggle{display:none}
  .mobile-menu-btn{display:block}
  .nav-mobile{display:flex;flex-direction:column}
  .header-inner{padding:0 12px}
  .header-right{gap:6px}

  /* Hide desktop inline search on mobile */
  .search-area{display:none}

  /* Mobile slide-down search bar */
  .mobile-search-bar{
    display:block;
    background:#2c3e50;
    padding:10px 16px;
    box-shadow:0 4px 12px rgba(0,0,0,.15);
    overflow:hidden;
  }
  .dark .mobile-search-bar{
    background:#1a2332;
  }
  .mobile-search-bar-inner{
    display:flex;
    align-items:center;
    gap:0;
    max-width:600px;
    margin:0 auto;
  }
  .mobile-search-input{
    flex:1;
    height:40px;
    padding:0 14px;
    border:1.5px solid rgba(255,255,255,.25);
    border-right:none;
    border-radius:6px 0 0 6px;
    background:#fff;
    font-size:15px;
    color:#303133;
    outline:none;
    font-family:inherit;
    -webkit-appearance:none;
  }
  .mobile-search-input::placeholder{color:#aaa}
  .dark .mobile-search-input{background:#2a2a2a;color:#e5e5e5;border-color:rgba(255,255,255,.15)}
  .dark .mobile-search-input::placeholder{color:#666}
  .mobile-search-btn{
    display:flex;
    align-items:center;
    justify-content:center;
    width:44px;
    height:40px;
    border:none;
    border-radius:0 6px 6px 0;
    background:rgba(255,255,255,.15);
    color:rgba(255,255,255,.9);
    font-size:18px;
    cursor:pointer;
    transition:background .15s;
    flex-shrink:0;
  }
  .mobile-search-btn:active{background:rgba(255,255,255,.25)}

  .mobile-search-slide-enter-active{transition:max-height .25s ease-out,opacity .2s ease-out,padding .25s ease-out}
  .mobile-search-slide-leave-active{transition:max-height .2s ease-in,opacity .15s ease-in,padding .2s ease-in}
  .mobile-search-slide-enter-from,.mobile-search-slide-leave-to{max-height:0;opacity:0;padding-top:0;padding-bottom:0;overflow:hidden}
  .mobile-search-slide-enter-to,.mobile-search-slide-leave-from{max-height:80px;opacity:1}
}
</style>
