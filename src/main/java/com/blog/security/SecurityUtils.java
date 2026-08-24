package com.blog.security;

import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：获取当前登录用户
 *
 * @author Liangkunrui
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户ID，未登录时抛出 401 业务异常
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser.userId();
        }
        throw new BusinessException(ResultCode.UNAUTHORIZED);
    }
}
