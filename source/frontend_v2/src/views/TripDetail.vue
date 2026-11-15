<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page" v-if="trip">
        <div class="head">
          <div>
            <p class="eyebrow">TRIP DETAIL</p>
            <h1 class="title">{{ trip.title }}</h1>
            <p class="sub">
              {{ trip.destination }} · {{ trip.startDate }} ~ {{ trip.endDate }} · {{ trip.peopleCount }}人
              <span class="chip" style="margin-left:8px;cursor:default">{{ statusText(trip.status) }}</span>
            </p>
          </div>
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <button type="button" class="btn-ghost" @click="$router.push('/')">← 返回</button>
            <button v-if="!editing" type="button" class="btn-ghost" @click="editing = true">重命名</button>
            <button v-else type="button" class="btn-primary" style="height:36px" @click="saveTitle">保存标题</button>
            <input
              v-if="editing"
              v-model="trip.title"
              style="height:36px;border:1px solid #d6cfc3;border-radius:8px;padding:0 10px"
              @keyup.enter="saveTitle"
            />
            <button type="button" class="btn-primary" style="height:36px" :disabled="generating" @click="generate">
              {{ generating ? '规划中…' : (plan ? '重新规划' : '智能规划') }}
            </button>
            <el-dropdown @command="setStatus">
              <button type="button" class="btn-ghost">状态 ▾</button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="generated">标记已规划</el-dropdown-item>
                  <el-dropdown-item command="ongoing">标记进行中</el-dropdown-item>
                  <el-dropdown-item command="done">标记已完成</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <button type="button" class="btn-ghost" style="color:#9f1239;border-color:#e7c4ce" @click="remove">删除</button>
          </div>
        </div>

        <div class="grid-2" style="align-items:start">
          <div style="display:flex;flex-direction:column;gap:16px">
            <div class="card">
              <div class="card-title">行程总览</div>
              <el-alert
                v-if="plan"
                :title="plan.summary || '已生成行程方案'"
                type="success"
                :closable="false"
                show-icon
              />
              <el-alert
                v-else
                title="尚未生成规划，点击右上角「智能规划」开始"
                type="info"
                :closable="false"
                show-icon
              />
              <div v-if="plan" style="margin-top:12px">
                <div class="kv"><span>预算估算</span><b>¥{{ plan.estimatedCost ?? '—' }}</b></div>
                <div class="kv"><span>规划来源</span><b>{{ plan.provider }} / {{ plan.model }}</b></div>
                <div class="kv" style="border:none"><span>总览建议</span><b style="max-width:70%;text-align:right;font-weight:500">{{ plan.advice || '—' }}</b></div>
              </div>
            </div>

            <div class="card" v-if="plan && plan.days?.length">
              <div class="card-title">每日安排</div>
              <div v-for="day in plan.days" :key="day.day" style="margin-bottom:16px">
                <h3 style="font-size:14px;margin-bottom:8px">第 {{ day.day }} 天 · {{ day.date }} · {{ day.theme }}</h3>
                <div v-for="item in day.items" :key="item.slot + item.name" class="plan-item">
                  <span class="chip" style="cursor:default;min-width:56px;justify-content:center">{{ slotText(item.slot) }}</span>
                  <div style="flex:1">
                    <div style="font-weight:600">
                      <span class="chip" style="cursor:default;margin-right:6px">{{ item.type }}</span>
                      {{ item.name }}
                    </div>
                    <div class="muted" style="margin-top:4px">{{ item.description }}</div>
                  </div>
                  <div v-if="item.cost != null" style="font-weight:600;color:#c2410c;white-space:nowrap">¥{{ item.cost }}</div>
                </div>
              </div>
            </div>
          </div>

          <div style="display:flex;flex-direction:column;gap:16px">
            <div class="card">
              <div class="card-title">天气信息</div>
              <div v-if="weather.live">
                <div style="display:flex;align-items:baseline;gap:10px;margin-bottom:6px">
                  <b>{{ weather.live.weather }}</b>
                  <span style="font-size:28px;font-weight:700">{{ weather.live.temperature }}°C</span>
                </div>
                <div class="muted">{{ trip.destination }} · {{ weather.live.winddirection }}风 {{ weather.live.windpower }} 级 · 湿度 {{ weather.live.humidity }}%</div>
              </div>
              <p v-else class="muted">暂无天气数据</p>
              <div v-if="weather.forecast?.casts?.length" style="display:flex;gap:8px;margin-top:12px">
                <div v-for="c in weather.forecast.casts" :key="c.date" class="f-item" style="flex:1;text-align:center;background:#f7f2ea;border-radius:10px;padding:8px 4px;font-size:12px">
                  <div class="muted">{{ c.date.slice(5) }}</div>
                  <div>{{ c.dayweather }}</div>
                  <div class="muted">{{ c.nighttemp }}~{{ c.daytemp }}°C</div>
                </div>
              </div>
            </div>

            <div class="card">
              <div class="card-title">路线示意</div>
              <div v-if="plan">
                <div v-for="day in plan.days" :key="day.day" style="display:flex;gap:10px;align-items:flex-start;padding:8px 0">
                  <div style="width:10px;height:10px;border-radius:50%;background:#c2410c;margin-top:5px;flex-shrink:0"></div>
                  <div>
                    <b>D{{ day.day }}</b>
                    <span class="muted"> · {{ (day.items||[]).filter(i=>i.type==='景点').map(i=>i.name).join(' → ') || day.theme }}</span>
                  </div>
                </div>
              </div>
              <p v-else class="muted">生成规划后展示每日路线</p>
            </div>

            <div class="card" v-if="plan">
              <div class="card-title">地图点位</div>
              <div class="map-box">
                <div id="amap" class="map-canvas"></div>
                <div v-if="mapHint" class="map-fallback">
                  <p>{{ mapHint }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-else class="page"><p class="muted">加载中…</p></div>
    </main>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSide from '@/components/AppSide.vue'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const trip = ref(null)
const plan = ref(null)
const editing = ref(false)
const generating = ref(false)
const weather = ref({})
const mapReady = ref(false)
const mapHint = ref('地图加载中…')
let map = null
let markers = []

const tripId = () => route.params.id

function statusText(s) {
  return { draft: '待规划', generated: '已规划', ongoing: '进行中', done: '已完成' }[s] || s
}
function slotText(s) {
  return { morning: '上午', noon: '中午', afternoon: '下午', evening: '晚上', night: '住宿' }[s] || s
}

async function load() {
  trip.value = await api.get('/trips/' + tripId())
  plan.value = await api.get(`/trips/${tripId()}/plan`).catch(() => null)
  await nextTick()
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
  const el = document.getElementById('amap')
  if (!el) return
  const points = []
  for (const day of plan.value?.days || []) {
    for (const item of day.items || []) {
      if (item.lng && item.lat) points.push({ name: item.name, lng: item.lng, lat: item.lat, day: day.day })
    }
  }

  // Always paint something immediately — never leave "地图加载中…" hanging.
  if (!points.length) {
    mapReady.value = false
    mapHint.value = plan.value
      ? '本方案未包含经纬度，下方为行程示意'
      : '生成规划后展示地图点位'
    renderPlaceholderMap(el)
    return
  }

  renderSimpleMap(el, points)

  const amapKey = window.AMAP_KEY || ''
  if (!amapKey) {
    mapHint.value = `已展示 ${points.length} 个点位示意（未配置高德 Key）`
    return
  }

  mapHint.value = '正在加载高德地图…'
  ensureAmapScript(amapKey, 6000)
    .then(() => {
      if (!window.AMap) throw new Error('AMap unavailable')
      // Clear SVG placeholder before mounting real map
      el.innerHTML = ''
      map = new window.AMap.Map('amap', {
        zoom: 11,
        center: [points[0].lng, points[0].lat],
        viewMode: '2D'
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
      if (points.length > 1) {
        map.add(new window.AMap.Polyline({
          path: points.map(p => [p.lng, p.lat]),
          strokeColor: '#c2410c',
          strokeWeight: 4,
          strokeOpacity: 0.85
        }))
        map.setFitView()
      }
      mapReady.value = true
      mapHint.value = ''
    })
    .catch(() => {
      // Keep the SVG already rendered; just explain.
      mapReady.value = false
      mapHint.value = `已展示 ${points.length} 个点位示意（高德地图未能加载）`
      if (el && !el.querySelector('svg')) renderSimpleMap(el, points)
    })
}

function ensureAmapScript(key, timeoutMs = 6000) {
  if (window.AMap) return Promise.resolve()
  return new Promise((resolve, reject) => {
    const existing = document.querySelector('script[data-amap="1"]')
    if (existing) {
      const t = setTimeout(() => reject(new Error('timeout')), timeoutMs)
      existing.addEventListener('load', () => { clearTimeout(t); resolve() })
      existing.addEventListener('error', () => { clearTimeout(t); reject(new Error('error')) })
      return
    }
    const s = document.createElement('script')
    s.src = `https://webapi.amap.com/maps?v=2.0&key=${key}`
    s.async = true
    s.dataset.amap = '1'
    const timer = setTimeout(() => reject(new Error('timeout')), timeoutMs)
    s.onload = () => { clearTimeout(timer); resolve() }
    s.onerror = () => { clearTimeout(timer); reject(new Error('load error')) }
    document.head.appendChild(s)
  })
}

function renderPlaceholderMap(el) {
  const dest = trip.value?.destination || '目的地'
  el.innerHTML = `
    <div style="height:260px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:8px;background:#f0ebe2;color:#5c564c">
      <div style="width:48px;height:48px;border-radius:50%;background:#c2410c;color:#fff;display:grid;place-items:center;font-size:20px">✈</div>
      <div style="font-weight:700;color:#0c1222">${dest}</div>
      <div style="font-size:12px">暂无地图点位</div>
    </div>`
}

function renderSimpleMap(el, points) {
  const lats = points.map(p => p.lat)
  const lngs = points.map(p => p.lng)
  const minLat = Math.min(...lats), maxLat = Math.max(...lats)
  const minLng = Math.min(...lngs), maxLng = Math.max(...lngs)
  const pad = 30
  const w = el.clientWidth || 360
  const h = 260
  const x = lng => pad + ((lng - minLng) / ((maxLng - minLng) || 1)) * (w - pad * 2)
  const y = lat => h - pad - ((lat - minLat) / ((maxLat - minLat) || 1)) * (h - pad * 2)
  const path = points.map(p => `${x(p.lng)},${y(p.lat)}`).join(' ')
  const dots = points.map(p =>
    `<circle cx="${x(p.lng)}" cy="${y(p.lat)}" r="6" fill="#c2410c"/>
     <text x="${x(p.lng)}" y="${y(p.lat) - 12}" text-anchor="middle" font-size="11" fill="#0c1222">D${p.day}</text>`
  ).join('')
  el.innerHTML = `
    <svg width="100%" height="${h}" viewBox="0 0 ${w} ${h}" xmlns="http://www.w3.org/2000/svg">
      <rect width="100%" height="100%" fill="#f0ebe2" rx="8"/>
      <polyline points="${path}" fill="none" stroke="#c2410c" stroke-width="3" stroke-dasharray="6 4"/>
      ${dots}
    </svg>`
}

onMounted(load)
onUnmounted(() => { if (map) map.destroy() })
</script>

<style scoped>
.plan-item {
  display: flex; gap: 12px; padding: 12px; border-radius: 10px;
  background: #f7f2ea; margin-bottom: 8px; align-items: flex-start;
}
.map-box {
  height: 260px; border-radius: 12px; overflow: hidden;
  background: #f0ebe2; position: relative;
}
.map-canvas { width: 100%; height: 100%; }
.map-fallback {
  position: absolute; left: 0; right: 0; bottom: 0; padding: 8px 12px;
  text-align: center; color: #5c564c; font-size: 12px;
  background: #f0ebe2e6; pointer-events: none;
}
</style>
