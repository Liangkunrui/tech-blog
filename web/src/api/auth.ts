import request from './request'
import type { Result, UserVO, LoginVO } from './types'

export interface RegisterPayload {
  username: string
  password: string
  nickname?: string
  email?: string
  bio?: string
}

export function register(payload: RegisterPayload) {
  return request.post<unknown, Result<UserVO>>('/auth/register', payload)
}

export function login(payload: { username: string; password: string }) {
  return request.post<unknown, Result<LoginVO>>('/auth/login', payload)
}

export function getMe() {
  return request.get<unknown, Result<UserVO>>('/users/me')
}
