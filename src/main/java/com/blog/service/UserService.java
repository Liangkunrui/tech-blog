package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BusinessException;
import com.blog.common.ResultCode;
import com.blog.dto.LoginRequest;
import com.blog.dto.RegisterRequest;
import com.blog.dto.UpdatePasswordRequest;
import com.blog.dto.UpdateProfileRequest;
import com.blog.entity.Article;
import com.blog.entity.Favorite;
import com.blog.entity.Follow;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.FavoriteMapper;
import com.blog.mapper.FollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.util.JwtUtil;
import com.blog.vo.ArticleListItemVO;
import com.blog.vo.FavoriteVO;
import com.blog.vo.FollowVO;
import com.blog.vo.LoginVO;
import com.blog.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用户服务：注册、登录、资料维护、个人中心
 *
 * @author Liangkunrui
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final FavoriteMapper favoriteMapper;
    private final FollowMapper followMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 注册：用户名唯一校验 + BCrypt 加密存储
     */
    @Transactional
    public UserVO register(RegisterRequest request) {
        Long count = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setBio(request.getBio());
        user.setStatus(1);
        userMapper.insert(user);
        return UserVO.from(user);
    }

    /**
     * 登录：校验密码 + 签发 JWT
     */
    public LoginVO login(LoginRequest request) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BusinessException(400, "用户名或密码错误");
        }
        if (Integer.valueOf(0).equals(user.getStatus())) {
            throw new BusinessException(403, "账号已被禁用");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        return LoginVO.of(token, UserVO.from(user));
    }

    /**
     * 按ID查询用户
     */
    public UserVO getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return UserVO.from(user);
    }

    /**
     * 修改个人信息（仅更新非空字段）
     */
    public UserVO updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        userMapper.updateById(user);
        return UserVO.from(user);
    }

    /**
     * 修改密码：校验原密码后更新
     */
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(400, "原密码错误");
        }
        User update = new User();
        update.setId(userId);
        update.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userMapper.updateById(update);
    }

    /**
     * 个人中心：我的文章
     */
    public IPage<ArticleListItemVO> pageMyArticles(Long userId, long pageNum, long pageSize) {
        Page<Article> page = articleMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getUserId, userId)
                        .orderByDesc(Article::getCreateTime));
        return page.convert(ArticleListItemVO::from);
    }

    /**
     * 个人中心：我的收藏
     */
    public IPage<FavoriteVO> pageMyFavorites(Long userId, long pageNum, long pageSize) {
        Page<Favorite> page = favoriteMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Favorite>()
                        .eq(Favorite::getUserId, userId)
                        .orderByDesc(Favorite::getCreateTime));
        List<Long> articleIds = page.getRecords().stream().map(Favorite::getArticleId).toList();
        Map<Long, Article> articleMap = articleIds.isEmpty() ? Map.of()
                : articleMapper.selectBatchIds(articleIds).stream()
                        .collect(Collectors.toMap(Article::getId, article -> article));
        return page.convert(favorite -> {
            Article article = articleMap.get(favorite.getArticleId());
            return new FavoriteVO(favorite.getArticleId(),
                    article != null ? article.getTitle() : "文章已删除", favorite.getCreateTime());
        });
    }

    /**
     * 个人中心：我的关注（我关注了谁）
     */
    public IPage<FollowVO> pageMyFollowing(Long userId, long pageNum, long pageSize) {
        Page<Follow> page = followMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, userId)
                        .orderByDesc(Follow::getCreateTime));
        return toFollowVOPage(page, Follow::getUserId);
    }

    /**
     * 个人中心：我的粉丝
     */
    public IPage<FollowVO> pageMyFollowers(Long userId, long pageNum, long pageSize) {
        Page<Follow> page = followMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getUserId, userId)
                        .orderByDesc(Follow::getCreateTime));
        return toFollowVOPage(page, Follow::getFollowerId);
    }

    private IPage<FollowVO> toFollowVOPage(Page<Follow> page, Function<Follow, Long> targetUserIdExtractor) {
        List<Long> userIds = page.getRecords().stream().map(targetUserIdExtractor).toList();
        Map<Long, User> userMap = userIds.isEmpty() ? Map.of()
                : userMapper.selectBatchIds(userIds).stream()
                        .collect(Collectors.toMap(User::getId, user -> user));
        return page.convert(follow -> {
            User user = userMap.get(targetUserIdExtractor.apply(follow));
            return user != null
                    ? new FollowVO(user.getId(), user.getUsername(), user.getNickname(), user.getAvatar(),
                    follow.getCreateTime())
                    : null;
        });
    }
}
