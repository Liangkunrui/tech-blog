package com.blog.controller;

import com.blog.common.Result;
import com.blog.security.SecurityUtils;
import com.blog.service.InteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 互动接口：点赞、收藏、关注
 *
 * @author Liangkunrui
 */
@RestController
@RequiredArgsConstructor
public class InteractionController {

    private final InteractionService interactionService;

    // ---------------- 点赞 ----------------

    @PostMapping("/api/articles/{id}/like")
    public Result<Void> like(@PathVariable Long id) {
        interactionService.like(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @DeleteMapping("/api/articles/{id}/like")
    public Result<Void> unlike(@PathVariable Long id) {
        interactionService.unlike(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @GetMapping("/api/articles/{id}/like/status")
    public Result<Boolean> likeStatus(@PathVariable Long id) {
        return Result.ok(interactionService.isLiked(SecurityUtils.getCurrentUserId(), id));
    }

    // ---------------- 收藏 ----------------

    @PostMapping("/api/articles/{id}/favorite")
    public Result<Void> favorite(@PathVariable Long id) {
        interactionService.favorite(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @DeleteMapping("/api/articles/{id}/favorite")
    public Result<Void> unfavorite(@PathVariable Long id) {
        interactionService.unfavorite(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @GetMapping("/api/articles/{id}/favorite/status")
    public Result<Boolean> favoriteStatus(@PathVariable Long id) {
        return Result.ok(interactionService.isFavorited(SecurityUtils.getCurrentUserId(), id));
    }

    // ---------------- 关注 ----------------

    @PostMapping("/api/users/{id}/follow")
    public Result<Void> follow(@PathVariable Long id) {
        interactionService.follow(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @DeleteMapping("/api/users/{id}/follow")
    public Result<Void> unfollow(@PathVariable Long id) {
        interactionService.unfollow(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    @GetMapping("/api/users/{id}/follow/status")
    public Result<Boolean> followStatus(@PathVariable Long id) {
        return Result.ok(interactionService.isFollowed(SecurityUtils.getCurrentUserId(), id));
    }
}
