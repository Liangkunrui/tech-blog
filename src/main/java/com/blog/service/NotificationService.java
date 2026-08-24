package com.blog.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务（阶段3占位：仅记录日志；阶段4接入 RabbitMQ 异步生成站内信）
 *
 * @author Liangkunrui
 */
@Slf4j
@Service
public class NotificationService {

    /**
     * 发布通知事件
     *
     * @param type       通知类型（见 NotificationType）
     * @param fromUserId 触发者ID
     * @param toUserId   接收者ID
     * @param targetId   目标ID（如文章ID）
     * @param content    通知内容
     */
    public void publish(int type, Long fromUserId, Long toUserId, Long targetId, String content) {
        // 自己触发的不通知自己
        if (toUserId == null || toUserId.equals(fromUserId)) {
            return;
        }
        log.info("通知事件: type={}, fromUserId={}, toUserId={}, targetId={}, content={}",
                type, fromUserId, toUserId, targetId, content);
    }
}
