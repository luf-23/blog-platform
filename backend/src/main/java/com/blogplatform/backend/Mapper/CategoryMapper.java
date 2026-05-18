package com.blogplatform.backend.Mapper;

import org.apache.ibatis.annotations.*;
import com.blogplatform.backend.entity.Category;

import java.util.List;

@Mapper
public interface CategoryMapper {

    @Select("select * from category where user_id = #{userId}")
    List<Category> selectByUserId(Integer userId);

    @Insert("insert into category (user_id, category_name, category_description, create_time, update_time) values (#{userId},#{categoryName},#{categoryDescription},now(),now())")
    void add(Category category);

    @Delete("delete from category where category_id = #{categoryId}")
    void deleteById(Integer categoryId);

    @Update("update category set category_name = #{categoryName}, category_description = #{categoryDescription}, update_time = now() where category_id = #{categoryId}")
    void update(Category category);


    @Select("select user_id from category where category_id = #{categoryId}")
    Integer selectUserIdByCategoryId(Integer categoryId);

    @Select("select category_id from category where user_id=#{userId} and category_name=#{categoryName} and category_description=#{categoryDescription}")
    Integer selectCategoryId(Integer userId, String categoryName,String categoryDescription);
}
