package com.blog.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 编辑文章请求（仅更新非空字段）
 *
 * @author Liangkunrui
 */
@Data
public class ArticleUpdateRequest {

    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    private String content;

    @Size(max = 255, message = "摘要长度不能超过255")
    private String summary;

    private Long categoryId;

    /** 状态: 0草稿 1已发布 */
    private Integer status;

    /** 标签名称列表（提供时整体替换） */
    private List<@Size(max = 50, message = "标签名长度不能超过50") String> tags;
}
