package com.blog.mq;

/**
 * RabbitMQ 交换机/队列/路由键常量
 *
 * @author Liangkunrui
 */
public final class NotificationMqConstants {

    /** 通知交换机（topic） */
    public static final String EXCHANGE = "blog.notification.exchange";

    /** 通知队列 */
    public static final String QUEUE = "blog.notification.queue";

    /** 路由键前缀，如 notification.comment / notification.like */
    public static final String ROUTING_KEY_PREFIX = "notification.";

    private NotificationMqConstants() {
    }
}
