package com.blog.controller;

import com.blog.common.Result;
import com.blog.service.TagService;
import com.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 标签接口
 *
 * @author Liangkunrui
 */
@RestController
@RequestMapping("/api/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    /**
     * 热门标签列表（公开）
     */
    @GetMapping
    public Result<List<TagVO>> list() {
        return Result.ok(tagService.listHot());
    }
}
