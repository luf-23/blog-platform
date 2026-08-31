USE blog_platform;

-- Safe upgrade path for an existing database; 01_table.sql already contains
-- the same definitions for fresh installations.
CREATE TABLE IF NOT EXISTS user_follow (
    follower_id  INT NOT NULL COMMENT '关注者',
    following_id INT NOT NULL COMMENT '被关注者',
    create_time  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (follower_id, following_id),
    INDEX idx_user_follow_following (following_id, create_time),
    CONSTRAINT fk_user_follow_follower FOREIGN KEY (follower_id) REFERENCES user(user_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_follow_following FOREIGN KEY (following_id) REFERENCES user(user_id) ON DELETE CASCADE,
    CHECK (follower_id <> following_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE user
SET avatar_image = '/defaults/avatar.png'
WHERE avatar_image IS NULL OR avatar_image = ''
   OR avatar_image = 'https://luf-23.oss-cn-wuhan-lr.aliyuncs.com/avatar/avatar-4.png';

UPDATE user
SET background_image = '/defaults/profile-background.png'
WHERE background_image IS NULL OR background_image = '' OR background_image = '/background/background1.jpg';

UPDATE article
SET cover_image = '/defaults/article-cover.png'
WHERE cover_image IS NULL OR cover_image = '';

UPDATE category
SET cover_image = '/defaults/article-cover.png'
WHERE cover_image IS NULL OR cover_image = '';

ALTER TABLE user
    MODIFY avatar_image VARCHAR(512) NOT NULL DEFAULT '/defaults/avatar.png' COMMENT '头像 URL',
    MODIFY background_image VARCHAR(512) NOT NULL DEFAULT '/defaults/profile-background.png' COMMENT '背景图 URL';

ALTER TABLE article
    MODIFY cover_image VARCHAR(512) NOT NULL DEFAULT '/defaults/article-cover.png' COMMENT '封面 URL';

ALTER TABLE category
    MODIFY cover_image VARCHAR(512) NOT NULL DEFAULT '/defaults/article-cover.png' COMMENT '分类封面';
