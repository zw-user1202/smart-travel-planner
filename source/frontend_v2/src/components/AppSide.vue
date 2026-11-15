<template>
  <aside class="side">
    <div class="side-brand">
      <span class="mark">✈</span>
      <span>旅行工作台</span>
    </div>
    <nav class="side-nav">
      <a
        v-for="item in navs"
        :key="item.path"
        :href="item.path"
        class="nav-item"
        :class="{ on: isActive(item.path) }"
      >{{ item.label }}</a>
      <a v-if="isAdmin" href="/admin" class="nav-item" :class="{ on: isActive('/admin') }">后台管理</a>
    </nav>

    <div class="side-scroll">
      <section class="mod">
        <h4 class="mod-t">下一段旅程</h4>
        <div
          v-if="nextTrip"
          class="next-trip"
          @click="go('/trips/' + nextTrip.id)"
        >
          <div class="nt-dest">{{ nextTrip.destination }}</div>
          <div class="nt-meta">{{ nextTrip.startDate }} 出发</div>
          <div class="nt-count">{{ daysLeft(nextTrip) }}</div>
        </div>
        <p v-else class="empty-line">暂无进行中的行程</p>
      </section>

      <section class="mod">
        <h4 class="mod-t">预算与额度</h4>
        <div class="bar-block">
          <div class="bar-label">
            <span>预算 / 估算</span>
            <span>¥{{ stats.totalEstimated ?? 0 }} / ¥{{ stats.totalBudget ?? 0 }}</span>
          </div>
          <div class="bar"><i :style="{ width: budgetPct + '%' }"></i></div>
        </div>
        <div class="bar-block">
          <div class="bar-label">
            <span>今日 AI 额度</span>
            <span>{{ stats.todayUsedQuota ?? 0 }} / {{ stats.dailyQuota ?? 5 }}</span>
          </div>
          <div class="bar"><i :style="{ width: quotaPct + '%' }"></i></div>
        </div>
      </section>

      <section class="mod">
        <h4 class="mod-t">快捷目的地</h4>
        <div class="tag-cloud">
          <button
            v-for="t in quickDests"
            :key="t"
            type="button"
            class="tag"
            @click="go('/trips/new?dest=' + encodeURIComponent(t))"
          >{{ t }}</button>
        </div>
      </section>

      <section class="mod">
        <h4 class="mod-t">推荐线路</h4>
        <button
          v-for="r in topRoutes"
          :key="r.title"
          type="button"
          class="route-card"
          @click="go('/routes')"
        >
          <span class="rc-title">{{ r.title }}</span>
          <span class="rc-meta">{{ r.days }}天 · {{ r.prefs.split(',')[0] }}</span>
        </button>
        <button type="button" class="tag" style="width:100%;margin-top:4px" @click="go('/routes')">
          查看全部线路 →
        </button>
      </section>

      <section class="mod">
        <h4 class="mod-t">出行清单</h4>
        <div class="bar-block">
          <div class="bar-label">
            <span>完成进度</span>
            <span>{{ checklistDone }} / {{ checklistTotal }}</span>
          </div>
          <div class="bar"><i :style="{ width: checklistPct + '%' }"></i></div>
        </div>
        <button type="button" class="tag" style="width:100%" @click="go('/checklist')">
          去勾选清单 →
        </button>
      </section>

      <section class="mod">
        <h4 class="mod-t">我的足迹</h4>
        <div class="footprint">
          <div class="fp-item"><b>{{ footprint.cities }}</b><span>座城市</span></div>
          <div class="fp-item"><b>{{ footprint.days }}</b><span>天行程</span></div>
          <div class="fp-item"><b>{{ footprint.trips }}</b><span>次出行</span></div>
        </div>
        <button type="button" class="tag" style="width:100%;margin-top:8px" @click="go('/footprint')">
          查看足迹 →
        </button>
      </section>

      <section class="mod quote-mod">
        <p class="quote">把散落的灵感，收成一张可执行的日程表。</p>
      </section>

      <slot name="extra" />
    </div>

    <div class="side-user">
      <div class="avatar">{{ initial }}</div>
      <div class="meta">
        <div class="name">{{ displayName }}</div>
        <button type="button" class="logout" @click="logout">退出</button>
      </div>
    </div>
  </aside>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import api from '@/api'
import { loadChecklist, RECOMMENDED_ROUTES } from '@/api/domain'

const route = useRoute()
const user = useUserStore()

const stats = ref({})
const trips = ref([])
const checklist = ref(loadChecklist())

const navs = [
  { path: '/', label: '总览' },
  { path: '/trips/new', label: '新建行程' },
  { path: '/routes', label: '推荐线路' },
  { path: '/costs', label: '花费构成' },
  { path: '/footprint', label: '我的足迹' },
  { path: '/checklist', label: '出行清单' }
]

const quickDests = ['长沙', '成都', '杭州', '厦门', '西安', '大理', '重庆', '青岛']
const topRoutes = RECOMMENDED_ROUTES.slice(0, 3)

const displayName = computed(() => user.nickname || user.username || '旅行者')
const initial = computed(() => (displayName.value || 'U').slice(0, 1))
const isAdmin = computed(() => Number(user.role) === 1)

const budgetPct = computed(() => {
  const b = Number(stats.value.totalBudget || 0)
  const e = Number(stats.value.totalEstimated || 0)
  if (!b) return e ? 100 : 0
  return Math.min(100, Math.round((e / b) * 100))
})

const quotaPct = computed(() => {
  const used = Number(stats.value.todayUsedQuota || 0)
  const total = Number(stats.value.dailyQuota || 5)
  if (!total) return 0
  return Math.min(100, Math.round((used / total) * 100))
})

const checklistDone = computed(() => checklist.value.filter(x => x.done).length)
const checklistTotal = computed(() => checklist.value.length)
const checklistPct = computed(() =>
  checklistTotal.value ? Math.round((checklistDone.value / checklistTotal.value) * 100) : 0
)

const nextTrip = computed(() => {
  const list = trips.value || []
  return (
    list.find(t => t.status === 'ongoing') ||
    list.find(t => t.status === 'generated') ||
    list[0] ||
    null
  )
})

const footprint = computed(() => {
  const cities = new Set()
  let days = 0
  for (const t of trips.value || []) {
    if (t.destination) cities.add(t.destination)
    if (t.startDate && t.endDate) {
      const d = (new Date(t.endDate) - new Date(t.startDate)) / 86400000 + 1
      if (d > 0 && d < 400) days += d
    }
  }
  return { cities: cities.size, days, trips: (trips.value || []).length }
})

function daysLeft(t) {
  if (!t?.startDate) return ''
  const diff = Math.ceil((new Date(t.startDate) - new Date()) / 86400000)
  if (diff > 0) return `${diff} 天后出发`
  if (diff === 0) return '今天出发'
  return '进行中 / 已出发'
}

function isActive(path) {
  if (path === '/') return route.path === '/'
  return route.path === path || route.path.startsWith(path + '/')
}

function go(path) {
  window.location.href = path
}

function logout() {
  try { user.logout() } catch {}
  window.location.replace('/login')
}

onMounted(async () => {
  try {
    const [st, list] = await Promise.all([
      api.get('/workbench/stats'),
      api.get('/trips')
    ])
    stats.value = st || {}
    trips.value = list || []
  } catch {
    /* sidebar is best-effort */
  }
  try {
    checklist.value = loadChecklist()
  } catch {}
})
</script>
