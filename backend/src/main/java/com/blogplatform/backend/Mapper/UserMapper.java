package com.blogplatform.backend.Mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import com.blogplatform.backend.entity.User;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("select * from user where username = #{username}")
    User selectByUsername(String username);

    @Select("select * from user where email = #{email}")
    User selectByEmail(String email);

    @Insert("insert into user (username, password,nickname,email) values (#{username}, #{password},'匿名用户',#{email})")
    void add(String username, String password,String email);

    @Select("select * from user where user_id=#{id}")
    User selectById(int id);
    @Update( "update user set nickname=#{nickname}, signature=#{signature},avatar_image=#{avatarImage},background_image=#{backgroundImage},update_time=now() where user_id=#{userId} and username=#{username}")
    void update(User user);

    @Update("update user set last_login=now() where user_id=#{userId}")
    void updateLoginTime(Integer userId);

    @Update("update user set last_login_ip=#{ip},update_time=update_time where user_id= #{userId}")
    void updateLoginIp(Integer userId,String ip);

    @Select("select email from user")
    List<String> selectAllEmails();

    @Update("update user set password=#{password} where email=#{email}")
    void resetPassword(String email, String password);
}
