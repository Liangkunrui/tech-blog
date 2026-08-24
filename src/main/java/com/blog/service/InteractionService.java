package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.common.BusinessException;
import com.blog.common.NotificationType;
import com.blog.common.ResultCode;
import com.blog.entity.Article;
import com.blog.entity.ArticleLike;
import com.blog.entity.Favorite;
import com.blog.entity.Follow;
import com.blog.mapper.ArticleLikeMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.FavoriteMapper;
import com.blog.mapper.FollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 互动服务：点赞（Redis Set 为准 + 定时回写）、收藏、关注
 *
 * @author Liangkunrui
 */
@Service
@RequiredArgsConstructor
public class InteractionService {

    private static final String LIKE_KEY_PREFIX = "blog:article:like:";

    private final ArticleMapper articleMapper;
    private final ArticleLikeMapper articleLikeMapper;
    private final FavoriteMapper favoriteMapper;
    private final FollowMapper followMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ArticleService articleService;
    private final NotificationService notificationService;

    // ---------------- 点赞（Redis Set 为准） ----------------

    public void like(Long userId, Long articleId) {
        Article article = requirePublished(articleId);
        Long added = stringRedisTemplate.opsForSet().add(likeKey(articleId), String.valueOf(userId));
        if (added != null && added > 0) {
            notificationService.publish(NotificationType.LIKE, userId, article.getUserId(), articleId,
                    "点赞了你的文章《" + article.getTitle() + "》");
        }
    }

    public void unlike(Long userId, Long articleId) {
        stringRedisTemplate.opsForSet().remove(likeKey(articleId), String.valueOf(userId));
    }

    public boolean isLiked(Long userId, Long articleId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(likeKey(articleId), String.valueOf(userId)));
    }

    /**
     * 定时将 Redis 点赞 Set 回写数据库（article_like 表 + 文章点赞数）
     */
    @Scheduled(fixedDelayString = "${blog.view-flush-ms:300000}")
    public void flushLikeCounts() {
        Set<String> keys = stringRedisTemplate.keys(LIKE_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            Long articleId = Long.valueOf(key.substring(LIKE_KEY_PREFIX.length()));
            Set<String> members = stringRedisTemplate.opsForSet().members(key);
            Set<Long> likedUserIds = members == null ? Set.of()
                    : members.stream().map(Long::valueOf).collect(Collectors.toSet());

            // 回写 article_like 表（以 Redis Set 为准做对账）
            List<ArticleLike> rows = articleLikeMapper.selectList(
                    new LambdaQueryWrapper<ArticleLike>().eq(ArticleLike::getArticleId, articleId));
            for (ArticleLike row : rows) {
                if (!likedUserIds.contains(row.getUserId())) {
                    articleLikeMapper.deleteById(row.getId());
                }
            }
            Set<Long> dbUserIds = rows.stream().map(ArticleLike::getUserId).collect(Collectors.toSet());
            for (Long userId : likedUserIds) {
                if (!dbUserIds.contains(userId)) {
                    ArticleLike like = new ArticleLike();
                    like.setArticleId(articleId);
                    like.setUserId(userId);
                    articleLikeMapper.insert(like);
                }
            }
            // 回写文章点赞数并刷新详情缓存
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId)
                    .setSql("like_count = {0}", likedUserIds.size()));
            articleService.evictDetailCache(articleId);
            if (likedUserIds.isEmpty()) {
                stringRedisTemplate.delete(key);
            }
        }
    }

    // ---------------- 收藏 ----------------

    public void favorite(Long userId, Long articleId) {
        Article article = requirePublished(articleId);
        Long exists = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)
                .eq(Favorite::getUserId, userId));
        if (exists > 0) {
            throw new BusinessException(400, "请勿重复收藏");
        }
        Favorite favorite = new Favorite();
        favorite.setArticleId(articleId);
        favorite.setUserId(userId);
        favoriteMapper.insert(favorite);
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .setSql("favorite_count = favorite_count + 1"));
        articleService.evictDetailCache(articleId);
        notificationService.publish(NotificationType.FAVORITE, userId, article.getUserId(), articleId,
                "收藏了你的文章《" + article.getTitle() + "》");
    }

    public void unfavorite(Long userId, Long articleId) {
        favoriteMapper.delete(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)
                .eq(Favorite::getUserId, userId));
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .gt(Article::getFavoriteCount, 0)
                .setSql("favorite_count = favorite_count - 1"));
        articleService.evictDetailCache(articleId);
    }

    public boolean isFavorited(Long userId, Long articleId) {
        Long exists = favoriteMapper.selectCount(new LambdaQueryWrapper<Favorite>()
                .eq(Favorite::getArticleId, articleId)
                .eq(Favorite::getUserId, userId));
        return exists > 0;
    }

    // ---------------- 关注 ----------------

    public void follow(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw new BusinessException(400, "不能关注自己");
        }
        Long exists = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, targetUserId)
                .eq(Follow::getFollowerId, userId));
        if (exists > 0) {
            throw new BusinessException(400, "请勿重复关注");
        }
        Follow follow = new Follow();
        follow.setUserId(targetUserId);
        follow.setFollowerId(userId);
        followMapper.insert(follow);
        notificationService.publish(NotificationType.FOLLOW, userId, targetUserId, null, "关注了你");
    }

    public void unfollow(Long userId, Long targetUserId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, targetUserId)
                .eq(Follow::getFollowerId, userId));
    }

    public boolean isFollowed(Long userId, Long targetUserId) {
        Long exists = followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getUserId, targetUserId)
                .eq(Follow::getFollowerId, userId));
        return exists > 0;
    }

    // ---------------- 私有方法 ----------------

    private Article requirePublished(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return article;
    }

    private String likeKey(Long articleId) {
        return LIKE_KEY_PREFIX + articleId;
    }
}
