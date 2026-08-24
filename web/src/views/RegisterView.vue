<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'

const userStore = useUserStore()
const router = useRouter()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const nickname = ref('')
const submitting = ref(false)

async function submit() {
  if (!username.value || !password.value) {
    toast('请输入用户名和密码', 'error')
    return
  }
  if (password.value !== confirmPassword.value) {
    toast('两次输入的密码不一致', 'error')
    return
  }
  submitting.value = true
  try {
    await userStore.register({
      username: username.value,
      password: password.value,
      nickname: nickname.value || undefined,
    })
    toast('注册成功，请登录')
    router.push('/login')
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
      <h2>注册</h2>
      <div class="form-group">
        <label>用户名（4-20位）</label>
        <input v-model="username" class="form-input" autocomplete="off" placeholder="请输入用户名" />
      </div>
      <div class="form-group">
        <label>密码（6-32位）</label>
        <input v-model="password" type="password" class="form-input" autocomplete="new-password" placeholder="请输入密码" />
      </div>
      <div class="form-group">
        <label>确认密码</label>
        <input v-model="confirmPassword" type="password" class="form-input" autocomplete="new-password" placeholder="再次输入密码" />
      </div>
      <div class="form-group">
        <label>昵称（可选）</label>
        <input v-model="nickname" class="form-input" placeholder="默认与用户名相同" />
      </div>
      <button class="btn btn-primary auth-btn" :disabled="submitting" @click="submit">
        {{ submitting ? '注册中...' : '注册' }}
      </button>
      <p class="muted auth-tip">
        已有账号？<router-link to="/login">去登录</router-link>
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
