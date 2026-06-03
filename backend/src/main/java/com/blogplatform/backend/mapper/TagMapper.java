package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.Tag;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TagMapper {

    @Select("SELECT * FROM tag ORDER BY article_count DESC")
    List<Tag> selectAll();

    @Select("SELECT * FROM tag ORDER BY article_count DESC LIMIT #{limit}")
    List<Tag> selectPopular(@Param("limit") int limit);

    @Select("SELECT t.* FROM tag t " +
            "INNER JOIN article_tag at ON t.tag_id = at.tag_id " +
            "WHERE at.article_id = #{articleId}")
    List<Tag> selectByArticleId(Integer articleId);

    @Select("<script>" +
            "SELECT t.* FROM tag t " +
            "INNER JOIN article_tag at ON t.tag_id = at.tag_id " +
            "WHERE at.article_id IN " +
            "<foreach collection='articleIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Tag> selectByArticleIds(@Param("articleIds") List<Integer> articleIds);

    @Select("SELECT * FROM tag WHERE tag_name = #{tagName}")
    Tag selectByName(String tagName);

    @Select("SELECT * FROM tag WHERE tag_id = #{tagId}")
    Tag selectById(Integer tagId);

    @Insert("INSERT INTO tag (tag_name) VALUES (#{tagName})")
    @Options(useGeneratedKeys = true, keyProperty = "tagId")
    void insert(Tag tag);

    @Insert("INSERT IGNORE INTO article_tag (article_id, tag_id) VALUES (#{articleId}, #{tagId})")
    void addArticleTag(@Param("articleId") Integer articleId, @Param("tagId") Integer tagId);

    @Delete("DELETE FROM article_tag WHERE article_id = #{articleId}")
    void deleteArticleTags(Integer articleId);

    @Update("UPDATE tag SET article_count = (SELECT COUNT(*) FROM article_tag WHERE tag_id = tag.tag_id)")
    void refreshAllCounts();

    @Update("UPDATE tag SET article_count = article_count + 1 WHERE tag_id = #{tagId}")
    void incrementCount(Integer tagId);

    @Update("UPDATE tag SET article_count = GREATEST(article_count - 1, 0) WHERE tag_id = #{tagId}")
    void decrementCount(Integer tagId);
}
