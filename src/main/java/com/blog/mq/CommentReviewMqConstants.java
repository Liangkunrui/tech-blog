package com.blog.mq;

/**
 * 评论审核延时队列常量（RabbitMQ TTL + 死信队列实现延时）
 *
 * @author Liangkunrui
 */
public final class CommentReviewMqConstants {

    /** 延时交换机（direct）：评论进入审核时投递 */
    public static final String DELAY_EXCHANGE = "blog.comment.review.delay.exchange";

    /** 延时队列：消息到期后转入死信交换机 */
    public static final String DELAY_QUEUE = "blog.comment.review.delay.queue";

    /** 延时队列路由键 */
    public static final String DELAY_ROUTING_KEY = "comment.review.delay";

    /** 死信交换机（direct） */
    public static final String DLX = "blog.comment.review.dlx";

    /** 死信路由键 */
    public static final String DLX_ROUTING_KEY = "comment.review.done";

    /** 审核处理队列：收到死信消息后自动放行评论 */
    public static final String REVIEW_QUEUE = "blog.comment.review.queue";

    private CommentReviewMqConstants() {
    }
}
