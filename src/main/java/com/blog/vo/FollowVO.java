package com.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 关注/粉丝视图对象（个人中心）
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FollowVO {

    private Long userId;
    private String username;
    private String nickname;
    private String avatar;
    private LocalDateTime followTime;
}
