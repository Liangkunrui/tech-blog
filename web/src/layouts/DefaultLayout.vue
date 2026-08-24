<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { unreadCount } from '@/api/notification'

const userStore = useUserStore()
const router = useRouter()
const unread = ref(0)

async function loadUnread() {
  if (!userStore.isLoggedIn) return
  try {
    const res = await unreadCount()
    unread.value = res.data
  } catch {
    unread.value = 0
  }
}

function logout() {
  userStore.logout()
  unread.value = 0
  router.push('/')
}

onMounted(() => {
  if (userStore.isLoggedIn && !userStore.user) {
    userStore.fetchMe().catch(() => userStore.logout())
  }
  loadUnread()
})

watch(() => userStore.isLoggedIn, loadUnread)
</script>

<template>
  <header class="header">
    <div class="container header-inner">
      <router-link to="/" class="logo">📝 技术博客</router-link>
      <nav class="nav">
        <router-link to="/">首页</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/write">写文章</router-link>
        <router-link v-if="userStore.isLoggedIn" to="/notifications">
          通知<sup v-if="unread > 0" class="badge">{{ unread }}</sup>
        </router-link>
      </nav>
      <div class="user-area">
        <template v-if="userStore.isLoggedIn">
          <router-link to="/me" class="me-link">{{ userStore.displayName }}</router-link>
          <button class="btn btn-outline logout-btn" @click="logout">退出</button>
        </template>
        <template v-else>
          <router-link to="/login">登录</router-link>
          <router-link to="/register" class="register-link">注册</router-link>
        </template>
      </div>
    </div>
  </header>

  <main class="container main">
    <router-view />
  </main>

  <footer class="footer">
    <div class="container">技术博客社区 · SpringBoot + Vue3</div>
  </footer>
</template>

<style scoped>
.header {
  background: #fff;
  border-bottom: 1px solid #d0d7de;
  position: sticky;
  top: 0;
  z-index: 100;
}

.header-inner {
  display: flex;
  align-items: center;
  height: 56px;
  gap: 24px;
}

.logo {
  font-weight: 700;
  font-size: 18px;
  color: #24292f;
}

.nav {
  display: flex;
  gap: 16px;
  flex: 1;
}

.nav a {
  color: #24292f;
}

.badge {
  background: #cf222e;
  color: #fff;
  border-radius: 8px;
  padding: 0 6px;
  font-size: 11px;
}

.user-area {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 14px;
}

.me-link {
  font-weight: 600;
  color: #24292f;
}

.logout-btn {
  padding: 4px 12px;
  font-size: 13px;
}

.main {
  min-height: calc(100vh - 120px);
  padding-top: 20px;
  padding-bottom: 40px;
}

.footer {
  border-top: 1px solid #d0d7de;
  padding: 16px 0;
  text-align: center;
  color: #6e7781;
  font-size: 13px;
}
</style>
