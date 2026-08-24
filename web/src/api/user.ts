import request from './request'
import type { Result, UserVO, Page, ArticleListItem } from './types'

export interface ProfilePayload {
  nickname?: string
  avatar?: string
  email?: string
  bio?: string
}

export function updateProfile(payload: ProfilePayload) {
  return request.put<unknown, Result<UserVO>>('/users/me', payload)
}

export function updatePassword(oldPassword: string, newPassword: string) {
  return request.put<unknown, Result<null>>('/users/me/password', { oldPassword, newPassword })
}

export function myArticles(pageNum = 1, pageSize = 10) {
  return request.get<unknown, Result<Page<ArticleListItem>>>('/users/me/articles', {
    params: { pageNum, pageSize },
  })
}

export function myFavorites(pageNum = 1, pageSize = 10) {
  return request.get<unknown, Result<Page<{ articleId: number; articleTitle: string; createTime: string }>>>(
    '/users/me/favorites',
    { params: { pageNum, pageSize } },
  )
}

export function myFollowing(pageNum = 1, pageSize = 10) {
  return request.get<unknown, Result<Page<{ userId: number; username: string; nickname: string; avatar: string | null }>>>(
    '/users/me/following',
    { params: { pageNum, pageSize } },
  )
}

export function myFollowers(pageNum = 1, pageSize = 10) {
  return request.get<unknown, Result<Page<{ userId: number; username: string; nickname: string; avatar: string | null }>>>(
    '/users/me/followers',
    { params: { pageNum, pageSize } },
  )
}
