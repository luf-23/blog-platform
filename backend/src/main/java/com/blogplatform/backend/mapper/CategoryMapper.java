package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.Category;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("SELECT * FROM category WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Category> selectByUserId(Integer userId);

    @Select("SELECT * FROM category WHERE category_id = #{categoryId}")
    Category selectById(Integer categoryId);

    @Select("SELECT * FROM category WHERE user_id = #{userId} AND category_name = #{categoryName} LIMIT 1")
    Category selectByUserIdAndName(@Param("userId") Integer userId, @Param("categoryName") String categoryName);

    @Insert("INSERT INTO category (user_id, category_name, category_description, cover_image, create_time, update_time) " +
            "VALUES (#{userId}, #{categoryName}, #{categoryDescription}, #{coverImage}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "categoryId")
    void add(Category category);

    @Delete("DELETE FROM category WHERE category_id = #{categoryId}")
    void deleteById(Integer categoryId);

    @Update("UPDATE category SET category_name=#{categoryName}, category_description=#{categoryDescription}, cover_image=#{coverImage}, " +
            "update_time=NOW() WHERE category_id=#{categoryId}")
    void update(Category category);

    @Select("SELECT user_id FROM category WHERE category_id = #{categoryId}")
    Integer selectUserIdByCategoryId(Integer categoryId);

    @Update("UPDATE category SET article_count = (SELECT COUNT(*) FROM article WHERE category_id = category.category_id)")
    void refreshAllCounts();

    @Update("UPDATE category SET article_count = GREATEST(article_count - 1, 0) WHERE category_id = #{categoryId}")
    void decrementCount(Integer categoryId);

    @Update("UPDATE category SET article_count = article_count + 1 WHERE category_id = #{categoryId}")
    void incrementCount(Integer categoryId);

    @Select("SELECT category_id FROM category WHERE user_id=#{userId} AND category_name=#{categoryName} AND category_description=#{categoryDescription}")
    Integer selectCategoryId(@Param("userId") Integer userId,
                             @Param("categoryName") String categoryName,
                             @Param("categoryDescription") String categoryDescription);
}
