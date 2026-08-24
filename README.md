# 技术博客 / 社区系统 (tech-blog)

一个用于入门打基础的完整项目：基于 **SpringBoot + MyBatis-Plus + Redis + RabbitMQ + MySQL** 的技术博客与社区系统。

## 项目定位

- **难度**：★★☆（入门第一个完整项目）
- **目标**：通过一个完整的博客/社区业务闭环，熟悉主流 Java 后端技术栈的组合用法
- **当前状态**：🛠 开发中（阶段5 缓存与队列深化已完成）

## 技术栈

| 技术 | 使用场景 |
| ---- | ---- |
| SpringBoot 3.3 | 应用框架、Web 层、SpringSecurity 集成 |
| MyBatis-Plus 3.5 | 分页插件、逻辑删除、代码生成器、条件构造器 |
| Redis | 缓存文章详情/列表、点赞 Set、浏览量统计、登录 Token、热点文章缓存 |
| RabbitMQ | 发布文章后异步刷新缓存、异步发送邮件/站内信通知、评论审核延时队列 |
| MySQL 8 | 文章、用户、评论等核心表设计，索引优化 |
| SpringSecurity + JWT | 登录认证与接口鉴权 |

## 功能模块

- 用户注册登录（JWT + SpringSecurity）
- 文章发布、编辑、Markdown 渲染
- 分类、标签、评论、点赞、收藏、关注
- 个人中心、消息通知

## 开发进度

| 阶段 | 内容 | 状态 |
| ---- | ---- | ---- |
| 阶段 0 | 环境与工程搭建（骨架、统一响应/异常、MyBatis-Plus/Redis/Security 配置、建库建表） | ✅ 完成 |
| 阶段 1 | 用户模块（JWT + SpringSecurity 注册登录、个人中心） | ✅ 完成 |
| 阶段 2 | 文章模块（CRUD、Markdown、分类标签、列表/详情缓存） | ✅ 完成 |
| 阶段 3 | 互动模块（评论、点赞、收藏、关注） | ✅ 完成 |
| 阶段 4 | 消息通知（站内信、邮件、RabbitMQ） | ✅ 完成（站内信已接入 RabbitMQ，邮件可选未启用） |
| 阶段 5 | 缓存与队列深化（热点缓存、一致性、延时队列） | ✅ 完成 |
| 阶段 6 | 测试与部署（Docker Compose、部署文档） | ⬜ 待开始 |

## 本地运行

前置要求：JDK 17+、Maven 3.8+、MySQL 8、Redis（RabbitMQ 阶段4起需要）。

```bash
# 1. 初始化数据库（会创建 tech_blog 库与全部表）
mysql -uroot -p < sql/schema.sql

# 2. 复制本地配置模板并填入数据库/Redis 密码
cp src/main/resources/application-local.example.yml src/main/resources/application-local.yml

# 3. 启动
mvn spring-boot:run

# 4. 验证
curl http://localhost:8080/api/health
# => {"code":200,"message":"操作成功","data":"ok"}
```

> 本地敏感配置（数据库密码等）放在 `application-local.yml`（已被 `.gitignore` 忽略），不会提交到仓库。

### 前端（Vue3）

前置要求：Node.js 18+（开发时经 Vite 代理访问后端，无需处理跨域）。

```bash
# 1. 安装依赖
cd web
npm install

# 2. 启动开发服务器（http://localhost:5173，/api 自动代理到 8080）
npm run dev

# 3. 生产构建
npm run build
```

> 前端需求设计与开发计划见 [web/docs/需求设计.md](web/docs/需求设计.md) 与 [web/docs/开发计划.md](web/docs/开发计划.md)

## 文档

- [需求文档](docs/需求文档.md)
- [开发计划](docs/开发计划.md)

## 工程结构

```
tech-blog
├── docs/                    # 后端需求文档、开发计划
├── sql/schema.sql           # 建库建表脚本（tech_blog 库，10 张核心表）
├── pom.xml
├── web/                     # 前端（Vue3 + Vite + TS，前后端分离）
│   ├── docs/                # 前端需求设计、开发计划
│   └── src/                 # api / stores / router / layouts / views
└── src
    ├── main/java/com/blog
    │   ├── BlogApplication.java
    │   ├── common/          # 统一响应 Result、状态码、业务异常、全局异常处理
    │   ├── config/          # Security / JWT / MyBatis-Plus / Redis / Jackson / 字段自动填充
    │   ├── controller/      # 接口层（Auth / User / Health）
    │   ├── dto/             # 请求对象（含参数校验）
    │   ├── entity/          # 数据库实体（BaseEntity / User / Article / Favorite / Follow）
    │   ├── mapper/          # MyBatis-Plus Mapper
    │   ├── security/        # JWT 过滤器、当前登录用户、SecurityUtils
    │   ├── service/         # 业务层（UserService）
    │   ├── util/            # 工具（JwtUtil）
    │   └── vo/              # 视图对象
    ├── main/resources/      # application.yml、application-local.yml、mapper xml
    └── test/                # 单元/集成测试
```
