<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { createArticle, updateArticle, getArticle, listCategories, createCategory } from '@/api/article'
import { renderMarkdown } from '@/utils/markdown'
import { toast } from '@/utils/message'
import type { CategoryVO } from '@/api/types'

const route = useRoute()
const router = useRouter()

const editId = computed(() => (route.query.id as string) || '')
const isEdit = computed(() => !!editId.value)

const title = ref('')
const content = ref('')
const summary = ref('')
const categoryId = ref<string | undefined>()
const tagsInput = ref('')
const categories = ref<CategoryVO[]>([])
const newCategoryName = ref('')
const creatingCategory = ref(false)
const preview = ref(false)
const submitting = ref(false)
const loading = ref(false)
const notFound = ref(false)

const previewHtml = computed(() => renderMarkdown(content.value))

function parseTags(): string[] {
  return tagsInput.value
    .split(/[,，]/)
    .map((t) => t.trim())
    .filter(Boolean)
}

async function loadCategories() {
  const res = await listCategories()
  categories.value = res.data
}

/** 新建分类：创建成功后选中；重名时刷新列表并选中同名分类 */
async function createNewCategory() {
  const name = newCategoryName.value.trim()
  if (!name) {
    toast('请输入分类名', 'error')
    return
  }
  creatingCategory.value = true
  try {
    const res = await createCategory(name)
    toast('分类已创建')
    newCategoryName.value = ''
    await loadCategories()
    categoryId.value = res.data.id
  } catch {
    // 已存在（后端返回 400"分类已存在"）：刷新列表并选中同名分类
    await loadCategories()
    const existed = categories.value.find((c) => c.name === name)
    if (existed) {
      categoryId.value = existed.id
      toast('分类已存在，已为你选中')
    }
  } finally {
    creatingCategory.value = false
  }
}

async function loadArticle() {
  loading.value = true
  try {
    const res = await getArticle(editId.value)
    const a = res.data
    title.value = a.title
    content.value = a.content
    summary.value = a.summary || ''
    categoryId.value = a.category?.id
    tagsInput.value = (a.tags || []).map((t) => t.name).join(',')
  } catch {
    notFound.value = true
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!title.value.trim()) {
    toast('请输入标题', 'error')
    return
  }
  if (!content.value.trim()) {
    toast('请输入内容', 'error')
    return
  }
  submitting.value = true
  const payload = {
    title: title.value.trim(),
    content: content.value,
    summary: summary.value.trim() || undefined,
    categoryId: categoryId.value ? Number(categoryId.value) : undefined,
    tags: parseTags(),
  }
  try {
    const res = isEdit.value
      ? await updateArticle(editId.value, payload)
      : await createArticle(payload)
    toast(isEdit.value ? '更新成功' : '发布成功')
    router.push(`/articles/${res.data.id}`)
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  if (isEdit.value) {
    loadArticle()
  }
})
</script>

<template>
  <div class="editor">
    <h2>{{ isEdit ? '编辑文章' : '发布文章' }}</h2>
    <p v-if="notFound" class="muted">文章不存在或已删除，<router-link to="/write">重新发布</router-link></p>
    <template v-else>
      <div class="card form">
        <div class="form-group">
          <label>标题 *</label>
          <input v-model="title" class="form-input" maxlength="100" placeholder="请输入标题" />
        </div>
        <div class="form-group row">
          <div class="half">
            <label>分类</label>
            <select v-model="categoryId" class="form-input">
              <option :value="undefined">不选择</option>
              <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
            </select>
            <div class="category-create">
              <input
                v-model="newCategoryName"
                class="form-input"
                maxlength="50"
                placeholder="新分类名"
                @keyup.enter="createNewCategory"
              />
              <button class="btn btn-outline" :disabled="creatingCategory" @click="createNewCategory">
                {{ creatingCategory ? '创建中...' : '新建' }}
              </button>
            </div>
          </div>
          <div class="half">
            <label>标签（逗号分隔）</label>
            <input v-model="tagsInput" class="form-input" placeholder="如：Spring,Redis" />
          </div>
        </div>
        <div class="form-group">
          <label>摘要</label>
          <input v-model="summary" class="form-input" maxlength="255" placeholder="选填，列表页展示" />
        </div>
        <div class="form-group">
          <label>内容（Markdown）*</label>
          <div class="editor-toolbar">
            <button class="btn btn-outline" @click="preview = !preview">
              {{ preview ? '编辑' : '预览' }}
            </button>
          </div>
          <textarea v-if="!preview" v-model="content" class="form-input markdown-area" rows="14" placeholder="支持 Markdown 语法"></textarea>
          <div v-else class="markdown-preview" v-html="previewHtml"></div>
        </div>
        <button class="btn btn-primary" :disabled="submitting" @click="submit">
          {{ submitting ? '提交中...' : isEdit ? '保存修改' : '发布' }}
        </button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.editor h2 {
  margin-bottom: 16px;
}

.row {
  display: flex;
  gap: 16px;
}

.half {
  flex: 1;
}

.category-create {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}

.category-create .form-input {
  flex: 1;
}

.category-create .btn {
  flex-shrink: 0;
}

.editor-toolbar {
  margin-bottom: 8px;
}

.markdown-area {
  font-family: 'Consolas', 'Courier New', monospace;
  resize: vertical;
}

.markdown-preview {
  min-height: 240px;
  border: 1px solid #d0d7de;
  border-radius: 6px;
  padding: 12px;
  background: #fafbfc;
}

.markdown-preview :deep(pre) {
  background: #f6f8fa;
  padding: 12px;
  border-radius: 6px;
  overflow-x: auto;
}

.markdown-preview :deep(img) {
  max-width: 100%;
}
</style>
