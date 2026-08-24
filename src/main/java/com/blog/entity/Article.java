package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文章实体
 *
 * @author Liangkunrui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("article")
public class Article extends BaseEntity {

    /** 作者ID */
    private Long userId;

    /** 分类ID */
    private Long categoryId;

    /** 标题 */
    private String title;

    /** Markdown 内容 */
    private String content;

    /** 摘要 */
    private String summary;

    /** 状态: 0草稿 1已发布 */
    private Integer status;

    /** 浏览量 */
    private Integer viewCount;

    /** 点赞数 */
    private Integer likeCount;

    /** 收藏数 */
    private Integer favoriteCount;

    /** 评论数 */
    private Integer commentCount;

    /** 逻辑删除: 0未删 1已删 */
    @TableLogic
    private Integer deleted;
}
