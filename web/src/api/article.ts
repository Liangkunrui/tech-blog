import request from './request'
import type { Result, Page, ArticleListItem, ArticleDetail, CategoryVO, TagVO } from './types'

export interface ArticleQuery {
  pageNum?: number
  pageSize?: number
  categoryId?: number
  tagId?: number
  keyword?: string
  sort?: string
}

export interface ArticlePayload {
  title: string
  content: string
  summary?: string
  categoryId?: number | null
  status?: number
  tags?: string[]
}

export function listArticles(params: ArticleQuery) {
  return request.get<unknown, Result<Page<ArticleListItem>>>('/articles', { params })
}

export function hotArticles(topN = 10) {
  return request.get<unknown, Result<ArticleListItem[]>>('/articles/hot', { params: { topN } })
}

export function getArticle(id: number | string) {
  return request.get<unknown, Result<ArticleDetail>>(`/articles/${id}`)
}

export function createArticle(payload: ArticlePayload) {
  return request.post<unknown, Result<ArticleDetail>>('/articles', payload)
}

export function updateArticle(id: number | string, payload: Partial<ArticlePayload>) {
  return request.put<unknown, Result<ArticleDetail>>(`/articles/${id}`, payload)
}

export function deleteArticle(id: number | string) {
  return request.delete<unknown, Result<null>>(`/articles/${id}`)
}

export function listCategories() {
  return request.get<unknown, Result<CategoryVO[]>>('/categories')
}

/** 创建分类（重名时后端返回 400"分类已存在"） */
export function createCategory(name: string, sort = 0) {
  return request.post<unknown, Result<CategoryVO>>('/categories', null, { params: { name, sort } })
}

export function listTags() {
  return request.get<unknown, Result<TagVO[]>>('/tags')
}
