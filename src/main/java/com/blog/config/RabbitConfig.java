package com.blog.config;

import com.blog.mq.CommentReviewMqConstants;
import com.blog.mq.NotificationMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：通知交换机（topic）+ 持久化队列 + JSON 消息转换 + 评论审核延时队列
 *
 * @author Liangkunrui
 */
@Configuration
public class RabbitConfig {

    // ---------------- 通知 ----------------

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NotificationMqConstants.EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NotificationMqConstants.QUEUE).build();
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange())
                .with(NotificationMqConstants.ROUTING_KEY_PREFIX + "*");
    }

    // ---------------- 评论审核延时队列（TTL + DLX） ----------------

    /** 延时交换机：待审核评论进入延时队列 */
    @Bean
    public DirectExchange commentReviewDelayExchange() {
        return new DirectExchange(CommentReviewMqConstants.DELAY_EXCHANGE, true, false);
    }

    /** 死信交换机：延时到期的消息转入此交换机，路由到审核处理队列 */
    @Bean
    public DirectExchange commentReviewDlx() {
        return new DirectExchange(CommentReviewMqConstants.DLX, true, false);
    }

    /** 延时队列：消息 TTL 到期后自动进入死信交换机 */
    @Bean
    public Queue commentReviewDelayQueue() {
        return QueueBuilder.durable(CommentReviewMqConstants.DELAY_QUEUE)
                .withArgument("x-dead-letter-exchange", CommentReviewMqConstants.DLX)
                .withArgument("x-dead-letter-routing-key", CommentReviewMqConstants.DLX_ROUTING_KEY)
                .build();
    }

    /** 审核处理队列：消费死信消息，超时自动放行评论 */
    @Bean
    public Queue commentReviewQueue() {
        return QueueBuilder.durable(CommentReviewMqConstants.REVIEW_QUEUE).build();
    }

    @Bean
    public Binding commentReviewDelayBinding() {
        return BindingBuilder.bind(commentReviewDelayQueue())
                .to(commentReviewDelayExchange())
                .with(CommentReviewMqConstants.DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding commentReviewDlxBinding() {
        return BindingBuilder.bind(commentReviewQueue())
                .to(commentReviewDlx())
                .with(CommentReviewMqConstants.DLX_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
