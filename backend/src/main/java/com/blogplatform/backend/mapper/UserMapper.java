package com.blogplatform.backend.mapper;

import com.blogplatform.backend.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM user WHERE email = #{email}")
    User selectByEmail(String email);

    @Insert("INSERT INTO user (username, password, nickname, email) VALUES (#{username}, #{password}, #{nickname}, #{email})")
    void add(@Param("username") String username,
             @Param("password") String password,
             @Param("nickname") String nickname,
             @Param("email") String email);

    @Select("SELECT * FROM user WHERE user_id = #{id}")
    User selectById(int id);

    @Select("<script>SELECT * FROM user WHERE user_id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<User> selectByIds(@Param("ids") List<Integer> ids);

    @Update("UPDATE user SET nickname=#{nickname}, signature=#{signature}, " +
            "avatar_image=#{avatarImage}, background_image=#{backgroundImage}, " +
            "update_time=NOW() WHERE user_id=#{userId}")
    void update(User user);

    @Update("UPDATE user SET last_login=NOW() WHERE user_id=#{userId}")
    void updateLoginTime(Integer userId);

    @Update("UPDATE user SET last_login_ip=#{ip}, update_time=update_time WHERE user_id=#{userId}")
    void updateLoginIp(@Param("userId") Integer userId, @Param("ip") String ip);

    @Update("UPDATE user SET password=#{password} WHERE email=#{email}")
    void resetPassword(@Param("email") String email, @Param("password") String password);

    @Select("SELECT email FROM user WHERE email IS NOT NULL")
    List<String> selectAllEmails();

    @Select("SELECT COUNT(*) FROM user")
    int totalCount();

    @Select("SELECT * FROM user ORDER BY create_time DESC LIMIT #{offset}, #{pageSize}")
    List<User> selectPage(@Param("offset") int offset, @Param("pageSize") int pageSize);

    @Select("SELECT COUNT(*) FROM user")
    int countAll();

    @Update("UPDATE user SET role=#{role} WHERE user_id=#{userId}")
    void updateRole(@Param("userId") Integer userId, @Param("role") String role);
}
