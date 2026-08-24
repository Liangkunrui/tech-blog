package com.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文章详情视图对象
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleDetailVO {

    private Long id;
    private String title;
    private String content;
    private String summary;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer favoriteCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private UserVO author;
    private CategoryVO category;
    private List<TagVO> tags;
}
