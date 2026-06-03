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
    user_id      INT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username     VARCHAR(50)  NOT NULL UNIQUE COMMENT '用户名',
    password     VARCHAR(60)  NOT NULL COMMENT 'BCrypt 哈希',
    nickname     VARCHAR(50)  NOT NULL COMMENT '昵称',
    signature    VARCHAR(512) COMMENT '个性签名',
    avatar_image VARCHAR(512) DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/default.png' COMMENT '头像 URL',
    background_image VARCHAR(512) DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/background/default.jpg' COMMENT '背景图 URL',
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
    cover_image          VARCHAR(512) COMMENT '分类封面',
    article_count        INT DEFAULT 0 COMMENT '文章数量',
    create_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_name (user_id, category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE tag (
    tag_id        INT AUTO_INCREMENT PRIMARY KEY COMMENT '标签 ID',
    tag_name      VARCHAR(30) NOT NULL UNIQUE COMMENT '标签名',
    article_count INT DEFAULT 0 COMMENT '使用数量',
    create_time   TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT = 100001;

CREATE TABLE article (
    article_id    INT AUTO_INCREMENT PRIMARY KEY COMMENT '文章 ID',
    user_id       INT NOT NULL COMMENT '作者 ID',
    category_id   INT COMMENT '分类 ID',
    title         VARCHAR(200) NOT NULL COMMENT '标题',
    summary       VARCHAR(500) COMMENT '摘要',
    content       LONGTEXT NOT NULL COMMENT '内容（Markdown）',
    cover_image   VARCHAR(512) DEFAULT 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/article/background/default.jpg' COMMENT '封面 URL',
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
    root_id          INT NULL COMMENT '根评论 ID（一级评论的 comment_id）',
    reply_to_user_id INT NULL COMMENT '回复目标用户 ID',
    content          TEXT NOT NULL COMMENT '评论内容',
    like_count       INT DEFAULT 0 COMMENT '点赞数',
    status           TINYINT DEFAULT 1 COMMENT '状态 1:正常 0:删除',
    create_time      TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    FOREIGN KEY (article_id) REFERENCES article (article_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user (user_id) ON DELETE SET NULL,
    FOREIGN KEY (parent_id) REFERENCES comment (comment_id) ON DELETE CASCADE,
    FOREIGN KEY (reply_to_user_id) REFERENCES user (user_id) ON DELETE SET NULL,
    INDEX idx_article_parent (article_id, parent_id),
    INDEX idx_article_root   (article_id, root_id)
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

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------------
-- 演示数据（密码 123456 的 BCrypt 哈希）
-- ---------------------------------------------------------------------------
SET @pwd = '$2b$10$I/Me9zozCbEwd0Tlkd9SQuLOqDGpQ6yJWc5pcCIxXG8P/F222D3H2';

INSERT INTO user (user_id, username, password, nickname, signature, email, role) VALUES
(100001, 'admin', @pwd, '管理员',   '系统管理员，负责审核与公告',   'admin@demo.local', 'admin'),
(100002, 'alice', @pwd, '爱丽丝',   '前端与交互设计爱好者',         'alice@demo.local', 'user'),
(100003, 'bob',   @pwd, '鲍勃',     'Java 后端开发',               'bob@demo.local',   'user'),
(100004, 'carol', @pwd, '卡罗',     '算法与数据结构练习',           'carol@demo.local', 'user'),
(100005, 'david', @pwd, '大卫',     '摄影与旅行记录',               'david@demo.local', 'user');

INSERT INTO category (category_id, user_id, category_name, category_description) VALUES
(100001, 100001, '平台动态',   '站点公告与更新说明'),
(100002, 100001, '运维笔记',   '部署与配置记录'),
(100003, 100002, '前端开发',   'Vue 与工程化实践'),
(100004, 100002, '读书随笔',   '阅读摘录与感想'),
(100005, 100003, 'Java 后端',  'Spring Boot 相关'),
(100006, 100003, '生活杂谈',   '日常随想'),
(100007, 100004, '算法练习',   'LeetCode 题解'),
(100008, 100004, '面试准备',   '知识点整理'),
(100009, 100005, '旅行摄影',   '旅途见闻'),
(100010, 100005, '器材评测',   '相机与镜头体验');

INSERT INTO tag (tag_id, tag_name) VALUES
(100001, 'Vue 3'),
(100002, 'Spring Boot'),
(100003, 'MySQL'),
(100004, 'JavaScript'),
(100005, 'Java'),
(100006, 'Docker'),
(100007, 'LeetCode'),
(100008, '摄影'),
(100009, '前端'),
(100010, '后端'),
(100011, '数据库'),
(100012, '旅行'),
(100013, 'TypeScript'),
(100014, 'Redis'),
(100015, '面试'),
(100016, 'CSS'),
(100017, 'Python'),
(100018, 'Go');

SET @c1 = '## Vue 3 Composition API 入门\n\nVue 3 带来了全新的 Composition API，这是一套基于函数的 API，可以让我们更灵活地组织和复用代码。\n\n### 什么是 Composition API\n\nComposition API 是 Vue 3 引入的一种新的 API 风格，它基于函数组合的思想，旨在解决 Options API 中逻辑复用困难的问题。\n\n```javascript\nimport { ref, reactive, computed } from ''vue''\n\nexport default {\n  setup() {\n    const count = ref(0)\n    const state = reactive({\n      name: ''Vue 3'',\n      version: ''3.4.21''\n    })\n    const double = computed(() => count.value * 2)\n    return { count, state, double }\n  }\n}\n```\n\n### 响应式原理\n\nVue 3 的响应式系统基于 `Proxy` 实现，相比 Vue 2 的 `Object.defineProperty`，具有更好的性能和更全面的特性支持。通过 `ref()` 和 `reactive()` 创建的数据都会被代理，当数据变化时，视图会自动更新。\n\n### 生命周期钩子\n\n在 Composition API 中，生命周期钩子以 `on` 前缀的函数形式导入和使用：\n\n```javascript\nimport { onMounted, onUnmounted } from ''vue''\n\nonMounted(() => {\n  console.log(''组件挂载完成'')\n})\n```\n\n通过合理使用 Composition API，我们可以将相关逻辑聚合在一起，提高代码的可读性和可维护性。';

SET @c2 = '## Spring Boot 3 快速上手\n\nSpring Boot 3 要求 Java 17+，带来了对 Jakarta EE 10 的全面支持。\n\n### 项目结构\n\n```\nsrc/main/java/com/example/\n├── config/\n├── controller/\n├── entity/\n├── mapper/\n├── service/\n└── Application.java\n```\n\n### 数据访问\n\nMyBatis 注解方式简洁直观：\n\n```java\n@Mapper\npublic interface UserMapper {\n    @Select("SELECT * FROM user WHERE user_id = #{id}")\n    User findById(Integer id);\n}\n```\n\n### JWT 认证\n\n使用 Java JWT 实现无状态认证，通过拦截器验证 Token 并将用户信息存入 ThreadLocal，实现请求上下文隔离。\n\n合理分层、职责清晰是 Spring Boot 项目的核心设计原则。';

SET @c3 = '## LeetCode 双指针技巧总结\n\n双指针是解决数组和链表问题的利器，掌握这一技巧可以将 O(n²) 的暴力解法优化到 O(n)。\n\n### 快慢指针\n\n```java\npublic boolean hasCycle(ListNode head) {\n    ListNode slow = head, fast = head;\n    while (fast != null && fast.next != null) {\n        slow = slow.next;\n        fast = fast.next.next;\n        if (slow == fast) return true;\n    }\n    return false;\n}\n```\n\n### 左右指针\n\n适用于有序数组的搜索问题，例如两数之和、三数之和等经典题目。核心思路是根据当前和与目标值的大小关系移动指针，每次迭代将搜索空间缩小一半。\n\n坚持每天刷题，持续积累才是提升算法能力的正确方式。';

SET @c4 = '## 冰岛自驾游记：追逐极光的十天\n\n十月的冰岛，白昼渐短，极光开始频繁出没。带着相机和无限的期待，我踏上了这段梦想中的旅程。\n\n### Day 1-2：雷克雅未克\n\n首都不大，却充满了独特的北欧气质。色彩斑斓的建筑、街头涂鸦艺术、地热温泉……每一处都值得细细品味。哈尔格林姆斯教堂登高俯瞰全城，是来雷克雅未克必打卡的地标。\n\n### Day 3-5：黄金圈\n\n盖锡尔间歇泉每隔几分钟就会喷发一次，壮观震撼。黄金瀑布在阳光下折射出彩虹，美不胜收。辛格韦利尔国家公园横跨欧亚两大板块，地质奇观令人叹为观止。\n\n### 极光邂逅\n\n第七天深夜，我们开车驶离小镇，远离光污染。突然，北方天际出现了淡绿色的光带，随后越来越强，舞动起来……那一刻，所有疲惫都化为了感动。\n\n旅行不只是到达，更是途中每一个心动瞬间。';

INSERT INTO article (article_id, user_id, category_id, title, summary, content, status, view_count, like_count, comment_count) VALUES
(100001, 100002, 100003, 'Vue 3 Composition API 完全指南',
 '深入讲解 Vue 3 Composition API 的核心概念，包括 ref、reactive、computed 和生命周期钩子，帮助你快速掌握 Vue 3 的函数式编程范式。',
 @c1, 'published', 1280, 86, 13),
(100002, 100003, 100005, 'Spring Boot 3 + MyBatis 实战教程',
 '从零搭建 Spring Boot 3 项目，整合 MyBatis 进行数据库操作，实现 JWT 无状态认证，包含完整的项目结构和最佳实践。',
 @c2, 'published', 956, 64, 8),
(100003, 100004, 100007, 'LeetCode 双指针专题：从入门到精通',
 '系统整理双指针解题模板，覆盖快慢指针、左右指针、滑动窗口等常见变体，含 10+ 经典例题详解。',
 @c3, 'published', 743, 51, 6),
(100004, 100005, 100009, '冰岛十天自驾：追逐极光全记录',
 '十月冰岛自驾游全程记录，从雷克雅未克出发环岛一周，包含极光拍摄技巧、住宿攻略和行程安排。',
 @c4, 'published', 2341, 198, 4),
(100005, 100002, 100003, 'Vue Router 4 动态路由与懒加载',
 '详解 Vue Router 4 的路由懒加载、动态路由匹配、导航守卫等核心特性，附完整代码示例。',
 '## Vue Router 4 路由进阶\n\n路由懒加载可以显著减小初始包体积，通过 `() => import(''./views/Home.vue'')` 的方式实现按需加载。', 'published', 432, 29, 2),
(100006, 100003, 100005, 'Redis 缓存最佳实践',
 '探讨 Redis 在高并发场景下的缓存策略，包括缓存穿透、缓存击穿、缓存雪崩的解决方案。',
 '## Redis 缓存设计\n\n合理的缓存策略能将数据库压力降低 90% 以上。本文从实际业务场景出发，探讨各种缓存方案的优劣。', 'published', 671, 45, 3),
(100007, 100001, 100001, '平台 2025 年功能更新公告',
 '博客平台重大更新：新增标签系统、优化评论体验、引入 AI 写作助手，全面提升用户体验。',
 '## 平台更新说明\n\n本次更新引入了全新的标签系统，支持为文章添加多个标签，方便读者按兴趣筛选内容。', 'published', 389, 22, 1),
(100008, 100004, 100008, '2025 年前端面试高频考点整理',
 '汇总 2025 年前端面试中最常被问到的题目，涵盖 JavaScript 核心原理、Vue/React 框架、工程化配置等方向。',
 '## 前端面试准备\n\n掌握核心原理比死记硬背更重要。本文整理了常见的面试考点，并附上详细解析。', 'published', 1876, 134, 9),
(100009, 100002, 100004, '《人月神话》读书笔记',
 '软件工程经典著作精读，深入理解 Brooks 定律、二次系统效应、焦油坑等核心概念，结合现代软件实践加以诠释。',
 '## 人月神话\n\n这本写于 1975 年的书，至今仍然是软件工程领域的圣经。Brooks 的每一个论断都经受住了时间的考验。', 'published', 287, 31, 2),
(100010, 100005, 100010, '索尼 A7M4 实拍体验报告',
 '使用索尼 A7M4 拍摄三个月后的真实感受，从画质、对焦、续航、操控等维度全面评测。',
 '## 索尼 A7M4 深度评测\n\n三个月、五千张照片之后，我对这台相机有了非常全面的认识。总体而言，它是目前最均衡的全画幅微单。', 'published', 921, 73, 3);

-- 文章标签关联
INSERT INTO article_tag (article_id, tag_id) VALUES
(100001, 100001), (100001, 100004), (100001, 100009),
(100002, 100002), (100002, 100005), (100002, 100010),
(100003, 100007), (100003, 100005), (100003, 100015),
(100004, 100012), (100004, 100008),
(100005, 100001), (100005, 100004),
(100006, 100014), (100006, 100010), (100006, 100011),
(100007, 100001),
(100008, 100004), (100008, 100009), (100008, 100015),
(100009, 100005),
(100010, 100008);

-- 更新标签文章计数
UPDATE tag SET article_count = (
    SELECT COUNT(*) FROM article_tag WHERE article_tag.tag_id = tag.tag_id
);

-- 更新分类文章计数
UPDATE category SET article_count = (
    SELECT COUNT(*) FROM article WHERE article.category_id = category.category_id AND article.status = 'published'
);

-- 分层评论演示（文章 100001）
INSERT INTO comment (comment_id, article_id, user_id, parent_id, root_id, reply_to_user_id, content, like_count) VALUES
(100001, 100001, 100002, NULL,   100001, NULL,   '写得很清晰，Vue 3 组合式 API 的例子非常实用，看完之后感觉思路清晰多了！', 12),
(100002, 100001, 100003, 100001, 100001, 100002, '同感！尤其是响应式那一段，对比 Vue 2 讲解得很到位。', 5),
(100003, 100001, 100004, 100002, 100001, 100003, '响应式原理那章我也反复看了两遍，Proxy 比 defineProperty 确实强很多。', 3),
(100004, 100001, 100005, 100001, 100001, 100002, 'computed 的缓存机制讲解得很清楚，之前一直没弄明白。', 2),
(100005, 100001, 100003, NULL,   100005, NULL,   '有没有配套源码仓库？想跟着敲一遍加深理解。', 8),
(100006, 100001, 100002, 100005, 100005, 100003, '作者在文末放了 GitHub 链接，直接去找一下就好。', 4),
(100007, 100001, 100004, 100005, 100005, 100003, '我 fork 了一份，已经在本地跑起来了，有问题可以一起讨论。', 1),
(100008, 100001, 100005, NULL,   100008, NULL,   '排版很舒服，代码块高亮也很顺眼，阅读体验相当好。', 6),
(100009, 100001, 100003, NULL,   100009, NULL,   '期待续篇，讲讲 Pinia 状态管理和路由守卫的实际用法。', 9),
(100010, 100001, 100002, 100009, 100009, 100003, '续篇已经在草稿箱了，这周末应该能发出来。', 3),
(100011, 100001, 100004, NULL,   100011, NULL,   '已收藏，信息量很大，需要慢慢消化。', 4),
(100012, 100001, 100005, 100011, 100011, 100004, '同收藏！收藏夹里的文章以后有时间再看（大雾）。', 7),
(100013, 100001, 100003, 100012, 100011, 100005, '哈哈哈，收藏夹=学习了的幻觉，但这篇真的值得认真读。', 2);

-- 文章 100002 的评论
INSERT INTO comment (comment_id, article_id, user_id, parent_id, root_id, reply_to_user_id, content, like_count) VALUES
(100014, 100002, 100002, NULL,   100014, NULL,   'MyBatis 的注解方式比 XML 简洁太多了，特别适合中小项目。', 7),
(100015, 100002, 100004, 100014, 100014, 100002, '复杂 SQL 还是推荐 XML，动态 SQL 写起来更清晰。', 4),
(100016, 100002, 100003, 100015, 100014, 100004, '@script 注解配合 if/foreach 其实也挺好用的，习惯了就好。', 2),
(100017, 100002, 100005, NULL,   100017, NULL,   'JWT 部分讲解得很详细，之前一直对 Token 刷新机制不太理解。', 5);

-- 文章 100003 的评论
INSERT INTO comment (comment_id, article_id, user_id, parent_id, root_id, reply_to_user_id, content, like_count) VALUES
(100018, 100003, 100002, NULL,   100018, NULL,   '双指针模板总结得很全，刷题效率提升了不少！', 11),
(100019, 100003, 100003, 100018, 100018, 100002, '滑动窗口那道题能再出一个例子吗？感觉还没太懂。', 3),
(100020, 100003, 100004, 100019, 100018, 100003, '我来补充一下：滑动窗口的核心是维护窗口内的状态，当条件不满足时收缩左边界。', 6),
(100021, 100003, 100005, NULL,   100021, NULL,   '快慢指针检测环那道题是经典中的经典，面试必考。', 4);

INSERT INTO comment_like_record (comment_id, user_id) VALUES
(100001, 100003),(100001, 100004),(100001, 100005),
(100005, 100002),(100005, 100004),
(100009, 100002),(100009, 100004),(100009, 100005),
(100018, 100002),(100018, 100003);

INSERT INTO article_like_record (article_id, user_id) VALUES
(100001, 100003),(100001, 100004),(100001, 100005),
(100002, 100002),(100002, 100004),
(100003, 100002),(100003, 100003),
(100004, 100002),(100004, 100003),(100004, 100004);

INSERT INTO announcement (title, content, type) VALUES
('平台正式上线公告', '欢迎使用 Blog Platform！平台现已全面上线，支持 Markdown 写作、文章分类、标签系统、评论互动等功能，欢迎大家体验。', 'success'),
('新功能：标签系统上线', '文章标签功能现已上线，创作者可以为文章添加多个标签，读者也可以通过标签筛选感兴趣的内容。', 'info'),
('维护通知', '计划于本周日凌晨 2:00-4:00 进行系统维护，届时平台将短暂不可访问，请提前做好准备。', 'warning');

ALTER TABLE user AUTO_INCREMENT = 100006;
ALTER TABLE category AUTO_INCREMENT = 100011;
ALTER TABLE tag AUTO_INCREMENT = 100019;
ALTER TABLE article AUTO_INCREMENT = 100011;
ALTER TABLE comment AUTO_INCREMENT = 100022;
