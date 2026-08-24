package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Tag;
import com.blog.mapper.TagMapper;
import com.blog.vo.TagVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 标签服务
 *
 * @author Liangkunrui
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    /**
     * 热门标签列表（按文章数倒序）
     */
    public List<TagVO> listHot() {
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                        .orderByDesc(Tag::getArticleCount)
                        .orderByAsc(Tag::getId))
                .stream()
                .map(TagVO::from)
                .toList();
    }
}
