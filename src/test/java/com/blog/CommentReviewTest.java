package com.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 阶段5 评论审核延时队列测试：敏感词评论进入待审核，TTL 到期后自动放行
 *
 * @author Liangkunrui
 */
@SpringBootTest
@AutoConfigureMockMvc
class CommentReviewTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Test
    void sensitiveCommentAutoReleasedAfterDelay() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String tokenA = registerAndLogin("ra_" + suffix);
        String tokenB = registerAndLogin("rb_" + suffix);

        MvcResult articleResult = mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "审核测试文章 " + suffix,
                                "content", "内容"))))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long articleId = objectMapper.readTree(articleResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 1. 正常评论：立即可见
        mockMvc.perform(post("/api/articles/" + articleId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("content", "普通评论"))))
                .andExpect(jsonPath("$.data.status").value(1));

        // 2. 敏感词评论：进入待审核（status=0），评论列表不显示
        mockMvc.perform(post("/api/articles/" + articleId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("content", "这里有广告信息"))))
                .andExpect(jsonPath("$.data.status").value(0));
        mockMvc.perform(get("/api/articles/" + articleId + "/comments"))
                .andExpect(jsonPath("$.data.total").value(1));

        // 3. 等待延时队列到期（默认10s + 余量），评论被自动放行 → 列表 total=2、评论数联动
        Awaitility.await().atMost(25, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> mockMvc.perform(get("/api/articles/" + articleId + "/comments"))
                        .andExpect(jsonPath("$.data.total").value(2))
                        .andExpect(jsonPath("$.data.records[1].content").value("这里有广告信息")));
        mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(jsonPath("$.data.commentCount").value(2));
    }
}
