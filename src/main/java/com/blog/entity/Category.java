package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 分类实体
 *
 * @author Liangkunrui
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class Category extends BaseEntity {

    /** 分类名称 */
    private String name;

    /** 排序值（越小越靠前） */
    private Integer sort;
}
