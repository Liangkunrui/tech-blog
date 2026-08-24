package com.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 收藏视图对象（个人中心-我的收藏）
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FavoriteVO {

    private Long articleId;
    private String articleTitle;
    private LocalDateTime createTime;
}
