<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <p class="eyebrow">COSTS</p>
            <h1 class="title">花费构成</h1>
            <p class="sub">从已生成的智能规划中聚合开销，对照预算一目了然。</p>
          </div>
        </div>

        <div class="grid-4" style="margin-bottom:20px">
          <div class="stat-tile"><div class="n">¥{{ costs.budget }}</div><div class="t">预算合计</div></div>
          <div class="stat-tile"><div class="n">¥{{ costs.estimated }}</div><div class="t">估算合计</div></div>
          <div class="stat-tile"><div class="n">¥{{ costs.total }}</div><div class="t">规划明细合计</div></div>
          <div class="stat-tile">
            <div class="n" :style="{ color: diffColor }">¥{{ diff }}</div>
            <div class="t">估算 − 预算</div>
          </div>
        </div>

        <div class="grid-2" style="align-items:start">
          <div class="card">
            <div class="card-title">按类型构成</div>
            <div v-if="costs.byType.length">
              <div v-for="row in costs.byType" :key="row.type" class="bar-row">
                <span class="ct">{{ row.type }}</span>
                <div class="bar"><i :style="{ width: row.pct + '%' }"></i></div>
                <span class="cv">¥{{ row.cost }}</span>
              </div>
              <p class="muted" style="margin-top:8px">占比合计 100% · 来自各行程 plan JSON</p>
            </div>
            <p v-else class="muted">还没有已规划行程的费用数据，先去「智能规划」生成方案。</p>
          </div>

          <div class="card">
            <div class="card-title">分行程开销</div>
            <div v-if="rows.length">
              <div v-for="row in rows" :key="row.id" class="kv" style="cursor:pointer" @click="$router.push('/trips/'+row.id)">
                <span>
                  <b>{{ row.title }}</b>
                  <span class="muted" style="display:block">{{ row.destination }} · {{ row.statusText }}</span>
                </span>
                <b style="color:#c2410c">{{ row.estimatedCost ? '¥'+row.estimatedCost : '—' }}</b>
              </div>
            </div>
            <p v-else class="muted">暂无行程</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppSide from '@/components/AppSide.vue'
import { aggregateCosts, loadAllTrips } from '@/api/domain'

const trips = ref([])
const costs = ref({ byType: [], total: 0, budget: 0, estimated: 0 })

const diff = computed(() => Number(costs.value.estimated || 0) - Number(costs.value.budget || 0))
const diffColor = computed(() => (diff.value > 0 ? '#c2410c' : '#166534'))

const rows = computed(() =>
  trips.value.map(t => ({
    ...t,
    statusText: { draft: '待规划', generated: '已规划', ongoing: '进行中', done: '已完成' }[t.status] || t.status
  }))
)

onMounted(async () => {
  trips.value = await loadAllTrips()
  costs.value = aggregateCosts(trips.value)
})
</script>
