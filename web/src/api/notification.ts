import request from './request'
import type { Result, Page, NotificationVO } from './types'

export function listNotifications(pageNum = 1, pageSize = 20) {
  return request.get<unknown, Result<Page<NotificationVO>>>('/notifications', {
    params: { pageNum, pageSize },
  })
}

export function unreadCount() {
  return request.get<unknown, Result<number>>('/notifications/unread-count')
}

export function markRead(id: number) {
  return request.put<unknown, Result<null>>(`/notifications/${id}/read`)
}

export function markAllRead() {
  return request.put<unknown, Result<null>>('/notifications/read-all')
}
