package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.Result;
import com.blog.dto.ArticleCreateRequest;
import com.blog.dto.ArticleUpdateRequest;
import com.blog.security.SecurityUtils;
import com.blog.service.ArticleService;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleListItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 文章接口
 *
 * @author Liangkunrui
 */
@RestController
@RequestMapping("/api/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 文章分页列表（公开）
     */
    @GetMapping
    public Result<IPage<ArticleListItemVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize,
                                                 @RequestParam(required = false) Long categoryId,
                                                 @RequestParam(required = false) Long tagId,
                                                 @RequestParam(required = false) String keyword,
                                                 @RequestParam(defaultValue = "latest") String sort) {
        return Result.ok(articleService.pageArticles(pageNum, pageSize, categoryId, tagId, keyword, sort));
    }

    /**
     * 热点文章 TopN（公开，按浏览量）
     */
    @GetMapping("/hot")
    public Result<List<ArticleListItemVO>> hot(@RequestParam(defaultValue = "10") int topN) {
        return Result.ok(articleService.hotArticles(topN));
    }

    /**
     * 文章详情（公开，浏览量+1）
     */
    @GetMapping("/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable Long id) {
        return Result.ok(articleService.getDetail(id));
    }

    /**
     * 发布文章
     */
    @PostMapping
    public Result<ArticleDetailVO> create(@Valid @RequestBody ArticleCreateRequest request) {
        return Result.ok(articleService.create(SecurityUtils.getCurrentUserId(), request));
    }

    /**
     * 编辑文章（仅作者）
     */
    @PutMapping("/{id}")
    public Result<ArticleDetailVO> update(@PathVariable Long id,
                                          @Valid @RequestBody ArticleUpdateRequest request) {
        return Result.ok(articleService.update(SecurityUtils.getCurrentUserId(), id, request));
    }

    /**
     * 删除文章（仅作者，逻辑删除）
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.delete(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }
}
