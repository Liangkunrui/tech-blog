package com.blog.vo;

import com.blog.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 通知视图对象
 *
 * @author Liangkunrui
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {

    private Long id;
    private Integer type;
    private Long fromUserId;
    private String fromUserName;
    private Long targetId;
    private String content;
    private Integer isRead;
    private LocalDateTime createTime;

    public static NotificationVO from(Notification notification) {
        return new NotificationVO(notification.getId(), notification.getType(),
                notification.getFromUserId(), null, notification.getTargetId(),
                notification.getContent(), notification.getIsRead(), notification.getCreateTime());
    }
}
