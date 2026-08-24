-- ============================================================
-- 技术博客/社区系统 数据库初始化脚本
-- 版本: v0.1  对应需求文档 6.1 核心表设计
-- 执行方式: mysql -uroot -p < sql/schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS `tech_blog` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `tech_blog`;

-- 专用数据库账号（本地开发用，生产环境请另行规划）
CREATE USER IF NOT EXISTS 'blog'@'localhost' IDENTIFIED BY 'blog123456';
GRANT ALL PRIVILEGES ON `tech_blog`.* TO 'blog'@'localhost';
FLUSH PRIVILEGES;

-- ------------------------------------------------------------
-- 1. 用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名（唯一）',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密）',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像地址',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `bio`         VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1正常 0禁用',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    KEY `idx_create_time` (`create_time`)
) ENGINE = InnoDB COMMENT = '用户表';

-- ------------------------------------------------------------
-- 2. 文章表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `article`;
CREATE TABLE `article` (
    `id`             BIGINT       NOT NULL COMMENT '主键',
    `user_id`        BIGINT       NOT NULL COMMENT '作者ID',
    `category_id`    BIGINT       DEFAULT NULL COMMENT '分类ID',
    `title`          VARCHAR(100) NOT NULL COMMENT '标题',
    `content`        LONGTEXT     NOT NULL COMMENT 'Markdown 内容',
    `summary`        VARCHAR(255) DEFAULT NULL COMMENT '摘要',
    `status`         TINYINT      NOT NULL DEFAULT 0 COMMENT '状态: 0草稿 1已发布',
    `view_count`     INT          NOT NULL DEFAULT 0 COMMENT '浏览量',
    `like_count`     INT          NOT NULL DEFAULT 0 COMMENT '点赞数',
    `favorite_count` INT          NOT NULL DEFAULT 0 COMMENT '收藏数',
    `comment_count`  INT          NOT NULL DEFAULT 0 COMMENT '评论数',
    `deleted`        TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删 1已删',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_category_id` (`category_id`),
    KEY `idx_status_create_time` (`status`, `create_time`)
) ENGINE = InnoDB COMMENT = '文章表';

-- ------------------------------------------------------------
-- 3. 分类表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
    `id`          BIGINT      NOT NULL COMMENT '主键',
    `name`        VARCHAR(50) NOT NULL COMMENT '分类名称',
    `sort`        INT         NOT NULL DEFAULT 0 COMMENT '排序值（越小越靠前）',
    `create_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB COMMENT = '分类表';

-- ------------------------------------------------------------
-- 4. 标签表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `tag`;
CREATE TABLE `tag` (
    `id`            BIGINT      NOT NULL COMMENT '主键',
    `name`          VARCHAR(50) NOT NULL COMMENT '标签名称',
    `article_count` INT         NOT NULL DEFAULT 0 COMMENT '文章数（冗余计数）',
    `create_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_name` (`name`)
) ENGINE = InnoDB COMMENT = '标签表';

-- ------------------------------------------------------------
-- 5. 文章-标签关联表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `article_tag`;
CREATE TABLE `article_tag` (
    `id`          BIGINT   NOT NULL COMMENT '主键',
    `article_id`  BIGINT   NOT NULL COMMENT '文章ID',
    `tag_id`      BIGINT   NOT NULL COMMENT '标签ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_tag` (`article_id`, `tag_id`),
    KEY `idx_tag_id` (`tag_id`)
) ENGINE = InnoDB COMMENT = '文章-标签关联表';

-- ------------------------------------------------------------
-- 6. 评论表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `article_id`  BIGINT       NOT NULL COMMENT '文章ID',
    `user_id`     BIGINT       NOT NULL COMMENT '评论人ID',
    `parent_id`   BIGINT       NOT NULL DEFAULT 0 COMMENT '父评论ID（0为顶级评论）',
    `content`     VARCHAR(2000) NOT NULL COMMENT '评论内容',
    `status`      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态: 1通过 0待审核',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_article_status` (`article_id`, `status`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '评论表';

-- ------------------------------------------------------------
-- 7. 点赞表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `article_like`;
CREATE TABLE `article_like` (
    `id`          BIGINT   NOT NULL COMMENT '主键',
    `article_id`  BIGINT   NOT NULL COMMENT '文章ID',
    `user_id`     BIGINT   NOT NULL COMMENT '点赞用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '点赞表';

-- ------------------------------------------------------------
-- 8. 收藏表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `favorite`;
CREATE TABLE `favorite` (
    `id`          BIGINT   NOT NULL COMMENT '主键',
    `article_id`  BIGINT   NOT NULL COMMENT '文章ID',
    `user_id`     BIGINT   NOT NULL COMMENT '收藏用户ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_article_user` (`article_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE = InnoDB COMMENT = '收藏表';

-- ------------------------------------------------------------
-- 9. 关注表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `follow`;
CREATE TABLE `follow` (
    `id`          BIGINT   NOT NULL COMMENT '主键',
    `user_id`     BIGINT   NOT NULL COMMENT '被关注者ID',
    `follower_id` BIGINT   NOT NULL COMMENT '粉丝ID',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_follower` (`user_id`, `follower_id`),
    KEY `idx_follower_id` (`follower_id`)
) ENGINE = InnoDB COMMENT = '关注表';

-- ------------------------------------------------------------
-- 10. 通知表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `notification`;
CREATE TABLE `notification` (
    `id`          BIGINT       NOT NULL COMMENT '主键',
    `user_id`     BIGINT       NOT NULL COMMENT '接收者ID',
    `from_user_id` BIGINT      DEFAULT NULL COMMENT '触发者ID（系统通知为空）',
    `type`        TINYINT      NOT NULL COMMENT '类型: 1评论 2点赞 3收藏 4关注 5系统',
    `target_id`   BIGINT       DEFAULT NULL COMMENT '目标ID（如文章ID）',
    `content`     VARCHAR(500) DEFAULT NULL COMMENT '通知内容',
    `is_read`     TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已读: 0未读 1已读',
    `deleted`     TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删 1已删',
    `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_read` (`user_id`, `is_read`)
) ENGINE = InnoDB COMMENT = '通知表';
