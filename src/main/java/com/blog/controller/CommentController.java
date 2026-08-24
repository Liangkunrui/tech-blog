package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.Result;
import com.blog.dto.CommentCreateRequest;
import com.blog.security.SecurityUtils;
import com.blog.service.CommentService;
import com.blog.vo.CommentVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论接口
 *
 * @author Liangkunrui
 */
@RestController
@RequestMapping("/api/articles/{articleId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 评论列表（公开）
     */
    @GetMapping
    public Result<IPage<CommentVO>> list(@PathVariable Long articleId,
                                         @RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(commentService.pageByArticle(articleId, pageNum, pageSize));
    }

    /**
     * 发表评论
     */
    @PostMapping
    public Result<CommentVO> create(@PathVariable Long articleId,
                                    @Valid @RequestBody CommentCreateRequest request) {
        return Result.ok(commentService.create(SecurityUtils.getCurrentUserId(), articleId, request));
    }
}
