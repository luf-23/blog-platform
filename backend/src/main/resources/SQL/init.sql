-- Blog Platform：空库一键初始化（建库 + 表结构 + 演示数据）
-- 演示账号密码均为明文 123456（BCrypt 存储）
-- 用法：mysql -u root -p < init.sql

DROP DATABASE IF EXISTS blog_platform;

CREATE DATABASE blog_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE blog_platform;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------------------------------------------------------------------------
-- 表结构
-- ---------------------------------------------------------------------------
CREATE TABLE user (
    user_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(60) NOT NULL COMMENT 'BCrypt 哈希',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    signature VARCHAR(512) COMMENT '个性签名',
    avatar_image VARCHAR(512) DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png' COMMENT '头像 URL',
    background_image VARCHAR(512) DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/background/default.jpg' COMMENT '背景图 URL',
    email VARCHAR(255) NULL UNIQUE COMMENT '邮箱',
    last_login TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(255) COMMENT '最后登录 IP',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE category (
    category_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '分类 ID',
    user_id INT NOT NULL COMMENT '用户 ID',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    category_description VARCHAR(50) COMMENT '分类描述',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_name_description (user_id, category_name, category_description)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE article (
    article_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '文章 ID',
    category_id INT NOT NULL COMMENT '分类 ID',
    title VARCHAR(50) NOT NULL COMMENT '标题',
    content LONGTEXT NOT NULL COMMENT '内容',
    status ENUM('draft', 'published', 'pending') DEFAULT 'draft' COMMENT '文章状态',
    cover_image VARCHAR(512) DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/article/background/default.jpg' COMMENT '封面 URL',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (category_id) REFERENCES category (category_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE comment (
    comment_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '评论 ID',
    article_id INT NOT NULL COMMENT '文章 ID',
    user_id INT NULL COMMENT '评论者 ID',
    parent_id INT NULL COMMENT '父评论 ID，NULL 为一级评论',
    root_id INT NULL COMMENT '根评论 ID（一级评论的 comment_id）',
    content TEXT NOT NULL COMMENT '评论内容',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE SET NULL,
    FOREIGN KEY (parent_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    INDEX idx_article_parent (article_id, parent_id),
    INDEX idx_article_root (article_id, root_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE announcement (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '公告 ID',
    title VARCHAR(50) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    type ENUM('success', 'warning', 'danger', 'info') DEFAULT 'success' COMMENT '公告类型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE article_like_record (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞 ID',
    article_id INT NOT NULL COMMENT '文章 ID',
    user_id INT NULL COMMENT '点赞者 ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE comment_like_record (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞 ID',
    comment_id INT NOT NULL COMMENT '评论 ID',
    user_id INT NULL COMMENT '点赞者 ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    FOREIGN KEY (comment_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 演示数据（密码 123456 的 BCrypt 哈希）
-- ---------------------------------------------------------------------------
SET @demo_pwd = '$2b$10$I/Me9zozCbEwd0Tlkd9SQuLOqDGpQ6yJWc5pcCIxXG8P/F222D3H2';
SET @demo_content = '这是一段用于 Blog Platform 演示环境的统一正文。平台支持 Markdown 写作、文章分类、草稿与发布流程，以及社区广场中的公开阅读体验。你可以在「发现」页浏览他人文章，在「我的博客」中创建分类并撰写新稿。管理员账号用于审核与公告管理，普通账号则适合本地开发与功能联调测试。';

INSERT INTO user (user_id, username, password, nickname, signature, email) VALUES
(100001, 'admin', @demo_pwd, '管理员', '系统管理员，负责审核与公告', 'admin@demo.local'),
(100002, 'alice', @demo_pwd, '爱丽丝', '前端与交互设计爱好者', 'alice@demo.local'),
(100003, 'bob',   @demo_pwd, '鲍勃',   'Java 后端开发', 'bob@demo.local'),
(100004, 'carol', @demo_pwd, '卡罗',   '算法与数据结构练习', 'carol@demo.local'),
(100005, 'david', @demo_pwd, '大卫',   '摄影与旅行记录', 'david@demo.local');

INSERT INTO category (category_id, user_id, category_name, category_description) VALUES
(100001, 100001, '平台动态', '站点公告与更新说明'),
(100002, 100001, '运维笔记', '部署与配置记录'),
(100003, 100002, '前端开发', 'Vue 与工程化实践'),
(100004, 100002, '读书随笔', '阅读摘录与感想'),
(100005, 100003, 'Java 后端', 'Spring Boot 相关'),
(100006, 100003, '生活杂谈', '日常随想'),
(100007, 100004, '算法练习', 'LeetCode 题解'),
(100008, 100004, '面试准备', '知识点整理'),
(100009, 100005, '旅行摄影', '旅途见闻'),
(100010, 100005, '器材评测', '相机与镜头体验');

INSERT INTO article (article_id, category_id, title, content, status) VALUES
(100001, 100001, '标题-1',  @demo_content, 'published'),
(100002, 100001, '标题-2',  @demo_content, 'published'),
(100003, 100002, '标题-3',  @demo_content, 'pending'),
(100004, 100002, '标题-4',  @demo_content, 'draft'),
(100005, 100003, '标题-5',  @demo_content, 'published'),
(100006, 100003, '标题-6',  @demo_content, 'published'),
(100007, 100003, '标题-7',  @demo_content, 'published'),
(100008, 100004, '标题-8',  @demo_content, 'published'),
(100009, 100004, '标题-9',  @demo_content, 'draft'),
(100010, 100005, '标题-10', @demo_content, 'published'),
(100011, 100005, '标题-11', @demo_content, 'published'),
(100012, 100005, '标题-12', @demo_content, 'pending'),
(100013, 100006, '标题-13', @demo_content, 'published'),
(100014, 100006, '标题-14', @demo_content, 'published'),
(100015, 100007, '标题-15', @demo_content, 'published'),
(100016, 100007, '标题-16', @demo_content, 'published'),
(100017, 100007, '标题-17', @demo_content, 'published'),
(100018, 100008, '标题-18', @demo_content, 'published'),
(100019, 100008, '标题-19', @demo_content, 'pending'),
(100020, 100009, '标题-20', @demo_content, 'published'),
(100021, 100009, '标题-21', @demo_content, 'published'),
(100022, 100009, '标题-22', @demo_content, 'published'),
(100023, 100010, '标题-23', @demo_content, 'published'),
(100024, 100010, '标题-24', @demo_content, 'draft');

-- 树形评论演示（文章 100005、100001）
INSERT INTO comment (comment_id, article_id, user_id, parent_id, root_id, content) VALUES
(100001, 100005, 100002, NULL, 100001, '写得很清晰，Vue 3 组合式 API 的例子很实用。'),
(100002, 100005, 100003, 100001, 100001, '同感，尤其是响应式那一段。'),
(100003, 100005, 100004, 100002, 100001, '响应式原理那章我也反复看了两遍。'),
(100004, 100005, 100003, NULL, 100004, '有没有配套源码仓库？想跟着敲一遍。'),
(100005, 100005, 100002, 100004, 100004, '作者在文末放了 GitHub 链接，可以自取。'),
(100006, 100005, 100005, NULL, 100006, '排版舒服，代码块高亮也顺眼。'),
(100007, 100005, 100004, 100006, 100006, '暗色模式下阅读体验也不错。'),
(100008, 100005, 100003, NULL, 100008, '期待续篇，讲讲 Pinia 和路由守卫。'),
(100009, 100005, 100002, NULL, 100009, '已收藏，周末慢慢啃。'),
(100010, 100005, 100005, 100009, 100009, '同收藏，这篇信息量挺大。'),
(100011, 100001, 100003, NULL, 100011, '平台动态这篇信息量刚好，适合新人入门。'),
(100012, 100001, 100002, 100011, 100011, '同意，公告和分类流程讲得很清楚。'),
(100013, 100001, 100004, NULL, 100013, '建议补充一下评论区的使用说明。');

INSERT INTO comment_like_record (comment_id, user_id) VALUES
(100001, 100003),
(100001, 100004),
(100002, 100002),
(100006, 100003),
(100011, 100002);

INSERT INTO article_like_record (article_id, user_id) VALUES
(100005, 100002),
(100005, 100003),
(100005, 100004),
(100001, 100003),
(100006, 100002);

ALTER TABLE user AUTO_INCREMENT = 100006;
ALTER TABLE category AUTO_INCREMENT = 100011;
ALTER TABLE article AUTO_INCREMENT = 100025;
ALTER TABLE comment AUTO_INCREMENT = 100014;
