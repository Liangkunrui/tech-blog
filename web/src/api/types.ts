/** 后端统一响应结构 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * 分页对象：id/数值字段在后端统一序列化为字符串（雪花 ID 超 JS 安全整数范围），
 * 使用 Number() 转换后再计算。
 */
export interface Page<T> {
  records: T[]
  total: string
  size: string
  current: string
  pages: string
}

export interface UserVO {
  id: string
  username: string
  nickname: string | null
  avatar: string | null
  email: string | null
  bio: string | null
  createTime: string
}

export interface LoginVO {
  token: string
  user: UserVO
}

export interface CategoryVO {
  id: string
  name: string
  sort: number
  articleCount: string
}

export interface TagVO {
  id: string
  name: string
  articleCount: string
}

export interface ArticleListItem {
  id: string
  title: string
  summary: string | null
  status: number
  viewCount: number
  likeCount: number
  commentCount: number
  createTime: string
  authorId: string
  authorName: string | null
  categoryId: string | null
  categoryName: string | null
}

export interface ArticleDetail {
  id: string
  title: string
  content: string
  summary: string | null
  status: number
  viewCount: number
  likeCount: number
  favoriteCount: number
  commentCount: number
  createTime: string
  updateTime: string
  author: UserVO | null
  category: CategoryVO | null
  tags: TagVO[]
}

export interface CommentVO {
  id: string
  articleId: string
  parentId: string
  content: string
  status: number
  createTime: string
  userId: string
  username: string
  nickname: string | null
  avatar: string | null
}

export interface NotificationVO {
  id: string
  type: number
  fromUserId: string | null
  fromUserName: string | null
  targetId: string | null
  content: string
  isRead: number
  createTime: string
}
