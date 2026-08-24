<script setup lang="ts">
import { useRoute } from 'vue-router'

const route = useRoute()
</script>

<template>
  <div class="card placeholder">
    <h2>文章详情（开发中）</h2>
    <p class="muted">文章 ID：{{ route.params.id }}</p>
    <p class="muted">计划：Markdown 渲染、评论、点赞、收藏（见 web/docs/开发计划.md F2/F3）</p>
  </div>
</template>

<style scoped>
.placeholder {
  padding: 40px;
  text-align: center;
}
</style>
