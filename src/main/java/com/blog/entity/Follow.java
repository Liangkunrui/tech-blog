package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 关注实体
 *
 * @author Liangkunrui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("follow")
public class Follow extends BaseEntity {

    /** 被关注者ID */
    private Long userId;

    /** 粉丝ID */
    private Long followerId;
}
