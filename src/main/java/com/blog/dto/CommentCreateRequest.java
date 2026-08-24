package com.blog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论请求
 *
 * @author Liangkunrui
 */
@Data
public class CommentCreateRequest {

    @NotBlank(message = "评论内容不能为空")
    @Size(max = 2000, message = "评论内容长度不能超过2000")
    private String content;

    /** 父评论ID（0或不传为顶级评论） */
    private Long parentId;
}
