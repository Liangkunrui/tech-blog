package com.blog.vo;

import com.blog.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 用户信息视图对象
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String email;
    private String bio;
    private LocalDateTime createTime;

    public static UserVO from(User user) {
        return new UserVO(user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getEmail(), user.getBio(), user.getCreateTime());
    }
}
