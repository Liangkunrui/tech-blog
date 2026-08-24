# 技术博客 / 社区系统 (tech-blog)

一个用于入门打基础的完整项目：基于 **SpringBoot + MyBatis-Plus + Redis + RabbitMQ + MySQL** 的技术博客与社区系统。

## 项目定位

- **难度**：★★☆（入门第一个完整项目）
- **目标**：通过一个完整的博客/社区业务闭环，熟悉主流 Java 后端技术栈的组合用法
- **当前状态**：📋 规划阶段（仅需求文档与开发计划，尚未开始编码）

## 技术栈

| 技术 | 使用场景 |
| ---- | ---- |
| SpringBoot | 应用框架、Web 层、SpringSecurity 集成 |
| MyBatis-Plus | 分页插件、逻辑删除、代码生成器、条件构造器 |
| Redis | 缓存文章详情/列表、点赞 Set、浏览量统计、登录 Token、热点文章缓存 |
| RabbitMQ | 发布文章后异步刷新缓存、异步发送邮件/站内信通知、评论审核延时队列 |
| MySQL | 文章、用户、评论等核心表设计，索引优化 |
| JWT | 无状态登录认证 |

## 功能模块

- 用户注册登录（JWT + SpringSecurity）
- 文章发布、编辑、Markdown 渲染
- 分类、标签、评论、点赞、收藏、关注
- 个人中心、消息通知

## 文档

- [需求文档](docs/需求文档.md)
- [开发计划](docs/开发计划.md)

## 目录规划（预留）

```
tech-blog
├── docs/                  # 需求文档、开发计划
└── src/                   # （规划中）SpringBoot 工程
    ├── main/java/...      # controller / service / mapper / entity / dto / vo / config / common
    ├── main/resources/    # application.yml、mapper xml、sql 脚本
    └── test/              # 单元测试
```
