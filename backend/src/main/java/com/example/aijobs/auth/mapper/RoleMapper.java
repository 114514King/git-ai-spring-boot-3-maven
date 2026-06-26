package com.example.aijobs.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aijobs.auth.entity.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface RoleMapper extends BaseMapper<Role> {

    @Select("""
            SELECT r.code
            FROM role r
            INNER JOIN user_role ur ON ur.role_id = r.id
            WHERE ur.user_id = #{userId}
            ORDER BY r.code
            """)
    List<String> selectCodesByUserId(Long userId);
}
