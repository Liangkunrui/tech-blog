package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
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
@Service
@RequiredArgsConstructor
public class ArticleService {

    private static final String DETAIL_KEY_PREFIX = "blog:article:detail:";
    private static final String LIST_KEY_PREFIX = "blog:article:list:";
    private static final String VIEW_KEY_PREFIX = "blog:article:view:";
    private static final Duration DETAIL_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration LIST_CACHE_TTL = Duration.ofMinutes(5);

    private final ArticleMapper articleMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final UserMapper userMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 列表缓存内容（分页数据快照）
     */
    public record CachedList(List<ArticleListItemVO> records, long total, long current, long size) {
    }

    // ---------------- 查询 ----------------

    /**
     * 已发布文章分页列表：支持分类/标签/关键词筛选，按最新或热度排序
     */
    public IPage<ArticleListItemVO> pageArticles(long pageNum, long pageSize, Long categoryId,
                                                 Long tagId, String keyword, String sort) {
        String cacheKey = LIST_KEY_PREFIX + md5(categoryId + "|" + tagId + "|" + keyword + "|" + sort + "|" + pageNum + "|" + pageSize);
        Object cached = redisTemplate.opsForValue().get(cacheKey);
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
     */
    public ArticleDetailVO getDetail(Long id) {
        String cacheKey = DETAIL_KEY_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        ArticleDetailVO vo;
        if (cached instanceof ArticleDetailVO detail) {
            vo = detail;
        } else {
            Article article = articleMapper.selectById(id);
            if (article == null || !Integer.valueOf(1).equals(article.getStatus())) {
                throw new BusinessException(ResultCode.NOT_FOUND);
            }
            vo = buildDetail(article);
            redisTemplate.opsForValue().set(cacheKey, vo, DETAIL_CACHE_TTL);
        }
        incrementView(id);
        return vo;
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
        evictListCache();
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
            User author = userMap.get(vo.getAuthorId());
            if (author != null) {
                vo.setAuthorName(StringUtils.hasText(author.getNickname()) ? author.getNickname() : author.getUsername());
            }
            Category category = categoryMap.get(vo.getCategoryId());
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
        Page<ArticleListItemVO> page = new Page<>(cl.current(), cl.size(), cl.total());
        page.setRecords(cl.records());
        return page;
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
