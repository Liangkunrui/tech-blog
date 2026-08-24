package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.common.NotificationType;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mq.CommentReviewMessage;
import com.blog.mq.CommentReviewMqConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * 评论审核消费者：延时队列到期后自动放行（status 0 → 1），并补记文章评论数与通知
 *
 * @author Liangkunrui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CommentReviewConsumer {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final ArticleService articleService;
    private final NotificationService notificationService;

    @RabbitListener(queues = CommentReviewMqConstants.REVIEW_QUEUE)
    public void handle(CommentReviewMessage message) {
        Comment comment = commentMapper.selectById(message.commentId());
        if (comment == null) {
            log.warn("审核放行的评论不存在，忽略: commentId={}", message.commentId());
            return;
        }
        if (Integer.valueOf(1).equals(comment.getStatus())) {
            // 已放行（重复消息），忽略
            log.warn("评论已放行，忽略重复消息: commentId={}", message.commentId());
            return;
        }
        comment.setStatus(1);
        commentMapper.updateById(comment);
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, comment.getArticleId())
                .setSql("comment_count = comment_count + 1"));
        articleService.evictDetailCache(comment.getArticleId());

        Article article = articleMapper.selectById(comment.getArticleId());
        notificationService.publish(NotificationType.COMMENT, comment.getUserId(),
                article != null ? article.getUserId() : null, comment.getArticleId(),
                article != null ? "评论了你的文章《" + article.getTitle() + "》" : "评论了你的文章");
        log.info("评论审核通过（延时自动放行）: commentId={}", message.commentId());
    }
}
