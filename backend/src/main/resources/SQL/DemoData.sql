-- Demo 数据：在 Table.sql 初始化后执行，可重复执行（会先清理同 ID 范围的旧数据）
-- 所有演示账号密码均为：123456（MD5: e10adc3949ba59abbe56e057f20f883e）

USE blog_platform;

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------------
-- 清理旧 Demo 数据（避免主键 / 唯一键冲突）
-- ---------------------------------------------------------------------------
SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM article
WHERE article_id BETWEEN 100001 AND 100024
   OR category_id BETWEEN 100001 AND 100010;

DELETE FROM category
WHERE category_id BETWEEN 100001 AND 100010
   OR user_id BETWEEN 100001 AND 100005;

DELETE FROM user
WHERE user_id BETWEEN 100001 AND 100005
   OR username IN ('admin', 'alice', 'bob', 'carol', 'david');

SET FOREIGN_KEY_CHECKS = 1;

-- 统一正文（约 150 字，所有文章共用）
SET @demo_content = '这是一段用于 Blog Platform 演示环境的统一正文。平台支持 Markdown 写作、文章分类、草稿与发布流程，以及社区广场中的公开阅读体验。你可以在「发现」页浏览他人文章，在「我的博客」中创建分类并撰写新稿。管理员账号用于审核与公告管理，普通账号则适合本地开发与功能联调测试。';

-- ---------------------------------------------------------------------------
-- 用户
-- ---------------------------------------------------------------------------
INSERT INTO user (user_id, username, password, nickname, signature, email) VALUES
(100001, 'admin', 'e10adc3949ba59abbe56e057f20f883e', '管理员', '系统管理员，负责审核与公告', 'admin@demo.local'),
(100002, 'alice', 'e10adc3949ba59abbe56e057f20f883e', '爱丽丝', '前端与交互设计爱好者', 'alice@demo.local'),
(100003, 'bob',   'e10adc3949ba59abbe56e057f20f883e', '鲍勃',   'Java 后端开发', 'bob@demo.local'),
(100004, 'carol', 'e10adc3949ba59abbe56e057f20f883e', '卡罗',   '算法与数据结构练习', 'carol@demo.local'),
(100005, 'david', 'e10adc3949ba59abbe56e057f20f883e', '大卫',   '摄影与旅行记录', 'david@demo.local');

ALTER TABLE user AUTO_INCREMENT = 100006;

-- ---------------------------------------------------------------------------
-- 分类
-- ---------------------------------------------------------------------------
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

ALTER TABLE category AUTO_INCREMENT = 100011;

-- ---------------------------------------------------------------------------
-- 文章（标题格式：标题-x；正文均为 @demo_content）
-- status: published 用于社区展示，draft / pending 便于测试管理流程
-- ---------------------------------------------------------------------------
INSERT INTO article (article_id, category_id, title, content, status) VALUES
-- admin
(100001, 100001, '标题-1',  @demo_content, 'published'),
(100002, 100001, '标题-2',  @demo_content, 'published'),
(100003, 100002, '标题-3',  @demo_content, 'pending'),
(100004, 100002, '标题-4',  @demo_content, 'draft'),
-- alice
(100005, 100003, '标题-5',  @demo_content, 'published'),
(100006, 100003, '标题-6',  @demo_content, 'published'),
(100007, 100003, '标题-7',  @demo_content, 'published'),
(100008, 100004, '标题-8',  @demo_content, 'published'),
(100009, 100004, '标题-9',  @demo_content, 'draft'),
-- bob
(100010, 100005, '标题-10', @demo_content, 'published'),
(100011, 100005, '标题-11', @demo_content, 'published'),
(100012, 100005, '标题-12', @demo_content, 'pending'),
(100013, 100006, '标题-13', @demo_content, 'published'),
(100014, 100006, '标题-14', @demo_content, 'published'),
-- carol
(100015, 100007, '标题-15', @demo_content, 'published'),
(100016, 100007, '标题-16', @demo_content, 'published'),
(100017, 100007, '标题-17', @demo_content, 'published'),
(100018, 100008, '标题-18', @demo_content, 'published'),
(100019, 100008, '标题-19', @demo_content, 'pending'),
-- david
(100020, 100009, '标题-20', @demo_content, 'published'),
(100021, 100009, '标题-21', @demo_content, 'published'),
(100022, 100009, '标题-22', @demo_content, 'published'),
(100023, 100010, '标题-23', @demo_content, 'published'),
(100024, 100010, '标题-24', @demo_content, 'draft');

ALTER TABLE article AUTO_INCREMENT = 100025;
