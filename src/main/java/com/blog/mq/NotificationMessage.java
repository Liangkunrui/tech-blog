package com.blog.mq;

/**
 * 通知消息体（生产者 → RabbitMQ → 消费者）
 *
 * @param eventId    消息唯一ID（用于消费去重）
 * @param type       通知类型（见 NotificationType）
 * @param fromUserId 触发者ID
 * @param toUserId   接收者ID
 * @param targetId   目标ID（如文章ID）
 * @param content    通知内容
 * @author Liangkunrui
 */
public record NotificationMessage(String eventId, int type, Long fromUserId,
                                  Long toUserId, Long targetId, String content) {
}
