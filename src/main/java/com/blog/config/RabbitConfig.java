package com.blog.config;

import com.blog.mq.NotificationMqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置：通知交换机（topic）+ 持久化队列 + JSON 消息转换
 *
 * @author Liangkunrui
 */
@Configuration
public class RabbitConfig {

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

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
