package com.blog.service;

import com.blog.entity.Notification;
import com.blog.mapper.NotificationMapper;
import com.blog.mq.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * 通知消费者：从 RabbitMQ 接收消息，异步生成站内信（按 eventId 去重）
 *
 * @author Liangkunrui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationMapper notificationMapper;

    @RabbitListener(queues = com.blog.mq.NotificationMqConstants.QUEUE)
    public void handle(NotificationMessage message) {
        try {
            Notification notification = new Notification();
            notification.setEventId(message.eventId());
            notification.setUserId(message.toUserId());
            notification.setFromUserId(message.fromUserId());
            notification.setType(message.type());
            notification.setTargetId(message.targetId());
            notification.setContent(message.content());
            notification.setIsRead(0);
            notificationMapper.insert(notification);
            log.info("站内信已生成: eventId={}, toUserId={}, type={}", message.eventId(), message.toUserId(), message.type());
        } catch (DuplicateKeyException e) {
            // event_id 唯一索引冲突 → 重复消息，忽略
            log.warn("重复通知消息，忽略: eventId={}, 原因={}", message.eventId(), e.getMessage());
        } catch (Exception e) {
            // 消费失败：重试耗尽后丢弃，避免死循环（listener.retry 已在配置中开启）
            log.error("通知消费失败: eventId={}, 原因={}", message.eventId(), e.getMessage());
            throw new AmqpRejectAndDontRequeueException("通知消费失败", e);
        }
    }
}
