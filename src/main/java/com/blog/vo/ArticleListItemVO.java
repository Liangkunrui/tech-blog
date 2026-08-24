package com.blog.vo;

import com.blog.entity.Article;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文章列表项视图对象
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArticleListItemVO {

    private Long id;
    private String title;
    private String summary;
    private Integer status;
    private Integer viewCount;
    private Integer likeCount;
    private Integer commentCount;
    private LocalDateTime createTime;
    private Long authorId;
    private String authorName;
    private Long categoryId;
    private String categoryName;

    public static ArticleListItemVO from(Article article) {
        return new ArticleListItemVO(article.getId(), article.getTitle(), article.getSummary(),
                article.getStatus(), article.getViewCount(), article.getLikeCount(),
                article.getCommentCount(), article.getCreateTime(),
                article.getUserId(), null, article.getCategoryId(), null);
    }
}
