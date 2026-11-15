<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <p class="eyebrow">FOOTPRINT</p>
            <h1 class="title">我的足迹</h1>
            <p class="sub">去过哪里、走了多久、完成多少次出行。</p>
          </div>
        </div>

        <div class="grid-4" style="margin-bottom:20px">
          <div class="stat-tile"><div class="n">{{ stats.cities }}</div><div class="t">座城市</div></div>
          <div class="stat-tile"><div class="n">{{ stats.days }}</div><div class="t">天行程</div></div>
          <div class="stat-tile"><div class="n">{{ stats.trips }}</div><div class="t">次出行</div></div>
          <div class="stat-tile"><div class="n">{{ stats.done }}</div><div class="t">已完成</div></div>
        </div>

        <div class="grid-2" style="align-items:start">
          <div class="card">
            <div class="card-title">城市徽章</div>
            <div v-if="stats.cityList?.length" style="display:flex;flex-wrap:wrap;gap:10px">
              <button
                v-for="c in stats.cityList"
                :key="c.name"
                type="button"
                class="city-chip"
                @click="openCity(c.name)"
              >
                {{ c.name }}
                <span class="cnt">×{{ c.count }}</span>
              </button>
            </div>
            <p v-else class="muted">还没有行程，先去新建一个吧。</p>
          </div>

          <div class="card">
            <div class="card-title">出行时间线</div>
            <div v-if="trips.length">
              <div
                v-for="t in trips"
                :key="t.id"
                class="kv"
                style="cursor:pointer"
                @click="$router.push('/trips/'+t.id)"
              >
                <span>
                  <b>{{ t.title }}</b>
                  <span class="muted" style="display:block">{{ t.destination }} · {{ t.startDate }} 出发</span>
                </span>
                <span class="chip" style="cursor:default">{{ statusText(t.status) }}</span>
              </div>
            </div>
            <p v-else class="muted">暂无记录</p>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppSide from '@/components/AppSide.vue'
import { footprintStats, loadAllTrips } from '@/api/domain'

const router = useRouter()
const trips = ref([])
const stats = ref({ cities: 0, days: 0, trips: 0, done: 0, cityList: [] })

function statusText(s) {
  return { draft: '待规划', generated: '已规划', ongoing: '进行中', done: '已完成' }[s] || s
}

function openCity(name) {
  const hit = trips.value.find(t => t.destination === name)
  if (hit) router.push('/trips/' + hit.id)
}

onMounted(async () => {
  trips.value = await loadAllTrips()
  stats.value = footprintStats(trips.value)
})
</script>
