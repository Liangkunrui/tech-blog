package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.common.BusinessException;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.vo.CategoryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 分类服务
 *
 * @author Liangkunrui
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    /**
     * 分类列表（含各分类下已发布文章数）
     */
    public List<CategoryVO> listWithArticleCount() {
        List<Category> categories = categoryMapper.selectList(new LambdaQueryWrapper<Category>()
                .orderByAsc(Category::getSort)
                .orderByAsc(Category::getId));
        List<Map<String, Object>> counts = articleMapper.selectMaps(new QueryWrapper<Article>()
                .select("category_id AS categoryId, COUNT(*) AS cnt")
                .eq("status", 1)
                .groupBy("category_id"));
        Map<Long, Long> countMap = counts.stream()
                .filter(m -> m.get("categoryId") != null)
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("categoryId")).longValue(),
                        m -> ((Number) m.get("cnt")).longValue()));
        return categories.stream().map(category -> {
            CategoryVO vo = CategoryVO.from(category);
            vo.setArticleCount(countMap.getOrDefault(category.getId(), 0L));
            return vo;
        }).toList();
    }

    /**
     * 创建分类
     */
    @Transactional
    public CategoryVO create(String name, Integer sort) {
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getName, name));
        if (count > 0) {
            throw new BusinessException(400, "分类已存在");
        }
        Category category = new Category();
        category.setName(name);
        category.setSort(sort == null ? 0 : sort);
        categoryMapper.insert(category);
        return CategoryVO.from(category);
    }
}
