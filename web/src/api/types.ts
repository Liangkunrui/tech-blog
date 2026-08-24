/** 后端统一响应结构 */
export interface Result<T = unknown> {
  code: number
  message: string
  data: T
}

export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface UserVO {
  id: number
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
  id: number
  name: string
  sort: number
  articleCount: number
}

export interface TagVO {
  id: number
  name: string
  articleCount: number
}

export interface ArticleListItem {
  id: number
  title: string
  summary: string | null
  status: number
  viewCount: number
  likeCount: number
  commentCount: number
  createTime: string
  authorId: number
  authorName: string | null
  categoryId: number | null
  categoryName: string | null
}

export interface ArticleDetail {
  id: number
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
  id: number
  articleId: number
  parentId: number
  content: string
  status: number
  createTime: string
  userId: number
  username: string
  nickname: string | null
  avatar: string | null
}

export interface NotificationVO {
  id: number
  type: number
  fromUserId: number | null
  fromUserName: string | null
  targetId: number | null
  content: string
  isRead: number
  createTime: string
}
