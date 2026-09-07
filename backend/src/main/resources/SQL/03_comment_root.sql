-- Existing databases only: stop the backend, select the application database,
-- then run this script once before starting the updated backend.
-- New databases already contain this column and indexes in 01_table.sql.
USE blog_platform;

-- Resolve all descendants, including soft-deleted intermediate comments.
-- Build the mapping before changing the schema so a recursion error stops here.
CREATE TEMPORARY TABLE comment_root_backfill AS
WITH RECURSIVE comment_tree AS (
    SELECT comment_id, article_id, comment_id AS root_comment_id
    FROM comment
    WHERE parent_id IS NULL
    UNION ALL
    SELECT child.comment_id, child.article_id, tree.root_comment_id
    FROM comment child
    JOIN comment_tree tree ON child.parent_id = tree.comment_id
                          AND child.article_id = tree.article_id
)
SELECT comment_id, root_comment_id FROM comment_tree;

ALTER TABLE comment
    ADD COLUMN root_comment_id INT NULL COMMENT '所属一级评论 ID，一级评论为 NULL' AFTER parent_id;

UPDATE comment c
JOIN comment_root_backfill roots ON roots.comment_id = c.comment_id
SET c.root_comment_id = roots.root_comment_id
WHERE c.parent_id IS NOT NULL;

ALTER TABLE comment
    DROP INDEX idx_article_parent,
    ADD INDEX idx_comment_root_page (article_id, parent_id, status, create_time, comment_id),
    ADD INDEX idx_comment_reply_page (article_id, root_comment_id, status, create_time, comment_id);

DROP TEMPORARY TABLE comment_root_backfill;

-- Must return no rows. If it returns rows, repair orphaned, cross-article or
-- cyclic parent relationships before starting the backend.
SELECT comment_id, article_id, parent_id
FROM comment
WHERE parent_id IS NOT NULL AND root_comment_id IS NULL;
