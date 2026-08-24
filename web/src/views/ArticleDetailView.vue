<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getArticle } from '@/api/article'
import { listComments, createComment, likeArticle, unlikeArticle, likeStatus, favoriteArticle, unfavoriteArticle, favoriteStatus } from '@/api/interaction'
import { useUserStore } from '@/stores/user'
import { renderMarkdown } from '@/utils/markdown'
import { toast } from '@/utils/message'
import type { ArticleDetail, CommentVO } from '@/api/types'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const article = ref<ArticleDetail | null>(null)
const comments = ref<CommentVO[]>([])
const commentTotal = ref(0)
const newComment = ref('')
const liked = ref(false)
const favorited = ref(false)
const commenting = ref(false)
const loading = ref(true)
const notFound = ref(false)

const contentHtml = computed(() => (article.value ? renderMarkdown(article.value.content) : ''))

async function load() {
  loading.value = true
  notFound.value = false
  try {
    const res = await getArticle(route.params.id as string)
    article.value = res.data
    loadComments()
    if (userStore.isLoggedIn) {
      loadStatus()
    }
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  const res = await listComments(route.params.id as string)
  comments.value = res.data.records
  commentTotal.value = res.data.total
}

async function loadStatus() {
  const [l, f] = await Promise.all([
    likeStatus(route.params.id as string),
    favoriteStatus(route.params.id as string),
  ])
  liked.value = l.data
  favorited.value = f.data
}

async function toggleLike() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (liked.value) {
    await unlikeArticle(route.params.id as string)
    liked.value = false
    if (article.value) article.value.likeCount--
  } else {
    await likeArticle(route.params.id as string)
    liked.value = true
    if (article.value) article.value.likeCount++
  }
}

async function toggleFavorite() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (favorited.value) {
    await unfavoriteArticle(route.params.id as string)
    favorited.value = false
    if (article.value) article.value.favoriteCount--
  } else {
    await favoriteArticle(route.params.id as string)
    favorited.value = true
    if (article.value) article.value.favoriteCount++
  }
}

async function submitComment() {
  if (!userStore.isLoggedIn) {
    router.push({ name: 'login', query: { redirect: route.fullPath } })
    return
  }
  if (!newComment.value.trim()) {
    toast('评论内容不能为空', 'error')
    return
  }
  commenting.value = true
  try {
    const res = await createComment(route.params.id as string, newComment.value)
    newComment.value = ''
    if (res.data.status === 0) {
      toast('评论含敏感词，待审核后展示')
    } else {
      toast('评论成功')
    }
    loadComments()
    if (article.value) article.value.commentCount++
  } finally {
    commenting.value = false
  }
}

function fmtTime(s: string) {
  return s ? s.replace('T', ' ').slice(0, 19) : ''
}

onMounted(load)
</script>

<template>
  <div v-if="loading" class="muted">加载中...</div>
  <div v-else-if="notFound" class="card placeholder">
    <h2>文章不存在或已删除</h2>
    <router-link to="/">返回首页</router-link>
  </div>
  <div v-else-if="article" class="detail">
    <h1 class="title">{{ article.title }}</h1>
    <div class="meta muted">
      <span>{{ article.author?.nickname || article.author?.username || '匿名' }}</span>
      <span v-if="article.category">· {{ article.category.name }}</span>
      <span>· {{ fmtTime(article.createTime) }}</span>
      <span>· 👁 {{ article.viewCount }}</span>
    </div>
    <div v-if="article.tags?.length" class="tags">
      <span v-for="t in article.tags" :key="t.id" class="tag">{{ t.name }}</span>
    </div>

    <!-- Markdown 内容 -->
    <div class="card content" v-html="contentHtml"></div>

    <!-- 互动 -->
    <div class="card actions">
      <button class="btn" :class="liked ? 'btn-active' : 'btn-outline'" @click="toggleLike">
        👍 点赞 {{ article.likeCount }}
      </button>
      <button class="btn" :class="favorited ? 'btn-active' : 'btn-outline'" @click="toggleFavorite">
        ⭐ 收藏 {{ article.favoriteCount }}
      </button>
    </div>

    <!-- 评论 -->
    <div class="card comments">
      <h3>评论（{{ commentTotal }}）</h3>
      <div class="comment-form">
        <textarea v-model="newComment" class="form-input" rows="3" placeholder="写下你的评论..."></textarea>
        <button class="btn btn-primary" :disabled="commenting" @click="submitComment">
          {{ commenting ? '提交中...' : '发表评论' }}
        </button>
      </div>
      <div v-if="comments.length" class="comment-list">
        <div v-for="c in comments" :key="c.id" class="comment-item">
          <div class="comment-head">
            <strong>{{ c.nickname || c.username }}</strong>
            <span class="muted">· {{ fmtTime(c.createTime) }}</span>
            <span v-if="c.status === 0" class="pending-tag">待审核</span>
          </div>
          <p>{{ c.content }}</p>
        </div>
      </div>
      <p v-else class="muted empty">暂无评论</p>
    </div>
  </div>
</template>

<style scoped>
.placeholder {
  padding: 40px;
  text-align: center;
}

.title {
  font-size: 26px;
  margin-bottom: 8px;
}

.meta {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.tags {
  margin-bottom: 12px;
}

.content {
  padding: 24px;
  margin-bottom: 12px;
}

.content :deep(h1),
.content :deep(h2),
.content :deep(h3) {
  margin: 16px 0 8px;
}

.content :deep(p) {
  margin: 8px 0;
}

.content :deep(pre) {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}

.content :deep(code) {
  background: #f6f8fa;
  padding: 2px 5px;
  border-radius: 4px;
}

.content :deep(pre code) {
  padding: 0;
  background: none;
}

.content :deep(blockquote) {
  border-left: 4px solid #d0d7de;
  margin: 8px 0;
  padding-left: 12px;
  color: #57606a;
}

.content :deep(img) {
  max-width: 100%;
}

.actions {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
}

.btn-active {
  background: #0969da;
  color: #fff;
}

.comments {
  margin-bottom: 12px;
}

.comments h3 {
  margin-bottom: 12px;
}

.comment-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 16px;
}

.comment-form .btn {
  align-self: flex-end;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.comment-item {
  border-bottom: 1px solid #f0f0f0;
  padding-bottom: 10px;
}

.comment-head {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 4px;
}

.pending-tag {
  background: #fff8c5;
  color: #9a6700;
  font-size: 12px;
  padding: 0 8px;
  border-radius: 8px;
}

.empty {
  padding: 16px 0;
  text-align: center;
}
</style>
