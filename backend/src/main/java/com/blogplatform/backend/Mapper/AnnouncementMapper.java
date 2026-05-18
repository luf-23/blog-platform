package com.blogplatform.backend.Mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.blogplatform.backend.entity.Announcement;

import java.util.List;

@Mapper
public interface AnnouncementMapper {
    @Select("SELECT * FROM announcement")
    List<Announcement> selectAll();

    @Delete("DELETE FROM announcement WHERE id = #{id}")
    void deleteById(Integer id);

    @Insert("INSERT INTO announcement(title,content,type) VALUES(#{title}, #{content},#{type})")
    void add(Announcement announcement);
}
