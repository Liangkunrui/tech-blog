package com.blog.common;

/**
 * 通知类型常量（与 notification 表 type 字段对应）
 *
 * @author Liangkunrui
 */
public final class NotificationType {

    public static final int COMMENT = 1;
    public static final int LIKE = 2;
    public static final int FAVORITE = 3;
    public static final int FOLLOW = 4;
    public static final int SYSTEM = 5;

    private NotificationType() {
    }
}
