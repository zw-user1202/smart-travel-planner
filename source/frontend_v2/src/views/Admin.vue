<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <p class="eyebrow">ADMIN</p>
            <h1 class="title">后台管理</h1>
            <p class="sub">用户管理 · 系统配置 · API密钥管理</p>
          </div>
          <button type="button" class="btn-ghost" @click="$router.push('/')">返回总览</button>
        </div>

        <!-- 统计卡片 -->
        <div class="stat-row">
          <div class="stat-card"><div class="stat-num">{{ stats.totalUsers ?? '-' }}</div><div class="stat-label">总用户数</div></div>
          <div class="stat-card"><div class="stat-num">{{ stats.activeUsers ?? '-' }}</div><div class="stat-label">活跃用户</div></div>
          <div class="stat-card"><div class="stat-num">{{ stats.adminUsers ?? '-' }}</div><div class="stat-label">管理员</div></div>
        </div>

        <!-- 用户管理 -->
        <div class="card">
          <div class="card-title">用户管理</div>
          <table class="data-table">
            <thead>
              <tr>
                <th>ID</th><th>用户名</th><th>昵称</th><th>每日AI额度</th><th>角色</th><th>状态</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="u in users" :key="u.id">
                <td>{{ u.id }}</td>
                <td>{{ u.username }}</td>
                <td>{{ u.nickname || '-' }}</td>
                <td>
                  <input type="number" v-model.number="u.dailyQuota" min="0" class="quota-input" @change="updateQuota(u)" />
                </td>
                <td><span :class="u.role === 1 ? 'tag tag-admin' : 'tag tag-user'">{{ u.role === 1 ? '管理员' : '普通用户' }}</span></td>
                <td><span :class="u.status === 1 ? 'tag tag-ok' : 'tag tag-off'">{{ u.status === 1 ? '正常' : '已禁用' }}</span></td>
                <td>
                  <button v-if="u.status === 1" class="btn-sm btn-danger" @click="toggleStatus(u, 0)">禁用</button>
                  <button v-else class="btn-sm btn-ok" @click="toggleStatus(u, 1)">启用</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- API密钥配置 -->
        <div class="card">
          <div class="card-title">API密钥配置</div>
          <div class="form-grid">
            <div class="form-item">
              <label>DeepSeek API Key</label>
              <div class="input-row">
                <input v-model="configMap['deepseek.api-key']" type="password" placeholder="sk-..." class="form-input" />
                <button class="btn-sm" @click="toggleShow('deepseek.api-key')">{{ showKeys['deepseek.api-key'] ? '隐藏' : '显示' }}</button>
              </div>
              <p class="hint">智能规划大模型接口密钥</p>
            </div>
            <div class="form-item">
              <label>DeepSeek 模型名</label>
              <input v-model="configMap['deepseek.model']" type="text" placeholder="deepseek-chat" class="form-input" />
              <p class="hint">默认 deepseek-chat</p>
            </div>
            <div class="form-item">
              <label>高德地图 Web服务 Key</label>
              <div class="input-row">
                <input v-model="configMap['amap.key']" type="password" placeholder="高德Web服务Key" class="form-input" />
                <button class="btn-sm" @click="toggleShow('amap.key')">{{ showKeys['amap.key'] ? '隐藏' : '显示' }}</button>
              </div>
              <p class="hint">用于地理编码、路径规划、天气查询</p>
            </div>
            <div class="form-item">
              <label>高德地图 JS API Key</label>
              <div class="input-row">
                <input v-model="configMap['amap.web-js-key']" type="password" placeholder="高德JS API Key" class="form-input" />
                <button class="btn-sm" @click="toggleShow('amap.web-js-key')">{{ showKeys['amap.web-js-key'] ? '隐藏' : '显示' }}</button>
              </div>
              <p class="hint">用于前端地图展示</p>
            </div>
            <div class="form-item">
              <label>高德地图安全密钥</label>
              <div class="input-row">
                <input v-model="configMap['amap.security-js-code']" type="password" placeholder="安全密钥" class="form-input" />
                <button class="btn-sm" @click="toggleShow('amap.security-js-code')">{{ showKeys['amap.security-js-code'] ? '隐藏' : '显示' }}</button>
              </div>
              <p class="hint">JS API 安全密钥</p>
            </div>
            <div class="form-item">
              <label>百度地图 API Key（预留）</label>
              <div class="input-row">
                <input v-model="configMap['baidu.key']" type="password" placeholder="百度地图API Key" class="form-input" />
                <button class="btn-sm" @click="toggleShow('baidu.key')">{{ showKeys['baidu.key'] ? '隐藏' : '显示' }}</button>
              </div>
              <p class="hint">备用地图服务密钥</p>
            </div>
          </div>
          <div class="form-actions">
            <button class="btn-primary" @click="saveConfig" :disabled="saving">{{ saving ? '保存中...' : '保存配置' }}</button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import AppSide from '@/components/AppSide.vue'
import api from '@/api'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const role = computed(() => Number(user.role) || 0)

const users = ref([])
const stats = ref({})
const configMap = reactive({})
const showKeys = reactive({})
const saving = ref(false)

function toggleShow(key) {
  showKeys[key] = !showKeys[key]
  const inputs = document.querySelectorAll('.form-input')
  // Re-render by toggling type
}

async function loadUsers() {
  try {
    users.value = await api.get('/admin/users')
  } catch (e) { /* ignore */ }
}

async function loadStats() {
  try {
    stats.value = await api.get('/admin/stats')
  } catch (e) { /* ignore */ }
}

async function loadConfig() {
  try {
    const items = await api.get('/admin/config')
    for (const item of items) {
      configMap[item.cfgKey] = item.cfgValue || ''
    }
  } catch (e) { /* ignore */ }
}

async function toggleStatus(u, status) {
  try {
    await api.put(`/admin/users/${u.id}/status`, { status })
    u.status = status
    ElMessage.success(status === 1 ? '已启用' : '已禁用')
    loadStats()
  } catch (e) { /* ignore */ }
}

async function updateQuota(u) {
  try {
    await api.put(`/admin/users/${u.id}/quota`, { dailyQuota: u.dailyQuota })
    ElMessage.success('额度已更新')
  } catch (e) { /* ignore */ }
}

async function saveConfig() {
  saving.value = true
  try {
    const items = Object.entries(configMap).map(([cfgKey, cfgValue]) => ({ cfgKey, cfgValue }))
    await api.put('/admin/config', items)
    ElMessage.success('配置保存成功')
  } catch (e) { /* ignore */ }
  saving.value = false
}

onMounted(() => {
  loadUsers()
  loadStats()
  loadConfig()
})
</script>

<style scoped>
.layout { display: flex; min-height: 100vh; }
.main { flex: 1; padding: 24px 32px; background: #f5f7fa; }
.page { max-width: 1100px; }
.head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 24px; }
.eyebrow { font-size: 12px; color: #8b8fa3; letter-spacing: 2px; margin: 0 0 4px; }
.title { font-size: 26px; font-weight: 700; margin: 0 0 4px; color: #1a1d2e; }
.sub { font-size: 14px; color: #8b8fa3; margin: 0; }

.stat-row { display: flex; gap: 16px; margin-bottom: 24px; }
.stat-card {
  flex: 1; background: #fff; border-radius: 12px; padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-num { font-size: 32px; font-weight: 700; color: #4f6ef7; }
.stat-label { font-size: 13px; color: #8b8fa3; margin-top: 4px; }

.card {
  background: #fff; border-radius: 12px; padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06); margin-bottom: 24px;
}
.card-title { font-size: 16px; font-weight: 600; color: #1a1d2e; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #eef0f5; }

.data-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.data-table th {
  text-align: left; padding: 10px 12px; background: #f8f9fc; color: #6b7280;
  font-weight: 500; border-bottom: 1px solid #eef0f5;
}
.data-table td { padding: 10px 12px; border-bottom: 1px solid #f3f4f8; color: #374151; }
.data-table tr:hover { background: #f8f9ff; }

.tag { display: inline-block; padding: 2px 10px; border-radius: 20px; font-size: 12px; }
.tag-admin { background: #ede9fe; color: #7c3aed; }
.tag-user { background: #e0f2fe; color: #0369a1; }
.tag-ok { background: #dcfce7; color: #16a34a; }
.tag-off { background: #fee2e2; color: #dc2626; }

.quota-input {
  width: 64px; padding: 4px 8px; border: 1px solid #e2e5f0; border-radius: 6px;
  font-size: 13px; text-align: center;
}

.btn-sm {
  padding: 4px 12px; border: none; border-radius: 6px; font-size: 12px;
  cursor: pointer; background: #eef0f5; color: #374151;
}
.btn-sm:hover { background: #e2e5f0; }
.btn-danger { background: #fee2e2; color: #dc2626; }
.btn-danger:hover { background: #fecaca; }
.btn-ok { background: #dcfce7; color: #16a34a; }
.btn-ok:hover { background: #bbf7d0; }

.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.form-item label { display: block; font-size: 13px; font-weight: 500; color: #374151; margin-bottom: 6px; }
.form-input {
  width: 100%; padding: 8px 12px; border: 1px solid #e2e5f0; border-radius: 8px;
  font-size: 13px; box-sizing: border-box;
}
.form-input:focus { outline: none; border-color: #4f6ef7; }
.input-row { display: flex; gap: 8px; }
.input-row .form-input { flex: 1; }
.hint { font-size: 11px; color: #9ca3af; margin: 4px 0 0; }

.form-actions { margin-top: 24px; text-align: right; }
.btn-primary {
  padding: 10px 32px; background: #4f6ef7; color: #fff; border: none;
  border-radius: 8px; font-size: 14px; cursor: pointer;
}
.btn-primary:hover { background: #3b5de7; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.btn-ghost {
  padding: 8px 20px; background: transparent; color: #6b7280;
  border: 1px solid #e2e5f0; border-radius: 8px; cursor: pointer; font-size: 13px;
}
.btn-ghost:hover { background: #f3f4f8; }
</style>
