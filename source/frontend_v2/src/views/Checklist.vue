<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <p class="eyebrow">CHECKLIST</p>
            <h1 class="title">出行清单</h1>
            <p class="sub">行前逐项勾选，本地保存，不怕漏带。</p>
          </div>
          <div style="min-width:200px">
            <div class="kv" style="border:none;padding:0">
              <span>完成进度</span>
              <b>{{ doneCount }} / {{ list.length }}</b>
            </div>
            <div class="bar" style="height:8px;background:#efe8dc;border-radius:99px;overflow:hidden;margin-top:8px">
              <i :style="{ display:'block', height:'100%', width: pct + '%', background:'#c2410c', borderRadius:'99px' }"></i>
            </div>
          </div>
        </div>

        <div class="grid-2" style="align-items:start">
          <div class="card" v-for="g in groups" :key="g">
            <div class="card-title">{{ g }}</div>
            <label v-for="item in list.filter(x => x.group === g)" :key="item.id" class="check-item">
              <input type="checkbox" :checked="item.done" @change="toggle(item.id)" />
              <span :class="{ done: item.done }">{{ item.text }}</span>
              <button type="button" class="logout" style="margin-left:auto;color:#9f1239" @click.prevent="remove(item.id)">删除</button>
            </label>
          </div>
        </div>

        <div class="card" style="margin-top:16px;max-width:760px">
          <div class="card-title">添加事项</div>
          <div style="display:flex;gap:10px">
            <el-input v-model="newText" placeholder="例如：防晒霜、常用药" size="large" @keyup.enter="add" />
            <el-select v-model="newGroup" size="large" style="width:140px">
              <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
            </el-select>
            <el-button type="primary" size="large" @click="add">添加</el-button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import AppSide from '@/components/AppSide.vue'
import { loadChecklist, saveChecklist } from '@/api/domain'
import { ElMessage } from 'element-plus'

const groups = ['证件票据', '衣物洗护', '电子设备', '出行确认', '其他']
const list = ref(loadChecklist())
const newText = ref('')
const newGroup = ref('其他')

const doneCount = computed(() => list.value.filter(x => x.done).length)
const pct = computed(() => (list.value.length ? Math.round((doneCount.value / list.value.length) * 100) : 0))

function persist() {
  saveChecklist(list.value)
}

function toggle(id) {
  const item = list.value.find(x => x.id === id)
  if (item) item.done = !item.done
  persist()
}

function remove(id) {
  list.value = list.value.filter(x => x.id !== id)
  persist()
}

function add() {
  const text = newText.value.trim()
  if (!text) {
    ElMessage.warning('请输入事项')
    return
  }
  list.value.push({
    id: 'c' + Date.now(),
    group: newGroup.value,
    text,
    done: false
  })
  newText.value = ''
  persist()
  ElMessage.success('已添加')
}
</script>
