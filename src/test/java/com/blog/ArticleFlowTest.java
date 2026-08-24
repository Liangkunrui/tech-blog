package com.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * 文章模块集成测试：分类 → 发文 → 公开浏览 → 编辑/删除权限 → 标签关联
 *
 * @author Liangkunrui
 */
@SpringBootTest
@AutoConfigureMockMvc
class ArticleFlowTest {

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
    void articleFullFlow() throws Exception {
        String suffix = String.valueOf(System.currentTimeMillis());
        String tokenA = registerAndLogin("author_" + suffix);
        String tokenB = registerAndLogin("other_" + suffix);

        // 1. 创建分类
        MvcResult categoryResult = mockMvc.perform(post("/api/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .param("name", "Java " + suffix)
                        .param("sort", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn();
        long categoryId = objectMapper.readTree(categoryResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 2. 发布文章（带标签）
        MvcResult createResult = mockMvc.perform(post("/api/articles")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "SpringBoot 入门 " + suffix,
                                "content", "# 标题\n\nMarkdown 内容",
                                "summary", "入门教程",
                                "categoryId", categoryId,
                                "tags", List.of("Spring", "JWT")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.tags[0].name").exists())
                .andReturn();
        long articleId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 3. 公开列表（未登录可见）—— 用唯一关键词检索确认新文章在列表中
        mockMvc.perform(get("/api/articles").param("keyword", suffix))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.records[*].title",
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString(suffix))));

        // 4. 分类列表含文章数
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].articleCount",
                        org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.greaterThanOrEqualTo(1))));

        // 5. 公开详情（未登录，含作者/分类/标签）
        mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.author.username").value("author_" + suffix))
                .andExpect(jsonPath("$.data.category.name").value("Java " + suffix))
                .andExpect(jsonPath("$.data.tags.length()").value(2));

        // 6. 他人无权编辑/删除 → 403
        mockMvc.perform(put("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of("title", "越权修改"))))
                .andExpect(jsonPath("$.code").value(403));
        mockMvc.perform(delete("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(jsonPath("$.code").value(403));

        // 7. 作者编辑成功（改标题 + 替换标签）
        mockMvc.perform(put("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "SpringBoot 进阶 " + suffix,
                                "tags", List.of("Spring", "MyBatis-Plus")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("SpringBoot 进阶 " + suffix))
                .andExpect(jsonPath("$.data.tags.length()").value(2));

        // 8. 作者删除 → 详情 404（逻辑删除）
        mockMvc.perform(delete("/api/articles/" + articleId)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/articles/" + articleId))
                .andExpect(jsonPath("$.code").value(404));

        // 9. 未登录不能发文 → 401
        mockMvc.perform(post("/api/articles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(Map.of(
                                "title", "无权限", "content", "x"))))
                .andExpect(status().isUnauthorized());
    }
}
