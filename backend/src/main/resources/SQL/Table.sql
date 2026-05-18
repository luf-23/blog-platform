-- 创建数据库
create database blog_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

use blog_platform;

drop table if exists user;
-- 用户表
CREATE TABLE user (
    user_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password CHAR(64) NOT NULL COMMENT '加密密码',
    nickname VARCHAR(50) NOT NULL COMMENT '昵称',
    signature VARCHAR(512) COMMENT '个性签名',
    avatar_image VARCHAR(512) COMMENT '头像URL',
    background_image VARCHAR(512) COMMENT '背景图URL',
    -- email VARCHAR(255) NOT NULL UNIQUE COMMENT '邮箱',
    last_login TIMESTAMP NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) COMMENT '最后登录IP',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=100001;

ALTER TABLE user MODIFY last_login_ip VARCHAR(255) COMMENT '最后登录IP';
ALTER TABLE user ADD COLUMN email VARCHAR(255) NULL UNIQUE comment '邮箱';
ALTER TABLE user MODIFY background_image  VARCHAR(512) comment '背景图URL' DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/background/default.jpg';
ALTER TABLE user MODIFY avatar_image VARCHAR(512) comment '头像URL' DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png';
ALTER TABLE user MODIFY update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间';

-- 文章分类表
CREATE TABLE category(
    category_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '分类ID',
    user_id INT NOT NULL COMMENT '用户ID',
    category_name VARCHAR(50) NOT NULL COMMENT '分类名称',
    category_description VARCHAR(50) COMMENT '分类描述',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=100001;

ALTER TABLE category ADD CONSTRAINT uk_user_name_description UNIQUE (user_id, category_name, category_description);




-- 文章表
CREATE TABLE article(
    article_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '文章ID',
    category_id INT NOT NULL COMMENT '分类ID',
    title VARCHAR(50) NOT NULL COMMENT '标题',
    content LONGTEXT NOT NULL COMMENT '内容',
    status ENUM('draft', 'published','pending') DEFAULT 'draft' COMMENT '文章状态',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    cover_image VARCHAR(512) comment '文章封面图URL' DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/article/background/default.jpg',
    FOREIGN KEY (category_id) REFERENCES category(category_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=100001;


-- 评论表
CREATE TABLE comment(
    comment_id INT AUTO_INCREMENT PRIMARY KEY COMMENT '评论ID',
    article_id INT NOT NULL COMMENT '文章ID',
    user_id INT NULL COMMENT '评论者ID',
    content TEXT NOT NULL COMMENT '评论内容',
    is_read TINYINT(1) DEFAULT 0 COMMENT '是否已读',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (article_id) REFERENCES article(article_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=100001;


-- 公告表
CREATE TABLE announcement(
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    title VARCHAR(50) NOT NULL COMMENT '标题',
    content TEXT NOT NULL COMMENT '内容',
    date TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
    type ENUM('success', 'warning', 'danger', 'info') DEFAULT 'success' COMMENT '公告类型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=100001;


-- 文章点赞表
CREATE TABLE article_like_record(
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    article_id INT NOT NULL COMMENT '文章ID',
    user_id INT NULL COMMENT '点赞者ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    FOREIGN KEY (article_id) REFERENCES article(article_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=100001;


-- 评论点赞表
CREATE TABLE comment_like_record(
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '点赞ID',
    comment_id INT NOT NULL COMMENT '评论ID',
    user_id INT NULL COMMENT '点赞者ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
    FOREIGN KEY (comment_id) REFERENCES comment(comment_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(user_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=100001;