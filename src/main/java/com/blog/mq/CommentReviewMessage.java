package com.blog.mq;

/**
 * 评论审核消息体
 *
 * @param commentId 待审核评论ID
 * @author Liangkunrui
 */
public record CommentReviewMessage(Long commentId) {
}
