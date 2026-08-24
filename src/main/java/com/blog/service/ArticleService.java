package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
import com.blog.common.NullCacheMarker;
import com.blog.common.ResultCode;
import com.blog.dto.ArticleCreateRequest;
import com.blog.dto.ArticleUpdateRequest;
import com.blog.entity.Article;
import com.blog.entity.ArticleTag;
import com.blog.entity.Category;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.ArticleTagMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.TagMapper;
import com.blog.mapper.UserMapper;
import com.blog.vo.ArticleDetailVO;
import com.blog.vo.ArticleListItemVO;
import com.blog.vo.CategoryVO;
import com.blog.vo.TagVO;
import com.blog.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章服务：发布/编辑/删除/列表/详情，Redis 缓存（Cache Aside）+ 浏览量统计
 *
 * @author Liangkunrui
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final String DETAIL_KEY_PREFIX = "blog:article:detail:";
    private static final String LIST_KEY_PREFIX = "blog:article:list:";
    private static final String VIEW_KEY_PREFIX = "blog:article:view:";
    private static final String HOT_KEY_PREFIX = "blog:article:hot:";
    private static final String LOCK_KEY_PREFIX = "blog:article:lock:";
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration HOT_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration NULL_CACHE_TTL = Duration.ofMinutes(2);
    private static final Duration LOCK_TTL = Duration.ofSeconds(3);

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final NotificationService notificationService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 列表缓存内容（分页数据快照）
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CachedList {
        private java.util.List<ArticleListItemVO> records;
        private long total;
        private long current;
        private long size;
    }

    // ---------------- 查询 ----------------

    /**
     * 已发布文章分页列表：支持分类/标签/关键词筛选，按最新或热度排序
     */
    public IPage<ArticleListItemVO> pageArticles(long pageNum, long pageSize, Long categoryId,
                                                 Long tagId, String keyword, String sort) {
        String cacheKey = LIST_KEY_PREFIX + md5(categoryId + "|" + tagId + "|" + keyword + "|" + sort + "|" + pageNum + "|" + pageSize);
        Object cached = readCacheSafe(cacheKey);
        if (cached instanceof CachedList cl) {
            return toPage(cl);
        }

        LambdaQueryWrapper<Article> wrapper = new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .eq(categoryId != null, Article::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Article::getTitle, keyword);
        if (tagId != null) {
            List<Long> articleIds = articleTagMapper.selectList(
                            new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getTagId, tagId))
                    .stream().map(ArticleTag::getArticleId).toList();
            if (articleIds.isEmpty()) {
                Page<ArticleListItemVO> empty = new Page<>(pageNum, pageSize, 0);
                empty.setRecords(List.of());
                return empty;
            }
            wrapper.in(Article::getId, articleIds);
        }
        if ("hot".equalsIgnoreCase(sort)) {
            wrapper.orderByDesc(Article::getViewCount);
        } else {
            wrapper.orderByDesc(Article::getCreateTime);
        }
        wrapper.orderByDesc(Article::getId);

        Page<Article> page = articleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ArticleListItemVO> records = page.getRecords().stream()
                .map(ArticleListItemVO::from)
                .toList();
        fillAuthorAndCategory(records);

        CachedList cl = new CachedList(records, page.getTotal(), page.getCurrent(), page.getSize());
        redisTemplate.opsForValue().set(cacheKey, cl, LIST_CACHE_TTL);
        return toPage(cl);
    }

    /**
     * 文章详情（仅已发布），浏览量 +1（Redis 计数）
     * 缓存策略：空值缓存防穿透 + 互斥锁防击穿（Cache Aside）
     */
    public ArticleDetailVO getDetail(Long id) {
        String cacheKey = DETAIL_KEY_PREFIX + id;
        // 1. 读缓存：命中直接返回；命中空值标记则视为不存在
        ArticleDetailVO cached = readCachedDetail(cacheKey);
        if (cached != null) {
            incrementView(id);
            return cached;
        }
        // 2. 缓存未命中：尝试获取互斥锁，避免热点 Key 失效瞬间的缓存击穿
        String lockKey = LOCK_KEY_PREFIX + id;
        if (tryLock(lockKey)) {
            try {
                // 双重检查：等锁期间可能已被其他线程重建
                cached = readCachedDetail(cacheKey);
                if (cached != null) {
                    incrementView(id);
                    return cached;
                }
                Article article = articleMapper.selectById(id);
                if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
                    // 空值缓存（短 TTL），防缓存穿透
                    redisTemplate.opsForValue().set(cacheKey, NullCacheMarker.INSTANCE, NULL_CACHE_TTL);
                    throw new BusinessException(ResultCode.NOT_FOUND);
                }
                ArticleDetailVO vo = buildDetail(article);
                redisTemplate.opsForValue().set(cacheKey, vo, DETAIL_CACHE_TTL);
                incrementView(id);
                return vo;
            } finally {
                unlock(lockKey);
            }
        }
        // 3. 未拿到锁：短暂等待后重试读缓存（最多 3 次），仍无则回源查询兜底
        for (int i = 0; i < 3; i++) {
            sleep(100);
            cached = readCachedDetail(cacheKey);
            if (cached != null) {
                incrementView(id);
                return cached;
            }
        }
        Article article = articleMapper.selectById(id);
        if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
            redisTemplate.opsForValue().set(cacheKey, NullCacheMarker.INSTANCE, NULL_CACHE_TTL);
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        ArticleDetailVO vo = buildDetail(article);
        redisTemplate.opsForValue().set(cacheKey, vo, DETAIL_CACHE_TTL);
        incrementView(id);
        return vo;
    }

    /**
     * 热点文章 TopN（按浏览量，Redis 缓存）
     */
    public List<ArticleListItemVO> hotArticles(int topN) {
        int limit = Math.min(Math.max(topN, 1), 50);
        String cacheKey = HOT_KEY_PREFIX + limit;
        Object cached = readCacheSafe(cacheKey);
        if (cached instanceof List<?> list) {
            return list.stream()
                    .filter(ArticleListItemVO.class::isInstance)
                    .map(ArticleListItemVO.class::cast)
                    .toList();
        }
        List<Article> articles = articleMapper.selectList(new LambdaQueryWrapper<Article>()
                .eq(Article::getStatus, 1)
                .orderByDesc(Article::getViewCount)
                .orderByDesc(Article::getId)
                .last("LIMIT " + limit));
        List<ArticleListItemVO> records = articles.stream()
                .map(ArticleListItemVO::from)
                .toList();
        fillAuthorAndCategory(records);
        redisTemplate.opsForValue().set(cacheKey, records, HOT_CACHE_TTL);
        return records;
    }

    // ---------------- 写操作 ----------------

    /**
     * 发布文章（标签不存在时自动创建）
     */
    @Transactional
    public ArticleDetailVO create(Long userId, ArticleCreateRequest request) {
        if (request.getCategoryId() != null && categoryMapper.selectById(request.getCategoryId()) == null) {
            throw new BusinessException(400, "分类不存在");
        }
        Article article = new Article();
        article.setUserId(userId);
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setSummary(request.getSummary());
        article.setCategoryId(request.getCategoryId());
        article.setStatus(request.getStatus() == null ? 1 : request.getStatus());
        article.setViewCount(0);
        article.setLikeCount(0);
        article.setFavoriteCount(0);
        article.setCommentCount(0);
        articleMapper.insert(article);
        if (request.getTags() != null) {
            bindTags(article.getId(), request.getTags());
        }
        // 发布成功后异步通知作者的粉丝
        if (Integer.valueOf(1).equals(article.getStatus())) {
            notificationService.publishNewArticleToFollowers(userId, article.getId(), article.getTitle());
        }
        evictListCache();
        evictHotCache();
        return buildDetail(article);
    }

    /**
     * 编辑文章（仅作者），标签提供时整体替换
     */
    @Transactional
    public ArticleDetailVO update(Long userId, Long articleId, ArticleUpdateRequest request) {
        Article article = requireOwnedArticle(articleId, userId);
        if (request.getTitle() != null) {
            article.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            article.setContent(request.getContent());
        }
        if (request.getSummary() != null) {
            article.setSummary(request.getSummary());
        }
        if (request.getCategoryId() != null) {
            if (categoryMapper.selectById(request.getCategoryId()) == null) {
                throw new BusinessException(400, "分类不存在");
            }
            article.setCategoryId(request.getCategoryId());
        }
        if (request.getStatus() != null) {
            article.setStatus(request.getStatus());
        }
        articleMapper.updateById(article);
        if (request.getTags() != null) {
            unbindAllTags(articleId);
            bindTags(articleId, request.getTags());
        }
        evictDetailCache(articleId);
        evictListCache();
        evictHotCache();
        return buildDetail(article);
    }

    /**
     * 删除文章（逻辑删除，仅作者）
     */
    @Transactional
    public void delete(Long userId, Long articleId) {
        requireOwnedArticle(articleId, userId);
        articleMapper.deleteById(articleId);
        unbindAllTags(articleId);
        evictDetailCache(articleId);
        evictListCache();
        evictHotCache();
    }

    // ---------------- 浏览量统计（Redis 计数 + 定时落库） ----------------

    /**
     * 定时将 Redis 中的浏览量增量刷入数据库（默认每 5 分钟，可用 blog.view-flush-ms 调整）
     */
    @Scheduled(fixedDelayString = "${blog.view-flush-ms:300000}")
    public void flushViewCounts() {
        Set<String> keys = stringRedisTemplate.keys(VIEW_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            String deltaStr = stringRedisTemplate.opsForValue().get(key);
            if (deltaStr == null) {
                continue;
            }
            long delta = Long.parseLong(deltaStr);
            if (delta <= 0) {
                continue;
            }
            // 原子扣减已累计的计数，避免与并发 INCR 冲突
            stringRedisTemplate.opsForValue().increment(key, -delta);
            Long articleId = Long.valueOf(key.substring(VIEW_KEY_PREFIX.length()));
            articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                    .eq(Article::getId, articleId)
                    .setSql("view_count = view_count + {0}", delta));
            evictDetailCache(articleId);
        }
        // 浏览量变化影响热点排名
        evictHotCache();
    }

    // ---------------- 私有方法 ----------------

    private Article requireOwnedArticle(Long articleId, Long userId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!article.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        return article;
    }

    private ArticleDetailVO buildDetail(Article article) {
        ArticleDetailVO vo = new ArticleDetailVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setContent(article.getContent());
        vo.setSummary(article.getSummary());
        vo.setStatus(article.getStatus());
        vo.setViewCount(article.getViewCount());
        vo.setLikeCount(article.getLikeCount());
        vo.setFavoriteCount(article.getFavoriteCount());
        vo.setCommentCount(article.getCommentCount());
        vo.setCreateTime(article.getCreateTime());
        vo.setUpdateTime(article.getUpdateTime());

        User author = userMapper.selectById(article.getUserId());
        if (author != null) {
            vo.setAuthor(UserVO.from(author));
        }
        if (article.getCategoryId() != null) {
            Category category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                vo.setCategory(CategoryVO.from(category));
            }
        }
        List<Long> tagIds = articleTagMapper.selectList(
                        new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, article.getId()))
                .stream().map(ArticleTag::getTagId).toList();
        if (!tagIds.isEmpty()) {
            vo.setTags(tagMapper.selectBatchIds(tagIds).stream().map(TagVO::from).toList());
        }
        return vo;
    }

    private void fillAuthorAndCategory(List<ArticleListItemVO> records) {
        Set<Long> authorIds = records.stream().map(ArticleListItemVO::getAuthorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> categoryIds = records.stream().map(ArticleListItemVO::getCategoryId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> userMap = authorIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(authorIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Category> categoryMap = categoryIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(categoryIds).stream().collect(Collectors.toMap(Category::getId, c -> c));
        for (ArticleListItemVO vo : records) {
            User author = vo.getAuthorId() != null ? userMap.get(vo.getAuthorId()) : null;
            if (author != null) {
                vo.setAuthorName(StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername());
            }
            Category category = vo.getCategoryId() != null ? categoryMap.get(vo.getCategoryId()) : null;
            if (category != null) {
                vo.setCategoryName(category.getName());
            }
        }
    }

    private void bindTags(Long articleId, List<String> tagNames) {
        List<String> names = tagNames.stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
        for (String name : names) {
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
            if (tag == null) {
                tag = new Tag();
                tag.setName(name);
                tag.setArticleCount(0);
                tagMapper.insert(tag);
            }
            ArticleTag relation = new ArticleTag();
            relation.setArticleId(articleId);
            relation.setTagId(tag.getId());
            articleTagMapper.insert(relation);
            tag.setArticleCount(tag.getArticleCount() + 1);
            tagMapper.updateById(tag);
        }
    }

    private void unbindAllTags(Long articleId) {
        List<ArticleTag> relations = articleTagMapper.selectList(
                new LambdaQueryWrapper<ArticleTag>().eq(ArticleTag::getArticleId, articleId));
        for (ArticleTag relation : relations) {
            Tag tag = tagMapper.selectById(relation.getTagId());
            if (tag != null && tag.getArticleCount() != null && tag.getArticleCount() > 0) {
                tag.setArticleCount(tag.getArticleCount() - 1);
                tagMapper.updateById(tag);
            }
            articleTagMapper.deleteById(relation.getId());
        }
    }

    private void incrementView(Long articleId) {
        stringRedisTemplate.opsForValue().increment(VIEW_KEY_PREFIX + articleId);
    }

    /**
     * 使文章详情缓存失效（供评论/点赞/收藏等计数变化时调用）
     */
    public void evictDetailCache(Long articleId) {
        redisTemplate.delete(DETAIL_KEY_PREFIX + articleId);
    }

    // ponytail: 开发环境用 KEYS 全量匹配清理列表缓存；生产量大时应改为 SCAN 或版本号方式
    private void evictListCache() {
        Set<String> keys = redisTemplate.keys(LIST_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private Page<ArticleListItemVO> toPage(CachedList cl) {
        Page<ArticleListItemVO> page = new Page<>(cl.getCurrent(), cl.getSize(), cl.getTotal());
        page.setRecords(cl.getRecords());
        return page;
    }

    /**
     * 安全读取缓存：反序列化失败（如旧格式脏数据）时清除该 key 并按未命中处理，避免 500
     */
    private Object readCacheSafe(String cacheKey) {
        try {
            return redisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            log.warn("缓存读取失败，清除脏数据: key={}, 原因={}", cacheKey, e.getMessage());
            redisTemplate.delete(cacheKey);
            return null;
        }
    }

    private ArticleDetailVO readCachedDetail(String cacheKey) {
        Object cached = readCacheSafe(cacheKey);
        if (cached instanceof ArticleDetailVO vo) {
            return vo;
        }
        // 缓存中存在非详情值（空值标记）→ 视为不存在
        if (Boolean.TRUE.equals(redisTemplate.hasKey(cacheKey))) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return null;
    }

    private boolean tryLock(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue().setIfAbsent(key, "1", LOCK_TTL));
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void evictHotCache() {
        Set<String> keys = redisTemplate.keys(HOT_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    private String md5(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 算法不可用", e);
        }
    }
}
