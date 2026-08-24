package com.blog;

import com.blog.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 阶段5 缓存深化测试：空值缓存防穿透、热点 TopN、缓存一致性
 *
 * @author Liangkunrui
 */
@SpringBootTest
@AutoConfigureMockMvc
class CacheConsistencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ArticleService articleService;

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

    private long createArticle(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", title,
                                "content", "内容"))))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    @Test
    void cachePenetrationHotListAndConsistency() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String token = registerAndLogin("cache_" + suffix);

        long article1 = createArticle(token, "热点文章A " + suffix);
        long article2 = createArticle(token, "热点文章B " + suffix);

        // 制造浏览量：A 访问 2 次，B 访问 1 次（详情接口内部 INCR）
        mockMvc.perform(get("/api/articles/" + article1)).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/articles/" + article1)).andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/articles/" + article2)).andExpect(jsonPath("$.code").value(200));

        // 触发浏览量落库（同时失效热点缓存）
        articleService.flushViewCounts();

        // 热点 TopN：浏览量高的 A 应排在 B 前面（允许历史测试数据存在，只断言相对顺序）
        MvcResult hotResult = mockMvc.perform(get("/api/articles/hot?topN=50"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        com.fasterxml.jackson.databind.JsonNode records =
                objectMapper.readTree(hotResult.getResponse().getContentAsString()).path("data");
        int idxA = -1;
        int idxB = -1;
        for (int i = 0; i < records.size(); i++) {
            long id = records.get(i).path("id").asLong();
            if (id == article1) {
                idxA = i;
            }
            if (id == article2) {
                idxB = i;
            }
        }
        org.junit.jupiter.api.Assertions.assertTrue(idxA >= 0 && idxB >= 0, "两篇文章都应出现在热点列表");
        org.junit.jupiter.api.Assertions.assertTrue(idxA < idxB, "浏览量更高的文章应排在热点列表前面");

        // 空值缓存防穿透：访问不存在的文章 → 404 且写入空值标记
        long fakeId = 999999999999999999L;
        mockMvc.perform(get("/api/articles/" + fakeId))
                .andExpect(jsonPath("$.code").value(404));
        org.junit.jupiter.api.Assertions.assertTrue(
                redisTemplate.hasKey("blog:article:detail:" + fakeId),
                "不存在的文章应写入空值缓存标记（防穿透）");
        // 再次访问同样 404（命中空值缓存，不再查询 DB）
        mockMvc.perform(get("/api/articles/" + fakeId))
                .andExpect(jsonPath("$.code").value(404));

        // 缓存一致性：编辑标题后详情立即返回新标题（写路径主动失效缓存）
        mockMvc.perform(put("/api/articles/" + article1)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "热点文章A-修订 " + suffix))))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/articles/" + article1))
                .andExpect(jsonPath("$.data.title").value("热点文章A-修订 " + suffix));
    }
}
