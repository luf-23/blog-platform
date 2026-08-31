DROP DATABASE IF EXISTS blog_platform;

CREATE DATABASE blog_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE blog_platform;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE user (
    user_id      INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username     VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password     VARCHAR(60)  NOT NULL COMMENT 'BCrypt 哈希',
    nickname     VARCHAR(50)  NOT NULL COMMENT '昵称',
    signature    VARCHAR(512) COMMENT '个性签名',
    avatar_image VARCHAR(512) NOT NULL DEFAULT '/defaults/avatar.png' COMMENT '头像 URL',
    background_image VARCHAR(512) NOT NULL DEFAULT '/defaults/profile-background.png' COMMENT '背景图 URL',
    email        VARCHAR(255) NULL UNIQUE COMMENT '邮箱',
    role         ENUM('user','admin') DEFAULT 'user' COMMENT '角色',
    last_login   TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(255) COMMENT '最后登录 IP',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE category (
    category_id          INT AUTO_INCREMENT PRIMARY KEY COMMENT '分类 ID',
    user_id              INT NOT NULL COMMENT '用户 ID',
    category_name        VARCHAR(50) NOT NULL COMMENT '分类名称',
    category_description VARCHAR(200) COMMENT '分类描述',
    cover_image          VARCHAR(512) NOT NULL DEFAULT '/defaults/article-cover.png' COMMENT '分类封面',
    article_count        INT DEFAULT 0 COMMENT '文章数量',
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_name (user_id, category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE tag (
    tag_id        INT AUTO_INCREMENT PRIMARY KEY COMMENT '标签 ID',
    tag_name      VARCHAR(30) NOT NULL UNIQUE COMMENT '标签名',
    parent_id     INT NULL COMMENT '一级标签 ID；NULL 表示一级标签',
    sort_order    INT NOT NULL DEFAULT 0 COMMENT '同级排序',
    article_count INT DEFAULT 0 COMMENT '使用数量',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_tag_parent FOREIGN KEY (parent_id) REFERENCES tag (tag_id) ON DELETE RESTRICT,
    INDEX idx_tag_parent_sort (parent_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE article (
    article_id    INT AUTO_INCREMENT PRIMARY KEY COMMENT '文章 ID',
    user_id       INT NOT NULL COMMENT '作者 ID',
    category_id   INT COMMENT '分类 ID',
    title         VARCHAR(200) NOT NULL COMMENT '标题',
    summary       VARCHAR(500) COMMENT '摘要',
    content       LONGTEXT NOT NULL COMMENT '内容（Markdown）',
    cover_image   VARCHAR(512) NOT NULL DEFAULT '/defaults/article-cover.png' COMMENT '封面 URL',
    status        ENUM('draft','published','pending') DEFAULT 'draft' COMMENT '文章状态',
    view_count    INT DEFAULT 0 COMMENT '浏览量',
    like_count    INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    FOREIGN KEY (category_id) REFERENCES category (category_id) ON DELETE SET NULL,
    INDEX idx_status_create (status, create_time),
    INDEX idx_user_status (user_id, status),
    FULLTEXT INDEX ft_title_summary (title, summary)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE article_tag (
    article_id INT NOT NULL COMMENT '文章 ID',
    tag_id     INT NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (article_id, tag_id),
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    FOREIGN KEY (tag_id) REFERENCES tag (tag_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE comment (
    comment_id       INT AUTO_INCREMENT PRIMARY KEY COMMENT '评论 ID',
    article_id       INT NOT NULL COMMENT '文章 ID',
    user_id          INT NULL COMMENT '评论者 ID',
    parent_id        INT NULL COMMENT '父评论 ID，NULL 为一级评论',
    reply_to_user_id INT NULL COMMENT '回复目标用户 ID',
    content          TEXT NOT NULL COMMENT '评论内容',
    like_count       INT DEFAULT 0 COMMENT '点赞数',
    status           TINYINT DEFAULT 1 COMMENT '状态 1:正常 0:删除',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE SET NULL,
    FOREIGN KEY (parent_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to_user_id) REFERENCES user (user_id) ON DELETE SET NULL,
    INDEX idx_article_parent (article_id, parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE announcement (
    id          INT AUTO_INCREMENT PRIMARY KEY COMMENT '公告 ID',
    title       VARCHAR(100) NOT NULL COMMENT '标题',
    content     TEXT NOT NULL COMMENT '内容',
    date        TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    type        ENUM('success','warning','danger','info') DEFAULT 'info' COMMENT '公告类型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE article_like_record (
    id          INT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞 ID',
    article_id  INT NOT NULL COMMENT '文章 ID',
    user_id     INT NULL COMMENT '点赞者 ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_article_user (article_id, user_id),
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE comment_like_record (
    id          INT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞 ID',
    comment_id  INT NOT NULL COMMENT '评论 ID',
    user_id     INT NULL COMMENT '点赞者 ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    UNIQUE KEY uk_comment_user (comment_id, user_id),
    FOREIGN KEY (comment_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE user_follow (
    follower_id  INT NOT NULL COMMENT '关注者',
    following_id INT NOT NULL COMMENT '被关注者',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id),
    INDEX idx_user_follow_following (following_id, create_time),
    FOREIGN KEY (follower_id) REFERENCES user(user_id) ON DELETE CASCADE,
    FOREIGN KEY (following_id) REFERENCES user(user_id) ON DELETE CASCADE,
    CHECK (follower_id <> following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

SET FOREIGN_KEY_CHECKS = 1;
