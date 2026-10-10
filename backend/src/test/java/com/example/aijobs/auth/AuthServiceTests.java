package com.example.aijobs.auth;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.example.aijobs.auth.dto.LoginRequest;
import com.example.aijobs.auth.dto.LoginResponse;
import com.example.aijobs.auth.dto.RegisterRequest;
import com.example.aijobs.auth.dto.RegisterResponse;
import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.entity.Role;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.example.aijobs.auth.mapper.UserRoleMapper;
import com.example.aijobs.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTests {

    @Mock private PlatformUserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private UserRoleMapper userRoleMapper;

    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        JwtService jwtService = new JwtService(
                "test-secret-key-with-at-least-thirty-two-bytes", Duration.ofHours(2));
        authService = new AuthService(userMapper, roleMapper, userRoleMapper, passwordEncoder, jwtService);
    }

    @Test
    void registerHashesPasswordAndAssignsRequestedRole() {
        Role role = new Role();
        role.setId(1L);
        role.setCode("STUDENT");
        when(userMapper.selectCount(any(Wrapper.class))).thenReturn(0L);
        when(roleMapper.selectOne(any(Wrapper.class))).thenReturn(role);
        doAnswer(invocation -> {
            PlatformUser user = invocation.getArgument(0);
            user.setId(10L);
            return 1;
        }).when(userMapper).insert(any(PlatformUser.class));

        RegisterResponse response = authService.register(new RegisterRequest(
                "student1", "password123", "student@example.com", "", "测试学生", "STUDENT"));

        assertEquals(10L, response.userId());
        assertEquals("STUDENT", response.role());
        org.mockito.ArgumentCaptor<PlatformUser> userCaptor =
                org.mockito.ArgumentCaptor.forClass(PlatformUser.class);
        verify(userMapper).insert(userCaptor.capture());
        assertNotEquals("password123", userCaptor.getValue().getPasswordHash());
        assertEquals(true, passwordEncoder.matches("password123", userCaptor.getValue().getPasswordHash()));
    }

    @Test
    void loginReturnsBearerTokenForActiveUser() {
        PlatformUser user = new PlatformUser();
        user.setId(10L);
        user.setUsername("student1");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(roleMapper.selectCodesByUserId(10L)).thenReturn(List.of("STUDENT"));

        LoginResponse response = authService.login(new LoginRequest("student1", "password123"));

        assertEquals("Bearer", response.tokenType());
        assertEquals(List.of("STUDENT"), response.roles());
        assertEquals("10", new JwtService(
                "test-secret-key-with-at-least-thirty-two-bytes", Duration.ofHours(2))
                .parse(response.accessToken()).getSubject());
    }

    @Test
    void loginRejectsWrongPassword() {
        PlatformUser user = new PlatformUser();
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("student1", "wrong-password")));

        assertEquals(401, exception.getStatus().value());
    }

    @Test
    void loginReturnsDatabaseRolesForHrAndAdmin() {
        PlatformUser user = new PlatformUser();
        user.setId(7L);
        user.setUsername("synthetic-hr-admin");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(roleMapper.selectCodesByUserId(7L)).thenReturn(List.of("HR", "ADMIN"));

        LoginResponse response = authService.login(new LoginRequest("synthetic-hr-admin", "password123"));

        assertEquals(List.of("HR", "ADMIN"), response.roles());
        assertThrows(UnsupportedOperationException.class, () -> response.roles().add("STUDENT"));
    }

    @Test
    void loginRejectsUserWithoutAssignedRoles() {
        PlatformUser user = new PlatformUser();
        user.setId(7L);
        user.setUsername("synthetic-no-role");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setStatus("ACTIVE");
        when(userMapper.selectOne(any(Wrapper.class))).thenReturn(user);
        when(roleMapper.selectCodesByUserId(7L)).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.login(new LoginRequest("synthetic-no-role", "password123")));

        assertEquals(403, exception.getStatus().value());
    }
}
