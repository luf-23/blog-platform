package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagMapper {

    @Select("SELECT t.*, p.tag_name AS parent_name FROM tag t " +
            "LEFT JOIN tag p ON p.tag_id = t.parent_id " +
            "ORDER BY COALESCE(p.sort_order, t.sort_order), COALESCE(p.tag_id, t.tag_id), " +
            "CASE WHEN t.parent_id IS NULL THEN 0 ELSE 1 END, t.sort_order, t.tag_name")
    List<Tag> selectAll();

    @Select("SELECT t.*, p.tag_name AS parent_name FROM tag t " +
            "JOIN tag p ON p.tag_id = t.parent_id " +
            "ORDER BY t.article_count DESC, t.sort_order, t.tag_name LIMIT #{limit}")
    List<Tag> selectPopular(@Param("limit") int limit);

    @Select("SELECT t.*, p.tag_name AS parent_name FROM tag t " +
            "INNER JOIN article_tag at ON t.tag_id = at.tag_id " +
            "LEFT JOIN tag p ON p.tag_id = t.parent_id " +
            "WHERE at.article_id = #{articleId}")
    List<Tag> selectByArticleId(Integer articleId);

    @Select("<script>" +
            "SELECT t.*, p.tag_name AS parent_name FROM tag t " +
            "INNER JOIN article_tag at ON t.tag_id = at.tag_id " +
            "LEFT JOIN tag p ON p.tag_id = t.parent_id " +
            "WHERE at.article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Tag> selectByArticleIds(@Param("articleIds") List<Integer> articleIds);

    @Select("SELECT t.*, p.tag_name AS parent_name FROM tag t LEFT JOIN tag p ON p.tag_id = t.parent_id WHERE t.tag_name = #{tagName}")
    Tag selectByName(String tagName);

    @Select("SELECT t.*, p.tag_name AS parent_name FROM tag t LEFT JOIN tag p ON p.tag_id = t.parent_id WHERE t.tag_id = #{tagId}")
    Tag selectById(Integer tagId);

    @Insert("INSERT INTO tag (tag_name, parent_id, sort_order) VALUES (#{tagName}, #{parentId}, #{sortOrder})")
    @Options(useGeneratedKeys = true, keyProperty = "tagId")
    void insert(Tag tag);

    @Insert("INSERT IGNORE INTO article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    void addArticleTag(@Param("articleId") Integer articleId, @Param("tagId") Integer tagId);

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    void deleteArticleTags(Integer articleId);

    @Update("UPDATE tag SET article_count = (SELECT COUNT(*) FROM article_tag WHERE tag_id = tag.tag_id) WHERE parent_id IS NOT NULL")
    void refreshLeafCounts();

    @Select("SELECT tag_id FROM tag WHERE parent_id IS NULL")
    List<Integer> selectRootIds();

    @Select("SELECT COALESCE(SUM(article_count), 0) FROM tag WHERE parent_id = #{parentId}")
    int sumChildCounts(Integer parentId);

    @Update("UPDATE tag SET article_count = #{articleCount} WHERE tag_id = #{tagId}")
    void updateArticleCount(@Param("tagId") Integer tagId, @Param("articleCount") Integer articleCount);

    default void refreshAllCounts() {
        refreshLeafCounts();
        for (Integer rootId : selectRootIds()) updateArticleCount(rootId, sumChildCounts(rootId));
    }

    @Update("UPDATE tag SET article_count = article_count + 1 WHERE tag_id = #{tagId}")
    void incrementCount(Integer tagId);

    @Update("UPDATE tag SET article_count = GREATEST(article_count - 1, 0) WHERE tag_id = #{tagId}")
    void decrementCount(Integer tagId);

    @Select("SELECT COUNT(*) FROM article_tag WHERE tag_id = #{tagId}")
    int countArticlesByTagId(Integer tagId);

    @Select("SELECT COUNT(*) FROM tag WHERE parent_id = #{tagId}")
    int countChildren(Integer tagId);

    @Update("UPDATE tag SET tag_name = #{tagName}, parent_id = #{parentId}, sort_order = #{sortOrder} WHERE tag_id = #{tagId}")
    int update(@Param("tagId") Integer tagId,
               @Param("tagName") String tagName,
               @Param("parentId") Integer parentId,
               @Param("sortOrder") Integer sortOrder);

    @Delete("DELETE FROM tag WHERE tag_id = #{tagId}")
    int deleteById(Integer tagId);
}
