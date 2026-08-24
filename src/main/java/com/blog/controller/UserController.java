package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.Result;
import com.blog.dto.UpdatePasswordRequest;
import com.blog.dto.UpdateProfileRequest;
import com.blog.security.SecurityUtils;
import com.blog.service.UserService;
import com.blog.vo.ArticleListItemVO;
import com.blog.vo.FavoriteVO;
import com.blog.vo.FollowVO;
import com.blog.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户接口：个人信息、个人中心
 *
 * @author Liangkunrui
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(userService.getById(SecurityUtils.getCurrentUserId()));
    }

    /**
     * 修改个人信息
     */
    @PutMapping("/me")
    public Result<UserVO> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return Result.ok(userService.updateProfile(SecurityUtils.getCurrentUserId(), request));
    }

    /**
     * 修改密码
     */
    @PutMapping("/me/password")
    public Result<Void> updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(SecurityUtils.getCurrentUserId(), request);
        return Result.ok();
    }

    /**
     * 用户主页信息
     */
    @GetMapping("/{id}")
    public Result<UserVO> getUser(@PathVariable Long id) {
        return Result.ok(userService.getById(id));
    }

    /**
     * 个人中心：我的文章
     */
    @GetMapping("/me/articles")
    public Result<IPage<ArticleListItemVO>> myArticles(@RequestParam(defaultValue = "1") long pageNum,
                                                       @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(userService.pageMyArticles(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    /**
     * 个人中心：我的收藏
     */
    @GetMapping("/me/favorites")
    public Result<IPage<FavoriteVO>> myFavorites(@RequestParam(defaultValue = "1") long pageNum,
                                                 @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(userService.pageMyFavorites(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    /**
     * 个人中心：我的关注
     */
    @GetMapping("/me/following")
    public Result<IPage<FollowVO>> myFollowing(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(userService.pageMyFollowing(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    /**
     * 个人中心：我的粉丝
     */
    @GetMapping("/me/followers")
    public Result<IPage<FollowVO>> myFollowers(@RequestParam(defaultValue = "1") long pageNum,
                                               @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(userService.pageMyFollowers(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }
}
