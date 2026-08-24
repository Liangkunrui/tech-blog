package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.CategoryService;
import com.blog.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 分类接口
 *
 * @author Liangkunrui
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 分类列表（公开，含文章数）
     */
    @GetMapping
    public Result<List<CategoryVO>> list() {
        return Result.ok(categoryService.listWithArticleCount());
    }

    /**
     * 创建分类
     */
    @PostMapping
    public Result<CategoryVO> create(@RequestParam String name, @RequestParam(defaultValue = "0") Integer sort) {
        return Result.ok(categoryService.create(name, sort));
    }
}
