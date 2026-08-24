package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知实体
 *
 * @author Liangkunrui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {

    /** 消息唯一ID（防重复消费） */
    private String eventId;

    /** 接收者ID */
    private Long userId;

    /** 触发者ID（系统通知为空） */
    private Long fromUserId;

    /** 类型: 1评论 2点赞 3收藏 4关注 5系统 */
    private Integer type;

    /** 目标ID（如文章ID） */
    private Long targetId;

    /** 通知内容 */
    private String content;

    /** 是否已读: 0未读 1已读 */
    private Integer isRead;

    /** 逻辑删除: 0未删 1已删 */
    @TableLogic
    private Integer deleted;
}
