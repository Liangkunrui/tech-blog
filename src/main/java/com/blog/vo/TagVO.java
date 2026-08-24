package com.blog.vo;

import com.blog.entity.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 标签视图对象
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TagVO {

    private Long id;
    private String name;
    private Integer articleCount;

    public static TagVO from(Tag tag) {
        return new TagVO(tag.getId(), tag.getName(), tag.getArticleCount());
    }
}
