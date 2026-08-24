package com.blog.vo;

import com.blog.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分类视图对象
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryVO {

    private Long id;
    private String name;
    private Integer sort;
    private Long articleCount;

    public static CategoryVO from(Category category) {
        return new CategoryVO(category.getId(), category.getName(), category.getSort(), null);
    }
}
