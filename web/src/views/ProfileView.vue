<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { myArticles, myFavorites, myFollowing, myFollowers, updateProfile, updatePassword } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { toast } from '@/utils/message'
import type { ArticleListItem } from '@/api/types'

const userStore = useUserStore()

const tab = ref<'articles' | 'favorites' | 'following' | 'followers' | 'profile'>('articles')

// 列表数据
const articles = ref<ArticleListItem[]>([])
const favorites = ref<{ articleId: number; articleTitle: string; createTime: string }[]>([])
const following = ref<{ userId: number; username: string; nickname: string; avatar: string | null }[]>([])
const followers = ref<{ userId: number; username: string; nickname: string; avatar: string | null }[]>([])
const loading = ref(false)

// 资料表单
const nickname = ref('')
const email = ref('')
const bio = ref('')
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const saving = ref(false)

async function loadArticles() {
  loading.value = true
  try {
    const res = await myArticles()
    articles.value = res.data.records
  } finally {
    loading.value = false
  }
}

async function loadFavorites() {
  loading.value = true
  try {
    const res = await myFavorites()
    favorites.value = res.data.records
  } finally {
    loading.value = false
  }
}

async function loadFollowing() {
  loading.value = true
  try {
    const res = await myFollowing()
    following.value = res.data.records
  } finally {
    loading.value = false
  }
}

async function loadFollowers() {
  loading.value = true
  try {
    const res = await myFollowers()
    followers.value = res.data.records
  } finally {
    loading.value = false
  }
}

function switchTab(t: typeof tab.value) {
  tab.value = t
  if (t === 'articles' && !articles.value.length) loadArticles()
  if (t === 'favorites' && !favorites.value.length) loadFavorites()
  if (t === 'following' && !following.value.length) loadFollowing()
  if (t === 'followers' && !followers.value.length) loadFollowers()
}

async function saveProfile() {
  saving.value = true
  try {
    const res = await updateProfile({
      nickname: nickname.value || undefined,
      email: email.value || undefined,
      bio: bio.value || undefined,
    })
    userStore.user = res.data
    toast('资料已更新')
  } finally {
    saving.value = false
  }
}

async function savePassword() {
  if (!oldPassword.value || !newPassword.value) {
    toast('请填写密码', 'error')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast('两次输入的新密码不一致', 'error')
    return
  }
  saving.value = true
  try {
    await updatePassword(oldPassword.value, newPassword.value)
    toast('密码已修改，请重新登录')
    oldPassword.value = newPassword.value = confirmPassword.value = ''
    userStore.logout()
  } finally {
    saving.value = false
  }
}

function fmtTime(s: string) {
  return s ? s.replace('T', ' ').slice(0, 19) : ''
}

onMounted(() => {
  if (userStore.user) {
    nickname.value = userStore.user.nickname || ''
    email.value = userStore.user.email || ''
    bio.value = userStore.user.bio || ''
  }
  loadArticles()
})
</script>

<template>
  <div class="profile">
    <div class="card header">
      <div class="avatar">{{ (userStore.displayName || '?').slice(0, 1).toUpperCase() }}</div>
      <div>
        <h2>{{ userStore.displayName }}</h2>
        <p class="muted">@{{ userStore.user?.username }}</p>
        <p v-if="userStore.user?.bio" class="bio">{{ userStore.user.bio }}</p>
      </div>
    </div>

    <nav class="tabs">
      <button v-for="t in (['articles', 'favorites', 'following', 'followers', 'profile'] as const)" :key="t" class="tab" :class="{ active: tab === t }" @click="switchTab(t)">
        {{ { articles: '我的文章', favorites: '我的收藏', following: '我的关注', followers: '我的粉丝', profile: '资料设置' }[t] }}
      </button>
    </nav>

    <div v-if="loading" class="muted">加载中...</div>

    <!-- 我的文章 -->
    <div v-else-if="tab === 'articles'" class="card list">
      <router-link v-for="a in articles" :key="a.id" :to="`/articles/${a.id}`" class="list-item">
        <span class="item-title">{{ a.title }}</span>
        <span class="muted">{{ fmtTime(a.createTime) }} · 👁 {{ a.viewCount }} · 👍 {{ a.likeCount }}</span>
      </router-link>
      <p v-if="!articles.length" class="muted empty">还没有文章，<router-link to="/write">去写一篇</router-link></p>
    </div>

    <!-- 我的收藏 -->
    <div v-else-if="tab === 'favorites'" class="card list">
      <router-link v-for="f in favorites" :key="f.articleId" :to="`/articles/${f.articleId}`" class="list-item">
        <span class="item-title">{{ f.articleTitle }}</span>
        <span class="muted">{{ fmtTime(f.createTime) }}</span>
      </router-link>
      <p v-if="!favorites.length" class="muted empty">暂无收藏</p>
    </div>

    <!-- 关注/粉丝 -->
    <div v-else-if="tab === 'following' || tab === 'followers'" class="card list">
      <div v-for="u in tab === 'following' ? following : followers" :key="u.userId" class="list-item">
        <span class="item-title">{{ u.nickname || u.username }}</span>
        <span class="muted">@{{ u.username }}</span>
      </div>
      <p v-if="!((tab === 'following' ? following : followers).length)" class="muted empty">暂无数据</p>
    </div>

    <!-- 资料设置 -->
    <div v-else class="card form">
      <h3>基本资料</h3>
      <div class="form-group">
        <label>昵称</label>
        <input v-model="nickname" class="form-input" maxlength="50" />
      </div>
      <div class="form-group">
        <label>邮箱</label>
        <input v-model="email" class="form-input" maxlength="100" />
      </div>
      <div class="form-group">
        <label>简介</label>
        <textarea v-model="bio" class="form-input" rows="3" maxlength="255"></textarea>
      </div>
      <button class="btn btn-primary" :disabled="saving" @click="saveProfile">保存资料</button>

      <hr class="divider" />

      <h3>修改密码</h3>
      <div class="form-group">
        <label>原密码</label>
        <input v-model="oldPassword" type="password" class="form-input" />
      </div>
      <div class="form-group">
        <label>新密码</label>
        <input v-model="newPassword" type="password" class="form-input" />
      </div>
      <div class="form-group">
        <label>确认新密码</label>
        <input v-model="confirmPassword" type="password" class="form-input" />
      </div>
      <button class="btn btn-outline" :disabled="saving" @click="savePassword">修改密码</button>
    </div>
  </div>
</template>

<style scoped>
.header {
  display: flex;
  gap: 16px;
  align-items: center;
  margin-bottom: 16px;
}

.avatar {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: #0969da;
  color: #fff;
  font-size: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.bio {
  margin-top: 4px;
  color: #57606a;
  font-size: 14px;
}

.tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.tab {
  padding: 8px 16px;
  border: 1px solid #d0d7de;
  background: #fff;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}

.tab.active {
  background: #0969da;
  color: #fff;
  border-color: #0969da;
}

.list {
  display: flex;
  flex-direction: column;
}

.list-item {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 4px;
  border-bottom: 1px solid #f0f0f0;
  color: #24292f;
}

.item-title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.empty {
  padding: 24px 0;
  text-align: center;
}

.form {
  max-width: 520px;
}

.divider {
  border: none;
  border-top: 1px solid #d0d7de;
  margin: 24px 0;
}
</style>
