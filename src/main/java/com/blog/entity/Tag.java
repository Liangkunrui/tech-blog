package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 标签实体
 *
 * @author Liangkunrui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tag")
public class Tag extends BaseEntity {

    /** 标签名称 */
    private String name;

    /** 文章数（冗余计数） */
    private Integer articleCount;
}
