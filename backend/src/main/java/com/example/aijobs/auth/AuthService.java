package com.example.aijobs.auth;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.aijobs.auth.dto.LoginRequest;
import com.example.aijobs.auth.dto.LoginResponse;
import com.example.aijobs.auth.dto.RegisterRequest;
import com.example.aijobs.auth.dto.RegisterResponse;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.entity.Role;
import com.example.aijobs.auth.entity.UserRole;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.auth.mapper.UserRoleMapper;
import com.example.aijobs.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final PlatformUserMapper userMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(PlatformUserMapper userMapper, RoleMapper roleMapper,
                       UserRoleMapper userRoleMapper, PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        LambdaQueryWrapper<PlatformUser> duplicateQuery = Wrappers.<PlatformUser>lambdaQuery()
                .eq(PlatformUser::getUsername, request.username())
                .or().eq(PlatformUser::getEmail, request.username())
                .or().eq(PlatformUser::getUsername, request.email())
                .or().eq(PlatformUser::getEmail, request.email());
        if (StringUtils.hasText(request.phone())) {
            duplicateQuery.or().eq(PlatformUser::getPhone, request.phone());
        }
        long duplicates = userMapper.selectCount(duplicateQuery);
        if (duplicates > 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "用户名、邮箱或手机号已存在");
        }

        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, request.role()));
        if (role == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "注册角色不存在，请先执行数据库初始化脚本");
        }

        PlatformUser user = new PlatformUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEmail(request.email());
        user.setPhone(StringUtils.hasText(request.phone()) ? request.phone() : null);
        user.setRealName(StringUtils.hasText(request.realName()) ? request.realName() : null);
        user.setStatus("ACTIVE");
        userMapper.insert(user);

        UserRole userRole = new UserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(role.getId());
        userRoleMapper.insert(userRole);
        return new RegisterResponse(user.getId(), user.getUsername(), role.getCode());
    }

    public LoginResponse login(LoginRequest request) {
        PlatformUser user = userMapper.selectOne(Wrappers.<PlatformUser>lambdaQuery()
                .eq(PlatformUser::getUsername, request.account())
                .or().eq(PlatformUser::getEmail, request.account()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (!"ACTIVE".equals(user.getStatus())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "用户已被禁用");
        }
        JwtService.Token token = jwtService.issue(user.getId(), user.getUsername());
        return new LoginResponse("Bearer", token.value(), token.expiresAt());
    }
}
