package com.example.aijobs.auth;

import com.example.aijobs.auth.entity.PlatformUser;
import com.example.aijobs.auth.mapper.PlatformUserMapper;
import com.example.aijobs.auth.mapper.RoleMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final PlatformUserMapper userMapper;
    private final RoleMapper roleMapper;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(JwtService jwtService, PlatformUserMapper userMapper,
                                   RoleMapper roleMapper,
                                   RestAuthenticationEntryPoint authenticationEntryPoint) {
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization)) {
            filterChain.doFilter(request, response);
            return;
        }
        if (!authorization.startsWith("Bearer ") || !StringUtils.hasText(authorization.substring(7))) {
            reject(request, response, "Bearer 令牌格式不正确");
            return;
        }

        try {
            Claims claims = jwtService.parse(authorization.substring(7));
            Long userId = Long.valueOf(claims.getSubject());
            PlatformUser user = userMapper.selectById(userId);
            if (user == null || !"ACTIVE".equals(user.getStatus())) {
                reject(request, response, "用户不存在或已被禁用");
                return;
            }

            List<String> roles = roleMapper.selectCodesByUserId(userId);
            if (roles.isEmpty()) {
                reject(request, response, "用户未分配角色");
                return;
            }
            AuthenticatedUser principal = new AuthenticatedUser(userId, user.getUsername(), List.copyOf(roles));
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList();
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(principal, null, authorities));
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException exception) {
            reject(request, response, "令牌无效或已过期");
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response,
                new JwtAuthenticationException(message));
    }
}
