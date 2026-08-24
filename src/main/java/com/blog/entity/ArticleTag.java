package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章-标签关联实体
 *
 * @author Liangkunrui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article_tag")
public class ArticleTag extends BaseEntity {

    /** 文章ID */
    private Long articleId;

    /** 标签ID */
    private Long tagId;
}
