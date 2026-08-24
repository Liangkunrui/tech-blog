package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 点赞实体（点赞以 Redis Set 为准，此表由定时任务回写）
 *
 * @author Liangkunrui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article_like")
public class ArticleLike extends BaseEntity {

    /** 文章ID */
    private Long articleId;

    /** 点赞用户ID */
    private Long userId;
}
