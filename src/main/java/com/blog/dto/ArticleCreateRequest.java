package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 发布文章请求
 *
 * @author Liangkunrui
 */
@Data
public class ArticleCreateRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题长度不能超过100")
    private String title;

    @NotBlank(message = "内容不能为空")
    private String content;

    @Size(max = 255, message = "摘要长度不能超过255")
    private String summary;

    /** 分类ID（可选，提供时需存在） */
    private Long categoryId;

    /** 状态: 0草稿 1已发布（默认已发布） */
    private Integer status;

    /** 标签名称列表（不存在则自动创建） */
    private List<@Size(max = 50, message = "标签名长度不能超过50") String> tags;
}
