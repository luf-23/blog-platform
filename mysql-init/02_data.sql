USE blog_platform;

SET NAMES utf8mb4;

-- Development administrator. Change the password immediately in non-local environments.
-- Login: admin / Admin@123
INSERT INTO user (username, password, nickname, email, role)
VALUES (
    'admin',
    '$2a$10$QUy17DlLViblocZSivLXdemuDqPQC2oGW6r0mCiKihUpx/H13aGDy',
    'Blog-Platform Admin',
    'admin@blog-platform.local',
    'admin'
);

-- Development user. Login: luf-23 / yangzhijun%
INSERT INTO user (username, password, nickname, role)
VALUES (
    'luf-23',
    '$2a$10$II4ufoq7ODvPTHjSgoKfW.sHUdrSZzQt9P4Gd70EyavAOfeeo9W4G',
    'luf-23',
    'user'
);

-- 一级标签只用于分组，二级标签可贴到文章。
INSERT INTO tag (tag_name, sort_order) VALUES
    ('技术研发', 10), ('产品设计', 20), ('职场管理', 30), ('个人成长', 40), ('行业观察', 50);

INSERT INTO tag (tag_name, parent_id, sort_order)
SELECT '前端开发', tag_id, 10 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT '后端开发', tag_id, 20 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT '移动端', tag_id, 30 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT '数据库', tag_id, 40 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT '运维部署', tag_id, 50 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT '架构设计', tag_id, 60 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT '算法', tag_id, 70 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT 'AI/ML', tag_id, 80 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT '测试', tag_id, 90 FROM tag WHERE tag_name = '技术研发' UNION ALL
SELECT '安全', tag_id, 100 FROM tag WHERE tag_name = '技术研发';

INSERT INTO tag (tag_name, parent_id, sort_order)
SELECT '产品经理', tag_id, 10 FROM tag WHERE tag_name = '产品设计' UNION ALL
SELECT '交互设计', tag_id, 20 FROM tag WHERE tag_name = '产品设计' UNION ALL
SELECT 'UI视觉', tag_id, 30 FROM tag WHERE tag_name = '产品设计' UNION ALL
SELECT '用户体验', tag_id, 40 FROM tag WHERE tag_name = '产品设计' UNION ALL
SELECT '需求分析', tag_id, 50 FROM tag WHERE tag_name = '产品设计';

INSERT INTO tag (tag_name, parent_id, sort_order)
SELECT '职场技能', tag_id, 10 FROM tag WHERE tag_name = '职场管理' UNION ALL
SELECT '团队管理', tag_id, 20 FROM tag WHERE tag_name = '职场管理' UNION ALL
SELECT '面试求职', tag_id, 30 FROM tag WHERE tag_name = '职场管理' UNION ALL
SELECT '绩效晋升', tag_id, 40 FROM tag WHERE tag_name = '职场管理' UNION ALL
SELECT '沟通协作', tag_id, 50 FROM tag WHERE tag_name = '职场管理';

INSERT INTO tag (tag_name, parent_id, sort_order)
SELECT '学习方法', tag_id, 10 FROM tag WHERE tag_name = '个人成长' UNION ALL
SELECT '阅读写作', tag_id, 20 FROM tag WHERE tag_name = '个人成长' UNION ALL
SELECT '时间管理', tag_id, 30 FROM tag WHERE tag_name = '个人成长' UNION ALL
SELECT '理财投资', tag_id, 40 FROM tag WHERE tag_name = '个人成长' UNION ALL
SELECT '健康生活', tag_id, 50 FROM tag WHERE tag_name = '个人成长';

INSERT INTO tag (tag_name, parent_id, sort_order)
SELECT '互联网', tag_id, 10 FROM tag WHERE tag_name = '行业观察' UNION ALL
SELECT '科技趋势', tag_id, 20 FROM tag WHERE tag_name = '行业观察' UNION ALL
SELECT '创业', tag_id, 30 FROM tag WHERE tag_name = '行业观察' UNION ALL
SELECT '商业模式', tag_id, 40 FROM tag WHERE tag_name = '行业观察' UNION ALL
SELECT '政策解读', tag_id, 50 FROM tag WHERE tag_name = '行业观察';
