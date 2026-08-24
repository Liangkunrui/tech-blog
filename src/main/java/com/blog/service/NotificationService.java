package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
import com.blog.common.NotificationType;
import com.blog.common.ResultCode;
import com.blog.entity.Follow;
import com.blog.entity.Notification;
import com.blog.entity.User;
import com.blog.mapper.FollowMapper;
import com.blog.mapper.NotificationMapper;
import com.blog.mapper.UserMapper;
import com.blog.mq.NotificationMessage;
import com.blog.mq.NotificationMqConstants;
import com.blog.vo.NotificationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 通知服务：MQ 生产者（异步生成站内信）+ 用户通知查询
 *
 * @author Liangkunrui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final RabbitTemplate rabbitTemplate;
    private final NotificationMapper notificationMapper;
    private final FollowMapper followMapper;
    private final UserMapper userMapper;

    /**
     * 发布通知事件（异步：发送到 RabbitMQ，由消费者落库）
     */
    public void publish(int type, Long fromUserId, Long toUserId, Long targetId, String content) {
        // 自己触发的不通知自己
        if (toUserId == null || toUserId.equals(fromUserId)) {
            return;
        }
        NotificationMessage message = new NotificationMessage(
                UUID.randomUUID().toString(), type, fromUserId, toUserId, targetId, content);
        try {
            rabbitTemplate.convertAndSend(NotificationMqConstants.EXCHANGE,
                    NotificationMqConstants.ROUTING_KEY_PREFIX + typeKey(type), message);
            log.info("通知消息已发送: type={}, toUserId={}", type, toUserId);
        } catch (Exception e) {
            // ponytail: MQ 不可用时降级为日志，不影响主流程；生产环境应接入重试/补偿
            log.warn("通知消息发送失败（MQ 不可用），已降级: type={}, toUserId={}, 原因={}", type, toUserId, e.getMessage());
        }
    }

    /**
     * 新文章发布后通知作者的粉丝（类型：系统通知）
     */
    public void publishNewArticleToFollowers(Long authorId, Long articleId, String title) {
        List<Follow> follows = followMapper.selectList(
                new LambdaQueryWrapper<Follow>().eq(Follow::getUserId, authorId));
        for (Follow follow : follows) {
            publish(NotificationType.SYSTEM, authorId, follow.getFollowerId(), articleId,
                    "发布了新文章《" + title + "》");
        }
    }

    /**
     * 我的通知列表（分页）
     */
    public IPage<NotificationVO> pageMine(Long userId, long pageNum, long pageSize) {
        Page<Notification> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userId)
                        .orderByDesc(Notification::getCreateTime));
        Set<Long> fromUserIds = page.getRecords().stream()
                .map(Notification::getFromUserId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, User> userMap = fromUserIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(fromUserIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user));
        return page.convert(notification -> {
            NotificationVO vo = NotificationVO.from(notification);
            User fromUser = userMap.get(notification.getFromUserId());
            if (fromUser != null) {
                vo.setFromUserName(fromUser.getNickname() != null ? fromUser.getNickname() : fromUser.getUsername());
            }
            return vo;
        });
    }

    /**
     * 未读通知数
     */
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    /**
     * 标记单条已读（仅本人）
     */
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification == null || !notification.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Notification update = new Notification();
        update.setId(notificationId);
        update.setIsRead(1);
        notificationMapper.updateById(update);
    }

    /**
     * 全部标记已读
     */
    public void markAllRead(Long userId) {
        Notification update = new Notification();
        update.setIsRead(1);
        notificationMapper.update(update, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
    }

    private String typeKey(int type) {
        return switch (type) {
            case NotificationType.COMMENT -> "comment";
            case NotificationType.LIKE -> "like";
            case NotificationType.FAVORITE -> "favorite";
            case NotificationType.FOLLOW -> "follow";
            default -> "system";
        };
    }
}
