package com.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.ArticleLike;
import com.blog.mapper.ArticleLikeMapper;
import com.blog.service.InteractionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 互动模块集成测试：评论、点赞（Redis Set + 回写）、收藏、关注
 *
 * @author Liangkunrui
 */
@SpringBootTest
@AutoConfigureMockMvc
class InteractionFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private ArticleLikeMapper articleLikeMapper;

    private String registerAndLogin(String username) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", username, "password", "123456"))))
                .andExpect(jsonPath("$.code").value(200));
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "username", username, "password", "123456"))))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("token").asText();
    }

    private long createArticle(String token, long categoryId, String suffix) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "互动测试文章 " + suffix,
                                "content", "内容",
                                "categoryId", categoryId))))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    void interactionFullFlow() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String tokenA = registerAndLogin("ia_" + suffix);
        String tokenB = registerAndLogin("iu_" + suffix);

        // A 创建分类 + 文章
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("name", "互动分类 " + suffix))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        long articleId = createArticle(tokenA, categoryId, suffix);

        // 1. 点赞：B 点赞 → status true；重复点赞幂等；取消 → false；再点赞
        mockMvc.perform(post("/api/articles/" + articleId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/articles/" + articleId + "/like/status")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(delete("/api/articles/" + articleId + "/like")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/articles/" + articleId + "/like/status")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data").value(false));
        mockMvc.perform(post("/api/articles/" + articleId + "/like")
                .header("Authorization", "Bearer " + tokenB));

        // 2. 收藏：收藏 → status true；个人中心可见；取消 → false
        mockMvc.perform(post("/api/articles/" + articleId + "/favorite")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/articles/" + articleId + "/favorite/status")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/api/users/me/favorites")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].articleId").value(articleId));
        mockMvc.perform(delete("/api/articles/" + articleId + "/favorite")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/articles/" + articleId + "/favorite/status")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data").value(false));

        // 3. 关注：B 关注 A → status true；B 的关注列表 / A 的粉丝列表
        MvcResult meResult = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long authorId = objectMapper.readTree(meResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/users/" + authorId + "/follow")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/users/" + authorId + "/follow/status")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data").value(true));
        mockMvc.perform(get("/api/users/me/following")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].userId").value(authorId));
        mockMvc.perform(get("/api/users/me/followers")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data.total").value(1));
        // 不能关注自己
        mockMvc.perform(post("/api/users/" + authorId + "/follow")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(400));
        mockMvc.perform(delete("/api/users/" + authorId + "/follow")
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(200));

        // 4. 评论：B 评论 → 公开列表可见；详情 commentCount=1
        mockMvc.perform(post("/api/articles/" + articleId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "content", "写得很棒！"))))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content").value("写得很棒！"));
        mockMvc.perform(get("/api/articles/" + articleId + "/comments"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].username").value("iu_" + suffix));
        mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(jsonPath("$.data.commentCount").value(1));

        // 5. 点赞定时回写：手动触发 flush，验证 article_like 表与文章点赞数
        interactionService.flushLikeCounts();
        long likeRows = articleLikeMapper.selectCount(new LambdaQueryWrapper<ArticleLike>()
                .eq(ArticleLike::getArticleId, articleId));
        org.junit.jupiter.api.Assertions.assertEquals(1, likeRows, "article_like 表应回写 1 条点赞记录");
        mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        // 6. 未登录访问互动接口 → 401
        mockMvc.perform(get("/api/articles/" + articleId + "/like/status"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/articles/" + articleId + "/like"))
                .andExpect(status().isUnauthorized());
    }
}
