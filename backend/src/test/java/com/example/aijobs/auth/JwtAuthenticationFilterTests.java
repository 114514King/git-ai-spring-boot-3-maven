package com.example.aijobs.auth;

import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTests {

    @Mock private PlatformUserMapper userMapper;
    @Mock private RoleMapper roleMapper;
    @Mock private FilterChain filterChain;

    private JwtService jwtService;
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-with-at-least-thirty-two-bytes", Duration.ofHours(2));
        filter = new JwtAuthenticationFilter(jwtService, userMapper, roleMapper,
                new RestAuthenticationEntryPoint(new ObjectMapper()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenBuildsAuthenticationWithDatabaseRoles() throws Exception {
        PlatformUser user = new PlatformUser();
        user.setId(42L);
        user.setUsername("student1");
        user.setStatus("ACTIVE");
        when(userMapper.selectById(42L)).thenReturn(user);
        when(roleMapper.selectCodesByUserId(42L)).thenReturn(List.of("STUDENT"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + jwtService.issue(42L, "student1").value());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        AuthenticatedUser principal = assertInstanceOf(AuthenticatedUser.class,
                SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        assertEquals(42L, principal.id());
        assertEquals(List.of("STUDENT"), principal.roles());
        assertEquals("ROLE_STUDENT", SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().iterator().next().getAuthority());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void invalidTokenReturnsUnifiedUnauthorizedResponse() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer invalid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertFalse(response.getContentAsString().isBlank());
        verifyNoInteractions(userMapper, roleMapper, filterChain);
    }
}
