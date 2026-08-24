package com.blog.security;

/**
 * 当前登录用户（JWT 解析后存入 SecurityContext 的主体）
 *
 * @param userId   用户ID
 * @param username 用户名
 * @author Liangkunrui
 */
public record LoginUser(Long userId, String username) {
}
