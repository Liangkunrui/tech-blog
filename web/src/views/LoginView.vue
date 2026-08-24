<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'

const userStore = useUserStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const submitting = ref(false)

async function submit() {
  if (!username.value || !password.value) {
    toast('请输入用户名和密码', 'error')
    return
  }
  submitting.value = true
  try {
    await userStore.login(username.value, password.value)
    toast('登录成功')
    router.push((route.query.redirect as string) || '/')
  } catch {
    /* 错误提示已由拦截器处理 */
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="card auth-card">
      <h2>登录</h2>
      <div class="form-group">
        <label>用户名</label>
        <input v-model="username" class="form-input" autocomplete="off" placeholder="请输入用户名" />
      </div>
      <div class="form-group">
        <label>密码</label>
        <input v-model="password" type="password" class="form-input" autocomplete="new-password" placeholder="请输入密码" @keyup.enter="submit" />
      </div>
      <button class="btn btn-primary auth-btn" :disabled="submitting" @click="submit">
        {{ submitting ? '登录中...' : '登录' }}
      </button>
      <p class="muted auth-tip">
        还没有账号？<router-link to="/register">去注册</router-link>
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  display: flex;
  justify-content: center;
  padding-top: 60px;
}

.auth-card {
  width: 360px;
}

.auth-card h2 {
  margin-bottom: 20px;
}

.auth-btn {
  width: 100%;
  margin-top: 8px;
}

.auth-tip {
  margin-top: 14px;
  text-align: center;
}
</style>
