package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
import com.blog.common.NotificationType;
import com.blog.common.ResultCode;
import com.blog.dto.CommentCreateRequest;
import com.blog.entity.Article;
import com.blog.entity.Comment;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CommentMapper;
import com.blog.mapper.UserMapper;
import com.blog.vo.CommentVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 评论服务
 *
 * @author Liangkunrui
 */
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentMapper commentMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final ArticleService articleService;
    private final NotificationService notificationService;

    /**
     * 发表评论（仅已发布文章可评论）
     */
    @Transactional
    public CommentVO create(Long userId, Long articleId, CommentCreateRequest request) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (request.getParentId() != null && request.getParentId() != 0) {
            Comment parent = commentMapper.selectById(request.getParentId());
            if (parent == null || !parent.getArticleId().equals(articleId)) {
                throw new BusinessException(400, "父评论不存在");
            }
        }
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(userId);
        comment.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        comment.setContent(request.getContent());
        comment.setStatus(1);
        commentMapper.insert(comment);

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .setSql("comment_count = comment_count + 1"));
        articleService.evictDetailCache(articleId);
        notificationService.publish(NotificationType.COMMENT, userId, article.getUserId(), articleId,
                "评论了你的文章《" + article.getTitle() + "》");

        User user = userMapper.selectById(userId);
        CommentVO vo = CommentVO.from(comment);
        if (user != null) {
            vo.setUsername(user.getUsername());
            vo.setNickname(user.getNickname());
            vo.setAvatar(user.getAvatar());
        }
        return vo;
    }

    /**
     * 文章评论分页列表（公开）
     */
    public IPage<CommentVO> pageByArticle(Long articleId, long pageNum, long pageSize) {
        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Comment>()
                        .eq(Comment::getArticleId, articleId)
                        .eq(Comment::getStatus, 1)
                        .orderByAsc(Comment::getCreateTime));
        Set<Long> userIds = page.getRecords().stream()
                .map(Comment::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user));
        return page.convert(comment -> {
            CommentVO vo = CommentVO.from(comment);
            User user = userMap.get(comment.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
                vo.setAvatar(user.getAvatar());
            }
            return vo;
        });
    }
}
