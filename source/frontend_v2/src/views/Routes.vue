<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <p class="eyebrow">ROUTES</p>
            <h1 class="title">推荐线路</h1>
            <p class="sub">选一条现成线路，一键填入新建行程并可直接智能规划。</p>
          </div>
          <button type="button" class="btn-primary" @click="$router.push('/trips/new')">+ 自定义行程</button>
        </div>

        <div class="grid-3">
          <button
            v-for="r in routes"
            :key="r.title"
            type="button"
            class="route-card"
            @click="start(r)"
          >
            <span class="rt">{{ r.title }}</span>
            <span class="rm">{{ r.dest }} · {{ r.days }} 天 · 预算约 ¥{{ r.budget }}</span>
            <div class="tags">
              <span v-for="p in r.prefs.split(',')" :key="p" class="tag">{{ p }}</span>
            </div>
            <span style="margin-top:8px;font-size:13px;font-weight:600;color:#c2410c">开始规划 →</span>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import AppSide from '@/components/AppSide.vue'
import { RECOMMENDED_ROUTES } from '@/api/domain'

const routes = RECOMMENDED_ROUTES

function start(r) {
  const query = {
    dest: r.dest,
    days: String(r.days),
    prefs: r.prefs,
    budget: String(r.budget)
  }
  window.location.href = '/trips/new?' + new URLSearchParams(query).toString()
}
</script>
