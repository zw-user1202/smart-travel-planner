<template>
  <div class="login-page">
    <div class="login-card card">
      <div class="brand">
        <div class="logo">✈</div>
        <h1>旅行规划助手工作台</h1>
        <p class="muted">基于 Java Web · 智能行程规划 · 地图展示</p>
      </div>

      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-position="top" @submit.prevent="doLogin">
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" placeholder="请输入用户名" size="large" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" size="large" @keyup.enter="doLogin" />
            </el-form-item>
            <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doLogin">登 录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm" label-position="top" @submit.prevent="doRegister">
            <el-form-item label="用户名">
              <el-input v-model="regForm.username" placeholder="3-20位用户名" size="large" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="regForm.nickname" placeholder="怎么称呼你" size="large" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="regForm.password" type="password" show-password placeholder="至少6位" size="large" />
            </el-form-item>
            <el-button type="primary" size="large" style="width:100%" :loading="loading" @click="doRegister">注 册</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const tab = ref('login')
const loading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', password: '', nickname: '' })

async function doLogin() {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}

async function doRegister() {
  if (!regForm.username || !regForm.password) {
    ElMessage.warning('请填写用户名和密码')
    return
  }
  loading.value = true
  try {
    await userStore.register(regForm)
    ElMessage.success('注册成功，已自动登录')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background:
    radial-gradient(circle at 20% 20%, rgba(59,130,246,.18), transparent 40%),
    radial-gradient(circle at 80% 80%, rgba(16,185,129,.15), transparent 40%),
    #f4f7fb;
  padding: 24px;
}
.login-card { width: 420px; padding: 32px 28px; }
.brand { text-align: center; margin-bottom: 18px; }
.logo {
  width: 56px; height: 56px; margin: 0 auto 10px; border-radius: 16px;
  background: linear-gradient(135deg,#3b82f6,#1d4ed8); color:#fff;
  display:flex; align-items:center; justify-content:center; font-size:26px;
}
.brand h1 { font-size: 20px; margin-bottom: 4px; }
</style>
