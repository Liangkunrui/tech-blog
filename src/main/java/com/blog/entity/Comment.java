package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 评论实体
 *
 * @author Liangkunrui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {

    /** 文章ID */
    private Long articleId;

    /** 评论人ID */
    private Long userId;

    /** 父评论ID（0为顶级评论） */
    private Long parentId;

    /** 评论内容 */
    private String content;

    /** 状态: 1通过 0待审核 */
    private Integer status;

    /** 逻辑删除: 0未删 1已删 */
    @TableLogic
    private Integer deleted;
}
