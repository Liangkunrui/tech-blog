<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div class="card placeholder">
    <h2>发布文章（开发中）</h2>
    <p class="muted">编辑模式文章 ID：{{ route.query.id || '（新建）' }}</p>
    <p class="muted">计划：标题/分类/标签/摘要/Markdown 编辑器 + 预览（见 web/docs/开发计划.md F3）</p>
  </div>
</template>

<style scoped>
.placeholder {
  padding: 40px;
  text-align: center;
}
</style>
