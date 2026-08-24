<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listNotifications, markRead, markAllRead } from '@/api/notification'
import type { NotificationVO } from '@/api/types'

const notifications = ref<NotificationVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = 20
const loading = ref(false)

const TYPE_TEXT: Record<number, string> = {
  1: '评论',
  2: '点赞',
  3: '收藏',
  4: '关注',
  5: '系统',
}

async function load() {
  loading.value = true
  try {
    const res = await listNotifications(pageNum.value, pageSize)
    notifications.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function readOne(n: NotificationVO) {
  if (n.isRead) return
  await markRead(n.id)
  n.isRead = 1
}

async function readAll() {
  await markAllRead()
  notifications.value.forEach((n) => (n.isRead = 1))
}

function fmtTime(s: string) {
  return s ? s.replace('T', ' ').slice(0, 19) : ''
}

onMounted(load)
</script>

<template>
  <div class="notifications">
    <div class="head">
      <h2>通知中心</h2>
      <button v-if="notifications.length" class="btn btn-outline" @click="readAll">全部已读</button>
    </div>

    <div v-if="loading" class="muted">加载中...</div>
    <div v-else class="card list">
      <div
        v-for="n in notifications"
        :key="n.id"
        class="item"
        :class="{ unread: n.isRead === 0 }"
        @click="readOne(n)"
      >
        <span class="type-tag">{{ TYPE_TEXT[n.type] || '通知' }}</span>
        <div class="body">
          <p>{{ n.content }}</p>
          <p class="muted">{{ n.fromUserName || '系统' }} · {{ fmtTime(n.createTime) }}</p>
        </div>
        <span v-if="n.isRead === 0" class="dot"></span>
      </div>
      <p v-if="!notifications.length" class="muted empty">暂无通知</p>
    </div>

    <div v-if="total > pageSize" class="pager">
      <button class="btn btn-outline" :disabled="pageNum <= 1" @click="pageNum--; load()">上一页</button>
      <span class="muted">{{ pageNum }} / {{ Math.ceil(total / pageSize) }}</span>
      <button class="btn btn-outline" :disabled="pageNum >= Math.ceil(total / pageSize)" @click="pageNum++; load()">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.list {
  display: flex;
  flex-direction: column;
}

.item {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  padding: 12px 4px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
}

.item.unread {
  background: #f6fbff;
}

.type-tag {
  background: #ddf4ff;
  color: #0969da;
  font-size: 12px;
  padding: 2px 10px;
  border-radius: 10px;
  flex-shrink: 0;
}

.body {
  flex: 1;
}

.dot {
  width: 8px;
  height: 8px;
  background: #cf222e;
  border-radius: 50%;
  margin-top: 8px;
  flex-shrink: 0;
}

.empty {
  padding: 24px 0;
  text-align: center;
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 16px;
  margin-top: 16px;
}
</style>
