package com.blog;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.entity.Notification;
import com.blog.mapper.NotificationMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * 通知模块集成测试：互动事件 → RabbitMQ 异步生成站内信 → 未读/已读
 *
 * @author Liangkunrui
 */
@SpringBootTest
@AutoConfigureMockMvc
class NotificationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NotificationMapper notificationMapper;

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
    void notificationFlow() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String tokenA = registerAndLogin("na_" + suffix);
        String tokenB = registerAndLogin("nb_" + suffix);

        // A 创建分类 + 文章
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("name", "通知分类 " + suffix))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        MvcResult articleResult = mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "通知测试文章 " + suffix,
                                "content", "内容",
                                "categoryId", categoryId))))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long articleId = objectMapper.readTree(articleResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // B 关注 A → 触发 FOLLOW 通知
        MvcResult meResult = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn();
        long authorId = objectMapper.readTree(meResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        mockMvc.perform(post("/api/users/" + authorId + "/follow")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(200));

        // B 点赞 + 评论 → 触发 LIKE / COMMENT 通知
        mockMvc.perform(post("/api/articles/" + articleId + "/like")
                .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/articles/" + articleId + "/comments")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("content", "异步通知测试"))))
                .andExpect(jsonPath("$.code").value(200));

        // 等待消费者异步落库（A 应收到 FOLLOW + LIKE + COMMENT 至少 3 条）
        Awaitility.await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, authorId)) >= 3);

        // 通知列表 + 未读数
        mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.greaterThanOrEqualTo(3)));

        // 标记一条已读 → 未读数减少
        MvcResult listResult = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + tokenA))
                .andReturn();
        long firstId = objectMapper.readTree(listResult.getResponse().getContentAsString())
                .path("data").path("records").get(0).path("id").asLong();
        mockMvc.perform(put("/api/notifications/" + firstId + "/read")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        // 全部已读 → 未读数 0
        mockMvc.perform(put("/api/notifications/read-all")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/notifications/unread-count")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.data").value(0));

        // 粉丝广播：B 已关注 A，A 再发一篇 → B 收到系统通知
        mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "第二篇文章 " + suffix,
                                "content", "内容",
                                "categoryId", categoryId))))
                .andExpect(jsonPath("$.code").value(200));
        MvcResult meB = mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + tokenB))
                .andReturn();
        long userIdB = objectMapper.readTree(meB.getResponse().getContentAsString())
                .path("data").path("id").asLong();
        Awaitility.await().atMost(15, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                        .eq(Notification::getUserId, userIdB)
                        .eq(Notification::getType, 5)) >= 1);
    }
}
