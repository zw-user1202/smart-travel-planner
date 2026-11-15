<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <p class="eyebrow">NEW TRIP</p>
            <h1 class="title">新建行程</h1>
            <p class="sub">填写目的地、日期、人数与偏好，创建后可一键智能规划。</p>
          </div>
        </div>
        <div class="card" style="max-width:760px">
          <el-form :model="form" label-width="90px" label-position="left">
            <el-form-item label="目的地" required>
              <el-input v-model="form.destination" placeholder="例如：长沙 / 成都 / 杭州" size="large" clearable />
            </el-form-item>
            <el-form-item label="行程标题">
              <el-input v-model="form.title" placeholder="留空将自动生成" size="large" clearable />
            </el-form-item>
            <el-form-item label="日期范围" required>
              <el-date-picker
                v-model="range"
                type="daterange"
                size="large"
                value-format="YYYY-MM-DD"
                start-placeholder="出发日期"
                end-placeholder="返回日期"
                :disabled-date="d => d.getTime() < Date.now() - 86400000"
                style="width:100%"
              />
            </el-form-item>
            <el-form-item label="出行人数" required>
              <el-input-number v-model="form.peopleCount" :min="1" :max="20" size="large" />
            </el-form-item>
            <el-form-item label="总预算">
              <el-input-number v-model="form.budget" :min="0" :step="100" :precision="0" size="large" style="width:220px" />
              <span class="muted" style="margin-left:8px">元（可选）</span>
            </el-form-item>
            <el-form-item label="旅行偏好">
              <el-checkbox-group v-model="form.preferences">
                <el-checkbox v-for="p in prefs" :key="p" :label="p">{{ p }}</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" :loading="loading" @click="submit(false)">创建行程</el-button>
              <el-button type="success" size="large" :loading="loading" @click="submit(true)">创建并智能规划</el-button>
              <el-button size="large" @click="$router.push('/')">返回</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppSide from '@/components/AppSide.vue'
import api from '@/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const range = ref([])
const prefs = ['自然风光', '人文历史', '美食探店', '亲子友好', '摄影打卡', '休闲放松', '户外徒步', '夜生活']
const form = reactive({
  destination: '',
  title: '',
  peopleCount: 2,
  budget: 3000,
  preferences: ['人文历史', '美食探店']
})

onMounted(() => {
  if (route.query.dest) form.destination = String(route.query.dest)
  const days = Number(route.query.days || 0)
  if (days > 0) {
    const start = new Date()
    start.setDate(start.getDate() + 7)
    const end = new Date(start)
    end.setDate(end.getDate() + days - 1)
    const fmt = d => d.toISOString().slice(0, 10)
    range.value = [fmt(start), fmt(end)]
  }
  if (route.query.prefs) {
    form.preferences = String(route.query.prefs).split(',').filter(Boolean)
  }
  if (route.query.budget) form.budget = Number(route.query.budget) || form.budget
})

async function submit(withPlan) {
  if (!form.destination) {
    ElMessage.warning('请填写目的地')
    return
  }
  if (!range.value || range.value.length !== 2) {
    ElMessage.warning('请选择日期范围')
    return
  }
  loading.value = true
  try {
    const trip = await api.post('/trips', {
      destination: form.destination,
      title: form.title,
      startDate: range.value[0],
      endDate: range.value[1],
      peopleCount: form.peopleCount,
      budget: form.budget,
      preferences: form.preferences
    })
    if (withPlan) {
      ElMessage.success('行程已创建，正在生成规划…')
      await api.post(`/trips/${trip.id}/generate`)
      ElMessage.success('规划完成')
    } else {
      ElMessage.success('行程已创建')
    }
    router.push('/trips/' + trip.id)
  } finally {
    loading.value = false
  }
}
</script>
