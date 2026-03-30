<!-- build: 20260329-v4 Dashboard (front-end style) -->
<script setup>
import { ref, onMounted, onUnmounted, shallowRef } from 'vue'
import {
  getOverview,
  getViewStatistics,
  getVisitorStatistics,
  getArticleViewTop10,
  getProvinceDistribution
} from '@/api/report'
import { getConfigByKey } from '@/api/settings'
import * as echarts from 'echarts/core'
import { LineChart, BarChart, PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import dayjs from 'dayjs'

echarts.use([
  LineChart,
  BarChart,
  PieChart,
  TitleComponent,
  TooltipComponent,
  GridComponent,
  LegendComponent,
  CanvasRenderer
])

/* ---- Overview ---- */
const overview = ref({})
const loadingOverview = ref(false)

const statCards = [
  { key: 'totalViewCount', label: '总浏览量', icon: 'icon-eye', color: '#303133', bg: '#f5f7fa' },
  { key: 'totalVisitorCount', label: '总访客数', icon: 'icon-user', color: '#303133', bg: '#f5f7fa' },
  { key: 'todayViewCount', label: '今日浏览', icon: 'icon-today', color: '#303133', bg: '#f5f7fa' },
  { key: 'todayNewVisitorCount', label: '今日新访客', icon: 'icon-new', color: '#67c23a', bg: '#f0f9eb' },
  { key: 'totalArticleCount', label: '文章总数', icon: 'icon-bianjiwenzhang_huaban', color: '#303133', bg: '#f5f7fa' },
  { key: 'totalCommentCount', label: '评论总数', icon: 'icon-comment', color: '#303133', bg: '#f5f7fa' },
  { key: 'totalMessageCount', label: '留言总数', icon: 'icon-liuyanban-05', color: '#303133', bg: '#f5f7fa' },
  { key: 'pendingCommentCount', label: '待审评论', icon: 'icon-shenhe', color: '#e6a23c', bg: '#fdf6ec' },
  { key: 'pendingMessageCount', label: '待审留言', icon: 'icon-shenhe1', color: '#f56c6c', bg: '#fef0f0' }
]

const fetchOverview = async () => {
  loadingOverview.value = true
  try {
    const res = await getOverview()
    overview.value = res.data ?? {}
  } finally {
    loadingOverview.value = false
  }
}

/* ---- Run days ---- */
const runDays = ref(null)
const runStartDate = ref('')

const fetchRunDays = async () => {
  try {
    const res = await getConfigByKey('start-time')
    const dateStr = res.data?.configValue ?? ''
    if (dateStr) {
      runStartDate.value = dateStr
      const start = dayjs(dateStr)
      runDays.value = dayjs().diff(start, 'day')
    }
  } catch (e) {
    console.error('获取运行天数失败', e)
  }
}

/* ---- Charts ---- */
const viewChartEl = ref(null)
const visitorChartEl = ref(null)
const barChartEl = ref(null)
const pieChartEl = ref(null)
const viewChart = shallowRef(null)
const visitorChart = shallowRef(null)
const barChart = shallowRef(null)
const pieChart = shallowRef(null)

const makeShortcuts = () => [
  { text: '最近 7 天', value: () => [dayjs().subtract(6, 'day').toDate(), new Date()] },
  { text: '最近 30 天', value: () => [dayjs().subtract(29, 'day').toDate(), new Date()] }
]

const defaultRange = () => [
  dayjs().subtract(6, 'day').format('YYYY-MM-DD'),
  dayjs().format('YYYY-MM-DD')
]

const viewDateRange = ref(defaultRange())
const visitorDateRange = ref(defaultRange())

const makeLineOption = (title, categories, data, color) => ({
  tooltip: {
    trigger: 'axis',
    backgroundColor: '#ffffff',
    borderColor: '#e4e7ed',
    borderWidth: 1,
    textStyle: { color: '#303133', fontSize: 13 },
    extraCssText: 'box-shadow: 0 4px 16px rgba(0,0,0,.06); border-radius: 8px;'
  },
  grid: { left: 44, right: 20, top: 24, bottom: 30 },
  xAxis: {
    type: 'category',
    data: categories,
    axisLine: { lineStyle: { color: '#ebeef5' } },
    axisTick: { show: false },
    axisLabel: { color: '#909399', fontSize: 11, margin: 12 }
  },
  yAxis: {
    type: 'value',
    splitLine: { lineStyle: { color: '#f2f6fc', type: 'dashed' } },
    axisLabel: { color: '#909399', fontSize: 11 }
  },
  series: [
    {
      name: title,
      type: 'line',
      data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { color, width: 2 },
      itemStyle: { color, borderWidth: 2, borderColor: '#fff' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: color + '18' },
            { offset: 1, color: color + '02' }
          ]
        }
      }
    }
  ]
})

const splitStr = (s) => (s ? s.split(',') : [])
const splitNum = (s) => (s ? s.split(',').map(Number) : [])

const fetchViewChart = async () => {
  const [begin, end] = viewDateRange.value
  const res = await getViewStatistics({ begin, end })
  const vo = res.data ?? {}
  viewChart.value?.setOption(
    makeLineOption('浏览量', splitStr(vo.dateList), splitNum(vo.viewCountList), '#303133')
  )
}

const fetchVisitorChart = async () => {
  const [begin, end] = visitorDateRange.value
  const res = await getVisitorStatistics({ begin, end })
  const vo = res.data ?? {}
  visitorChart.value?.setOption(
    makeLineOption('访客数', splitStr(vo.dateList), splitNum(vo.newVisitorCountList), '#909399')
  )
}

const fetchBarChart = async () => {
  const res = await getArticleViewTop10()
  const vo = res.data ?? {}
  const titles = (vo.titleList ?? []).slice(0, 10).reverse()
  const counts = (vo.viewCountList ?? []).slice(0, 10).reverse()
  barChart.value?.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: '#ffffff',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133', fontSize: 13 },
      extraCssText: 'box-shadow: 0 4px 16px rgba(0,0,0,.06); border-radius: 8px;'
    },
    grid: { left: 130, right: 24, top: 16, bottom: 24 },
    xAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: '#f2f6fc', type: 'dashed' } },
      axisLabel: { color: '#909399', fontSize: 11 }
    },
    yAxis: {
      type: 'category',
      data: titles.map((t) => (t && t.length > 14 ? t.slice(0, 14) + '…' : t)),
      axisLabel: { color: '#606266', fontSize: 12 },
      axisLine: { lineStyle: { color: '#ebeef5' } },
      axisTick: { show: false }
    },
    series: [
      {
        name: '阅读量',
        type: 'bar',
        data: counts,
        barMaxWidth: 20,
        itemStyle: {
          color: '#303133',
          borderRadius: [0, 4, 4, 0]
        }
      }
    ]
  })
}

const fetchPieChart = async () => {
  const res = await getProvinceDistribution()
  const vo = res.data ?? {}
  const provinces = splitStr(vo.provinceList)
  const counts = splitNum(vo.countList)
  const pieData = provinces.map((name, i) => ({
    name: name || '未知',
    value: counts[i] ?? 0
  }))
  pieChart.value?.setOption({
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)',
      backgroundColor: '#ffffff',
      borderColor: '#e4e7ed',
      borderWidth: 1,
      textStyle: { color: '#303133', fontSize: 13 },
      extraCssText: 'box-shadow: 0 4px 16px rgba(0,0,0,.06); border-radius: 8px;'
    },
    color: [
      '#303133', '#606266', '#909399', '#c0c4cc',
      '#67c23a', '#e6a23c', '#f56c6c', '#409eff',
      '#b3b3b3', '#d4d7de'
    ],
    legend: {
      orient: 'vertical',
      right: 10,
      top: 'center',
      textStyle: { color: '#606266', fontSize: 12 },
      icon: 'circle',
      itemWidth: 8,
      itemHeight: 8
    },
    series: [
      {
        name: '访客省份',
        type: 'pie',
        radius: ['42%', '72%'],
        center: ['38%', '50%'],
        avoidLabelOverlap: true,
        label: { show: false },
        labelLine: { show: false },
        data: pieData,
        itemStyle: { borderColor: '#fff', borderWidth: 3 },
        emphasis: {
          itemStyle: {
            shadowBlur: 12,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.1)'
          },
          scaleSize: 5
        }
      }
    ]
  })
}

/* ---- Resize ---- */
let resizeTimer = null
const handleResize = () => {
  clearTimeout(resizeTimer)
  resizeTimer = setTimeout(() => {
    viewChart.value?.resize()
    visitorChart.value?.resize()
    barChart.value?.resize()
    pieChart.value?.resize()
  }, 200)
}

const initCharts = () => {
  viewChart.value = echarts.init(viewChartEl.value)
  visitorChart.value = echarts.init(visitorChartEl.value)
  barChart.value = echarts.init(barChartEl.value)
  pieChart.value = echarts.init(pieChartEl.value)
  fetchViewChart()
  fetchVisitorChart()
  fetchBarChart()
  fetchPieChart()
}

onMounted(() => {
  fetchOverview()
  fetchRunDays()
  initCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  viewChart.value?.dispose()
  visitorChart.value?.dispose()
  barChart.value?.dispose()
  pieChart.value?.dispose()
})
</script>

<template>
  <div class="dashboard">
    <!-- Stat cards -->
    <div v-loading="loadingOverview" class="stat-grid">
      <div v-for="card in statCards" :key="card.key" class="stat-card">
        <div class="stat-icon-wrap" :style="{ background: card.bg }">
          <span :class="['iconfont', card.icon]" :style="{ color: card.color }" />
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ overview[card.key] ?? '-' }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
      </div>
    </div>

    <!-- Run days banner -->
    <div v-if="runDays !== null" class="run-banner">
      <div class="run-banner-inner">
        <span class="run-icon iconfont icon-time" />
        <span class="run-text">本站已稳定运行</span>
        <span class="run-days">{{ runDays }}</span>
        <span class="run-unit">天</span>
      </div>
    </div>

    <!-- Line charts -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">浏览量趋势</span>
          <el-date-picker
            v-model="viewDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="-"
            start-placeholder="开始"
            end-placeholder="结束"
            :shortcuts="makeShortcuts()"
            size="small"
            @change="fetchViewChart"
          />
        </div>
        <div ref="viewChartEl" class="chart-body" />
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">访客数趋势</span>
          <el-date-picker
            v-model="visitorDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="-"
            start-placeholder="开始"
            end-placeholder="结束"
            :shortcuts="makeShortcuts()"
            size="small"
            @change="fetchVisitorChart"
          />
        </div>
        <div ref="visitorChartEl" class="chart-body" />
      </div>
    </div>

    <!-- Bar + Pie -->
    <div class="chart-row">
      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">阅读量 TOP 10</span>
        </div>
        <div ref="barChartEl" class="chart-body" />
      </div>

      <div class="chart-card">
        <div class="chart-header">
          <span class="chart-title">访客省份分布</span>
        </div>
        <div ref="pieChartEl" class="chart-body" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ---- Stat cards ---- */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
}

.stat-card {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 18px 16px;
  display: flex;
  align-items: center;
  gap: 14px;
  transition: box-shadow 0.2s ease;
  cursor: default;
}

.stat-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.stat-icon-wrap {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-icon-wrap .iconfont {
  font-size: 18px;
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
  font-weight: 500;
}

/* ---- Run banner ---- */
.run-banner {
  background: #303133;
  border-radius: 8px;
  padding: 16px 24px;
}

.run-banner-inner {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.run-icon {
  font-size: 20px;
  color: rgba(255, 255, 255, 0.7);
}

.run-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

.run-days {
  font-size: 24px;
  font-weight: 800;
  color: #ffffff;
  line-height: 1;
  margin: 0 2px;
}

.run-unit {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
  font-weight: 500;
}

/* ---- Chart cards ---- */
.chart-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

@media (max-width: 900px) {
  .chart-row {
    grid-template-columns: 1fr;
  }
}

.chart-card {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  padding: 20px;
  transition: box-shadow 0.2s ease;
}

.chart-card:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.03);
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
  flex-wrap: wrap;
  gap: 8px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.chart-body {
  height: 280px;
}

@media (max-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }
  .stat-card {
    padding: 12px 10px;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .stat-icon-wrap {
    width: 36px;
    height: 36px;
  }
  .stat-icon-wrap .iconfont {
    font-size: 15px;
  }
  .stat-value {
    font-size: 20px;
  }
  .stat-label {
    font-size: 11px;
  }
  .chart-body {
    height: 200px;
  }
}

@media (max-width: 400px) {
  .stat-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 6px;
  }
  .stat-card {
    padding: 10px 8px;
  }
  .stat-value {
    font-size: 18px;
  }
}
</style>
