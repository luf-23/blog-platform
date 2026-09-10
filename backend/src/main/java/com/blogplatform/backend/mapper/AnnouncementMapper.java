package com.blogplatform.backend.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import com.blogplatform.backend.entity.Announcement;

import java.util.List;

@Mapper
public interface AnnouncementMapper {
    @Select("SELECT * FROM announcement ORDER BY id DESC")
    List<Announcement> selectAll();

    @Select("SELECT * FROM announcement WHERE id > #{afterId} ORDER BY id ASC")
    List<Announcement> selectAfterId(Integer afterId);

    @Delete("DELETE FROM announcement WHERE id = #{id}")
    void deleteById(Integer id);

    @Insert("INSERT INTO announcement(title,content,type) VALUES(#{title}, #{content},#{type})")
    void add(Announcement announcement);
}
