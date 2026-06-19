package com.yxy.monitor.mapper;

import com.yxy.monitor.entity.SysUser;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface SysUserMapper {
    // 根据用户名查询用户
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    SysUser selectByUsername(String username);

    // 注册新增用户
    @Insert("INSERT INTO sys_user(username, password, nickname, email, status, create_time) " +
    "VALUES(#{username}, #{password}, #{nickname}, #{email}, 1, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(SysUser sysUser);

    // 修改密码
    @Update("UPDATE sys_user SET password = #{newPwd} WHERE ID = #{userId}")
    void updatePwd(@Param("userId") Long userId, @Param("newPwd") String newPwd);

    // 查询用户所有角色标识
    @Select("SELECT r.role_key FROM sys_role r LEFT JOIN sys_user_role ur ON r.id = ur.role_id WHERE ur.user_id = #{userId}")
    List<String> getUserRoleKeys(@Param("userId") Long userId);

    // 查询用户所有权限标识
    @Select("SELECT p.perm_key FROM sys_permission p " +
            "LEFT JOIN sys_role_perm rp ON p.id = rp.perm_id " +
            "LEFT JOIN sys_user_role ur ON rp.role_id = ur.role_id " +
            "WHERE ur.user_id = #{userId}"
    )
    List<String> getUserPermKeys(@Param("userId") Long userId);
}
