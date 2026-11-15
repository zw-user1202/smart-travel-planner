<template>
  <div class="layout">
    <aside class="side">
      <div class="side-brand">✈ 旅行工作台</div>
      <el-menu :default-active="active" router background-color="#0f172a" text-color="#cbd5e1" active-text-color="#fff">
        <el-menu-item index="/">工作台总览</el-menu-item>
        <el-menu-item index="/trips/new">新建行程</el-menu-item>
      </el-menu>
      <div class="side-user">
        <div class="name">{{ nickname }}</div>
        <el-button text type="info" @click="logout">退出登录</el-button>
      </div>
    </aside>

    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <h2>你好，{{ nickname }}</h2>
            <p class="muted">在这里管理你的全部旅行计划：待规划 · 进行中 · 已完成</p>
          </div>
          <el-button type="primary" size="large" @click="$router.push('/trips/new')">
            <el-icon style="margin-right:6px"><Plus /></el-icon>新建行程
          </el-button>
        </div>

        <el-row :gutter="16" class="stats">
          <el-col :span="6"><div class="stat-card"><div class="label">全部行程</div><div class="num">{{ stats.total || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card green"><div class="label">已规划</div><div class="num">{{ (stats.generated||0) + (stats.ongoing||0) }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card orange"><div class="label">待规划</div><div class="num">{{ stats.planned || 0 }}</div></div></el-col>
          <el-col :span="6"><div class="stat-card purple"><div class="label">已完成</div><div class="num">{{ stats.done || 0 }}</div></div></el-col>
        </el-row>

        <el-row :gutter="16" style="margin-top:16px">
          <el-col :span="16">
            <div class="card">
              <div class="card-title">
                <span>行程看板</span>
                <el-radio-group v-model="filter" size="small" @change="loadTrips">
                  <el-radio-button label="">全部</el-radio-button>
                  <el-radio-button label="draft">待规划</el-radio-button>
                  <el-radio-button label="generated">已规划</el-radio-button>
                  <el-radio-button label="ongoing">进行中</el-radio-button>
                  <el-radio-button label="done">已完成</el-radio-button>
                </el-radio-group>
              </div>

              <el-table :data="trips" style="width:100%" @row-click="goDetail" row-key="id">
                <el-table-column prop="title" label="行程" min-width="180">
                  <template #default="{ row }">
                    <div class="trip-title">{{ row.title }}</div>
                    <div class="muted">{{ row.destination }} · {{ row.peopleCount }}人</div>
                  </template>
                </el-table-column>
                <el-table-column label="日期" width="180">
                  <template #default="{ row }">{{ row.startDate }} ~ {{ row.endDate }}</template>
                </el-table-column>
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="预算估算" width="120">
                  <template #default="{ row }">
                    <span v-if="row.estimatedCost">¥{{ row.estimatedCost }}</span>
                    <span v-else class="muted">—</span>
                  </template>
                </el-table-column>
                <el-table-column label="操作" width="160" fixed="right">
                  <template #default="{ row }">
                    <el-button link type="primary" @click.stop="$router.push('/trips/'+row.id)">查看</el-button>
                    <el-button link type="danger" @click.stop="remove(row)">删除</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <el-empty description="还没有行程，点击右上角新建一个吧" />
                </template>
              </el-table>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="card" style="margin-bottom:16px">
              <div class="card-title">额度与预算</div>
              <div class="kv"><span>今日 AI 额度</span><b>{{ stats.todayUsedQuota ?? 0 }} / 5 已用</b></div>
              <div class="kv"><span>预算合计</span><b>¥{{ stats.totalBudget ?? 0 }}</b></div>
              <div class="kv"><span>估算合计</span><b>¥{{ stats.totalEstimated ?? 0 }}</b></div>
              <el-progress :percentage="Math.min(100, ((stats.todayUsedQuota||0)/5)*100)" :stroke-width="10" style="margin-top:12px" />
            </div>
            <div class="card">
              <div class="card-title">最近行程</div>
              <div v-for="t in stats.recentTrips || []" :key="t.id" class="recent" @click="$router.push('/trips/'+t.id)">
                <div>
                  <div style="font-weight:600">{{ t.title }}</div>
                  <div class="muted">{{ t.startDate }} 出发</div>
                </div>
                <el-tag size="small" :type="statusType(t.status)">{{ statusText(t.status) }}</el-tag>
              </div>
              <el-empty v-if="!(stats.recentTrips||[]).length" description="暂无数据" :image-size="60" />
            </div>
          </el-col>
        </el-row>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import api from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const stats = ref({})
const trips = ref([])
const filter = ref('')

const nickname = computed(() => userStore.nickname || userStore.username)
const active = computed(() => route.path)

function statusText(s) {
  return { draft: '待规划', generated: '已规划', ongoing: '进行中', done: '已完成' }[s] || s
}
function statusType(s) {
  return { draft: 'info', generated: 'primary', ongoing: 'warning', done: 'success' }[s] || 'info'
}

async function loadAll() {
  const [st, list] = await Promise.all([
    api.get('/workbench/stats'),
    api.get('/trips', { params: { status: filter.value || undefined } })
  ])
  stats.value = st || {}
  trips.value = list || []
}

function goDetail(row) {
  router.push('/trips/' + row.id)
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除行程「${row.title}」吗？`, '提示', { type: 'warning' })
  await api.delete('/trips/' + row.id)
  ElMessage.success('已删除')
  loadAll()
}

function logout() {
  userStore.logout()
  router.push('/login')
}

onMounted(loadAll)
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.side {
  width: 220px; background: #0f172a; color: #cbd5e1; display: flex; flex-direction: column; position: sticky; top: 0; height: 100vh;
}
.side-brand { padding: 22px 20px; font-weight: 700; color: #fff; font-size: 16px; border-bottom: 1px solid rgba(255,255,255,.06); }
.side :deep(.el-menu) { border-right: none; flex: 1; }
.side-user { padding: 16px; border-top: 1px solid rgba(255,255,255,.06); }
.side-user .name { margin-bottom: 6px; color: #fff; font-weight: 600; }
.main { flex: 1; }
.head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.stats { margin-bottom: 4px; }
.card-title { display: flex; justify-content: space-between; align-items: center; font-weight: 700; margin-bottom: 14px; }
.trip-title { font-weight: 600; cursor: pointer; }
.trip-title:hover { color: #2563eb; }
.kv { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #eef2f7; font-size: 14px; }
.recent { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid #f1f5f9; cursor: pointer; }
.recent:hover { background: #f8fafc; }
:deep(.el-table__row) { cursor: pointer; }
</style>
