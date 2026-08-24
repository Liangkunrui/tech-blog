<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listArticles, hotArticles, listCategories, listTags } from '@/api/article'
import type { ArticleListItem, CategoryVO, TagVO } from '@/api/types'
import Pagination from '@/components/Pagination.vue'

const articles = ref<ArticleListItem[]>([])
const hot = ref<ArticleListItem[]>([])
const categories = ref<CategoryVO[]>([])
const tags = ref<TagVO[]>([])
const total = ref('0')
const pageNum = ref(1)
const pageSize = 10
const keyword = ref('')
const categoryId = ref<string | undefined>()
const tagId = ref<string | undefined>()
const sort = ref('latest')
const loading = ref(false)

async function loadArticles() {
  loading.value = true
  try {
    const res = await listArticles({
      pageNum: pageNum.value,
      pageSize,
      categoryId: categoryId.value ? Number(categoryId.value) : undefined,
      tagId: tagId.value ? Number(tagId.value) : undefined,
      keyword: keyword.value || undefined,
      sort: sort.value,
    })
    articles.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function loadMeta() {
  const [c, t, h] = await Promise.all([listCategories(), listTags(), hotArticles(5)])
  categories.value = c.data
  tags.value = t.data
  hot.value = h.data
}

function search() {
  pageNum.value = 1
  loadArticles()
}

function changePage(p: number) {
  pageNum.value = p
  loadArticles()
}

function fmtTime(s: string) {
  return s ? s.replace('T', ' ').slice(0, 19) : ''
}

onMounted(() => {
  loadArticles()
  loadMeta()
})
</script>

<template>
  <div>
    <!-- 热点 -->
    <section v-if="hot.length" class="hot">
      <h3>🔥 热门文章</h3>
      <div class="hot-list">
        <router-link v-for="(a, i) in hot" :key="a.id" :to="`/articles/${a.id}`" class="hot-item">
          <span class="hot-rank">{{ i + 1 }}</span>
          <span class="hot-title">{{ a.title }}</span>
          <span class="muted">{{ a.viewCount }} 浏览</span>
        </router-link>
      </div>
    </section>

    <!-- 筛选 -->
    <section class="card filter">
      <select v-model="categoryId" class="form-input filter-item" @change="search">
        <option :value="undefined">全部分类</option>
        <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
      </select>
      <select v-model="sort" class="form-input filter-item" @change="search">
        <option value="latest">最新</option>
        <option value="hot">热度</option>
      </select>
      <input
        v-model="keyword"
        class="form-input filter-item filter-keyword"
        placeholder="搜索标题..."
        @keyup.enter="search"
      />
      <button class="btn btn-primary" @click="search">搜索</button>
    </section>

    <section v-if="tags.length" class="tags">
      <span class="tag tag-selectable" :class="{ active: tagId === undefined }" @click="tagId = undefined; search()">全部</span>
      <span
        v-for="t in tags"
        :key="t.id"
        class="tag tag-selectable"
        :class="{ active: tagId === t.id }"
        @click="tagId = t.id; search()"
      >
        {{ t.name }}
      </span>
    </section>

    <!-- 文章列表 -->
    <div v-if="loading" class="muted">加载中...</div>
    <section v-else class="article-list">
      <article v-for="a in articles" :key="a.id" class="card article-item">
        <router-link :to="`/articles/${a.id}`" class="article-title">{{ a.title }}</router-link>
        <p class="muted summary">{{ a.summary || '（无摘要）' }}</p>
        <div class="meta muted">
          <span>{{ a.authorName }}</span>
          <span v-if="a.categoryName">· {{ a.categoryName }}</span>
          <span>· {{ fmtTime(a.createTime) }}</span>
          <span class="stat">👁 {{ a.viewCount }}</span>
          <span class="stat">👍 {{ a.likeCount }}</span>
          <span class="stat">💬 {{ a.commentCount }}</span>
        </div>
      </article>
      <p v-if="!articles.length" class="muted empty">暂无文章</p>
    </section>

    <!-- 分页 -->
    <Pagination :page="pageNum" :total="Number(total)" :page-size="pageSize" @change="changePage" />
  </div>
</template>

<style scoped>
.hot {
  margin-bottom: 16px;
}

.hot-list {
  display: flex;
  gap: 12px;
  overflow-x: auto;
}

.hot-item {
  flex-shrink: 0;
  background: #fff;
  border: 1px solid #d0d7de;
  border-radius: 8px;
  padding: 10px 14px;
  min-width: 220px;
  display: flex;
  gap: 8px;
  align-items: center;
  color: #24292f;
}

.hot-rank {
  background: #0969da;
  color: #fff;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  text-align: center;
  line-height: 22px;
  font-size: 12px;
  flex-shrink: 0;
}

.hot-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter {
  display: flex;
  gap: 12px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.filter-item {
  width: auto;
}

.filter-keyword {
  flex: 1;
  min-width: 160px;
}

.tags {
  margin-bottom: 16px;
}

.tag-selectable {
  cursor: pointer;
}

.tag-selectable.active {
  background: #0969da;
  color: #fff;
}

.article-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.article-item {
  transition: box-shadow 0.2s;
}

.article-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}

.article-title {
  font-size: 18px;
  font-weight: 600;
  color: #24292f;
}

.summary {
  margin: 4px 0 8px;
}

.meta {
  display: flex;
  gap: 12px;
}

.stat {
  margin-left: 4px;
}

.empty {
  text-align: center;
  padding: 40px 0;
}
</style>
