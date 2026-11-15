<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <p class="eyebrow">WORKBENCH</p>
            <h1 class="title">世界那么大，不想去看看吗</h1>
            <p class="sub">你好，{{ nickname }} · 在这里管理全部旅行计划</p>
          </div>
          <button type="button" class="btn-primary" @click="$router.push('/trips/new')">
            <span>+</span> 新建行程
          </button>
        </div>

        <div class="grid-4" style="margin-bottom:20px">
          <div class="stat-tile"><div class="n">{{ stats.total || 0 }}</div><div class="t">全部行程</div></div>
          <div class="stat-tile"><div class="n">{{ (stats.generated||0)+(stats.ongoing||0) }}</div><div class="t">已规划</div></div>
          <div class="stat-tile"><div class="n">{{ stats.planned || 0 }}</div><div class="t">待规划</div></div>
          <div class="stat-tile"><div class="n">¥{{ stats.totalEstimated ?? 0 }}</div><div class="t">估算合计</div></div>
        </div>

        <div class="grid-2" style="align-items:start">
          <div class="card">
            <div class="card-title">
              <span>行程看板</span>
              <div style="display:flex;gap:6px;flex-wrap:wrap">
                <button
                  v-for="f in filters"
                  :key="f.value"
                  type="button"
                  class="chip"
                  :class="{ on: filter === f.value }"
                  @click="filter = f.value; load()"
                >{{ f.label }}</button>
              </div>
            </div>
            <ul v-if="trips.length" style="list-style:none">
              <li
                v-for="t in trips"
                :key="t.id"
                class="trip-row"
                @click="$router.push('/trips/'+t.id)"
              >
                <div>
                  <h3 style="font-size:15px;margin-bottom:4px">{{ t.title }}</h3>
                  <p class="muted">{{ t.destination }} · {{ t.peopleCount }} 人 · {{ t.startDate }} ~ {{ t.endDate }}</p>
                </div>
                <div style="display:flex;gap:12px;align-items:center;flex-shrink:0">
                  <span class="chip" style="cursor:default">{{ statusText(t.status) }}</span>
                  <span style="font-weight:600;min-width:64px;text-align:right">
                    {{ t.estimatedCost ? '¥'+t.estimatedCost : '—' }}
                  </span>
                </div>
              </li>
            </ul>
            <div v-else style="text-align:center;padding:28px 12px;color:#7a7366">
              <p style="margin-bottom:12px">还没有行程</p>
              <button type="button" class="btn-ghost" @click="$router.push('/trips/new')">去新建一个</button>
            </div>
          </div>

          <div style="display:flex;flex-direction:column;gap:16px">
            <div class="card">
              <div class="card-title">快捷入口</div>
              <div style="display:flex;flex-direction:column;gap:8px">
                <button v-for="q in quick" :key="q.path" type="button" class="btn-ghost" style="width:100%;text-align:left" @click="$router.push(q.path)">
                  {{ q.label }} → {{ q.desc }}
                </button>
              </div>
            </div>
            <div class="card">
              <div class="card-title">最近行程</div>
              <div
                v-for="t in stats.recentTrips || []"
                :key="t.id"
                class="kv"
                style="cursor:pointer"
                @click="$router.push('/trips/'+t.id)"
              >
                <span>{{ t.title }}</span>
                <span class="muted">{{ t.startDate }}</span>
              </div>
              <p v-if="!(stats.recentTrips||[]).length" class="muted">暂无数据</p>
            </div>
            <div class="card">
              <div class="card-title">额度与预算</div>
              <div class="kv"><span>今日 AI 额度</span><b>{{ stats.todayUsedQuota ?? 0 }} / {{ stats.dailyQuota ?? 5 }}</b></div>
              <div class="kv"><span>预算合计</span><b>¥{{ stats.totalBudget ?? 0 }}</b></div>
              <div class="kv" style="border:none"><span>估算合计</span><b>¥{{ stats.totalEstimated ?? 0 }}</b></div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppSide from '@/components/AppSide.vue'
import { useUserStore } from '@/stores/user'
import api from '@/api'

const user = useUserStore()
const stats = ref({})
const trips = ref([])
const filter = ref('')
const nickname = computed(() => user.nickname || user.username || '旅行者')

const filters = [
  { label: '全部', value: '' },
  { label: '待规划', value: 'draft' },
  { label: '已规划', value: 'generated' },
  { label: '进行中', value: 'ongoing' },
  { label: '已完成', value: 'done' }
]

const quick = [
  { path: '/routes', label: '推荐线路', desc: '选一条一键规划' },
  { path: '/costs', label: '花费构成', desc: '预算与开销分析' },
  { path: '/footprint', label: '我的足迹', desc: '城市与出行统计' },
  { path: '/checklist', label: '出行清单', desc: '行前勾选准备' }
]

function statusText(s) {
  return { draft: '待规划', generated: '已规划', ongoing: '进行中', done: '已完成' }[s] || s
}

async function load() {
  const [st, list] = await Promise.all([
    api.get('/workbench/stats'),
    api.get('/trips', { params: { status: filter.value || undefined } })
  ])
  stats.value = st || {}
  trips.value = list || []
}

onMounted(load)
</script>

<style scoped>
.trip-row {
  display: flex; justify-content: space-between; gap: 16px; padding: 16px 4px;
  border-bottom: 1px solid #f3ede3; cursor: pointer;
}
.trip-row:last-child { border-bottom: 0; }
.trip-row:hover { background: #faf6ef; }
</style>
