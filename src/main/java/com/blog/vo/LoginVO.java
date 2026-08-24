package com.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录结果视图对象
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /** JWT Token */
    private String token;

    /** 用户信息 */
    private UserVO user;

    public static LoginVO of(String token, UserVO user) {
        return new LoginVO(token, user);
    }
}
