<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  page: number
  total: number
  pageSize: number
  /** 当前页前后各展示的页码数 */
  window?: number
}>()

const emit = defineEmits<{ (e: 'change', page: number): void }>()

const totalPages = computed(() => Math.max(1, Math.ceil(props.total / props.pageSize)))
const windowSize = props.window ?? 5

/** 当前页前后 windowSize 页内的页码列表（首尾页始终保留） */
const pages = computed<number[]>(() => {
  const current = props.page
  const last = totalPages.value
  const set = new Set<number>([1, last])
  for (let p = current - windowSize; p <= current + windowSize; p++) {
    if (p >= 1 && p <= last) set.add(p)
  }
  return [...set].sort((a, b) => a - b)
})

/** 用省略号分隔的页码序列 */
const display = computed<(number | '…')[]>(() => {
  const list = pages.value
  const out: (number | '…')[] = []
  let prev = 0
  for (const p of list) {
    if (prev && p - prev > 1) out.push('…')
    out.push(p)
    prev = p
  }
  return out
})

function go(p: number) {
  if (p < 1 || p > totalPages.value || p === props.page) return
  emit('change', p)
}
</script>

<template>
  <div v-if="totalPages > 1" class="pager">
    <button class="btn btn-outline" :disabled="page <= 1" @click="go(page - 1)">上一页</button>
    <template v-for="(p, i) in display" :key="i">
      <span v-if="p === '…'" class="ellipsis">…</span>
      <button v-else class="btn page-btn" :class="{ active: p === page }" @click="go(p)">
        {{ p }}
      </button>
    </template>
    <button class="btn btn-outline" :disabled="page >= totalPages" @click="go(page + 1)">下一页</button>
    <span class="muted">共 {{ totalPages }} 页</span>
  </div>
</template>

<style scoped>
.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  margin-top: 20px;
  flex-wrap: wrap;
}

.page-btn {
  min-width: 36px;
  padding: 6px 10px;
  background: #fff;
  border: 1px solid #d0d7de;
  border-radius: 6px;
}

.page-btn.active {
  background: #0969da;
  color: #fff;
  border-color: #0969da;
}

.ellipsis {
  color: #6e7781;
}
</style>
