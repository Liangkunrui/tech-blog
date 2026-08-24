import request from './request'
import type { Result, Page, CommentVO } from './types'

// 评论
export function listComments(articleId: number | string, pageNum = 1, pageSize = 20) {
  return request.get<unknown, Result<Page<CommentVO>>>(`/articles/${articleId}/comments`, {
    params: { pageNum, pageSize },
  })
}

export function createComment(articleId: number | string, content: string, parentId?: number) {
  return request.post<unknown, Result<CommentVO>>(`/articles/${articleId}/comments`, {
    content,
    parentId,
  })
}

// 点赞
export function likeArticle(articleId: number | string) {
  return request.post<unknown, Result<null>>(`/articles/${articleId}/like`)
}

export function unlikeArticle(articleId: number | string) {
  return request.delete<unknown, Result<null>>(`/articles/${articleId}/like`)
}

export function likeStatus(articleId: number | string) {
  return request.get<unknown, Result<boolean>>(`/articles/${articleId}/like/status`)
}

// 收藏
export function favoriteArticle(articleId: number | string) {
  return request.post<unknown, Result<null>>(`/articles/${articleId}/favorite`)
}

export function unfavoriteArticle(articleId: number | string) {
  return request.delete<unknown, Result<null>>(`/articles/${articleId}/favorite`)
}

export function favoriteStatus(articleId: number | string) {
  return request.get<unknown, Result<boolean>>(`/articles/${articleId}/favorite/status`)
}

// 关注
export function followUser(userId: number) {
  return request.post<unknown, Result<null>>(`/users/${userId}/follow`)
}

export function unfollowUser(userId: number) {
  return request.delete<unknown, Result<null>>(`/users/${userId}/follow`)
}

export function followStatus(userId: number) {
  return request.get<unknown, Result<boolean>>(`/users/${userId}/follow/status`)
}
