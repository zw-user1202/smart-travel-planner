<template>
  <div class="layout">
    <AppSide />
    <main class="main">
      <div class="page">
        <div class="head">
          <div>
            <p class="eyebrow">ADMIN</p>
            <h1 class="title">后台管理</h1>
            <p class="sub">管理员入口占位：用户 / 配置等能力可在完整后台模块中继续扩展。</p>
          </div>
          <button type="button" class="btn-ghost" @click="$router.push('/')">返回总览</button>
        </div>
        <div class="card" style="max-width:720px">
          <div class="card-title">说明</div>
          <p class="muted" style="line-height:1.8">
            当前账号角色为 {{ role === 1 ? '管理员' : '普通用户' }}。
            若需完整的用户列表、额度与系统配置界面，可在此模块继续接入 /api/admin/* 接口。
          </p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import AppSide from '@/components/AppSide.vue'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const role = computed(() => Number(user.role) || 0)
</script>
