<template>
  <div class="layout">
    <aside class="side">
      <div class="side-brand">✈ 旅行工作台</div>
      <el-menu :default-active="'/'" router background-color="#0f172a" text-color="#cbd5e1" active-text-color="#fff">
        <el-menu-item index="/">工作台总览</el-menu-item>
        <el-menu-item index="/trips/new">新建行程</el-menu-item>
      </el-menu>
    </aside>

    <main class="main">
      <div class="page" v-if="trip">
        <div class="head">
          <div>
            <el-button text @click="$router.push('/')">← 返回工作台</el-button>
            <h2 style="margin-top:4px">
              <template v-if="!editing">{{ trip.title }}</template>
              <el-input v-else v-model="trip.title" style="width:320px" @keyup.enter="saveTitle" />
            </h2>
            <p class="muted">
              {{ trip.destination }} · {{ trip.startDate }} ~ {{ trip.endDate }} · {{ trip.peopleCount }}人
              <el-tag size="small" style="margin-left:8px" :type="statusType(trip.status)">{{ statusText(trip.status) }}</el-tag>
            </p>
          </div>
          <div class="actions">
            <el-button v-if="!editing" @click="editing = true">重命名</el-button>
            <el-button v-else type="primary" @click="saveTitle">保存标题</el-button>
            <el-button type="primary" :loading="generating" @click="generate">
              {{ plan ? '重新规划' : '智能规划' }}
            </el-button>
            <el-dropdown @command="setStatus" style="margin-left:8px">
              <el-button>状态<el-icon><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="generated">标记已规划</el-dropdown-item>
                  <el-dropdown-item command="ongoing">标记进行中</el-dropdown-item>
                  <el-dropdown-item command="done">标记已完成</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button type="danger" @click="remove">删除</el-button>
          </div>
        </div>

        <el-row :gutter="16">
          <el-col :span="15">
            <div class="card" style="margin-bottom:16px">
              <div class="card-title">行程总览</div>
              <el-alert v-if="plan" :title="plan.summary || '已生成行程方案'" type="success" :closable="false" show-icon />
              <el-alert v-else title="尚未生成规划，点击右上角「智能规划」开始" type="info" :closable="false" show-icon />
              <div v-if="plan" style="margin-top:12px">
                <div class="kv"><span>预算估算</span><b>¥{{ plan.estimatedCost ?? '—' }}</b></div>
                <div class="kv"><span>规划来源</span><b>{{ plan.provider }} / {{ plan.model }}</b></div>
                <div class="kv" style="border:none"><span>总览建议</span><b style="max-width:70%;text-align:right;font-weight:500">{{ plan.advice || '—' }}</b></div>
              </div>
            </div>

            <div class="card" v-if="plan && plan.days?.length">
              <div class="card-title">每日安排</div>
              <div v-for="day in plan.days" :key="day.day" class="day-block">
                <h3>第 {{ day.day }} 天 · {{ day.date }} · {{ day.theme }}</h3>
                <div v-for="item in day.items" :key="item.slot + item.name" class="plan-item">
                  <el-tag class="slot-tag" size="small" :type="slotType(item.slot)">{{ slotText(item.slot) }}</el-tag>
                  <div>
                    <div style="font-weight:600">
                      <el-tag size="small" style="margin-right:6px">{{ item.type }}</el-tag>
                      {{ item.name }}
                    </div>
                    <div class="muted" style="margin-top:4px">{{ item.description }}</div>
                  </div>
                  <div class="cost" v-if="item.cost != null">¥{{ item.cost }}</div>
                </div>
              </div>
            </div>

            <div class="card" style="margin-top:16px" v-if="plan">
              <div class="card-title">景点图片参考</div>
              <el-empty v-if="!images.length" description="配置高德 Key 后可自动匹配景点图片；当前展示目的地占位图" :image-size="60" />
              <div class="img-grid" v-else>
                <div v-for="(img,i) in images" :key="i" class="img-item">
                  <img :src="img.url" :alt="img.name" />
                  <div class="muted" style="padding:6px 4px;text-align:center">{{ img.name }}</div>
                </div>
              </div>
            </div>
          </el-col>

          <el-col :span="9">
            <div class="card" style="margin-bottom:16px">
              <div class="card-title">
                <span>地图展示</span>
                <span class="muted" style="font-weight:400">高德地图</span>
              </div>
              <div id="amap" class="map-box">
                <div v-if="!amapReady" class="map-fallback">
                  <p>{{ mapHint }}</p>
                  <p class="muted" style="margin-top:8px">在 application.yml 配置 app.amap.key 后自动加载真实地图</p>
                </div>
              </div>
            </div>

            <div class="card" style="margin-bottom:16px">
              <div class="card-title">天气信息</div>
              <div v-if="weather.live" class="weather">
                <div class="w-main">
                  <b>{{ weather.live.weather }}</b>
                  <span style="font-size:28px;margin-left:10px">{{ weather.live.temperature }}°C</span>
                </div>
                <div class="muted">{{ trip.destination }} · {{ weather.live.winddirection }}风 {{ weather.live.windpower }} 级 · 湿度 {{ weather.live.humidity }}%</div>
              </div>
              <el-empty v-else description="配置高德 Key 后可显示实时天气" :image-size="50" />
              <div v-if="weather.forecast?.casts?.length" class="forecast">
                <div v-for="c in weather.forecast.casts" :key="c.date" class="f-item">
                  <div class="muted">{{ c.date.slice(5) }}</div>
                  <div>{{ c.dayweather }}</div>
                  <div class="muted">{{ c.nighttemp }}~{{ c.daytemp }}°C</div>
                </div>
              </div>
            </div>

            <div class="card">
              <div class="card-title">路线示意</div>
              <div class="route" v-if="plan">
                <div v-for="(day, idx) in plan.days" :key="day.day" class="route-node">
                  <div class="dot"></div>
                  <div>
                    <b>D{{ day.day }}</b>
                    <span class="muted"> · {{ (day.items||[]).filter(i=>i.type==='景点').map(i=>i.name).join(' → ') || day.theme }}</span>
                  </div>
                </div>
              </div>
              <el-empty v-else description="生成规划后展示每日路线" :image-size="50" />
            </div>
          </el-col>
        </el-row>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const trip = ref(null)
const plan = ref(null)
const editing = ref(false)
const generating = ref(false)
const weather = ref({})
const amapReady = ref(false)
const mapHint = ref('地图加载中…')
let map = null
let markers = []

const tripId = () => route.params.id

const images = computed(() => {
  if (!plan.value?.days) return []
  const list = []
  for (const day of plan.value.days) {
    for (const item of day.items || []) {
      if (item.type === '景点' && item.image) {
        list.push({ name: item.name, url: item.image })
      }
    }
  }
  if (list.length) return list.slice(0, 8)
  // 占位：用高德静态图（无需 JS key 时可能失败，失败则空）
  return (plan.value.days[0]?.items || [])
    .filter(i => i.type === '景点')
    .slice(0, 4)
    .map(i => ({
      name: i.name,
      url: i.lng && i.lat
        ? `https://restapi.amap.com/v3/staticmap?location=${i.lng},${i.lat}&zoom=13&size=400*240&scale=2`
        : ''
    }))
    .filter(i => i.url)
})

function statusText(s) {
  return { draft: '待规划', generated: '已规划', ongoing: '进行中', done: '已完成' }[s] || s
}
function statusType(s) {
  return { draft: 'info', generated: 'primary', ongoing: 'warning', done: 'success' }[s] || 'info'
}
function slotText(s) {
  return { morning: '上午', noon: '中午', afternoon: '下午', evening: '晚上', night: '住宿' }[s] || s
}
function slotType(s) {
  return { morning: 'primary', noon: 'warning', afternoon: 'success', evening: 'danger', night: 'info' }[s] || 'info'
}

async function load() {
  trip.value = await api.get('/trips/' + tripId())
  plan.value = await api.get(`/trips/${tripId()}/plan`).catch(() => null)
  loadMap()
  loadWeather()
}

async function generate() {
  generating.value = true
  try {
    plan.value = await api.post(`/trips/${tripId()}/generate`)
    trip.value.status = 'generated'
    ElMessage.success(`规划完成（${plan.value.provider}）`)
    loadMap()
  } finally {
    generating.value = false
  }
}

async function saveTitle() {
  trip.value = await api.put(`/trips/${tripId()}/title`, { title: trip.value.title })
  editing.value = false
  ElMessage.success('已保存')
}

async function setStatus(status) {
  trip.value = await api.put(`/trips/${tripId()}/status`, { status })
  ElMessage.success('状态已更新')
}

async function remove() {
  await ElMessageBox.confirm('确定删除该行程吗？', '提示', { type: 'warning' })
  await api.delete('/trips/' + tripId())
  ElMessage.success('已删除')
  router.push('/')
}

async function loadWeather() {
  if (!trip.value) return
  try {
    weather.value = await api.get('/map/weather', { params: { city: trip.value.destination } })
  } catch {
    weather.value = {}
  }
}

function loadMap() {
  // 若页面已有高德 JS API（配置了 amap key 在 index 注入），则用之；否则显示占位
  const el = document.getElementById('amap')
  if (!el) return
  const points = []
  for (const day of plan.value?.days || []) {
    for (const item of day.items || []) {
      if (item.lng && item.lat) {
        points.push({ name: item.name, lng: item.lng, lat: item.lat, day: day.day })
      }
    }
  }
  if (!points.length) {
    mapHint.value = plan.value ? '当前规划点位未包含坐标，生成时可配置 DeepSeek 获取真实经纬度' : '生成规划后展示地图点位'
    return
  }
  // 尝试加载高德 JS API（需要在 public/index 或环境变量中提供 key）
  const amapKey = window.AMAP_KEY || ''
  if (!amapKey) {
    mapHint.value = '检测到 ' + points.length + ' 个点位。配置高德 Web 端 JS Key 后可交互浏览地图'
    renderSimpleMap(el, points)
    return
  }
  ensureAmapScript(amapKey).then(() => {
    map = new window.AMap.Map('amap', {
      zoom: 11,
      center: [points[0].lng, points[0].lat]
    })
    markers = points.map(p => {
      const m = new window.AMap.Marker({
        position: [p.lng, p.lat],
        title: `D${p.day} ${p.name}`,
        label: { content: `D${p.day} ${p.name}`, direction: 'top' }
      })
      m.setMap(map)
      return m
    })
    map.add(markers)
    amapReady.value = true
    mapHint.value = ''
    // 路线
    if (points.length > 1) {
      const line = new window.AMap.Polyline({
        path: points.map(p => [p.lng, p.lat]),
        strokeColor: '#3b82f6',
        strokeWeight: 4,
        strokeOpacity: 0.8
      })
      map.add(line)
      map.setFitView()
    }
  }).catch(() => {
    mapHint.value = '高德地图 JS 加载失败，请检查网络或 Key'
    renderSimpleMap(el, points)
  })
}

function ensureAmapScript(key) {
  if (window.AMap) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const s = document.createElement('script')
    s.src = `https://webapi.amap.com/maps?v=2.0&key=${key}`
    s.onload = resolve
    s.onerror = reject
    document.head.appendChild(s)
  })
}

function renderSimpleMap(el, points) {
  // 无 Key 时用简单 SVG 路线示意，保证答辩可展示
  const lats = points.map(p => p.lat)
  const lngs = points.map(p => p.lng)
  const minLat = Math.min(...lats), maxLat = Math.max(...lats)
  const minLng = Math.min(...lngs), maxLng = Math.max(...lngs)
  const pad = 30
  const w = el.clientWidth || 360
  const h = 280
  const x = lng => pad + ((lng - minLng) / ((maxLng - minLng) || 1)) * (w - pad * 2)
  const y = lat => h - pad - ((lat - minLat) / ((maxLat - minLat) || 1)) * (h - pad * 2)
  const path = points.map(p => `${x(p.lng)},${y(p.lat)}`).join(' ')
  const dots = points.map(p =>
    `<circle cx="${x(p.lng)}" cy="${y(p.lat)}" r="6" fill="#2563eb"/>
     <text x="${x(p.lng)}" y="${y(p.lat) - 12}" text-anchor="middle" font-size="11" fill="#0f172a">D${p.day}</text>`
  ).join('')
  el.innerHTML = `
    <svg width="100%" height="${h}" viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg">
      <rect width="100%" height="100%" fill="#f0f7ff" rx="8"/>
      <polyline points="${path}" fill="none" stroke="#2563eb" stroke-width="3" stroke-dasharray="6 4"/>
      ${dots}
    </svg>`
}

onMounted(load)
onUnmounted(() => {
  if (map) map.destroy()
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.side { width: 220px; background: #0f172a; color: #cbd5e1; position: sticky; top: 0; height: 100vh; }
.side-brand { padding: 22px 20px; font-weight: 700; color: #fff; font-size: 16px; border-bottom: 1px solid rgba(255,255,255,.06); }
.side :deep(.el-menu) { border-right: none; }
.main { flex: 1; }
.head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; gap: 12px; flex-wrap: wrap; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.card-title { display: flex; justify-content: space-between; font-weight: 700; margin-bottom: 12px; }
.kv { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #eef2f7; font-size: 14px; gap: 16px; }
.map-box { height: 280px; border-radius: 8px; overflow: hidden; background: #f0f7ff; position: relative; }
.map-fallback { padding: 20px; text-align: center; color: #334155; height: 100%; display:flex; flex-direction:column; align-items:center; justify-content:center; }
.weather .w-main { display: flex; align-items: baseline; margin-bottom: 6px; }
.forecast { display: flex; gap: 8px; margin-top: 12px; }
.f-item { flex: 1; text-align: center; background: #f8fafc; border-radius: 8px; padding: 8px 4px; font-size: 12px; }
.route-node { display: flex; gap: 10px; align-items: flex-start; padding: 8px 0; }
.route-node .dot { width: 10px; height: 10px; border-radius: 50%; background: #2563eb; margin-top: 5px; flex-shrink: 0; }
.img-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.img-item img { width: 100%; height: 110px; object-fit: cover; border-radius: 8px; background: #e2e8f0; }
</style>
