package com.blog.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.blog.common.Result;
import com.blog.security.SecurityUtils;
import com.blog.service.NotificationService;
import com.blog.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知接口
 *
 * @author Liangkunrui
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 我的通知列表（分页）
     */
    @GetMapping
    public Result<IPage<NotificationVO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                              @RequestParam(defaultValue = "10") long pageSize) {
        return Result.ok(notificationService.pageMine(SecurityUtils.getCurrentUserId(), pageNum, pageSize));
    }

    /**
     * 未读通知数
     */
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(notificationService.unreadCount(SecurityUtils.getCurrentUserId()));
    }

    /**
     * 标记单条已读
     */
    @PutMapping("/{id}/read")
    public Result<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.getCurrentUserId(), id);
        return Result.ok();
    }

    /**
     * 全部标记已读
     */
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.getCurrentUserId());
        return Result.ok();
    }
}
